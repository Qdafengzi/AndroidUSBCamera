package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES20
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError

class EffectCrop(ctx: Context) : AbstractEffect(ctx) {

    private var aspectRatio: Float = 1.0f // 默认为1:1
    private var uAspectRatioLocation: Int = -1

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.crop_vertex

    override fun getFragmentSourceId(): Int = R.raw.crop_fragment


    override fun init() {
        super.init()
        uAspectRatioLocation = GLES20.glGetUniformLocation(mProgram, "uAspectRatio")
    }

    override fun beforeDraw() {
        // 在绘制之前设置裁切比例uniform
        super.beforeDraw()
        GLES20.glUseProgram(mProgram)
        GLES20.glUniform1f(uAspectRatioLocation, aspectRatio)
        checkGlError("aspectRatio")
    }

    override fun drawFrame(textureId: Int): Int {
        // 如果需要在这里进行任何额外的处理，可以在这里添加
        return super.drawFrame(textureId)
    }

    fun setAspectRatio(width: Float, height: Float) {
        aspectRatio = width / height
    }

    companion object {
        const val ID = 10000
    }
}