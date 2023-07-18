package com.android.launcher3.model;

import com.android.launcher3.database.SQLite;
import com.android.launcher3.database.table.ScreenItemTable;

public class ScreenItem {

    private long id;

    private long screenRank;

    private long modified;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getScreenRank() {
        return screenRank;
    }

    public void setScreenRank(long screenRank) {
        this.screenRank = screenRank;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public void delete() {
        SQLite.with(ScreenItemTable.class).delete(getId());
    }

    public boolean update() {
        return SQLite.with(ScreenItemTable.class).update(this);
    }

    public void insert() {
        SQLite.with(ScreenItemTable.class).insert(this);
    }

    @Override
    public String toString() {
        return "ScreenItem{" +
                "id=" + id +
                ", screenRank=" + screenRank +
                ", modified=" + modified +
                '}';
    }
}
