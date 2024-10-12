package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES30
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError
import java.lang.reflect.Array.setFloat

/**
 *contrast value ranges from 0.0 to 4.0, with 1.0 as the normal level
 */
class EffectContrast(context: Context) : AbstractEffect(context) {
    private var contrastLocation = 0
    private var contrast = 1.2f

    companion object {
        const val ID = 900
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_contrast_fragment

    override fun init() {
        super.init()
        contrastLocation = GLES30.glGetUniformLocation(mProgram, "contrast")
        setContrast(contrast)
    }

    override fun beforeDraw() {
        super.beforeDraw()
        GLES30.glUseProgram(mProgram)
        setContrast(contrast)
    }


    fun setContrast(contrast: Float) {
        this.contrast = contrast
        GLES30.glUniform1f(contrastLocation, contrast)
        checkGlError("glUniform1f contrast")
    }
}