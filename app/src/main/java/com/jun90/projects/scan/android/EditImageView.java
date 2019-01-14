package com.jun90.projects.scan.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import com.jun90.projects.scan.support.AbsolutePoint;
import com.jun90.projects.scan.support.AdjustmentScanTask;
import com.jun90.projects.scan.support.CorrectionScanTask;
import com.jun90.projects.scan.support.ImageScanner;
import com.jun90.projects.scan.support.MirrorScanTask;
import com.jun90.projects.scan.support.RelativePoint;
import com.jun90.projects.scan.support.RotatingScanTask;
import com.jun90.projects.scan.support.ZoomScanTask;

public class EditImageView extends AppCompatImageView implements ViewTreeObserver.OnGlobalLayoutListener, View.OnTouchListener {

    public static class Corner {

        private int mX, mY;

        public Corner() {

        }

        public Corner(int x, int y) {
            mX = x;
            mY = y;
        }

        public int getX() {
            return mX;
        }

        public void setX(int x) {
            mX = x;
        }

        public int getY() {
            return mY;
        }

        public void setY(int y) {
            mY = y;
        }

    }

    private Corner[] mCorners = new Corner[7];
    private Corner mSelectCorner;
    private boolean mIsLoaded = false;
    private Bitmap mWaitDrawImage, mThumbnail, mPreview;
    private double mRadian1 = 0, mRadian2 = 0;
    private boolean mMirrorX, mMirrorY;
    private CorrectionScanTask mCorrectionScanTask;

    private float mContrast, mBrightness;
    private int mRadius, mStrokeWidth;
    private int mWidth, mHeight;

    private int mAngle = 0;

    public EditImageView(Context context) {
        this(context, null);
    }

    public EditImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mRadius = (int) (8 * getResources().getDisplayMetrics().density);
        mStrokeWidth = (int) (1 * getResources().getDisplayMetrics().density);
        getViewTreeObserver().addOnGlobalLayoutListener(this);
        setOnTouchListener(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(mStrokeWidth);
        paint.setColor(getResources().getColor(R.color.colorAccent));
        for(int i = 0; i < 4; i++) {
            if(mCorners[i] == null) continue;
            canvas.drawCircle(mCorners[i].getX(), mCorners[i].getY(), mRadius, paint);
            canvas.drawLine(mCorners[i].getX(), mCorners[i].getY(), mCorners[(i + 1) % 4].getX(), mCorners[(i + 1) % 4].getY(), paint);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if(event.getPointerCount() == 1) {
                    mSelectCorner = selectCorner((int) event.getX(0), (int) event.getY(0));
                } else {
                    mSelectCorner = null;
                    mRadian1 = Math.atan2(Math.round(event.getY(1) - event.getY(0)),
                            Math.round(event.getX(1) - event.getX(0)));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(event.getPointerCount() == 1) {
                    if(mSelectCorner != null) {
                        moveCorner(mSelectCorner, (int) event.getX(0), (int) event.getY(0));
                        preview(false);
                    }
                    mSelectCorner = selectCorner((int) event.getX(0), (int) event.getY(0));
                } else {
                    mRadian2 = Math.atan2(Math.round(event.getY(1) - event.getY(0)),
                            Math.round(event.getX(1) - event.getX(0)));
                    int d = (int) Math.round(Math.toDegrees(mRadian2 - mRadian1));
                    if(d != 0) {
                        setAngle(mAngle + d);
                        mRadian1 = Math.atan2(Math.round(event.getY(1) - event.getY(0)),
                                Math.round(event.getX(1) - event.getX(0)));
                    }
                }
                break;
        }
        return true;
    }

    public Corner selectCorner(int x, int y) {
        for(int i = 0; i < 4; i++)
            if(x >= mCorners[i].getX() - mRadius * 2 && x < mCorners[i].getX() + mRadius * 2 && y >= mCorners[i].getY() - mRadius * 2 && y <= mCorners[i].getY() + mRadius * 2)
                return mCorners[i];
        return null;
    }

    private int getCornerX(int i) {
        return mCorners[i].getX() - mCorners[4].getX();
    }

    private int getCornerY(int i) {
        return mCorners[i].getY() - mCorners[4].getY();
    }

    private void moveCorner(Corner corner, int x, int y) {
        if(x < mCorners[4].getX()) x = mCorners[4].getX();
        else if(x > mCorners[5].getX()) x = mCorners[5].getX();
        if(y < mCorners[4].getY()) y = mCorners[4].getY();
        else if(y > mCorners[5].getY()) y = mCorners[5].getY();
        int index = -1;
        for(int i = 0; i < 4 && index < 0; i++)
            if(mCorners[i] == corner) index = i;
        if(index >= 0) {
            try {
                mCorners[index].setX(x);
                mCorners[index].setY(y);
                mCorrectionScanTask = new CorrectionScanTask(new RelativePoint(new AbsolutePoint(getCornerX(0), getCornerY(0)), mCorners[6].getX(), mCorners[6].getY()),
                        new RelativePoint(new AbsolutePoint(getCornerX(1), getCornerY(1)), mCorners[6].getX(), mCorners[6].getY()),
                        new RelativePoint(new AbsolutePoint(getCornerX(2), getCornerY(2)), mCorners[6].getX(), mCorners[6].getY()),
                        new RelativePoint(new AbsolutePoint(getCornerX(3), getCornerY(3)), mCorners[6].getX(), mCorners[6].getY()));
            } catch (IllegalArgumentException e) {
                resetCorners(mCorners[6].getX(), mCorners[6].getY());
            }
        }
    }

    public ImageScanner<Bitmap> getImageScanner() {
        return getImageScanner(false);
    }

    public ImageScanner<Bitmap> getImageScanner(boolean withCorrectionScanTask) {
        ImageScanner<Bitmap> scanner = new ImageScanner<Bitmap>(Bitmap.class);
        if(mContrast != 0 || mBrightness != 0)
            scanner.addTask(new AdjustmentScanTask(mContrast, mBrightness));
        if(mMirrorX || mMirrorY)
            scanner.addTask(new MirrorScanTask(mMirrorX, mMirrorY));
        if(mAngle != 0)
            scanner.addTask(new RotatingScanTask(mAngle));
        if(withCorrectionScanTask && mCorrectionScanTask != null)
            scanner.addTask(mCorrectionScanTask);
        return scanner;
    }

    public Bitmap runScanner(Bitmap bitmap) {
        return getImageScanner().run(bitmap);
    }

    public void setContrast(float contrast) {
        setContrast(contrast, true);
    }

    public void setContrast(float contrast, boolean preview) {
        mContrast = contrast;
        if(preview) preview(false);
    }

    public void setBrightness(float brightness) {
        setBrightness(brightness, true);
    }

    public void setBrightness(float brightness, boolean preview) {
        mBrightness = brightness;
        if(preview) preview(false);
    }

    public void setMirrorX(boolean mirrorX) {
        setMirrorX(mirrorX, true);
    }

    public void setMirrorX(boolean mirrorX, boolean preview) {
        mMirrorX = mirrorX;
        if(preview) preview(false);
    }

    public void setMirrorY(boolean mirrorY) {
        setMirrorY(mirrorY, true);
    }

    public void setMirrorY(boolean mirrorY, boolean preview) {
        mMirrorY = mirrorY;
        if(preview) preview(false);
    }

    public void setAngle(int angle) {
        setAngle(angle, true);
    }

    public void setAngle(int angle, boolean preview) {
        mAngle = angle % 360;
        if(preview) preview(true);
    }

    public static Bitmap makeThumbnail(Bitmap image, int maxWidth, int maxHeight) {
        double scaleWidth = maxWidth * 1.0 / image.getWidth(), scaleHeight = maxHeight * 1.0 / image.getHeight();
        double scale = scaleWidth < scaleHeight ? scaleWidth : scaleHeight;
        Bitmap thumbnail;
        if(scale == 1) {
            thumbnail = image;
        } else {
            ImageScanner<Bitmap> scanner = new ImageScanner<Bitmap>(Bitmap.class);
            scanner.addTask(new ZoomScanTask(scale, scale));
            thumbnail = scanner.run(image);
        }
        return thumbnail;
    }

    public void drawImageBitmap() {
        if(!mIsLoaded || mWaitDrawImage == null) return;
        mThumbnail = makeThumbnail(mWaitDrawImage, mWidth, mHeight);
        resetCorners(mThumbnail.getWidth(), mThumbnail.getHeight());
        super.setImageBitmap(mThumbnail);
        mWaitDrawImage = null;
    }

    public void preview() {
        preview(true);
    }

    public void preview(boolean resetCorners) {
        if(mThumbnail != null) {
            mPreview = makeThumbnail(runScanner(mThumbnail), mWidth, mHeight);
            if(resetCorners) resetCorners(mPreview.getWidth(), mPreview.getHeight());
            super.setImageBitmap(mPreview);
        }
    }

    private void resetCorners(int width, int height) {
        mCorners[4] = new Corner((getWidth() - width) / 2, (getHeight() - height) / 2);
        mCorners[5] = new Corner(mCorners[4].getX() + width, mCorners[4].getY() + height);
        mCorners[6] = new Corner(width, height);
        mCorners[0] = new Corner(mCorners[4].getX() ,mCorners[4].getY());
        mCorners[1] = new Corner(mCorners[5].getX(), mCorners[4].getY());
        mCorners[2] = new Corner(mCorners[5].getX(), mCorners[5].getY());
        mCorners[3] = new Corner(mCorners[4].getX(), mCorners[5].getY());
        mSelectCorner = null;
        mCorrectionScanTask = null;
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        mWaitDrawImage = bm;
        drawImageBitmap();
    }

    @Override
    public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        mIsLoaded = true;
        mWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        mHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        drawImageBitmap();
    }

}
