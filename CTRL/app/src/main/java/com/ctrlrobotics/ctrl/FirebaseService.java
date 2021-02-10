package com.ctrlrobotics.ctrl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class FirebaseService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i("Tai", "FirebaseService: onBind");
        if (db == null) {db = FirebaseFirestore.getInstance();}
        return null;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public FirebaseFirestore db;
    public DocumentReference robotRef;
    public ListenerRegistration regRobotRef;
    public Map<String, Object>  robotState;

    public void initRobotState() {
        Map<String, Integer> initialActivityValues = new HashMap<>();
        initialActivityValues.put("analytics", 0);
        initialActivityValues.put("chat", 0);
        initialActivityValues.put("delivery", 0);
        initialActivityValues.put("fr", 0);
        initialActivityValues.put("presentation", 0);
        initialActivityValues.put("telepresence", 0);

        Map<String, Object> initialRobotState = new HashMap<>();
        initialRobotState.put("activityValues", initialActivityValues);

        // Set initial activity values
        robotRef.set(initialRobotState);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Firebase service Started", Toast.LENGTH_LONG).show();
        String refPath = intent.getStringExtra("REF_PATH");
        String deviceId = intent.getStringExtra("DEVICE_ID");

        // Firebase references
        db = FirebaseFirestore.getInstance();
        robotRef = db.collection(refPath).document(deviceId);

        initRobotState();

        // Listen for remote changes
        regRobotRef = robotRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.w("Tai", "FirebaseService: Listen failed.", e);
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                Log.i("Tai", "FirebaseService: SnapshotListener: Got data snapshot");
                robotState = documentSnapshot.getData();
                EventBus.getDefault().post(new MessageEvent(robotState));   // Publish robotState (intercepted by @Subscribe onEvent for registered activities)
            } else {
                Log.d("Tai", "FirebaseService: SnapshotListener: Snapshot: null");
            }
        });
        return START_STICKY;                    // Sticky will restart the service unless stopped explicitly
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        sendMessageToFirebaseDoc("Web", "accordionSummaryMsg", "-");
        regRobotRef.remove();
        db.terminate();
        Toast.makeText(this, "Firebase service Destroyed", Toast.LENGTH_LONG).show();
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

}
