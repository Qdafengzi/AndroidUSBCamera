package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES20
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import java.lang.reflect.Array.setFloat

/**
 * saturation: The degree of saturation or desaturation to apply to the image (0.0 - 2.0, with 1.0 as the default)
 */
class EffectSaturation(context: Context) : AbstractEffect(context) {

    private var saturationLocation = 0
    private var saturation = 0f

    companion object {
        const val ID = 800
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_saturation_fragment

    override fun init() {
        super.init()
        saturationLocation = GLES20.glGetUniformLocation(mProgram, "saturation")
    }

    override fun beforeDraw() {
        super.beforeDraw()
        setSaturation(saturation)
    }


    fun setSaturation(saturation: Float) {
        this.saturation = saturation
        GLES20.glUniform1f(saturationLocation,saturation)
    }
}