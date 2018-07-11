package com.jolson.webrtcusinggcm;

import android.app.Application;

import com.jolson.webrtcusinggcm.data.PreferenceHelper;
import com.facebook.stetho.Stetho;


public class MyApplication extends Application {
    static  MyApplication myApplication;
    private PreferenceHelper preferenceHelper;
    private String buddy="fDUv3jFfl_0:APA91bGQLMhO9A4yQWiGfEtvNA3xON__W3wZZZ6cRIJvAq2yPO0d5SUxThzz6fOqdG2-D-y9pmdZDY-Jt9ozU4yWxTqk7kVj6AnuMEQPc8BfPaDN8CJd5qwmFlzNFqCmqlrjTCDtUj81ZC9ehsg3jkuHAwYnfRfN-Q";
    private String FIREBASE_KEY="key=AIzaSyAn_jJCYDAyF4Zrj3gTYIE-7ro8cqCCAMc";
    private String ICE_SERVER_HEADER="Basic Sm9sc29uZGM6ZDdkOGZkNWMtNzU0Zi0xMWU4LWJjNjktNGNhOTVkMGY5N2Q1";



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
