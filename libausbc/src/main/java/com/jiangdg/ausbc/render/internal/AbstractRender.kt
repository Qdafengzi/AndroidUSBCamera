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
package com.jiangdg.ausbc.render.internal

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.util.Log
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/** Abstract render class based on opengl es 2.0
 *      openGL ES  initial and frame drawing
 *
 * @author Created by jiangdg on 2021/12/27
 */
abstract class AbstractRender(context: Context) {
    private var mFragmentShader: Int = 0
    private var mVertexShader: Int = 0
    private var mContext: Context? = null
    private var mStMatrixHandle = 0
    private var mMVPMatrixHandle = 0
    protected var mWidth: Int = 0
    protected var mHeight: Int = 0
    protected var mProgram: Int = 0
    protected var mPositionLocation = 0
    protected var mTextureCoordLocation = 0
    protected var mTextureSampler = 0

    var mTriangleVertices: FloatBuffer = ByteBuffer.allocateDirect(
        mTriangleVerticesData.size * FLOAT_SIZE_BYTES
    ).order(ByteOrder.nativeOrder()).asFloatBuffer()

    init {
        this.mContext = context
        this.mTriangleVertices.put(mTriangleVerticesData).position(0)
    }

    open fun setSize(width: Int, height: Int) {
        this.mWidth = width
        this.mHeight = height
        GLES30.glViewport(0, 0, mWidth, mHeight)
    }

    open fun drawFrame(textureId: Int): Int {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT or GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glViewport(0, 0, mWidth, mHeight)
        // 1. 激活程序，绑定纹理
        GLES30.glUseProgram(mProgram)

        // 2. 链接顶点属性
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
        GLES30.glVertexAttribPointer(mPositionLocation, 3, GLES30.GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        GLES30.glEnableVertexAttribArray(mPositionLocation)
        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
        GLES30.glVertexAttribPointer(mTextureCoordLocation, 2, GLES30.GL_FLOAT, false,
            TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices)
        GLES30.glEnableVertexAttribArray(mTextureCoordLocation)

        beforeDraw()

        // 3. 绘制
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(getBindTextureType(), textureId)
        GLES30.glUniform1i(mTextureSampler, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindTexture(getBindTextureType(), 0)
        return textureId
    }

    protected open fun beforeDraw() {}
    protected open fun init() {}
    protected open fun clear() {}
    protected open fun getBindTextureType() = GLES30.GL_TEXTURE_2D
    protected abstract fun getVertexSourceId(): Int
    protected abstract fun getFragmentSourceId(): Int

    fun initGLES() {
        val vertexShaderSource = MediaUtils.readRawTextFile(mContext!!, getVertexSourceId())
        val fragmentShaderSource = MediaUtils.readRawTextFile(mContext!!, getFragmentSourceId())
        mProgram = createProgram(vertexShaderSource, fragmentShaderSource)
        if (mProgram == 0) {
            Logger.e(TAG, "create program failed, err = ${GLES30.glGetError()}")
            return
        }
        mPositionLocation = GLES30.glGetAttribLocation(mProgram, "aPosition")
        mTextureCoordLocation = GLES30.glGetAttribLocation(mProgram, "aTextureCoordinate")
        mTextureSampler = GLES30.glGetUniformLocation(mProgram, "uTextureSampler")
        if (isGLESStatusError()) {
            Logger.e(TAG, "create external texture failed, err = ${GLES30.glGetError()}")
            return
        }
        init()
        Logger.i(TAG, "init surface texture render success!")
    }

    fun releaseGLES() {
        if (mVertexShader != 0) {
            GLES30.glDeleteShader(mVertexShader)
        }
        if (mFragmentShader != 0) {
            GLES30.glDeleteShader(mFragmentShader)
        }
        if (mProgram != 0) {
            GLES30.glDeleteProgram(mProgram)
        }
        clear()
        Logger.i(TAG, "release surface texture render success!")
    }

    fun getRenderWidth() = mWidth

    fun getRenderHeight() = mHeight

    private fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES30.glCreateShader(shaderType)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader, info = ${GLES30.glGetShaderInfoLog(shader)}, T = ${Thread.currentThread().name}")
            GLES30.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        // 创建顶点、片段着色器
        mVertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        if (mVertexShader == 0) {
            Logger.i(TAG, "vertexSource err = ${GLES30.glGetError()}: \n $vertexSource")
            return 0
        }
        mFragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        if (mFragmentShader == 0) {
            Logger.i(TAG, "fragmentSource err = ${GLES30.glGetError()}: \n $fragmentSource")
            return 0
        }
        // 创建链接程序，并将着色器依附到程序
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, mVertexShader)
        GLES30.glAttachShader(program, mFragmentShader)
        GLES30.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Logger.e(TAG, "create program failed.")
            GLES30.glDeleteProgram(program)
            return 0
        }
        return program
    }

    private fun isGLESStatusError() = GLES30.glGetError() != GLES30.GL_NO_ERROR

    protected fun createTexture(textures: IntArray) {
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        Logger.i(TAG, "create texture, id = ${textures[0]}")
    }

    fun createOESTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        Logger.i(TAG, "create external texture, id = ${textures[0]}")
        return textures[0]
    }

    companion object {
        private const val TAG = "AbstractRender"
        private const val FLOAT_SIZE_BYTES = 4
        const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES
        const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3

        private val mTriangleVerticesData = floatArrayOf(
            // 坐标            纹理
            // X,    Y,    Z,  U,  V
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f,
        )
    }
}