package com.ctrlrobotics.ctrl.presentation;

import android.os.Handler;
import android.util.Log;

import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.function.beans.handmotion.CombinationHandMotion;
import com.sanbot.opensdk.function.beans.headmotion.DRelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.waistmotion.RelativeAngleWaistMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.DistanceWheelMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.NoAngleWheelMotion;
import com.sanbot.opensdk.function.beans.wheelmotion.RelativeAngleWheelMotion;
import com.sanbot.opensdk.function.beans.wing.AbsoluteAngleWingMotion;
import com.sanbot.opensdk.function.beans.wing.NoAngleWingMotion;
import com.sanbot.opensdk.function.beans.wing.RelativeAngleWingMotion;
import com.sanbot.opensdk.function.unit.HandMotionManager;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.WaistMotionManager;
import com.sanbot.opensdk.function.unit.WheelMotionManager;
import com.sanbot.opensdk.function.unit.WingMotionManager;

import java.util.Timer;
import java.util.TimerTask;

public class AnimatronicsHelper {

    String TAG = "AnimatronicsHelper";

    String robotType;

    private int speed, distance;
    private int turnSpeed, turnAngle;
    private int headSpeed, headAngle;
    private int waistSpeed, waistAngle;
    private int wingSpeed, wingAngle;

    private HardWareManager hardwareManager;
    private HandMotionManager handMotionManager;
    private HeadMotionManager headMotionManager;
    private WaistMotionManager waistMotionManager;
    private WheelMotionManager wheelMotionManager;
    private ModularMotionManager modularMotionManager;
    private WingMotionManager wingMotionManager;

    private int sleepTime;
    private int duration;

    boolean isReset = false;

    public final int ARMS_WAVE = 0;
    public final int ARMS_STRETCH_SIDE = 1;
    public final int ARMS_STRETCH_FRONT= 2;
    public final int ARMS_HAND_FORWARD1 = 3;
    public final int ARMS_RAISE_HAND = 4;
    public final int ARMS_DROP_HAND1 = 5;
    public final int ARMS_DROP_HAND2 = 6;
    public final int ARMS_DEFEND = 7;
    public final int ARMS_OPEN_ARM = 8;
    public final int ARMS_DROP_HAND3 = 9;
    public final int ARMS_STRETCH_HIGH_NARROW = 10;
    public final int ARMS_STRETCH_FLAT_NARROW = 11;
    public final int ARMS_STRETCH_HIGH_WIDE = 12;
    public final int ARMS_STRETCH_FLAT_WIDE = 13;
    public final int ARMS_HIGH_WIDE = 14;
    public final int ARMS_FLAT_WIDE = 15;
    public final int ARMS_HAND_HAND = 16;
    public final int ARMS_CHEER = 17;
    public final int ARMS_HAND_FORWARD2 = 18;
    public final int ARMS_MUSCLE = 19;
    public final int ARMS_HAND_FORWARD3 = 20;
    public final int ARMS_HAND_READY = 21;
    public final int ARMS_HAND_RIGHT_WALK = 22;
    public final int ARMS_HAND_HEAD = 23;
    public final int ARMS_HAND_EXPAND_CHEST = 24;
    public final int ARMS_HAND_GLIDE = 25;
    public final int ARMS_HAND_OK = 26;
    public final int ARMS_HAND_V = 27;

    public final int BOTH_ARMS = 103;
    public final int LEFT_ARM = 102;
    public final int RIGHT_ARM = 101;

    public final int WHEELS_FORWARD = 200;
    public final int WHEELS_BACK = 201;
    public final int WHEELS_LEFT_FORWARD = 202;
    public final int WHEELS_RIGHT_FORWARD = 203;
    public final int WHEELS_LEFT_BACK = 204;
    public final int WHEELS_RIGHT_BACK = 205;
    public final int WHEELS_LEFT_TRANSLATION = 206;
    public final int WHEELS_RIGHT_TRANSLATION = 207;
    public final int WHEELS_STOP = 208;

    public final int WAIST_FRONT = 301;
    public final int WAIST_BACK = 302;
    public final int WAIST_STOP = 300;

    public final int WHEELS_TURN_LEFT = 402;
    public final int WHEELS_TURN_RIGHT = 401;
    public final int WHEELS_TURN_STOP = 400;

    public final int WINGS_LEFT = 502;
    public final int WINGS_RIGHT = 501;
    public final int WINGS_BOTH = 500;
    public final int WINGS_UP = 503;
    public final int WINGS_DOWN = 504;

    public final int LED_ALL = 600;
    public final int LED_LEFT_HAND = 601;
    public final int LED_RIGHT_HAND = 602;
    public final int LED_LEFT_HEAD = 603;
    public final int LED_RIGHT_HEAD = 604;
    public final int LED_WHEEL = 605;

    public final int LED_OFF = 700;
    public final int LED_WHITE = 701;
    public final int LED_RED = 702;
    public final int LED_GREEN = 703;
    public final int LED_PINK = 704;
    public final int LED_PURPLE = 705;
    public final int LED_BLUE = 706;
    public final int LED_YELLOW = 707;
    public final int LED_FLICKER_WHITE = 708;
    public final int LED_FLICKER_RED = 709;
    public final int LED_FLICKER_GREEN = 710;
    public final int LED_FLICKER_PINK = 711;
    public final int LED_FLICKER_PURPLE = 712;
    public final int LED_FLICKER_BLUE = 713;
    public final int LED_FLICKER_YELLOW = 714;
    public final int LED_FLICKER_RANDOM = 715;



    // Constructor
    public AnimatronicsHelper(HeadMotionManager headMotionManager,
                              HandMotionManager handMotionManager,
                              WaistMotionManager waistMotionManager,
                              WheelMotionManager wheelMotionManager,
                              ModularMotionManager modularMotionManager,
                              WingMotionManager wingMotionManager,
                              HardWareManager hardWareManager) {

        this.handMotionManager = handMotionManager;
        this.headMotionManager = headMotionManager;
        this.waistMotionManager = waistMotionManager;
        this.wheelMotionManager = wheelMotionManager;
        this.modularMotionManager = modularMotionManager;
        this.wingMotionManager = wingMotionManager;
        this.hardwareManager = hardWareManager;

    }

    // Setters
    public void setDistance(int distance) {this.distance = distance;}
    public void setSpeed(int speed) {
        this.speed = speed;
    }
    public void setTurnSpeed(int turnSpeed) {this.turnSpeed = turnSpeed;}
    public void setTurnAngle(int turnAngle) {this.turnAngle = turnAngle;}
    public void setRobotType (String robotType) {this.robotType = robotType;}
    public void setHeadSpeed(int headSpeed) {this.headSpeed = headSpeed;}
    public void setHeadAngle(int headAngle) {this.headAngle = headAngle;}
    public void setWaistAngle(int waistAngle) {this.waistAngle = waistAngle;}
    public void setWaistSpeed(int waistSpeed) {this.waistSpeed = waistSpeed;}
    public void setDuration(int duration) {this.duration = duration/100;}  // Specify duration in ms
    public void setWAngle(int wAngle) {this.wAngle = wAngle;}
    public void setWingAngle(int angle) {this.wingAngle = angle;}
    public void setWingSpeed(int speed) {this.wingSpeed = speed;}
    public void setLedDelayTime(int delayTime) {this.delayTime = (byte) delayTime;}   // For flickering. 1-255. Unit is 100 ms
    public void setLedRandomCount(int randomCount) {this.randomCount = (byte) randomCount;} // Number of random colors for random color flashing mode. 1-7
    public void setResetMotion(boolean isReset) {this.isReset = isReset;}
    private void setSleepTime(int sleepTime) {this.sleepTime = sleepTime;}

    public byte handPart;
    public byte handMotion;
    public void doHandMotion(int arm, int action) {
        switch(arm) {
            case BOTH_ARMS:
                this.handPart = CombinationHandMotion.PART_BOTH;
                break;
            case LEFT_ARM:
                this.handPart = CombinationHandMotion.PART_LEFT;
                break;
            case RIGHT_ARM:
                this.handPart = CombinationHandMotion.PART_RIGHT;
                break;
            default:
                Log.e("Tai", TAG + ": doHandMotion: arm: " + "invalid switch case");
                throw new IllegalStateException("Unexpected value: " + arm);
        }
        switch(action) {
            case ARMS_CHEER:
                this.handMotion = CombinationHandMotion.MOTION_CHEER;
                break;
            case ARMS_DEFEND:
                this.handMotion = CombinationHandMotion.MOTION_DEFEND;
                break;
            case ARMS_DROP_HAND1:
                this.handMotion = CombinationHandMotion.MOTION_DROP_HAND1;
                break;
            case ARMS_DROP_HAND2:
                this.handMotion = CombinationHandMotion.MOTION_DROP_HAND2;
                break;
            case ARMS_DROP_HAND3:
                this.handMotion = CombinationHandMotion.MOTION_DROP_HAND3;
                break;
            case ARMS_FLAT_WIDE:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_FLAT_WIDE;
                break;
            case ARMS_HAND_EXPAND_CHEST:
                this.handMotion = CombinationHandMotion.MOTION_HAND_EXPAND_CHEST;
                break;
            case ARMS_HAND_FORWARD2:
                this.handMotion = CombinationHandMotion.MOTION_HAND_FORWARD2;
                break;
            case ARMS_HAND_FORWARD3:
                this.handMotion = CombinationHandMotion.MOTION_HAND_FORWARD3;
                break;
            case ARMS_HAND_FORWARD1:
                this.handMotion = CombinationHandMotion.MOTION_HAND_FORWARD1;
                break;
            case ARMS_HAND_GLIDE:
                this.handMotion = CombinationHandMotion.MOTION_HAND_GLIDE;
                break;
            case ARMS_HAND_HAND:
                this.handMotion = CombinationHandMotion.MOTION_HAND_HAND;
                break;
            case ARMS_HAND_OK:
                this.handMotion = CombinationHandMotion.MOTION_HAND_OK;
                break;
            case ARMS_HAND_READY:
                this.handMotion = CombinationHandMotion.MOTION_HAND_READY;
                break;
            case ARMS_HAND_RIGHT_WALK:
                this.handMotion = CombinationHandMotion.MOTION_HAND_RIGHT_WALK;
                break;
            case ARMS_HAND_V:
                this.handMotion = CombinationHandMotion.MOTION_HAND_V;
                break;
            case ARMS_HIGH_WIDE:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_HIGH_WIDE;
                break;
            case ARMS_MUSCLE:
                this.handMotion = CombinationHandMotion.MOTION_MUSCLE;
                break;
            case ARMS_OPEN_ARM:
                this.handMotion = CombinationHandMotion.MOTION_OPEN_ARM;
                break;
            case ARMS_RAISE_HAND:
                this.handMotion = CombinationHandMotion.MOTION_RAISE_HAND;
                break;
            case ARMS_STRETCH_FLAT_NARROW:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_FLAT_NARROW;
                break;
            case ARMS_STRETCH_FLAT_WIDE:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_FLAT_WIDE;
                break;
            case ARMS_STRETCH_FRONT:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_FRONT;
                break;
            case ARMS_STRETCH_SIDE:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_SIDE;
                break;
            case ARMS_STRETCH_HIGH_NARROW:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_HIGH_NARROW;
                break;
            case ARMS_STRETCH_HIGH_WIDE:
                this.handMotion = CombinationHandMotion.MOTION_STRETCH_HIGH_WIDE;
                break;
            case ARMS_WAVE:
                this.handMotion = CombinationHandMotion.MOTION_WAVE;
                break;
            case ARMS_HAND_HEAD:
                this.handMotion = CombinationHandMotion.MOTION_HAND_HEAD;
                break;
            default:
                Log.e("Tai", TAG + ": doHandMotion: action: " + "invalid switch case");
                throw new IllegalStateException("Unexpected value: " + handPart);
        }
        CombinationHandMotion combinationHandMotion = new CombinationHandMotion(handPart, handMotion, CombinationHandMotion.ACTION_START);
        handMotionManager.doCombinationMotionReset(combinationHandMotion, isReset);
    }

    byte wheelAction;
    public void doWheelTranslation(int motion) {
        switch(motion) {
            case WHEELS_FORWARD:
                this.wheelAction = DistanceWheelMotion.ACTION_FORWARD_RUN;
                break;
            case WHEELS_BACK:
                this.wheelAction = DistanceWheelMotion.ACTION_BACK_RUN;
                break;
            case WHEELS_LEFT_FORWARD:
                this.wheelAction = DistanceWheelMotion.ACTION_LEFT_FORWARD_RUN;
                break;
            case WHEELS_RIGHT_FORWARD:
                this.wheelAction = DistanceWheelMotion.ACTION_RIGHT_FORWARD_RUN;
                break;
            case WHEELS_LEFT_BACK:
                this.wheelAction = DistanceWheelMotion.ACTION_LEFT_BACK_RUN;
                break;
            case WHEELS_RIGHT_BACK:
                this.wheelAction = DistanceWheelMotion.ACTION_RIGHT_BACK_RUN;
                break;
            case WHEELS_LEFT_TRANSLATION:
                this.wheelAction = DistanceWheelMotion.ACTION_LEFT_TRANSLATION;
                break;
            case WHEELS_RIGHT_TRANSLATION:
                this.wheelAction = DistanceWheelMotion.ACTION_RIGHT_TRANSLATION;
                break;
            case WHEELS_STOP:
                this.wheelAction = DistanceWheelMotion.ACTION_STOP_RUN;
                break;
            default:
                Log.e("Tai", TAG + ": doWheelMotion: " + "invalid switch case");
                throw new IllegalStateException("Unexpected value: " + wheelAction);
        }
        DistanceWheelMotion distanceWheelMotion = new DistanceWheelMotion(wheelAction, speed, distance);
        wheelMotionManager.doDistanceMotion(distanceWheelMotion);
    }

    byte wheelDurationAction;
    int wAngle = 0;            // valid for 0 - 200
    public void doWheelDuration(int motion) {
        switch(motion) {
            case WHEELS_FORWARD:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_FORWARD;
                break;
            case WHEELS_BACK:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_BACK;
                break;
            case WHEELS_LEFT_FORWARD:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_LEFT_FORWARD;
                break;
            case WHEELS_RIGHT_FORWARD:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_RIGHT_FORWARD;
                break;
            case WHEELS_LEFT_BACK:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_LEFT_BACK;
                break;
            case WHEELS_RIGHT_BACK:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_RIGHT_BACK;
                break;
            case WHEELS_LEFT_TRANSLATION:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_LEFT_TRANSLATION;
                break;
            case WHEELS_RIGHT_TRANSLATION:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_RIGHT_TRANSLATION;
                break;
            case WHEELS_STOP:
                this.wheelDurationAction = NoAngleWheelMotion.ACTION_STOP;
                break;
            default:
                Log.e("Tai", TAG + ": doWheelDuration: " + "invalid switch case");
                throw new IllegalStateException("Unexpected value: " + wheelDurationAction);
        }
        NoAngleWheelMotion noAngleWheelMotion = new NoAngleWheelMotion(wheelDurationAction, speed, wAngle, duration, NoAngleWheelMotion.STATUS_KEEP);
        wheelMotionManager.doNoAngleMotion(noAngleWheelMotion);
    }

    byte wheelRotationAction;
    public void doWheelRotation(int motion) {
        switch(motion) {
            case WHEELS_TURN_LEFT:
                this.wheelRotationAction = RelativeAngleWheelMotion.TURN_LEFT;
                break;
            case WHEELS_TURN_RIGHT:
                this.wheelRotationAction = RelativeAngleWheelMotion.TURN_RIGHT;
                break;
            case WHEELS_TURN_STOP:
                this.wheelRotationAction = RelativeAngleWheelMotion.TURN_STOP;
                break;
            default:
                Log.e("Tai", TAG + ": doWaistMotion: " + "invalid switch case");
                throw new IllegalStateException("Unexpected value: " + wheelRotationAction);
        }
        RelativeAngleWheelMotion wheelMotion = new RelativeAngleWheelMotion(wheelRotationAction, turnSpeed, turnAngle);
        wheelMotionManager.doRelativeAngleMotion(wheelMotion);
    }

    byte waistAction;
    void doWaistMotion(int motion){
        switch(motion) {
            case WAIST_BACK:
                waistAction = RelativeAngleWaistMotion.ACTION_BACK;
                break;
            case WAIST_FRONT:
                waistAction = RelativeAngleWaistMotion.ACTION_FRONT;
                break;
            case WAIST_STOP:
                waistAction = RelativeAngleWaistMotion.ACTION_STOP;
                break;
            default:
                Log.e("Tai", TAG + ": doWaistMotion: " + "invalid switch case");
                throw new IllegalStateException("Unexpected value: " + waistAction);
        }
        RelativeAngleWaistMotion relativeAngleWaistMotion = new RelativeAngleWaistMotion(waistAction, waistSpeed, waistAngle);
        waistMotionManager.doRelativeAngleMotion(relativeAngleWaistMotion);
    }

    byte wingPart;
    byte wingAction;
    void doAbsWingMotion(int wing){
        switch(wing) {
            case WINGS_LEFT:
                wingPart = AbsoluteAngleWingMotion.PART_LEFT;
                break;
            case WINGS_RIGHT:
                wingPart = AbsoluteAngleWingMotion.PART_RIGHT;
                break;
            case WINGS_BOTH:
                wingPart = AbsoluteAngleWingMotion.PART_BOTH;
                break;
            default:
                Log.e("Tai", TAG + ": doWingMotion: " + "invalid switch case");
                throw new IllegalStateException("Unexpected value: " + wingPart);
        }
        AbsoluteAngleWingMotion absoluteAngleWingMotion = new AbsoluteAngleWingMotion(wingPart, wingSpeed, wingAngle);
        wingMotionManager.doAbsoluteAngleMotion(absoluteAngleWingMotion);
    }
    void doRelWingMotion(int wing, int action){
            switch(wing) {
                case WINGS_LEFT:
                    wingPart = RelativeAngleWingMotion.PART_LEFT;
                    break;
                case WINGS_RIGHT:
                    wingPart = RelativeAngleWingMotion.PART_RIGHT;
                    break;
                case WINGS_BOTH:
                    wingPart = RelativeAngleWingMotion.PART_BOTH;
                    break;
                default:
                    Log.e("Tai", TAG + ": doWingMotion: " + "invalid switch case");
                    throw new IllegalStateException("Unexpected value: " + wingPart);
            }
            switch(action) {
                case WINGS_UP:
                    wingAction = RelativeAngleWingMotion.PART_LEFT;
                    break;
                case WINGS_DOWN:
                    wingAction = RelativeAngleWingMotion.PART_RIGHT;
                    break;
                default:
                    Log.e("Tai", TAG + ": doWingMotion: " + "invalid switch case");
                    throw new IllegalStateException("Unexpected value: " + wingPart);
            }
            RelativeAngleWingMotion relativeAngleWingMotion = new RelativeAngleWingMotion(wingPart, wingSpeed, wingAction, wingAngle);
            wingMotionManager.doRelativeAngleMotion(relativeAngleWingMotion);
        }

    void headLeft() {
        String fn = "headLeft";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_LEFT, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);;
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_HORIZONTAL, DRelativeAngleHeadMotion.ACTION_LEFT, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    void headRight() {
        String fn = "headRight";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_RIGHT, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_HORIZONTAL, DRelativeAngleHeadMotion.ACTION_RIGHT, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    void headDown() {
        String fn = "headDown";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_DOWN, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_VERTICAL, RelativeAngleHeadMotion.ACTION_DOWN, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }
    void headUp() {
        String fn = "headUp";
        switch (robotType) {
            case "Elf":
                RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_UP, headAngle);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
                break;
            case "Max":
                DRelativeAngleHeadMotion dRelativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_VERTICAL, RelativeAngleHeadMotion.ACTION_UP, headSpeed, headAngle);
                headMotionManager.doDRelativeAngleMotion(dRelativeAngleHeadMotion);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }
    }

    byte ledPart;
    byte ledMode;
    byte delayTime;
    byte randomCount;
    public void lightLed(int part, int mode) {
        switch (part) {
            case LED_ALL:
                this.ledPart = LED.PART_ALL;
                break;
            case LED_LEFT_HAND:
                this.ledPart = LED.PART_LEFT_HAND;
                break;
            case LED_LEFT_HEAD:
                this.ledPart = LED.PART_LEFT_HEAD;
                break;
            case LED_RIGHT_HEAD:
                this.ledPart = LED.PART_RIGHT_HEAD;
                break;
            case LED_RIGHT_HAND:
                this.ledPart = LED.PART_RIGHT_HAND;
                break;
            case LED_WHEEL:
                this.ledPart = LED.PART_WHEEL;
                break;
        }
        switch(mode) {
            case LED_BLUE:
                this.ledMode = LED.MODE_BLUE;
                break;
            case LED_GREEN:
                this.ledMode = LED.MODE_GREEN;
                break;
            case LED_WHITE:
                this.ledMode = LED.MODE_WHITE;
                break;
            case LED_RED:
                this.ledMode = LED.MODE_RED;
                break;
            case LED_PINK:
                this.ledMode = LED.MODE_PINK;
                break;
            case LED_PURPLE:
                this.ledMode = LED.MODE_PURPLE;
                break;
            case LED_YELLOW:
                this.ledMode = LED.MODE_YELLOW;
                break;
            case LED_FLICKER_YELLOW:
                this.ledMode = LED.MODE_FLICKER_YELLOW;
                break;
            case LED_FLICKER_BLUE:
                this.ledMode = LED.MODE_FLICKER_BLUE;
                break;
            case LED_FLICKER_GREEN:
                this.ledMode = LED.MODE_FLICKER_GREEN;
                break;
            case LED_FLICKER_WHITE:
                this.ledMode = LED.MODE_FLICKER_WHITE;
                break;
            case LED_FLICKER_RED:
                this.ledMode = LED.MODE_FLICKER_RED;
                break;
            case LED_FLICKER_PINK:
                this.ledMode = LED.MODE_FLICKER_PINK;
                break;
            case LED_FLICKER_PURPLE:
                this.ledMode = LED.MODE_FLICKER_PURPLE;
                break;
            case LED_FLICKER_RANDOM:
                this.ledMode = LED.MODE_FLICKER_RANDOM;
                break;
            case LED_OFF:
                this.ledMode = LED.MODE_CLOSE;
                break;

        }
        hardwareManager.setLED(new LED(ledPart,ledMode,delayTime,randomCount));
    }




    public void doNodNo(){
        doNodNoRight();
        doNodNoLeft();
    }
    public void doRightOk(){
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                CombinationHandMotion combinationHandMotion = new CombinationHandMotion( CombinationHandMotion.PART_RIGHT, CombinationHandMotion.MOTION_HAND_OK, CombinationHandMotion.ACTION_START);
                handMotionManager.doCombinationMotionReset(combinationHandMotion, true);
            }
        }, 100);
//            CombinationHandMotion combinationHandMotion = new CombinationHandMotion( CombinationHandMotion.PART_RIGHT, CombinationHandMotion.MOTION_HAND_OK, CombinationHandMotion.ACTION_START);
//            handMotionManager.doCombinationMotionReset(combinationHandMotion, true);
        Log.d("RHO", "Right hand ok cleared");
        //handMotionManager.doResetMotion();
    }
    public void doNodNoRight(){
        headMotionManager.doResetMotion();
        DRelativeAngleHeadMotion relativeAngleHeadMotion = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_HORIZONTAL, DRelativeAngleHeadMotion.ACTION_RIGHT,55,20 );
        headMotionManager.doDRelativeAngleMotion(relativeAngleHeadMotion);
        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                DRelativeAngleHeadMotion relativeAngleHeadMotionDOWN = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_HORIZONTAL, DRelativeAngleHeadMotion.ACTION_LEFT, 55, 20);
                headMotionManager.doDRelativeAngleMotion(relativeAngleHeadMotionDOWN);
            }
        },1000);
        headMotionManager.doResetMotion();
    }
    public void doNodNoLeft(){
        headMotionManager.doResetMotion();
        RelativeAngleHeadMotion relativeAngleHeadMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_LEFT,55);
        headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotion);
        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                RelativeAngleHeadMotion relativeAngleHeadMotionDOWN = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_RIGHT, 55);
                headMotionManager.doRelativeAngleMotion(relativeAngleHeadMotionDOWN);
            }
        },1000);
        headMotionManager.doResetMotion();
        Log.d("MOTION", "Head motion complete");
    }
    public void doBowForward() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                RelativeAngleWaistMotion relativeAngleWaistMotion = new RelativeAngleWaistMotion(RelativeAngleWaistMotion.ACTION_FRONT, 10, 15);
                waistMotionManager.doRelativeAngleMotion(relativeAngleWaistMotion);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        RelativeAngleWaistMotion relativeAngleWaistMotion = new RelativeAngleWaistMotion(RelativeAngleWaistMotion.ACTION_BACK, 10, 15);
                        waistMotionManager.doRelativeAngleMotion(relativeAngleWaistMotion);
                    }
                }, 1500);
            }
        }, 100);
    }
    public void doNodYes() {
        //headMotionManager.doResetMotion();
        DRelativeAngleHeadMotion relativeAngleHeadMotionDOWN = new DRelativeAngleHeadMotion(DRelativeAngleHeadMotion.DIRECTION_HEAD_VERTICAL, DRelativeAngleHeadMotion.ACTION_DOWN,10,20 );
        headMotionManager.doDRelativeAngleMotion(relativeAngleHeadMotionDOWN);
        Log.d("TAG", "Up motion cleared");
        new Timer().schedule(new TimerTask(){
            @Override
            public void run(){
                headMotionManager.doResetMotion();
            }
        },1000);
        Log.d("TAG", "Head nod yes cleared");
    }
    public void doLeftPeace(){
        CombinationHandMotion combinationHandMotion = new CombinationHandMotion( CombinationHandMotion.PART_RIGHT, CombinationHandMotion.MOTION_HAND_V, CombinationHandMotion.ACTION_START);
        handMotionManager.doCombinationMotionReset(combinationHandMotion, true);
        Log.d("PEACE", "Motion hand V cleared");
    }

    public void goForward() {
        DistanceWheelMotion distanceWheelMotionF = new DistanceWheelMotion(DistanceWheelMotion.ACTION_FORWARD_RUN, speed, distance);
        switch (this.robotType) {
            case "Elf":
                wheelMotionManager.doDistanceMotion(distanceWheelMotionF);
//                    refF.update("switch", 0);
                wheelMotionManager.setWheelMotionListener(new WheelMotionManager.WheelMotionListener() {
                    @Override
                    public void onWheelStatus(String s) {
                        Log.i("Tai", TAG + ": goForward" + robotType + ": onWheelStatus: " + s);
                    }
                });
                break;
            case "Max":
                wheelMotionManager.doDistanceMotion(distanceWheelMotionF);
//                    refF.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + "goForward: refF: unspecified robotType");
                break;
        }
    }
    public void goLeft() {
        RelativeAngleWheelMotion relativeAngleWheelMotion = new RelativeAngleWheelMotion(RelativeAngleWheelMotion.TURN_LEFT, this.turnSpeed, this.turnAngle);
        switch (robotType) {
            case "Elf":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
//                    refL.update("switch", 0);
                wheelMotionManager.setWheelMotionListener(new WheelMotionManager.WheelMotionListener() {
                    @Override
                    public void onWheelStatus(String s) {
                        Log.i("Tai", TAG + ": goLeft" + robotType + ": onWheelStatus: " + s);
                    }
                });
                break;
            case "Max":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
//                    refL.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": goLeft: " + "unspecified robotType");
                break;
        }
    }
    public void goRight() {
        String fn = "goRight";
        RelativeAngleWheelMotion relativeAngleWheelMotion = new RelativeAngleWheelMotion(RelativeAngleWheelMotion.TURN_RIGHT, this.turnSpeed, this.turnAngle);
        switch (this.robotType) {
            case "Elf":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
//                    refR.update("switch", 0);
                wheelMotionManager.setWheelMotionListener(new WheelMotionManager.WheelMotionListener() {
                    @Override
                    public void onWheelStatus(String s) {
                        Log.i("Tai", TAG + ": " + fn + robotType + ": onWheelStatus: " + s);
                    }
                });
                break;
            case "Max":
                wheelMotionManager.doRelativeAngleMotion(relativeAngleWheelMotion);
//                    refR.update("switch", 0);
                break;
            default:
                Log.d("Tai", TAG + ": " + fn + ": " + "unspecified robotType");
                break;
        }

    }
    public void stop() {
        String fn = "stop";
        DistanceWheelMotion distanceWheelMotion = new DistanceWheelMotion(DistanceWheelMotion.ACTION_STOP_RUN, 5, 100);
        switch (this.robotType) {
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
//                    refS.update("switch", 0);
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
}
