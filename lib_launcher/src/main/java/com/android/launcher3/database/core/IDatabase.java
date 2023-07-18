package com.android.launcher3.database.core;

public interface IDatabase {

    <T extends ITable> T getTable(Class<T> clazz);

    void initTable(SQLiteHelper helper);

}
