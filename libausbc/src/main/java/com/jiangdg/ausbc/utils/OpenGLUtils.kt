package com.jiangdg.ausbc.utils

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES30
import javax.microedition.khronos.opengles.GL10

/** opengl es tool
 *
 * @author Created by jiangdg on 2023/1/17
 */
object OpenGLUtils {
    private const val TAG = "OpenGLUtils"

    fun glGenTextures(textures: IntArray) {
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        Logger.i(TAG, "create texture, id = ${textures[0]}")
    }

    fun isGlEsSupported(context: Context): Boolean {
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.let {
            try {
                it.deviceConfigurationInfo.glEsVersion.toFloat() >= 2.0f
            } catch (e: Exception) {
                false
            }
        } ?: false
    }

    private fun glGetErrorStr(err: Int): String {
        return when (err) {
            GL10.GL_STACK_OVERFLOW -> "stack overflow"
            GL10.GL_STACK_UNDERFLOW -> "stack underflow"
            GLES30.GL_NO_ERROR -> "GL_NO_ERROR"
            GLES30.GL_INVALID_ENUM -> "GL_INVALID_ENUM"
            GLES30.GL_INVALID_VALUE -> "GL_INVALID_VALUE"
            GLES30.GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
            GLES30.GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
            GLES30.GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
            else -> "unknown"
        }
    }

    fun checkGlError(op: String) {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            val msg = op + ": glError 0x" + Integer.toHexString(error) + ":" + glGetErrorStr(error)
            Logger.e(TAG, msg)
        }
    }
}