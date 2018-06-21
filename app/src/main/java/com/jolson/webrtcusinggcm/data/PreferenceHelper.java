package com.jolson.webrtcusinggcm.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.jolson.webrtcusinggcm.R;

public class PreferenceHelper {
    private static final String ACCESS_TOKEN = "ftoken";
    private static PreferenceHelper sPreferenceHelper;

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    private PreferenceHelper(Context appContext) {
        sp = appContext.getSharedPreferences(appContext.getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        editor = sp.edit();
    }

    public static PreferenceHelper get(Context c) {
        if (sPreferenceHelper == null) {
            sPreferenceHelper = new PreferenceHelper(c);
        }
        return sPreferenceHelper;
    }

    public void putAccessToken(String s){
        editor.putString(ACCESS_TOKEN, s);
        editor.apply();
    }
    public String getAccessToken(){
        return sp.getString(ACCESS_TOKEN, null);
    }


}

