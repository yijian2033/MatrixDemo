package com.lulu.matrixdemo;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;

/**
 * create by zyj
 * on 2019/6/14
 **/
public class ZoomImage extends AppCompatImageView implements ViewTreeObserver.OnGlobalLayoutListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    //确保只加载一次
    private boolean onece;

    //缩放的初始化值
    private float mInitSale;

    //缩放的最大值
    private float mMaxScale;

    //双击放大的值
    private float mMidScale;
    private Matrix mScaleMatrix;
    private ScaleGestureDetector mScaleGestureDetector;

    //---------------------- 自由移动   ----------------------------------

    //记录上一次多点触控的数量
    private int mLastPointCount;
    //上一次的中心点
    private float mLastX;
    private float mLastY;

    //移动的标准值
    private int mTouchSlop;

    private boolean isCanDrag;

    private boolean isCheckLeftAndRight;
    private boolean isCheckTopAndBottom;

    //--------------------双击放大和缩小----------------
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;

    public ZoomImage(Context context) {
        this(context, null);
    }

    public ZoomImage(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mScaleMatrix = new Matrix();
        setScaleType(ScaleType.MATRIX);

        //手指多点触控
        mScaleGestureDetector = new ScaleGestureDetector(context, this);
        //触摸
        setOnTouchListener(this);
        //获取标准值,移动的标准值
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        //双击事件
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {//双击
                if (isAutoScale) {
                    return true;
                }
                //当前比例如果小于midScale,就放大为midScale,其他的所有情况，都缩小为initScale;
                //获取中心点
                float x = e.getX();
                float y = e.getY();
                float scale = getScale();
                if (scale < mMidScale) {//放大
//                    mScaleMatrix.postScale(mMidScale / scale, mMidScale / scale, x, y);
//                    setImageMatrix(mScaleMatrix);

                    postDelayed(new AutoScaleRunnable(mMidScale, x, y), 16);
                    isAutoScale = true;
                } else {//缩小为初始值
//                    mScaleMatrix.postScale(mInitSale / scale, mInitSale / scale, x, y);
//                    setImageMatrix(mScaleMatrix);
                    postDelayed(new AutoScaleRunnable(mInitSale, x, y), 16);
                    isAutoScale = true;
                }
                return true;
            }
        });

    }


    public class AutoScaleRunnable implements Runnable {

        /**
         * 缩放的目标量
         */
        private float mTargetScale;
        //中心点
        private float x;
        private float y;

        private final float SMALL = 0.93f;
        private final float BIGGER = 1.03f;

        private float tempScale;

        public AutoScaleRunnable(float mTargetScale, float x, float y) {
            this.mTargetScale = mTargetScale;
            this.x = x;
            this.y = y;
            if (getScale() < mTargetScale) {
                tempScale = BIGGER;
            }
            if (getScale() > mTargetScale) {
                tempScale = SMALL;
            }
        }

        @Override
        public void run() {
            //进行缩放
            mScaleMatrix.postScale(tempScale, tempScale, x, y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);

            float currentScale = getScale();
            //相放大而且是可以放大的
            //想缩小而且是可以缩小的
            if ((tempScale > 1.0f && currentScale < mTargetScale) || (tempScale < 1.0f && currentScale > mTargetScale)) {
                //执行run方法
                postDelayed(this, 16);
            } else {
                //达到目标值，就设置为目标值
                float scale = mTargetScale / currentScale;
                mScaleMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mScaleMatrix);
                isAutoScale = false;
            }

        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //添加布局完成之后的监听
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    /**
     * 布局完成之后，开始计算宽高，主要是对图片进行缩放
     */
    @Override
    public void onGlobalLayout() {
        if (!onece) {
            //获取控件的宽高
            int width = getWidth();
            int height = getHeight();
            //缩放比例
            float scale = 1.0f;
            //获取图片
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return;
            }
            //获取图片的宽高
            int dWidth = drawable.getIntrinsicWidth();
            int dHeight = drawable.getIntrinsicHeight();

            //如果图片宽大于控件的宽
            if (width < dWidth && height > dHeight) {
                scale = width * 1.0f / dWidth;
            }
            //如果图片的高度大于控件的高度
            if (width > dWidth && height < dHeight) {
                scale = height * 1.0f / dHeight;
            }

            //如果图片的宽度和高度都大于控件的,取它们比例的最小值，进行缩放
            //如果图片的宽高都小于控件的宽高，进行放大
            if ((width < dWidth && height < dHeight) || (width > dWidth && height > dHeight)) {
                scale = Math.min(width * 1.0f / dWidth, height * 1.0f / dHeight);
            }

            mInitSale = scale;
            mMidScale = scale * 2;
            mMaxScale = scale * 4;

            //将图片移到中心点
            int x = width / 2 - dWidth / 2;
            int y = height / 2 - dHeight / 2;
            mScaleMatrix.postTranslate(x, y);
            //然后进行缩放
            mScaleMatrix.postScale(mInitSale, mInitSale, width / 2, height / 2);
            setImageMatrix(mScaleMatrix);

            onece = true;
        }
    }

    /**
     * 获取缩放的值，因为x和Y的缩放值是一样的
     * <p>
     * xScale  xSkew  xTrans
     * ySkew   yScale yTrans
     * 0       0       0
     *
     * @return
     */
    public float getScale() {
        float[] value = new float[9];
        mScaleMatrix.getValues(value);
        return value[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {

        float scaleFactor = detector.getScaleFactor();
        float scale = getScale();
        Log.d("scale_outside", "scale : " + scale + "\n  ---- scaleFactor :" + scaleFactor);
        //缩放范围控制
        //当前缩放小于最大的缩放，并且 scaleFactor>1表明还可以放大
        //当前缩放大于最小的缩放，并且 scaleFactor<1 表明还可以缩小
        if ((scale < mMaxScale && scaleFactor > 1.0f) || (scale > mInitSale && scaleFactor < 1.0f)) {

            if (scale * scaleFactor < mInitSale) {
                scaleFactor = mInitSale / scale;
            }

            if (scale * scaleFactor > mMaxScale) {
                scale = mMaxScale / scale;
            }
            Log.d("scale_inside", "scale : " + scale + "\n  ---- scaleFactor :" + scaleFactor);
            //缩放
            mScaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            //防止图片空白和检查图片居中
            checkBorderAndCenterWhenScale();
            setImageMatrix(mScaleMatrix);
        }

        return true;
    }

    /**
     * 图片缩放的时候边界控制和位置控制
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rect = getMatrixRectF();

        //控制图片的边界
        //移动的距离
        float deltaX = 0;
        float deltaY = 0;

        //判断整个图片是不是大于控件宽度
        if (rect.width() >= getWidth()) {

            if (rect.left > 0) {//左边界
                deltaX = -rect.left;//向左移动
            }

            if (rect.right < getWidth()) {//右边界
                deltaX = getWidth() - rect.right;
            }
        }

        //垂直方向
        if (rect.height() >= getHeight()) {
            if (rect.top > 0) {
                deltaY = -rect.top;
            }
            if (rect.bottom < getHeight()) {
                deltaY = getHeight() - rect.bottom;
            }
        }

        //如果宽度或者高度小于控件的宽度或者高度，让其居中
        if (rect.width() < getWidth()) {
            deltaX = getWidth() / 2f - rect.right + rect.width() / 2f;
        }

        if (rect.height() < getHeight()) {
            deltaY = getHeight() / 2f - rect.bottom + rect.height() / 2f;
        }
        mScaleMatrix.postTranslate(deltaX, deltaY);

    }

    /**
     * 获取缩放后图片的大小和位置
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mScaleMatrix;
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }


    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        //双击注册
        if (mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        //手指缩放注册
        mScaleGestureDetector.onTouchEvent(event);

        float x = 0;
        float y = 0;
        int pointerCount = event.getPointerCount();
        //获取平均值也就是中心点
        for (int i = 0; i < pointerCount; i++) {
            x += event.getX(i);
            y += event.getY(i);
        }
        //获取平均值
        x /= pointerCount;
        y /= pointerCount;
        //还在移动中
        if (mLastPointCount != pointerCount) {
            //手指发生改变，重新判断
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointCount = pointerCount;

        //防止和viewpager的冲突
        RectF rf = getMatrixRectF();

        //移动时候的判定
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                //放大之后可以移动
                if (rf.width() > getWidth() + 0.01 || rf.height() > getHeight() + 0.01) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (rf.width() > getWidth() + 0.01 || rf.height() > getHeight() + 0.01) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                //移动的距离
                float dx = x - mLastX;
                float dy = y - mLastY;
                //如果移动的话
                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                if (isCanDrag) {//可以移动就完成图片的移动
                    RectF rectF = getMatrixRectF();
                    if (getDrawable() != null) {
                        isCheckLeftAndRight = isCheckTopAndBottom = true;
                        //图片的宽度么有超过控件的宽度就不可以移动
                        if (rectF.width() < getWidth()) {
                            isCheckLeftAndRight = false;
                            dx = 0;
                        }
                        //图片的高度没有超过控件的高度就不可以移动
                        if (rectF.height() < getHeight()) {
                            isCheckTopAndBottom = false;
                            dy = 0;
                        }
                        mScaleMatrix.postTranslate(dx, dy);

                        checkBorderWhenTranslate();
                        setImageMatrix(mScaleMatrix);
                    }

                }
                //不断记录上一次的xy
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //手指抬起就没有了
                mLastPointCount = 0;
                break;
        }


        return true;
    }


    /**
     * 自由移动时候的边界控制,防止空白部分出现
     */
    private void checkBorderWhenTranslate() {
        RectF matrixRectF = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (matrixRectF.top > 0 && isCheckTopAndBottom) {
            deltaY = -matrixRectF.top;
        }

        if (matrixRectF.bottom < height && isCheckTopAndBottom) {
            deltaY = height - matrixRectF.bottom;
        }

        if (matrixRectF.left > 0 && isCheckLeftAndRight) {
            deltaX = -matrixRectF.left;
        }

        if (matrixRectF.right < width && isCheckLeftAndRight) {
            deltaX = width - matrixRectF.right;
        }

        mScaleMatrix.postTranslate(deltaX, deltaY);

    }

    /**
     * 判断是否移动
     *
     * @param dx
     * @param dy
     * @return
     */
    private boolean isMoveAction(float dx, float dy) {

        //勾股定理
        double sqrt = Math.sqrt(dx * dx + dy * dx);
        //然后再和标准值做比较，如果大于0的话，是可以移动的
        return sqrt > mTouchSlop;
    }

}
