package com.ark.browser.launcher.demo.utils;

import android.graphics.BitmapFactory;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.util.DeepLinks;
import com.android.launcher3.ShortcutInfo;
import com.ark.browser.launcher.demo.R;
import com.zpj.utils.ContextUtils;
import com.zpj.utils.FileUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomepageUtils {

    private static final String DEFAULT_NAV_SITES = "default_nav_sites.json";
    private static final String NAV_SITES = "nav_sites.json";

    private static JSONObject getJson() {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(FileUtil.readFromData(NAV_SITES));
        } catch (Exception e) {
            try {
                String nav = FileUtils.readAssetFile(ContextUtils.getApplicationContext(), DEFAULT_NAV_SITES);
                FileUtil.writeToData(NAV_SITES, nav);
                jsonObject = new JSONObject(nav);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return jsonObject;
    }

    private static ShortcutInfo createHotseatItem(int pos, String title, String url) {
        ShortcutInfo item = new ShortcutInfo();
        item.setIconBitmap(null);
        item.title = title;
        item.id = pos + 1;
        item.url = url;
        item.screenId = pos;
        item.spanX = 1;
        item.spanY = 1;
        item.itemType = ItemInfo.ITEM_TYPE_APPLICATION;
        item.container = ItemInfo.CONTAINER_HOTSEAT;
        item.cellX = pos;
        item.cellY = 0;
        return item;
    }

    public static List<ItemInfo> initHomeNav() {
        List<ItemInfo> gridList = new ArrayList<>();

        gridList.add(createHotseatItem(0, "浏览器管家", DeepLinks.DEEPLINK_MANAGER));
        gridList.add(createHotseatItem(1, "书签收藏",  DeepLinks.DEEPLINK_COLLECTIONS));
        gridList.add(createHotseatItem(2, "菜单",  DeepLinks.DEEPLINK_BROWSER));
        gridList.add(createHotseatItem(3, "下载管理",  DeepLinks.DEEPLINK_DOWNLOADS));
        gridList.add(createHotseatItem(4, "设置",  DeepLinks.DEEPLINK_SETTINGS));

        int colCount = gridList.size();

        try {
            JSONArray jsonArray = getJson().getJSONArray("sites");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject site = jsonArray.getJSONObject(i);
                int type = site.getInt("type");
                String url = site.getString("url");
                String title = site.getString("title");
                int id = site.getInt("position") + 1 + colCount;
                int cellX = i % 5;
                int cellY = i / 5 + 3;
                if (type == ItemInfo.ITEM_TYPE_APPLICATION) {
                    ShortcutInfo item = new ShortcutInfo();
                    item.setIconBitmap(BitmapFactory.decodeResource(ContextUtils.getApplicationContext().getResources(), R.drawable.ic_launcher_home));
                    item.title = title;
                    item.id = id;
                    item.url = url;
                    item.screenId = 0;
                    item.spanX = 1;
                    item.spanY = 1;
                    item.itemType = ItemInfo.ITEM_TYPE_APPLICATION;
                    item.container = ItemInfo.CONTAINER_DESKTOP;
                    item.cellX = cellX;
                    item.cellY = cellY;
                    gridList.add(item);
                } else {
                    FolderInfo folder = new FolderInfo();
                    folder.cellX = cellX;
                    folder.cellY = cellY;
                    folder.screenId = 0;
                    folder.container = ItemInfo.CONTAINER_DESKTOP;
                    folder.spanX = 1;
                    folder.spanY = 1;
                    folder.id = id;
                    folder.title = title;
                    JSONArray folderItems = site.getJSONArray("sites");
                    for (int j = 0; j < folderItems.length(); j++) {
                        JSONObject folderItem = folderItems.getJSONObject(j);
                        ShortcutInfo item = new ShortcutInfo();
                        item.setIconBitmap(BitmapFactory.decodeResource(ContextUtils.getApplicationContext().getResources(), R.drawable.ic_launcher_home));
                        item.title = folderItem.getString("title");
                        item.id = folderItem.getInt("position");
                        item.url = folderItem.getString("url");
                        item.screenId = 0;
                        item.spanX = 1;
                        item.spanY = 1;
                        item.itemType = ItemInfo.ITEM_TYPE_APPLICATION;
                        item.container = ItemInfo.CONTAINER_DESKTOP;
                        item.cellX = j % 4;
                        item.cellY = j / 4;
                        folder.add(item, false);
                    }
                    gridList.add(folder);
                }

//                item.setOrdinal(site.getInt("position"));

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
//        gridList.add(new DragItem());
        return gridList;
    }

}
