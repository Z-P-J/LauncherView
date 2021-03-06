/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.launcher3.model;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.folder.Folder;
import com.android.launcher3.folder.FolderIconPreviewVerifier;
import com.android.launcher3.util.LooperIdleLock;
import com.android.launcher3.util.TraceHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CancellationException;

import static com.android.launcher3.folder.ClippedFolderIconLayoutRule.MAX_NUM_ITEMS_IN_PREVIEW;
import static com.android.launcher3.model.LoaderResults.filterCurrentWorkspaceItems;

/**
 * Runnable for the thread that loads the contents of the launcher:
 * - workspace icons
 * - widgets
 * - all apps icons
 * - deep shortcuts within apps
 */
public class LoaderTask implements Runnable {
    private static final String TAG = "LoaderTask";

    private final LauncherAppState mApp;
    private final BgDataModel mBgDataModel;

    private final LoaderResults mResults;

    private boolean mStopped;

    public LoaderTask(LauncherAppState app, BgDataModel dataModel,
                      LoaderResults results) {
        mApp = app;
        mBgDataModel = dataModel;
        mResults = results;
    }

    protected synchronized void waitForIdle() {
        // Wait until the either we're stopped or the other threads are done.
        // This way we don't start loading all apps until the workspace has settled
        // down.
        LooperIdleLock idleLock = mResults.newIdleLock(this);
        // Just in case mFlushingWorkerThread changes but we aren't woken up,
        // wait no longer than 1sec at a time
        while (!mStopped && idleLock.awaitLocked(1000)) ;
    }

    private synchronized void verifyNotStopped() throws CancellationException {
        if (mStopped) {
            throw new CancellationException("Loader stopped");
        }
    }

    public void run() {
        synchronized (this) {
            // Skip fast if we are already stopped.
            if (mStopped) {
                return;
            }
        }

        TraceHelper.beginSection(TAG);
        Log.d(TAG, "run before");
        try (LauncherModel.LoaderTransaction transaction = mApp.getModel().beginLoader(this)) {
            Log.d(TAG, "run start");
            TraceHelper.partitionSection(TAG, "step 1.1: loading workspace");
            loadWorkspace();

            verifyNotStopped();
            TraceHelper.partitionSection(TAG, "step 1.2: bind workspace workspace");
            mResults.bindWorkspace();

            // Notify the installer packages of packages with active installs on the first screen.
//            TraceHelper.partitionSection(TAG, "step 1.3: send first screen broadcast");
//            sendFirstScreenActiveInstallsBroadcast();

            // Take a break
            TraceHelper.partitionSection(TAG, "step 1 completed, wait for idle");
            waitForIdle();
            verifyNotStopped();

            // second step
//            TraceHelper.partitionSection(TAG, "step 2.1: loading all apps");
//            loadAllApps();

//            TraceHelper.partitionSection(TAG, "step 2.2: Binding all apps");
//            verifyNotStopped();
//            mResults.bindAllApps();

//            verifyNotStopped();
//            TraceHelper.partitionSection(TAG, "step 2.3: Update icon cache");

            // Take a break
//            TraceHelper.partitionSection(TAG, "step 2 completed, wait for idle");
//            waitForIdle();
//            verifyNotStopped();

            // third step
//            TraceHelper.partitionSection(TAG, "step 3.1: loading deep shortcuts");
//            loadDeepShortcuts();

//            verifyNotStopped();
//            TraceHelper.partitionSection(TAG, "step 3.2: bind deep shortcuts");
//            mResults.bindDeepShortcuts();

            // Take a break
//            TraceHelper.partitionSection(TAG, "step 3 completed, wait for idle");
//            waitForIdle();
//            verifyNotStopped();

            // fourth step
//            TraceHelper.partitionSection(TAG, "step 4.1: loading widgets");

//            verifyNotStopped();
//            TraceHelper.partitionSection(TAG, "step 4.2: Binding widgets");
//            mResults.bindWidgets();

            transaction.commit();
        } catch (CancellationException e) {
            // Loader stopped, ignore
            TraceHelper.partitionSection(TAG, "Cancelled");
        }
        TraceHelper.endSection(TAG);
    }

    public synchronized void stopLocked() {
        mStopped = true;
        this.notify();
    }

    private void loadWorkspace() {
        final Context context = mApp.getContext();
        final ContentResolver contentResolver = context.getContentResolver();

        final Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher_home);

        boolean clearDb = false;
//        try {
//            boolean b = ImportDataTask.performImportIfPossible(context);
//            Log.d(TAG, "loadWorkspace b=" + b);
//        } catch (Exception e) {
//            // Migration failed. Clear workspace.
//            e.printStackTrace();
//            clearDb = true;
//        }

        if (!clearDb && GridSizeMigrationTask.ENABLED &&
                !GridSizeMigrationTask.migrateGridIfNeeded(context)) {
            // Migration failed. Clear workspace.
            clearDb = true;
        }

        if (clearDb) {
            Log.d(TAG, "loadWorkspace: resetting launcher database");
            LauncherSettings.Settings.call(contentResolver,
                    LauncherSettings.Settings.METHOD_CREATE_EMPTY_DB);
        }

        Log.d(TAG, "loadWorkspace: loading default favorites");
        LauncherSettings.Settings.call(contentResolver,
                LauncherSettings.Settings.METHOD_LOAD_DEFAULT_FAVORITES);

        synchronized (mBgDataModel) {
            mBgDataModel.clear();
            mBgDataModel.workspaceScreens.addAll(LauncherModel.loadWorkspaceScreensDb(context));

            final LoaderCursor c = new LoaderCursor(contentResolver.query(
                    LauncherSettings.Favorites.CONTENT_URI, null, null, null, null), mApp);

            try {
                final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
//                final int appWidgetIdIndex = c.getColumnIndexOrThrow(
//                        LauncherSettings.Favorites.APPWIDGET_ID);
//                final int appWidgetProviderIndex = c.getColumnIndexOrThrow(
//                        LauncherSettings.Favorites.APPWIDGET_PROVIDER);
                final int spanXIndex = c.getColumnIndexOrThrow
                        (LauncherSettings.Favorites.SPANX);
                final int spanYIndex = c.getColumnIndexOrThrow(
                        LauncherSettings.Favorites.SPANY);
                final int rankIndex = c.getColumnIndexOrThrow(
                        LauncherSettings.Favorites.RANK);
                final int optionsIndex = c.getColumnIndexOrThrow(
                        LauncherSettings.Favorites.OPTIONS);

//                final LongSparseArray<UserHandle> allUsers = c.allUsers;
//                final LongSparseArray<Boolean> quietMode = new LongSparseArray<>();
//                final LongSparseArray<Boolean> unlockedUsers = new LongSparseArray<>();
//                for (UserHandle user : mUserManager.getUserProfiles()) {
//                    long serialNo = mUserManager.getSerialNumberForUser(user);
//                    allUsers.put(serialNo, user);
//                    quietMode.put(serialNo, mUserManager.isQuietModeEnabled(user));
//
//                    boolean userUnlocked = mUserManager.isUserUnlocked(user);
//
//                    unlockedUsers.put(serialNo, userUnlocked);
//                }

                ShortcutInfo info;

                while (!mStopped && c.moveToNext()) {
                    switch (c.itemType) {
                        case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            info = new ShortcutInfo();
                            c.applyCommonProperties(info);
                            info.setIconBitmap(icon);
                            info.title = c.getString(titleIndex);
                            info.rank = c.getInt(rankIndex);

                            c.checkAndAddItem(info, mBgDataModel);
                            break;
                        case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                            FolderInfo folderInfo = mBgDataModel.findOrMakeFolder(c.id);
                            c.applyCommonProperties(folderInfo);
                            folderInfo.title = c.getString(c.titleIndex);
                            if (TextUtils.isEmpty(folderInfo.title)) {
                                folderInfo.title = "文件夹";
                            }
                            folderInfo.options = c.getInt(optionsIndex);
                            c.markRestored();
                            c.checkAndAddItem(folderInfo, mBgDataModel);
                            break;
                        default:
                            break;
                    }
                }
            } finally {
                Utilities.closeSilently(c);
            }

            // Break early if we've stopped loading
            if (mStopped) {
                mBgDataModel.clear();
                return;
            }

            // Remove dead items
            if (c.commitDeleted()) {
                // Remove any empty folder
                ArrayList<Long> deletedFolderIds = (ArrayList<Long>) LauncherSettings.Settings
                        .call(contentResolver,
                                LauncherSettings.Settings.METHOD_DELETE_EMPTY_FOLDERS)
                        .getSerializable(LauncherSettings.Settings.EXTRA_VALUE);
                for (long folderId : deletedFolderIds) {
                    mBgDataModel.workspaceItems.remove(mBgDataModel.folders.get(folderId));
                    mBgDataModel.folders.remove(folderId);
                    mBgDataModel.itemsIdMap.remove(folderId);
                }
            }

            FolderIconPreviewVerifier verifier =
                    new FolderIconPreviewVerifier(mApp.getInvariantDeviceProfile());
            // Sort the folder items and make sure all items in the preview are high resolution.
            for (FolderInfo folder : mBgDataModel.folders) {
                Collections.sort(folder.contents, Folder.ITEM_POS_COMPARATOR);
                verifier.setFolderInfo(folder);

                int numItemsInPreview = 0;
                for (ShortcutInfo info : folder.contents) {
                    if (info.usingLowResIcon
                            && info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                            && verifier.isItemInPreview(info.rank)) {
                        numItemsInPreview++;
                    }

                    if (numItemsInPreview >= MAX_NUM_ITEMS_IN_PREVIEW) {
                        break;
                    }
                }
            }

            c.commitRestoredItems();

            // Remove any empty screens
            ArrayList<Long> unusedScreens = new ArrayList<>(mBgDataModel.workspaceScreens);
            for (ItemInfo item : mBgDataModel.itemsIdMap) {
                long screenId = item.screenId;
                if (item.container == LauncherSettings.Favorites.CONTAINER_DESKTOP &&
                        unusedScreens.contains(screenId)) {
                    unusedScreens.remove(screenId);
                }
            }

            // If there are any empty screens remove them, and update.
            if (unusedScreens.size() != 0) {
                mBgDataModel.workspaceScreens.removeAll(unusedScreens);
                LauncherModel.updateWorkspaceScreenOrder(context, mBgDataModel.workspaceScreens);
            }
        }
    }

//    private void loadAllApps() {
//        final List<UserHandle> profiles = mUserManager.getUserProfiles();
//
//        // Clear the list of apps
//        mBgAllAppsList.clear();
//        for (UserHandle user : profiles) {
//            // Query for the set of apps
//            final List<LauncherActivityInfo> apps = mLauncherApps.getActivityList(null, user);
//            // Fail if we don't have any apps
//            // TODO: Fix this. Only fail for the current user.
//            if (apps == null || apps.isEmpty()) {
//                return;
//            }
//            boolean quietMode = mUserManager.isQuietModeEnabled(user);
//            // Create the ApplicationInfos
//            for (int i = 0; i < apps.size(); i++) {
//                LauncherActivityInfo app = apps.get(i);
//                // This builds the icon bitmaps.
//                mBgAllAppsList.add(new AppInfo(app, user, quietMode), app);
//            }
//        }
//
//        if (FeatureFlags.LAUNCHER3_PROMISE_APPS_IN_ALL_APPS) {
//            // get all active sessions and add them to the all apps list
//            for (PackageInstaller.SessionInfo info :
//                    mPackageInstaller.getAllVerifiedSessions()) {
//                mBgAllAppsList.addPromiseApp(mApp.getContext(),
//                        PackageInstallerCompat.PackageInstallInfo.fromInstallingState(info));
//            }
//        }
//
//        mBgAllAppsList.added = new ArrayList<>();
//    }

}
