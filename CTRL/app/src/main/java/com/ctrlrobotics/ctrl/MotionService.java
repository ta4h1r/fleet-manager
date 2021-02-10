package com.ctrlrobotics.ctrl;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.sanbot.opensdk.base.BindBaseService;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.headmotion.DRelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.DistanceWheelMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.RelativeAngleWheelMotion;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.ProjectorManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;

import java.util.HashMap;
import java.util.Map;

public class MotionService extends BindBaseService {

    private final static String TAG = "MotionService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public FirebaseFirestore db;
    public DocumentReference robotRef;

    private DocumentReference refF;
    private DocumentReference refL;
    private DocumentReference refR;
    private DocumentReference refS;
    private DocumentReference refLight;
    private DocumentReference refPrjctr;
    private DocumentReference refHeadUp;
    private DocumentReference refHeadDown;
    private DocumentReference refHeadLeft;
    private DocumentReference refHeadRight;
    private DocumentReference refHeadReset;

    private ListenerRegistration regF;
    private ListenerRegistration regL;
    private ListenerRegistration regR;
    private ListenerRegistration regS;
    private ListenerRegistration regLight;
    private ListenerRegistration regPrjctr;
    private ListenerRegistration regHeadUp;
    private ListenerRegistration regHeadDown;
    private ListenerRegistration regHeadLeft;
    private ListenerRegistration regHeadRight;
    private ListenerRegistration regHeadReset;


    private WheelMotionManager wheelMotionManager;
    private ModularMotionManager modularMotionManager;
    private HardWareManager hardWareManager;
    private ProjectorManager projectorManager;
    private HeadMotionManager headMotionManager;

    private int speed = 2;
    private int distance = 10;
    private int turnAngle = 15;
    private int turnSpeed = 2;
    private int headSpeed = 5;
    private int headAngle = 10;

    private Handler handler;

    private String robotType;

    @Override
    public void onCreate() {
        register(MotionService.class);
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Motion service Started", Toast.LENGTH_LONG).show();
        String refPath = intent.getStringExtra("REF_PATH");
        String deviceId = intent.getStringExtra("DEVICE_ID");
        robotType = intent.getStringExtra("ROBOT_TYPE");

        // Firebase references
        db = FirebaseFirestore.getInstance();
        robotRef = db.collection(refPath).document(deviceId);

        // Sanbot managers
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);
        modularMotionManager= (ModularMotionManager) getUnitManager(FuncConstant.MODULARMOTION_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        projectorManager = (ProjectorManager) getUnitManager(FuncConstant.PROJECTOR_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);

        // Worker threads handler
        handler = new Handler();

        initRobotState();
        initDbListeners();

        return START_STICKY;                    // Sticky will restart the service unless stopped explicitly
    }
    @Override
    public void onDestroy() {
        Toast.makeText(this, "Motion service Destroyed", Toast.LENGTH_LONG).show();
        // To stop firebase leaks, remove the listeners
        regF.remove();
        regL.remove();
        regR.remove();
        regS.remove();
        regLight.remove();
        regPrjctr.remove();
        regHeadUp.remove();
        regHeadDown.remove();
        regHeadLeft.remove();
        regHeadRight.remove();
        regHeadReset.remove();
        super.onDestroy();
    }
    @Override
    protected void onMainServiceConnected() {
    }

    public void initRobotState() {

        refF = robotRef.collection("motionValues").document("Forward");
        refL = robotRef.collection("motionValues").document("Left");
        refR = robotRef.collection("motionValues").document("Right");
        refS = robotRef.collection("motionValues").document("Stop");
        refHeadLeft = robotRef.collection("motionValues").document("headLeft");
        refHeadRight = robotRef.collection("motionValues").document("headRight");
        refHeadDown = robotRef.collection("motionValues").document("headDown");
        refHeadUp = robotRef.collection("motionValues").document("headUp");
        refHeadReset = robotRef.collection("motionValues").document("headReset");
        refLight = robotRef.collection("motionValues").document("light");
        refPrjctr = robotRef.collection("motionValues").document("projector");

        // Set initial values
        Map<String, Integer> initialState;

        initialState = new HashMap<>();
        initialState.put("switch", 0);
        initialState.put("speed", 5);                   // valid range: 1-10
        initialState.put("distance", 500);               // in cm
        initialState.put("wheelStatus", 0);
        refF.set(initialState);

        initialState = new HashMap<>();
        initialState.put("switch", 0);
        initialState.put("speed", 5);                   // valid range: 1-10
        initialState.put("angle", 360);                  // in degrees
        initialState.put("wheelStatus", 0);
        refL.set(initialState);
        refR.set(initialState);

        initialState = new HashMap<>();
        initialState.put("switch", 0);
        initialState.put("wheelStatus", 0);
        refS.set(initialState);

        initialState = new HashMap<>();
        initialState.put("switch", 0);
        initialState.put("speed", 5);                   // valid range: 1-10
        initialState.put("angle", 10);                  // in degrees
        refHeadRight.set(initialState);
        refHeadLeft.set(initialState);
        refHeadUp.set(initialState);
        refHeadDown.set(initialState);

        initialState = new HashMap<>();
        initialState.put("switch", 0);
        refHeadReset.set(initialState);
        refLight.set(initialState);
        refPrjctr.set(initialState);

    }
    private void initDbListeners() {
        // Listen for remote changes
        regF = refF.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: refF: Listen failed.", e);
                    return;
                }
                String ref = "refF";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    speed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    distance = Integer.parseInt(documentSnapshot.get("distance").toString());
                    switch (documentSnapshot.get("switch").toString()) {
                        case "0": {
                            Log.i("Tai", TAG + ": refF: switch: 0");
//                            handler.post(runnableS);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": refF: switch: 1");
                            handler.post(runnableF);
                            break;
                        }
                    }
                    switch (documentSnapshot.get("wheelStatus").toString()) {
                        case "0": {
                            Log.i("Tai", TAG + ": refF: wheelStatus: 0");
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": refF: wheelStatus: 1");
                            break;
                        }
                        case "5": {
                            Log.i("Tai", TAG + ": refF: wheelStatus: 5");
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: refF: Snapshot: null");
                }
            }
        });
        regL = refL.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refL";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String wheelStatus = documentSnapshot.get("wheelStatus").toString();
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");;
                    turnSpeed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    turnAngle = Integer.parseInt(documentSnapshot.get("angle").toString());
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
//                            handler.post(runnableS);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableL);
                            break;
                        }
                    }
                    switch (wheelStatus) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                        case "5": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regR = refR.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refR";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String wheelStatus = documentSnapshot.get("wheelStatus").toString();
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    turnSpeed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    turnAngle = Integer.parseInt(documentSnapshot.get("angle").toString());
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
//                            handler.post(runnableS);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableR);
                            break;
                        }
                    }
                    switch (wheelStatus) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                        case "5": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regS = refS.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refS";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String wheelStatus = documentSnapshot.get("wheelStatus").toString();
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
//                            handler.post(runnableS);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableS);
                            break;
                        }
                    }
                    switch (wheelStatus) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                        case "5": {
                            Log.i("Tai", TAG + ": " + ref + ": wheelStatus: " + wheelStatus);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regHeadLeft = refHeadLeft.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refHeadLeft";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    headSpeed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    headAngle = Integer.parseInt(documentSnapshot.get("angle").toString());
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableHL);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regHeadRight = refHeadRight.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refHeadRight";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    headSpeed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    headAngle = Integer.parseInt(documentSnapshot.get("angle").toString());
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableHR);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regHeadUp = refHeadUp.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refHeadUp";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    headSpeed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    headAngle = Integer.parseInt(documentSnapshot.get("angle").toString());
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableHU);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regHeadDown = refHeadDown.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refHeadDown";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    headSpeed = Integer.parseInt(documentSnapshot.get("speed").toString());
                    headAngle = Integer.parseInt(documentSnapshot.get("angle").toString());
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableHD);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regHeadReset = refHeadReset.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refHeadReset";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableHReset);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regLight = refLight.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refLight";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableLightOff);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnableLightOn);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
        regPrjctr = refPrjctr.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("Tai", "MotionService: Listen failed.", e);
                    return;
                }
                String ref = "refPrjctr";
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String swtch = documentSnapshot.get("switch").toString();
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Got data snapshot");
                    switch (swtch) {
                        case "0": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnablePrjctrOff);
                            break;
                        }
                        case "1": {
                            Log.i("Tai", TAG + ": " + ref + ": switch: " + swtch);
                            handler.post(runnablePrjctrOn);
                            break;
                        }
                    }
                } else {
                    Log.d("Tai", "MotionService: SnapshotListener: " + ref + ": Snapshot: null");
                }
            }
        });
    }

    // Locomotion
    final Runnable runnableF = new Runnable() {
        @Override
        public void run() {
            goForward();
        }
    };
    final Runnable runnableL= new Runnable() {
        @Override
        public void run() {
            goLeft();
        }
    };
    final Runnable runnableR = new Runnable() {
        @Override
        public void run() {
            goRight();
        }
    };
    final Runnable runnableS = new Runnable() {
        @Override
        public void run() {
            stop();
        }
    };

    public void goForward() {
        DistanceWheelMotion distanceWheelMotionF = new DistanceWheelMotion(DistanceWheelMotion.ACTION_FORWARD_RUN, speed, distance);
        switch (robotType) {
            case "Elf":
                wheelMotionManager.doDistanceMotion(distanceWheelMotionF);
                refF.update("switch", 0);
                wheelMotionManager.setWheelMotionListener(new WheelMotionManager.WheelMotionListener() {
                    @Override
                    public void onWheelStatus(String s) {
                        Log.i("Tai", TAG + ": goForward" + robotType + ": onWheelStatus: " + s);
                    }
                });
                break;
            case "Max":
                wheelMotionManager.doDistanceMotion(distanceWheelMotionF);
                refF.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + "goForward: refF: unspecified robotType");
                break;
        }
    }
    public void goLeft() {
        RelativeAngleWheelMotion relativeAngleWheelMotion = new RelativeAngleWheelMotion(RelativeAngleWheelMotion.TURN_LEFT, turnSpeed, turnAngle);
        switch (robotType) {
            case "Elf":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
                refL.update("switch", 0);
                wheelMotionManager.setWheelMotionListener(new WheelMotionManager.WheelMotionListener() {
                    @Override
                    public void onWheelStatus(String s) {
                        Log.i("Tai", TAG + ": goLeft" + robotType + ": onWheelStatus: " + s);
                    }
                });
                break;
            case "Max":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
                refL.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": goLeft: " + "unspecified robotType");
                break;
        }
    }
    public void goRight() {
        String fn = "goRight";
        RelativeAngleWheelMotion relativeAngleWheelMotion = new RelativeAngleWheelMotion(RelativeAngleWheelMotion.TURN_RIGHT, turnSpeed, turnAngle);
        switch (robotType) {
            case "Elf":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
                refR.update("switch", 0);
                wheelMotionManager.setWheelMotionListener(new WheelMotionManager.WheelMotionListener() {
                    @Override
                    public void onWheelStatus(String s) {
                        Log.i("Tai", TAG + ": " + fn + robotType + ": onWheelStatus: " + s);
                    }
                });
                break;
            case "Max":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
                refR.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }

    }
    public void stop() {
        String fn = "stop";
        DistanceWheelMotion distanceWheelMotion = new DistanceWheelMotion(DistanceWheelMotion.ACTION_STOP_RUN, 5, 100);
        switch (robotType) {
            case "Elf":
            case "Max":
                wheelMotionManager.doDistanceMotion(distanceWheelMotion);
                wheelMotionManager.setWheelMotionListener(new WheelMotionManager.WheelMotionListener() {
                    @Override
                    public void onWheelStatus(String s) {
                        Log.i("Tai", TAG + ": " + fn + robotType + ": onWheelStatus: " + s);
//                        refS.update("wheelStatus", Integer.valueOf(s));
                    }
                });
                refS.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    public void goWander(){
        modularMotionManager.switchWander(true);
    }
    public void stopWander(){
        modularMotionManager.switchWander(false);
    }

    // Head
    final Runnable runnableHL = new Runnable() {
        @Override
        public void run() {
            headLeft();
        }
    };
    final Runnable runnableHR = new Runnable() {
        @Override
        public void run() {
            headRight();
        }
    };
    final Runnable runnableHU = new Runnable() {
        @Override
        public void run() {
            headUp();
        }
    };
    final Runnable runnableHD = new Runnable() {
        @Override
        public void run() {
            headDown();
        }
    };
    final Runnable runnableHReset = new Runnable() {
        @Override
        public void run() {
            headReset();
        }
    };
    final Runnable runnableLightOn = new Runnable() {
        @Override
        public void run() {
            lightOn();
        }
    };
    final Runnable runnableLightOff = new Runnable() {
        @Override
        public void run() {
            lightOff();
        }
    };
    final Runnable runnablePrjctrOn = new Runnable() {
        @Override
        public void run() {
            prjctrOn();
        }
    };
    final Runnable runnablePrjctrOff = new Runnable() {
        @Override
        public void run() {
            prjctrOff();
        }
    };

    private void headLeft() {
        String fn = "headLeft";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_LEFT, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
                refHeadLeft.update("switch", 0);
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_HORIZONTAL, DRelativeAngleHeadMotion.ACTION_LEFT, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                refHeadLeft.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void headRight() {
        String fn = "headRight";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_RIGHT, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
                refHeadRight.update("switch", 0);
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_HORIZONTAL, DRelativeAngleHeadMotion.ACTION_RIGHT, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                refHeadRight.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void headDown() {
        String fn = "headDown";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_DOWN, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
                refHeadDown.update("switch", 0);
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_VERTICAL, RelativeAngleHeadMotion.ACTION_DOWN, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                refHeadDown.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void headUp() {
        String fn = "headUp";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_UP, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
                refHeadUp.update("switch", 0);
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_VERTICAL, RelativeAngleHeadMotion.ACTION_UP, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                refHeadUp.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void headReset() {
        String fn = "headReset";
        switch (robotType) {
            case "Elf":
            case "Max":
                headMotionManager.doResetMotion();
                refHeadReset.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void lightOn() {
        String fn = "lightOn";
        switch (robotType) {
            case "Elf":
            case "Max":
                hardWareManager.switchWhiteLight(true);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void lightOff() {
        String fn = "lightOff";
        switch (robotType) {
            case "Elf":
            case "Max":
                hardWareManager.switchWhiteLight(false);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void prjctrOn() {
        String fn = "prjctrOn";
        switch (robotType) {
            case "Elf":
                projectorManager.switchProjector(true);
                break;
            case "Max":
                Log.d("Tai", TAG + ": " + fn + ": " + "no projector on Max");
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void prjctrOff() {
        String fn = "prjctrOff";
        switch (robotType) {
            case "Elf":
                projectorManager.switchProjector(false);
                break;
            case "Max":
                Log.d("Tai", TAG + ": " + fn + ": " + "no projector on Max");
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }

}
