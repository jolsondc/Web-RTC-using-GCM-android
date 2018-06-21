package com.jolson.webrtcusinggcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.jolson.webrtcusinggcm.Pojo.SignalData;
import com.jolson.webrtcusinggcm.Pojo.SignalModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

import io.socket.client.Socket;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignallingClient {
    private static SignallingClient instance;
    private String roomName = null;
    private Socket socket;
    boolean isChannelReady = true;
    boolean isInitiator = false;
    boolean isStarted = false;
    private TurnServer turnServer;
    public static final String SIGNAL_RECIEVED="GCM_SIGNAL_RECIVED";
    private SignalingInterface callback;
    private Context context;
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
                        callback.onOfferReceived(data);
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



    public static SignallingClient getInstance() {
        if (instance == null) {
            instance = new SignallingClient();

        }
        return instance;
    }


    public void init(InternetCall signalingInterface, TurnServer retrofitInstance) {
        this.callback = signalingInterface;
        LocalBroadcastManager.getInstance(signalingInterface).registerReceiver(
                localReciver,
                new IntentFilter(SIGNAL_RECIEVED));
        context=signalingInterface;
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
    
    public void close() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(localReciver);
    }


    interface SignalingInterface {
        void onRemoteHangUp(String msg);

        void onOfferReceived(JSONObject data);

        void onAnswerReceived(JSONObject data);

        void onIceCandidateReceived(JSONArray data);

        void onTryToStart();
    }
}
