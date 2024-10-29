package jp.co.cyberagent.android.gpuimage.filter.custom;

import android.opengl.GLES30;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter

import kotlin.jvm.JvmOverloads;

/**
 * 动态阈值算法
 */
class GPUDynamicThresholdFilter @JvmOverloads
constructor(
    threshold: Float = 0.5f  // 默认阈值
) : GPUImageFilter(NO_FILTER_VERTEX_SHADER, FRAGMENT_SHADER) {

    companion object {
        val FRAGMENT_SHADER = """
        precision highp float;
        varying vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        uniform float threshold;  // 动态阈值，用于调整亮度
        void main() {
            vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);
            float luminance = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114));  // 计算亮度
            float brightnessFactor = (luminance > threshold) ? 1.2 : 0.8;  // 根据亮度与阈值比较调整亮度系数
            vec4 correctedColor = vec4(textureColor.rgb * brightnessFactor, textureColor.a);
            gl_FragColor = correctedColor;
        }
        """.trimIndent()
    }

    private var threshold = 0.5f

    private var thresholdLocation = 0

    init {
        this.threshold = threshold
    }

    override fun onInitialized() {
        super.onInitialized()
        setThreshold(threshold)
    }

    override fun onInit() {
        super.onInit()
        thresholdLocation = GLES30.glGetUniformLocation(program, "threshold")
    }

    fun setThreshold(threshold: Float) {
        this.threshold = threshold
        setFloat(thresholdLocation, this.threshold)
    }
}
