package com.ctrlrobotics.ctrl.presentation;

import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.ctrlrobotics.ctrl.MessageEvent;
import com.ctrlrobotics.ctrl.MotionService;
import com.ctrlrobotics.ctrl.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.unit.HandMotionManager;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WaistMotionManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.WingMotionManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PresentationActivity extends TopBaseActivity implements TextureView.SurfaceTextureListener, MoviePlayer.PlayerFeedback {

    private String TAG = "PresentationActivity";
    public Map<String, Object>  robotState;
    private String refPath;
    private String deviceId;
    private String robotType;
    private String robotAlias;

    TextView tvAlias;

    private static HardWareManager hardwareManager;
    private static HandMotionManager handMotionManager;
    private static HeadMotionManager headMotionManager;
    private static WaistMotionManager waistMotionManager;
    private static  WheelMotionManager wheelMotionManager;
    private static ModularMotionManager modularMotionManager;
    private static WingMotionManager wingMotionManager;
    private static SystemManager systemManager;

    private AnimatronicsHelper animatronicsHelper;
    private AnimBuilder animBuilder;
    private Handler handler;                     // wheels handler
    private Handler lwHandler;                   // left wing handler
    private Handler rwHandler;                   // right wing handler

    FirebaseFirestore db;
    DocumentReference robotRef;

    private TextureView mTextureView;
    private boolean mSurfaceTextureReady = false;
    private String[] mMovieFiles;
    private MoviePlayer.PlayTask mPlayTask;
    private boolean shouldStartVideo = false;

    TextToSpeech tts;
    Set<String> a = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Tai", TAG + " onCreate");
        register(PresentationActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_presentation);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // The screen is always on
        getSupportActionBar().hide();                                           // Hide the action bar at the top

        // Sanbot managers
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        hardwareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        handMotionManager = (HandMotionManager) getUnitManager(FuncConstant.HANDMOTION_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        waistMotionManager = (WaistMotionManager) getUnitManager(FuncConstant.WAIST_MANAGER);
        modularMotionManager= (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);
        wingMotionManager = (WingMotionManager) getUnitManager(FuncConstant.WINGMOTION_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        refPath = getIntent().getStringExtra("REF_PATH");
        deviceId = getIntent().getStringExtra("DEVICE_ID");
        robotType = getIntent().getStringExtra("ROBOT_TYPE");
        robotAlias = getIntent().getStringExtra("ROBOT_ALIAS");

        tvAlias = findViewById(R.id.tv_alias);
        tvAlias.setText(capitalizeFirstLetter(robotAlias));
        tvAlias.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        // Firebase references
        db = FirebaseFirestore.getInstance();
        robotRef = db.collection(refPath).document(deviceId);

        initRobotControls();
        initAnimSequence();

        handMotionManager.doResetMotion();

        mTextureView = (TextureView) findViewById(R.id.movie_texture_view);
        mTextureView.setSurfaceTextureListener(this);

        mMovieFiles = MiscUtils.getFiles(getFilesDir(), "*.mp4");
        if (mMovieFiles.length > 0) {
            Log.i("Tai", TAG + ": movieFiles: " + mMovieFiles[0]);
            shouldStartVideo = true;
        } else {
            shouldStartVideo = false;
            Log.i("Tai", TAG + ": movieFiles: " + "No mp4 files in directory: " + getFilesDir());
        }

        ttsInit();

        rwHandler.post(runnableShowEmotionIndefinitely);

    }

    private String capitalizeFirstLetter(String str) {
        String cap = str.substring(0, 1).toUpperCase() + str.substring(1);
        return cap;
    }

    private Intent motionServiceIntent;
    DocumentReference refF;                     // To adjust speed from the front end, we listen to this value
    DocumentReference refStart;                 // To start a sequence in sync
    DocumentReference refDelays;                // To change Thread sleep times from firebase
    DocumentReference refHands;                 // To deliver hand movement commands
    DocumentReference refHead;                  // To deliver head movement commands
    DocumentReference refSay;                   // For TTS commands
    int speed = 1;
    int distance;
    int nLoops = 1;
    Map<String, Object> delayList;
    DelayVals delayVals;
    private void initRobotControls() {
        // To control robot movements from web page we use the motion service
        motionServiceIntent = new Intent(PresentationActivity.this, MotionService.class);
        motionServiceIntent.putExtra("REF_PATH", refPath);
        motionServiceIntent.putExtra("DEVICE_ID", deviceId);
        motionServiceIntent.putExtra("ROBOT_TYPE", robotType);
        startService(motionServiceIntent);

        // Initialize Sync variables
        Map<String, Object> initialState;
        initialState = new HashMap<>();
        initialState.put("nLoops", nLoops);
        initialState.put("switch", 0);

        // Listen to the Sync/PresentationActivity document
        refStart = db.collection("Sync").document("PresentationActivity");
        refStart.set(initialState);
        refStart.addSnapshotListener(PresentationActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", TAG + ": Listen failed.", e);
                    return;
                }
                String ref = "refStart";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    switch (swtch) {
                        case "0": {
                            // This is the switch default state. Do not alter the control code unless you are sure you want to do this.
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            if (!robotAlias.equals("ariel")) {
                                try {
                                    startAnimSequence1();
                                } catch (InterruptedException err) {
                                    Log.d(TAG, ": animSequence: caught exception");
                                    err.printStackTrace();
                                    refStart.update("switch", 0);
                                }
                            }
                            break;
                        }
                        case "2": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            try {
                                if(robotAlias.equals("ariel")) {
                                    startAnimSequence2();
                                } else {
                                    refStart.update("switch", 0);
                                }
                            } catch (InterruptedException err) {
                                Log.d(TAG, ": animSequence: caught exception");
                                err.printStackTrace();
                                refStart.update("switch", 0);
                            }
                            break;
                        }
                        case "3": {
                            try {
                                startAnimSequence3();
                            } catch (InterruptedException err) {
                                Log.d(TAG, ": animSequence: caught exception");
                                err.printStackTrace();
                                refStart.update("switch", 0);
                            }
                            break;
                        }
                        case "stop": {
                            handler.removeCallbacksAndMessages(null);
                            handMotionManager.doResetMotion();
                            refStart.update("switch", 0);

                            try {
                                lwHandler.removeCallbacks(runnablePersistLightFlicker);
                                handler.post(() -> {
                                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HEAD, animatronicsHelper.LED_BLUE);
                                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_BLUE);
                                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HEAD, animatronicsHelper.LED_BLUE);
                                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_BLUE);
                                    animatronicsHelper.lightLed(animatronicsHelper.LED_WHEEL, animatronicsHelper.LED_BLUE);
                                    animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_BLUE);
                                });
                            } catch(Exception err) {
                                Log.e("Tai", TAG + ": refStart: case: stop: exception: " + err);
                                err.printStackTrace();
                            }

                        }
                        default:
                            Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Invalid switch case");
                            break;
                    }
                    nLoops = Integer.parseInt(documentSnapshot.get("nLoops").toString());   // Set the number of loops to run

                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });


        // Listen to the speed value on the front end slider (initial state set by MotionService)
        refF = robotRef.collection("motionValues").document("Forward");
        refF.addSnapshotListener(PresentationActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", TAG + ": refF: Listen failed.", e);
                    return;
                }
                String ref = "refF";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Got data snapshot");
                    speed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    distance = Integer.parseInt(documentSnapshot.get("distance").toString());
                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: refF: Snapshot: null");
                }
            }
        });

        // Initialize delay variables in firebase
        initialState = new HashMap<>();
        for(int i = 0; i < 99; i++) {
            initialState.put("delay_" + String.valueOf(i+1), 0);
        }
        initialState.put("setDefaults", false);

        // Listen to the custom delay values set in firebase
        refDelays = robotRef.collection("presentation").document("delays");
        Map<String, Object> finalInitialState = initialState;
        refDelays.addSnapshotListener(PresentationActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String ref = "refDelays";
                if (e != null) {
                    Log.w("Tai", TAG + ": " + ref + ": Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Got data snapshot");
                    if(documentSnapshot.getBoolean("setDefaults")) {
                        refDelays.set(finalInitialState);
                    }
                    delayList = documentSnapshot.getData();
                    delayVals = new DelayVals();
                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: " + refF + ": Snapshot: null");
                    refDelays.set(finalInitialState);
//                    delayVals = new DelayVals();
                }
            }
        });


        // Listen for commands to move the hands
        initialState = new HashMap<>();
        initialState.put("command", 0);
        refHands = robotRef.collection("presentation").document("hands");
        refHands.set(initialState);
        refHands.addSnapshotListener(PresentationActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String ref = "refHands";
                if (e != null) {
                    Log.w("Tai", TAG + ": " + ref + ": Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Got data snapshot");
                    String command = documentSnapshot.get("command").toString();
                    switch(command) {
                        case "leftWave":
//                            new Thread(runnableLeftWave).start();
                            handler.post(runnableLeftWave);
                            refHands.update("command", 0);
                            break;
                        case "rightWave":
//                            new Thread(runnableRightWave).start();
                            handler.post(runnableRightWave);
                            refHands.update("command", 0);
                            break;
                        case "bothMuscles":
//                            new Thread(runnableBothMuscles).start();
                            handler.post(runnableBothMuscles);
                            refHands.update("command", 0);
                        default:
                            Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Invalid switch case");
                            refHands.update("command", 0);
                            break;
                    }
                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Snapshot: null");

                }
            }
        });


        // Listen for commands to move the head
        initialState = new HashMap<>();
        initialState.put("command", "0");
        refHead = robotRef.collection("presentation").document("head");
        refHead.set(initialState);
        refHead.addSnapshotListener(PresentationActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String ref = "refHead";
                if (e != null) {
                    Log.w("Tai", TAG + ": " + ref + ": Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Got data snapshot");
                    String command = documentSnapshot.get("command").toString();
                    switch(command) {
                        case "yes":
                            new Thread(runnableHeadYes).start();
                            refHead.update("command", 0);
                            break;
                        case "no":
                            new Thread(runnableHeadNo).start();
                            refHead.update("command", 0);
                            break;
                        default:
                            Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Invalid switch case: " + command.toString());
                            refHead.update("command", 0);
                            break;
                    }
                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Snapshot: null");

                }
            }
        });

        // Listen for TTS
        initialState = new HashMap<>();
        initialState.put("say", "0");
        refSay = robotRef.collection("presentation").document("say");
        refSay.set(initialState);
        refSay.addSnapshotListener(PresentationActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String ref = "refSay";
                if (e != null) {
                    Log.w("Tai", TAG + ": " + ref + ": Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Got data snapshot");
                    String str = documentSnapshot.get("say").toString();

                    if (!str.equals("0")) {
                        talk(str);
                    }

                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Snapshot: null");

                }
            }
        });


    }
    private void initAnimSequence() {
        animatronicsHelper = new AnimatronicsHelper(
                headMotionManager,
                handMotionManager,
                waistMotionManager,
                wheelMotionManager,
                modularMotionManager,
                wingMotionManager,
                hardwareManager
        );
        animBuilder = new AnimBuilder(
                animatronicsHelper,
                handMotionManager,
                wingMotionManager);

        // Worker thread handlers
        handler = new Handler();            // Wheels
        lwHandler = new Handler();          // Left wing
        rwHandler = new Handler();          // Right wing
        animatronicsHelper.setResetMotion(false);    // Whether to reset the hands

    }

    int translationSpeedMax = 6;
    int turnAngle = 355;
    int returnAngle = 265;
    int turnSpeed = 6;

    // main
    private void startAnimSequence1() throws InterruptedException {
        handler.removeCallbacksAndMessages(null);

        Log.i(TAG, ": startSequence: " + robotType);
        switch (robotType) {
            case "Max":

                for(int i = 0; i < nLoops; i++) {

                    int cumulativeDelayMillis =0;

                    // arms cheer
                    rwHandler.postDelayed(() -> {    // 6000
                        // arms
                        animatronicsHelper.setResetMotion(false);
                        animatronicsHelper.doHandMotion(animatronicsHelper.BOTH_ARMS, animatronicsHelper.ARMS_CHEER);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 8000;

                    // rotate l 360
                    handler.postDelayed(() -> {       //6000
                        // rotate
                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 6600 + 1000;
                    // rotate r 360
                    handler.postDelayed(() -> {       //6000
                        // rotate
                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 6600 + 1000;







                    // arms defend
                    rwHandler.postDelayed(() -> {
                        // arms
                        animatronicsHelper.setResetMotion(false);
                        animatronicsHelper.doHandMotion(animatronicsHelper.BOTH_ARMS, animatronicsHelper.ARMS_DEFEND);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 8000;

                    // rotate r
                    handler.postDelayed(() -> {
                        // rotate
                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(90);          //3400
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 3400;

                    // fwd
                    handler.postDelayed(() -> {          // 2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;

                    // back
                    handler.postDelayed(() -> {      //2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_BACK);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000 + 500;

                    // fwd
                    handler.postDelayed(() -> {          // 2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;

                    // back
                    handler.postDelayed(() -> {      //2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_BACK);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000 + 500;







                    // rotate r
                    handler.postDelayed(() -> {
                        // rotate
                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(180);          // 7200
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 5500;

                    // back
                    handler.postDelayed(() -> {      //2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_BACK);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000 + 500;

                    // fwd
                    handler.postDelayed(() -> {          // 2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;

                    // back
                    handler.postDelayed(() -> {      //2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_BACK);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000 + 500;

                    // fwd
                    handler.postDelayed(() -> {          // 2500
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;






                    // arms cheer
                    rwHandler.postDelayed(() -> {    // 6000
                        // arms
                        animatronicsHelper.setResetMotion(false);
                        animatronicsHelper.doHandMotion(animatronicsHelper.BOTH_ARMS, animatronicsHelper.ARMS_CHEER);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 8000;

                    // rotate r
                    handler.postDelayed(() -> {
                        // rotate
                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(90);          // 3400
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 3400;









                    // left
                    handler.postDelayed(() -> {          // 2500
                        // left
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_LEFT_TRANSLATION);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;

                    // right
                    handler.postDelayed(() -> {      //2500
                        // right
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_RIGHT_TRANSLATION);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;

                    // left
                    handler.postDelayed(() -> {          // 2500
                        // left
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_LEFT_TRANSLATION);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;

                    // right
                    handler.postDelayed(() -> {      //2500
                        // right
                        animatronicsHelper.setSpeed(translationSpeedMax);
                        animatronicsHelper.setDistance(100);   // cm
                        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_RIGHT_TRANSLATION);
                    }, cumulativeDelayMillis);
                    cumulativeDelayMillis += 2500 + 1000;




                    handler.postDelayed(runnableLeftWave, cumulativeDelayMillis);


                }

                break;
            case "Elf":
                Log.i("Tai", TAG + ": " + robotAlias);
                lwHandler.post(runnablePersistLightFlicker);
                if (robotAlias.equals("ling")) {
                    handler.post(runnableRotateLeftIndefinitely);
                } else if(robotAlias.equals("thandi")) {
                    handler.post(runnableRotateRightLeftIndefinitely);
                } else {
                    handler.post(runnableRotateRightIndefinitely);
                }

                break;
        }
        refStart.update("switch", 0);
    }
    // greet
    private void startAnimSequence2() throws InterruptedException {
        handler.removeCallbacksAndMessages(null);
        Log.i(TAG, ": startSequence: " + robotType);

        int cumulativeDelayMillis;

        switch (robotType) {
            case "Max":

                cumulativeDelayMillis = 0;

                // fwd
                handler.post(() -> {
                    animatronicsHelper.setSpeed(10);
                    animatronicsHelper.setDistance(100);
                    animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);
                });
                cumulativeDelayMillis += 3000;

//                // turn right 90
//                handler.postDelayed(() -> {
//                    animatronicsHelper.setTurnSpeed(10);
//                    animatronicsHelper.setTurnAngle(90);
//                    animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
//                }, cumulativeDelayMillis);
//                cumulativeDelayMillis += 2000;

                // wave arm
                // wave
                handler.postDelayed(() -> {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_WAVE);
                }, cumulativeDelayMillis);
                cumulativeDelayMillis += 5000;

                // speak
                handler.postDelayed(() -> {
                    talk("Good evening Mr. President, and esteemed guests. Mr. President, please join us in showing off some of our robot dance moves.");
                }, cumulativeDelayMillis);
                cumulativeDelayMillis += 5000;

//                // turn right 90
//                handler.postDelayed(() -> {
//                    animatronicsHelper.setTurnAngle(90);
//                    animatronicsHelper.setTurnSpeed(10);
//                    animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
//                }, cumulativeDelayMillis);
//                cumulativeDelayMillis += 2000;
//
//                // fwd
//                handler.postDelayed(() -> {
//                    animatronicsHelper.setSpeed(10);
//                    animatronicsHelper.setDistance(100);
//                    animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);
//                }, cumulativeDelayMillis);
//                cumulativeDelayMillis += 3000;          // == 15000


                break;
            case "Elf":

                for(int i = 0; i < nLoops; i++) {

                    Log.d(TAG, ": loop: " + i);

                    if (robotAlias.equals("thandi")) {
                        animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_BLUE);
                    } else if(robotAlias.equals("epsilon")) {
                        animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_RED);
                    } else if (robotAlias.equals("ling")) {
                        animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_GREEN);
                    }

                }

                break;
        }
        refStart.update("switch", 0);
    }
    // welcome
    private void startAnimSequence3() throws InterruptedException {
        handler.removeCallbacksAndMessages(null);
        Log.i(TAG, ": startSequence3: " + robotType);


        int cumulativeDelayMillis;

        switch (robotType) {

            case "Max":
                cumulativeDelayMillis = 0;

                // wave
                handler.postDelayed(() -> {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_WAVE);
                }, cumulativeDelayMillis);
                cumulativeDelayMillis += 5000;

                // speak
                handler.postDelayed(() -> {
                    String c = "Hi, my name is " + robotAlias + ". " +  getString(R.string.speech_0);
                    talk(c);
                }, cumulativeDelayMillis);
                cumulativeDelayMillis += 5000;
                break;
            case "Elf":
                Toast.makeText(PresentationActivity.this, "No sequence", Toast.LENGTH_LONG).show();
                break;
        }
        refStart.update("switch", 0);
    }
    private void startAnimSequence4() throws InterruptedException {
        handler.removeCallbacksAndMessages(null);
        Log.i(TAG, ": startSequence: " + robotType);
        switch (robotType) {
            case "Max":
                animBuilder.setRobotType(robotType);

                break;
            case "Elf":
                animBuilder.setRobotType(robotType);

                for(int i = 0; i < nLoops; i++) {

                }

                break;
        }
        refStart.update("switch", 0);
    }

    private class DelayVals {
        private int fwd;
        private int back;

        private int leftRotate;
        private int leftTranslate;

        private int rightRotate;
        private int rightTranslate;

        private int wave;
        private int wave2;

        private int head;


        public DelayVals() {
            this.fwd = ((Long) Objects.requireNonNull(delayList.get("delay_1"))).intValue();  //2000
            this.back = ((Long) Objects.requireNonNull(delayList.get("delay_10"))).intValue(); //2000

            this.leftRotate = ((Long) Objects.requireNonNull(delayList.get("delay_11"))).intValue(); //3000
            this.leftTranslate = ((Long) Objects.requireNonNull(delayList.get("delay_12"))).intValue(); //2000

            this.rightRotate = ((Long) Objects.requireNonNull(delayList.get("delay_13"))).intValue();
            this.rightTranslate = ((Long) Objects.requireNonNull(delayList.get("delay_14"))).intValue();

            this.wave = ((Long) Objects.requireNonNull(delayList.get("delay_15"))).intValue(); //3000
            this.wave2 = ((Long) Objects.requireNonNull(delayList.get("delay_16"))).intValue(); //4000

            this.head = ((Long) Objects.requireNonNull(delayList.get("delay_17"))).intValue(); //2000

        }
    }

    int count = 0;
    final Runnable runnableRotateLeftIndefinitely = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.setTurnSpeed(10);
            animatronicsHelper.setTurnAngle(90);
            animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);

            if (count % 2 == 0) {
                lwHandler.postDelayed(runnableLeftWingUp, 1000);
                rwHandler.postDelayed(runnableRightWingDown, 1000);
            } else {
                lwHandler.postDelayed(runnableLeftWingDown, 1000);
                rwHandler.postDelayed(runnableRightWingUp, 1000);
            }

            count++;
            handler.postDelayed(runnableRotateLeftIndefinitely, 2000);
        }
    };
    final Runnable runnableRotateRightIndefinitely = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.setTurnSpeed(10);
            animatronicsHelper.setTurnAngle(90);
            animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);

            if (count % 2 == 0) {
                lwHandler.postDelayed(runnableLeftWingUp, 1000);
                rwHandler.postDelayed(runnableRightWingDown, 1000);
            } else {
                lwHandler.postDelayed(runnableLeftWingDown, 1000);
                rwHandler.postDelayed(runnableRightWingUp, 1000);
            }

            count++;
            handler.postDelayed(runnableRotateRightIndefinitely, 2000);
        }
    };
    final Runnable runnableRotateRightLeftIndefinitely = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.setTurnSpeed(8);
            animatronicsHelper.setTurnAngle(360);
            animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);

            if (count % 2 == 0) {
                lwHandler.postDelayed(runnableLeftWingUp, 1000);
                rwHandler.postDelayed(runnableRightWingDown, 1000);
            } else {
                lwHandler.postDelayed(runnableLeftWingDown, 1000);
                rwHandler.postDelayed(runnableRightWingUp, 1000);
            }

            try {
                Thread.sleep(3000);      // x
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            animatronicsHelper.setTurnSpeed(4);
            animatronicsHelper.setTurnAngle(360);
            animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);

            if (count % 2 == 0) {
                lwHandler.postDelayed(runnableRightWingUp, 1000);
                rwHandler.postDelayed(runnableLeftWingDown, 1000);
            } else {
                lwHandler.postDelayed(runnableRightWingDown, 1000);
                rwHandler.postDelayed(runnableLeftWingUp, 1000);
            }

            count++;
            handler.postDelayed(runnableRotateRightLeftIndefinitely, 5000);     // 2x
        }
    };
    final Runnable runnableShowEmotionIndefinitely = new Runnable() {
        @Override
        public void run() {
            systemManager.showEmotion(EmotionsType.SURPRISE);
            handler.postDelayed(runnableShowEmotionIndefinitely, 5000);
        }
    };

    final Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            long startTime = System.nanoTime();

            animBuilder.setSpeed(speed);
            animBuilder.setSleepTime(delayVals.fwd);
            animBuilder.setAngle(30);

            animBuilder.goForward();
            animBuilder.turnLeft();
            animBuilder.goLeft();
            animBuilder.goBackward();

            long endTime = System.nanoTime();
            Log.d(TAG, "TIMERr1: " + String.valueOf((endTime -
                    startTime)));
        }
    };
    final Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            long startTime = System.nanoTime();

            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_RED);

            // Center the wings
            int center = 135;
            animBuilder.setWingSpeed(8);
            animBuilder.setSleepTime(6000);
            animBuilder.setWingAngle(center);
            animBuilder.moveBothWings();

            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PURPLE);

            // Forward
            animBuilder.setSleepTime(2000);
            animBuilder.setSpeed(10);
            animBuilder.goForward();

            // Center the wings
            animBuilder.setWingSpeed(8);
            animBuilder.setSleepTime(6000);
            animBuilder.setWingAngle(center);
            animBuilder.moveBothWings();

            // Shuffle wings
            int diff = 60;         // We can move a total of about 60 degrees in one second at speed 8
            for (int i = 0; i < 2; i++) {
                // Shuffle left
                lwHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        animBuilder.setSleepTime(1000);
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveLeftWingUp();
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveLeftWingDown();
                    }
                });
            }
            for (int i = 0; i < 2; i++) {
                // Shuffle right
                rwHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        animBuilder.setSleepTime(1000);
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveRightWingDown();
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveRightWingUp();
                    }
                });
            }

            // Hands up
//            animBuilder.setWingAngle(0);
//            animBuilder.moveLeftWing();

            // 7 random flickering colors every 100 ms
            animatronicsHelper.setLedDelayTime(1);
            animatronicsHelper.setLedRandomCount(7);
//            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_FLICKER_RANDOM);

            // Left rotate
            animBuilder.setSleepTime(3000);
            animBuilder.setAngle(90);
            animBuilder.turnLeft();

            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PURPLE);

//            animBuilder.setSleepTime(1000);
//            animBuilder.setHeadAngle(90);
//            animBuilder.turnHead();

            // Left translate
            animBuilder.setSleepTime(2000);
            animBuilder.goLeft();

            // Back
            animBuilder.setSleepTime(2000);
            animBuilder.goBackward();

            long endTime = System.nanoTime();
            Log.d(TAG, "TIMER: " + String.valueOf((endTime -
                    startTime)));
        }
    };
    final Runnable runnableFwd = new Runnable() {
        @Override
        public void run() {
//            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PURPLE);

            // Forward
            animBuilder.setSleepTime(delayVals.fwd);
            animBuilder.setSpeed(speed);
            animBuilder.goForward();
        }
    };
    final Runnable runnableWingsUp = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_BLUE);
            // Center the wings
            int position = 60;
            animBuilder.setWingSpeed(8);
            animBuilder.setSleepTime(2000);
            animBuilder.setWingAngle(position);
            animBuilder.moveBothWings();
        }
    };
    final Runnable runnableResetWings = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_RED);
            animBuilder.setSleepTime(7000);
            animBuilder.resetWings();
        }
    };
    final Runnable runnableShuffleWings = new Runnable() {
        @Override
        public void run() {
            // Shuffle wings
            int diff = 60;         // We can move a total of about 60 degrees in one second at speed 8
            for (int i = 0; i < 2; i++) {
                // Shuffle left
                lwHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        animBuilder.setSleepTime(2000);
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveLeftWingUp();
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveLeftWingDown();
                    }
                });
            }
            for (int i = 0; i < 2; i++) {
                // Shuffle right
                rwHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        animBuilder.setSleepTime(2000);
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveRightWingDown();
                        animBuilder.setWingAngle(diff);
                        animBuilder.moveRightWingUp();
                    }
                });
            }
        }
    };
    int diff = 90;
    final Runnable runnableLeftWingUp = new Runnable() {
        @Override
        public void run() {
//            animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_FLICKER_WHITE);
            // Shuffle left
            animBuilder.setSleepTime(1000);
            animBuilder.setWingAngle(diff);
            animBuilder.moveLeftWingUp();
        }
    };
    final Runnable runnableLeftWingDown = new Runnable() {
        @Override
        public void run() {
//            animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_FLICKER_WHITE);
            // Shuffle left
            animBuilder.setSleepTime(1000);
            animBuilder.setWingAngle(diff);
            animBuilder.moveLeftWingDown();
        }
    };
    final Runnable runnableRightWingUp = new Runnable() {
        @Override
        public void run() {
//            animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_FLICKER_WHITE);
            animBuilder.setSleepTime(1000);
            animBuilder.setWingAngle(diff);
            animBuilder.moveRightWingUp();
        }
    };

    final Runnable runnablePersistLightFlicker = new Runnable() {
        @Override
        public void run() {
            lwHandler.postDelayed(() -> {
                animatronicsHelper.setLedRandomCount(5);
                animatronicsHelper.setLedDelayTime(2);
                animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_FLICKER_RANDOM);
                animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_FLICKER_RANDOM);
                animatronicsHelper.lightLed(animatronicsHelper.LED_WHEEL, animatronicsHelper.LED_FLICKER_RANDOM);
                animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HEAD, animatronicsHelper.LED_FLICKER_RANDOM);
                animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HEAD, animatronicsHelper.LED_FLICKER_RANDOM);

                lwHandler.postDelayed(runnablePersistLightFlicker, 1000);
            }, 1000);
        }
    };

    final Runnable runnableRightWingDown = new Runnable() {
        @Override
        public void run() {
//            animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_FLICKER_WHITE);
            animBuilder.setSleepTime(1000);
            animBuilder.setWingAngle(diff);
            animBuilder.moveRightWingDown();
        }
    };
    final Runnable runnableLeftRotate = new Runnable() {
        @Override
        public void run() {
            // 7 random flickering colors every 100 ms
//            animatronicsHelper.setLedDelayTime(1);
//            animatronicsHelper.setLedRandomCount(7);
//            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_FLICKER_RANDOM);

            // Left rotate
            animBuilder.setSleepTime(delayVals.rightRotate);
            animBuilder.setAngle(30);
            animBuilder.turnLeft();

//            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PURPLE);
        }
    };
    final Runnable runnableRightRotate = new Runnable() {
        @Override
        public void run() {
            // 7 random flickering colors every 100 ms
//            animatronicsHelper.setLedDelayTime(1);
//            animatronicsHelper.setLedRandomCount(7);
//            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_FLICKER_RANDOM);

            // Left rotate
            animBuilder.setSleepTime(delayVals.leftRotate);
            animBuilder.setAngle(30);
            animBuilder.turnRight();

//            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PURPLE);
        }
    };


    final Runnable runnableLeftTranslate = new Runnable() {
        @Override
        public void run() {
            // Left translate
            animBuilder.setSleepTime(2000);
            animBuilder.goLeft();
        }
    };
    final Runnable runnableRightTranslate = new Runnable() {
        @Override
        public void run() {
            // Left translate
            animBuilder.setSleepTime(delayVals.rightTranslate);
            animBuilder.goRight();
        }
    };
    final Runnable runnableBack = new Runnable() {
        @Override
        public void run() {
            // Back
            animBuilder.setSleepTime(2000);
            animBuilder.goBackward();
        }
    };

    // Max only
    final Runnable runnableLeftWave = new Runnable() {    // Left hands wave
        @Override
        public void run() {
            animBuilder.setSleepTime(delayVals.wave);
            animBuilder.leftWave();
        }
    };
    final Runnable runnableRightWave = new Runnable() {
        @Override
        public void run() {
            animBuilder.setSleepTime(delayVals.wave);
            animBuilder.rightWave();
        }
    };
    final Runnable runnableBothMuscles = new Runnable() {
        @Override
        public void run() {
            animBuilder.setSleepTime(delayVals.wave2);
            animBuilder.bothMuscles();
        }
    };
    final Runnable runnableHeadYes = new Runnable() {
        @Override
        public void run() {
            animBuilder.setSleepTime(delayVals.head);
            animBuilder.headYes();
        }
    };
    final Runnable runnableHeadNo = new Runnable() {
        @Override
        public void run() {
            animBuilder.setSleepTime(delayVals.head);
            animBuilder.headNo();
        }
    };



    private void startVideo() {
        Log.d("Tai", TAG + "starting movie");
        mTextureView.setVisibility(View.VISIBLE);
        SurfaceTexture st = mTextureView.getSurfaceTexture();
        Surface surface = new Surface(st);
        MoviePlayer player = null;
        SpeedControlCallback callback = new SpeedControlCallback();
        try {
            player = new MoviePlayer(
                    new File(getFilesDir(), mMovieFiles[0]), surface, callback);
        } catch (IOException ioe) {
            Log.e(TAG, "Unable to play movie", ioe);
            surface.release();
            return;
        }
        adjustAspectRatio(player.getVideoWidth(), player.getVideoHeight());
        mPlayTask = new MoviePlayer.PlayTask(player, this);
        mPlayTask.setLoopMode(false);
        mPlayTask.execute();
    }
    private void stopPlayback() {
        /**
         * Requests stoppage if a movie is currently playing.  Does not wait for it to stop.
         */
        if (mPlayTask != null) {
            mPlayTask.requestStop();
        }
        mTextureView.setVisibility(View.INVISIBLE);
    }
    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v(TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    private void ttsInit() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setSpeechRate(0.875F);
                    tts.setPitch(0.9F);
                    Voice ttsVoice = new Voice("en_GB",new Locale("en","GB"),Voice.QUALITY_VERY_HIGH,400,true, a);
//                    Voice ttsVoice = new Voice("es-us-x-sfb-phone-hmm",new Locale("en","US"),500,400,false,a);
                    tts.setVoice(ttsVoice);
                    // Uncomment these to check available voices on the device
//                    myVoices = tts.getVoices();
//                    Log.i("Tai", String.valueOf(myVoices));
                    Log.i("Tai", "TTS initiated");
                }

                // TTS methods
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d("Tai", TAG + ": TTS onStart");
                        systemManager.showEmotion(EmotionsType.SPEAK);
                    }
                    @Override
                    public void onDone(String utteranceId) {
                        Log.d("Tai", TAG + ": TTS onDone");
                    }
                    @Override
                    public void onError(String utteranceId) {
                        Log.d("Tai", "TTS error");
                    }
                });

            }

        });
    }
    private void talk(String c) {
        // Converts a string input to speech for an initialized TTS instance
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Done");
        tts.speak(c, TextToSpeech.QUEUE_FLUSH, params);
    }


    /**------------    SurfaceTexture     ---------------*/
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        // There's a short delay between the start of the activity and the initialization
        // of the SurfaceTexture that backs the TextureView.  We don't want to try to
        // send a video stream to the TextureView before it has initialized, so we disable
        // the "play" button until this callback fires.
        Log.d("Tai", TAG +  ": SurfaceTexture ready (" + width + "x" + height + ")");
        mSurfaceTextureReady = true;
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurfaceTextureReady = false;
        // assume activity is pausing, so don't need to update controls
        return true;    // caller should release ST
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // ignore
    }

    /**------------    MoviePlayer     ---------------*/
    @Override
    public void playbackStopped() {
        Log.d(TAG, "playback stopped");
        mPlayTask = null;
    }








    @Override
    protected void onMainServiceConnected() {
        systemManager.switchFloatBar(false, PresentationActivity.class.getName());              // Show the hovering (back) button
        Log.i("Tai", TAG +  ": onMainServiceConnected");
        handMotionManager.doResetMotion();
    }

    /**----------------- Service hooks ---------------------- */
    private void doActivity(Map<String, Object> robotState) {
        JSONObject robotStateJson = new JSONObject(robotState);
        try {
            JSONObject activityValues = robotStateJson.getJSONObject("activityValues");
            String activity = "presentation";

            if(activityValues.getInt(activity) == 0 && refPath != null) {
                Log.i("Tai", TAG + ": doActivity: Finishing activity: " + activity);
                finish();
            }
        } catch (JSONException e) {
            Log.e("Tai", TAG + "doActivity: JSON Exception: " + e);
            e.printStackTrace();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.i("Tai", TAG + ": onStart");
        EventBus.getDefault().register(this);

        if(robotAlias.equals("lexi")) {
            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_BLUE);
            animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_BLUE);
            animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_BLUE);
        } else if (robotAlias.equals("micah")) {
            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PURPLE);
            animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_PURPLE);
            animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_PURPLE);
        }

        sendMessage("Web", "accordionSummaryMsg", "Ready");
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("Tai", TAG + ": onPause");

        // We're not keeping track of the state in static fields, so we need to shut the
        // playback down.  Ideally we'd preserve the state so that the player would continue
        // after a device rotation.
        //
        // We want to be sure that the player won't continue to send frames after we pause,
        // because we're tearing the view down.  So we wait for it to stop here.
        if (mPlayTask != null) {
            stopPlayback();
            mPlayTask.waitForStop();
        }

        handler.removeCallbacksAndMessages(null);

    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Tai", TAG + ": onStop");
        EventBus.getDefault().unregister(this);
        stopService(motionServiceIntent);

        sendMessage("Web", "accordionSummaryMsg", "-");
    }
    @Subscribe
    public void onEvent(MessageEvent event) {
        robotState = event.getMessage();
        Log.i("Tai", TAG + ": onEvent: robotState: " + robotState);
        doActivity(robotState);
    }

    private void sendMessage(String doc, String field, String msg) {
        db = FirebaseFirestore.getInstance();
        db.collection(refPath).document(deviceId).collection("messages").document(doc).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists() && documentSnapshot != null) {
                            db.collection(refPath).document(deviceId).collection("messages").document(doc).update(field, msg);
                        } else {
                            Map<String, String> initialState = new HashMap<>();
                            initialState.put(field, msg);
                            db.collection(refPath).document(deviceId).collection("messages").document(doc).set(initialState);
                        }
                    }
                });
    }
}
