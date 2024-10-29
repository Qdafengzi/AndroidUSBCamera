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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.camera.demo.Utils.calculateAverageColor
import com.camera.demo.databinding.FragmentGrayWorldBinding
import com.camera.utils.XLogger
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera.UVC_VS_FRAME_MJPEG
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.custom.GPUDynamicThresholdFilter
import jp.co.cyberagent.android.gpuimage.filter.custom.GPUGrayWorldBalanceFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.custom.GPUImagePerfectReflectorBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GrayWorldFragment : Fragment() {
    lateinit var binding: FragmentGrayWorldBinding


    private val mGPUPerfectReflectorBalanceFilter by lazy {
        GPUImagePerfectReflectorBalanceFilter()
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

        binding.composeView.setContent {

            val imageList = mutableListOf<Int>()


            val scope = rememberCoroutineScope()
            val bitmap = remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(Unit) {
                bitmap.value = BitmapFactory.decodeResource(resources, R.mipmap.image2)
                imageList.add(R.mipmap.image1)
                imageList.add(R.mipmap.image2)
                imageList.add(R.mipmap.image3)
                imageList.add(R.mipmap.image4)
                imageList.add(R.mipmap.image6)
                imageList.add(R.mipmap.image_l_1)
                imageList.add(R.mipmap.image_l_2)
                imageList.add(R.mipmap.image_l_3)
                imageList.add(R.mipmap.image_l_4)
            }


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White)
            ) {
                var gpuImageView by remember { mutableStateOf<GPUImageView?>(null) }
                bitmap.value?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4 / 3f)
                            .border(width = 1.dp, color = Color.Black),
                        contentDescription = "",
                        contentScale = ContentScale.Fit
                    )
                }

                AndroidView(
                    factory = { context ->
                        GPUImageView(context).apply {
                            gpuImageView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4 / 3f)
                        .border(width = 1.dp, color = Color.Black),
                    update = {
                        bitmap.value?.let {
                            gpuImageView?.setImage(bitmap.value)
                        }
                    },
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = {
                            if (bitmap.value != null) {
                                scope.launch(Dispatchers.IO) {
                                    gpuImageView?.filter = mGPUGrayWorldBalanceFilter
                                    val array = calculateAverageColor(bitmap.value!!)
                                    XLogger.d("${array[0]} ${array[1]} ${array[2]}")
                                    mGPUGrayWorldBalanceFilter.setGrayWorldFactors(
                                        array[0],
                                        array[1],
                                        array[2]
                                    )
                                    withContext(Dispatchers.Main) {
                                        gpuImageView?.requestRender()
                                    }

                                }
                            }
                        }
                    ) {
                        Text(text = "应用灰度算法", fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = {
                            if (bitmap.value != null) {
                                scope.launch(Dispatchers.IO) {
                                    gpuImageView?.filter = mGPUPerfectReflectorBalanceFilter
                                    val array = calculateAverageColor(bitmap.value!!)
                                    XLogger.d("${array[0]} ${array[1]} ${array[2]}")
                                    mGPUPerfectReflectorBalanceFilter.setPerfectReflectorFactors(
                                        array[0],
                                        array[1],
                                        array[2]
                                    )
                                    gpuImageView?.requestRender()
                                }
                            }

                        }
                    ) {
                        Text(text = "完美反射算法", fontSize = 16.sp)
                    }

                    TextButton(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                gpuImageView?.filter = GPUImageFilter()
                                gpuImageView?.setImage(bitmap.value)
                            }
                        }
                    ) {
                        Text(text = "重置", fontSize = 16.sp)
                    }
                }

                var show by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                show = true
                            }) {
                            Text("选照片")
                        }
                    }
                    DropdownMenu(
                        modifier = Modifier.fillMaxSize(),
                        expanded = show,
                        onDismissRequest = {
                            show = false
                        },
                    ) {
                        imageList.forEach {
                            DropdownMenuItem(
                                modifier = Modifier.fillMaxWidth()
                                ,
                                text = {
                                    Image(
                                        painter = painterResource(it),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth(0.5f)
                                            .aspectRatio(4 / 3f)
                                    )
                                },
                                onClick = {
                                    show = false
                                    bitmap.value = BitmapFactory.decodeResource(resources, it)
                                },
                            )
                        }
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