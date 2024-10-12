package com.jiangdg.ausbc.render.effect

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import com.jiangdg.ausbc.R
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.utils.OpenGLUtils.checkGlError
import java.nio.FloatBuffer

class EffectImageLevel(context: Context) : AbstractEffect(context) {

    private var minLocation = 0
    private var min: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var midLocation = 0
    private var mid: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f)
    private var maxLocation = 0
    private var max: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f)
    private var minOutputLocation = 0
    private var minOutput: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f)
    private var maxOutputLocation = 0
    private var maxOutput: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f)

    companion object {
        const val ID = 500
        private const val TAG = "EffectImageLevel"
    }

    override fun getId(): Int = ID

    override fun getClassifyId(): Int = CameraEffect.CLASSIFY_ID_FILTER

    override fun getVertexSourceId(): Int = R.raw.base_vertex

    override fun getFragmentSourceId(): Int = R.raw.effect_image_level_fragment

    override fun init() {
        super.init()
        this.min = floatArrayOf(0.0f, 0.0f, 0.0f)
        this.mid = floatArrayOf(1.0f, 1.0f, 1.0f)
        this.max = floatArrayOf(1.0f, 1.0f, 1.0f)
        minOutput = floatArrayOf(0.0f, 0.0f, 0.0f)
        maxOutput = floatArrayOf(1.0f, 1.0f, 1.0f)

        minLocation = GLES30.glGetUniformLocation(mProgram, "levelMinimum")
        midLocation = GLES30.glGetUniformLocation(mProgram, "levelMiddle")
        maxLocation = GLES30.glGetUniformLocation(mProgram, "levelMaximum")
        minOutputLocation = GLES30.glGetUniformLocation(mProgram, "minOutput")
        maxOutputLocation = GLES30.glGetUniformLocation(mProgram, "maxOutput")

        // Check if all uniforms were found
        if (minLocation == -1 || midLocation == -1 || maxLocation == -1 ||
            minOutputLocation == -1 || maxOutputLocation == -1) {
            Log.e(TAG,"Failed to get uniform locations in init")
        }
    }

    override fun beforeDraw() {
        super.beforeDraw()
        updateUniforms()
    }


    fun setMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        setRedMin(min, mid, max, minOut, maxOut)
        setGreenMin(min, mid, max, minOut, maxOut)
        setBlueMin(min, mid, max, minOut, maxOut)
    }

    fun setMin(min: Float, mid: Float, max: Float) {
        setMin(min, mid, max, 0.0f, 1.0f)
    }

    fun setRedMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        this.min[0] = min
        this.mid[0] = mid
        this.max[0] = max
        minOutput[0] = minOut
        maxOutput[0] = maxOut
        updateUniforms()
    }

    fun setRedMin(min: Float, mid: Float, max: Float) {
        setRedMin(min, mid, max, 0f, 1f)
    }

    fun setGreenMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        this.min[1] = min
        this.mid[1] = mid
        this.max[1] = max
        minOutput[1] = minOut
        maxOutput[1] = maxOut
        updateUniforms()
    }

    fun setGreenMin(min: Float, mid: Float, max: Float) {
        setGreenMin(min, mid, max, 0f, 1f)
    }

    fun setBlueMin(min: Float, mid: Float, max: Float, minOut: Float, maxOut: Float) {
        this.min[2] = min
        this.mid[2] = mid
        this.max[2] = max
        minOutput[2] = minOut
        maxOutput[2] = maxOut
        updateUniforms()
    }

    fun setBlueMin(min: Float, mid: Float, max: Float) {
        setBlueMin(min, mid, max, 0f, 1f)
    }

    private fun updateUniforms() {
        GLES30.glUniform3fv(minLocation, 1, FloatBuffer.wrap(min))
        checkGlError("glUniform3fv minLocation")

        GLES30.glUniform3fv(midLocation, 1, FloatBuffer.wrap(mid))
        checkGlError("glUniform3fv midLocation")

        GLES30.glUniform3fv(maxLocation, 1, FloatBuffer.wrap(max))
        checkGlError("glUniform3fv maxLocation")

        GLES30.glUniform3fv(minOutputLocation, 1, FloatBuffer.wrap(minOutput))
        checkGlError("glUniform3fv minOutputLocation")

        GLES30.glUniform3fv(maxOutputLocation, 1, FloatBuffer.wrap(maxOutput))
        checkGlError("glUniform3fv maxOutputLocation")
    }
}