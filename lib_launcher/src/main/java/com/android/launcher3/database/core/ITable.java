package com.android.launcher3.database.core;

public interface ITable {

    String getTableName();

    String getColumnId();

    String[] getTableColumns();

    String getCreateTableSql();

}
