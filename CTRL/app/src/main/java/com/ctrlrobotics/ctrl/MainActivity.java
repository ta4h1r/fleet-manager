package com.ctrlrobotics.ctrl;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.ctrlrobotics.ctrl.analytics.AnalyticsActivity;
import com.ctrlrobotics.ctrl.chat.SpeechActivity;
import com.ctrlrobotics.ctrl.presentation.PresentationActivity;
import com.ctrlrobotics.ctrl.slam.SlamActivity;
import com.ctrlrobotics.ctrl.tele.TelepresenceActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.SystemManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends TopBaseActivity {

    private String TAG = "MainActivity";

    SystemManager systemManager;

    FirebaseFirestore db;

    private RequestQueue mQueue;
    Thread initRobotThread;
    Thread initFirebaseThread;

    TextView tvStatus;
    ImageView ivRefresh;
    Animation animation;

    private String deviceId;
    private String refPath;
    private JSONObject robotDetails;
    private String robotAlias;
    private String robotType;
    private JSONObject robotAbilities;
    public Map<String, Object> robotState;

    private Intent serviceIntent;
    private Intent batteryIntent;
    private Intent sensorsIntent;

    private Intent chatIntent;
    private Intent frIntent;
    private Intent analyticsIntent;
    private Intent deliveryIntent;
    private Intent presentationIntent;
    private Intent telepresenceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(MainActivity.class);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // The screen is always on
        getSupportActionBar().hide();                                           // Hide the action bar at the top

        // Sanbot managers
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        // UI
        tvStatus = findViewById(R.id.tv_status);
        ivRefresh = findViewById(R.id.iv_refresh);
        animation = new RotateAnimation(0.0f, 360.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        animation.setRepeatCount(-1);
        animation.setDuration(2000);

        // DB
        FirebaseApp.initializeApp(getApplicationContext());
        db = FirebaseFirestore.getInstance();

        // API requests queue
        mQueue = Volley.newRequestQueue(this);

        initRobotThread = new Thread(this::initRobot);
        initFirebaseThread = new Thread(this::initFirebaseServices);

        Log.i("Tai", "MainActivity: onCreate: initiating robot");
        initRobotThread.start();
    }
    private void initRobot() {
        SystemManager systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        deviceId = systemManager.getDeviceId();

        runOnUiThread(() -> {
            tvStatus.setText("Device ID: \n" + deviceId);
            tvStatus.setTextColor(getResources().getColor(R.color.Red));
        });
        runOnUiThread(() -> ivRefresh.setAnimation(animation));
        Log.i("Tai", "initRobot: deviceId: " + deviceId);

        JSONObject data = new JSONObject();
        JSONObject response;

        try {
            data.put("command", "getRobotInfo");
            data.put("deviceId", deviceId);
            response = postCommand(data);                                                           // Send @command to backend to process @data (blocking)
            handleResponse(response);                                                               // Sets the refPath variable

            Log.i("Tai", "initRobot: refPath: " + refPath);
            Log.i("Tai", "initRobot: robotDetails: " + robotDetails);

            robotAbilities =  robotDetails.getJSONObject("abilities");
            robotType = robotDetails.getString("robotType");
            robotAlias = robotDetails.getString("robotAlias");
            Log.i("Tai", "initRobot: robotType: " + robotType);
            Log.i("Tai", "initRobot: robotAlias: " + robotAlias);
            Log.i("Tai", "initRobot: robotAbilities: " + robotAbilities);

            initFirebaseThread.start();
            runOnUiThread(() -> ivRefresh.clearAnimation());
            runOnUiThread(() -> {
                tvStatus.append("\n" + "Alias: \n" + robotAlias);
                tvStatus.setTextColor(getResources().getColor(R.color.Green));
            });
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Tai", "initRobot: JSON exception" + e);
            runOnUiThread(() -> ivRefresh.clearAnimation());
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e("Tai", "initRobot: Null pointer exception" + e);
            runOnUiThread(() -> ivRefresh.clearAnimation());
        }

    }
    private JSONObject postCommand(JSONObject data) throws JSONException {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        String url =  "https://1t6ooufoi9.execute-api.us-east-1.amazonaws.com/prod";
        try {
            JsonObjectRequest request = new JsonObjectRequest(url, data, future, future);
            mQueue.add(request);
            JSONObject response = future.get(); // this will block
            return response;
        } catch (InterruptedException e) {
            // exception handling
            Log.e("Tai", "postCommand: Interrupted exception: " + e);
            e.printStackTrace();
        } catch (ExecutionException e) {
            // exception handling
            Log.e("Tai", "postCommand: Execution exception: " + e);
            runOnUiThread(() -> {
                tvStatus.append("\n Error: \n" + e.getMessage());
                tvStatus.setTextColor(getResources().getColor(R.color.Red));
            });
            e.printStackTrace();
        }
        return null;
    }
    private void handleResponse(JSONObject response) {
        final JSONObject[] serverData = {new JSONObject()};
        final String[] serverMsg = new String[1];
        int serverStatus = 0;
        try {
            serverStatus = response.getInt("status");

            Log.i("Tai", "handleResponse: serverStatus: " + serverStatus);
            switch (serverStatus) {
                case 200:
                    serverMsg[0] = response.getString("message");
                    serverData[0] = response.getJSONObject("data");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "serverMsg: " + serverMsg[0], Toast.LENGTH_LONG).show() );
                    refPath = serverData[0].getString("refPath");
                    robotDetails = serverData[0].getJSONObject("robotDetails");
                    break;
                case 201:
                    serverMsg[0] = response.getString("message");
                    serverData[0] = response.getJSONObject("data");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "serverMsg: " + serverMsg[0], Toast.LENGTH_LONG).show());
                    Log.d("Tai", "handleResponse: serverMsg: " + serverMsg[0]);
                    Log.d("Tai", "handleResponse: serverData: " + serverData[0].toString());
                    break;
                default:
                    Log.d("Tai", "handleResponse: unhandled case");
                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Tai", "handleResponse: JSON Exception: " + e);
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e("Tai", "handleResponse: Null pointer exception: " + e);
        }
    }

    private void initFirebaseServices() {
        Log.i("Tai", "initFirebaseService: refPath: " + refPath);
        Log.i("Tai", "initFirebaseService: robotDetails: " + robotDetails.toString());

        serviceIntent = new Intent(MainActivity.this, FirebaseService.class);
        serviceIntent.putExtra("REF_PATH", refPath);
        serviceIntent.putExtra("DEVICE_ID", deviceId);
        startService(serviceIntent);

        batteryIntent = new Intent(MainActivity.this, BatteryService.class);
        batteryIntent.putExtra("REF_PATH", refPath);
        batteryIntent.putExtra("DEVICE_ID", deviceId);
        startService(batteryIntent);

        sensorsIntent = new Intent(MainActivity.this, SensorService.class);
        sensorsIntent.putExtra("REF_PATH", refPath);
        sensorsIntent.putExtra("DEVICE_ID", deviceId);
        sensorsIntent.putExtra("ROBOT_TYPE", robotType);
//        startService(sensorsIntent);     // TODO: This service is buggy AF. Intents occasionally leak and debilitate all other firebase services/listeners
    }
    private void doActivity(Map<String, Object> robotState) {
        JSONObject robotStateJson = new JSONObject(robotState);

        try {
            JSONObject activityValues = robotStateJson.getJSONObject("activityValues");
            String[] activityList = {"analytics", "chat", "delivery", "fr", "presentation", "telepresence"};
            chatIntent =  new Intent(MainActivity.this, SpeechActivity.class);      // TODO: Define other intents here
            telepresenceIntent =  new Intent(MainActivity.this, TelepresenceActivity.class);
            deliveryIntent = new Intent(MainActivity.this, SlamActivity.class);
            analyticsIntent = new Intent(MainActivity.this, AnalyticsActivity.class);
            presentationIntent = new Intent(MainActivity.this, PresentationActivity.class);
            Intent[] intentsList = {analyticsIntent, chatIntent, deliveryIntent, frIntent, presentationIntent, telepresenceIntent};
            for (int i = 0; i < activityList.length; i++) {

                Log.d("Tai", "doActivity: " + activityList[i] + " requested: " + activityValues.getInt(activityList[i]));
                Log.d("Tai", "doActivity: " + activityList[i] + " enabled: " + robotAbilities.getInt(activityList[i]));

                if(activityValues.getInt(activityList[i]) == 1 && robotAbilities.getInt(activityList[i]) == 1 && refPath != null) {
                    Log.i("Tai", "doActivity: Starting " + activityList[i] + " activity.");

                    Intent intent = intentsList[i];
                    intent.putExtra("REF_PATH", refPath);
                    intent.putExtra("DEVICE_ID", deviceId);
                    intent.putExtra("ROBOT_TYPE", robotType);
                    intent.putExtra("ROBOT_ALIAS", robotAlias);
                    startActivity(intent);

                    break;
                }
                else if (activityValues.getInt(activityList[i]) == 1 && robotAbilities.getInt(activityList[i]) == 0 && refPath != null) {
                    Log.i("Tai", "doActivity: " + activityList[i] + " has not been activated for this device: " + deviceId);
                    int finalI = i;
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(),  activityList[finalI] + " has not been activated for this device", Toast.LENGTH_SHORT).show());
                    String fieldRef = "activityValues." + activityList[i];
                    db.collection(refPath).document(deviceId).update(fieldRef, 0);
                } else if (refPath == null) {
                    Log.i("Tai", "doActivity: Invalid clientID. Reference path not found. ");
                } else if (activityValues.getInt(activityList[i]) == 0 && (robotAbilities.getInt(activityList[i]) == 0 | robotAbilities.getInt(activityList[i]) == 1) && refPath != null) {
                    String fieldRef = "activityValues." + activityList[i];
                    db.collection(refPath).document(deviceId).update(fieldRef, 0);
                }

            }

        } catch (JSONException e) {
            Log.e("Tai", "doActivity: JSON Exception: " + e);
            e.printStackTrace();
        }


    }

    // Service hook
    @Subscribe
    public void onEvent(MessageEvent event)  {
        robotState = event.getMessage();
        Log.i("Tai", TAG + "onEvent: robotState: " + robotState);
        doActivity(robotState);
    }

    // Lifecycle
    @Override
    protected void onStart() {
        super.onStart();
        Log.i("Tai", TAG + ": onStart");
        EventBus.getDefault().register(this);
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Tai", TAG + ": onStop");
        EventBus.getDefault().unregister(this);
    }
    @Override
    protected void onMainServiceConnected() {
        Log.i("Tai", "MainActivity: onMainServiceConnected");
        systemManager.switchFloatBar(true, MainActivity.class.getName());              // Show the hovering (back) button
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.i("Tai", "MainActivity: onPause");
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Tai", "MainActivity: onResume");
        if (refPath != null) {
            initRobotState();
        }
    }
    @Override
    protected void onDestroy() {
        if (batteryIntent != null) {stopService(batteryIntent);}
        if (sensorsIntent != null) {stopService(sensorsIntent);}
        if (serviceIntent != null) {stopService(serviceIntent);}  // Stop this one last because it terminates the db session used by other services
        Log.i("Tai", "MainActivity: onDestroy");
        super.onDestroy();
    }

    public void onClickRefresh(View view) {
        Log.i("Tai", TAG + ": onClickRefresh");
        ivRefresh.setAnimation(animation);
        if (refPath != null) {
            initRobotState();
        } else if (!initRobotThread.isAlive()) {
            initRobotThread.interrupt();
            initRobotThread = new Thread(() -> initRobot());
            initRobotThread.start();
        }
    }
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
        db = FirebaseFirestore.getInstance();
        db.collection(refPath).document(deviceId).set(initialRobotState);
    }
}
