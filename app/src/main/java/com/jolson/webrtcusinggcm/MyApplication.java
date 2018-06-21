package com.jolson.webrtcusinggcm;

import android.app.Application;

import com.jolson.webrtcusinggcm.data.PreferenceHelper;
import com.facebook.stetho.Stetho;


public class MyApplication extends Application {
    static  MyApplication myApplication;
    private PreferenceHelper preferenceHelper;
    private String buddy="GCM token of other phone";
    private String FIREBASE_KEY="key=Legacy server key (You should find this in Firebase project setting)";
    private String ICE_SERVER_HEADER="Basic aksdjkabdkasndklamsdlamsldnlakndansldnklasndklandlanasd";



    @Override
    public void onCreate() {
        super.onCreate();
        myApplication=this;
        Stetho.initializeWithDefaults(this);
    }

    public static MyApplication getInstance(){
        return myApplication;
    }

    public String getICE_SERVER_HEADER() {
        return ICE_SERVER_HEADER;
    }

    public String getFirebaseKey(){
        return  FIREBASE_KEY;
    }

    public PreferenceHelper getPreferenceHelper() {
        if(preferenceHelper==null){
            preferenceHelper =  PreferenceHelper.get(this);
        }
        return preferenceHelper;
    }


    public String getBuddyToken() {
return buddy;
    }

    public void setBuddyString(String s){
        this.buddy=s;
    }
}
