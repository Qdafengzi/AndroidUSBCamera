package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES30
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError
import java.lang.reflect.Array.setFloat

/**
 *  brightness value ranges from -1.0 to 1.0, with 0.0 as the normal level
 */
class EffectBrightness(context: Context) : AbstractEffect(context) {
    private var brightnessLocation = 0
    private var brightness = 0f
    companion object {
        const val ID = 1100
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_brightness_fragment

    override fun init() {
        super.init()
        brightnessLocation = GLES30.glGetUniformLocation(mProgram, "brightness")
    }

    override fun beforeDraw() {
        super.beforeDraw()
        GLES30.glUseProgram(mProgram)
        setBrightness(brightness)
    }
    fun setBrightness(brightness: Float) {
        this.brightness = brightness
        GLES30.glUniform1f(brightnessLocation, brightness)
        checkGlError("glUniform1f brightness")
    }

}