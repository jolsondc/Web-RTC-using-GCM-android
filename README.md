WebRTCusingGCM

*Signalling done with GCM/FCM .since it is API call .will be slow in signalling. but accurate enough to deliver signal(gcm notification) to the peer device.*


```java
public class MyApplication extends Application {
    private String buddy="GCM token of other phone";
    private String FIREBASE_KEY="key=Legacy server key (You should find this in Firebase project setting)";
    private String ICE_SERVER_HEADER="Basic aksdjkabdkasndklamsdlamsldnlakndansldnklasndklandlanasd";
    }
```

- buddy - will be GCM token of second peer always
- FIREBASE_KEY - find this key in firebase project setting. need this for calling fcm api to post gcm notification to 2nd peer and vice versa
- ICE_SERVER_HEADER - register at https://global.xirsys.net for ice servers. and get the header and paste here.


> NOTE: Register your project in firebase to create google-services.json file. after addding in your project. compile. and enjoy!!!


### Contributing to WebRtcUsingGCM
Just make pull request. You are in!