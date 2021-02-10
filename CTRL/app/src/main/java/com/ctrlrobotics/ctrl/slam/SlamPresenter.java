package com.ctrlrobotics.ctrl.slam;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.ctrlrobotics.ctrl.R;
import com.sanbot.map.Map;
import com.sanbot.map.MapClient;
import com.sanbot.map.MapHelper;
import com.sanbot.map.Msg;
import com.sanbot.map.PositionTag;
import com.sanbot.opensdk.function.unit.SpeechManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SlamPresenter implements MapClient.Listener, MapClient.a {

    private String TAG = "SlamPresenter";

    private MapClient mMapClient; //Map navigation object
    private IView iView;//Update the interface
    private Context mContext;
    private SpeechManager speechManager;//The management object of the robot talking
    private Handler wHandler;//Child thread handler
    private final List<PositionTag> tagList=new ArrayList<>();//Location points

    private boolean isOnlyNavi=false;
    private boolean init=false;
    private boolean existMap;
    private boolean existTags;
    private boolean inGuiding=false;
    private boolean visible=false;

    private PositionTag backTag;

    private TextToSpeech tts;

    public SlamPresenter(Context context, IView iView, SpeechManager speechManager, TextToSpeech tts){
        this.mContext=context;
        this.iView=iView;
        this.speechManager=speechManager;
        this.tts = tts;
        HandlerThread workThread = new HandlerThread("navigate-load");
        workThread.start();
        wHandler= new Handler(workThread.getLooper());
    }

    // Getters
    public Handler getwHandler() {
        return wHandler;
    }
    public MapClient getMapClient(){
        return mMapClient;
    }

    // Helpers
    public void start() {
        MapClient.Connect("com.ctrlrobotics.slam", this);
    }
    public void stop(){// Disconnect service
        if(mMapClient!=null){
            mMapClient.close();
        }
    }
    public void loadMap(){// Load map
        /**
         * Loads tags generates a bitmap for the current map
         */
        Map map = MapClient.getMap();
        if(map!=null){
            Log.i("Tai", TAG + ": loadMap: " + "success");
            existMap=true;
            List<PositionTag> list=map.getTags();
            if(list!=null&&list.size()>0){
                existTags=true;
                tagList.clear();
                tagList.addAll(list);
                iView.showTags(tagList);
            }else{
                existTags=false;
                iView.showNoTagsV();
            }
            int width=map.getWidth();
            int height=map.getHeight();
            Bitmap bitamp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);//Create bitmap must be rgb_565
            MapHelper.invertMap(map.getData(), map.getWidth(), map.getHeight());//Map data conversion
            MapHelper.setPixels(bitamp,map.getData(),width,height, 0b1111111111111111,0b0000000000000000,0b1100111001011001,0b0000000000000000);//Fill map data to bitmap
            iView.showMap(bitamp,list);//Show map
        }else{
            Log.e("Tai", TAG + ": loadMap: " + null);
            existMap=false;
            iView.showNoMapV();
        }
    }
    private int MAP_ACTIVE = 1;
    private int MAP_IDLE = 0;
    private void updateMaps() {
        /**
         * Updates the available maps and their application status
         * */
        List<String> mapNames = MapClient.getAllMaps();
        Log.i("Tai", TAG + ": updateMaps: " + "Got " + mapNames.size() + " maps");
        Log.i("Tai", TAG + ": updateMaps: " + "Current map: " + MapClient.getUsedMap());
        java.util.Map<String, Integer> mapData = new HashMap<>();
        for (int i = 0; i < mapNames.size(); i++) {
            if(MapClient.getUsedMap().equals(mapNames.get(i))) {   // If it is the current map
                mapData.put(mapNames.get(i), MAP_ACTIVE);                   // Set its status to 1
                loadMap();                                         // Get the associated tags
            } else {
                mapData.put(mapNames.get(i), MAP_IDLE);
            }
        }
        iView.setMaps(mapData);                 // Sets firebase info
    }
    public void switchMap (String mapName) {
        mMapClient.applyMap(mapName);
        mMapClient.setApplyMapListener(this);
    }
    public void loadBackTag(){//Load return point from settings
        wHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    int id = Integer.parseInt(Setting2.getString(mContext.getContentResolver(), "reception_rostag_id"));
                    for (PositionTag tag : tagList) {
                        if (tag.getId() == id) {
                            backTag = tag;
                            break;
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    public void guideTo(final PositionTag tag){
        if(mMapClient!=null){
            /*if(lockGuide){
                Log.v(TAG,"guideTo lockGuide....");
                return;
            }
            lockGuide =true;*/
            wHandler.post(new Runnable() {
                @Override
                public void run() {
                    int result=mMapClient.navigationTo(tag.getX(),tag.getY(),tag.getRadians());
                    //Log.v(TAG,"guideTo result:"+result);
                    if(result==Msg.Result.RESULT_OK) {
                        inGuiding=true;
                        //wHandler.postDelayed(guideR, 6000);
                        iView.showGuideV();
                        String s=mContext.getString(R.string.visit_follow)+":"+tag.getName();
                        iView.showGuideHint(s);
                        speak(s);
                    }else{
                        iView.toast(resultExplain(result));
                        iView.resetTagsListener();
                    }
                    //lockGuide=false;
                }
            });
        }
    }
    public void speak(final String c) {
        // Converts a string input to speech for an initialized TTS instance
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Done");
        if(tts!=null) {
            wHandler.post(() -> {
                tts.speak(c, TextToSpeech.QUEUE_FLUSH, params);
            });
        }
    }
    private String resultExplain(int code){
        String s="";
        switch (code){
            case Msg.Result.NO_CONNECTED:
                s=mContext.getString(R.string.navi_result_no_conn);
                break;
            case Msg.Result.NO_NAVIGATION:
                s=mContext.getString(R.string.navi_result_no_navi);
                break;
            case Msg.Result.NO_PREPARED:
                s=mContext.getString(R.string.navi_result_open_navi);
                break;
            case Msg.Result.NO_CHECK_POSITION:
                s=mContext.getString(R.string.navi_result_no_check);
                break;
            case Msg.Result.X86_NOCONNECTED:
                s=mContext.getString(R.string.navi_result_no_conn_x86);
                break;
            case Msg.Result.IN_VERIFY_MAP:
                s=mContext.getString(R.string.navi_result_checking);
                break;
            case Msg.Result.IN_BUILD_MAP:
                s=mContext.getString(R.string.navi_result_build);
                break;
            case Msg.Result.IN_TRANS_MAP:
                s=mContext.getString(R.string.navi_result_apply);
                break;
            case Msg.Result.RESULT_RES_TIMEOUT:
                s=mContext.getString(R.string.navi_result_cmd_timeout);
                break;
            case Msg.Result.RESULT_OK:
                s=mContext.getString(R.string.execute_success);
                break;
            case Msg.Result.CONN_X86_DISCONNECTED:
                s=mContext.getString(R.string.execute_success);
                break;
            default:
                s=mContext.getString(R.string.navi_result)+":0x"+code;
                break;
        }
        return s;
    }
    private void handlerNavigationError(int code){
        wHandler.removeCallbacks(cancelGuideR);
        if(code>=Msg.NaviState.ERROR){
            long cancelTime = ExplainCode.getCancelTime(code);
            if(cancelTime>0){//是否需要取消导航
                wHandler.postDelayed(cancelGuideR,cancelTime);
            }
            String error= ExplainCode.getNavi(mContext,code);
            if(code==Msg.NaviState.INFRARED_OBSTACLE){
                speak(mContext.getString(R.string.navi_people_stop_speak));
            }else{
                speak(error);
            }
            iView.showNaviError(error);
        }else{
            iView.showNaviError("");
        }
    }
    public void cancelGuide(){
        if(inGuiding) {
            iView.showProgress(mContext.getString(R.string.dialog_cancel_navi), mContext.getString(R.string.dialog_please_wait));
        }
        inGuiding=false;
        wHandler.removeCallbacks(backR);
        wHandler.removeCallbacks(cancelGuideR);
        if(mMapClient!=null){
            wHandler.post(new Runnable() {
                @Override
                public void run() {
                    int result=mMapClient.cancelNavigation();
                    /*if(result!=Msg.Result.RESULT_OK){
                        iView.toast("取消导航失败:"+resultExplain(result));
                    }*/
                    iView.closeProgressDialog();
                    if(isOnlyNavi){
                        iView.finishActivity();
                    }else {
                        iView.showMainV();
                    }
                    //lockGuide=false;
                }
            });
        }
    }
    private PositionTag arriveTag(double x,double y,double radians){//Determine which point to reach
        double minDistance=10000;
        PositionTag minTag=null;
        for(int i=0;i<tagList.size();i++){
            PositionTag item=tagList.get(i);
            double distance=Math.sqrt(Math.pow(x-item.getX(),2)+Math.pow(y-item.getY(),2));
            if(distance<minDistance){
                minDistance=distance;
                minTag=item;
            }
        }
        if(minDistance<10){//According to the distance is less than 10, it is considered to arrive
            return minTag;
        }
        return null;
    }
    private void handleMoveError(int status,int subStatus){
        if(status<Msg.MoveError.MOTOR_ID_ERROR){
            iView.showMoveError("");//清空显示
            return;
        }
        String eMsg=ExplainCode.getMoveError(mContext,status);
        if (status == Msg.MoveError.OBSTACLE_BACK) {
            eMsg += "(" + ExplainCode.getObstacleSub(mContext,subStatus) + ")";
        }else if(status==Msg.MoveError.TOUCH_STOP_BACK){
            eMsg+="("+mContext.getString(R.string.touch_stop_number)+":"+subStatus+")";
        }
        //eMsg = "[" + eMsg + "]";
        speak(eMsg);
        Log.i("Tai", TAG + ": moveError: " + eMsg);
        iView.showMoveError(eMsg);
    }

    private final Runnable cancelGuideR=new Runnable() {
        @Override
        public void run() {
            cancelGuide();
        }
    };
    private final Runnable backR=new Runnable() {
        @Override
        public void run() {
            if(!visible){
                return;
            }
            speak(mContext.getString(R.string.back_speak));
            iView.showGuideHint(mContext.getString(R.string.back_position)+":"+backTag.getName());
            int result=mMapClient.navigationTo(backTag.getX(),backTag.getY(),backTag.getRadians());
            if(result!=Msg.Result.RESULT_OK){
                String error=resultExplain(result);
                iView.showGuideHint(error);
            }
        }
    };

    /** ----- Map client listener ----- */
    @Override
    public void initialize(MapClient mapClient) {
        Log.d("Tai", TAG + ": initialize");
        mMapClient = mapClient;
        updateMaps();
//        loadBackTag();//Load out the return point

        if(mapClient.getCtrlState() != Msg.CtrlState.NAVIGATION){
            mapClient.openNavigation(); //Open Navigation
        }

        // Calibrate current position
        int ctrl=mapClient.getCtrlState();
        int navi=mapClient.getNaviState();

        Log.d("Tai", TAG + ": initialize: ctrlState: " + ExplainCode.getCtrl(ctrl));
        Log.d("Tai", TAG + ": initialize: naviState: " + ExplainCode.getNavi(mContext, navi));
        Log.d("Tai", TAG + ": initialize: tagList: " + tagList.size());

        if(ctrl==Msg.CtrlState.NAVIGATION&&navi==Msg.NaviState.NO_INIT&&tagList.size()>0){//Uninitialized site
            iView.showCheckDialog(tagList); //If necessary, perform position calibration
        }
        if(ctrl==Msg.CtrlState.NAVIGATION&&navi==Msg.NaviState.INIT){

        }

        iView.updatePosition(mapClient.getPose()[0],mapClient.getPose()[1],mapClient.getPose()[2]);//Initialize the current position
        init=true;

    }
    @Override
    public void connState(int i, Object o) {
        switch(i) {
            case 10001:
                Log.i("Tai", TAG + ": connState: " + "CONNECTING");
                break;
            case 10002:
                Log.i("Tai", TAG + ": connState: " + "CONNECTED");
                break;
            case 10004:
                Log.i("Tai", TAG + ": connState: " + "CONN_X86_CONNECTED");
                break;
            case 10005:
                Log.i("Tai", TAG + ": connState: " + "CONN_X86_DISCONNECTED");
                break;
            default:
                Log.i("Tai", TAG + ": connState: " + i);
        }
    }
    @Override
    public void mapState(int ctrl, int oldStrl, int navi, int oldNavi) {
//        Log.i("Tai", TAG + ": mapState: Navigation state changed");
        Log.i("Tai", TAG + ": mapState: Navigation state changed: " + "ctrl: " + ctrl + ", oldStrl: " + oldStrl + ", navi: " + navi + ", oldNavi: " + oldNavi);

        handlerNavigationError(navi);
        iView.pipeNavigationError(navi);
        if (navi == Msg.NaviState.ARRIVE) {//Reach a certain point
            double[] currentPose = mMapClient.getPose();//Get the current x,y coordinates (position)
            PositionTag tag = arriveTag(currentPose[0], currentPose[1], currentPose[2]);   // Check which tag we've arrived at
            boolean finish = false;

            if (GlobalSetting.supportBack) { // Support return to reception
                if (backTag != null) {//Have a reception point
                    if (tag != null && backTag.getId() == tag.getId()) {//Arrive at the reception point
                        speak(mContext.getString(R.string.backed_speak));
                        finish = true;
                    } else {//other
                        if (tag != null) {//There is an arrival point
                            speak(mContext.getString(R.string.arrived_position_speak) + tag.getName());
                            iView.resetTagsListener();
                        } else {//No arrival point
                            speak(mContext.getString(R.string.arrived_speak));
                        }
                        if(visible) {
                            wHandler.postDelayed(backR, 5000);
                        }
                    }
                } else {
                    speak(mContext.getString(R.string.no_reception_speak));
                    iView.showGuideHint(mContext.getString(R.string.no_reception));
                    finish=true;
                }
            } else {
                if (tag != null) {
                    speak(mContext.getString(R.string.arrived_position_speak) + tag.getName());

                    // Tell the nav-planner you have arrived at @tag
                    iView.navArrived(tag.getName());
                } else {
                    speak(mContext.getString(R.string.arrived_speak));
                }
                finish = true;
            }

            if (finish) {
                if(isOnlyNavi){
                    wHandler.postDelayed(new Runnable() {//Delay off, let it finish
                        @Override
                        public void run() {
                            iView.finishActivity();
                        }
                    }, 2000);
                }else {
                    wHandler.postDelayed(new Runnable() {//Delay off, let it finish
                        @Override
                        public void run() {
                            iView.showMainV();
                        }
                    }, 2000);
                }
                //lockGuide=false;
            }

        }
    }
    @Override
    public void position(double x, double y, double rad) {
//        Log.i("Tai", TAG + ": position: Position changed");
        iView.updatePosition(x,y,rad);
    }
    @Override
    public void mapChanged() {
        Log.i("Tai", TAG + ": mapChanged: " + MapClient.getUsedMap());
        iView.updateCalStatus();
    }
    @Override
    public void moveError(int err, int sub) {
        handleMoveError(err, sub);
    }

    /** ----- ApplyMap listener ----- */
    @Override
    public void a(int i) {
        Log.i("Tai", TAG + ": ApplyMapListener: " + resultExplain(i));
        updateMaps();
    }

    public interface IView {
        void showTags(List<PositionTag> list);//Show location point
        void showMainV();//Switch to the main interface
        void showGuideV();//Switch to the navigation interface
        void showNoTagsV();//Switch to the interface without location points
        void showNoMapV();//Switch to no map interface
        void showMap(Bitmap bitmap, List<PositionTag> tags);//Show map
        void updatePosition(double x, double y, double radian);//Update real-time location
        void showProgress(String title, String message);
        void closeProgressDialog();//Close loading box
        void showGuideHint(String s);//Tips for updating the navigation interface
        void toast(String s);//Spinning
        void showNaviError(String s);
        void showMoveError(String s);
        void finishActivity();
        void showCheckDialog(List<PositionTag> tagList);
        void setMaps(java.util.Map<String, Integer> mapData);
        void resetTagsListener();
        void navArrived(String tagName);
        void updateCalStatus();
        void pipeNavigationError(int navi);
    }


}
