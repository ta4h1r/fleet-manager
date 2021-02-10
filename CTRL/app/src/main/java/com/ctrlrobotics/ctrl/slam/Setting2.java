package com.ctrlrobotics.ctrl.slam;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;

public class Setting2 {
    private final static Uri SETTING_CONTENT_URI = Uri.parse("content://com.qihancloud.frontsettings.provider.FrontSettingContentProvider/config");
    
    public static String getString(ContentResolver resolver, String key) {
        String s = "";
        try {
            Cursor c = resolver.query(SETTING_CONTENT_URI, new String[]{"key", "value"}, "key=?", new String[]{key}, null);
            if (c != null) {
                while (c.moveToNext()) {
                    s = c.getString(1);
                }
                c.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }
}
