package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES20
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect

/**
 * todo:预览画面 大小改变要随之改变的 无效果-----》
 */
class EffectSharpen(context: Context) : AbstractEffect(context) {

    private var sharpnessLocation = 0
    private var sharpness = 0f
    private var imageWidthFactorLocation = 0
    private var imageHeightFactorLocation = 0

    companion object {
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
        setSharpness(sharpness)
    }


    var width = 0
    var height = 0

    override fun setSize(width: Int, height: Int) {
        super.setSize(width, height)
        this.width = width
        this.height = height

        GLES20.glUniform1f(imageWidthFactorLocation,1.0f / width)
        GLES20.glUniform1f(imageHeightFactorLocation,1.0f / height)
    }

    override fun beforeDraw() {
        super.beforeDraw()
        GLES20.glUniform1f(imageWidthFactorLocation,1.0f / width)
        GLES20.glUniform1f(imageHeightFactorLocation,1.0f / height)
    }


    fun setSharpness(sharpness: Float) {
        this.sharpness = sharpness
        GLES20.glUniform1f(sharpnessLocation,sharpness)
    }
}