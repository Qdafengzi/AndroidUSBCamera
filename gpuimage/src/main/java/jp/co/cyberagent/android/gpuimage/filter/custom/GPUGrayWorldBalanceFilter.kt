package jp.co.cyberagent.android.gpuimage.filter.custom

import android.opengl.GLES30
import com.gemlightbox.core.utils.XLogger
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter


/**
 * 灰度世界算法
 */
class GPUGrayWorldBalanceFilter @JvmOverloads constructor(avgR: Float = 1.0f, avgG: Float = 1.0f, avgB: Float = 1.0f) : GPUImageFilter(
    NO_FILTER_VERTEX_SHADER, FRAGMENT_SHADER
) {

    companion object {
        val FRAGMENT_SHADER = """
        precision highp float;
        varying vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        uniform float avgRed;
        uniform float avgGreen;
        uniform float avgBlue;
        void main() {
            vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
            // 使用外部传入的平均值
            float K = (avgRed + avgGreen + avgBlue) / 3.0;
            // 计算增益
            float gainR = K / avgRed;
            float gainG = K / avgGreen;
            float gainB = K / avgBlue;
            // 应用增益
            vec4 correctedColor = vec4(textureColor.r * gainR, textureColor.g * gainG, textureColor.b * gainB, textureColor.a);
            gl_FragColor = correctedColor;
        }
    """.trimIndent()
    }

    private var avgR = 1.0f
    private var avgG = 1.0f
    private var avgB = 1.0f

    private var avgRLocation = 0
    private var avgGLocation = 0
    private var avgBLocation = 0

    init {
        this.avgR = avgR
        this.avgG = avgG
        this.avgB = avgB
    }

    override fun onInitialized() {
        super.onInitialized()
        setGrayWorldFactors(avgR, avgG, avgB)
    }

    override fun onInit() {
        super.onInit()
        avgRLocation = GLES30.glGetUniformLocation(program, "avgRed")
        avgGLocation = GLES30.glGetUniformLocation(program, "avgGreen")
        avgBLocation = GLES30.glGetUniformLocation(program, "avgBlue")
    }

    fun setGrayWorldFactors(avgRed: Float, avgGreen: Float, avgBlue: Float) {
        this.avgR = avgRed / 255.0f
        this.avgG = avgGreen / 255.0f
        this.avgB = avgBlue / 255.0f
        XLogger.d("avgR:${avgR} avgG:${avgG} avgB:${avgB}")
        setFloat(avgRLocation, this.avgR)
        setFloat(avgGLocation, this.avgG)
        setFloat(avgBLocation, this.avgB)
    }
}
