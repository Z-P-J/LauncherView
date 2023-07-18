package com.android.launcher3.database.core;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.ArrayMap;

import com.zpj.utils.ContextUtils;

import java.util.Map;

public class SQLiteHelper extends SQLiteOpenHelper {

    private final Map<Class<? extends ITable>, ITable> mTables = new ArrayMap<>();

    SQLiteHelper(String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(ContextUtils.getApplicationContext(), name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (ITable table : mTables.values()) {
            db.execSQL(table.getCreateTableSql());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (ITable table : mTables.values()) {
            db.execSQL("DROP TABLE IF EXISTS " + table.getTableName());
        }
        onCreate(db);
    }

    public Map<Class<? extends ITable>, ITable> getTables() {
        return mTables;
    }

    public <T extends ITable> T getTable(Class<T> clazz) {
        return clazz.cast(mTables.get(clazz));
    }

    public void registerTable(Class<? extends ITable> clazz, ITable table) {
        mTables.put(clazz, table);
    }


}