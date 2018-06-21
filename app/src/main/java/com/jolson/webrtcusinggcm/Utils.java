package com.jolson.webrtcusinggcm;


import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class Utils {

    static Utils instance;
    public static final String API_ENDPOINT = "https://global.xirsys.net";

    public static Utils getInstance() {
        if (instance == null) {
            instance = new Utils();
        }
        return instance;
    }


    public  TurnServer getRetrofitInstance() {

        return new Retrofit.Builder()
                .client(provideOkHttpClient())
                .baseUrl(API_ENDPOINT)
                .addConverterFactory(GsonConverterFactory.create())

                .build().create(TurnServer.class);
    }

    static OkHttpClient provideOkHttpClient() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(new StethoInterceptor())
                .build();
        return client;
    }


    public static String getSignalingJson(String signal,String tokenTo){
        return "{ \"data\": {\n" +
                "    \"signal\": \""+signal+"\"\n" +
                "  },\n" +
                "  \"to\" : \""+tokenTo+"\"\n" +
                "}";
    }
}
