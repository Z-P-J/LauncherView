package com.android.launcher3.database;

public interface ITable {

    String getTableName();

    String getColumnId();

    String[] getTableColumns();

    String getCreateTableSql();

}
