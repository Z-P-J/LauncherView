package com.android.launcher3.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.zpj.utils.Callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class AbsTable<T> implements ITable {

    protected final SQLiteOpenHelper mHelper;

    public AbsTable(SQLiteOpenHelper helper) {
        mHelper = helper;
    }

    public void runTransaction(Callback<SQLiteDatabase> callback) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            callback.onCallback(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void insert(T model) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        insert(db, model);
        db.close();
    }

    public void insertAll(List<T> models) {
        runTransaction(db -> {
            for (T model : models) {
                insert(db, model);
            }
        });
    }

    private long insert(SQLiteDatabase db, T model) {
        ContentValues values = new ContentValues();
        bindToInsertValues(values, model);
        long id = db.insert(getTableName(), null, values);
        Log.e("HomepageManager", "insert=" + id + " model=" + model);
        if (id > -1) {
            updateModelId(model, id);
        }
        return id;
    }

    private int update(SQLiteDatabase db, T model) {
        ContentValues contentValues = new ContentValues();
        bindToContentValues(contentValues, model);
        return db.update(getTableName(), contentValues,
                getColumnId() + "=?",
                new String[]{String.valueOf(getModelId(model))});
    }

    public boolean update(T model) {
        try (SQLiteDatabase db = mHelper.getWritableDatabase()) {
            int result = update(db, model);
            Log.e("HomepageManager", "update result=" + result + " model=" + model);
            return result > 0;
        }
    }

    public void updateAll(List<T> models) {
        runTransaction(db -> {
            for (T model : models) {
                update(db, model);
            }
        });
    }

    public void delete() {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        delete(db, null, null);
        db.close();
    }

    public void delete(Object id) {
        delete(getColumnId(), id);
    }

    public void delete(String column, Object target) {
        try (SQLiteDatabase db = mHelper.getWritableDatabase();) {
            delete(db, column + "=?", new String[]{String.valueOf(target)});
        }
    }

    private void delete(SQLiteDatabase db, String whereClause, String[] whereArgs) {
        if (TextUtils.isEmpty(whereClause)) {
            whereArgs = null;
        }
        int result = db.delete(getTableName(), whereClause, whereArgs);
        Log.e("HomepageManager", "delete table=" + getTableName() + " result=" + result
                + " whereClause=" + whereClause
                + " whereArgs=" + Arrays.toString(whereArgs));
    }

    public T queryById(Object id) {
        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = null;
        T model = null;

        try {
            cursor = db.query(getTableName(),
                    getTableColumns(),
                    getColumnId() + "=?",
                    new String[]{String.valueOf(id)},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                model = createModel(cursor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return model;
    }

    public List<T> queryAll(String column, Object target) {
        List<T> list = new ArrayList<>();
        try (SQLiteDatabase db = mHelper.getReadableDatabase();
             Cursor cursor = db.query(getTableName(), getTableColumns(),
                     column + "=?",
                     new String[]{String.valueOf(target)},
                     null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(createModel(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<T> queryAll() {
        return queryAll(null);
    }

    public List<T> queryAll(String orderBy) {
        List<T> list = new ArrayList<>();
        try (SQLiteDatabase db = mHelper.getReadableDatabase();
             Cursor cursor = db.query(getTableName(), getTableColumns(),
                     null, null, null, null, orderBy)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(createModel(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    protected abstract Object getModelId(T model);

    protected abstract void updateModelId(T model, Number number);

    protected abstract T createModel(Cursor cursor);

    protected abstract void bindToInsertValues(ContentValues values, T model);

    protected void bindToContentValues(ContentValues values, T model) {
        putObject(values, getColumnId(), getModelId(model));
        bindToInsertValues(values, model);
    }

    public static void putObject(ContentValues values, String key, Object value) {
        if (value == null) {
            values.putNull(key);
        } else if (value instanceof String) {
            values.put(key, (String) value);
        } else if (value instanceof Byte) {
            values.put(key, (Byte) value);
        } else if (value instanceof Short) {
            values.put(key, (Short) value);
        } else if (value instanceof Integer) {
            values.put(key, (Integer) value);
        } else if (value instanceof Long) {
            values.put(key, (Long) value);
        } else if (value instanceof Float) {
            values.put(key, (Float) value);
        } else if (value instanceof Double) {
            values.put(key, (Double) value);
        } else if (value instanceof Boolean) {
            values.put(key, (Boolean) value);
        } else if (value instanceof byte[]) {
            values.put(key, (byte[]) value);
        } else {
            throw new IllegalArgumentException("Unsupported type " + value.getClass());
        }
    }

}
