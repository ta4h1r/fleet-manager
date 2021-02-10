package com.ctrlrobotics.ctrl.presentation;

import android.util.Log;

import com.sanbot.opensdk.function.beans.wing.NoAngleWingMotion;
import com.sanbot.opensdk.function.unit.HandMotionManager;
import com.sanbot.opensdk.function.unit.WingMotionManager;

public class AnimBuilder {

    private String TAG = "PresentationActivity";
    private static HandMotionManager handMotionManager;
    private AnimatronicsHelper animatronicsHelper;
    private WingMotionManager wingMotionManager;

    private int sleepTime;
    private int angle;
    private int speed;
    private int wingSpeed;
    private int wingAngle;
    private int headSpeed;
    private int headAngle;
    private String robotType;

    public AnimBuilder(AnimatronicsHelper animatronicsHelper, HandMotionManager handMotionManager, WingMotionManager wingMotionManager) {
        this.handMotionManager = handMotionManager;
        this.animatronicsHelper = animatronicsHelper;
        this.wingMotionManager = wingMotionManager;
        this.sleepTime = 1000;         // time window for action to complete
        this.speed = 5;
        this.wingSpeed = 5;
        this.wingAngle = 0;
        this.headSpeed = 5;
        this.headAngle = 0;
    }

    public void setSleepTime(int sleepTime) {this.sleepTime = sleepTime;}
    public void setAngle(int angle) {this.angle = angle;}
    public void setSpeed(int speed) {this.speed = speed;}
    public void setWingSpeed(int speed) {this.wingSpeed = speed;}
    public void setWingAngle(int angle) {this.wingAngle = angle;}
    public void setHeadSpeed(int angle) {this.headSpeed = angle;}
    public void setHeadAngle(int angle) {this.headAngle = angle;}
    public void setRobotType(String type) {this.robotType = type;}

    public void turnRight() {
        long startTime = System.nanoTime();

        animatronicsHelper.setTurnAngle(angle);
        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_RIGHT);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_STOP);

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: Right turn: " + String.valueOf((endTime -
                startTime)));
    }
    public void turnLeft() {
        long startTime = System.nanoTime();

        animatronicsHelper.setTurnAngle(angle);
        animatronicsHelper.setTurnSpeed(speed);
        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_STOP);

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: Left turn: " + String.valueOf((endTime -
                startTime)));
    }
    public void goForward() {
        long startTime = System.nanoTime();

        animatronicsHelper.setDuration(sleepTime);
        animatronicsHelper.setSpeed(speed);
//        animatronicsHelper.doWheelTranslation(animatronicsHelper.WHEELS_FORWARD);
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_FORWARD);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_STOP);

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: Forward: " + String.valueOf((endTime -
                startTime)));
    }
    public void goBackward() {
        long startTime = System.nanoTime();

        animatronicsHelper.setDuration(sleepTime);
        animatronicsHelper.setSpeed(speed);
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_BACK);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_STOP);

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: Backward: " + String.valueOf((endTime -
                startTime)));
    }
    public void goLeft() {
        long startTime = System.nanoTime();

        animatronicsHelper.setDuration(sleepTime);
        animatronicsHelper.setSpeed(speed);
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_LEFT_TRANSLATION);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_STOP);

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: Left: " + String.valueOf((endTime -
                startTime)));
    }
    public void goRight() {
        long startTime = System.nanoTime();

        animatronicsHelper.setDuration(sleepTime);
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_RIGHT_TRANSLATION);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_STOP);

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: Right: " + String.valueOf((endTime -
                startTime)));
    }

    public void moveLeftWing() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doAbsWingMotion(animatronicsHelper.WINGS_LEFT);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveLeftWing: " + String.valueOf((endTime -
                startTime)));
    };
    public void moveRightWing() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doAbsWingMotion(animatronicsHelper.WINGS_RIGHT);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveRightWing: " + String.valueOf((endTime -
                startTime)));
    };
    public void moveBothWings() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doAbsWingMotion(animatronicsHelper.WINGS_BOTH);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveBothWings: " + String.valueOf((endTime -
                startTime)));
    };

    public void moveLeftWingUp() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doRelWingMotion(animatronicsHelper.WINGS_LEFT, animatronicsHelper.WINGS_UP);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveLeftWingUp: " + String.valueOf((endTime -
                startTime)));
    };
    public void moveLeftWingDown() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doRelWingMotion(animatronicsHelper.WINGS_LEFT, animatronicsHelper.WINGS_DOWN);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveLeftWingDown: " + String.valueOf((endTime -
                startTime)));
    };
    public void moveRightWingUp() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doRelWingMotion(animatronicsHelper.WINGS_RIGHT, animatronicsHelper.WINGS_UP);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveRightWingUp: " + String.valueOf((endTime -
                startTime)));
    };
    public void moveRightWingDown() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doRelWingMotion(animatronicsHelper.WINGS_RIGHT, animatronicsHelper.WINGS_DOWN);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveRightWingDown: " + String.valueOf((endTime -
                startTime)));
    };
    public void moveBothWingsUp() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doRelWingMotion(animatronicsHelper.WINGS_BOTH, animatronicsHelper.WINGS_UP);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveBothWingsUp: " + String.valueOf((endTime -
                startTime)));
    };
    public void moveBothWingsDown() {
        long startTime = System.nanoTime();

        animatronicsHelper.setWingSpeed(wingSpeed);
        animatronicsHelper.setWingAngle(wingAngle);
        animatronicsHelper.doRelWingMotion(animatronicsHelper.WINGS_BOTH, animatronicsHelper.WINGS_DOWN);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveBothWingsDown: " + String.valueOf((endTime -
                startTime)));
    };

    public void moveHeadUp() {
        long startTime = System.nanoTime();

        animatronicsHelper.setHeadAngle(angle);
        animatronicsHelper.setHeadSpeed(speed);
        animatronicsHelper.setRobotType(robotType);
        animatronicsHelper.headUp();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveHeadUp: " + String.valueOf((endTime -
                startTime)));
    }
    public void moveHeadDown() {
        long startTime = System.nanoTime();

        animatronicsHelper.setHeadAngle(angle);
        animatronicsHelper.setHeadSpeed(speed);
        animatronicsHelper.setRobotType(robotType);
        animatronicsHelper.headDown();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveHeadDown: " + String.valueOf((endTime -
                startTime)));
    }
    public void moveHeadLeft() {
        long startTime = System.nanoTime();

        animatronicsHelper.setHeadAngle(angle);
        animatronicsHelper.setHeadSpeed(speed);
        animatronicsHelper.setRobotType(robotType);
        animatronicsHelper.headLeft();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveHeadLeft: " + String.valueOf((endTime -
                startTime)));
    }
    public void moveHeadRight() {
        long startTime = System.nanoTime();

        animatronicsHelper.setHeadAngle(angle);
        animatronicsHelper.setHeadSpeed(speed);
        animatronicsHelper.setRobotType(robotType);
        animatronicsHelper.headRight();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: moveHeadRight: " + String.valueOf((endTime -
                startTime)));
    }

    void resetWings() {
        long startTime = System.nanoTime();

        NoAngleWingMotion noAngleWingMotion = new
                NoAngleWingMotion(NoAngleWingMotion.PART_BOTH,
                8,NoAngleWingMotion.ACTION_RESET);
        wingMotionManager.doNoAngleMotion(noAngleWingMotion);
        try{
            Thread.sleep(sleepTime);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        Log.d(TAG, "TIMER: resetWings: " + String.valueOf((endTime -
                startTime)));
    }

    // Max only
    public void leftWave() {
        try {
            animatronicsHelper.setResetMotion(true);
            animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_WAVE);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void rightWave() {
        try {
            animatronicsHelper.setResetMotion(true);
            animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_WAVE);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void bothWave() {
        try {
            animatronicsHelper.setResetMotion(true);
            animatronicsHelper.doHandMotion(animatronicsHelper.BOTH_ARMS, animatronicsHelper.ARMS_WAVE);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void bothMuscles() {
        try {
            animatronicsHelper.setResetMotion(true);
            animatronicsHelper.doHandMotion(animatronicsHelper.BOTH_ARMS, animatronicsHelper.ARMS_MUSCLE);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void headYes() {
        try {
            animatronicsHelper.doNodYes();
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void headNo() {
        try {
            animatronicsHelper.doNodNo();
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }






    final Runnable runnableStop = new Runnable() {
        @Override
        public void run() {
            animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_STOP);
        }
    };
    final Runnable resetHands = new Runnable() {
        @Override
        public void run() {
            try {
                handMotionManager.doResetMotion();
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    final Runnable runnable1 = new Runnable() {
        @Override
        public void run() {
            try {
                animatronicsHelper.doHandMotion(animatronicsHelper.LEFT_ARM, animatronicsHelper.ARMS_STRETCH_HIGH_NARROW);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    final Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            try {
                animatronicsHelper.doHandMotion(animatronicsHelper.RIGHT_ARM, animatronicsHelper.ARMS_STRETCH_HIGH_NARROW);
                Thread.sleep(sleepTime);
//                sleepTime = 1000;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    final Runnable runnable13 = new Runnable() {
        @Override
        public void run() {
            long startTime = System.nanoTime();

            animatronicsHelper.setDuration(sleepTime);
            animatronicsHelper.doWheelRotation(animatronicsHelper.WHEELS_TURN_LEFT);
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            animatronicsHelper.doWheelDuration(animatronicsHelper.WHEELS_STOP);

            long endTime = System.nanoTime();
            Log.d(TAG, "TIMERr13: " + String.valueOf((endTime -
                    startTime)));
        }
    };

}
