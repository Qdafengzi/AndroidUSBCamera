package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES30
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError

class EffectWhiteBalance(context: Context) : AbstractEffect(context) {

    companion object {
        const val ID = 400
    }

    private var temperatureLocation = 0
    private var tintLocation = 0
    private var temperature = 0f
    private var tint = 0f

    override fun init() {
        super.init()
        this.tint = 0.0f
        this.temperature = 5500f

        temperatureLocation = GLES30.glGetUniformLocation(mProgram, "temperature")
        tintLocation = GLES30.glGetUniformLocation(mProgram, "tint")
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_whitebalance_fragment

    override fun beforeDraw() {
        super.beforeDraw()
        setTemperature(temperature)
        setTint(tint)
    }


    fun setTemperature(temperature: Float) {
        this.temperature = temperature
        GLES30.glUniform1f(
            temperatureLocation,
            if (this.temperature < 5000) (0.0004 * (this.temperature - 5000.0)).toFloat() else (0.00006 * (this.temperature - 5000.0)).toFloat()
        )
        checkGlError("setTemperature")
    }

    fun setTint(tint: Float) {
        this.tint = tint
        GLES30.glUniform1f(tintLocation, (this.tint / 100.0).toFloat())
        checkGlError("setTint")
    }
}