package com.camera.demo.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaVideoEncoder.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
 */

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.opengl.EGLContext;
import android.util.Log;
import android.view.Surface;


import com.camera.utils.XLogger;

import java.io.IOException;

public class MediaVideoEncoder extends MediaEncoder {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaVideoEncoder";

//    private static final String MIME_TYPE = "video/avc";

    // parameters for recording
    private int frameRate  = 30;
    private static final float BPP = 0.15f;
    private static final float BPP_HIGH = 0.4f;
    //private static final float BPP_LOW = 0.15f;

    private final int mWidth;
    private final int mHeight;
    // private RenderHandler mRenderHandler;
    private Surface mSurface;

    public MediaVideoEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener,
                             final int width, final int height, final int frameRate) {
        super(muxer, listener);
        if (DEBUG) Log.i(TAG, "MediaVideoEncoder: ");
        if (width % 2 != 0) {
            mWidth = width - 1;
        } else {
            mWidth = width;
        }
        if (height %2 != 0) {
            mHeight = height - 1;
        } else {
            mHeight = height;
        }
        this.frameRate = frameRate;
//        mWidth = width;
//        mHeight = height;
        // mRenderHandler = RenderHandler.createHandler(TAG);
    }

    public boolean frameAvailableSoon(final float[] tex_matrix) {
        boolean result;
        if (result = super.frameAvailableSoon()) {
            // mRenderHandler.draw(tex_matrix);
        }
        return result;
    }

    @Override
    public boolean frameAvailableSoon() {
        boolean result;
        if (result = super.frameAvailableSoon()) {
            // mRenderHandler.draw(null);
        }
        return result;
    }

    @Override
    protected void prepare() throws IOException {
        if (mWidth == 0 || mHeight == 0) {
            XLogger.d("width || height ==0");
            return;
        }
        XLogger.d("prepare:");
        mTrackIndex = -1;
        mMuxerStarted = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(MediaFormat.MIMETYPE_VIDEO_AVC);
        if (videoCodecInfo == null) {
            XLogger.e("Unable to find an appropriate codec for " + MediaFormat.MIMETYPE_VIDEO_AVC);

            return;
        }
        XLogger.d("selected codec:"+  videoCodecInfo.getName());

        XLogger.d("MediaCodec " + mWidth + " " + mHeight);
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, calcBitRate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        XLogger.d("format:"+ format);

        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();    // API >= 18
        mMediaCodec.start();

        XLogger.d("prepare finishing start...");
        if (mListener != null) {
            try {
                mListener.onPrepared(this);
            } catch (Exception e) {
                XLogger.e("prepare error:"+e.getMessage());
            }
        }
    }

    public void setEglContext(final EGLContext shared_context,
                              final int tex_id) {
        // mRenderHandler.setEglContext(shared_context, tex_id, mSurface, true);
    }

    public Surface getSurface() {
        return mSurface;
    }

    @Override
    protected void release() {
        if (DEBUG) Log.i(TAG, "release:");
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        // if (mRenderHandler != null) {
        //     mRenderHandler.release();
        //     mRenderHandler = null;
        // }
        super.release();
    }

    private int calcBitRate() {
        /*float bpp = BPP;
        if (!"US".equalsIgnoreCase(LocateManager.getInstance().getCountry())) {
            bpp = BPP_LOW;
        }*/

        return (int) (BPP * frameRate * mWidth * mHeight);
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return null if no codec matched
     */
    protected MediaCodecInfo selectVideoCodec(final String mimeType) {
        if (DEBUG) Log.v(TAG, "selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    if (DEBUG) Log.i(TAG, "codec:" + codecInfo.getName() + ",MIME=" + type);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        if (DEBUG) Log.i(TAG, "selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//            MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,

        };
    }

    private boolean isRecognizedViewoFormat(int colorFormat) {
        if (DEBUG) Log.i(TAG, "isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void signalEndOfInputStream() {
        if (DEBUG) Log.d(TAG, "sending EOS to encoder");
        if (mMediaCodec != null) {
            try {
                mMediaCodec.signalEndOfInputStream();    // API >= 18
            } catch (IllegalStateException e) {

            }
        }
        mIsEOS = true;
    }

}
