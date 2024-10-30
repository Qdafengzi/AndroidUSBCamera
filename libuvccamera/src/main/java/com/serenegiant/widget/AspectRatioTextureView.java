/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;

import com.herohan.uvcapp.BuildConfig;


/**
 * change the view size with keeping the specified aspect ratio.
 * if you set this view with in a FrameLayout and set property "android:layout_gravity="center",
 * you can show this view in the center of screen and keep the aspect ratio of content
 * XXX it is better that can set the aspect ratio as xml property
 *
 * @author admin
 */
public class AspectRatioTextureView extends TextureView    // API >= 14
        implements IAspectRatioView {

    private static final boolean DEBUG = BuildConfig.DEBUG;    // TODO set false on release
    private static final String TAG = AspectRatioTextureView.class.getSimpleName();

    private double mRequestedAspect = -1.0;
    private CameraViewInterface.Callback mCallback;

    public AspectRatioTextureView(final Context context) {
        this(context, null, 0);
    }

    public AspectRatioTextureView(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AspectRatioTextureView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAspectRatio(final double aspectRatio) {
        if (aspectRatio < 0) {
            throw new IllegalArgumentException();
        }
        // Use a range as a standard for comparing whether floating point numbers are equal
        float diff = 1e-6f;
        if (Math.abs(mRequestedAspect - aspectRatio) > diff) {
            mRequestedAspect = aspectRatio;
            new Handler(Looper.getMainLooper()).post(() -> {
                requestLayout();
            });
        }
    }

    /**
     * 只有改变分辨率的时候才能调用
     * @param width 分辨率宽度
     * @param height 分辨率高度
     */
    @Override
    public void setAspectRatio(final int width, final int height) {
        setAspectRatio(width / (double) height);
    }

    @Override
    public double getAspectRatio() {
        return mRequestedAspect;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if (mRequestedAspect > 0) {
            int initialWidth = MeasureSpec.getSize(widthMeasureSpec);
            int initialHeight = MeasureSpec.getSize(heightMeasureSpec);

            final int horizPadding = getPaddingLeft() + getPaddingRight();
            final int vertPadding = getPaddingTop() + getPaddingBottom();
            initialWidth -= horizPadding;
            initialHeight -= vertPadding;

            final double viewAspectRatio = (double) initialWidth / initialHeight;
            final double aspectDiff = mRequestedAspect / viewAspectRatio - 1;

            if (Math.abs(aspectDiff) > 0.01) {
                if (aspectDiff > 0) {
                    // width priority decision
                    initialHeight = (int) (initialWidth / mRequestedAspect);
                } else {
                    // height priority decision
                    initialWidth = (int) (initialHeight * mRequestedAspect);
                }
                initialWidth += horizPadding;
                initialHeight += vertPadding;
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY);
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY);
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

//
//    //    public ScaleGestureDetector mScaleGestureDetector;
//    private int mSurfaceTextureWidth;
//    private int mSurfaceTextureHeight;
//    private float mSavedScaleFactor = 1.0f;
//    private int mMaxZoom;
//    public static final float MAX_ZOOM_GESTURE_SIZE = 2.5f; // This affects the pinch to zoom gesture
//
//
//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        return mScaleGestureDetector.onTouchEvent(event);
//    }
//
//    ScaleGestureDetector mScaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
//
//        @Override
//        public boolean onScaleBegin(ScaleGestureDetector detector) {
//            return true;
//        }
//
//        @Override
//        public boolean onScale(ScaleGestureDetector detector) {
//            setCameraZoom(detector.getScaleFactor() * mSavedScaleFactor);
//            return false;
//        }
//
//        @Override
//        public void onScaleEnd(ScaleGestureDetector detector) {
//            // Set saved scale factor and make sure it's within legal range
//            mSavedScaleFactor = mSavedScaleFactor * detector.getScaleFactor();
//            if (mSavedScaleFactor < 1.0f) {
//                mSavedScaleFactor = 1.0f;
//            } else if (mSavedScaleFactor > MAX_ZOOM_GESTURE_SIZE + 1) {
//                mSavedScaleFactor = MAX_ZOOM_GESTURE_SIZE + 1;
//            }
//
//            setCameraZoom(mSavedScaleFactor);
//        }
//    });
//
//    private void setCameraZoom(float zoomScaleFactor) {
//        // Convert gesture to camera zoom value
//        int zoom = (int) ((zoomScaleFactor - 1) * mMaxZoom / MAX_ZOOM_GESTURE_SIZE);
//        // Sanity check for zoom level
//        if (zoom > mMaxZoom) {
//            zoom = mMaxZoom;
//        } else if (zoom < 0) {
//            zoom = 0;
//        }
//        int zoomVal = (int) zoomScaleFactor;
//        mZoomCallback.onZoomView(zoomVal);
//    }
//
//    private IZoomCallback mZoomCallback;
//
//    public void setZoomCallback(IZoomCallback callback){
//        mZoomCallback = callback;
//    }
//
//    public interface IZoomCallback{
//        void onZoomView(int zoom);
//    }

}
