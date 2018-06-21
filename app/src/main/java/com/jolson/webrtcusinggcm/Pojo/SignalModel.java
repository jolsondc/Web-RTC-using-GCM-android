package com.jolson.webrtcusinggcm.Pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SignalModel {
    @SerializedName("to")
    @Expose
    private String to;
    @SerializedName("data")
    @Expose
    private SignalData signalData;
    @SerializedName("message_id")
    @Expose
    private String message_id;

    public SignalModel(String to, SignalData signalData, String message_id) {
        this.to = to;
        this.signalData = signalData;
        this.message_id = message_id;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public SignalData getSignalData() {
        return signalData;
    }

    public void setSignalData(SignalData signalData) {
        this.signalData = signalData;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }
}
