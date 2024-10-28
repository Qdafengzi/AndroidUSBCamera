package com.camera.demo.encoder;
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

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;

import com.gemlightbox.core.utils.XLogger;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class MediaEncoder implements Runnable {

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
                    } catch (final Exception e) {
                        XLogger.d("error:"+e.getMessage());
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

    private RecordListener recordListener;
    /**
     * the method to request stop encoding
     */
    /*package*/ void stopRecording(RecordListener recordListener) {
        XLogger.d("stop Recording");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            this.recordListener = recordListener;
            mRequestStop = true;    // for rejecting newer frame
            mSync.notifyAll();
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
        } catch (Exception e) {
          XLogger.d("failed onStopped"+ e.getMessage());
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (Exception e) {
                XLogger.e("failed releasing MediaCodec"+ e.getMessage());
            }
        }
        if (mMuxerStarted) {
            final MediaMuxerWrapper muxer = mMuxer;
            if (muxer != null) {
                try {
                    if (muxer.stop()) {
                        mListener.onMuxerStopped();
                    }
                } catch (Exception e) {

                    XLogger.e("failed stopping muxer"+e.getMessage());
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
                    XLogger.d("------>put(buffer)");
                    inputBuffer.put(buffer);
                }
//                if (DEBUG) Log.v(TAG, "encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                  XLogger.d( "send BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    XLogger.d( " length 》=0");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                XLogger.d("------>INFO_TRY_AGAIN_LATER");
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    @SuppressLint("SuspiciousIndentation")
    protected void drain() {
        if (mMediaCodec == null) return;
        ByteBuffer[] encoderOutputBuffers = null;
        try {
            encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        } catch (IllegalStateException e) {

            XLogger.e(" mMediaCodec.getOutputBuffers() error"+e.getMessage());
            return;
        }

        int outputBufferIndex, count = 0;
        final MediaMuxerWrapper muxer = mMuxer;
        if (muxer == null) {
//            throw new NullPointerException("muxer is unexpectedly null");
            XLogger.e( "muxer is unexpectedly null");
            return;
        }
        LOOP: while (mIsCapturing) {
            try {
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            } catch (IllegalStateException e) {
                outputBufferIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
            }
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                    if (++count > 5)
                        break LOOP;        // out of while
                }
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                XLogger.v( "INFO_OUTPUT_BUFFERS_CHANGED");
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                XLogger.v( "INFO_OUTPUT_FORMAT_CHANGED");
                if (mMuxerStarted) {    // second time request is error
                    throw new RuntimeException("format changed twice");
                }
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                mTrackIndex = muxer.addTrack(format);
                mMuxerStarted = true;
                if (!muxer.start()) {
                    // we should wait until muxer is ready
                    synchronized (muxer) {
                        while (!muxer.isStarted())
                            try {
                                muxer.wait(100);
                            } catch (final InterruptedException e) {
                                XLogger.e("error----InterruptedException？"+e.getMessage());
                                break LOOP;
                            }
                    }
                }
            } else if (outputBufferIndex < 0) {
                // unexpected status
                XLogger.d( "drain:unexpected result from encoder#dequeueOutputBuffer: " + outputBufferIndex);
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[outputBufferIndex];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex + " was null");
                }
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
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
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }


    protected void drain1() {
        if (mMediaCodec == null || mMuxer == null) {
            XLogger.e("MediaCodec or Muxer is null");
            return;
        }

        final long TIMEOUT_USEC = 10000L; // 10ms
        int mWaitCount = 0;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

        while (mIsCapturing) {
            int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

            switch (outputBufferIndex) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    if (!mIsEOS && ++mWaitCount > 5) {
                        XLogger.d("No output available yet, breaking loop");
                        return;
                    }
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    handleOutputFormatChanged();
                    break;

                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    XLogger.v("INFO_OUTPUT_BUFFERS_CHANGED");
                    break;

                default:
                    if (outputBufferIndex >= 0) {
                        handleEncodedData(outputBufferIndex, bufferInfo);
                    } else {
                        XLogger.d("Unexpected result from dequeueOutputBuffer: " + outputBufferIndex);
                    }
                    break;
            }

            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                XLogger.d("End of stream reached");
                mIsCapturing = false;
            }
        }
    }

    private void handleOutputFormatChanged() {
        if (mMuxerStarted) {
            throw new RuntimeException("Format changed twice");
        }
        MediaFormat newFormat = mMediaCodec.getOutputFormat();
        mTrackIndex = mMuxer.addTrack(newFormat);
        mMuxerStarted = true;
        if (!mMuxer.start()) {
            waitForMuxerStart();
        }
        XLogger.v("Output format changed, muxer started");
    }

    private void waitForMuxerStart() {
        synchronized (mMuxer) {
            while (!mMuxer.isStarted()) {
                try {
                    mMuxer.wait(100);
                } catch (InterruptedException e) {
                    XLogger.e("Interrupted while waiting for muxer to start: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void handleEncodedData(int bufferIndex, MediaCodec.BufferInfo bufferInfo) {
        ByteBuffer encodedData = mMediaCodec.getOutputBuffer(bufferIndex);
        if (encodedData == null) {
            throw new RuntimeException("EncoderOutputBuffer " + bufferIndex + " was null");
        }

        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            XLogger.d("Ignoring BUFFER_FLAG_CODEC_CONFIG");
            bufferInfo.size = 0;
        }

        if (bufferInfo.size != 0 && mMuxerStarted) {
            encodedData.position(bufferInfo.offset);
            encodedData.limit(bufferInfo.offset + bufferInfo.size);
            bufferInfo.presentationTimeUs = getPTSUs();
            mMuxer.writeSampleData(mTrackIndex, encodedData, bufferInfo);
            prevOutputPTSUs = bufferInfo.presentationTimeUs;
        }

        mMediaCodec.releaseOutputBuffer(bufferIndex, false);
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
