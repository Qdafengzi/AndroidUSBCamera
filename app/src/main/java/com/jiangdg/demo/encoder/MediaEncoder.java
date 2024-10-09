package com.jiangdg.demo.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaEncoder.java
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

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;


import com.jiangdg.utils.XLogger;

import java.io.IOException;
import java.nio.ByteBuffer;

@TargetApi(18)
public abstract class MediaEncoder implements Runnable {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaEncoder";

    protected static final int TIMEOUT_USEC = 10000;    // 10[msec]
    protected static final int MSG_FRAME_AVAILABLE = 1;
    protected static final int MSG_STOP_RECORDING = 9;

    public interface MediaEncoderListener {
        void onPrepared(MediaEncoder encoder);
        void onStopped(MediaEncoder encoder);
        void onMuxerStopped();
    }

    protected final Object mSync = new Object();
    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean mIsCapturing;
    /**
     * Flag that indicate the frame data will be available soon.
     */
    private int mRequestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected boolean mIsEOS;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean mMuxerStarted;
    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
    protected MediaMuxerWrapper mMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)

    protected final MediaEncoderListener mListener;

    boolean mInputError = false;

    public MediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener) {
        if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
        if (muxer == null) throw new NullPointerException("MediaMuxerWrapper is null");
        mMuxer = muxer;
        muxer.addEncoder(this);
        mListener = listener;
        synchronized (mSync) {
            // create BufferInfo here for effectiveness(to reduce GC)
            mBufferInfo = new MediaCodec.BufferInfo();
            // wait for starting thread
            new Thread(this, getClass().getSimpleName()).start();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                com.jiangdg.utils.XLogger.d("error:"+e.getMessage());
            }
        }
    }

    public String getOutputPath() {
        final MediaMuxerWrapper muxer = mMuxer;
        return muxer != null ? muxer.getOutputPath() : null;
    }

    /**
     * the method to indicate frame data is soon available or already available
     * @return return true if encoder is ready to encod.
     */
    public boolean frameAvailableSoon() {
//        if (DEBUG) Log.v(TAG, "frameAvailableSoon");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                if (recordListener != null) {
                    recordListener.onStop();
                }
                return false;
            }
            mRequestDrain++;
            mSync.notifyAll();
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
//        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        synchronized (mSync) {
            mRequestStop = false;
            mRequestDrain = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            synchronized (mSync) {
                localRequestStop = mRequestStop;
                localRequestDrain = (mRequestDrain > 0);
                if (localRequestDrain)
                    mRequestDrain--;
            }

            if (mInputError) {
                inputError();
                release();
                break;
            }

            if (localRequestStop) {
                drain();
                // request stop recording
                signalEndOfInputStream();
                // process output data again for EOS signale
                drain();
                // release all related objects
                release();
                break;
            }

            if (localRequestDrain) {
                drain();
            } else {
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        XLogger.d("Encoder thread exiting");
        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

    /*
    * prepareing method for each sub class
    * this method should be implemented in sub class, so set this as abstract method
    * @throws IOException
    */
   /*package*/ abstract void prepare() throws IOException;

    /*package*/ void startRecording() {
       XLogger.d("start Recording");
        synchronized (mSync) {
            mIsCapturing = true;
            mRequestStop = false;
            mSync.notifyAll();
        }
    }

    public RecordListener recordListener;
    /**
     * the method to request stop encoding
     */
    public void stopRecording(RecordListener recordListener) {
        XLogger.d("录制 stop Recording");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            this.recordListener = recordListener;
            mRequestStop = true;    // for rejecting newer frame
            mSync.notifyAll();
            XLogger.d("录制 stop---");
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

//********************************************************************************
//********************************************************************************
    /**
     * Release all releated objects
     */
    protected void release() {
        XLogger.d("release:");
        try {
            mListener.onStopped(this);
        } catch (final Exception e) {
            XLogger.d("failed onStopped"+e.getMessage());
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {

                XLogger.d("failed releasing MediaCodec"+ e.getMessage());
            }
        }
        if (mMuxerStarted) {
            final MediaMuxerWrapper muxer = mMuxer;
            if (muxer != null) {
                try {
                    if (muxer.stop()) {
                        mListener.onMuxerStopped();
                    }
                } catch (final Exception e) {

                    XLogger.d("failed stopping muxer"+ e.getMessage());
                }
            }
        }
        mBufferInfo = null;
        mMuxer = null;
        if (recordListener != null) {
            recordListener.onStop();
        }
    }

    protected void signalEndOfInputStream() {
        XLogger.d( "sending EOS to encoder");
        // signalEndOfInputStream is only avairable for video encoding with surface
        // and equivalent sending a empty buffer with BUFFER_FLAG_END_OF_STREAM flag.
//        mMediaCodec.signalEndOfInputStream();    // API >= 18
        encode(null, 0, getPTSUs());
    }

    /**
     * Method to set byte array to the MediaCodec encoder
     * @param buffer
     * @param length　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing) return;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
//                if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                   XLogger.d("send BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    protected void drain() {
        XLogger.d("drain--------->");
        if (mMediaCodec == null) return;

        final MediaMuxerWrapper muxer = mMuxer;
        if (muxer == null) {
            XLogger.e("muxer is unexpectedly null");
            return;
        }

        int encoderStatus, count = 0;
        LOOP: while (mIsCapturing) {
            try {
                encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            } catch (IllegalStateException e) {
                XLogger.e("dequeueOutputBuffer error"+ e.getMessage());
                encoderStatus = MediaCodec.INFO_TRY_AGAIN_LATER;
            }

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!mIsEOS && ++count > 5) {
                    break LOOP; // Break if too many retries
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                XLogger.d("INFO_OUTPUT_FORMAT_CHANGED");
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }

                final MediaFormat format = mMediaCodec.getOutputFormat();
                mTrackIndex = muxer.addTrack(format);
                XLogger.d("录制配置" + "Muxer Format: " + format.toString() + " " + mTrackIndex);
                mMuxerStarted = true;

                synchronized (muxer) {
                    while (!muxer.isStarted()) {
                        try {
                            muxer.wait(100);
                        } catch (InterruptedException e) {
                            break LOOP;
                        }
                    }
                }
            } else if (encoderStatus < 0) {
                XLogger.d("unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                ByteBuffer encodedData = mMediaCodec.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    XLogger.d("drain:BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    count = 0;
                    if (!mMuxerStarted) {
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }

                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    mIsCapturing = false;
                    break; // Break on EOS
                }
            }
        }
    }


    protected void drain1() {
        XLogger.d("drain--------->");
        if (mMediaCodec == null) return;





        ByteBuffer[] encoderOutputBuffers = null;
        try {
            encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        } catch (IllegalStateException e) {
            XLogger.e(" mMediaCodec.getOutputBuffers() error:");
            e.printStackTrace();
            return;
        }

        int encoderStatus, count = 0;
        final MediaMuxerWrapper muxer = mMuxer;
        if (muxer == null) {
//            throw new NullPointerException("muxer is unexpectedly null");
            XLogger.e("muxer is unexpectedly null");
            return;
        }
        LOOP:    while (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            try {
                encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            } catch (IllegalStateException e) {
                encoderStatus = MediaCodec.INFO_TRY_AGAIN_LATER;
            }
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                    if (++count > 5)
                        break LOOP;        // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                XLogger.d("INFO_OUTPUT_BUFFERS_CHANGED");
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                XLogger.d("INFO_OUTPUT_FORMAT_CHANGED");
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mMuxerStarted) {    // second time request is error
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16

                mTrackIndex = muxer.addTrack(format);
                XLogger.d("录制配置"+ "Muxer Format: " + format.toString() +"  "+mTrackIndex);
                mMuxerStarted = true;
                if (!muxer.start()) {
                    // we should wait until muxer is ready
                    synchronized (muxer) {
                        while (!muxer.isStarted())
                            try {
                                muxer.wait(100);
                            } catch (final InterruptedException e) {
                                break LOOP;
                            }
                    }
                }
            } else if (encoderStatus < 0) {
                // unexpected status
                XLogger.d("drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    XLogger.d("drain:BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if (!mMuxerStarted) {
                        // muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }

    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;
    /**
     * get next encoding presentationTimeUs
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }

    protected void inputError() {
        final MediaMuxerWrapper muxer = mMuxer;
        if (muxer != null) {
            muxer.removeFailEncoder();
        }
    }

}
