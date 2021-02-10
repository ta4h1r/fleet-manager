package com.ctrlrobotics.ctrl;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanbot.opensdk.base.BindBaseService;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.InfrareListener;
import com.sanbot.opensdk.function.unit.interfaces.hardware.ObstacleListener;
import com.sanbot.opensdk.function.unit.interfaces.system.ObstacleStatusListener;

import java.util.HashMap;
import java.util.Map;

public class SensorService extends BindBaseService {

    private final static String TAG = "SensorService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public FirebaseFirestore db;
    public DocumentReference robotRef;

    private HardWareManager hardWareManager;
    private WheelMotionManager wheelMotionManager;
        private SystemManager systemManager;

    private Handler handler;

    private String robotType;

    @Override
    public void onCreate() {
        register(SensorService.class);
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Tai", TAG + ": Sensor service started");
        Toast.makeText(this, "Sensor service Started", Toast.LENGTH_LONG).show();
        String refPath = intent.getStringExtra("REF_PATH");
        String deviceId = intent.getStringExtra("DEVICE_ID");
        robotType = intent.getStringExtra("ROBOT_TYPE");

        // Firebase references
        db = FirebaseFirestore.getInstance();
        assert refPath != null;
        assert deviceId != null;
        robotRef = db.collection(refPath).document(deviceId);

        // Sanbot managers
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER); // We need this one so that Elf doesn't crash when the wheels move (stupid Chinese SDK)
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        // Worker threads handler
        handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                initIrSensorListeners();
            }
        }).start();

        return START_STICKY;
    }
    @Override
    protected void onMainServiceConnected() {

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(dbUpdateRunnable);
        Toast.makeText(this, "Sensor service Destroyed", Toast.LENGTH_LONG).show();
    }


    private void initIrSensorListeners() {
        String fn = "initSensorListeners";

        initSensorState();
        hardWareManager.setOnHareWareListener(new InfrareListener() {
            @Override
            public void infrareDistance(int part, int distance) {
//                Log.d("Tai", TAG +  " part: " + part + ", distance: " + distance );
                // Add/update the values of local ArrayList on every callback
                sensorState.put("infraRed_" + String.valueOf(part), distance);
            }
        });

        initObstacleState();
        switch(robotType) {
            case "Elf":
                hardWareManager.setOnHareWareListener(new ObstacleListener() {
                    @Override
                    public void onObstacleStatus(boolean avoidingObstacle) {
//                        Log.w("Tai", TAG + ": avoidingObstacle: " + avoidingObstacle);
                        obstacleState.put("obstaclePresent", avoidingObstacle);
                    }
                });
                break;
            case "Max":
                systemManager.setOnObstacleStatusListener(new ObstacleStatusListener() {
                    @Override
                    public void onObstacleStatus(int status, int location) {
                        String stringLoc = translateLocation(location);
                        if (stringLoc != null) {
                            obstacleState.put("status", status);
                            obstacleState.put("sensorLocation", stringLoc);
                        }
                    }
                });
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }

        initDbUploader();

    }

    Map<String, Integer> sensorState;
    Map<String, Object> obstacleState;
    DocumentReference refSensorState;
    DocumentReference refObstacleState;
    Runnable dbUpdateRunnable;
    private void initSensorState() {
        sensorState = new HashMap<>();
        for(int i = 0; i < 24; i++) {
            sensorState.put("infraRed_" + String.valueOf(i + 1), 0);
        }
    }
    private void initObstacleState() {
        String fn = "initObstacleState";
        obstacleState = new HashMap<>();
        switch (robotType) {
            case "Elf":
                obstacleState.put("obstaclePresent", false);
                break;
            case "Max":
                obstacleState.put("status", 0);
                obstacleState.put("sensorLocation", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    private void initDbUploader() {
        refSensorState = robotRef.collection("sensorValues").document("infraRedSensors");
        refObstacleState = robotRef.collection("sensorValues").document("obstacleDetector");
        dbUpdateRunnable  = () -> {
            if(sensorState != null) {
                refSensorState.set(sensorState);
            }
            if(obstacleState != null) {
                refObstacleState.set(obstacleState);
            }
            handler.postDelayed(dbUpdateRunnable, 4 * 60 * 1000);  // Update the firebase values with the current sensor data after a delay (NB: Free plan for firebase is capped at 20k writes per day)
        };
        handler.post(dbUpdateRunnable);   // Initiate runnable loop
    }
    private String translateLocation(int location) {
        switch (location) {
            case 0:
                return "No obstacle detected";
            case 1:
                return "Lower body";
            case 2:
                return "Upper body";
            case 3:
                return "Anti-fall";
            case 4:
                return "Hands";
            case 5:
                return "Left body";
            case 6:
                return "Right body";
            case 7:
                return "Back";
            default:
                return null;
        }
    }

}
