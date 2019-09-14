package com.qianxun.browser.utils;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.LauncherSettings;
import com.android.launcher3.R;
import com.android.launcher3.ShortcutInfo;

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
                String nav = FileUtil.readAssetFile(ContextHelper.getAppContext(), DEFAULT_NAV_SITES);
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
                String title = site.getString("title");
                int id = site.getInt("position");
                int cellX = i % 5;
                int cellY = i / 5 + 3;
                if (type == 0) {
                    ShortcutInfo item = new ShortcutInfo();
                    item.setIconBitmap(BitmapFactory.decodeResource(ContextHelper.getAppContext().getResources(), R.mipmap.ic_launcher_home));
                    item.title = title;
                    item.id = id;
                    item.screenId = 0;
                    item.spanX = 1;
                    item.spanY = 1;
                    item.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
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
                        item.setIconBitmap(BitmapFactory.decodeResource(ContextHelper.getAppContext().getResources(), R.mipmap.ic_launcher_home));
                        item.title = folderItem.getString("title");
                        item.id = folderItem.getInt("position");
                        item.screenId = 0;
                        item.spanX = 1;
                        item.spanY = 1;
                        item.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
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

    public static void removeHomeNav() {

    }

//    public static void saveHomeNav(List<SiteSuggestion> siteSuggestionList) {
//        try {
//            JSONObject jsonObject = new JSONObject(FileUtil.readFromData(NAV_SITES));
//            jsonObject.remove("sites");
//            JSONArray jsonArray = new JSONArray();
//            int i = 0;
//            for (SiteSuggestion siteSuggestion : siteSuggestionList) {
////                if (i == siteSuggestionList.size() - 1) break;
//                JSONObject jsonObject1 = new JSONObject();
//                jsonObject1.put("position", i);
//                jsonObject1.put("title", siteSuggestion.title);
//                jsonObject1.put("url", siteSuggestion.url);
//                jsonObject1.put("icon", siteSuggestion.icon);
//                jsonArray.put(jsonObject1);
//                i++;
//            }
//            jsonObject.put("sites", jsonArray);
//            Log.d("saveHomeNav", jsonObject.toString());
//            FileUtil.writeToData(NAV_SITES, jsonObject.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//    }

    public static boolean checkGridItem(String url) {
        String json = getJson().toString();
        return json.replaceAll("\\\\", "").contains(url.substring(0, url.length() - 1));
    }

//    public static boolean addGridItem(SiteSuggestion siteSuggestion) {
//        try {
//            JSONObject jsonObject = new JSONObject(FileUtil.readFromData(NAV_SITES));
//            JSONArray jsonArray = jsonObject.getJSONArray("sites");
//            JSONObject jsonObject1 = new JSONObject();
//            jsonObject1.put("position", jsonArray.length());
//            jsonObject1.put("title", siteSuggestion.title);
//            jsonObject1.put("url", siteSuggestion.url);
//            jsonObject1.put("icon", siteSuggestion.icon);
//            jsonArray.put(jsonObject1);
//            FileUtil.writeToData(NAV_SITES, jsonObject.toString());
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

    public static boolean removeGridItem(String url) {
        Log.d("removeGridItem", "url=" + url);
        try {
            JSONObject jsonObject = new JSONObject(FileUtil.readFromData(NAV_SITES));
            JSONArray jsonArray = jsonObject.getJSONArray("sites");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject1 = jsonArray.getJSONObject(i);
                Log.d("removeGridItem", "try---remove_url=" + jsonObject1.getString("url"));
                if (jsonObject1.getString("url").equals(url)) {
                    Log.d("removeGridItem", "removed_url=" + url);
                    jsonArray.remove(i);
                    break;
                }
            }
            FileUtil.writeToData(NAV_SITES, jsonObject.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

//    public static void Log(String TAG, ItemInfo itemInfo, long id, long screenId, int cellX, int cellY) {
//        Log.e(TAG, "---------------------------------------------");
//        Log.e(TAG, "sBgWorkspaceItems=" + LauncherModel.sBgWorkspaceItems.toString());
//        Log.e("-", "itemInfo=" + itemInfo.toString()
//                + "\nid=" + id + "\nscreenId=" + screenId
//                + "\ncellX=" + cellX + "\ncellY=" + cellY);
//        Log.e(TAG, "---------------------------------------------");
//    }
}
