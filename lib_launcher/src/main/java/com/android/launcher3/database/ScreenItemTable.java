package com.android.launcher3.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.launcher3.model.ScreenItem;

public class ScreenItemTable extends AbsTable<ScreenItem> {

    private static final String TABLE_NAME = "workspaceScreens";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_SCREEN_RANK = "screenRank";
    private static final String COLUMN_MODIFIED = "modified";

    private static final String[] TABLE_COLUMNS = {COLUMN_ID, COLUMN_SCREEN_RANK, COLUMN_MODIFIED};

    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS workspaceScreens(_id INTEGER PRIMARY KEY AUTOINCREMENT, screenRank INTEGER, modified INTEGER)";

    public ScreenItemTable(SQLiteOpenHelper helper) {
        super(helper);
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String getColumnId() {
        return COLUMN_ID;
    }

    @Override
    public String[] getTableColumns() {
        return TABLE_COLUMNS;
    }

    @Override
    public String getCreateTableSql() {
        return CREATE_TABLE;
    }

    @Override
    protected Object getModelId(ScreenItem model) {
        return model.getId();
    }

    @Override
    protected void updateModelId(ScreenItem model, Number number) {
        model.setId(number.longValue());
    }

    @Override
    protected ScreenItem createModel(Cursor cursor) {
        // 创建一个ScreenItem对象，并从Cursor对象中获取相应的属性值
        ScreenItem screenItem = new ScreenItem();
        screenItem.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        screenItem.setScreenRank(cursor.getLong(cursor.getColumnIndex(COLUMN_SCREEN_RANK)));
        screenItem.setModified(cursor.getLong(cursor.getColumnIndex(COLUMN_MODIFIED)));
        return screenItem;
    }

    @Override
    public void bindToInsertValues(ContentValues values, ScreenItem item) {
        values.put(COLUMN_SCREEN_RANK, item.getScreenRank());
        values.put(COLUMN_MODIFIED, item.getModified());
    }

}
