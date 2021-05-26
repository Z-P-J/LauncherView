package com.qianxun.browser.model;

import com.android.launcher3.ItemInfo;
import com.qianxun.browser.database.HomepageManager;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.NotNull;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = HomepageManager.class, name = "favorites")
public class FavoriteItem extends BaseModel {

    @PrimaryKey(autoincrement = true)
    @Column(name = "_id")
    private long id;
    @Column
    private String title;
    @Column
    private String url;
    @Column
    private long container;
    @Column
    private long screen;
    @Column
    private int cellX;
    @Column
    private int cellY;

    @NotNull
    @Column(defaultValue = "1")
    private int spanX = 1;

    @NotNull
    @Column(defaultValue = "1")
    private int spanY = 1;

    @Column
    private int itemType;
    @Column()
    private long modified;


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getContainer() {
        return container;
    }

    public void setContainer(long container) {
        this.container = container;
    }

    public long getScreen() {
        return screen;
    }

    public void setScreen(long screen) {
        this.screen = screen;
    }

    public int getCellX() {
        return cellX;
    }

    public void setCellX(int cellX) {
        this.cellX = cellX;
    }

    public int getCellY() {
        return cellY;
    }

    public void setCellY(int cellY) {
        this.cellY = cellY;
    }

    public int getSpanX() {
        return spanX;
    }

    public void setSpanX(int spanX) {
        this.spanX = spanX;
    }

    public int getSpanY() {
        return spanY;
    }

    public void setSpanY(int spanY) {
        this.spanY = spanY;
    }

    public int getItemType() {
        return itemType;
    }

    public void setItemType(int itemType) {
        this.itemType = itemType;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public static FavoriteItem from(ItemInfo info) {
        FavoriteItem favoriteItem = new FavoriteItem();
        if (info.id != ItemInfo.NO_ID) {
            favoriteItem.id = info.id;
        }
        favoriteItem.cellX = info.cellX;
        favoriteItem.cellY = info.cellY;
        favoriteItem.title = info.title.toString();
        favoriteItem.url = info.url;
        favoriteItem.container = info.container;
        favoriteItem.screen = info.screenId;
        favoriteItem.spanX = info.spanX;
        favoriteItem.spanY = info.spanY;
        favoriteItem.itemType = info.itemType;
        return favoriteItem;
    }

    public void applyCommonProperties(ItemInfo info) {
        info.id = id;
        info.cellX = cellX;
        info.cellY = cellY;
        info.title = title;
        info.url = url;
        info.container = container;
        info.screenId = screen;
        if (spanX < 1) {
            info.spanX = 1;
        } else {
            info.spanX = spanX;
        }
        if (spanY < 1) {
            info.spanY = 1;
        } else {
            info.spanY = spanY;
        }
        info.itemType = itemType;
    }

    @Override
    public String toString() {
        return "FavoriteItem{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", container=" + container +
                ", screen=" + screen +
                ", cellX=" + cellX +
                ", cellY=" + cellY +
                ", spanX=" + spanX +
                ", spanY=" + spanY +
                ", itemType=" + itemType +
                ", modified=" + modified +
                '}';
    }
}
