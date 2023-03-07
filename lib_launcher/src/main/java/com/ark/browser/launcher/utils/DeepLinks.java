package com.ark.browser.launcher.utils;

public class DeepLinks {

    public static final String DEEPLINK_MANAGER = "ark://manager";
    public static final String DEEPLINK_COLLECTIONS = "ark://collections";
    public static final String DEEPLINK_BROWSER = "ark://browser";
    public static final String DEEPLINK_DOWNLOADS = "ark://downloads";
    public static final String DEEPLINK_SETTINGS = "ark://settings";

    public static boolean isDeepLink(String url) {
        return url != null && url.startsWith("ark://");
    }

}
