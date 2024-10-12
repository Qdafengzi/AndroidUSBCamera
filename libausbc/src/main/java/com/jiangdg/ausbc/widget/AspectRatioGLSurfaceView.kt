/*
 * Copyright 2017-2023 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.ausbc.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Surface
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** 纵横比自适应GLSurfaceView
 *
 * @author Created by jiangdg on 2021/12/23
 */
class AspectRatioGLSurfaceView : GLSurfaceView, GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener, IAspectRatio {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    private var mAspectRatio = -1.0
    private var mSurfaceTexture: SurfaceTexture? = null
    private var mVertexBuffer: FloatBuffer? = null
    private var mListener: OnSurfaceLifecycleListener? = null
    private var mVertexShader = 0
    private var mFragmentShader = 0
    private var mProgram = 0
    private var mESOTextureId = 0
    private val mStMatrix = FloatArray(16)
    private val mMVPMatrix = FloatArray(16)
    private val mUIHandler = Handler(Looper.getMainLooper())
    private var mPositionLocation = 0
    private var mTextureCoordLocation = 0
    private var mStMatrixHandle = 0
    private var mMVPMatrixHandle = 0

    init {
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
        mVertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        mVertexBuffer?.put(VERTEX_DATA)?.position(0)
        Matrix.setIdentityM(mStMatrix, 0)
    }

    fun setOnSurfaceLifecycleListener(listener: OnSurfaceLifecycleListener?) {
        mListener = listener
    }

    private fun initSurface() {
        mESOTextureId = createExternalTexture()
        mSurfaceTexture = SurfaceTexture(mESOTextureId)
        mSurfaceTexture!!.setOnFrameAvailableListener(this)
    }

    private fun initGLESEngine() {
        mProgram = createProgram()
        if (mProgram == 0) {
            deInitSurface()
            return
        }
        mPositionLocation = GLES30.glGetAttribLocation(mProgram, "aPosition")
        mTextureCoordLocation = GLES30.glGetAttribLocation(mProgram, "aTextureCoordinate")
        mStMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uStMatrix")
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix")
        if (isGLESStatusError()) {
            deInitSurface()
            Logger.e(TAG, "create external texture failed")
            return
        }
        Logger.i(TAG, "init opengl es success. ")
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Logger.i(TAG, "onSurfaceCreated")
        initSurface()
        initGLESEngine()
        mUIHandler.post {
            if (mListener != null) {
                mListener!!.onSurfaceCreated(mSurfaceTexture)
            }
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Logger.i(TAG, "onSurfaceChanged, width=$width ,height=$height")
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        // 更新纹理
        if (mSurfaceTexture != null) {
            mSurfaceTexture!!.updateTexImage()
            mSurfaceTexture!!.getTransformMatrix(mStMatrix)
        }

        // 绘制纹理
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mESOTextureId)
        mVertexBuffer!!.position(0)
        GLES30.glVertexAttribPointer(
            mPositionLocation,
            3,
            GLES30.GL_FLOAT,
            false,
            20,
            mVertexBuffer
        )
        GLES30.glEnableVertexAttribArray(mPositionLocation)
        mVertexBuffer!!.position(3)
        GLES30.glVertexAttribPointer(
            mTextureCoordLocation,
            2,
            GLES30.GL_FLOAT,
            false,
            20,
            mVertexBuffer
        )
        GLES30.glEnableVertexAttribArray(mTextureCoordLocation)
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, getMVPMatrix(), 0)
        GLES30.glUniformMatrix4fv(mStMatrixHandle, 1, false, mStMatrix, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        Logger.i(TAG, "--------draw a frame---------")
    }

    private fun getMVPMatrix(): FloatArray {
        Matrix.setIdentityM(mMVPMatrix, 0)
        val radius = (0 * Math.PI / 180.0).toFloat()
        mMVPMatrix[0] *= cos(radius.toDouble()).toFloat()
        mMVPMatrix[1] += (-sin(radius.toDouble())).toFloat()
        mMVPMatrix[4] += sin(radius.toDouble()).toFloat()
        mMVPMatrix[5] *= cos(radius.toDouble()).toFloat()
        return mMVPMatrix
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        // 新的一帧到来
        // 会触发GLSurfaceView.Renderer#onDraw()绘制
        requestRender()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        deInitSurface()
        deInitGLESEngine()
        mUIHandler.post {
            if (mListener != null) {
                mListener!!.onSurfaceDestroyed()
            }
        }
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES30.glCreateShader(shaderType)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Logger.e(TAG, "Could not compile shader $shaderType:")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createProgram(): Int {
        // 创建顶点、片段着色器
        mVertexShader = loadShader(GLES30.GL_VERTEX_SHADER, MediaUtils.readRawTextFile(context, R.raw.camera_vertex))
        if (mVertexShader == 0) {
            return 0
        }
        Logger.i(TAG, "load vertex shader success, id = $mVertexShader")
        mFragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, MediaUtils.readRawTextFile(context, R.raw.camera_fragment))
        if (mFragmentShader == 0) {
            return 0
        }
        Logger.i(TAG, "load fragment shader success, id = $mFragmentShader")
        // 创建、链接程序，并将着色器依附到程序
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, mVertexShader)
        GLES30.glAttachShader(program, mFragmentShader)
        GLES30.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Logger.e(TAG, "Could not link program, err = " + linkStatus[0])
            return 0
        }
        // 使用程序
        GLES30.glUseProgram(program)
        Logger.i(TAG, "create and link program success, id = $program")
        return program
    }

    private fun deInitSurface() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture!!.setOnFrameAvailableListener(null)
            mSurfaceTexture!!.release()
            mSurfaceTexture = null
        }
    }

    private fun deInitGLESEngine() {
        if (mVertexShader != 0) {
            GLES30.glDeleteShader(mVertexShader)
            mVertexShader = 0
        }
        if (mFragmentShader != 0) {
            GLES30.glDeleteShader(mFragmentShader)
            mFragmentShader = 0
        }
        if (mProgram != 0) {
            GLES30.glDeleteProgram(mProgram)
            mProgram = 0
        }
        Logger.i(TAG, "release opengl es success")
    }

    private fun isGLESStatusError(): Boolean {
        return GLES30.glGetError() != GLES30.GL_NO_ERROR
    }

    interface OnSurfaceLifecycleListener {
        fun onSurfaceCreated(surface: SurfaceTexture?)
        fun onSurfaceDestroyed()
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST.toFloat())
        GLES30.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR.toFloat())
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        Logger.i(TAG, "create external texture success, texture id = " + textures[0])
        return textures[0]
    }

    fun setDefaultBufferSize(width: Int, height: Int) {
        mSurfaceTexture?.setDefaultBufferSize(width, height)
    }

    override fun setAspectRatio(width: Int, height: Int) {
        val orientation = context.resources.configuration.orientation
        // 处理竖屏和横屏情况
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setAspectRatio(height.toDouble() / width)
            return
        }
        setAspectRatio(width.toDouble() / height)
    }

    override fun getSurfaceWidth(): Int = width

    override fun getSurfaceHeight(): Int = height

    override fun getSurface(): Surface = holder.surface

    override fun postUITask(task: () -> Unit) {
        post {
            task()
        }
    }

    private fun setAspectRatio(aspectRatio: Double) {
        if (aspectRatio < 0 || mAspectRatio == aspectRatio) {
            return
        }
        mAspectRatio = aspectRatio
        Logger.i(TAG, "AspectRatio = $mAspectRatio")
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var initialWidth = MeasureSpec.getSize(widthMeasureSpec)
        var initialHeight = MeasureSpec.getSize(heightMeasureSpec)
        val horizontalPadding = paddingLeft - paddingRight
        val verticalPadding = paddingTop - paddingBottom
        initialWidth -= horizontalPadding
        initialHeight -= verticalPadding
        // 比较预览与TextureView(内容)纵横比
        // 如果有变化，重新设置TextureView尺寸
        val viewAspectRatio = initialWidth.toDouble() / initialHeight
        val diff = mAspectRatio / viewAspectRatio - 1
        var wMeasureSpec = widthMeasureSpec
        var hMeasureSpec = heightMeasureSpec
        if (mAspectRatio > 0 && abs(diff) > 0.01) {
            // diff > 0， 按宽缩放
            // diff < 0， 按高缩放
            if (diff > 0) {
                initialHeight = (initialWidth / mAspectRatio).toInt()
            } else {
                initialWidth = (initialHeight * mAspectRatio).toInt()
            }
            // 重新设置TextureView尺寸
            // 注意加回padding大小
            initialWidth += horizontalPadding
            initialHeight += verticalPadding
            wMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
            hMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
        }
        super.onMeasure(wMeasureSpec, hMeasureSpec)
    }

    companion object {
        private const val TAG = "AspectRatioGLSurfaceView"

        private val VERTEX_DATA = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 0f,  // 右上
            1.0f, -1.0f, 0f, 1f, 0f,  // 右下
            -1.0f, 1.0f, 0f, 0f, 1f,  // 左下
            1.0f, 1.0f, 0f, 1f, 1f
        )
    }
}