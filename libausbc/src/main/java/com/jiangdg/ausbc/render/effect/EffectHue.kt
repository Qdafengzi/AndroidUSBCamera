package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import com.elvishew.xlog.XLog
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError

/**
 * 0 to 360 todo：没起作用
 */
class EffectHue(context: Context) : AbstractEffect(context) {

    private var hue = 90.0f
    private var hueLocation = -1

    companion object {
        const val ID = 600
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_hue_fragment

    override fun init() {
        super.init()
        hueLocation = GLES20.glGetUniformLocation(mProgram, "hueAdjust")

    }

    override fun beforeDraw() {
        super.beforeDraw()
        GLES20.glUseProgram(mProgram)
        setHue(hue)
    }


    fun setHue(hue: Float) {
        this.hue = hue
        val hueAdjust = (this.hue % 360.0f) * Math.PI.toFloat() / 180.0f
        GLES20.glUniform1f(hueLocation, hueAdjust)
        checkGlError("glUniform1f hueLocation")
    }

}