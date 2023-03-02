package com.ark.browser.launcher.model;

import com.ark.browser.launcher.database.HomepageManager;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = HomepageManager.class, name = "workspaceScreens")
public class ScreenItem extends BaseModel {

    @PrimaryKey(autoincrement = true)
    @Column(name = "_id")
    private long id;

    @Column
    private long screenRank;

    @Column
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

    @Override
    public String toString() {
        return "ScreenItem{" +
                "id=" + id +
                ", screenRank=" + screenRank +
                ", modified=" + modified +
                '}';
    }
}
