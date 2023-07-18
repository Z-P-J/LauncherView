package com.android.launcher3.database;

import com.android.launcher3.database.core.AbsDatabase;
import com.android.launcher3.database.core.SQLiteHelper;
import com.android.launcher3.database.table.FavoriteItemTable;
import com.android.launcher3.database.table.ScreenItemTable;

public class HomepageDatabase extends AbsDatabase {

    public static final String DATABASE_NAME = "launcher.db";
    private static final int DATABASE_VERSION = 1;

    public HomepageDatabase() {
        super(DATABASE_NAME, DATABASE_VERSION);
    }

    @Override
    public void initTable(SQLiteHelper helper) {
        helper.registerTable(ScreenItemTable.class, new ScreenItemTable(helper));
        helper.registerTable(FavoriteItemTable.class, new FavoriteItemTable(helper));
    }


}
