package com.jolson.webrtcusinggcm;

import com.jolson.webrtcusinggcm.Pojo.SignalModel;
import com.jolson.webrtcusinggcm.Pojo.TurnServerPojo;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;


public interface TurnServer {
    @PUT("https://global.xirsys.net/_turn/webtest/")
    Call<TurnServerPojo> getIceCandidates(@Header("Authorization") String header);

    @POST("https://fcm.googleapis.com/fcm/send")
    Call<SignalModel> sendSignal(@Header("Authorization") String auth,
                                 @Body SignalModel signalData);
}
