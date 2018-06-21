package com.jolson.webrtcusinggcm.Firebase;

import android.util.Log;

import com.jolson.webrtcusinggcm.data.PreferenceHelper;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class FirebaseIDService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseIDService";
    PreferenceHelper helper;

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        helper = PreferenceHelper.get(getApplicationContext());
        helper.putAccessToken(refreshedToken);

    }

}
