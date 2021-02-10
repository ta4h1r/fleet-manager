package com.ctrlrobotics.ctrl.slam;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GestureDetectorCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.ctrlrobotics.ctrl.MessageEvent;
import com.ctrlrobotics.ctrl.R;
import com.ctrlrobotics.ctrl.presentation.AnimatronicsHelper;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.DoubleBounce;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.mongodb.lang.NonNull;
import com.sanbot.map.Msg;
import com.sanbot.map.PositionTag;
import com.sanbot.opensdk.base.BindBaseInterface;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.wheelmotion.DistanceWheelMotion;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.GravityDataListener;
import com.sanbot.opensdk.function.unit.interfaces.hardware.RawDataListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;


public class SlamActivity extends TopBaseActivity implements SlamPresenter.IView, BindBaseInterface, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private static String TAG = "SlamActivity";
    public Map<String, Object> robotState;
    private String refPath;
    private String deviceId;
    private String robotAlias;

    private FirebaseFirestore db;
    private DocumentReference mapsDoc;
    private DocumentReference tagsDoc;
    private ListenerRegistration regTagsDoc;
    private ListenerRegistration regMapsDoc;

    TextToSpeech tts;
    Set<String> a = new HashSet<>();

    private SlamPresenter mainPresenter;

    private MapView mapView;
    List<PositionTag> tagsList;

    private RequestQueue mQueue;

    private HardWareManager hardWareManager;
    private WheelMotionManager wheelMotionManager;
    private SystemManager systemManager;

    private Handler sensorRefreshHandler;
    private Handler enterLiftHandler;
    private Handler exitLiftHandler;
    private Handler liftDoorStateHandler;
    private Handler disembarkedHandler;
    private Handler mapCalHandler;
    private Handler goToTagHandler;
    private Handler alignInLiftHandler;
    private Handler connHandler;
    private Handler timeoutHandler;
    private Handler pickupHandler;
    private Handler turnHandler;

    private AnimatronicsHelper animatronicsHelper;
    private TextView tvIrSensors;
    private TextView tvLidarSensors;
    private TextView tvAlias;
    private Button btnEnter;
    private Button btnExit;
    private Button btnAlign;
    private Button btnAlignLr;
    private Button btnOpenBox;
    private TextView tvConnection;
    private TextView tvPos;
    private TextView tvStatus;
    private TextView tvWeight;
    private TextView tvArduino;
    private TextView tvPose;
    private ProgressBar progressBar;

    private static final String DEBUG_TAG = "debug";
    private GestureDetectorCompat mDetector;

    private boolean locked = false;
    private boolean shouldLockDoor = false;

    private Intent arduinoServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(SlamActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_slam);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // The screen is always on
        getSupportActionBar().hide();                                           // Hide the action bar at the top

        // Check hardware permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // JNI lib
        loadLibrary("MapHelper");

        // Firebase refs
        refPath = getIntent().getStringExtra("REF_PATH");
        deviceId = getIntent().getStringExtra("DEVICE_ID");
        robotAlias = getIntent().getStringExtra("ROBOT_ALIAS");

        initDbListeners();
        initS3Handler();
        ttsInit();

        // UI
        mapView = findViewById(R.id.mv_map);
        tvIrSensors = findViewById(R.id.tv_ir_sensors);
        tvLidarSensors = findViewById(R.id.tv_lidar_sensors);
        btnEnter = findViewById(R.id.btn_enter);
        btnExit = findViewById(R.id.btn_exit);
        btnAlign = findViewById(R.id.btn_align);
        btnAlignLr = findViewById(R.id.btn_align_lr);
        btnOpenBox = findViewById(R.id.btn_open_box);
        btnOpenBox.setVisibility(View.INVISIBLE);
        tvConnection = findViewById(R.id.tv_connection);
        tvArduino = findViewById(R.id.tv_arduino);
        tvArduino.append("\n");
        tvArduino.setMovementMethod(new ScrollingMovementMethod());
        tvPos = findViewById(R.id.tv_pos);
        tvPose = findViewById(R.id.tv_pose);
        tvStatus = findViewById(R.id.tv_status);
        tvAlias = findViewById(R.id.tv_alias);
        tvAlias.setText(capitalizeFirstLetter(robotAlias));
        tvWeight = findViewById(R.id.tv_weight);
        tvArduinoServiceCallback = (TextView)findViewById(R.id.tv_callback);
        tvArduinoServiceCallback.setText("Not attached.");

        // Call navigation constructor and start slam interface
        mainPresenter = new SlamPresenter(this.getApplicationContext(),this,
                (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER), tts);
        mainPresenter.start();

        // API requests queue
        mQueue = Volley.newRequestQueue(this);

        // Sanbot managers
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        // Data handlers
        sensorRefreshHandler = new Handler();
        enterLiftHandler = new Handler();
        exitLiftHandler = new Handler();
        liftDoorStateHandler = new Handler();
        disembarkedHandler = new Handler();
        mapCalHandler = new Handler();
        goToTagHandler = new Handler();
        alignInLiftHandler = new Handler();
        connHandler = new Handler();
        timeoutHandler = new Handler();
        pickupHandler = new Handler();
        turnHandler = new Handler();

        initSensorState();

        // Helper class
        animatronicsHelper = new AnimatronicsHelper(null, null, null, wheelMotionManager, null, null, hardWareManager);

        // Listeners
        hardWareManager.setOnHareWareListener(new RawDataListener() {
            @Override
            public void onRawDataResult(int i, String s) {
                switch(i) {
                    case 17:           // IR data; comes through by default every ~1 second
                        try {
                            JSONObject irData = new JSONObject(s);
                            Log.d(DEBUG_TAG, ": ir: " + irData);
                            for (int j=1; j<25; j++) {
                                sensorState.put("infraRed_" + j, (Integer) irData.get("data" + j));
                            }
                        } catch (JSONException e) {
                            Log.e("Tai", TAG + ": onRawDataResult IR exception: " + e);
                            e.printStackTrace();
                        }
                        break;
                    case 20010:        // Lidar data; comes through when hardwareManager.queryUltronicData() is called
                        try {
                            JSONObject lidarData = new JSONObject(s);
//                            Log.d("Tai", TAG + ": lidar: " + lidarData);
                            String[] strs = {
                                    "btm_back_lsb",
                                    "btm_front_lsb",
                                    "btm_left1_lsb",
                                    "btm_left2_lsb",
                                    "btm_right1_lsb",
                                    "btm_right2_lsb",
                                    "left1_3d_lsb",
                                    "left2_3d_lsb",
                                    "left3_3d_lsb",
                                    "left4_3d_lsb",
                                    "right1_3d_lsb",
                                    "right2_3d_lsb",
                                    "right3_3d_lsb",
                                    "right4_3d_lsb",
                                    "top_3d_lsb",
                                    "top_left_front_lsb",
                                    "top_right_front_lsb"
                            };
                            for (String str : strs) {
                                sensorState.put("lidar_" + str, (Integer) lidarData.get(str));
                            }
                        } catch (JSONException e) {
                            Log.e("Tai", TAG + ": onRawDataResult lidar exceotion: " + e);
                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        hardWareManager.setOnHareWareListener((GravityDataListener) v -> {
//                Log.i("Tai", TAG + ": onGravityDataResult: " + v);
            int thresholdWeight = 19;                // kg
            runOnUiThread(() -> {
                tvWeight.setText("current: " + v + " kg" + "\n" + "threshold: "+ thresholdWeight + " kg");
            });
            shouldLockDoor = !(v > thresholdWeight);
            if (shouldLockDoor) {
                runOnUiThread(() -> {
                    tvWeight.setTextColor(getResources().getColor(R.color.Green));
                });
            } else {
                runOnUiThread(() -> {
                    tvWeight.setTextColor(getResources().getColor(R.color.Yellow));
                });
            }
        });
        systemManager.setKeyStatusListener((type, key) -> {
//            Log.i("Tai", TAG + ": onKeyStatus: type :" + type + ", key: " + key);
            if (key.equals("8")) {      // The red shoulder button
                Log.i("Tai", TAG + ": onKeyStatus: removing handler callbacks" );
                statusTvDisplay("Action status");
                enterLiftHandler.removeCallbacksAndMessages(null);
                exitLiftHandler.removeCallbacksAndMessages(null);
                liftDoorStateHandler.removeCallbacksAndMessages(null);
                disembarkedHandler.removeCallbacksAndMessages(null);
                mapCalHandler.removeCallbacksAndMessages(null);
                goToTagHandler.removeCallbacksAndMessages(null);
                alignInLiftHandler.removeCallbacksAndMessages(null);
            }
        });

        btnEnter.setOnClickListener(v -> {
            enterLiftHandler.removeCallbacksAndMessages(null);
            enterLiftHandler.post(runnableEnterLiftFwdLong);
            statusTvDisplay("Action status");
        });
        btnExit.setOnClickListener(v -> {
            exitLiftHandler.removeCallbacksAndMessages(null);
            exitLiftHandler.post(runnableExitLift);
        });
        btnAlign.setOnClickListener(v -> {
            alignInLiftHandler.removeCallbacksAndMessages(null);
            alignInLiftHandler.post(runnableAlignInLiftAngle);
        });
        btnAlignLr.setOnClickListener(v -> {
            alignInLiftHandler.removeCallbacksAndMessages(null);
            alignInLiftHandler.post(runnableAlignInLiftDistance);
        });
        btnOpenBox.setOnClickListener(v -> {
            pickupHandler.removeCallbacksAndMessages(null);

            mainPresenter.speak("Please remove all contents from the box and then close the box door");

            new Thread(() -> {
                sendMessageToArduinoService(ArduinoService.MSG_SEND_TO_SERIAL, ArduinoService.ARG_SEND_0, 0, null);      // Sending zero will unlock
                locked = false;
                pickupHandler.post(runnableWaitForPickup);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    sendMessageToArduinoService(ArduinoService.MSG_SEND_TO_SERIAL, ArduinoService.ARG_SEND_1, 0, null);
                }
                sendMessageToArduinoService(ArduinoService.MSG_SEND_TO_SERIAL, ArduinoService.ARG_SEND_1, 0, null);
                locked = true;
            }).start();

        });

        // Start indefinite runnables
        sensorRefreshHandler.post(runnableSensorRefresh);
        connHandler.post(runnableConnectionStatus);

        // Loading icon
        Sprite doubleBounce = new DoubleBounce();
        progressBar = findViewById(R.id.spin_kit);
        progressBar.setIndeterminateDrawable(doubleBounce);
        progressBar.setVisibility(View.INVISIBLE);

        // Arduino
        arduinoServiceIntent = new Intent(getBaseContext(), ArduinoService.class);

        // Instantiate the gesture detector
        mDetector = new GestureDetectorCompat(this,this);
        mDetector.setOnDoubleTapListener(this);          // Set the gesture detector as the double tap listener.

    }

    // Action runnables
    private String mapName;
    private int retryCountLift = 0;
    private Runnable runnableEnterLift = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_RED);

            int lArmSideIr = sensorState.get("infraRed_10");
            int lArmBackIr = sensorState.get("infraRed_11");

            int rArmSideIr = sensorState.get("infraRed_13");
            int rArmBackIr = sensorState.get("infraRed_14");

            int lBtmBackIr = sensorState.get("infraRed_15");
            int rBtmBackIr = sensorState.get("infraRed_16");

            int btmFrontLi = sensorState.get("lidar_btm_front_lsb");

            int safeArmsDistance = 35;
            if ( (lArmBackIr<0 && rArmBackIr>safeArmsDistance) || (lArmBackIr>safeArmsDistance && rArmBackIr<0) || (lArmBackIr>safeArmsDistance && rArmBackIr>safeArmsDistance) || (lArmBackIr<0 && rArmBackIr<0) ) { // Check if there is space to move

                // back up into lift
                enterLiftHandler.post(() -> {
                    animatronicsHelper.setSpeed(3);
                    animatronicsHelper.setDuration(1500);
                    animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_BACK);
                });        // Move back
                enterLiftHandler.postDelayed(() -> {
                    stop();
                }, 800);   // Stop after delay

                // check if we're inside the lift
                if( (lArmSideIr<80 && rArmSideIr<80) && (lArmSideIr>40 && rArmSideIr>40) && (lBtmBackIr<50 && rBtmBackIr<50) ) {                   // All back and side IRs on the arms should have positive values less than 70 cm
                    animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_GREEN);

                    // Inform nav-planner that sanbot is onboard
                    new Thread(runnableNavEnterLift).start();
                    enterLiftHandler.removeCallbacksAndMessages(null); // Stop running loop
                    timeoutHandler.post(runnableRestartTimeoutThread);
                } else {
                    animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PINK);
                    enterLiftHandler.postDelayed(runnableEnterLift, 2000);     // Run again
                }

            } else {
                retryCountLift += 1;
                if (retryCountLift < 10) {
                    animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PINK);
                    enterLiftHandler.postDelayed(runnableEnterLift, 2000);     // Run again
                    runOnUiThread(() -> {
                        tvStatus.setText("retryCountLiftArmsSafety: " + retryCountLift);
                    });
                    Log.e("Tai", TAG + ": runnableEnterLift: retryCountLift: " + retryCountLift);
                } else {
                    runOnUiThread(() -> {
                        tvStatus.setText("runnableEnterLift: Unable to guarantee arms safety");
                        tvStatus.setTextColor(getResources().getColor(R.color.Red));
                    });
                    Log.e("Tai", TAG + ": runnableEnterLift: " + "Unable to guarantee arms safety.");
                }
            }

        }
    };

    private Runnable runnableEnterLiftFwdShort = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_RED);

            int lArmSideIr = sensorState.get("infraRed_10");
            int lArmFrontIr = sensorState.get("infraRed_9");

            int rArmSideIr = sensorState.get("infraRed_13");
            int rArmFrontIr = sensorState.get("infraRed_12");

            int btmFrontLi = sensorState.get("lidar_btm_front_lsb");

            int reRunDelay = 2800;        // multiple of stopDelay
            int stopDelay = 700;          // factor of reRunDelay
            int fwdSpeed = 2;

            if (robotAlias.equalsIgnoreCase("ariel")) {
                fwdSpeed = 2;
                stopDelay = 800;
                reRunDelay = 3200;
            }

            int safeArmsDistance = 35;
            int safeBoxDistance = 35;
            boolean boxSafe = (btmFrontLi>safeBoxDistance) || (btmFrontLi<0);
            boolean armsSafe = (lArmFrontIr<0 && rArmFrontIr>safeArmsDistance) ||
                    (lArmFrontIr>safeArmsDistance && rArmFrontIr<0) ||
                    (lArmFrontIr>safeArmsDistance && rArmFrontIr>safeArmsDistance) ||
                    (lArmFrontIr<0 && rArmFrontIr<0);
            if ( armsSafe && boxSafe ) { // Check if there is space to move

                // move, stop, move, stop, etc. to allow the sensor readings to stabilize before each move

                int finalFwdSpeed = fwdSpeed;
                enterLiftHandler.post(() -> {
                    animatronicsHelper.setSpeed(finalFwdSpeed);
                    animatronicsHelper.setDuration(1500);
                    animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_FORWARD);
                });        // Move fwd
                enterLiftHandler.postDelayed(() -> {
                    stop();
                }, stopDelay);   // Stop after delay

                hardWareManager.queryUltronicData();

                // check if we're inside the lift
                if( (lArmSideIr<80 && rArmSideIr<80) && (lArmSideIr>40 && rArmSideIr>40) && (btmFrontLi<50) ) {                   // All back and side IRs on the arms should have positive values less than 70 cm
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HEAD, animatronicsHelper.LED_GREEN);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HEAD, animatronicsHelper.LED_GREEN);

                    new Thread(runnableNavEnterLift).start();   // Starts alignment runnables upon API response
                    enterLiftHandler.removeCallbacksAndMessages(null); // Stop running loop
                    timeoutHandler.post(runnableRestartTimeoutThread);
                } else {
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HEAD, animatronicsHelper.LED_PINK);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HEAD, animatronicsHelper.LED_PINK);
                    enterLiftHandler.postDelayed(runnableEnterLiftFwdShort, reRunDelay);     // Run again
                }

            } else {
                retryCountLift += 1;
                if (retryCountLift < 1000) {
                    animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PINK);
                    enterLiftHandler.postDelayed(runnableEnterLiftFwdShort, reRunDelay);     // Run again
                    runOnUiThread(() -> {
                        tvStatus.setText("retryCountLiftArmsSafety: " + retryCountLift);
                    });
                    Log.e("Tai", TAG + ": runnableEnterLiftFwd: retryCountLift: " + retryCountLift);
                } else {
                    runOnUiThread(() -> {
                        tvStatus.setText("runnableEnterLiftFwd: Unable to guarantee arms safety");
                        tvStatus.setTextColor(getResources().getColor(R.color.Red));
                    });
                    Log.e("Tai", TAG + ": runnableEnterLiftFwd: " + "Unable to guarantee arms safety.");
                }
            }

        }
    };
    private Runnable runnableEnterLiftFwdLong = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_RED);

            int lArmFrontIr = sensorState.get("infraRed_9");
            int rArmFrontIr = sensorState.get("infraRed_12");

//            int btmFrontLi = sensorState.get("lidar_btm_front_lsb");
            int btmLeftLi = sensorState.get("lidar_btm_left2_lsb");
            int btmRightLi = sensorState.get("lidar_btm_right2_lsb");

            int reRunDelay = 3200;
            int stopDelay = 9400;
            int fwdSpeed = 2;

            int safeArmsDistance = 110;
            boolean armsSafe = (lArmFrontIr<0 && rArmFrontIr>safeArmsDistance) ||
                    (lArmFrontIr>safeArmsDistance && rArmFrontIr<0) ||
                    (lArmFrontIr>safeArmsDistance && rArmFrontIr>safeArmsDistance) ||
                    (lArmFrontIr<0 && rArmFrontIr<0);
            boolean doorsOpen;

            boolean ariel = robotAlias.equalsIgnoreCase("ariel");
            boolean lexi = robotAlias.equalsIgnoreCase("lexi");
            boolean micah = robotAlias.equalsIgnoreCase("micah");

            if(ariel) {
                doorsOpen = /**(btmFrontLi<25) &&*/ (btmLeftLi<50) && (btmRightLi<50);
            } else if (micah) {
                doorsOpen = /**(btmFrontLi<25) &&*/ (btmLeftLi<50) && (btmRightLi<50);
            } else if (lexi) {
                doorsOpen = /**(btmFrontLi<25) &&*/ (btmLeftLi<50) && (btmRightLi<50);
            } else {
                doorsOpen = /**(btmFrontLi<25) &&*/ (btmLeftLi<25) && (btmRightLi<25);
            }

            if ( armsSafe && doorsOpen ) {

                mainPresenter.speak("If you are riding the lift with me, please stand in the back corner of the elevator");

                int finalFwdSpeed = fwdSpeed;
                enterLiftHandler.post(() -> {
                    animatronicsHelper.setSpeed(finalFwdSpeed);
                    animatronicsHelper.setDuration(10000);
                    animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_FORWARD);
                });        // Move fwd

                enterLiftHandler.postDelayed(() -> {
                    stop();
                    enterLiftHandler.removeCallbacksAndMessages(null);
                    hardWareManager.queryUltronicData();
                    enterLiftHandler.post(runnableEnterLiftFwdShort);
                }, stopDelay);   // Stop after delay

            } else {
                retryCountLift += 1;
                if (retryCountLift < 1000) {
                    animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PINK);
                    enterLiftHandler.postDelayed(runnableEnterLiftFwdLong, reRunDelay);     // Run again
                    runOnUiThread(() -> {
                        tvStatus.setText("retryCountLiftDoorOpening: " + retryCountLift);
                    });
                    Log.e("Tai", TAG + ": runnableEnterLiftFwd: retryCountLift: " + retryCountLift);
                } else {
                    runOnUiThread(() -> {
                        tvStatus.setText("runnableEnterLiftFwdLong: Unable to guarantee door opening");
                        tvStatus.setTextColor(getResources().getColor(R.color.Red));
                    });
                    Log.e("Tai", TAG + ": runnableEnterLiftFwdLong: " + "Unable to guarantee door opening.");
                }
            }

        }
    };
    private Runnable runnableAlignWithLiftDoor = new Runnable() {
        @Override
        public void run() {
            double[] poseVals = mainPresenter.getMapClient().getPose();
            poseTvDisplay("\n@lift x: " + poseVals[0]);
            tvPose.append("\n@lift y: " + poseVals[1]);
            tvPose.append("\n@lift rad: " + poseVals[2]);
            tvPose.append("\nexpected x: " +  x);
            tvPose.append("\nexpected y: " +  y);
            tvPose.append("\nexpected rad: " +  rad);

            int lSensor = sensorState.get("lidar_btm_left2_lsb");     // front left
            int rSensor = sensorState.get("lidar_btm_right2_lsb");    // front right
            int lArmSensor = sensorState.get("infraRed_9");     // front left
            int rArmSensor = sensorState.get("infraRed_12");    // front right
            int tolerance = 2;     // Allowable precision error for all sensors, cm

            int turnSpeed = 10;
            int turnAngle = 1;

            if(lSensor>25 && rSensor>25) {     // Make sure we have a sensible reading

                if ( Math.abs(lSensor-rSensor) < tolerance ) {    // We're aligned
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_GREEN);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_GREEN);
                    enterLiftHandler.removeCallbacksAndMessages(null);

                    // Start watching for the door to open and enter
                    enterLiftHandler.postDelayed(runnableEnterLiftFwdLong, 500);

                    timeoutHandler.post(runnableRestartTimeoutThread);
                } else {

                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_BLUE);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_BLUE);

                    // Which arm is further from the target?
                    if( rSensor < lSensor ) {
                        // rotate right
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_RED);  // That arm with a larger difference lights up

                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    } else if ( rSensor > lSensor ) {
                        // rotate left
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_RED);

                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);
                    }

                    hardWareManager.queryUltronicData();
                    enterLiftHandler.postDelayed(runnableAlignWithLiftDoor, 1800);   // Check again
                }

            } else if ( (lSensor<25 && rSensor<25) || (lSensor>100 && rSensor<25) || (rSensor>100 && lSensor<25) || (rSensor>100 && lSensor>100) ) {      // The doors are already open

                // Check if the arms are safe
                if( (lArmSensor < 0 && rArmSensor < 0) || (lArmSensor > 100 && rArmSensor > 100) ) { // The arms are safe
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_GREEN);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_GREEN);
                    enterLiftHandler.removeCallbacksAndMessages(null);

                    enterLiftHandler.post(runnableEnterLiftFwdShort);

                    timeoutHandler.post(runnableRestartTimeoutThread);
                } else {
                    enterLiftHandler.postDelayed(runnableAlignWithLiftDoor, 1500);   // Check again
                }

            } else {
                enterLiftHandler.postDelayed(runnableAlignWithLiftDoor, 1500);       // Check again
            }

        }
    };
    private Runnable runnableTurnAround = new Runnable() {
        @Override
        public void run() {

            animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_YELLOW);
            animatronicsHelper.setTurnSpeed(5);
            animatronicsHelper.setTurnAngle(180);
            animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);

            turnHandler.postDelayed(runnableAlignWithLiftBackWall, 8000); // Delay to ensure that  the robot has made a complete rotation

        }
    };
    private Runnable runnableAlignWithLiftBackWall = new Runnable() {
        @Override
        public void run() {
            int lSensor = sensorState.get("infraRed_11");     // back left
            int rSensor = sensorState.get("infraRed_14");     // back right

            int liSensorLeft1 = sensorState.get("lidar_left1_3d_lsb");
            int liSensorLeft2 = sensorState.get("lidar_left2_3d_lsb");

            int liSensorRight1 = sensorState.get("lidar_right1_3d_lsb");
            int liSensorRight2 = sensorState.get("lidar_right2_3d_lsb");

            int tolerance = 3;     // Allowable precision error for all sensors, cm

            int turnSpeed = 1;
            int turnAngle = 2;

            if (robotAlias.equalsIgnoreCase("ariel")) {
                turnAngle = 3;
            }

            if(lSensor>0 && rSensor>0) {     // Make sure we have a reading

                if ( Math.abs(lSensor-rSensor) < tolerance ) {
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_GREEN);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_GREEN);
                    turnHandler.removeCallbacksAndMessages(null);

                    // Inform the nav-planner to start changing the map
                    new Thread(runnableNavChangeMap).start();

                    // Start checking to see when we can exit the lift
                    exitLiftHandler.postDelayed(runnableExitLift, 5000);

                    timeoutHandler.post(runnableRestartTimeoutThread);
                } else {

                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_BLUE);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_BLUE);

                    // Which arm is further from the target?
                    if( rSensor > lSensor ) {
                        // rotate right
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_RED);  // That arm with a larger difference lights up

                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    } else if ( rSensor < lSensor )  {
                        // rotate left
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_RED);

                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);
                    }

                    hardWareManager.queryUltronicData();
                    turnHandler.postDelayed(runnableAlignWithLiftBackWall, 2000);   // Check again

                }

            } else {
                int leftDiff = Math.abs(liSensorLeft1-liSensorLeft2);
                int rightDiff = Math.abs(liSensorRight1-liSensorRight2);

                if (leftDiff>rightDiff) {   // The corner is on the left side
                    // turn left
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_RED);  // That arm with a larger difference lights up

                    animatronicsHelper.setTurnSpeed(turnSpeed);
                    animatronicsHelper.setTurnAngle(turnAngle);
                    animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);
                } else if (rightDiff>leftDiff) {  // The corner is on the right side
                    // turn right
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_RED);  // That arm with a larger difference lights up

                    animatronicsHelper.setTurnSpeed(turnSpeed);
                    animatronicsHelper.setTurnAngle(turnAngle);
                    animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                }

                hardWareManager.queryUltronicData();
                turnHandler.postDelayed(runnableAlignWithLiftBackWall, 2000);   // Check again
            }
        }
    };

    private Runnable runnableCheckDoorClosed = new Runnable() {
        @Override
        public void run() {
            int lArmBackIr = sensorState.get("infraRed_11");
            int rArmBackIr = sensorState.get("infraRed_14");

            if ( (lArmBackIr>0 && rArmBackIr>0) && (lArmBackIr<100 && rArmBackIr<100) ) {  // Both positive and less than 100 cm

                // The door is closed
                animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_PURPLE);
                liftDoorStateHandler.removeCallbacksAndMessages(null);

                // Align robot angle in lift
                alignInLiftHandler.post(runnableAlignInLiftAngle);

                timeoutHandler.post(runnableRestartTimeoutThread);
            } else {
                liftDoorStateHandler.postDelayed(runnableCheckDoorClosed, 1000);
            }

        }
    };
    private Runnable runnableAlignInLiftAngle = new Runnable() {
        @Override
        public void run() {

            int lSensor = sensorState.get("lidar_btm_left2_lsb");     // front left
            int rSensor = sensorState.get("lidar_btm_right2_lsb");    // front right
            int tolerance = 2;     // Allowable precision error for all sensors, cm

            int turnSpeed = 1;
            int turnAngle = 2;

            if (robotAlias.equals("ariel")) {
                turnAngle = 3;
            }

            if(lSensor>0 && rSensor>0) {     // Make sure we have a reading

                if ( Math.abs(lSensor-rSensor) < tolerance ) {
                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_GREEN);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_GREEN);
                    alignInLiftHandler.removeCallbacksAndMessages(null);

                    alignInLiftHandler.post(runnableAlignInLiftDistance);

                    timeoutHandler.post(runnableRestartTimeoutThread);
                } else {

                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_BLUE);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_BLUE);

                    // Which arm is further from the target?
                    if( (rSensor < lSensor) || (rSensor>0 && lSensor<0) ) {
                        // rotate right
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_RED);  // That arm with a larger difference lights up

                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    } else if ( (rSensor > lSensor) || (rSensor>0 && lSensor<0) ) {
                        // rotate left
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_RED);

                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);
                    } else if (rSensor < 0 && lSensor < 0) {
                        // rotate right
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_RED);  // That arm with a larger difference lights up

                        animatronicsHelper.setTurnSpeed(turnSpeed);
                        animatronicsHelper.setTurnAngle(turnAngle);
                        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
                    }

                    hardWareManager.queryUltronicData();
                    alignInLiftHandler.postDelayed(runnableAlignInLiftAngle, 2100);   // Check again

                }

            } else {
                alignInLiftHandler.postDelayed(runnableAlignInLiftAngle, 2100);   // Check again
            }

        }
    };
    private Runnable runnableAlignInLiftDistance = new Runnable() {
        @Override
        public void run() {
//            int lSensor = sensorState.get("infraRed_10");
//            int rSensor = sensorState.get("infraRed_13");

//            int lSensor = sensorState.get("lidar_left1_3d_lsb");
//            int rSensor = sensorState.get("lidar_right1_3d_lsb");

            int lSensor = sensorState.get("lidar_btm_left1_lsb");
            int rSensor = sensorState.get("lidar_btm_right1_lsb");

//            int a = 75; // Target distance rSensor from wall, cm
//            int b = 75; // Target distance lSensor from wall, cm

            int a = 70; // Target distance rSensor from wall, cm
            int b = 70; // Target distance lSensor from wall, cm

            int tolerance = 2;     // Allowable precision error per sensor, cm

            int stopDelay = 30; // Factor
            int reRunDelay = 1500; // Multiple
            int translationSpeed = 2;
            if(robotAlias.equalsIgnoreCase("ariel")) {
                stopDelay = 600;
                reRunDelay = 2400;
            }

            if(lSensor>0 && rSensor>0) {     // Make sure we have a reading

                // We're aligned with the lift doors
                if ( (lSensor > b-tolerance && lSensor < b+tolerance) && (rSensor > a-tolerance && rSensor < a+tolerance) ) {    // If we're within the tolerance ranges

                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_GREEN);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_GREEN);
                    alignInLiftHandler.removeCallbacksAndMessages(null);

                    turnHandler.post(runnableTurnAround);

                    timeoutHandler.post(runnableRestartTimeoutThread);
                } else {

                    animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_BLUE);
                    animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_BLUE);

                    if( rSensor-a < lSensor-b ) {      // Check which wall should be approached
                        // translate left
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_RED);

                        animatronicsHelper.setSpeed(translationSpeed);
                        animatronicsHelper.setDuration(100);
                        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_LEFT_TRANSLATION);
                        alignInLiftHandler.postDelayed(() -> {
                            stop();
                        }, stopDelay);

                    } else if ( rSensor-a > lSensor-b ) {
                        // translate right
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_RED);

                        animatronicsHelper.setSpeed(translationSpeed);
                        animatronicsHelper.setDuration(100);
                        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_RIGHT_TRANSLATION);
                        alignInLiftHandler.postDelayed(() -> {
                            stop();
                        }, stopDelay);

                    }
                    hardWareManager.queryUltronicData();                                      // Refresh the sensor data
                    alignInLiftHandler.postDelayed(runnableAlignInLiftDistance, reRunDelay);   // Check again after delay

                }

            } else {
                hardWareManager.queryUltronicData();
                alignInLiftHandler.postDelayed(runnableAlignInLiftDistance, reRunDelay);   // Check again after delay
            }

        }
    };
    private Runnable runnableExitLift = new Runnable() {
        @Override
        public void run() {
            int lArmFrontIr = sensorState.get("infraRed_9");
            int rArmFrontIr = sensorState.get("infraRed_12");

            if (lArmFrontIr<0 && rArmFrontIr<0 && exitBool) {    // If there's nothing in front of the arms sensors (doors open)
                animatronicsHelper.setSpeed(10);
                animatronicsHelper.setDistance(200);          // 2m
                animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);  // Motor

                exitLiftHandler.removeCallbacksAndMessages(null);
                exitBool = false;

                disembarkedHandler.postDelayed(runnableDisembarked, 3000);    // Delay for stability

                timeoutHandler.post(runnableRestartTimeoutThread);
            } else {
                new Thread(runnableNavGetExitBool).start();                      // Ask the nav-planner if we are on the right floor
                exitLiftHandler.postDelayed(runnableExitLift, 1000);   // Otherwise check again after 1s
            }

        }
    };
    private Runnable runnableDisembarked = new Runnable() {
        @Override
        public void run() {
            int liBtmBack = sensorState.get("lidar_btm_back_lsb");

            // Inform nav-planner to switch onboard value, calibrate map, and route after a check to see that the robot is in safe position outside of the elevator
            if ( (liBtmBack<0) || (liBtmBack>90) ) {
                // We're out of the elevator
                animatronicsHelper.lightLed(animatronicsHelper.LED_ALL, animatronicsHelper.LED_BLUE);

                // Inform nav-planner that sanbot is no longer onboard
                new Thread(runnableNavDisembarked).start();

                disembarkedHandler.removeCallbacksAndMessages(null);

                mapCalHandler.post(runnableMapCal);

                timeoutHandler.post(runnableRestartTimeoutThread);
            } else {
                disembarkedHandler.postDelayed(runnableDisembarked, 1000);
            }
        }
    };
    private Runnable runnableMapCal = new Runnable() {
        @Override
        public void run() {
            DocumentReference docRef = db.collection(refPath).document(deviceId).collection("navigation").document("maps");
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("Tai", TAG + ": runnableMapCal: DocumentSnapshot data: " + document.getData());
                            if( document.getData().get(mapName).toString().equals("1") ) {   // If our map has changed
                                mapCalHandler.removeCallbacksAndMessages(null);
                                Log.d("Tai", TAG + ": mapsListener: mapName: " + mapName);

                                // Calibrate to lift
                                new Thread(runnableNavMapCal).start();

                                timeoutHandler.post(runnableRestartTimeoutThread);
                            } else {
                                mapCalHandler.postDelayed(runnableMapCal, 1000);
                            }
                        } else {
                            Log.d("Tai", TAG + "No such document");
                        }
                    } else {
                        Log.d("Tai", TAG + "get failed with ", task.getException());
                    }
                }
            });
        }
    };
    private Runnable runnableGoToTag = new Runnable() {
        @Override
        public void run() {

            // First check if our calibration has gone through
            DocumentReference docRef = db.collection(refPath).document(deviceId).collection("navigation").document("calibrate");
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@androidx.annotation.NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d("Tai", TAG + ": runnableGoToTag: DocumentSnapshot data: " + document.getData());
                            if(document.getData().get("status").toString().equals("1")) {
                                docRef.update("status", "0");

                                // Then we can route to dest tag
                                DocumentReference docRef = db.collection(refPath).document(deviceId).collection("navigation").document("dest");
                                docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {
                                            DocumentSnapshot document = task.getResult();
                                            if (document.exists()) {
                                                Log.d("Tai", TAG + "runnableGoToTag: DocumentSnapshot data: " + document.getData());

                                                String tagName = document.getData().get("destination").toString();

                                                // Ask nav-planner to route to tag
                                                tName = tagName;
                                                new Thread(runnableNavGoToTag).start();

                                                timeoutHandler.post(runnableRestartTimeoutThread);
                                            } else {
                                                Log.d(TAG, "No such document");
                                            }
                                        } else {
                                            Log.d(TAG, "get failed with ", task.getException());
                                        }
                                    }
                                });

                            } else {
                                Log.d("Tai", TAG + ": get calibrate doc: Waiting for nav-planner action");
                                goToTagHandler.postDelayed(runnableGoToTag, 1000);
                            }
                        } else {
                            Log.e("Tai", TAG + ": get calibrate doc: No such document");
                            goToTagHandler.postDelayed(runnableGoToTag, 1000);
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });

        }
    };
    private boolean doorSwitchClosed = false;
    Handler lockHandler = new Handler();
    private Runnable runnableWaitForPickup = new Runnable() {
        @Override
        public void run() {
            btnOpenBox.setVisibility(View.INVISIBLE);
            hardWareManager.queryGravityData();

            if (!doorSwitchClosed) {
                // Ask the user to close the door
            }

            // Check for box door closure or (food removal + box door closure)
            if ( (locked && doorSwitchClosed) || (!locked && doorSwitchClosed && shouldLockDoor) ) {
                sendMessageToArduinoService(ArduinoService.MSG_SEND_TO_SERIAL, ArduinoService.ARG_SEND_1, 0, null);
                locked = true;

                mainPresenter.speak("Beep... Beep... Beep... I'm reversing!");
                lockHandler.postDelayed(() -> {

                    // Move away from the door
                    animatronicsHelper.setSpeed(10);
                    animatronicsHelper.setDistance(30);
                    animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_BACK);

                }, 2000);

                // Route back to kitchen.
                lockHandler.postDelayed(() -> {
                    new Thread(runnableNavDelivered).start();
                }, 2150);

            } else {
                // run again
                pickupHandler.postDelayed(runnableWaitForPickup, 100);   // Frequency of the Arduino serial
            }

        }
    };

    // Indefinite runnables
    private Runnable runnableSensorRefresh = new Runnable() {
        @Override
        public void run() {
            Log.i(DEBUG_TAG, ": sensorStateRefresh: " + sensorState);
            // Display current data in text views
            try {
                StringBuilder str = new StringBuilder("\n");
                for (int i=9; i<17; i++) {
                    str.append("IR" + i + ": ");
                    str.append(sensorState.get("infraRed_" + i).toString());
                    str.append("\n");
                }

                tvIrSensors.setText(str.toString());

                str = new StringBuilder("\n");
                String[] strs = {
                        "btm_front_lsb",
                        "btm_back_lsb",
                        "btm_left1_lsb",
                        "btm_left2_lsb",
                        "btm_right1_lsb",
                        "btm_right2_lsb",
                        "left1_3d_lsb",
                        "left2_3d_lsb",
                        "left3_3d_lsb",
                        "left4_3d_lsb",
                        "right1_3d_lsb",
                        "right2_3d_lsb",
                        "right3_3d_lsb",
                        "right4_3d_lsb",
                        "top_3d_lsb",
                        "top_left_front_lsb",
                        "top_right_front_lsb"
                };
                for (String dispStr : strs) {
                    str.append("Li_" + dispStr + ": ");
                    str.append(sensorState.get("lidar_" + dispStr).toString());
                    str.append("\n");
                }
                tvLidarSensors.setText(str.toString());
            } catch (NullPointerException e) {
                Log.e("Tai", TAG + ": sensorRefresh exception: " + e);
                e.printStackTrace();
            }

            // Query new sensor data (trigger the rawData listener)
            hardWareManager.queryUltronicData();

            // Run again
            sensorRefreshHandler.postDelayed(runnableSensorRefresh, 500);
        }
    };
    private boolean isConnected;
    private Runnable runnableConnectionStatus = new Runnable() {
        @Override
        public void run() {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                runOnUiThread(() -> {
                    tvConnection.setText("Connected");
                    tvConnection.setTextColor(getResources().getColor(R.color.Green));
                });
            } else {
                runOnUiThread(() -> {
                    tvConnection.setText("Not connected");
                    tvConnection.setTextColor(getResources().getColor(R.color.Red));
                });
            }

            connHandler.postDelayed(runnableConnectionStatus, 200);

        }
    };
    private Thread timeoutThread;
    private Runnable runnableRestartTimeoutThread = new Runnable() {
        @Override
        public void run() {
            Log.i("Tai", TAG + ": Restarting timer");
            if(timeoutThread != null) {
                timeoutThread.interrupt();
            }
            timeoutThread = new Thread(() -> {
                try {
                    int minutesToTimeout = 5;
                    statusTvDisplay("Action status");
                    Thread.sleep(60 * minutesToTimeout * 1000);
                    Log.e("Tai", TAG + ": Operation timed out");
                    enterLiftHandler.removeCallbacksAndMessages(null);
                    exitLiftHandler.removeCallbacksAndMessages(null);
                    liftDoorStateHandler.removeCallbacksAndMessages(null);
                    disembarkedHandler.removeCallbacksAndMessages(null);
                    mapCalHandler.removeCallbacksAndMessages(null);
                    goToTagHandler.removeCallbacksAndMessages(null);
                    alignInLiftHandler.removeCallbacksAndMessages(null);
                    timeoutHandler.removeCallbacksAndMessages(null);

                    // Send stuck message to nav-planner
                    new Thread(runnableNavStuck).start();

                } catch (InterruptedException e) {
                    Log.i("Tai", TAG + ": Timeout interruption: " + e);
                }
            });
            timeoutThread.start();
        }
    };

    // Nav-planner api runnables
    private String tName;
    private int retryCount = 0;
    private int retryLimit = 8000;
    private boolean exitBool = false;
    private Runnable runnableNavArrived = new Runnable() {
        @Override
        public void run() {

            try {
                // capture the start time
                long startTime = System.nanoTime();

                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("tagName", tName);
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "arrived");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;

                    if (serverMsg.equals("Called elevator")) {
                        // Start watching for the door and enter lift
                        enterLiftHandler.post(runnableEnterLift);
                    } else if (serverMsg.equals("Waiting for pickup")) {
                        // Pop up button to open lock
                        runOnUiThread(() -> {
                            btnOpenBox.setVisibility(View.VISIBLE);
                        });
                        sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "Waiting...");
                    } else if (serverMsg.equals("Called elevator 2")) {
                        enterLiftHandler.post(runnableAlignWithLiftDoor);
                    } else {
                        Log.e("Tai", TAG + ": runnableNavArrived: Unknown nav-planner response");
                    }

                } else {
                    Log.e("Tai", TAG + ": runnableNavArrived: Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavArrived: retryCount: " + retryCount);
                        statusTvDisplay("retryCountArrived: " + retryCount);
                        new Thread(runnableNavArrived).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavArrived: retryCount: " + retryCount);
                        new Thread(runnableNavArrived).start();
                    }
                }

                // capture the method execution end time
                long endTime = System.nanoTime();
                // dump the execution time out
                Log.i("API_TIMER", String.valueOf((endTime -
                        startTime)/1000000));
            } catch (JSONException e) {
                Log.e("Tai", TAG + ": navArrived exception: " + e);
            }

        }
    };
    private Runnable runnableNavDisembarked = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "disembarked");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavDisembarked: retryCount: " + retryCount);
                        statusTvDisplay("retryCountDisembarked: " + retryCount);
                        new Thread(runnableNavDisembarked).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavDisembarked: retryCount: " + retryCount);
                        new Thread(runnableNavDisembarked).start();
                    }
                }

            } catch (JSONException e) {
                Log.e("Tai", TAG + ": navDisembarked exception: " + e);
            }
        }
    };
    private Runnable runnableNavEnterLift = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "boarded");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                    if (serverMsg.equals("Onboard 1")) {
                        // Start checking if the doors have closed
                        liftDoorStateHandler.post(runnableCheckDoorClosed);
                    }
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavEnterLift: retryCount: " + retryCount);
                        statusTvDisplay("retryCountEnterLift: " + retryCount);
                        new Thread(runnableNavEnterLift).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavEnterLift: retryCount: " + retryCount);
                        new Thread(runnableNavEnterLift).start();
                    }
                }

            } catch (JSONException e) {
                Log.e("Tai", TAG + ": navEnterLift exception: " + e);
            }
        }
    };
    private Runnable runnableNavChangeMap = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "changeMap");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                    if (serverMsg.equals("Change map")) {
                        // Start checking if the doors have closed
                        assert res != null;
                        JSONObject mapData = res.getJSONObject("data");
                        mapName = mapData.get("mapName").toString();   // We need to listen to this firebase key's value to trigger calibration
                    }
                } else {
                    Log.w("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavCheckDoorClosed: retryCount: " + retryCount);
                        statusTvDisplay("retryCountCheckDoorClosed: " + retryCount);
                        new Thread(runnableNavChangeMap).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavCheckDoorClosed: retryCount: " + retryCount);
                        new Thread(runnableNavChangeMap).start();
                    }
                }
            } catch (JSONException e) {
                Log.e("Tai", TAG + ": checkDoorClosed exception: " + e);
            }
        }
    };
    private Runnable runnableNavMapCal = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);
                data.put("position", "lift");

                postData.put("command", "calibrate");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                    if (serverMsg.equals("Req calibrate")) {
                        // Start checking if the calibration is done and goto
                        goToTagHandler.post(runnableGoToTag);
                    }
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavMapCal: retryCount: " + retryCount);
                        statusTvDisplay("retryCountMapCal: " + retryCount);
                        new Thread(runnableNavMapCal).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavMapCal: retryCount: " + retryCount);
                        new Thread(runnableNavMapCal).start();
                    }
                }

            } catch (JSONException e) {
                Log.e("Tai", TAG + ": navMapCal exception: " + e);
            }
        }
    };
    private Runnable runnableNavGoToTag = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);
                data.put("endPoint", tName);

                postData.put("command", "route");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                    if (serverMsg.equals("Route")) {
                        // ...
                    }
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavGoToTag: retryCount: " + retryCount);
                        statusTvDisplay("retryCountGoToTag: " + retryCount);
                        new Thread(runnableNavGoToTag).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavGoToTag: retryCount: " + retryCount);
                        new Thread(runnableNavGoToTag).start();
                    }
                }

            } catch (JSONException e) {
                Log.e("Tai", TAG + ": navGoToTag exception: " + e);
            }
        }
    };
    private Runnable runnableNavCalSuccess = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);
                data.put("tagName", tName);

                postData.put("command", "calStatusSuccess");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavCalSuccess: retryCount: " + retryCount);
                        statusTvDisplay("retryCountCalSuccess: " + retryCount);
                        new Thread(runnableNavCalSuccess).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavCalSuccess: retryCount: " + retryCount);
                        new Thread(runnableNavCalSuccess).start();
                    }
                }

            } catch (JSONException err) {
                Log.e("Tai", TAG + ": navCalSuccess exception: " + err);
            }
        }
    };
    private Runnable runnableNavStuck = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "stuck");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavStuck: retryCount: " + retryCount);
                        statusTvDisplay("retryCountStuck: " + retryCount);
                        new Thread(runnableNavStuck).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavStuck: retryCount: " + retryCount);
                        new Thread(runnableNavStuck).start();
                    }
                }

            } catch (JSONException err) {
                Log.e("Tai", TAG + ": navStuck exception: " + err);
            }
        }
    };
    private Runnable runnableNavDelivered = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "delivered");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavDelivered: retryCount: " + retryCount);
                        statusTvDisplay("retryCountDelivered: " + retryCount);
                        new Thread(runnableNavDelivered).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavDelivered: retryCount: " + retryCount);
                        new Thread(runnableNavDelivered).start();
                    }
                }

            } catch (JSONException err) {
                Log.e("Tai", TAG + ": navStuck exception: " + err);
            }
        }
    };

    private Runnable runnableNavGetExitBool = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "exit");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                    retryCount = 0;
                    if (serverMsg.equals("Reached target floor")) {
                        exitBool = true;
                    } else {
                        exitBool = false;
                    }
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                    retryCount += 1;
                    if (!isConnected && retryCount < retryLimit) {  // Try again if there's no connection
                        Log.i("Tai", ": runnableNavDelivered: retryCount: " + retryCount);
                        statusTvDisplay("retryCountExitBool: " + retryCount);
                        new Thread(runnableNavGetExitBool).start();
                    } else if (isConnected) {
                        Log.i("Tai", ": runnableNavGetExitBool: retryCount: " + retryCount);
                        new Thread(runnableNavGetExitBool).start();
                    }
                }

            } catch (JSONException err) {
                Log.e("Tai", TAG + ": navStuck exception: " + err);
            }
        }
    };
    private Runnable runnableNavResetElevatorVals = new Runnable() {
        @Override
        public void run() {
            try {
                JSONObject postData = new JSONObject();
                JSONObject res;

                JSONObject data = new JSONObject();
                data.put("refPath", refPath);
                data.put("deviceId", deviceId);

                postData.put("command", "reset");
                postData.put("data", data);

                res = post(postData);
                String serverMsg = handleResponse(res);

                if (serverMsg != null) {
                    Log.i("Tai", "Got nav-planner response: " + serverMsg);
                } else {
                    Log.i("Tai", "Did not receive nav-planner response");
                }

            } catch (JSONException err) {
                Log.e("Tai", TAG + ": navStuck exception: " + err);
            }
        }
    };

    /** Messenger for communicating with the service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
    /** Some text view we are using to show state information. */
    TextView tvArduinoServiceCallback;

    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ArduinoService.MSG_SET_VALUE:
                    tvArduinoServiceCallback.setText("Received from service: " + msg.arg1);
                    break;
                case ArduinoService.MSG_FOO:
                    tvArduinoServiceCallback.setText("Received from service: " + msg.obj);
                    break;
                case ArduinoService.MSG_TV_DISPLAY:
                    Bundle bundle = (Bundle) msg.obj;
                    arduinoTvDisplay(bundle.getString("textViewMessage"));
                    break;
                case ArduinoService.MSG_SET_BOOLEAN_VALUE:
                    if (msg.arg1 == ArduinoService.ARG_DOOR_SWITCH_CLOSED) {
                        switch (msg.arg2) {
                            case 0:
                                doorSwitchClosed = false;
                                break;
                            case 1:
                                doorSwitchClosed = true;
                                break;
                        }
                    } else {
                        Log.i("Tai", TAG + ": handleMessage: unexpected argument");
                    }
                    break;
                case ArduinoService.MSG_ARDUINO_DETACHED:
                    Log.i("Tai", TAG + ": handleMessage: arduino detached");
                    new Handler().postDelayed(() -> {
                        startService(arduinoServiceIntent);
                        bindToArduinoService();
                    }, 1000);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mIsBound = true;

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        ArduinoService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);

                // Give it some value as an example.
                msg = Message.obtain(null,
                        ArduinoService.MSG_SET_VALUE, this.hashCode(), 0);
                mService.send(msg);
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }

            // As part of the sample, tell the user what happened.
//            Toast.makeText(Binding.this, R.string.remote_service_connected,
//                    Toast.LENGTH_SHORT).show();

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mIsBound = false;
            tvArduinoServiceCallback.setText("Disconnected.");
        }
    };

    void bindToArduinoService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(this,
                ArduinoService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        tvArduinoServiceCallback.setText("Binding.");
    }
    void unbindFromArduinoService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null,
                            ArduinoService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }

            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            tvArduinoServiceCallback.setText("Unbinding.");
        }
    }
    public void sendMessageToArduinoService(int mWhat, int mArg1, int mArg2, Object mObj) {
        if (!mIsBound) return;
        // Create and send a message to the service, using a supported 'what' value
        Message msg = Message.obtain(null, mWhat, mArg1, mArg2, mObj);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /** -------------- Lifecycle ----------------*/
    @Override
    protected void onMainServiceConnected() {
        systemManager.switchFloatBar(false, SlamActivity.class.getName());
        Log.d("Tai", TAG + ": mainServiceConnected");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("Tai", TAG + ": onDestroy");
        sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "-");
        mainPresenter.stop();
    }

    /** -------------- Service hooks ----------------*/
    private void doActivity(Map<String, Object> robotState) {
        JSONObject robotStateJson = new JSONObject(robotState);
        try {
            JSONObject activityValues = robotStateJson.getJSONObject("activityValues");
            String activity = "delivery";

            if(activityValues.getInt(activity) == 0 && refPath != null) {
                Log.i("Tai", TAG + "doActivity: Finishing activity: " + activity);
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

        DocumentReference docRef = db.collection(refPath).document(deviceId).collection("navigation").document("calibrate");
        docRef.update("status", 0);

        sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "Ready");

        // Arduino service
        startService(arduinoServiceIntent);
        bindToArduinoService();
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Tai", TAG + ": onStop");
        EventBus.getDefault().unregister(this);

        mainPresenter.cancelGuide();
        regMapsDoc.remove();
        regTagsDoc.remove();
        resetTagsState();

        sensorRefreshHandler.removeCallbacksAndMessages(null);
        enterLiftHandler.removeCallbacksAndMessages(null);
        timeoutHandler.removeCallbacksAndMessages(null);

        sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "-");
        new Thread(runnableNavResetElevatorVals).start();

        // Kill Arduino service
        unbindFromArduinoService();
        if (arduinoServiceIntent != null) {stopService(arduinoServiceIntent);}
    }
    @Subscribe
    public void onEvent(MessageEvent event) {
        robotState = event.getMessage();
        Log.i("Tai", TAG + ": onEvent: robotState: " + robotState);
        doActivity(robotState);
    }

    /** -------------- SlamPresenter.IView ----------------*/
    @Override
    public void showTags(List<PositionTag> list) {
        this.tagsList = list;
        Map<String, Integer> tagData = new HashMap<>();
        for(PositionTag tag : tagsList) {              // For each tag in the list
//            Log.d("Tai", TAG + ": showTags: " + tag.getName().equals("start"));
            tagData.put(tag.getName(), 0);
//            if (tag.getName().equals("start")) {
//                setCalibrationPoint(tag.getName());
//            }
        }

        db = FirebaseFirestore.getInstance();
        String collectionPath = "navigation";
        String docPath = "tags";
        tagsDoc = db.collection(refPath).document(deviceId).collection(collectionPath).document(docPath);
        tagsDoc.set(tagData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Tai", TAG + ": showTags: " + "set tags success");
            }
        });

    }
    @Override
    public void showMainV() {

    }
    @Override
    public void showGuideV() {

    }
    @Override
    public void showNoTagsV() {

    }
    @Override
    public void showNoMapV() {

    }
    @Override
    public void showMap(Bitmap bitmap, List<PositionTag> tags) {
        try {
            Log.d("Tai", TAG + ": showMap: tags exist: " + !tags.isEmpty());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // Stop loading icon
                    progressBar.setVisibility(View.INVISIBLE);

                    mapView.invalidate();
                    mapView.setupMapView(bitmap, tags);
                    uploadBitmap(loadBitmapFromView(mapView));

                }
            });
        } catch (NullPointerException e) {
            Log.e("Tai", TAG + ": showMap: exception: " + e);
        }
    }
    @Override
    public void updatePosition(double x, double y, double rad) {
        final double angle=Math.toDegrees(rad);
//        Log.d("Tai", TAG + ": updatePosition: " + "x: " + x + " y: " + y + " theta: " + angle);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvPos.setText("Position: \nx = " + x + "\ny = " + y + "\nrad = " + rad);
                mapView.setPosition((float)x,(float)y,(float)angle);
                uploadBitmap(loadBitmapFromView(mapView));
            }
        });

    }
    @Override
    public void showProgress(String title, String message) {

    }
    @Override
    public void closeProgressDialog() {

    }
    @Override
    public void showGuideHint(String s) {

    }
    @Override
    public void toast(String s) {

    }
    @Override
    public void showNaviError(String s) {

    }
    @Override
    public void showMoveError(String s) {

    }
    @Override
    public void finishActivity() {

    }
    @Override
    public void showCheckDialog(List<PositionTag> tagList) {

    }
    @Override
    public void setMaps(Map<String, Integer> mapData) {
        mapsDoc.set(mapData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mainPresenter.loadMap();
            }
        });
    }
    @Override
    public void resetTagsListener() {
        resetTagsState();
    }
    @Override
    public void navArrived(String tagName) {
        Log.i("Tai", TAG + ": navArrived: " + tagName);
        tName = tagName;
        // Inform nav-planner that sanbot has arrived at a tag
        new Thread(runnableNavArrived).start();
        timeoutHandler.post(runnableRestartTimeoutThread);
    }
    @Override
    public void updateCalStatus() {
        DocumentReference docRef = db.collection(refPath).document(deviceId).collection("navigation").document("calibrate");
        docRef.update("status", 0);
        sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "Ready");
    }
    @Override
    public void pipeNavigationError(int navi) {
        String navError = ExplainCode.getNavi(this.getApplicationContext(), navi);
        Log.i("Tai", TAG + ": pipeNavigationError:  " + navError);
        sendMessageToFirebaseDoc("SlamActivity", "navStatus", navError);
        if (navi == Msg.NaviState.NO_INIT) {
            sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "Calibration needed.");
        }
    }

    /** -------------- Gesture listeners ----------------*/
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }
    @Override
    public boolean onDown(MotionEvent e) {
        Log.d(DEBUG_TAG,"onDown: " + e.toString());
        return true;
    }
    private boolean hide = true;
    private int flings = 0;
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
        flings += 1;
        if (hide && flings > 3) {
            mapView.setVisibility(View.INVISIBLE);
            tvIrSensors.setVisibility(View.INVISIBLE);
            tvLidarSensors.setVisibility(View.INVISIBLE);
            btnEnter.setVisibility(View.INVISIBLE);
            btnExit.setVisibility(View.INVISIBLE);
            btnAlign.setVisibility(View.INVISIBLE);
            btnAlignLr.setVisibility(View.INVISIBLE);
            btnOpenBox.setVisibility(View.INVISIBLE);
            tvArduino.setVisibility(View.INVISIBLE);
            tvPos.setVisibility(View.INVISIBLE);
            tvStatus.setVisibility(View.INVISIBLE);
            tvConnection.setVisibility(View.INVISIBLE);
            tvPose.setVisibility(View.INVISIBLE);
            tvArduino.setVisibility(View.INVISIBLE);
            tvArduinoServiceCallback.setVisibility(View.INVISIBLE);
            hide = false;
            flings = 0;
        } else if (!hide && flings > 3) {
            mapView.setVisibility(View.VISIBLE);
            tvIrSensors.setVisibility(View.VISIBLE);
            tvLidarSensors.setVisibility(View.VISIBLE);
            btnEnter.setVisibility(View.VISIBLE);
            btnExit.setVisibility(View.VISIBLE);
            btnAlign.setVisibility(View.VISIBLE);
            btnAlignLr.setVisibility(View.VISIBLE);
            tvArduino.setVisibility(View.VISIBLE);
            tvPos.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.VISIBLE);
            tvConnection.setVisibility(View.VISIBLE);
            btnOpenBox.setVisibility(View.VISIBLE);
            hide = true;
            flings = 0;
        }
        return true;
    }
    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }
    @Override
    public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX,
                            float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: " + event1.toString() + event2.toString());
        return true;
    }
    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }
    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());
        return true;
    }
    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }
    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    // Helper methods
    Map<String, Integer> sensorState;
    private void initSensorState() {
        sensorState = new HashMap<>();
        for(int i = 1; i < 25; i++) {
            sensorState.put("infraRed_" + String.valueOf(i + 1), 67);
        }
    }
    public void stop() {
        DistanceWheelMotion distanceWheelMotion = new DistanceWheelMotion(DistanceWheelMotion.ACTION_STOP_RUN, 5, 100);
        wheelMotionManager.doDistanceMotion(distanceWheelMotion);
    }     // stop wheels movement
    public static void loadLibrary(String libName) {
        System.loadLibrary(libName);
        Log.d("Tai", TAG + ": loadLibrary: " + libName + " loaded");
    }     // load slam libs
    private void sendMessageToFirebaseDoc(String doc, String field, String msg) {
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

    // API requests
    private JSONObject post(JSONObject data) throws JSONException {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        String url =  "https://3i0al4myn6.execute-api.us-east-1.amazonaws.com/prod";
        try {
            JsonObjectRequest request = new JsonObjectRequest(url, data, future, future);
            request.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));                       //This ensures that the request does not timeout before a response is received
            mQueue.add(request);
            JSONObject response = future.get(); // this will block
            return response;
        } catch (InterruptedException e) {
            // exception handling
            Log.e("Tai", "postCommand: Interrupted exception: " + e);
            e.printStackTrace();
        } catch (ExecutionException e) {
            // exception handling
            Log.e("Tai", "post: Execution exception: " + e);
            e.printStackTrace();
        }


        return null;
    }
    private String handleResponse(JSONObject response) {
        final String[] serverMsg = new String[1];
        int serverStatus;
        try {
            serverStatus = response.getInt("status");
            Log.i("Tai", "handleResponse: serverStatus: " + serverStatus);
            switch (serverStatus) {
                case 200:
                case 201:
                case 202:
                case 203:
                case 204:
                case 205:
                case 206:
                case 207:
                case 208:
                case 209:
                case 210:
                case 301:
                    serverMsg[0] = response.getString("message");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "serverMsg: " + serverMsg[0], Toast.LENGTH_SHORT).show());
                    return serverMsg[0];
                default:
                    Log.d("Tai", TAG + ": handleResponse: unexpected server response");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Unexpected server response", Toast.LENGTH_LONG).show());
                    return null;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Tai", "handleResponse: JSON Exception: " + e);
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e("Tai", "handleResponse: Null pointer exception: " + e);
        }
        return null;
    }

    // Firebase controls
    private String REQ_CHANGE_MAP = "2";
    private final String REQ_GOTO_TAG = "2";
    private final String REQ_CALIBRATION_TAG = "3";
    private void initDbListeners() {
        /* This function should only be called once */
        db = FirebaseFirestore.getInstance();
        String collectionPath = "navigation";
        String docPath = "maps";
        mapsDoc = db.collection(refPath).document(deviceId).collection(collectionPath).document(docPath);
        regMapsDoc = mapsDoc.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String ref = "mapsDoc";
                if (e != null) {
                    Log.w("Tai", TAG + ": " + ref + ": Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Got data snapshot");
                    Map<String, Object> mapNames = documentSnapshot.getData();
                    Iterator it = mapNames.entrySet().iterator();         // Convert the HashMap so we can loop through its elements
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        if(pair.getValue().toString().equals(REQ_CHANGE_MAP)) {
                            mainPresenter.switchMap(pair.getKey().toString());
                            runOnUiThread(() -> {
                                // Play loading icon
                                Toast.makeText(SlamActivity.this, "Changing map", Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.VISIBLE);
                            });
                            it.remove();
                            break;
                        }
                        it.remove(); // avoids a ConcurrentModificationException
                    }

                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });

        collectionPath = "navigation";
        docPath = "tags";
        tagsDoc = db.collection(refPath).document(deviceId).collection(collectionPath).document(docPath);
        regTagsDoc = tagsDoc.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            /**
             * Checks firebase for commands concerning tags
             * */
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                String ref = "tagsDoc";
                if (e != null) {
                    Log.w("Tai", TAG + ": " + ref + ": Listen failed.", e);
                    return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Got data snapshot");
                    Map<String, Object> mp = documentSnapshot.getData();
                    Iterator it = mp.entrySet().iterator();         // Convert the HashMap so we can loop through its elements
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry)it.next();
                        String key = pair.getKey().toString();
                        String value = pair.getValue().toString();

                        if(value.equals(REQ_CALIBRATION_TAG)) {
                            // Toast
                            runOnUiThread(()->{
                                Toast.makeText(SlamActivity.this, "Calibrate: " + key, Toast.LENGTH_LONG).show();
                                progressBar.setVisibility(View.VISIBLE);
                            });

                            setCalibrationPoint(key);
                            it.remove();

                            // Inform nav-planner that calibration has succeeded
                            tName = key;
                            new Thread(runnableNavCalSuccess).start();

                            break;
                        }
                        if(value.equals(REQ_GOTO_TAG)) {
                            goToTag(key);
                            resetTagsState();
                            it.remove();
                            break;
                        }
                        it.remove(); // avoids a ConcurrentModificationException
                    }

                } else {
                    Log.d("Tai", TAG + ": SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });

    }
    private void setCalibrationPoint(String calibrationPoint) {
        PositionTag calibrationTag = null;
        for(PositionTag tag : tagsList) {              // For each tag in the list
            if (tag.getName().equals(calibrationPoint)) {
                calibrationTag = tag;
            }
        }
        if (calibrationTag != null) {
            PositionTag finalCalibrationTag = calibrationTag;
            mainPresenter.getwHandler().post(() -> {
                mainPresenter.getMapClient().adjustPosition(finalCalibrationTag.getX(), finalCalibrationTag.getY(),finalCalibrationTag.getRadians());
                resetTagsState();
                sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "Calibrated");
            });
        } else {
            Log.e("Tai", TAG + ": setCalibrationPoint: " + "calibration tag not loaded");
        }

    }
    private void resetTagsState() {

        runOnUiThread(()->{
            Toast.makeText(SlamActivity.this, "Reset tags state", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.INVISIBLE);
        });

        Map<String, Integer> tagData = new HashMap<>();
        for(PositionTag tag : tagsList) {              // For each tag in the list
//            Log.d("Tai", TAG + ": showTags: " + tag.getName());
            tagData.put(tag.getName(), 0);
        }
        db = FirebaseFirestore.getInstance();
        String collectionPath = "navigation";
        String docPath = "tags";
        tagsDoc = db.collection(refPath).document(deviceId).collection(collectionPath).document(docPath);
        tagsDoc.set(tagData).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Tai", TAG + ": resetTagsState: " + "success");
            }
        });
    }
    JSONObject liftTag;
    double x, y, rad;
    private void goToTag(String goToPoint) {
        PositionTag goToTag = null;
        for(PositionTag tag : tagsList) {              // For each tag in the list
            if (tag.getName().equals(goToPoint)) {
                goToTag = tag;
            }
        }
        if (goToTag != null) {
            if (goToTag.getName().equals("lift")) {
                // set the lift tag coordinates to those specified by "lift" + floorNo in firebase
                DocumentReference refMaps = db.collection(refPath).document(deviceId).collection("navigation").document("maps");
                final int[] floorNo = {404};
                PositionTag finalGoToTag1 = goToTag;
                refMaps.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@androidx.annotation.NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                                Map<String, Object> mp = document.getData();
                                Iterator it = mp.entrySet().iterator();         // Convert the HashMap so we can loop through its elements
                                while (it.hasNext()) {
                                    Map.Entry pair = (Map.Entry)it.next();
                                    String key = pair.getKey().toString();
                                    String value = pair.getValue().toString();

                                    if(value.equals("1")) {       // If it is the current map
                                        floorNo[0] = Integer.parseInt(String.valueOf(key.charAt(2)));
                                        String liftField = "lift" + floorNo[0];

                                        DocumentReference refLiftTags = db.collection(refPath).document(deviceId).collection("navigation").document("liftTags");
                                        refLiftTags.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@androidx.annotation.NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()) {
                                                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());

                                                        JSONObject allLiftTags = null;
                                                        try {
                                                            allLiftTags = new JSONObject(document.getData().toString());
                                                            liftTag = allLiftTags.getJSONObject(liftField);
                                                            Log.i("Tai", TAG + ": liftTag: " + liftTag);

                                                            x = Double.parseDouble(liftTag.get("x").toString());
                                                            y = Double.parseDouble(liftTag.get("y").toString());
                                                            rad = Double.parseDouble(liftTag.get("rad").toString());
                                                            finalGoToTag1.setX(x);
                                                            finalGoToTag1.setY(y);
                                                            finalGoToTag1.setRadians(rad);

                                                            PositionTag finalGoToTag = finalGoToTag1;
                                                            Log.i("Tai", TAG + ": " + "goToTag: " + finalGoToTag1.getName());
                                                            mainPresenter.guideTo(finalGoToTag);
                                                        } catch (JSONException e) {
                                                            e.printStackTrace();
                                                        }

                                                    } else {
                                                        Log.d("Tai", TAG + ": refLiftTags: No such document");
                                                    }
                                                } else {
                                                    Log.d("Tai", TAG + ": refLiftTags: get failed with ", task.getException());
                                                }
                                            }
                                        });

                                        it.remove();
                                        break;
                                    }
                                    it.remove(); // avoids a ConcurrentModificationException
                                }
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                    }
                });
            } else {
                PositionTag finalGoToTag = goToTag;
                Log.i("Tai", TAG + ": " + "goToTag: " + goToTag.getName());
                mainPresenter.guideTo(finalGoToTag);
            }
        } else {
            Log.e("Tai", TAG + ": goToTag: " + "goToTag not loaded");
            resetTagsState();
        }

    }

    // S3 uploads TODO: Use ftp instead of s3
    private TransferUtility transferUtility;
    private CognitoCachingCredentialsProvider credentials;
    private Thread transferThread;
    private void initS3Handler() {
        // AWS login
        credentials = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "<identity-pool-id>", // Identity pool ID
                Regions.US_EAST_1 // Region
        );
        AmazonS3 s3Client = new AmazonS3Client(credentials, Region.getRegion(Regions.US_EAST_1));
        transferUtility = TransferUtility.builder()
                .context(getApplicationContext())
                .s3Client(s3Client)
                .build();
        transferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(transferUtility != null && mapView != null) {
                    uploadBitmap(loadBitmapFromView(mapView));
                }
            }
        });
    }
    public void uploadBitmap(Bitmap bitmap) {
        try {
            String fName = "map.jpg";        // file name on disk
            storeImage(bitmap, fName);
            File tempFile = getOutputMediaFile(fName);
            upload(tempFile, fName);
        } catch (NullPointerException e) {
            Log.e("Tai", TAG + ": uploadBitmap: NullPointerException: " + e);
        }
    }
    private void storeImage(Bitmap bitmap, String fName) {
        // Writes bitmap to file @fName on disk
        String dir = Environment.getExternalStorageDirectory() + "/SLAM/IMG/" + "DCIM/";
        final File f = new File(dir);
        if (!f.exists()) {
            f.mkdirs();
        }
        File file = new File(f, fName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("Tai", TAG + ": storeImage" + "Failed file storage");
        }
    }
    private File getOutputMediaFile(String fName) {
        // Retrieves file @fName from disk
        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            return null;
        } else {
            File dir = new File(Environment.getExternalStorageDirectory() + "/SLAM/IMG/" + "DCIM/");
            File outputFile = new File(dir, fName);
            return outputFile;
        }
    }
    private void upload(File tempFile, String fName) {
//        Log.d("Tai", tempFile.getAbsolutePath());
        TransferObserver observer = transferUtility.upload(
                "slam-buffer/" + refPath + "/" + deviceId,         // The S3 bucket to upload to (match the path in firebase)
                fName,                                                     // The S3 key (filename) for the object to upload
                tempFile,                                                  // The local file to upload
                CannedAccessControlList.PublicRead                         // To make the file public
        );
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    Toast toast = Toast.makeText(getApplicationContext(), "Map update sent", Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }
            @Override
            public void onError(int id, Exception ex) {

            }
        });
    }
    public static Bitmap loadBitmapFromView(View v) {
        if (v.getMeasuredHeight() > 0 && v.getMeasuredWidth() > 0) {
            v.measure(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
            Bitmap b = Bitmap.createBitmap(v.getMeasuredWidth(), v.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
            v.draw(c);
            return b;
        } else {
            return null;
        }
    }

    // TextView helpers
    public void arduinoTvDisplay(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvArduino.append(message+"\n");
            }
        });
    }
    public void statusTvDisplay (final String message) {
        runOnUiThread(() -> {
            tvStatus.setText(message);
            tvStatus.setTextColor(getResources().getColor(R.color.Blue));
        });
    }
    public void poseTvDisplay (final String message) {
        runOnUiThread(() -> {
            tvPose.setText(message);
            tvPose.setTextColor(getResources().getColor(R.color.WhiteSmoke));
        });
    }
    private String capitalizeFirstLetter(String str) {
        String cap = str.substring(0, 1).toUpperCase() + str.substring(1);
        return cap;
    }

    // TTS engine
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
                    if (robotAlias.equalsIgnoreCase("ariel")) {
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HEAD, animatronicsHelper.LED_YELLOW);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HEAD, animatronicsHelper.LED_YELLOW);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_YELLOW);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_YELLOW);
                    } else if (robotAlias.equalsIgnoreCase("lexi")) {
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HEAD, animatronicsHelper.LED_PURPLE);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HEAD, animatronicsHelper.LED_PURPLE);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_PURPLE);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_PURPLE);
                    } else if (robotAlias.equalsIgnoreCase("micah")) {
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HEAD, animatronicsHelper.LED_PINK);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HEAD, animatronicsHelper.LED_PINK);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_RIGHT_HAND, animatronicsHelper.LED_PINK);
                        animatronicsHelper.lightLed(animatronicsHelper.LED_LEFT_HAND, animatronicsHelper.LED_PINK);
                    }
                    mainPresenter.speak("Initiated");
                }

                // TTS methods
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        systemManager.showEmotion(EmotionsType.SPEAK);
                    }
                    @Override
                    public void onDone(String utteranceId) {

                    }
                    @Override
                    public void onError(String utteranceId) {
                        Log.d("Tai", "TTS error");
                    }
                });

            }

        });
    }

}
