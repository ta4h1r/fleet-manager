package com.ctrlrobotics.ctrl;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanbot.opensdk.base.BindBaseService;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;

import java.util.HashMap;
import java.util.Map;

public class BatteryService extends BindBaseService {

    private String TAG = "BatteryService";

    public FirebaseFirestore db;
    public DocumentReference robotRef;

    private SystemManager systemManager;
    private WheelMotionManager wheelMotionManager;

    private Handler handler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        register(BatteryService.class);
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Tai", TAG + ": Battery service started");
        Toast.makeText(this, "Battery service Started", Toast.LENGTH_LONG).show();
        String refPath = intent.getStringExtra("REF_PATH");
        String deviceId = intent.getStringExtra("DEVICE_ID");

        // Firebase references
        db = FirebaseFirestore.getInstance();
        assert refPath != null;
        assert deviceId != null;
        robotRef = db.collection(refPath).document(deviceId);

        // Sanbot managers
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        wheelMotionManager = (WheelMotionManager) getUnitManager(FuncConstant.WHEELMOTION_MANAGER);  // We need this one so that Elf doesn't crash when the wheels move (BindBaseService.kt is stupid like that)

        // Worker threads handler
        handler = new Handler();

        initBatteryUpdater();

        return START_STICKY;
    }

    DocumentReference batteryRef;
    Runnable batteryUpdaterRunnable;
    private void initBatteryUpdater() {
        batteryRef = robotRef.collection("sensorValues").document("battery");
        batteryUpdaterRunnable = () -> {
            Map<String, Integer> batteryState = new HashMap<>();
            batteryState.put("value", systemManager.getBatteryValue());
            batteryRef.set(batteryState);
            try {
                if(batteryState.get("value") < 20) {
                    sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "Low power");
                }
            } catch (NullPointerException e) {
                Log.e("Tai", TAG + ": initBatteryUpdater: exception: " + e);
            }
            handler.postDelayed(batteryUpdaterRunnable, 4 * 60 * 1000); // Update the firebase value every 4 minutes
        };
        handler.post(batteryUpdaterRunnable);       // Initiates the loop
    }
    private void sendMessageToFirebaseDoc(String doc, String field, String msg) {
        robotRef.collection("messages").document(doc).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists() && documentSnapshot != null) {
                            robotRef.collection("messages").document(doc).update(field, msg);
                        } else {
                            Map<String, String> initialState = new HashMap<>();
                            initialState.put(field, msg);
                            robotRef.collection("messages").document(doc).set(initialState);
                        }
                    }
                });
    }

    @Override
    protected void onMainServiceConnected() {

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(batteryUpdaterRunnable);
        Toast.makeText(this, "Battery service Destroyed", Toast.LENGTH_LONG).show();
    }
}
