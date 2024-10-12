package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES30
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError
import java.lang.reflect.Array.setFloat

/**
 * gamma value ranges from 0.0 to 3.0, with 1.0 as the normal level
 */
class EffectGamma(context: Context) : AbstractEffect(context) {
    private var gammaLocation = 0
    private var gamma = 1.0f
    companion object {
        const val ID = 1000
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_gamma_fragment

    override fun init() {
        super.init()
        gammaLocation = GLES30.glGetUniformLocation(mProgram, "gamma")
    }

    override fun beforeDraw() {
        super.beforeDraw()
        GLES30.glUseProgram(mProgram)
        setGamma(gamma)
    }

    fun setGamma(gamma: Float) {
        this.gamma = gamma
        GLES30.glUniform1f(gammaLocation, gamma)
        checkGlError("glUniform1f gamma")
    }

}