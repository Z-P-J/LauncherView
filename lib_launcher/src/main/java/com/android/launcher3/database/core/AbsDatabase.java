package com.android.launcher3.database.core;

import android.database.sqlite.SQLiteDatabase;

public abstract class AbsDatabase implements IDatabase {

    protected final SQLiteHelper mHelper;

    public AbsDatabase(String name, int version) {
        this(name, null, version);
    }

    public AbsDatabase(String name, SQLiteDatabase.CursorFactory factory, int version) {
        mHelper = new SQLiteHelper(name, factory, version);
        initTable(mHelper);
    }

    @Override
    public <T extends ITable> T getTable(Class<T> clazz) {
        return mHelper.getTable(clazz);
    }

    public void registerTable(Class<? extends ITable> clazz, ITable table) {
        mHelper.registerTable(clazz, table);
    }

}
