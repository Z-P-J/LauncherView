package com.android.launcher3.database.table;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import com.android.launcher3.database.core.AbsTable;
import com.android.launcher3.model.FavoriteItem;

import java.util.List;

public class FavoriteItemTable extends AbsTable<FavoriteItem> {

    // 定义表的名称和列名
    private static final String TABLE_NAME = "favorites";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TAB_ID = "tabId";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_CONTAINER = "container";
    private static final String COLUMN_SCREEN = "screen";
    private static final String COLUMN_CELL_X = "cellX";
    private static final String COLUMN_CELL_Y = "cellY";
    private static final String COLUMN_SPAN_X = "spanX";
    private static final String COLUMN_SPAN_Y = "spanY";
    private static final String COLUMN_ITEM_TYPE = "itemType";
    private static final String COLUMN_MODIFIED = "modified";

    private static final String[] TABLE_COLUMNS = {
            COLUMN_ID, COLUMN_TAB_ID, COLUMN_TITLE, COLUMN_URL, COLUMN_CONTAINER,
            COLUMN_SCREEN, COLUMN_CELL_X, COLUMN_CELL_Y, COLUMN_SPAN_X,
            COLUMN_SPAN_Y, COLUMN_ITEM_TYPE, COLUMN_MODIFIED, COLUMN_MODIFIED
    };

    // 定义创建表的SQL语句
    private static final String CREATE_TABLE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_TAB_ID + " INTEGER, " + COLUMN_TITLE + " TEXT, "
                    + COLUMN_URL + " TEXT, " + COLUMN_CONTAINER + " INTEGER, "
                    + COLUMN_SCREEN + " INTEGER, " + COLUMN_CELL_X + " INTEGER, "
                    + COLUMN_CELL_Y + " INTEGER, " + COLUMN_SPAN_X + " INTEGER NOT NULL ON CONFLICT FAIL, "
                    + COLUMN_SPAN_Y + " INTEGER NOT NULL ON CONFLICT FAIL, "
                    + COLUMN_ITEM_TYPE + " INTEGER, " + COLUMN_MODIFIED + " INTEGER)";

    public FavoriteItemTable(SQLiteOpenHelper helper) {
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
    protected Object getModelId(FavoriteItem model) {
        return model.getId();
    }

    @Override
    protected void updateModelId(FavoriteItem model, Number number) {
        model.setId(number.longValue());
    }

    @Override
    protected FavoriteItem createModel(Cursor cursor) {
        FavoriteItem model = new FavoriteItem();
        model.setId(cursor.getLong(cursor.getColumnIndex(COLUMN_ID)));
        model.setTabId(cursor.getLong(cursor.getColumnIndex(COLUMN_TAB_ID)));
        model.setTitle(cursor.getString(cursor.getColumnIndex(COLUMN_TITLE)));
        model.setUrl(cursor.getString(cursor.getColumnIndex(COLUMN_URL)));
        model.setContainer(cursor.getLong(cursor.getColumnIndex(COLUMN_CONTAINER)));
        model.setScreen(cursor.getLong(cursor.getColumnIndex(COLUMN_SCREEN)));
        model.setCellX(cursor.getInt(cursor.getColumnIndex(COLUMN_CELL_X)));
        model.setCellY(cursor.getInt(cursor.getColumnIndex(COLUMN_CELL_Y)));
        model.setSpanX(cursor.getInt(cursor.getColumnIndex(COLUMN_SPAN_X)));
        model.setSpanY(cursor.getInt(cursor.getColumnIndex(COLUMN_SPAN_Y)));
        model.setItemType(cursor.getInt(cursor.getColumnIndex(COLUMN_ITEM_TYPE)));
        model.setModified(cursor.getLong(cursor.getColumnIndex(COLUMN_MODIFIED)));
        return model;
    }

    @Override
    protected void bindToInsertValues(ContentValues values, FavoriteItem model) {
        values.put(COLUMN_TAB_ID, model.getTabId());
        values.put(COLUMN_TITLE, model.getTitle());
        values.put(COLUMN_URL, model.getUrl());
        values.put(COLUMN_CONTAINER, model.getContainer());
        values.put(COLUMN_SCREEN, model.getScreen());
        values.put(COLUMN_CELL_X, model.getCellX());
        values.put(COLUMN_CELL_Y, model.getCellY());
        values.put(COLUMN_SPAN_X, model.getSpanX());
        values.put(COLUMN_SPAN_Y, model.getSpanY());
        values.put(COLUMN_ITEM_TYPE, model.getItemType());
        values.put(COLUMN_MODIFIED, model.getModified());
    }

    public void deleteByContainer(long container) {
        delete(COLUMN_CONTAINER, container);
    }

    public List<FavoriteItem> queryByUrl(String url) {
        return super.queryAll(COLUMN_URL, url);
    }
}
