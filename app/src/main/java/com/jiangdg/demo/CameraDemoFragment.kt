package com.jiangdg.demo

import android.app.usage.ExternalStorageStats
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IEncodeDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.demo.databinding.FragmentDemo01Binding
import com.jiangdg.utils.XLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer


data class CameraUIState(
    val maxBrightness: Float = 100f,
    val minBrightness: Float = 0f,
    val zoom: Float = 1f,
)

class CameraViewModel : ViewModel() {
    private val _cameraUIState = MutableStateFlow(CameraUIState())
    val cameraUIState = _cameraUIState.asStateFlow()

    fun updateBrightness(min: Int, max: Int) {
        _cameraUIState.update {
            it.copy(minBrightness = min.toFloat(), maxBrightness = max.toFloat())
        }
    }

    fun updateZoom(zoom: Float){
        _cameraUIState.update {
            it.copy(zoom = zoom)
        }
    }

}

class CameraDemoFragment : CameraFragment() {
    override fun getCameraView(): IAspectRatio? {
        return AspectRatioSurfaceView(requireContext())
    }

    val viewModel = CameraViewModel()

    private lateinit var mViewBinding: FragmentDemo01Binding
    override fun getCameraViewContainer(): ViewGroup? {
        return mViewBinding.fragmentContainer
    }

    override fun getRootView(inflater: LayoutInflater, container: ViewGroup?): View? {
        mViewBinding = FragmentDemo01Binding.inflate(inflater, container, false)
        return mViewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewBinding.compose.setContent {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(color = Color.White)) {
                val cameraUIState = viewModel.cameraUIState.collectAsState().value
                BrightnessView(cameraUIState)
                ZoomView(cameraUIState)
                TakePictureView()
            }
        }
    }

    @Composable
    fun TakePictureView() {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = {
                    (getCurrentCamera() as? CameraUVC)?.captureImage(object : ICaptureCallBack {
                        override fun onBegin() {
                        }

                        override fun onError(error: String?) {
                        }

                        override fun onComplete(path: String?) {
                            XLogger.d("拍照完成:${path}")
                        }
                    })

                }) {
                Text("拍照")
            }


            Button(onClick = {
                (getCurrentCamera() as? CameraUVC)?.captureVideoStart(object : ICaptureCallBack {
                    override fun onBegin() {

                    }

                    override fun onError(error: String?) {

                    }

                    override fun onComplete(path: String?) {

                    }
                }, path = "")

            }) {
                Text("录像")
            }
        }
    }

    @Composable
    fun BrightnessView(cameraUIState:CameraUIState) {
        Text("亮度")
        val sliderValue = remember { mutableFloatStateOf(0f) }

        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue.floatValue,
            valueRange = cameraUIState.minBrightness..cameraUIState.maxBrightness,
            onValueChange = {progress->
                sliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setBrightness(progress.toInt())
            },
            onValueChangeFinished = {
            }
        )
    }

    @Composable
    fun ZoomView(cameraUIState:CameraUIState) {
        Text("Zoom")
        val sliderValue = remember { mutableFloatStateOf(1f) }

        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue.floatValue,
            valueRange = 1f..cameraUIState.zoom,
            onValueChange = {progress->
                sliderValue.floatValue = progress
//                (getCurrentCamera() as? CameraUVC)?.setZoom(progress.toInt())

            },
            onValueChangeFinished = {
            }
        )
    }

    override fun onCameraState(
        self: MultiCameraClient.ICamera,
        code: ICameraStateCallBack.State,
        msg: String?
    ) {
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
                XLogger.d("相机打开-----》")
                (getCurrentCamera() as? CameraUVC)?.apply {
                    setAutoFocus(true)
                    setAutoWhiteBalance(true)
                    setEncodeDataCallBack(object :IEncodeDataCallBack{
                        override fun onEncodeData(
                            type: IEncodeDataCallBack.DataType,
                            buffer: ByteBuffer,
                            offset: Int,
                            size: Int,
                            timestamp: Long
                        ) {
                            XLogger.d("数据来了：${size} $timestamp")
                        }

                    })
                    setRenderSize(100,100)

//                    sendCameraCommand()
//                    setZoom(2)


                    val min = getBrightnessMin()?:0
                    val max = getBrightnessMax()?:100
//                val zoom = (getCurrentCamera() as? CameraUVC)?.getZoom()?:0f
//                viewModel.updateZoom(zoom.toFloat())
                    viewModel.updateBrightness(min,max)
//                XLogger.d("亮度 最大:${max} 最小：${min} zoom:${zoom}")
                }

            }

            ICameraStateCallBack.State.CLOSED -> {
                XLogger.d("相机关闭-----》")

            }

            ICameraStateCallBack.State.ERROR -> {
                XLogger.d("相机错误-----》")
            }
        }
    }
}