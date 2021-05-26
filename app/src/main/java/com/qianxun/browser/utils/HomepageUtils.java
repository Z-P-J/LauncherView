package com.qianxun.browser.utils;

import android.graphics.BitmapFactory;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;
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
                String nav = FileUtils.readAssetFile(ContextHelper.getAppContext(), DEFAULT_NAV_SITES);
                FileUtil.writeToData(NAV_SITES, nav);
                jsonObject = new JSONObject(nav);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return jsonObject;
    }

    public static List<ItemInfo> initHomeNav() {
        List<ItemInfo> gridList = new ArrayList<>();
        try {
            JSONArray jsonArray = getJson().getJSONArray("sites");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject site = jsonArray.getJSONObject(i);
                int type = site.getInt("type");
                String url = site.getString("url");
                String title = site.getString("title");
                int id = site.getInt("position") + 1;
                int cellX = i % 5;
                int cellY = i / 5 + 3;
                if (type == 0) {
                    ShortcutInfo item = new ShortcutInfo();
                    item.setIconBitmap(BitmapFactory.decodeResource(ContextHelper.getAppContext().getResources(), R.drawable.ic_launcher_home));
                    item.title = title;
                    item.id = id;
                    item.url = url;
                    item.screenId = 0;
                    item.spanX = 1;
                    item.spanY = 1;
                    item.itemType = ItemInfo.ITEM_TYPE_APPLICATION;
                    item.container = -100;
                    item.cellX = cellX;
                    item.cellY = cellY;
                    gridList.add(item);
                } else {
                    FolderInfo folder = new FolderInfo();
                    folder.cellX = cellX;
                    folder.cellY = cellY;
                    folder.screenId = 0;
                    folder.container = -100;
                    folder.spanX = 1;
                    folder.spanY = 1;
                    folder.id = id;
                    folder.title = title;
                    JSONArray folderItems = site.getJSONArray("sites");
                    for (int j = 0; j < folderItems.length(); j++) {
                        JSONObject folderItem = folderItems.getJSONObject(j);
                        ShortcutInfo item = new ShortcutInfo();
                        item.setIconBitmap(BitmapFactory.decodeResource(ContextHelper.getAppContext().getResources(), R.drawable.ic_launcher_home));
                        item.title = folderItem.getString("title");
                        item.id = folderItem.getInt("position");
                        item.url = folderItem.getString("url");
                        item.screenId = 0;
                        item.spanX = 1;
                        item.spanY = 1;
                        item.itemType = ItemInfo.ITEM_TYPE_APPLICATION;
                        item.container = -100;
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
