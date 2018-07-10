package com.jolson.webrtcusinggcm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jolson.webrtcusinggcm.Pojo.SignalData;
import com.jolson.webrtcusinggcm.Pojo.SignalModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignallingClient extends Service{
    boolean isChannelReady = true;
    boolean  isInitiator = false;
    boolean isStarted = false;
    private TurnServer turnServer;
    public static final String SIGNAL_RECIEVED="GCM_SIGNAL_RECIVED";
    private SignalingInterface callback;
    private final IBinder mBinder = new LocalBinder();
    // Initialize a new BroadcastReceiver instance
    private BroadcastReceiver localReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                Object o =new JsonParser().parse(intent.getStringExtra("message"));

                if(o instanceof JsonObject){
                    JSONObject data = new JSONObject( intent.getStringExtra("message"));
                    String type = data.getString("type");
                    Log.d("SignallingClient", "Json Received ::  Type :"+type+" :" + data.toString());

                    if (type.equalsIgnoreCase("got user media")) {
                        callback.onTryToStart();
                    }
                    if (type.equalsIgnoreCase("bye")) {
                        callback.onRemoteHangUp(type);
                    }
                    if (type.equalsIgnoreCase("offer")) {
                        if(callback!=null) {
                            callback.onOfferReceived(data);
                        }else{
                            Intent internetActivityIntent = new Intent(context,InternetCall.class);
                            internetActivityIntent.putExtra("offer",data.toString());
                            startActivity(internetActivityIntent);
                        }
                    } else if (type.equalsIgnoreCase("answer") && isStarted) {
                        callback.onAnswerReceived(data);
                    }
                }else if(o instanceof JsonArray){
                    JSONArray jsonArray = new JSONArray(intent.getStringExtra("message"));
                    if (jsonArray.length()>0 && isStarted) {
                        callback.onIceCandidateReceived(jsonArray);
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };



  /*  public static SignallingClient getInstance() {
        if (instance == null) {
            instance = new SignallingClient();

        }
        return instance;
    }*/


    public void init(InternetCall signalingInterface, TurnServer retrofitInstance) {
        this.callback = signalingInterface;

        isInitiator=true;
        turnServer =retrofitInstance;
    }



    public void emitMessage(SessionDescription message) {
        try {
            Log.d("SignallingClient", "emitMessage() called with: message = [" + message.type.canonicalForm() + "]");
            JSONObject obj = new JSONObject();
            obj.put("type", message.type.canonicalForm());
            obj.put("sdp", message.description);
            Log.d("emitMessage", obj.toString());
           // socket.emit("message", obj);
            turnServer.sendSignal(MyApplication.getInstance().getFirebaseKey(),new SignalModel(MyApplication.getInstance().getBuddyToken(),new SignalData(obj.toString(), MyApplication.getInstance().getPreferenceHelper().getAccessToken()),"")).enqueue(new Callback<SignalModel>() {
                @Override
                public void onResponse(Call<SignalModel> call, Response<SignalModel> response) {
                }

                @Override
                public void onFailure(Call<SignalModel> call, Throwable t) {
                    Log.i("TAG","on onFailure :"+t.getMessage());

                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void emitIceCandidate(ArrayList<IceCandidate> iceCandidate) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        try {
            for (IceCandidate iceCandidate1:iceCandidate) {


            JSONObject object = new JSONObject();
            object.put("type", "candidate");
            object.put("label", iceCandidate1.sdpMLineIndex);
            object.put("id", iceCandidate1.sdpMid);
            object.put("candidate", iceCandidate1.sdp);
            //socket.emit("message", object);
            Log.d("emitIceCandidate :", object.toString());
            jsonArray.put(object);

            }
            jsonObject.put("candidateObject",jsonArray);

            turnServer.sendSignal(MyApplication.getInstance().getFirebaseKey(),new SignalModel(MyApplication.getInstance().getBuddyToken(),new SignalData(jsonArray.toString(), MyApplication.getInstance().getPreferenceHelper().getAccessToken()),"")).enqueue(new Callback<SignalModel>() {
                @Override
                public void onResponse(Call<SignalModel> call, Response<SignalModel> response) {

                }

                @Override
                public void onFailure(Call<SignalModel> call, Throwable t) {
                    Log.i("TAG","on onFailure :"+t.getMessage());

                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    private void close() {
        LocalBroadcastManager.getInstance(SignallingClient.this).unregisterReceiver(localReciver);
    }

    public class LocalBinder extends Binder {
        public SignallingClient getService() {
            // Return this instance of LocalService so clients can call public methods
            return SignallingClient.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        Log.i("TAG","onDestroy Signallingclient service");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    private String ACTION_STOP="android.copy.stop.service";

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
           /* String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Intent activityIntent = new Intent(this, InternetCall.class);
            PendingIntent pendingActivityIntent=PendingIntent.getActivity(this,0,activityIntent,0);
            Intent serviceIntent= new Intent(this,SignallingClient.class);
            serviceIntent.setAction(ACTION_STOP);
            PendingIntent pStopSelf = PendingIntent.getService(this, 0, serviceIntent,PendingIntent.FLAG_CANCEL_CURRENT);
            NotificationCompat.Action action = new NotificationCompat.Action.Builder(0, "STOP", pStopSelf).build();
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Service is running")
                    .setContentText("Click to stop")
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .addAction(action)
                    .setContentIntent(pendingActivityIntent).build();
            startForeground(1, notification);*/
            Intent notificationIntent = new Intent(this, InternetCall.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification =
                    new Notification.Builder(this, CHANNEL_ID)
                            .setContentTitle("Service is running")
                           // .setContentText("Click to stop")
                            .setSmallIcon(android.R.drawable.ic_menu_call)
                            .setContentIntent(pendingIntent)
                            .setTicker("Whats ticker?")
                            .build();

            startForeground(1, notification);
        }

        LocalBroadcastManager.getInstance(SignallingClient.this).registerReceiver(localReciver,
                new IntentFilter(SIGNAL_RECIEVED));

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent!=null&&!TextUtils.isEmpty(intent.getAction())&&ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            Log.d("TAG","stop it self is called");
        }
        return START_STICKY;
    }

    interface SignalingInterface {
        void onRemoteHangUp(String msg);

        void onOfferReceived(JSONObject data);

        void onAnswerReceived(JSONObject data);

        void onIceCandidateReceived(JSONArray data);

        void onTryToStart();
    }
}
