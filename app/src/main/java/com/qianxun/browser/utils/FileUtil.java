package com.qianxun.browser.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {

    public static String readAssetFile(Context context, String str) throws IOException {
        String str2 = "";
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(str);
            byte[] bArr = new byte[8092];
            while (true) {
                int read = inputStream.read(bArr);
                if (read <= 0) {
                    break;
                }
                str2 = new StringBuilder(String.valueOf(str2)).append(new String(bArr, 0, read)).toString();
            }
            return str2;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static String readAssetFile(String str) throws IOException {
        return readAssetFile(ContextHelper.getAppContext(), str);
    }

    public static Bitmap getImageFromAssetsFile(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public static void writeToData(String fileName, String content) throws Exception {
        FileOutputStream outStream = ContextHelper.getAppContext().openFileOutput(fileName, Context.MODE_PRIVATE);
        outStream.write(content.getBytes());
        outStream.close();
    }

    public static String readFromData(String fileNmae) throws FileNotFoundException, IOException{
        FileInputStream inStream = null;
        inStream = ContextHelper.getAppContext().openFileInput(fileNmae);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();//输出到内存
        int len=0;
        byte[] buffer = new byte[1024];
        while((len = inStream.read(buffer))!=-1){
            outStream.write(buffer, 0, len);//
        }

        byte[] content_byte = outStream.toByteArray();
        return new String(content_byte);
    }

//    public static List<UserAgent> getUserAgentList() {
//        List<UserAgent> userAgentList = new ArrayList<>();
//        try {
//            JSONArray jsonArray = new JSONArray(FileUtil.readFromData("user_agent.json"));
//            for (int i = 0; i < jsonArray.length(); i++) {
//                JSONObject jsonObject = jsonArray.getJSONObject(i);
//                userAgentList.add(new UserAgent(jsonObject.getString("user_agent_name"), jsonObject.getString("user_agent")));
//            }
//            return userAgentList;
//        } catch (FileNotFoundException e) {
//            try {
//                FileUtil.writeToData("user_agent.json", FileUtil.readAssetFile("user_agent.json"));
//                return getUserAgentList();
//            } catch (Exception e1) {
//                e1.printStackTrace();
//            }
//        } catch (IOException e2) {
//            e2.printStackTrace();
//        } catch (JSONException e3) {
//            e3.printStackTrace();
//        }
//        return null;
//    }
//
//    public static UserAgent getCurrentUserAgent() {
//        List<UserAgent> userAgentList = getUserAgentList();
//        return userAgentList == null ? null : userAgentList.get(SPHelper.getInt("user_agent_selected_position", 0));
//    }
}
