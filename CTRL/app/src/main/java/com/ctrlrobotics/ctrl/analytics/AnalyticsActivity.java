package com.ctrlrobotics.ctrl.analytics;

import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Expression;
import com.amazonaws.mobileconnectors.dynamodbv2.document.Table;
import com.amazonaws.mobileconnectors.dynamodbv2.document.datatype.Document;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.ctrlrobotics.ctrl.MessageEvent;
import com.ctrlrobotics.ctrl.R;
import com.ctrlrobotics.ctrl.presentation.AnimatronicsHelper;
import com.ctrlrobotics.ctrl.slam.SlamActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sanbot.opensdk.base.BindBaseActivity;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.EmotionsType;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.unit.FaceTrackManager;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.SystemManager;
import com.sanbot.opensdk.function.unit.interfaces.hardware.PIRListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Time;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class AnalyticsActivity extends TopBaseActivity {

    private String TAG = "AnalyticsActivity";
    public Map<String, Object> robotState;
    private String refPath;
    private String deviceId;

    Button mic;
    TextToSpeech tts;
    Set<String> a = new HashSet<>();

    private SpeechManager speechManager;
    private SystemManager systemManager;
    private HardWareManager hardwareManager;

    CognitoCachingCredentialsProvider credentials;
    AmazonDynamoDBClient dbClient;
    Table sessionsDB;
    Table questionsDB;
    Boolean permiss = false;
    Boolean isTalking = false;
    private Integer qNo = 0;
    Animation pulse;
    TextView tvQuery;
    TextView tvResponse;
    Document session;

    private boolean isTimeout = true;
    private boolean shouldUploadAnswer = false;

    private Thread timeoutThread;
    private Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        register(AnalyticsActivity.class);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_analytics);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   // The screen is always on
        getSupportActionBar().hide();                                           // Hide the action bar at the top

        //UI Elements
        tvQuery = findViewById(R.id.questiontextview_an);
        tvResponse = findViewById(R.id.answertextview_an);

        // Sanbot hardware managers.
        hardwareManager =  (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        systemManager = (SystemManager) getUnitManager(FuncConstant.SYSTEM_MANAGER);

        //Function initializations.
        initSpeechListener();
        ttsInit();
        awsLogin();

        // Get Firebase ref
        refPath = getIntent().getStringExtra("REF_PATH");
        deviceId = getIntent().getStringExtra("DEVICE_ID");

        hardwareManager.setOnHareWareListener(new PIRListener() {
            @Override
            public void onPIRCheckResult(boolean isChecked, int part) {
                if (part == 1) {
                    Log.i("NIK", isChecked ? "Front PIR triggered" : "Front PIR off");
                    //tvStatus.setText(isChecked ? "Front PIR triggered" : "Ready");
                } else {
                    Log.i("NIK", isChecked ? "Back PIR triggered" : "Back PIR off");
                    //tvStatus2.setText(isChecked ? "Back PIR triggered" : "Back PIR off");
                }
                Log.d("Tai", TAG + ": onPIR: isTimeout: " + isTimeout + ", isTalking: " + isTalking + ", permiss: " + permiss);
                if ((!permiss && part == 1 && !isTalking && isTimeout)) {
                    talk("Hello, may I ask you a question about your experience at Hotel Sky?");
                    Log.i("NIK","Permission requested from subjected");
                    isTalking = true;
                    shouldUploadAnswer = false;
                }
            }
        });

        t = new Timer();
        timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                isTimeout = true;
                Log.e("Tai", TAG + ": Timeout");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    protected void onMainServiceConnected() {
        systemManager.switchFloatBar(false, AnalyticsActivity.class.getName());
        Log.d("Tai", TAG + ": mainServiceConnected");
    }
    private int qIndex;
    private String qAnswer;
    private void initSpeechListener(){
        RecognizeListener speechListener = new RecognizeListener() {
            @Override
            public void onStopRecognize() {
            }
            @Override
            public void onStartRecognize() {

            }
            @Override
            public void onRecognizeVolume(int i) {
            }
            @Override
            public void onRecognizeText(RecognizeTextBean recognizeTextBean) {
                String answer = recognizeTextBean.getText();
                qAnswer = answer;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvResponse.setText(answer);
                    }
                });

                Log.d("Tai", TAG + ": Recognized text: " + answer);

                shouldUploadAnswer = qNo == 1 | qNo == 2;
                Log.d("Tai", TAG + ": permiss: " + permiss + ", qNo: " + qNo + ", shouldUpload: " + shouldUploadAnswer + ": isTimeout: " + isTimeout);

                // TODO: Make an api call to dialogflow agent container and use a set of permission granted/denied intents.
                String[] permissionGrantedWords = {"yes", "fine", "cool", "yeah", "sure", "totally", "yup", "yep", "course"};
                String[] permissionDeniedWords = {"no", "nah", "na", "thanks", "thank", "nope", "nup", "never", "shut"};
                String[] words = answer.split(" ");

                if(!permiss) {
                    for (int i = 0; i < words.length; i++) {
                        if (Arrays.asList(permissionGrantedWords).contains(words[i])) {
                            permiss = true;
                            break;
                        } else {
                            speechManager.doSleep();
                        }
                    }
                }

                Log.d("Tai", TAG + ": qAnswer: " + qAnswer + ", qIndex: " + qIndex + ", topic: " + topics.get(qIndex));
                String topic = topics.get(qIndex);
                if (shouldUploadAnswer) {
                    new Thread(() -> uploadSession(qAnswer, topic)).start();
                }

                if (permiss && qNo < 2){                             // Ask 2 questions before end
                    askQ();                                          // Ask another question
                } else if (!permiss && qNo < 2) {
                    for (String word : words) {                      // foreach
                        if (Arrays.asList(permissionDeniedWords).contains(word)) {
                            speechManager.doSleep();
                            break;
                        } else {
                            talk("Would you like to answer more questions about your hotel experience?");
                            shouldUploadAnswer = false;
                        }
                    }
                } else {
                    permiss = false;
                    talk("Thank you for your time.");
                    qNo = 0;
                }

            }
            @Override
            public boolean onRecognizeResult(Grammar grammar) {
                return false;
            }
            @Override
            public void onError(int i, int i1) {
            }
        };
        speechManager.setOnSpeechListener(speechListener);
    }
    private void ttsInit() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    Log.i("Tai", TAG + ": TTS initated");
                    tts.setSpeechRate(0.875F);
                    tts.setPitch(0.9F);
                    Voice ttsVoice = new Voice("en_GB",new Locale("en","GB"),Voice.QUALITY_VERY_HIGH,400,true, a);
//                    Voice ttsVoice = new Voice("es-us-x-sfb-phone-hmm",new Locale("en","US"),500,400,false,a);
                    tts.setVoice(ttsVoice);
                    // Uncomment these to check available voices on the device
//                    myVoices = tts.getVoices();
//                    Log.i("Tai", String.valueOf(myVoices));
                }
                // TTS methods
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        //systemManager.showEmotion(EmotionsType.SPEAK);
                        isTalking = true;
                    }
                    @Override
                    public void onDone(String utteranceId) {
                        isTalking = false;
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run() {
                                systemManager.showEmotion(EmotionsType.QUESTION);
                            }
                        });
                        Log.i("NIK","Finished asking question number:" + qNo);
                        if (qNo <= questions.size()) {
                            speechManager.doWakeUp();
                        } else {
                            speechManager.doSleep();
                            qNo=0;
                            permiss = false;
                        }

                        isTimeout = false;
                        timeoutThread.interrupt();
                        timeoutThread = new Thread(() -> {
                            try {
                                Thread.sleep(20000);
                                isTimeout = true;
                                permiss = false;
                                isTalking = false;
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
                        Log.d("NIK", "TTS error");
                    }
                });
            }
        });
    }
    private void awsLogin() {
        credentials = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "<identity-pool-id>",
                Regions.US_EAST_2 // Region (cognito provider)
        );
        dbClient = new AmazonDynamoDBClient(credentials);
        dbClient.setRegion(Region.getRegion(Regions.US_EAST_2));  // Region (Table)
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sessionsDB = Table.loadTable(dbClient, "an_sessions");
                    Log.d("NIK", "Connected to sessionsDB.");
                    questionsDB =  Table.loadTable(dbClient, "an_questions");
                    Log.d("NIK", "Connected to questionsDB.");
                    loadQuestionsFromDb();
                } catch(Exception e) {
                    loadQuestions();
                    Log.e("Tai", TAG + ": awsLogin: exception: " + e);
                }
            }
        }).start();

    }
    HashMap<Integer, String> questions = new HashMap<>();
    HashMap<Integer, String> topics = new HashMap<>();
    private void loadQuestions(){
        // Fail safe questions loader in case db does not load
        questions.put(0, "How was your meal?"); //Food + Beverage
        questions.put(1, "How is the internet speed?"); //Connectivity
        questions.put(2, "Did you enjoy your room?"); //Rooms
        questions.put(3, "Did you find the facilities of the hotel useful?"); //Facilities
        questions.put(4, "Has our staff offered you excellent service?");//Service
        questions.put(5, "Are the hygiene levels satisfactory?");//Cleanliness

        String[] tList = {"Food+Beverage", "Connectivity", "Rooms", "Facilities", "Service", "Cleanliness"};
        for (int i = 0; i < questions.size(); i++) {
            topics.put(i, tList[i]);
        }
    }
    private void loadQuestionsFromDb() {
        if(questionsDB != null) {
            List<Document> scanData = getAllDocs(questionsDB);
            if (scanData.size() > 0) {
                for (int i = 0; i < scanData.size(); i++) {
                    String q =  Objects.requireNonNull(scanData.get(i).get("question")).asString();
                    String t =  Objects.requireNonNull(scanData.get(i).get("topic")).asString();
                    Log.d("Tai", TAG + ": loadQuestionsFromDb: question: " + q);
                    Log.d("Tai", TAG + ": loadQuestionsFromDb: topic: " + t);
                    topics.put(i, t);
                    questions.put(i, q);
                }
            }
        }
    }
    private void talk(String c) {
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Done");
        tts.speak(c, TextToSpeech.QUEUE_FLUSH, params);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvQuery.setText(c);
            }
        });
    }
    int i = 0;
    private void askQ(){
        int questionNo;
        if (i < questions.size() && questions.size() % 2 == 0) {
            questionNo = i;
            i++;
        } else {
            questionNo = new Random().nextInt(questions.size());
        }

        qIndex = questionNo;
        String question = questions.get(questionNo);
        Log.d("Tai", TAG + ": askQ: " + question);
        talk(question);
        qNo++;

    }
    public void listenButtonClicked(View view) {
        speechManager.doWakeUp();
    }

    public List<Document> getAllDocs(Table table) {
        final Expression expression = new Expression();             // No filter. To add filters to the scan, see https://aws.amazon.com/blogs/mobile/using-amazon-dynamodb-document-api-with-aws-mobile-sdk-for-android-part-1/
        return table.scan(expression).getAllResults();
    }
    public void uploadSession(String answer, String topic) {
        Log.d("Tai", TAG + ": uploadSession: answer: " + answer + ", topic: " + topic);
        try {
            Table table = sessionsDB;
            Document doc = new Document();
            String sessionId = UUID.randomUUID().toString();

            doc.put("text", answer);
            doc.put("id", sessionId);
            doc.put("sector", topic);

            table.putItem(doc);
        } catch(NullPointerException e) {
            Log.e("Tai", TAG + ": putQuestion: " + e);
        }
    }

    /** -------------- Service hooks ----------------*/
    private void doActivity(Map<String, Object> robotState) {
        JSONObject robotStateJson = new JSONObject(robotState);
        try {
            JSONObject activityValues = robotStateJson.getJSONObject("activityValues");
            String activity = "analytics";          // firebase activityValue

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