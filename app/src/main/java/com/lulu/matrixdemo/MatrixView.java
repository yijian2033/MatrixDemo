package com.lulu.matrixdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * create by zyj
 * on 2019/6/13
 **/
public class MatrixView extends View {

    private Matrix matrix;

    public MatrixView(Context context) {
        this(context, null);
    }

    public MatrixView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MatrixView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.img_home_pic01);
        matrix = new Matrix();
    }

    private Bitmap mBitmap;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rect rect = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        canvas.drawBitmap(mBitmap, rect, rect, null);

        matrix.setTranslate(mBitmap.getWidth(), mBitmap.getHeight());
//        canvas.drawBitmap(mBitmap, matrix, null);
//
//        matrix.setScale(2,2);


//        matrix.preRotate(135);

        canvas.drawBitmap(mBitmap, matrix, null);
    }
}
