package com.ctrlrobotics.ctrl.chat;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.ctrlrobotics.ctrl.MessageEvent;
import com.ctrlrobotics.ctrl.R;
import com.ctrlrobotics.ctrl.presentation.AnimatronicsHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.mongodb.lang.NonNull;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchUser;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoDatabase;
import com.mongodb.stitch.core.auth.providers.anonymous.AnonymousCredential;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.headmotion.DRelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.unit.HandMotionManager;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.WaistMotionManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.PIRListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;

import org.bson.Document;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

/**
 * function: Conversational chatbot
 * create date: 2020/5/26 13:59
 * @author Taahir I. Bhaiyat
 */

public class SpeechActivity extends TopBaseActivity {

    private String TAG = "SpeechActivityV3";

    TextView tvQuestion;
    TextView tvAnswer;
    Button btnMic;

    private SpeechManager speechManager;
    private HardWareManager hardwareManager;
    private SystemManager systemManager;
    private HeadMotionManager headMotionManager;
    private HandMotionManager handMotionManager;
    private WaistMotionManager waistMotionManager;

    private RecognizeListener speechListener;

    TextToSpeech tts;
    Set<String> a = new HashSet<>();

    Animation pulse;

    private RequestQueue mQueue;

    public Map<String, Object>  robotState;
    private String refPath;
    private String deviceId;              // TODO: Hook this up to a unique chatbot agent
    private String robotAlias;

    StitchAppClient client;
    RemoteMongoClient remoteMongoClient;
    RemoteMongoDatabase db;
    RemoteMongoCollection<Document> remoteCollection;
    String dbName = "qa-module";
    String collName = "qa_streams";
    String stitchAppClientId = "<stitch-client-id>";

    TextView tvAskMe0;
    TextView tvAskMe1;
    TextView tvAskMe2;
    TextView tvAskMe3;
    TextView tvAskMe4;
    TextView tvAlias;

    WebView webView;
    String searchQuery;
    String currQuery;

    private boolean isTimeout = true;
    private Thread timeoutThread;

    private AnimatronicsHelper animatronicsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(SpeechActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_speech_v3);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // The screen is always on
        getSupportActionBar().hide();                                           // Hide the action bar at the top

        // Layout variables
        tvQuestion = findViewById(R.id.questiontextview);         // Question TextView
        tvQuestion.setTextColor(Color.GRAY);

        tvAnswer = findViewById(R.id.answertextview);             // Answer TextView
        tvAnswer.setTextColor(Color.BLACK);
        tvAnswer.setGravity(Gravity.LEFT);

        btnMic = findViewById(R.id.btn_listen);
        pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);

        tvAskMe0 = findViewById(R.id.tv_ask_me_0);
        tvAskMe1 = findViewById(R.id.tv_ask_me_1);
        tvAskMe2 = findViewById(R.id.tv_ask_me_2);
        tvAskMe3 = findViewById(R.id.tv_ask_me_3);
        tvAskMe4 = findViewById(R.id.tv_ask_me_4);
        tvAlias = findViewById(R.id.tv_alias);

        // Sanbot hardware managers
        hardwareManager =  (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        handMotionManager = (HandMotionManager) getUnitManager(FuncConstant.HANDMOTION_MANAGER);
        waistMotionManager = (WaistMotionManager) getUnitManager(FuncConstant.WAIST_MANAGER);

        // Sanbot mic
        initSpeechListener();     // Sanbot mic
        ttsInit();                // TextToSpeech engine
        mQueue = Volley.newRequestQueue(this);     // Dialogflow API requests queue

        refPath = getIntent().getStringExtra("REF_PATH");
        deviceId = getIntent().getStringExtra("DEVICE_ID");
        robotAlias = getIntent().getStringExtra("ROBOT_ALIAS");
        tvAlias.setText(capitalizeFirstLetter(robotAlias));
        webView = findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());

        searchQuery = "CTRL Robotics";

        mongoLogin();

        hardwareManager.setOnHareWareListener(new PIRListener() {
            @Override
            public void onPIRCheckResult(boolean isChecked, int part) {
                if (part == 1) {
                    Log.i("Tai", TAG + (isChecked ? ": Front PIR triggered" : ": Front PIR off"));
                } else {
                    Log.i("Tai", TAG + (isChecked ? ": Back PIR triggered" : ": Back PIR off"));
                }
                Log.d("Tai", TAG + ": onPIR: isTimeout: " + isTimeout);
                if ((part == 1 && isTimeout)) {
                    webView.setVisibility(View.INVISIBLE);
                    tvAnswer.setText("Welcome to Hotel Sky! You can ask me questions");
                    tvQuestion.setText("Query");
                    talk("Welcome to Hotel Sky! You can ask me questions.");
                }
            }
        });

        timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                isTimeout = true;
                Log.e("Tai", TAG + ": Timeout");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        animatronicsHelper = new AnimatronicsHelper(headMotionManager, handMotionManager,
                waistMotionManager, null,
                null, null,
                hardwareManager);

    }

    private String capitalizeFirstLetter(String str) {
        String cap = str.substring(0, 1).toUpperCase() + str.substring(1);
        return cap;
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
                        systemManager.showEmotion(EmotionsType.SPEAK);

                        isTimeout = false;
                        timeoutThread.interrupt();
                        timeoutThread = new Thread(() -> {
                            try {
                                Thread.sleep(20000);
                                isTimeout = true;
                                Log.e("Tai", TAG + ": timeoutThread.timeout");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                        timeoutThread.start();
                        Log.i("Tai", TAG + ": timeoutThread.start");

                    }
                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> {
                            Log.d("Tai", TAG + ": Voice complete");
                            systemManager.showEmotion(EmotionsType.QUESTION);
                            btnMic.startAnimation(pulse);

                            tvAnswer.setText("Listening...");
                            new Thread(() -> {
                                try {
                                    Thread.sleep(4000);
                                    runOnUiThread(() -> {
                                        tvAnswer.setText("Touch the mic to speak to me");
                                    });
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }).start();

                        });
                        speechManager.doWakeUp();

                        isTimeout = false;
                        timeoutThread.interrupt();
                        timeoutThread = new Thread(() -> {
                            try {
                                Thread.sleep(20000);
                                isTimeout = true;
                                Log.e("Tai", TAG + ": timeoutThread.timeout");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                        timeoutThread.start();
                        Log.i("Tai", TAG + ": timeoutThread.start");
                    }
                    @Override
                    public void onError(String utteranceId) {
                        Log.d("Tai", "TTS error");
                    }
                });

            }

        });
    }
    public void listenButtonClicked(View view) {

        if (!pulse.hasEnded()) {
            pulse.cancel();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnMic.startAnimation(pulse);
                }
            });

        }
        speechManager.doWakeUp();
    }
    private void initSpeechListener() {
        /**
         * Initialize the mic to pick up coherent speech
         */
        speechListener = new RecognizeListener() {
            @Override
            public boolean onRecognizeResult(Grammar grammar) {
                grammar.getText();
                return true;
            }
            @Override
            public void onRecognizeText(RecognizeTextBean recognizeTextBean) {
                btnMic.clearAnimation();
                String query = recognizeTextBean.getText();

                currQuery = query;                              // Saving this to update searchQuery if requested

                Log.d("Tai", "Recognized speech: " + query);
                tvQuestion.setText(query);
                tvAnswer.setText("Thinking...");

                new Thread(() -> {
                    try {
                        // capture the start time
                        long startTime = System.nanoTime();

                        JSONObject data = new JSONObject();
                        JSONObject res;

                        data.put("query", query);
                        res = post(data);

                        String response = handleResponse(res);
                        boolean reqSearch = checkFallback(res);
                        boolean reqName = checkName(res);

                        if (response != null) {

                            if(reqName){
                                response = "I'm " + robotAlias;
                            }

                            Log.i("Tai", "Got AI response: " + response);

                            String finalResponse = response;
                            runOnUiThread(() -> tvAnswer.setText(finalResponse));

                            animatronicsCheck(res);
                            talk(response);

                            if (reqSearch) {
                                runOnUiThread(() -> {
                                    webView.loadUrl("https://www.google.com/search?q=" + searchQuery);
                                    webView.setVisibility(View.VISIBLE);
                                });
                            } else {
                                runOnUiThread(() -> {
                                    webView.setVisibility(View.INVISIBLE);
                                });
                            }

                        } else {
                            Log.i("Tai", "Did not receive AI response");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tvAnswer.setText("Connection error");
                                }
                            });
                        }

                        // capture the method execution end time
                        long endTime = System.nanoTime();
                        // dump the execution time out
                        Log.i("AI_TIMER", String.valueOf((endTime -
                                startTime)/1000000));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).start();

                refreshQs();

            }
            @Override
            public void onRecognizeVolume(int i) {

            }
            @Override
            public void onStartRecognize() {
                Log.i("Tai", TAG + "onStartRecognize");
            }
            @Override
            public void onStopRecognize() {
                Log.i("Tai", TAG + "onStopRecognize");
                tvAnswer.setText("Touch the mic to speak to me");
            }
            @Override
            public void onError(int i, int i1) {
                Log.e("Tai", TAG + "onRecognizeError");
            }
        };
        speechManager.setOnSpeechListener(speechListener);
    }
    private void talk(String c) {
        // Converts a string input to speech for an initialized TTS instance
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Done");
        tts.speak(c, TextToSpeech.QUEUE_FLUSH, params);
    }
    private JSONObject post(JSONObject data) throws JSONException {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        String url =  "https://rr78bg4ga4.execute-api.us-east-1.amazonaws.com/prod";
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
                    Log.i("Tai", TAG + ": handleResponse: response: " + serverData[0].get("response"));
                    return serverData[0].getString("response");
                case 204:
                    serverMsg[0] = response.getString("message");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "serverMsg: " + serverMsg[0], Toast.LENGTH_LONG).show());
                    Log.d("Tai", "handleResponse: serverMsg: " + serverMsg[0]);
                    Log.d("Tai", "handleResponse: serverData: " + serverData[0].toString());
                    return null;
                default:
                    Log.d("Tai", "handleResponse: unexpected server response");
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
    private boolean checkFallback(JSONObject response) {
        final JSONObject[] serverData = {new JSONObject()};
        try {
            serverData[0] = response.getJSONObject("data");
            String intent = serverData[0].getString("intent");
            Log.d("Tai", TAG + ": checkIntent: intent: " + intent);
            if (intent.equals("Default Fallback Intent")) {
                searchQuery = currQuery;                 // Store searchQuery to be used in the followup intent
            }
            return (intent.equals("Default Fallback Intent - reqSearch") || intent.equals("Default Fallback Intent - yes")) ;
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean checkName(JSONObject response) {
        final JSONObject[] serverData = {new JSONObject()};
        try {
            serverData[0] = response.getJSONObject("data");
            String intent = serverData[0].getString("intent");
            Log.d("Tai", TAG + ": checkIntent: intent: " + intent);
            return ( intent.equals("sanbot.name") ) ;
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
    }


    private void mongoLogin() {
        try {
            // Create the default Stitch Client
            client = Stitch.initializeDefaultAppClient(stitchAppClientId);
            // Log-in using an Anonymous authentication provider from Stitch
            client.getAuth().loginWithCredential(new AnonymousCredential()).addOnCompleteListener(
                    new OnCompleteListener<StitchUser>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onComplete(@NonNull final Task<StitchUser> task) {
                            if (task.isSuccessful()) {
                                Log.i("Tai", String.format(
                                        "logged in as user %s with provider %s",
                                        task.getResult().getId(),
                                        task.getResult().getLoggedInProviderType()));
                            } else {
                                Log.e("Tai", "failed to log in", task.getException());
                            }

                            // Get db collection from remote client
                            remoteMongoClient = client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");
                            db = remoteMongoClient.getDatabase(dbName);
                            remoteCollection = db.getCollection(collName);

                            if (remoteCollection == null) {
                                Log.d("Tai", "Does this collection even exist?");
                            } else {
                                loadNewQs();                         // Load questions into memory
                            }

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void mongoLoginResume() {
        // Login to MongoDB again
        try {
            // Get the default mongo client
            client = Stitch.getDefaultAppClient();
            // Log-in using an Anonymous authentication provider from Stitch
            client.getAuth().loginWithCredential(new AnonymousCredential()).addOnCompleteListener(
                    new OnCompleteListener<StitchUser>() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void onComplete(@NonNull final Task<StitchUser> task) {
                            if (task.isSuccessful()) {
                                Log.i("Tai", String.format(
                                        "logged in as user %s with provider %s",
                                        task.getResult().getId(),
                                        task.getResult().getLoggedInProviderType()));
                            } else {
                                Log.e("Tai", "failed to log in", task.getException());
                            }

                            // Get db collection from remote client
                            remoteMongoClient = client.getServiceClient(RemoteMongoClient.factory, "mongodb-atlas");
                            db = remoteMongoClient.getDatabase(dbName);
                            remoteCollection = db.getCollection(collName);

                            if (remoteCollection == null) {
                                Log.d("Tai", "Does this collection even exist?");
                            } else {
                                loadNewQs();                         // Load questions into memory
                            }

                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    List<String> qList;
    List<String> dispQs;
    private void loadNewQs() {
        Log.d("Tai", TAG + ": loadNewQs: loading questions into memory...");
        qList = new ArrayList<>();
        try {
            remoteCollection.find()
                    .limit(1000)
                    .projection(new Document().append("questions", 1))
                    .forEach(document -> {
                        Object q = document.get("questions");
                        Gson gson = new Gson();
                        String qJsonString = gson.toJson(q);
                        try {
                            JSONArray qJson = new JSONArray(qJsonString);
                            qList.add(qJson.getString(0));                    // Get the first question in the array
//                            Log.d("Tai", TAG + ": loadNewQs: found question: " + qJson.getString(0));
                        } catch (JSONException e) {
                            Log.e("Tai", TAG + ": JSON exception: " + e);
                            e.printStackTrace();
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@androidx.annotation.NonNull Task<Void> task) {
                            Log.d("Tai", TAG + ": loadNewQs: Done.");
                            refreshQs();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Tai", TAG + ": Caught exception in DB query.");
        }
    }
    private void refreshQs() {
        // Generate a random number
        int min = 0;
        int max = qList.size();

        // Make a fresh list of questions to display on UI
        dispQs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            int randomNum = new Random().nextInt(max);
            dispQs.add(qList.get(randomNum));
        }

        runOnUiThread(() -> {
            tvAskMe1.setText(dispQs.get(0));
            tvAskMe2.setText(dispQs.get(1));
            tvAskMe3.setText(dispQs.get(2));
            tvAskMe4.setText(dispQs.get(3));
        });
    }

    // TopBaseActivity
    @Override
    protected void onMainServiceConnected() {
        systemManager.showEmotion(EmotionsType.SMILE);
        Log.i("Tai", TAG + ": onMainServiceConnected");
        systemManager.switchFloatBar(false, SpeechActivity.class.getName());              // Show the hovering (back) button
    }

    private void animatronicsCheck(JSONObject response) {

        final JSONObject[] serverData = {new JSONObject()};
        try {
            serverData[0] = response.getJSONObject("data");
            String intent = serverData[0].getString("intent");
            String res = serverData[0].getString("response");
            Log.d("Tai", TAG + ": checkIntent: intent: " + intent);


            if(intent.equals("sanbot.arms.move")) {
                if(res.contains("muscles")) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.BOTH_ARMS, animatronicsHelper.ARMS_MUSCLE);
                }
            }

            if (intent.equals("sanbot.build.greeting - now")) {
                animatronicsHelper.setResetMotion(true);
                animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_WAVE);
            }

            if(intent.equals("sanbot.pose.selfie")) {
                double randomNumber = Math.random();
                if (randomNumber > 0 &&randomNumber < 0.1) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_WAVE);
                } else if (randomNumber > 0.1 && randomNumber < 0.2) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_HAND_OK);
                } else if (randomNumber > 0.2 && randomNumber < 0.3) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_WAVE);
                } else if (randomNumber > 0.3 && randomNumber < 0.4) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_HAND_OK);
                } else if (randomNumber > 0.4 && randomNumber < 0.5) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_WAVE);
                } else if (randomNumber > 0.5 && randomNumber < 0.6) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_HAND_OK);
                } else if (randomNumber > 0.6 && randomNumber < 0.7) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_WAVE);
                } else if (randomNumber > 0.7 && randomNumber < 0.8) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_HAND_OK);
                } else if (randomNumber > 0.8 && randomNumber < 0.9) {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_WAVE);
                } else {
                    animatronicsHelper.setResetMotion(true);
                    animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_HAND_OK);
                }

            }

            String[] byes = {"bye", "goodbye", "thank you"};
            for (String bye : byes) {
                if (res.contains(bye)){
                    Log.d("UNKNOWNQ", "Unknown Question detected");
                    animatronicsHelper.doBowForward();
                    break;
                }
            }

            if (intent.equals("Default Fallback Intent")) {
                animatronicsHelper.doNodNo();
            }

        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }

    }


    // Activity lifecycle
    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.shutdown();
        speechManager.doSleep();
        Log.i("Tai", "SpeechActivity: onDestroy");
//        robotRef.getFirestore().terminate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mongoLoginResume();
    }

    // Service hooks
    private void doActivity(Map<String, Object> robotState) {
        JSONObject robotStateJson = new JSONObject(robotState);
        try {
            JSONObject activityValues = robotStateJson.getJSONObject("activityValues");
            String activity = "chat";

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

        sendMessage("Web", "accordionSummaryMsg", "Ready");
    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.i("Tai", TAG + ": onStop");
        EventBus.getDefault().unregister(this);

        sendMessage("Web", "accordionSummaryMsg", "-");
    }
    @Subscribe
    public void onEvent(MessageEvent event) {
        robotState = event.getMessage();
        Log.i("Tai", TAG + ": onEvent: robotState: " + robotState);
        doActivity(robotState);
    }

    private FirebaseFirestore fb;
    private void sendMessage(String doc, String field, String msg) {
        fb = FirebaseFirestore.getInstance();
        fb.collection(refPath).document(deviceId).collection("messages").document(doc).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists() && documentSnapshot != null) {
                            fb.collection(refPath).document(deviceId).collection("messages").document(doc).update(field, msg);
                        } else {
                            Map<String, String> initialState = new HashMap<>();
                            initialState.put(field, msg);
                            fb.collection(refPath).document(deviceId).collection("messages").document(doc).set(initialState);
                        }
                    }
                });
    }

}
