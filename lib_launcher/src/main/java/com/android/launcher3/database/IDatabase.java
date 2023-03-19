package com.android.launcher3.database;

import java.util.List;
import java.util.Map;

public interface IDatabase {

    <T extends ITable> T getTable(Class<T> clazz);

    void initTable(SQLiteHelper helper);

}
