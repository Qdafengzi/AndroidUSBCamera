package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError

/**
 * todo:预览画面 大小改变要随之改变的
 */
class EffectSharpen(context: Context) : AbstractEffect(context) {

    private var sharpnessLocation = 0
    private var sharpness = 0f
    private var imageWidthFactorLocation = 0
    private var imageHeightFactorLocation = 0

    companion object {
        private const val TAG = "EffectSharpen"
        const val ID = 700
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.sharpness_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_sharpness_fragment

    override fun init() {
        super.init()
        sharpnessLocation = GLES20.glGetUniformLocation(mProgram, "sharpness")
        imageWidthFactorLocation = GLES20.glGetUniformLocation(mProgram, "imageWidthFactor")
        imageHeightFactorLocation = GLES20.glGetUniformLocation(mProgram, "imageHeightFactor")

        if (sharpnessLocation == -1 || imageWidthFactorLocation == -1 || imageHeightFactorLocation == -1) {
           Log.e(TAG,"Failed to get uniform locations")
        }
        setSharpness(sharpness)
    }


    var width = 0
    var height = 0

    override fun setSize(width: Int, height: Int) {
        super.setSize(width, height)
        this.width = width
        this.height = height
    }

    override fun beforeDraw() {
        super.beforeDraw()
        GLES20.glUseProgram(mProgram)
        GLES20.glUniform1f(imageWidthFactorLocation, 1.0f / width)
        checkGlError("glUniform1f imageWidthFactor")
        GLES20.glUniform1f(imageHeightFactorLocation, 1.0f / height)
        checkGlError("glUniform1f imageHeightFactor")
        GLES20.glUniform1f(sharpnessLocation, sharpness)
        checkGlError("glUniform1f sharpness")
    }


    fun setSharpness(sharpness: Float) {
        this.sharpness = sharpness
        GLES20.glUniform1f(sharpnessLocation,sharpness)
    }
}