package com.camera.demo;


import android.opengl.EGL14;

import com.camera.demo.encoder.EglCore;
import com.camera.demo.encoder.MediaEncoder;
import com.camera.demo.encoder.MediaMuxerWrapper;
import com.camera.demo.encoder.MediaVideoEncoder;
import com.camera.demo.encoder.RecordListener;
import com.camera.demo.encoder.WindowSurface;
import com.camera.utils.XLogger;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;

public class GPUImageMovieWriter extends GPUImageFilter {
    private MediaMuxerWrapper mMuxer;
    private MediaVideoEncoder mVideoEncoder;
    private WindowSurface mCodecInput;

    private EGLSurface mEGLScreenSurface;
    private EGL10 mEGL;
    private EGLDisplay mEGLDisplay;
    private EGLContext mEGLContext;
    private EglCore mEGLCore;

    private boolean mIsRecording = false;

    private int frameRate = 30;
    public GPUImageErrorListener gpuImageErrorListener;

    public boolean drawVideo = false;

    public interface StartRecordListener {
        void onRecordStart();

        void onRecordError(Exception e);
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    @Override
    public void onInit() {
        super.onInit();
        mEGL = (EGL10) EGLContext.getEGL();
        mEGLDisplay = mEGL.eglGetCurrentDisplay();
        mEGLContext = mEGL.eglGetCurrentContext();
        mEGLScreenSurface = mEGL.eglGetCurrentSurface(EGL10.EGL_DRAW);
    }

    @Override
    public synchronized void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        // Draw on screen surface
        super.onDraw(textureId, cubeBuffer, textureBuffer);

        if (mIsRecording && drawVideo) {
            drawVideo = false;
            // create encoder surface
            if (mCodecInput == null) {
                if (mVideoEncoder == null || mVideoEncoder.getSurface() == null) {
                    return;
                }
                mEGLCore = new EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE);
                mCodecInput = new WindowSurface(mEGLCore, mVideoEncoder.getSurface(), false);
            }

            // Draw on encoder surface
            mCodecInput.makeCurrent();
            super.onDraw(textureId, cubeBuffer, textureBuffer);
            if (mCodecInput != null) {
                mCodecInput.swapBuffers();
            }
            mVideoEncoder.frameAvailableSoon();
        }

        // Make screen surface be current surface
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLScreenSurface, mEGLScreenSurface, mEGLContext);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseEncodeSurface();
    }

    public synchronized void prepareRecording(final String outputPath, final int width, final int height) {
        XLogger.d("录制-------prepareRecording ---"+width +"*"+height + "path:"+ outputPath);
        runOnDraw(() -> {
            if (mIsRecording) {
                XLogger.d("video bug GePU write already in recording");
                XLogger.e("录制-------正在进行");
                return;
            }

            try {
                mMuxer = new MediaMuxerWrapper(outputPath);
                // for video capturing
                mVideoEncoder = new MediaVideoEncoder(mMuxer, mMediaEncoderListener, width, height, frameRate);
                mMuxer.prepare();
                XLogger.d("录制-------->prepareRecording");
            } catch (Exception e) {
                XLogger.e("录制------>video bug error"+e.getMessage());
                if (gpuImageErrorListener != null) {
                    gpuImageErrorListener.onError();
                }
            }
        });
    }

    public synchronized void stopRecording(final RecordListener recordListener) {
        runOnDraw(() -> {
            if (!mIsRecording) {
                return;
            }

            try {
                mMuxer.stopRecording(recordListener);
                mIsRecording = false;
                releaseEncodeSurface();
                XLogger.d("暂停了");
            } catch (Exception e) {
                XLogger.d("error:"+e.getMessage());
            }
        });
    }

    private void releaseEncodeSurface() {
        if (mEGLCore != null) {
            mEGLCore.makeNothingCurrent();
            mEGLCore.release();
            mEGLCore = null;
        }

        if (mCodecInput != null) {
            mCodecInput.release();
            mCodecInput = null;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder = null;
        }
        if (mMuxer != null) {
            mMuxer = null;
        }
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            XLogger.d("onPrepared..."+encoder.getOutputPath());
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            XLogger.d("onStopped..."+encoder.getOutputPath());
        }

        @Override
        public void onMuxerStopped() {
            XLogger.d("onMuxerStopped...");
        }
    };

    public synchronized void startRecording(StartRecordListener startRecordListener) {
        runOnDraw(() -> {
            try {
                if (mVideoEncoder != null) {
                    mMuxer.prepare();
                    mMuxer.startRecording();

                    mIsRecording = true;

                    if (startRecordListener != null) {
                        startRecordListener.onRecordStart();
                    }
                }
            } catch (Exception e) {
                XLogger.e("video bug location>>>"+e.getMessage());
                if (startRecordListener != null) {
                    startRecordListener.onRecordError(e);
                }
            }
        });
    }

    public interface GPUImageErrorListener {
        void onError();
    }
}