package com.camera.demo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.camera.demo.databinding.FragmentGrayWorldBinding
import com.camera.utils.XLogger
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUDynamicThresholdFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUGrayWorldBalanceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUPerfectReflectorBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GrayWorldFragment : Fragment() {
    lateinit var binding: FragmentGrayWorldBinding

    private val mGPUDynamicThresholdFilter by lazy {
        GPUDynamicThresholdFilter()
    }

    private val mGPUPerfectReflectorBalanceFilter by lazy {
        GPUPerfectReflectorBalanceFilter()
    }

    private val mGPUGrayWorldBalanceFilter by lazy {
        GPUGrayWorldBalanceFilter()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentGrayWorldBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.image1)
        binding.composeView.setContent {
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White)
            ) {
                var gpuImageView by remember { mutableStateOf<GPUImageView?>(null) }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4 / 3f)
                        .border(width = 1.dp,color = Color.Black)
                    ,
                    contentDescription = "",
                    contentScale = ContentScale.FillWidth
                )
                AndroidView(
                    factory = { context ->
                        GPUImageView(context).apply {
                            gpuImageView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4 / 3f)
                        .border(width = 1.dp,color = Color.Black)
                    ,
                    update = {
                        gpuImageView?.setImage(bitmap)
                    },
                )
                Row(modifier = Modifier.fillMaxWidth()){
                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                gpuImageView?.filter = mGPUGrayWorldBalanceFilter
                                val array = calculateAverageColor(bitmap)
                                XLogger.d("${array[0]} ${array[1]} ${array[2]}")
                                mGPUGrayWorldBalanceFilter.setGrayWorldFactors(array[0], array[1], array[2])
                                withContext(Dispatchers.Main){
                                    gpuImageView?.requestRender()
                                }

                            }
                        }
                    ) {
                        Text(text = "应用灰度算法", fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                gpuImageView?.filter = mGPUPerfectReflectorBalanceFilter
                                val array = calculateAverageColor(bitmap)
                                XLogger.d("${array[0]} ${array[1]} ${array[2]}")
                                mGPUPerfectReflectorBalanceFilter.setPerfectReflectorFactors(array[0], array[1], array[2])
                                gpuImageView?.requestRender()
                            }
                        }
                    ) {
                        Text(text = "完美反射算法", fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                gpuImageView?.filter = GPUImageFilter()
                                gpuImageView?.setImage(bitmap)
                            }
                        }
                    ) {
                        Text(text = "重置", fontSize = 16.sp)
                    }
                }






//                val sliderValue = remember { mutableFloatStateOf(0.5f) }
//
//                SliderView(
//                    modifier = Modifier.fillMaxWidth(),
//                    name = "动态阈值算法",
//                    range = 0f..1f,
//                    sliderValue = sliderValue,
//                    onValueChange = {
//                        sliderValue.floatValue = it
//                        mGPUDynamicThresholdFilter.setThreshold(it)
//                        gpuImageView?.requestRender()
//                    }
//                )
            }
        }
    }

    fun calculateAverageColor(bitmap: Bitmap): FloatArray {
        var totalRed: Long = 0
        var totalGreen: Long = 0
        var totalBlue: Long = 0
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        // 遍历图像的每一个像素
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = bitmap.getPixel(x, y)
                totalRed += ((color shr 16) and 0xff).toLong() // 红色通道
                totalGreen += ((color shr 8) and 0xff).toLong() // 绿色通道
                totalBlue += (color and 0xff).toLong() // 蓝色通道
            }
        }

        // 计算每个通道的平均值
        val avgRed = totalRed.toFloat() / totalPixels
        val avgGreen = totalGreen.toFloat() / totalPixels
        val avgBlue = totalBlue.toFloat() / totalPixels

        return floatArrayOf(avgRed, avgGreen, avgBlue)
    }
}