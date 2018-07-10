package com.jolson.webrtcusinggcm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.jolson.webrtcusinggcm.Pojo.IceServer;
import com.jolson.webrtcusinggcm.Pojo.TurnServerPojo;
import com.jolson.webrtcusinggcm.WebRtc.CustomPeerConnectionObserver;
import com.jolson.webrtcusinggcm.WebRtc.CustomSdpObserver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class InternetCall extends AppCompatActivity implements View.OnClickListener, SignallingClient.SignalingInterface  {

   SurfaceViewRenderer localVideoView;
    SurfaceViewRenderer remoteVideoView;



    private static final String TAG = "InternetCall";

    PeerConnectionFactory peerConnectionFactory;
    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    VideoRenderer localRenderer;
    VideoRenderer remoteRenderer;

    PeerConnection localPeer;
    List<IceServer> iceServers;
    EglBase rootEglBase;
    Intent serviceIntent;//= new Intent(this, SignallingClient.class);

    boolean gotUserMedia,isCAlled=false,isInitiated=false;
    List<PeerConnection.IceServer> peerIceServers = new ArrayList<>();

    private boolean mBound = false;
    private SignallingClient mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SignallingClient.LocalBinder binder = (SignallingClient.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            Log.i("TAG","onServiceConnected");
                mService.init(InternetCall.this, Utils.getInstance().getRetrofitInstance());

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            Log.i("TAG","onServiceDisconnected");
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        serviceIntent = new Intent(this, SignallingClient.class);
        bindService(serviceIntent, mConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();
        hangup();
        unbindService(mConnection);
        mBound = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        startService(serviceIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_main);

        localVideoView =(SurfaceViewRenderer) findViewById(R.id.local_gl_surface_view);
        remoteVideoView =(SurfaceViewRenderer) findViewById(R.id.remote_gl_surface_view);


        findViewById(R.id.fab_video).setOnClickListener(v -> {
            if(isCAlled){
                hangup();
                finish();
            }else{
                startCall();
                doCall();

            }

        });

        getIceServers(getIntent());





    }


    private void startCall() {
            localVideoView.setVisibility(View.VISIBLE);
            ((FloatingActionButton) findViewById(R.id.fab_video)).setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            isCAlled=true;
            initVideos();

            start();


    }

    private void initVideos() {
        rootEglBase = EglBase.create();

            try {
                localVideoView.init(rootEglBase.getEglBaseContext(), null);
                remoteVideoView.init(rootEglBase.getEglBaseContext(), null);
                localVideoView.setZOrderMediaOverlay(true);
                remoteVideoView.setZOrderMediaOverlay(true);
            }catch (IllegalStateException e){

            }


    }

    private void getIceServers(Intent intent) {
        //get Ice servers using xirsys
        Utils.getInstance().getRetrofitInstance().getIceCandidates(MyApplication.getInstance().getICE_SERVER_HEADER()).enqueue(new Callback<TurnServerPojo>() {
            @Override
            public void onResponse(@NonNull Call<TurnServerPojo> call, @NonNull Response<TurnServerPojo> response) {
                TurnServerPojo body = response.body();
                if (body != null) {
                    iceServers = body.iceServerList.iceServers;
                }
                for (IceServer iceServer : iceServers) {
                    if (iceServer.credential == null) {
                        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder(iceServer.url).createIceServer();
                        peerIceServers.add(peerIceServer);
                    } else {
                        PeerConnection.IceServer peerIceServer = PeerConnection.IceServer.builder(iceServer.url)
                                .setUsername(iceServer.username)
                                .setPassword(iceServer.credential)
                                .createIceServer();
                        peerIceServers.add(peerIceServer);
                    }
                }

                if(intent.hasExtra("offer")&& !TextUtils.isEmpty(intent.getStringExtra("offer"))){
                    try {
                        onOfferReceived(new JSONObject(intent.getStringExtra("offer")));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                Log.d("onApiResponse", "IceServers" + iceServers.toString());
            }

            @Override
            public void onFailure(@NonNull Call<TurnServerPojo> call, @NonNull Throwable t) {
                t.printStackTrace();
            }
        });
    }


    public void start() {
        //Initialize PeerConnectionFactory globals.
        PeerConnectionFactory.InitializationOptions initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this)
                        .setEnableVideoHwAcceleration(true)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        //Create a new PeerConnectionFactory instance - using Hardware encoder and decoder.
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(
                rootEglBase.getEglBaseContext(),  /* enableIntelVp8Encoder */true,  /* enableH264HighProfile */true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());
        peerConnectionFactory = new PeerConnectionFactory(options, defaultVideoEncoderFactory, defaultVideoDecoderFactory);

    //Now create a VideoCapturer instance.
    VideoCapturer videoCapturerAndroid = createCameraCapturer(new Camera1Enumerator(false));



        //Create MediaConstraints - Will be useful for specifying video and audio constraints.
        audioConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();
        sdpConstraints = new MediaConstraints();
        //Create a VideoSource instance
            videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
            localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);


        //create an AudioSource instance
        audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);


            videoCapturerAndroid.startCapture(1024, 720, 30);
            localVideoView.setVisibility(View.VISIBLE);
            //create a videoRenderer based on SurfaceViewRenderer instance
            localRenderer = new VideoRenderer(localVideoView);
            // And finally, with our VideoRenderer ready, we
            // can add our renderer to the VideoTrack.
            localVideoTrack.addRenderer(localRenderer);

            localVideoView.setMirror(true);
            remoteVideoView.setMirror(true);



        gotUserMedia = true;

        if (mService.isInitiator) {
            onTryToStart();
        }

    }





    @Override
    public void onTryToStart() {
        Log.i("METHODS","onTryToStart");

        runOnUiThread(() -> {
            if (!mService.isStarted && localVideoTrack != null && mService.isChannelReady) {
                createPeerConnection();
                mService.isStarted = true;

            }
        });
    }


    /**
     * Creating the local peerconnection instance
     */
    private void createPeerConnection() {
        Log.i("METHODS","createPeerConnection");
        PeerConnection.RTCConfiguration rtcConfig =
                new PeerConnection.RTCConfiguration(peerIceServers);
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
        // Use ECDSA encryption.
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        localPeer = peerConnectionFactory.createPeerConnection(rtcConfig, new CustomPeerConnectionObserver("localPeerCreation") {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                onIceCandidateReceived(iceCandidate);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                showToast("Received Remote stream");
                super.onAddStream(mediaStream);
                gotRemoteStream(mediaStream);
            }
        });

        addStreamToLocalPeer();
        isInitiated=true;
    }

    /**
     * Adding the stream to the localpeer
     */
    private void addStreamToLocalPeer() {
        Log.i("METHODS","addStreamToLocalPeer");

        //creating local mediastream
        MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);

            stream.addTrack(localVideoTrack);

        localPeer.addStream(stream);
    }

    /**
     * This method is called when the app is initiator - We generate the offer and send it over through socket
     * to remote peer
     */
    private void doCall() {
        Log.i("METHODS","doCall");

        localPeer.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"), sessionDescription);
                Log.d("onCreateSuccess", "SignallingClient emit ");
                if(!iceCandidateSent) {
                    mService.emitMessage(sessionDescription);
                }
            }

            @Override
            public void onSetSuccess() {

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        },sdpConstraints);
    /*    localPeer.createOffer(new CustomSdpObserver("localCreateOffer"){
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                //we have localOffer. Set it as local desc for localpeer and remote desc for remote peer.
                //try to create answer from the remote peer.
                super.onCreateSuccess(sessionDescription);
                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocalDesc"){
                    @Override
                    public void onCreateSuccess(SessionDescription sessionDescription) {
                        super.onCreateSuccess(sessionDescription);
                        Log.d(TAG, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");

                        SignallingClient.getInstance().emitMessage(sessionDescription);

                    }

                    @Override
                    public void onSetFailure(String s) {
                        super.onSetFailure(s);
                        Log.d(TAG, "onSetFailure() called with: s = [" + s + "]");

                    }
                }, sessionDescription);
                Log.d("onCreateSuccess", "SignallingClient emit ");
            }
        },sdpConstraints);*/
    }

    /** Sets the speaker phone mode. */
    private void setSpeakerphoneOn(boolean on) {
         AudioManager myAudioManager;
        myAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        myAudioManager.setMode(AudioManager.STREAM_VOICE_CALL);
        boolean wasOn = myAudioManager.isSpeakerphoneOn();
        if (wasOn == on) {
            return;
        }
        myAudioManager.setSpeakerphoneOn(on);
    }

    /**
     * Received remote peer's media stream. we will get the first video track and render it
     */
    private void gotRemoteStream(MediaStream stream) {
        Log.i("METHODS","gotRemoteStream");

        //we have remote video stream. add to the renderer.

            final VideoTrack videoTrack = stream.videoTracks.get(0);
            runOnUiThread(() -> {
                try {
                    setSpeakerphoneOn(true);
                    localVideoView.setVisibility(View.GONE);
                    remoteRenderer = new VideoRenderer(remoteVideoView);
                    remoteVideoView.setVisibility(View.VISIBLE);
                    videoTrack.addRenderer(remoteRenderer);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


    }

ArrayList<IceCandidate> iceCandidates = new ArrayList<>();
    /**
     * Received local ice candidate. Send it to remote peer through signalling for negotiation
     */
    public void onIceCandidateReceived(IceCandidate iceCandidate) {
       // Log.i("METHODS","onIceCandidateReceived");
        iceCandidates.add(iceCandidate);
        //we have received ice candidate. We can set it to the other peer.
     //   sendAllCandidates();

    }


    public void sendAllCandidates(){
            Log.i("METHODS","sendAllCandidates :"+iceCandidates.size());
            if(!iceCandidateSent) {
                mService.emitIceCandidate(iceCandidates);
                iceCandidateSent=true;
            }

    }




    @Override
    public void onRemoteHangUp(String msg) {
        showToast("Remote Peer hungup");
        runOnUiThread(this::hangup);
    }

    /**
     * SignallingCallback - Called when remote peer sends offer
     */
    @Override
    public void onOfferReceived(final JSONObject data) {
        Log.i("METHODS","onOfferReceived");
        showToast("Received Offer");

        if(!isInitiated){
            startCall();
        }

        runOnUiThread(() -> {


            try {
                localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"),new SessionDescription(SessionDescription.Type.OFFER, data.getString("sdp")));

                doAnswer();

            } catch (JSONException e) {
                e.printStackTrace();
            }catch (NullPointerException e){
                e.printStackTrace();
            }
        });
    }


    private void doAnswer() {
        Log.i("METHODS","doAnswer");
        localPeer.createAnswer(new CustomSdpObserver("localCreateAns") {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                Log.d(TAG, "onCreateSuccess() called with: sessionDescription = [" + sessionDescription + "]");

                localPeer.setLocalDescription(new CustomSdpObserver("localSetLocal"), sessionDescription);
                mService.emitMessage(sessionDescription);
            }

            @Override
            public void onSetFailure(String s) {
                super.onSetFailure(s);
                Log.d(TAG, "onSetFailure() called with: s = [" + s + "]");

            }
        }, new MediaConstraints());
    }

    /**
     * SignallingCallback - Called when remote peer sends answer to your offer
     */

    @Override
    public void onAnswerReceived(JSONObject data) {
        Log.i("METHODS","onAnswerReceived :"+data.toString());

        showToast("Received Answer");
        try {
            localPeer.setRemoteDescription(new CustomSdpObserver("localSetRemote"), new SessionDescription(SessionDescription.Type.fromCanonicalForm(data.getString("type").toLowerCase()), data.getString("sdp")));
            sendAllCandidates();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    boolean iceCandidateSent=false;
    /**
     * Remote IceCandidate received
     */
    @Override
    public void onIceCandidateReceived(JSONArray data) {
        Log.i("METHODS","onIceCandidateReceived :"+data.toString());
        try {
            for (int i = 0; i < data.length(); i++) {
                JSONObject object =data.getJSONObject(i);
                localPeer.addIceCandidate(new IceCandidate(object.getString("id"), object.getInt("label"), object.getString("candidate")));

            }
            if(!iceCandidateSent) {
                sendAllCandidates();
            }

        } catch (JSONException e) {
            e.printStackTrace();

        }catch (NullPointerException e){
        }
    }



    /**
     * Closing up - normal hangup and app destroye
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

        }
    }

    private void hangup() {
        try {
            if(localPeer!=null) {
                localPeer.close();
            }
            if(rootEglBase!=null) {
                rootEglBase.release();
            }
            localPeer = null;
            rootEglBase=null;
            mService.isInitiator=false;
            mService.isStarted=false;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }




    public void showToast(final String msg) {
        runOnUiThread(() -> Toast.makeText(InternetCall.this, msg, Toast.LENGTH_SHORT).show());
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}
