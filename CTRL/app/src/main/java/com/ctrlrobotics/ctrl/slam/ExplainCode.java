package com.ctrlrobotics.ctrl.slam;

import android.content.Context;

import com.ctrlrobotics.ctrl.R;
import com.sanbot.map.Msg;

public class ExplainCode {
    public static String getCtrl(int ctrl){
        String res="";
        switch (ctrl){
            case Msg.CtrlState.FREE:
                res="Idle state";
                break;
            case Msg.CtrlState.BUILD:
                res="In the process of mapping";
                break;
            case Msg.CtrlState.NAVIGATION:
                res="Navigation";
                break;
            case Msg.CtrlState.WORK_3D:
                res="3D working state";
                break;
        }
        return res;
    }

    public static String getNavi(Context ctx, int navi){
        String result;
        switch (navi) {
            case Msg.NaviState.NO_NAVI:
                result = ctx.getString(R.string.son_state_no_navi);
                break;
            case Msg.NaviState.NO_DEFINE:
                result = ctx.getString(R.string.son_state_no_define);
                break;
            case Msg.NaviState.NO_INIT:
                result = ctx.getString(R.string.son_state_no_init_pos);
                break;
            case Msg.NaviState.INIT:
                result = ctx.getString(R.string.son_state_init_pos);
                break;
            case Msg.NaviState.NAVI:
                result = ctx.getString(R.string.son_state_navi);
                break;
            case Msg.NaviState.ARRIVE:
                result = ctx.getString(R.string.son_state_arrive);
                break;
            case Msg.NaviState.CANCEL:
                result = ctx.getString(R.string.son_state_cancel_navi);
                break;
            case Msg.NaviState.ERROR:
                result = ctx.getString(R.string.son_state_program_exception);
                break;
            case Msg.NaviState.TOO_CLOSE:
                result = ctx.getString(R.string.son_state_obstacle_too_close);
                break;
            case Msg.NaviState.GLOBAL_PLAN_FAIL:
                result = ctx.getString(R.string.son_state_plan_fail);
                break;
            case Msg.NaviState.PLAN_IN_OBSTACLE:
                result =ctx.getString(R.string.son_state_target_in_obstacle);
                break;
            case Msg.NaviState.PLAN_ALL_BLOCK:
                result = ctx.getString(R.string.son_state_all_death);
                break;
            case Msg.NaviState.PLAN_BLOCK_BY_PEOPLE:// 全局轨迹上有人阻挡
                result = ctx.getString(R.string.son_state_people_stop);
                break;
            case Msg.NaviState.ROBOT_BLOCK_BY_PEOPLE:// 机器人被围住导致导航暂停
                result = ctx.getString(R.string.son_state_surrounded);
                break;
            case Msg.NaviState.PAUSE_BY_BUTTON:// 按下肩膀红色按钮导致导航暂停
                result = ctx.getString(R.string.son_state_red_btn);
                break;
            case Msg.NaviState.NO_MAP:
                result = ctx.getString(R.string.son_state_no_map);
                break;
            case Msg.NaviState.FAIL_3D:
                result = ctx.getString(R.string.son_state_3d_fail);
                break;
            case Msg.NaviState.WITHOUT_DATA_DRIVE:
                result = ctx.getString(R.string.son_state_no_chassis);
                break;
            case Msg.NaviState.WITHOUT_DATA_DRIVE_3D:
                result = ctx.getString(R.string.son_state_no_chassis_3d);
                break;
            case Msg.NaviState.WITHOUT_DATA_DRIVE_MAP:
                result = ctx.getString(R.string.son_state_no_chassis_map);
                break;
            case Msg.NaviState.WITHOUT_DATA_3D_DRIVE_MAP:
                result = ctx.getString(R.string.son_state_no_chassis_3d_map);
                break;
            case Msg.NaviState.INFRARED_OBSTACLE:
                result =ctx.getString(R.string.son_state_bottom_obstacle);
                break;
            case Msg.NaviState.POSITIONING_ERROR:
                result = ctx.getString(R.string.son_state_location_error);
                break;
            case Msg.NaviState.PLANING:
                result = ctx.getString(R.string.son_state_plan);
                break;
            case Msg.NaviState.PLAN_TIMEOUT_OBSTACLES:
                result = ctx.getString(R.string.son_state_target_obstacle);
                break;
            case Msg.NaviState.PLAN_TIMEOUT_SELF_IN_OBSTACLES:
                result = ctx.getString(R.string.son_state_self_obstacle_timeout);
                break;
            case Msg.NaviState.PLAN_TIMEOUT_NO_PATH:
                result = ctx.getString(R.string.son_state_no_path_timeout);
                break;
            case Msg.NaviState.PLAN_ERROR_SELF_IN_OBSTACLES:
                result = ctx.getString(R.string.son_state_self_obstacle);
                break;
            default:
                result = "" + navi;
                break;
        }
        //result=result+"  码:"+Integer.toHexString(navi);
        return result;
    }

    //导航状态为以下状态时建议作延时取消导航处理(time>0)
    public static long getCancelTime(int naviState){
        long time;
        switch (naviState) {
            case Msg.NaviState.ERROR://0x07
                time = 10000;
                break;
            case Msg.NaviState.GLOBAL_PLAN_FAIL://0x09
                time = 10000;
                break;
            case Msg.NaviState.PLAN_IN_OBSTACLE://0x13
                time = 30000;
                break;
            case Msg.NaviState.PLAN_ALL_BLOCK://0x14
                time=10000;
                break;
            case Msg.NaviState.NO_MAP://0x30
                time = 10000;
                break;
            case Msg.NaviState.FAIL_3D://0x31
                time = 10000;
                break;
            case Msg.NaviState.WITHOUT_DATA_DRIVE://0x32
                time = 10000;
                break;
            case Msg.NaviState.WITHOUT_DATA_DRIVE_3D://0x33
                time = 10000;
                break;
            case Msg.NaviState.WITHOUT_DATA_DRIVE_MAP://0x34
                time = 10000;
                break;
            case Msg.NaviState.WITHOUT_DATA_3D_DRIVE_MAP://0x35
                time = 10000;
                break;
            case Msg.NaviState.PLAN_TIMEOUT_OBSTACLES://0x39
                time = 10000;
                break;
            case Msg.NaviState.PLAN_TIMEOUT_SELF_IN_OBSTACLES://0x3a
                time = 10000;
                break;
            case Msg.NaviState.PLAN_TIMEOUT_NO_PATH://0x3b
                time = 10000;
                break;
            case Msg.NaviState.TOO_CLOSE://0x08
                time=10000;
                break;
            default:
                time=0;
                break;
        }
        return time;
    }


    public static String getMoveError(Context ctx, int state){
        String s;
        switch (state){
            case Msg.MoveError.MOTOR_COMMUNICATION_TIMEOUT://电机通信超时 1
                s=ctx.getString(R.string.move_error_comm_timeout);
                break;
            case Msg.MoveError.OBSTACLE_BACK://避障返回 1
                s=ctx.getString(R.string.move_error_obstacles);
                break;
            case Msg.MoveError.ARM_STATE_ERROR://手状态错误 1
                s=ctx.getString(R.string.move_error_arm);
                break;
            case Msg.MoveError.ARM_CONTROL_TIMEOUT://手控制超时错误 1
                s=ctx.getString(R.string.move_error_arm);
                break;
            case Msg.MoveError.NO_MAGNETIC1://磁道轨模式未检测磁条1
                s=ctx.getString(R.string.move_error_magnetic);
                break;
            case Msg.MoveError.RELIEVE_BRAKE_ERROR://刹车解除错误 1
                s=ctx.getString(R.string.move_error_brake);
                break;
            case Msg.MoveError.TOUCH_STOP_BACK://触摸停止返回 1
                s=ctx.getString(R.string.move_error_touch);
                break;
            case Msg.MoveError.MOTOR_PROTECT_BACK://电机保护返回 1
                s=ctx.getString(R.string.move_error_protect);
                break;
            case Msg.MoveError.CHARGING:
            case Msg.MoveError.CHARGE_BACK:
                s=ctx.getString(R.string.move_error_charge);
                break;
                default:
                    s=ctx.getString(R.string.move_error)+":"+state;
                    break;
        }
        return s;
    }

    public static String getObstacleSub(Context ctx, int status){
        String s="";
        switch (status){
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_1:
                //s="前面红外左1";
                s=ctx.getString(R.string.obstacle_front_infrared_left1);
                break;
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_2:
                //s="前面红外左2";
                s=ctx.getString(R.string.obstacle_front_infrared_left2);
                break;
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_3:
                //s="前面红外左3";
                s=ctx.getString(R.string.obstacle_front_infrared_left3);
                break;
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_4:
                //s="前面红外左4";
                s=ctx.getString(R.string.obstacle_front_infrared_left4);
                break;
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_5:
                //s="前面红外左5";
                s=ctx.getString(R.string.obstacle_front_infrared_left5);
                break;
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_6:
                //s="前面红外左6";
                s=ctx.getString(R.string.obstacle_front_infrared_left6);
                break;
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_7:
                //s="前面红外左7";
                s=ctx.getString(R.string.obstacle_front_infrared_left7);
                break;
            case Msg.ObstacleSub.IOBSTACLE_FRONT_LEFT_8:
                //s="前面红外左8";
                s=ctx.getString(R.string.obstacle_front_infrared_left8);
                break;
            case Msg.ObstacleSub.IAOBSTACLE_LEFT_FRONT:
                //s="左前手臂红外";
                s=ctx.getString(R.string.obstacle_left_front_arm_infrared);
                break;
            case Msg.ObstacleSub.IAOBSTACLE_LEFT_MIDDLE:
                //s="左中手臂红外";
                s=ctx.getString(R.string.obstacle_left_middle_arm_infrared);
                break;
            case Msg.ObstacleSub.IAOBSTACLE_LEFT_BEHIND:
                //s="左后手臂红外";
                s=ctx.getString(R.string.obstacle_left_after_arm_infrared);
                break;
            case Msg.ObstacleSub.IAOBSTACLE_RIGHT_FRONT:
                //s="右前手臂红外";
                s=ctx.getString(R.string.obstacle_right_front_arm_infrared);
                break;
            case Msg.ObstacleSub.IAOBSTACLE_RIGHT_MIDDLE:
                //s="右中手臂红外";
                s=ctx.getString(R.string.obstacle_right_middle_arm_infrared);
                break;
            case Msg.ObstacleSub.IAOBSTACLE_RIGHT_BEHIND:
                //s="右后手臂红外";
                s=ctx.getString(R.string.obstacle_right_after_arm_infrared);
                break;
            case Msg.ObstacleSub.IOBSTACLE_BEHIND_LEFT:
                //s="背后左红外";
                s=ctx.getString(R.string.obstacle_after_left_infrared);
                break;
            case Msg.ObstacleSub.IOBSTACLE_BEHIND_RIGHT:
                //s="背后右红外";
                s=ctx.getString(R.string.obstacle_after_right_infrared);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_1:
                //s="防摔红外左1";
                s=ctx.getString(R.string.obstacle_drop_infrared_left1);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_2:
                //s="防摔红外左2";
                s=ctx.getString(R.string.obstacle_drop_infrared_left2);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_3:
                //s="防摔红外左3";
                s=ctx.getString(R.string.obstacle_drop_infrared_left3);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_4:
                //s="防摔红外左4";
                s=ctx.getString(R.string.obstacle_drop_infrared_left4);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_5:
                //s="防摔红外左5";
                s=ctx.getString(R.string.obstacle_drop_infrared_left5);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_6:
                //s="防摔红外左6";
                s=ctx.getString(R.string.obstacle_drop_infrared_left6);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_7:
                //s="防摔红外左7";
                s=ctx.getString(R.string.obstacle_drop_infrared_left7);
                break;
            case Msg.ObstacleSub.IOBSTACLE_UNBREAK_LEFT_8:
                //s="防摔红外左8";
                s=ctx.getString(R.string.obstacle_drop_infrared_left8);
                break;
            case Msg.ObstacleSub.UOBSTACLE_BEHIND:
                //s="背后超声波";
                s=ctx.getString(R.string.obstacle_after_ultrasonic);
                break;
            case Msg.ObstacleSub.UOBSTACLE_BOTTOM_LEFT_FRONT_1:
                //s="底下左前超声波1";
                s=ctx.getString(R.string.obstacle_bottom_left_front_ultrasonic1);
                break;
            case Msg.ObstacleSub.UOBSTACLE_BOTTOM_LEFT_FRONT_2:
                //s="底下左前超声波2";
                s=ctx.getString(R.string.obstacle_bottom_left_front_ultrasonic2);
                break;
            case Msg.ObstacleSub.UOBSTACLE_BOTTOM_LEFT_FRONT_3:
                //s="底下左前超声波3";
                s=ctx.getString(R.string.obstacle_bottom_left_front_ultrasonic3);
                break;
            case Msg.ObstacleSub.UOBSTACLE_BOTTOM_LEFT_FRONT_4:
                //s="底下左前超声波4";
                s=ctx.getString(R.string.obstacle_bottom_left_front_ultrasonic4);
                break;
            case Msg.ObstacleSub.UOBSTACLE_BOTTOM_LEFT_FRONT_5:
                //s="底下左前超声波5";
                s=ctx.getString(R.string.obstacle_bottom_left_front_ultrasonic5);
                break;
            case Msg.ObstacleSub.UOBSTACLE_CHEST_LEFT:
                //s="胸前左超声波";
                s=ctx.getString(R.string.obstacle_chest_left_ultrasonic);
                break;
            case Msg.ObstacleSub.UOBSTACLE_CHEST_RIGHT:
                //s="胸前右超声波";
                s=ctx.getString(R.string.obstacle_chest_right_ultrasonic);
                break;
            case Msg.ObstacleSub.DOBSTACLE_HEAD:
                //s="头部3D";
                s=ctx.getString(R.string.obstacle_head_3d);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_1:
                //s="底部左前 3D1";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d1);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_2:
                //s="底部左前 3D2";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d2);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_3:
                //s="底部左前 3D3";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d3);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_4:
                //s="底部左前 3D4";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d4);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_5:
                //s="底部左前 3D5";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d5);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_6:
                //s="底部左前 3D6";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d6);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_7:
                //s="底部左前 3D7";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d7);
                break;
            case Msg.ObstacleSub.DOBSTACLE_BOTTOM_LEFT_FRONT_8:
                //s="底部左前 3D8";
                s=ctx.getString(R.string.obstacle_bottom_left_front_3d8);
                break;
        }
        return s;
    }
}
