/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.AutoInstallsLayout.LayoutParserCallback;
import com.android.launcher3.LauncherSettings.Favorites;
import com.android.launcher3.LauncherSettings.WorkspaceScreens;
import com.android.launcher3.config.FeatureFlags;
import com.android.launcher3.model.DbDowngradeHelper;
import com.android.launcher3.provider.LauncherDbUtils;
import com.android.launcher3.provider.LauncherDbUtils.SQLiteTransaction;
import com.android.launcher3.provider.RestoreDbTask;
import com.android.launcher3.util.NoLocaleSQLiteHelper;
import com.android.launcher3.util.Preconditions;
import com.android.launcher3.util.Thunk;
import com.qianxun.browser.utils.HomepageUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;

public class LauncherProvider extends ContentProvider {
    private static final String TAG = "LauncherProvider";
    private static final boolean LOGD = false;

    private static final String DOWNGRADE_SCHEMA_FILE = "downgrade_schema.json";

    /**
     * Represents the schema of the database. Changes in scheme need not be backwards compatible.
     */
    public static final int SCHEMA_VERSION = 27;

    public static final String AUTHORITY = FeatureFlags.AUTHORITY;

    static final String EMPTY_DATABASE_CREATED = "EMPTY_DATABASE_CREATED";

    private static final String RESTRICTION_PACKAGE_NAME = "workspace.configuration.package.name";

    private final ChangeListenerWrapper mListenerWrapper = new ChangeListenerWrapper();
    private Handler mListenerHandler;

    protected DatabaseHelper mOpenHelper;

    /**
     * $ adb shell dumpsys activity provider com.android.launcher3
     */
    @Override
    public void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        LauncherAppState appState = LauncherAppState.getInstanceNoCreate();
        if (appState == null || !appState.getModel().isModelLoaded()) {
            return;
        }
        appState.getModel().dumpState("", fd, writer, args);
    }

    @Override
    public boolean onCreate() {
        if (FeatureFlags.IS_DOGFOOD_BUILD) {
            Log.d(TAG, "Launcher process started");
        }
        mListenerHandler = new Handler(mListenerWrapper);

        // The content provider exists for the entire duration of the launcher main process and
        // is the first component to get created.
        MainProcessInitializer.initialize(getContext().getApplicationContext());
        return true;
    }

    /**
     * Sets a provider listener.
     */
    public void setLauncherProviderChangeListener(LauncherProviderChangeListener listener) {
        Preconditions.assertUIThread();
        mListenerWrapper.mListener = listener;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)) {
            return "vnd.android.cursor.dir/" + args.table;
        } else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    /**
     * Overridden in tests
     */
    protected synchronized void createDbIfNotExists() {
        if (mOpenHelper == null) {
            mOpenHelper = new DatabaseHelper(getContext(), mListenerHandler);

            if (RestoreDbTask.isPending(getContext())) {
                if (!RestoreDbTask.performRestore(mOpenHelper)) {
                    mOpenHelper.createEmptyDB(mOpenHelper.getWritableDatabase());
                }
                // Set is pending to false irrespective of the result, so that it doesn't get
                // executed again.
                RestoreDbTask.setPending(getContext(), false);
            }
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        createDbIfNotExists();

        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = qb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);

        return result;
    }

    @Thunk static long dbInsertAndCheck(DatabaseHelper helper,
            SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (values == null) {
            throw new RuntimeException("Error: attempting to insert null values");
        }
        if (!values.containsKey(LauncherSettings.ChangeLogColumns._ID)) {
            throw new RuntimeException("Error: attempting to add item without specifying an id");
        }
        helper.checkId(table, values);
        return db.insert(table, nullColumnHack, values);
    }

    private void reloadLauncherIfExternal() {
        if (Utilities.ATLEAST_MARSHMALLOW && Binder.getCallingPid() != Process.myPid()) {
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app != null) {
                app.getModel().forceReload();
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        createDbIfNotExists();
        SqlArguments args = new SqlArguments(uri);

        // In very limited cases, we support system|signature permission apps to modify the db.
        if (Binder.getCallingPid() != Process.myPid()) {
            if (!initializeExternalAdd(initialValues)) {
                return null;
            }
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        addModifiedTime(initialValues);
        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, initialValues);
        if (rowId < 0) return null;

        uri = ContentUris.withAppendedId(uri, rowId);
        notifyListeners();

        if (Utilities.ATLEAST_MARSHMALLOW) {
            reloadLauncherIfExternal();
        } else {
            // Deprecated behavior to support legacy devices which rely on provider callbacks.
            LauncherAppState app = LauncherAppState.getInstanceNoCreate();
            if (app != null && "true".equals(uri.getQueryParameter("isExternalAdd"))) {
                app.getModel().forceReload();
            }

            String notify = uri.getQueryParameter("notify");
            if (notify == null || "true".equals(notify)) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
        }
        return uri;
    }

    private boolean initializeExternalAdd(ContentValues values) {
        // 1. Ensure that externally added items have a valid item id
        long id = mOpenHelper.generateNewItemId();
        values.put(LauncherSettings.Favorites._ID, id);

        // 2. In the case of an app widget, and if no app widget id is specified, we
        // attempt allocate and bind the widget.
        Integer itemType = values.getAsInteger(LauncherSettings.Favorites.ITEM_TYPE);


        // Add screen id if not present
        long screenId = values.getAsLong(LauncherSettings.Favorites.SCREEN);
        SQLiteStatement stmp = null;
        try {
            stmp = mOpenHelper.getWritableDatabase().compileStatement(
                    "INSERT OR IGNORE INTO workspaceScreens (_id, screenRank) " +
                            "select ?, (ifnull(MAX(screenRank), -1)+1) from workspaceScreens");
            stmp.bindLong(1, screenId);

            ContentValues valuesInserted = new ContentValues();
            valuesInserted.put(LauncherSettings.BaseLauncherColumns._ID, stmp.executeInsert());
            mOpenHelper.checkId(WorkspaceScreens.TABLE_NAME, valuesInserted);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            Utilities.closeSilently(stmp);
        }
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        createDbIfNotExists();
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try (SQLiteTransaction t = new SQLiteTransaction(db)) {
            int numValues = values.length;
            for (int i = 0; i < numValues; i++) {
                addModifiedTime(values[i]);
                if (dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]) < 0) {
                    return 0;
                }
            }
            t.commit();
        }

        notifyListeners();
        reloadLauncherIfExternal();
        return values.length;
    }

    @Override
    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        createDbIfNotExists();
        try (SQLiteTransaction t = new SQLiteTransaction(mOpenHelper.getWritableDatabase())) {
            ContentProviderResult[] result =  super.applyBatch(operations);
            t.commit();
            reloadLauncherIfExternal();
            return result;
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        createDbIfNotExists();
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();


        int count = db.delete(args.table, args.where, args.args);
        if (count > 0) {
            notifyListeners();
            reloadLauncherIfExternal();
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        createDbIfNotExists();
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        addModifiedTime(values);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count > 0) notifyListeners();

        reloadLauncherIfExternal();
        return count;
    }

    @Override
    public Bundle call(String method, final String arg, final Bundle extras) {
        if (Binder.getCallingUid() != Process.myUid()) {
            return null;
        }
        createDbIfNotExists();

        switch (method) {
            case LauncherSettings.Settings.METHOD_CLEAR_EMPTY_DB_FLAG: {
                clearFlagEmptyDbCreated();
                return null;
            }
            case LauncherSettings.Settings.METHOD_WAS_EMPTY_DB_CREATED : {
                Bundle result = new Bundle();
                result.putBoolean(LauncherSettings.Settings.EXTRA_VALUE,
                        Utilities.getPrefs(getContext()).getBoolean(EMPTY_DATABASE_CREATED, false));
                return result;
            }
            case LauncherSettings.Settings.METHOD_DELETE_EMPTY_FOLDERS: {
                Bundle result = new Bundle();
                result.putSerializable(LauncherSettings.Settings.EXTRA_VALUE, deleteEmptyFolders());
                return result;
            }
            case LauncherSettings.Settings.METHOD_NEW_ITEM_ID: {
                Bundle result = new Bundle();
                result.putLong(LauncherSettings.Settings.EXTRA_VALUE, mOpenHelper.generateNewItemId());
                return result;
            }
            case LauncherSettings.Settings.METHOD_NEW_SCREEN_ID: {
                Bundle result = new Bundle();
                result.putLong(LauncherSettings.Settings.EXTRA_VALUE, mOpenHelper.generateNewScreenId());
                return result;
            }
            case LauncherSettings.Settings.METHOD_CREATE_EMPTY_DB: {
                mOpenHelper.createEmptyDB(mOpenHelper.getWritableDatabase());
                return null;
            }
            case LauncherSettings.Settings.METHOD_LOAD_DEFAULT_FAVORITES: {
                loadDefaultFavoritesIfNecessary();
                return null;
            }
        }
        return null;
    }

    /**
     * Deletes any empty folder from the DB.
     * @return Ids of deleted folders.
     */
    private ArrayList<Long> deleteEmptyFolders() {
        ArrayList<Long> folderIds = new ArrayList<>();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        try (SQLiteTransaction t = new SQLiteTransaction(db)) {
            // Select folders whose id do not match any container value.
            String selection = LauncherSettings.Favorites.ITEM_TYPE + " = "
                    + LauncherSettings.Favorites.ITEM_TYPE_FOLDER + " AND "
                    + LauncherSettings.Favorites._ID +  " NOT IN (SELECT " +
                            LauncherSettings.Favorites.CONTAINER + " FROM "
                                + Favorites.TABLE_NAME + ")";
            try (Cursor c = db.query(Favorites.TABLE_NAME,
                    new String[] {LauncherSettings.Favorites._ID},
                    selection, null, null, null, null)) {
                LauncherDbUtils.iterateCursor(c, 0, folderIds);
            }
            if (!folderIds.isEmpty()) {
                db.delete(Favorites.TABLE_NAME, Utilities.createDbSelectionQuery(
                        LauncherSettings.Favorites._ID, folderIds), null);
            }
            t.commit();
        } catch (SQLException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            folderIds.clear();
        }
        return folderIds;
    }

    /**
     * Overridden in tests
     */
    protected void notifyListeners() {
        mListenerHandler.sendEmptyMessage(ChangeListenerWrapper.MSG_LAUNCHER_PROVIDER_CHANGED);
    }

    @Thunk static void addModifiedTime(ContentValues values) {
        values.put(LauncherSettings.ChangeLogColumns.MODIFIED, System.currentTimeMillis());
    }

    private void clearFlagEmptyDbCreated() {
        Utilities.getPrefs(getContext()).edit().remove(EMPTY_DATABASE_CREATED).commit();
    }

    /**
     * Loads the default workspace based on the following priority scheme:
     *   1) From the app restrictions
     *   2) From a package provided by play store
     *   3) From a partner configuration APK, already in the system image
     *   4) The default configuration for the particular device
     */
    synchronized private void loadDefaultFavoritesIfNecessary() {
        SharedPreferences sp = Utilities.getPrefs(getContext());

        if (sp.getBoolean(EMPTY_DATABASE_CREATED, false)) {
            Log.d(TAG, "loading default workspace");
//
//            AppWidgetHost widgetHost = mOpenHelper.newLauncherWidgetHost();
//            AutoInstallsLayout loader = createWorkspaceLoaderFromAppRestriction(widgetHost);
//            if (loader == null) {
//                loader = AutoInstallsLayout.get(getContext(),widgetHost, mOpenHelper);
//            }
//            if (loader == null) {
//                final Partner partner = Partner.get(getContext().getPackageManager());
//                if (partner != null && partner.hasDefaultLayout()) {
//                    final Resources partnerRes = partner.getResources();
//                    int workspaceResId = partnerRes.getIdentifier(Partner.RES_DEFAULT_LAYOUT,
//                            "xml", partner.getPackageName());
//                    if (workspaceResId != 0) {
//                        loader = new DefaultLayoutParser(getContext(), widgetHost,
//                                mOpenHelper, partnerRes, workspaceResId);
//                    }
//                }
//            }
//
//            final boolean usingExternallyProvidedLayout = loader != null;
//            if (loader == null) {
//                loader = getDefaultLayoutParser(widgetHost);
//            }
//
//            // There might be some partially restored DB items, due to buggy restore logic in
//            // previous versions of launcher.
            mOpenHelper.createEmptyDB(mOpenHelper.getWritableDatabase());
            // Populate favorites table with initial favorites
            if ((mOpenHelper.loadFavorites(mOpenHelper.getWritableDatabase()) <= 0)) {
                // Unable to load external layout. Cleanup and load the internal layout.
                mOpenHelper.createEmptyDB(mOpenHelper.getWritableDatabase());
                mOpenHelper.loadFavorites(mOpenHelper.getWritableDatabase());
            }
            clearFlagEmptyDbCreated();
        }
    }

    /**
     * The class is subclassed in tests to create an in-memory db.
     */
    public static class DatabaseHelper extends NoLocaleSQLiteHelper implements LayoutParserCallback {
        private final Context mContext;
        private long mMaxItemId = -1;
        private long mMaxScreenId = -1;

        DatabaseHelper(Context context, Handler widgetHostResetHandler) {
            this(context, widgetHostResetHandler, LauncherFiles.LAUNCHER_DB);
            // Table creation sometimes fails silently, which leads to a crash loop.
            // This way, we will try to create a table every time after crash, so the device
            // would eventually be able to recover.
            if (!tableExists(Favorites.TABLE_NAME) || !tableExists(WorkspaceScreens.TABLE_NAME)) {
                Log.e(TAG, "Tables are missing after onCreate has been called. Trying to recreate");
                // This operation is a no-op if the table already exists.
                addFavoritesTable(getWritableDatabase(), true);
                addWorkspacesTable(getWritableDatabase(), true);
            }

            initIds();
        }

        /**
         * Constructor used in tests and for restore.
         */
        public DatabaseHelper(
                Context context, Handler widgetHostResetHandler, String tableName) {
            super(context, tableName, SCHEMA_VERSION);
            mContext = context;
        }

        protected void initIds() {
            // In the case where neither onCreate nor onUpgrade gets called, we read the maxId from
            // the DB here
            if (mMaxItemId == -1) {
                mMaxItemId = initializeMaxItemId(getWritableDatabase());
            }
            if (mMaxScreenId == -1) {
                mMaxScreenId = initializeMaxScreenId(getWritableDatabase());
            }
        }

        private boolean tableExists(String tableName) {
            Cursor c = getReadableDatabase().query(
                    true, "sqlite_master", new String[] {"tbl_name"},
                    "tbl_name = ?", new String[] {tableName},
                    null, null, null, null, null);
            try {
                return c.getCount() > 0;
            } finally {
                c.close();
            }
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            if (LOGD) Log.d(TAG, "creating new launcher database");

            mMaxItemId = 1;
            mMaxScreenId = 0;

            addFavoritesTable(db, false);
            addWorkspacesTable(db, false);

            // Fresh and clean launcher DB.
            mMaxItemId = initializeMaxItemId(db);
            onEmptyDbCreated();
        }

        /**
         * Overriden in tests.
         */
        protected void onEmptyDbCreated() {

            // Set the flag for empty DB
            Utilities.getPrefs(mContext).edit().putBoolean(EMPTY_DATABASE_CREATED, true).commit();
        }

        private void addFavoritesTable(SQLiteDatabase db, boolean optional) {
            Favorites.addTableToDb(db, optional);
        }

        private void addWorkspacesTable(SQLiteDatabase db, boolean optional) {
            String ifNotExists = optional ? " IF NOT EXISTS " : "";
            db.execSQL("CREATE TABLE " + ifNotExists + WorkspaceScreens.TABLE_NAME + " (" +
                    LauncherSettings.WorkspaceScreens._ID + " INTEGER PRIMARY KEY," +
                    LauncherSettings.WorkspaceScreens.SCREEN_RANK + " INTEGER," +
                    LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                    ");");
        }

        private void removeOrphanedItems(SQLiteDatabase db) {
            // Delete items directly on the workspace who's screen id doesn't exist
            //  "DELETE FROM favorites WHERE screen NOT IN (SELECT _id FROM workspaceScreens)
            //   AND container = -100"
            String removeOrphanedDesktopItems = "DELETE FROM " + Favorites.TABLE_NAME +
                    " WHERE " +
                    LauncherSettings.Favorites.SCREEN + " NOT IN (SELECT " +
                    LauncherSettings.WorkspaceScreens._ID + " FROM " + WorkspaceScreens.TABLE_NAME + ")" +
                    " AND " +
                    LauncherSettings.Favorites.CONTAINER + " = " +
                    LauncherSettings.Favorites.CONTAINER_DESKTOP;
            db.execSQL(removeOrphanedDesktopItems);

            // Delete items contained in folders which no longer exist (after above statement)
            //  "DELETE FROM favorites  WHERE container <> -100 AND container <> -101 AND container
            //   NOT IN (SELECT _id FROM favorites WHERE itemType = 2)"
            String removeOrphanedFolderItems = "DELETE FROM " + Favorites.TABLE_NAME +
                    " WHERE " +
                    LauncherSettings.Favorites.CONTAINER + " <> " +
                    LauncherSettings.Favorites.CONTAINER_DESKTOP +
                    " AND "
                    + LauncherSettings.Favorites.CONTAINER + " <> " +
                    LauncherSettings.Favorites.CONTAINER_HOTSEAT +
                    " AND "
                    + LauncherSettings.Favorites.CONTAINER + " NOT IN (SELECT " +
                    LauncherSettings.Favorites._ID + " FROM " + Favorites.TABLE_NAME +
                    " WHERE " + LauncherSettings.Favorites.ITEM_TYPE + " = " +
                    LauncherSettings.Favorites.ITEM_TYPE_FOLDER + ")";
            db.execSQL(removeOrphanedFolderItems);
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            super.onOpen(db);

            File schemaFile = mContext.getFileStreamPath(DOWNGRADE_SCHEMA_FILE);
            DbDowngradeHelper.updateSchemaFile(schemaFile, SCHEMA_VERSION, mContext,
                    R.raw.downgrade_schema);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (LOGD) Log.d(TAG, "onUpgrade triggered: " + oldVersion);
            switch (oldVersion) {
                // The version cannot be lower that 12, as Launcher3 never supported a lower
                // version of the DB.
                case 12: {
                    // With the new shrink-wrapped and re-orderable workspaces, it makes sense
                    // to persist workspace screens and their relative order.
                    mMaxScreenId = 0;
                    addWorkspacesTable(db, false);
                }
                case 13: {
                    try (SQLiteTransaction t = new SQLiteTransaction(db)) {
                        // Insert new column for holding widget provider name
                        db.execSQL("ALTER TABLE favorites " +
                                "ADD COLUMN appWidgetProvider TEXT;");
                        t.commit();
                    } catch (SQLException ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        // Old version remains, which means we wipe old data
                        break;
                    }
                }
                case 14: {
                    try (SQLiteTransaction t = new SQLiteTransaction(db)) {
                        // Insert new column for holding update timestamp
                        db.execSQL("ALTER TABLE favorites " +
                                "ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                        db.execSQL("ALTER TABLE workspaceScreens " +
                                "ADD COLUMN modified INTEGER NOT NULL DEFAULT 0;");
                        t.commit();
                    } catch (SQLException ex) {
                        Log.e(TAG, ex.getMessage(), ex);
                        // Old version remains, which means we wipe old data
                        break;
                    }
                }
                case 15: {
                    if (!addIntegerColumn(db, Favorites.RESTORED, 0)) {
                        // Old version remains, which means we wipe old data
                        break;
                    }
                }
                case 16: {
                    // No-op
                }
                case 17: {
                    // No-op
                }
                case 18: {
                    // Due to a data loss bug, some users may have items associated with screen ids
                    // which no longer exist. Since this can cause other problems, and since the user
                    // will never see these items anyway, we use database upgrade as an opportunity to
                    // clean things up.
                    removeOrphanedItems(db);
                }
                case 19: {
                    // Add userId column
                    if (!addProfileColumn(db)) {
                        // Old version remains, which means we wipe old data
                        break;
                    }
                }
                case 20:
                    if (!updateFolderItemsRank(db, true)) {
                        break;
                    }
                case 21:
                    // Recreate workspace table with screen id a primary key
                    if (!recreateWorkspaceTable(db)) {
                        break;
                    }
                case 22: {
                    if (!addIntegerColumn(db, Favorites.OPTIONS, 0)) {
                        // Old version remains, which means we wipe old data
                        break;
                    }
                }
                case 23:
                    // No-op
                case 24:
                    // No-op
                case 25:

                case 26:
                    // QSB was moved to the grid. Clear the first row on screen 0.
                    if (FeatureFlags.QSB_ON_FIRST_SCREEN &&
                            !LauncherDbUtils.prepareScreenZeroToHostQsb(mContext, db)) {
                        break;
                    }
                case 27:
                    // DB Upgraded successfully
                    return;
            }

            // DB was not upgraded
            Log.w(TAG, "Destroying all old data.");
            createEmptyDB(db);
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                DbDowngradeHelper.parse(mContext.getFileStreamPath(DOWNGRADE_SCHEMA_FILE))
                        .onDowngrade(db, oldVersion, newVersion);
            } catch (Exception e) {
                Log.d(TAG, "Unable to downgrade from: " + oldVersion + " to " + newVersion +
                        ". Wiping databse.", e);
                createEmptyDB(db);
            }
        }

        /**
         * Clears all the data for a fresh start.
         */
        public void createEmptyDB(SQLiteDatabase db) {
            try (SQLiteTransaction t = new SQLiteTransaction(db)) {
                db.execSQL("DROP TABLE IF EXISTS " + Favorites.TABLE_NAME);
                db.execSQL("DROP TABLE IF EXISTS " + WorkspaceScreens.TABLE_NAME);
                onCreate(db);
                t.commit();
            }
        }

        /**
         * Recreates workspace table and migrates data to the new table.
         */
        public boolean recreateWorkspaceTable(SQLiteDatabase db) {
            try (SQLiteTransaction t = new SQLiteTransaction(db)) {
                final ArrayList<Long> sortedIDs;

                try (Cursor c = db.query(WorkspaceScreens.TABLE_NAME,
                        new String[] {LauncherSettings.WorkspaceScreens._ID},
                        null, null, null, null,
                        LauncherSettings.WorkspaceScreens.SCREEN_RANK)) {
                    // Use LinkedHashSet so that ordering is preserved
                    sortedIDs = new ArrayList<>(
                            LauncherDbUtils.iterateCursor(c, 0, new LinkedHashSet<Long>()));
                }
                db.execSQL("DROP TABLE IF EXISTS " + WorkspaceScreens.TABLE_NAME);
                addWorkspacesTable(db, false);

                // Add all screen ids back
                int total = sortedIDs.size();
                for (int i = 0; i < total; i++) {
                    ContentValues values = new ContentValues();
                    values.put(LauncherSettings.WorkspaceScreens._ID, sortedIDs.get(i));
                    values.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, i);
                    addModifiedTime(values);
                    db.insertOrThrow(WorkspaceScreens.TABLE_NAME, null, values);
                }
                t.commit();
                mMaxScreenId = sortedIDs.isEmpty() ? 0 : Collections.max(sortedIDs);
            } catch (SQLException ex) {
                // Old version remains, which means we wipe old data
                Log.e(TAG, ex.getMessage(), ex);
                return false;
            }
            return true;
        }

        @Thunk boolean updateFolderItemsRank(SQLiteDatabase db, boolean addRankColumn) {
            try (SQLiteTransaction t = new SQLiteTransaction(db)) {
                if (addRankColumn) {
                    // Insert new column for holding rank
                    db.execSQL("ALTER TABLE favorites ADD COLUMN rank INTEGER NOT NULL DEFAULT 0;");
                }

                // Get a map for folder ID to folder width
                Cursor c = db.rawQuery("SELECT container, MAX(cellX) FROM favorites"
                        + " WHERE container IN (SELECT _id FROM favorites WHERE itemType = ?)"
                        + " GROUP BY container;",
                        new String[] {Integer.toString(LauncherSettings.Favorites.ITEM_TYPE_FOLDER)});

                while (c.moveToNext()) {
                    db.execSQL("UPDATE favorites SET rank=cellX+(cellY*?) WHERE "
                            + "container=? AND cellX IS NOT NULL AND cellY IS NOT NULL;",
                            new Object[] {c.getLong(1) + 1, c.getLong(0)});
                }

                c.close();
                t.commit();
            } catch (SQLException ex) {
                // Old version remains, which means we wipe old data
                Log.e(TAG, ex.getMessage(), ex);
                return false;
            }
            return true;
        }

        private boolean addProfileColumn(SQLiteDatabase db) {
            return addIntegerColumn(db, Favorites.PROFILE_ID, 0);
        }

        private boolean addIntegerColumn(SQLiteDatabase db, String columnName, long defaultValue) {
            try (SQLiteTransaction t = new SQLiteTransaction(db)) {
                db.execSQL("ALTER TABLE favorites ADD COLUMN "
                        + columnName + " INTEGER NOT NULL DEFAULT " + defaultValue + ";");
                t.commit();
            } catch (SQLException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                return false;
            }
            return true;
        }

        // Generates a new ID to use for an object in your database. This method should be only
        // called from the main UI thread. As an exception, we do call it when we call the
        // constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        @Override
        public long generateNewItemId() {
            if (mMaxItemId < 0) {
                throw new RuntimeException("Error: max item id was not initialized");
            }
            mMaxItemId += 1;
            return mMaxItemId;
        }

        @Override
        public long insertAndCheck(SQLiteDatabase db, ContentValues values) {
            return dbInsertAndCheck(this, db, Favorites.TABLE_NAME, null, values);
        }

        public void checkId(String table, ContentValues values) {
            long id = values.getAsLong(LauncherSettings.BaseLauncherColumns._ID);
            if (WorkspaceScreens.TABLE_NAME.equals(table)) {
                mMaxScreenId = Math.max(id, mMaxScreenId);
            }  else {
                mMaxItemId = Math.max(id, mMaxItemId);
            }
        }

        private long initializeMaxItemId(SQLiteDatabase db) {
            return getMaxId(db, Favorites.TABLE_NAME);
        }

        // Generates a new ID to use for an workspace screen in your database. This method
        // should be only called from the main UI thread. As an exception, we do call it when we
        // call the constructor from the worker thread; however, this doesn't extend until after the
        // constructor is called, and we only pass a reference to LauncherProvider to LauncherApp
        // after that point
        public long generateNewScreenId() {
            if (mMaxScreenId < 0) {
                throw new RuntimeException("Error: max screen id was not initialized");
            }
            mMaxScreenId += 1;
            return mMaxScreenId;
        }

        private long initializeMaxScreenId(SQLiteDatabase db) {
            return getMaxId(db, WorkspaceScreens.TABLE_NAME);
        }

        @Thunk int loadFavorites(SQLiteDatabase db) {
//            ArrayList<Long> screenIds = new ArrayList<Long>();
//            // TODO: Use multiple loaders with fall-back and transaction.
//            int count = screenIds.size();
//
//            // Add the screens specified by the items above
//            Collections.sort(screenIds);
//            int rank = 0;
//            ContentValues values = new ContentValues();
//            for (Long id : screenIds) {
//                values.clear();
//                values.put(LauncherSettings.WorkspaceScreens._ID, id);
//                values.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, rank);
//                if (dbInsertAndCheck(this, db, WorkspaceScreens.TABLE_NAME, null, values) < 0) {
//                    throw new RuntimeException("Failed initialize screen table"
//                            + "from default layout");
//                }
//                rank++;
//            }

            ContentValues values = new ContentValues();
            values.clear();
            values.put(LauncherSettings.WorkspaceScreens._ID, 0);
            values.put(LauncherSettings.WorkspaceScreens.SCREEN_RANK, 0);
            if (dbInsertAndCheck(this, db, LauncherSettings.WorkspaceScreens.TABLE_NAME, null, values) < 0) {
                throw new RuntimeException("Failed initialize screen table"
                        + "from default layout");
            }

            ArrayList<ItemInfo> itemInfoArrayList = new ArrayList<>(HomepageUtils.initHomeNav());
            for (ItemInfo info : itemInfoArrayList) {
                values.clear();
                values.put(Favorites._ID, generateNewItemId());
                values.put(Favorites.TITLE, info.title.toString());
                values.put(Favorites.CELLX, info.cellX);
                values.put(Favorites.CELLY, info.cellY);
                values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_APPLICATION);
                values.put(Favorites.CONTAINER, Favorites.CONTAINER_DESKTOP);
                if (dbInsertAndCheck(this, db, LauncherSettings.Favorites.TABLE_NAME, null, values) < 0) {
                    throw new RuntimeException("Failed initialize screen table"
                            + "from default layout");
                }
            }

            // Ensure that the max ids are initialized
            mMaxItemId = initializeMaxItemId(db);
            mMaxScreenId = initializeMaxScreenId(db);

            return itemInfoArrayList.size();
        }
    }

    /**
     * @return the max _id in the provided table.
     */
    @Thunk static long getMaxId(SQLiteDatabase db, String table) {
        Cursor c = db.rawQuery("SELECT MAX(_id) FROM " + table, null);
        // get the result
        long id = -1;
        if (c != null && c.moveToNext()) {
            id = c.getLong(0);
        }
        if (c != null) {
            c.close();
        }

        if (id == -1) {
            throw new RuntimeException("Error: could not query max id in " + table);
        }

        return id;
    }

    static class SqlArguments {
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri url, String where, String[] args) {
            if (url.getPathSegments().size() == 1) {
                this.table = url.getPathSegments().get(0);
                this.where = where;
                this.args = args;
            } else if (url.getPathSegments().size() != 2) {
                throw new IllegalArgumentException("Invalid URI: " + url);
            } else if (!TextUtils.isEmpty(where)) {
                throw new UnsupportedOperationException("WHERE clause not supported: " + url);
            } else {
                this.table = url.getPathSegments().get(0);
                this.where = "_id=" + ContentUris.parseId(url);
                this.args = null;
            }
        }

        SqlArguments(Uri url) {
            if (url.getPathSegments().size() == 1) {
                table = url.getPathSegments().get(0);
                where = null;
                args = null;
            } else {
                throw new IllegalArgumentException("Invalid URI: " + url);
            }
        }
    }

    private static class ChangeListenerWrapper implements Handler.Callback {

        private static final int MSG_LAUNCHER_PROVIDER_CHANGED = 1;
        private static final int MSG_APP_WIDGET_HOST_RESET = 2;

        private LauncherProviderChangeListener mListener;

        @Override
        public boolean handleMessage(Message msg) {
            if (mListener != null) {
                switch (msg.what) {
                    case MSG_LAUNCHER_PROVIDER_CHANGED:
                        mListener.onLauncherProviderChanged();
                        break;
                    case MSG_APP_WIDGET_HOST_RESET:
                        mListener.onAppWidgetHostReset();
                        break;
                }
            }
            return true;
        }
    }
}
