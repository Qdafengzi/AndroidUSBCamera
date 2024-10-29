package jp.co.cyberagent.android.gpuimage.filter.custom;

import android.opengl.GLES30;

import com.gemlightbox.core.utils.XLogger;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

import kotlin.jvm.JvmOverloads;

/**
 *  完美反射算法
 */
class GPUImagePerfectReflectorBalanceFilter @JvmOverloads constructor(maxR: Float = 0.7154364f, maxG: Float = 0.70618117f, maxB: Float = 0.6837189f) : GPUImageFilter(
    NO_FILTER_VERTEX_SHADER, FRAGMENT_SHADER
) {

    companion object {
        val FRAGMENT_SHADER = """
        precision highp float;
        varying vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        uniform float maxRed;
        uniform float maxGreen;
        uniform float maxBlue;
        void main() {
            vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
            // 应用增益，将最亮的颜色调整为白色
            float gainR = 1.0 / maxRed;
            float gainG = 1.0 / maxGreen;
            float gainB = 1.0 / maxBlue;
            vec4 correctedColor = vec4(textureColor.r * gainR, textureColor.g * gainG, textureColor.b * gainB, textureColor.a);
            gl_FragColor = correctedColor;
        }
        """.trimIndent()
    }

    private var maxR = 0.7154364f
    private var maxG = 0.70618117f
    private var maxB = 0.6837189f

    private var maxRLocation = 0
    private var maxGLocation = 0
    private var maxBLocation = 0

    init {
        this.maxR = maxR
        this.maxG = maxG
        this.maxB = maxB
    }

    override fun onInitialized() {
        super.onInitialized()
        setPerfectReflectorFactorsInnerFun(maxR, maxG, maxB)
    }

    override fun onInit() {
        super.onInit()
        maxRLocation = GLES30.glGetUniformLocation(program, "maxRed")
        maxGLocation = GLES30.glGetUniformLocation(program, "maxGreen")
        maxBLocation = GLES30.glGetUniformLocation(program, "maxBlue")
    }


    private fun setPerfectReflectorFactorsInnerFun(maxR: Float, maxG: Float, maxB: Float) {
        this.maxR = maxR
        this.maxG = maxG
        this.maxB = maxB
        XLogger.d("maxR:${maxR} maxG:${maxG} maxB:${maxB}")
        setFloat(maxRLocation, this.maxR)
        setFloat(maxGLocation, this.maxG)
        setFloat(maxBLocation, this.maxB)
    }

    fun reset(){
        setPerfectReflectorFactorsInnerFun(maxR, maxG, maxB)
    }

    fun setPerfectReflectorFactors(maxRed: Float, maxGreen: Float, maxBlue: Float) {
        XLogger.d("maxRed:${maxRed} maxGreen:${maxGreen} maxBlue:${maxBlue}")
        if (maxRed == 0f && maxGreen == 0f && maxBlue == 0f) {
            return
        }
        this.maxR = maxRed / 255.0f
        this.maxG = maxGreen / 255.0f
        this.maxB = maxBlue / 255.0f

        XLogger.d("maxR:${maxR} maxG:${maxG} maxB:${maxB}")
        setFloat(maxRLocation, this.maxR)
        setFloat(maxGLocation, this.maxG)
        setFloat(maxBLocation, this.maxB)
    }
}
