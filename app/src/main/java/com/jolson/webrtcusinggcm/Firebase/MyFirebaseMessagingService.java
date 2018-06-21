package com.jolson.webrtcusinggcm.Firebase;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.jolson.webrtcusinggcm.MyApplication;
import com.jolson.webrtcusinggcm.SignallingClient;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCM Service";
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO: Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.


            Intent intent = new Intent(SignallingClient.SIGNAL_RECIEVED);
            // Put the random number to intent to broadcast it
            intent.putExtra("message",remoteMessage.getData().get("signal"));
            MyApplication.getInstance().setBuddyString(remoteMessage.getData().get("myToken"));

            // Send the broadcast
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);


    }
}