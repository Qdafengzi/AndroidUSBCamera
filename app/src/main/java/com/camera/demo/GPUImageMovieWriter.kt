package com.camera.demo

import android.opengl.EGL14
import com.camera.demo.encoder.CameraXListener
import com.camera.demo.encoder.EglCore
import com.camera.demo.encoder.MediaEncoder
import com.camera.demo.encoder.MediaMuxerWrapper
import com.camera.demo.encoder.MediaVideoEncoder
import com.camera.demo.encoder.RecordListener
import com.camera.demo.encoder.WindowSurface
import com.gemlightbox.core.utils.XLogger
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

class GPUImageMovieWriter(private val cameraXListener: CameraXListener) : GPUImageFilter() {
    private var mMuxer: MediaMuxerWrapper? = null
    private var mVideoEncoder: MediaVideoEncoder? = null
    private var mCodecInput: WindowSurface? = null

    private var mEGLScreenSurface: EGLSurface? = null
    private var mEGL: EGL10? = null
    private var mEGLDisplay: EGLDisplay? = null
    private var mEGLContext: EGLContext? = null
    private var mEGLCore: EglCore? = null

    private var mIsRecording = false

    private var frameRate = 30

    var drawVideo: Boolean = false

    fun setFrameRate(frameRate: Int) {
        this.frameRate = frameRate
    }

    override fun onInit() {
        super.onInit()
        mEGL = (EGLContext.getEGL() as EGL10)
        mEGLDisplay = mEGL?.eglGetCurrentDisplay()
        mEGLContext = mEGL?.eglGetCurrentContext()
        mEGLScreenSurface = mEGL?.eglGetCurrentSurface(EGL10.EGL_DRAW)
    }
    var scaleFactor = 1.0f

    fun setCaleFactor(scaleFactor:Float){
        this.scaleFactor = scaleFactor
    }

    @Synchronized
    override fun onDraw(textureId: Int, cubeBuffer: FloatBuffer, textureBuffer: FloatBuffer) {
        // Draw on screen surface
        super.onDraw(textureId, cubeBuffer, textureBuffer)

        if (mIsRecording && drawVideo) {
            drawVideo = false
            // create encoder surface
            if (mCodecInput == null) {
                if (mVideoEncoder == null || mVideoEncoder!!.surface == null) {
                    XLogger.e("mVideoEncoder == null")
                    return
                }
                mEGLCore = EglCore(EGL14.eglGetCurrentContext(), EglCore.FLAG_RECORDABLE)
                mCodecInput = WindowSurface(mEGLCore, mVideoEncoder!!.surface, false)
            }

            // Draw on encoder surface
            mCodecInput?.makeCurrent()
            // 你可能需要修改这里来应用缩放
//            val scaledCubeBuffer = scaleBuffer(cubeBuffer, scaleFactor)
//            val scaledTextureBuffer = scaleBuffer(textureBuffer, scaleFactor)
            XLogger.d("GPUWriter OnDraw")
//            super.onDraw(textureId, scaledCubeBuffer, scaledTextureBuffer)
            super.onDraw(textureId, cubeBuffer, textureBuffer)
            mCodecInput?.swapBuffers()
            mVideoEncoder?.frameAvailableSoon()
        }

        // Make screen surface be current surface
        mEGL?.eglMakeCurrent(mEGLDisplay, mEGLScreenSurface, mEGLScreenSurface, mEGLContext)
        val error = EGL14.eglGetError()
        if (error != EGL14.EGL_SUCCESS) {
            XLogger.e("EGL : eglError $error")
        }
    }

    /**
     * 画面的缩放
     */
    private fun scaleBuffer(buffer: FloatBuffer, scaleFactor: Float): FloatBuffer {
        // 重置buffer的position，以确保从头开始读取
        buffer.position(0)

        // 创建一个临时数组用于存储缩放后的坐标
        val scaledArray = FloatArray(buffer.capacity())

        // 遍历buffer中的每个元素，应用缩放因子
        for (i in scaledArray.indices) {
            scaledArray[i] = buffer.get() * scaleFactor
        }

        // 将缩放后的坐标数组重新写回buffer
        buffer.clear() // 清除buffer，重置position和limit
        buffer.put(scaledArray)
        buffer.position(0) // 重置position，使得buffer准备好被读取

        return buffer
    }


    override fun onDestroy() {
        super.onDestroy()
//        releaseEncodeSurface()
    }

    @Synchronized
    fun prepareRecording(
        outputPath: String?,
        width: Int,
        height: Int,
        prepareCallback: (success: Boolean) -> Unit
    ) {
        runOnDraw {
            if (mIsRecording) {
                XLogger.d("video bug GPU write already in recording")
                return@runOnDraw
            }
            try {
                mMuxer = MediaMuxerWrapper(outputPath)
                // for video capturing
                mVideoEncoder = MediaVideoEncoder(mMuxer, mMediaEncoderListener, width, height, frameRate)
                mMuxer?.prepare()
                prepareCallback(true)
                XLogger.d("video start record")
            } catch (e: Exception) {
                prepareCallback(false)
                XLogger.e("record video bug error" + e.message)
                cameraXListener.recordPrepareError(e.message ?: "")
            }
        }
    }

    @Synchronized
    fun stopRecording(recordListener: RecordListener?) {
        runOnDraw {
            if (!mIsRecording) {
                XLogger.e("record have stopped return ")
                return@runOnDraw
            }
            try {
                mMuxer?.stopRecording(recordListener)
            } catch (e: Exception) {
                XLogger.e("record error:" + e.message)
                cameraXListener.stopError(e.message ?: "")
            } finally {
                mIsRecording = false
                XLogger.d("record release encodeSurface1")
                releaseEncodeSurface()
            }
        }
    }

    private fun releaseEncodeSurface() {
        XLogger.d("record releaseEncodeSurface")
        if (mEGLCore != null) {
            mEGLCore!!.makeNothingCurrent()
            mEGLCore!!.release()
            mEGLCore = null
        }

        if (mCodecInput != null) {
            mCodecInput!!.release()
            mCodecInput = null
        }
        if (mVideoEncoder != null) {
            mVideoEncoder = null
        }
        if (mMuxer != null) {
            mMuxer = null
        }
    }

    /**
     * callback methods from encoder
     */
    private val mMediaEncoderListener: MediaEncoder.MediaEncoderListener = object : MediaEncoder.MediaEncoderListener {
        override fun onPrepared(encoder: MediaEncoder) {
            XLogger.d("onPrepared...")
        }

        override fun onStopped(encoder: MediaEncoder) {
            XLogger.d("onStopped...")
        }

        override fun onMuxerStopped() {
            XLogger.d("onMuxerStopped...")
        }
    }

    @Synchronized
    fun startRecording() {
        runOnDraw {
            try {
                if (mVideoEncoder != null) {
//                    mMuxer?.prepare()
                    mMuxer?.startRecording()
                    mIsRecording = true
                    cameraXListener.recordStart()
                }
            } catch (e: Exception) {
                XLogger.e("video bug location>>>" + e.message)
                cameraXListener.recordError(e.message ?: "")
                mIsRecording = false
            }
        }
    }
}
