package com.ctrlrobotics.ctrl.slam;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.ctrlrobotics.ctrl.R;
import com.sanbot.map.PositionTag;

import java.util.ArrayList;
import java.util.List;

/**
 * "Functional Description"
 * <p/>
 * Created by xieziqi on 2018/4/13
 * Copyright (c) 2016 QihanCloud, Inc. All Rights Reserved.
 */

public class MapView extends View {
    private final List<PositionTag> mList=new ArrayList<>();
    private Bitmap mBitmap;
    private Paint mPaint;
    private int mWidth, mHeigth;
    private float currX;
    private float currY;
    private float angle;
    private Bitmap cBitmap;
    private final List<PositionTag> globalPath=new ArrayList<>();
    private final List<PositionTag> localPath=new ArrayList<>();

    public MapView(Context context) {
        super(context);
        init();
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);
        cBitmap= BitmapFactory.decodeResource(getContext().getResources(), R.drawable.arror);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mBitmap != null) {
            mWidth = mBitmap.getWidth();
            mHeigth = mBitmap.getHeight();
        }
        Log.d("xzq", "onMeasure mWidth = " + mWidth + " mHeigth = " + mHeigth);
        this.setMeasuredDimension(mWidth, mHeigth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mPaint);
            drawGlobalPath(canvas);
            drawLocalPath(canvas);
            drawPoint(canvas);
            drawPosition(canvas);
        }
    }

    private void drawPoint(Canvas canvas) {
        int mTagTextSize = 20;
        mPaint.setTextSize(mTagTextSize);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setStrokeWidth(2);
        for (int i = 0; i < mList.size(); i++) {
            mPaint.setColor(Color.parseColor("#666666"));
            PositionTag pt=mList.get(i);
            float x = (float) pt.getX();
            float y = (float)pt.getY();
            float mTagRadius = 6f;
            canvas.drawCircle(x, y, mTagRadius, mPaint);
            double ex= Math.cos(pt.getRadians())*20;
            double ey= Math.sin(pt.getRadians())*20;
            //canvas.drawLine(x,y,x+(float)ex,y-(float)ey,mPaint);
            drawAL(canvas,x,y,x+(float)ex,y-(float)ey);
            canvas.drawText(mList.get(i).getName(),x,y+ mTagRadius + mTagTextSize,mPaint);
        }
    }

    private void drawAL(Canvas canvas, float sx, float sy, float ex, float ey)
    {
        double H = 8; // 箭头高度
        double L = 4; // 底边的一半
        double awrad = Math.atan(L / H); // 箭头角度
        double arraow_len = Math.sqrt(L * L + H * H); // 箭头的长度
        double[] arrXY_1 = rotateVec(ex - sx, ey - sy, awrad, true, arraow_len);
        double[] arrXY_2 = rotateVec(ex - sx, ey - sy, -awrad, true, arraow_len);
        double x_3 = ex - arrXY_1[0]; // (x3,y3)是第一端点
        double y_3 = ey - arrXY_1[1];
        double x_4 = ex - arrXY_2[0]; // (x4,y4)是第二端点
        double y_4 = ey - arrXY_2[1];
        // 画线
        canvas.drawLine(sx, sy, ex, ey,mPaint);
        Path triangle = new Path();
        triangle.moveTo(ex, ey);
        triangle.lineTo((float)x_3, (float)y_3);
        triangle.lineTo((float)x_4, (float)y_4);
        triangle.close();
        canvas.drawPath(triangle,mPaint);
    }

    // 计算
    private double[] rotateVec(float px, float py, double ang, boolean isChLen, double newLen)
    {
        double mathstr[] = new double[2];
        // 矢量旋转函数，参数含义分别是x分量、y分量、旋转角、是否改变长度、新长度
        double vx = px * Math.cos(ang) - py * Math.sin(ang);
        double vy = px * Math.sin(ang) + py * Math.cos(ang);
        if (isChLen) {
            double d = Math.sqrt(vx * vx + vy * vy);
            vx = vx / d * newLen;
            vy = vy / d * newLen;
            mathstr[0] = vx;
            mathstr[1] = vy;
        }
        return mathstr;
    }

    private void drawPosition(Canvas canvas){
        canvas.save();
        canvas.translate(currX,currY);
        canvas.rotate(-angle);
        canvas.drawBitmap(cBitmap,-cBitmap.getWidth()/2f,-cBitmap.getHeight()/2f,mPaint);
        canvas.restore();
        //mPaint.setColor(Color.RED);
        //canvas.drawCircle(currX, currY, 4f, mPaint);
    }

    private void drawGlobalPath(Canvas canvas){
        if(globalPath.size()==0){
            return;
        }
        Path path=new Path();
        for(int i=0;i<globalPath.size();i++){
            PositionTag tag=globalPath.get(i);
            if(i==0){
                path.moveTo((float)tag.getX(),(float)tag.getY());
            }else {
                path.lineTo((float) tag.getX(), (float) tag.getY());
            }
        }
        mPaint.setColor(Color.parseColor("#99ccff"));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(8);
        canvas.drawPath(path,mPaint);
    }

    private void drawLocalPath(Canvas canvas){
        if(localPath.size()==0){
            return;
        }
        Path path=new Path();
        for(int i=0;i<localPath.size();i++){
            PositionTag tag=localPath.get(i);
            if(i==0){
                path.moveTo((float)tag.getX(),(float)tag.getY());
            }else {
                path.lineTo((float) tag.getX(), (float) tag.getY());
            }
        }
        mPaint.setColor(Color.parseColor("#ff6666"));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(8);
        canvas.drawPath(path,mPaint);
    }

    public void setupMapView(Bitmap bmp, List<PositionTag> list) {
        mList.clear();
        if(list!=null) {
            mList.addAll(list);
        }
        mBitmap = bmp;
        requestLayout();
    }

    public void setPosition(float x,float y,float angle){
        currX=x;
        currY=y;
        this.angle=angle;
        postInvalidate();
    }

    public void setGlobalPath(List<PositionTag> list){
        globalPath.clear();
        globalPath.addAll(list);
        postInvalidate();
    }

    public void setLocalPath(List<PositionTag> list){
        localPath.clear();
        localPath.addAll(list);
        postInvalidate();
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBitmap != null)
            mBitmap.recycle();
    }
}
