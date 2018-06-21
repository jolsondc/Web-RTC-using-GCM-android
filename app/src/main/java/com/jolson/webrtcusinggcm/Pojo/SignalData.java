package com.jolson.webrtcusinggcm.Pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SignalData {
    @SerializedName("signal")
    @Expose
    private String signal;
    @SerializedName("myToken")
    @Expose
    private String myToken;
    public SignalData(String signal,String myToken) {

        this.signal = signal;
        this.myToken =myToken;
    }

    public String getMyToken() {
        return myToken;
    }

    public void setMyToken(String myToken) {
        this.myToken = myToken;
    }

    public String getSignal() {
        return signal;
    }

    public void setSignal(String signal) {
        this.signal = signal;
    }
}
