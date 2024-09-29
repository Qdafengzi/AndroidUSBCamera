package com.jiangdg.demo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IEncodeDataCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.effect.EffectContrast
import com.jiangdg.ausbc.render.effect.EffectHue
import com.jiangdg.ausbc.render.effect.EffectImageLevel
import com.jiangdg.ausbc.render.effect.EffectSaturation
import com.jiangdg.ausbc.render.effect.EffectSharpen
import com.jiangdg.ausbc.render.effect.EffectWhiteBalance
import com.jiangdg.ausbc.render.effect.bean.CameraEffect
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import com.jiangdg.ausbc.widget.IAspectRatio
import com.jiangdg.demo.databinding.FragmentDemo01Binding
import com.jiangdg.utils.XLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
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
    override fun getCameraRequest(): CameraRequest {
        val request = CameraRequest.Builder()
//            .setPreviewWidth(2160) // camera preview width
//            .setPreviewHeight(2160) // camera preview height
            .setRenderMode(CameraRequest.RenderMode.OPENGL) // camera render mode
            .setDefaultRotateType(RotateType.ANGLE_0) // rotate camera image when opengl mode
            .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO) // set audio source
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV) // set preview format, MJPEG recommended
//            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG) // set preview format, MJPEG recommended
            .setAspectRatioShow(true) // aspect render,default is true
            .setCaptureRawImage(false) // capture raw image picture when opengl mode
            .setRawPreviewData(true)  // preview raw image when opengl mode
//            .setDefaultEffect(Effect)
            .create()

        return request

    }

    override fun getCameraView(): IAspectRatio? {
        return AspectRatioSurfaceView(requireContext())
    }

    val viewModel = CameraViewModel()

    val mWhiteBalance by lazy {
        EffectWhiteBalance(this@CameraDemoFragment.requireContext())
    }


    private val mEffectDataList by lazy {
        arrayListOf(
//            CameraEffect.NONE_FILTER,

            CameraEffect(
                EffectImageLevel.ID,
                "ImageLevel",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectImageLevel(requireActivity()),
                coverResId = R.mipmap.filter0
            ),

            CameraEffect(
                EffectWhiteBalance.ID,
                "WhiteBalance",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectWhiteBalance(requireActivity()),
                coverResId = R.mipmap.filter0
            ),

            CameraEffect(
                EffectHue.ID,
                "Hue",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectHue(requireActivity()),
                coverResId = R.mipmap.filter0
            ),

            CameraEffect(
                EffectSharpen.ID,
                "Sharpness",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectSharpen(requireActivity()),
                coverResId = R.mipmap.filter0
            ),

            CameraEffect(
                EffectSaturation.ID,
                "Saturation",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectSaturation(requireActivity()),
                coverResId = R.mipmap.filter0
            ),

            CameraEffect(
                EffectContrast.ID,
                "Contrast",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectContrast(requireActivity()),
                coverResId = R.mipmap.filter0
            ),
        )
    }

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
            val scrollState = rememberScrollState()
            // Create a nested scroll connection to handle nested scroll scenarios
            val nestedScrollConnection = remember {
                object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {}
            }


            Column(modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState, enabled = true)
                .nestedScroll(connection = nestedScrollConnection)
                .background(color = Color.White),
            ) {
                val cameraUIState = viewModel.cameraUIState.collectAsState().value
                val contrastSliderValue = remember { mutableFloatStateOf(1f) }
                SliderView(
                    name = "Contrast",
                    range = 0f..4f,
                    sliderValue = contrastSliderValue,
                    onValueChange = { progress ->
                        contrastSliderValue.floatValue = progress
                        mEffectDataList.forEachIndexed { _, cameraEffect ->
                            if (cameraEffect.effect is EffectContrast) {
                                (cameraEffect.effect as EffectContrast).setContrast(progress)
                            }
                        }
                    }
                )


                val hueSliderValue = remember { mutableFloatStateOf(90f) }
                SliderView(
                    name = "Hue",
                    range = 0f..360f,
                    sliderValue = hueSliderValue,
                    onValueChange = { progress ->
                        hueSliderValue.floatValue = progress
                        mEffectDataList.forEachIndexed { _, cameraEffect ->
                            if (cameraEffect.effect is EffectHue){
                                (cameraEffect.effect as EffectHue).setHue(progress)
                            }
                        }
                    }
                )


                val sharpnessSliderValue = remember { mutableFloatStateOf(0f) }
                SliderView(
                    name = "Sharpness",
                    range = -4f..4f,
                    sliderValue = sharpnessSliderValue,
                    onValueChange = { progress ->
                        sharpnessSliderValue.floatValue = progress
                        mEffectDataList.forEachIndexed { _, cameraEffect ->
                            if (cameraEffect.effect is EffectSharpen){
                                (cameraEffect.effect as EffectSharpen).setSharpness(progress)
                            }
                        }
                    }
                )

                val toneSliderValue = remember { mutableFloatStateOf(0f) }
                SliderView(
                    name = "Tone",
                    range = -0.3f..0.3f,
                    sliderValue = toneSliderValue,
                    onValueChange = { progress ->
                        toneSliderValue.floatValue = progress
                        mEffectDataList.forEachIndexed { _, cameraEffect ->
                            if (cameraEffect.effect is EffectImageLevel){
                                (cameraEffect.effect as EffectImageLevel).setMin(progress,1.0f,0.62f)
                            }
                        }
                    }
                )


                val saturationSliderValue = remember { mutableFloatStateOf(0f) }
                SliderView(
                    name = "Saturation",
                    range = -1f..1f,
                    sliderValue = saturationSliderValue,
                    onValueChange = { progress ->
                        saturationSliderValue.floatValue = progress
                        mEffectDataList.forEachIndexed { _, cameraEffect ->
                            if (cameraEffect.effect is EffectSaturation) {
                                (cameraEffect.effect as EffectSaturation).setSaturation(progress)
                            }
                        }
                    }
                )

                val temperatureSliderValue = remember { mutableFloatStateOf(5000f) }
                SliderView(
                    name = "temperature",
                    range = 2000f..9000f,
                    sliderValue = temperatureSliderValue,
                    onValueChange = { progress ->
                        temperatureSliderValue.floatValue = progress
                        mEffectDataList.forEachIndexed { _, cameraEffect ->
                            if (cameraEffect.effect is EffectWhiteBalance){
                                (cameraEffect.effect as EffectWhiteBalance).setTemperature(progress)
                            }
                        }
                    }
                )

                val tintSliderValue = remember { mutableFloatStateOf(50f) }
                SliderView(
                    name = "tint",
                    range = 0f..100f,
                    sliderValue = tintSliderValue,
                    onValueChange = { progress ->
                        tintSliderValue.floatValue = progress
                        mEffectDataList.forEachIndexed { _, cameraEffect ->
                            if (cameraEffect.effect is EffectWhiteBalance){
                                (cameraEffect.effect as EffectWhiteBalance).setTint(progress)
                            }
                        }
                    }
                )

                val brightnessSliderValue = remember { mutableFloatStateOf(0f) }
                SliderView(
                    name = "Brightness",
                    range = cameraUIState.minBrightness..cameraUIState.maxBrightness,
                    sliderValue = brightnessSliderValue,
                    onValueChange = { progress ->
                        brightnessSliderValue.floatValue = progress
                        (getCurrentCamera() as? CameraUVC)?.setBrightness(progress.toInt())
                    }
                )
                ZoomView(cameraUIState)
                TakePictureView()
            }
        }
    }


    fun refreshGallery2(context: Context, filePath: String) {
        MediaScannerConnection.scanFile(context, arrayOf(filePath), null) { path, uri ->
            // MediaScannerConnection is complete
            XLogger.d("拍照完成:${path}")
        }
    }

    fun refreshGallery(context: Context, filePath: String) {
        val file = File(filePath)
        val uri = Uri.fromFile(file)
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri)
        context.sendBroadcast(intent)
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
                            path?.let {
                                XLogger.d("拍照完成:${path}")
                                refreshGallery2(this@CameraDemoFragment.requireContext(),path)
                            }
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



    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SliderView(name:String ,range:ClosedFloatingPointRange<Float> = 0f..1f,sliderValue:MutableFloatState,onValueChange:(progress:Float)->Unit){
        Text(text = "${name}:${sliderValue.floatValue}", fontSize = 8.sp)
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = sliderValue.floatValue,
            valueRange = range,
            onValueChange = {progress->
                onValueChange(progress)
            },
            onValueChangeFinished = {
            },
            track = {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(color = Color(0xFF0099A1), shape = RoundedCornerShape(7.dp))
                )
            },

            thumb = {
                val shape = CircleShape
                Spacer(
                    modifier = Modifier
                        .size(20.dp)
                        .indication(
                            interactionSource = MutableInteractionSource(),
                            indication = null
                        )
                        .hoverable(interactionSource = MutableInteractionSource())
                        .shadow(6.dp, shape, clip = false)
                        .background(color = Color.White, shape)
                )
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

    fun cropAndScaleRGBA(
        data: ByteArray,
        originalWidth: Int,
        originalHeight: Int,
        cropX: Int,
        cropY: Int,
        cropWidth: Int,
        cropHeight: Int,
        newWidth: Int,
        newHeight: Int
    ): ByteArray {
        val newData = ByteArray(newWidth * newHeight * 4)
        val scaleX = cropWidth.toFloat() / newWidth
        val scaleY = cropHeight.toFloat() / newHeight

        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                // Calculate the corresponding position in the cropped region
                val origX = (x * scaleX).toInt() + cropX
                val origY = (y * scaleY).toInt() + cropY

                // Calculate positions in the byte arrays
                val origPos = (origY * originalWidth + origX) * 4
                val newPos = (y * newWidth + x) * 4

                // Copy RGBA values
                for (i in 0 until 4) {
                    newData[newPos + i] = data[origPos + i]
                }
            }
        }

        return newData
    }
    fun createBitmapFromRGBA(data: ByteArray, width: Int, height: Int): Bitmap {
        // Create a Bitmap from RGBA byte array
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(data))
        }
    }


    fun cropNV21(
        data: ByteArray,
        width: Int,
        height: Int,
        cropX: Int,
        cropY: Int,
        cropWidth: Int,
        cropHeight: Int
    ): ByteArray {
        // Calculate the size of the cropped frame
        val croppedSize = cropWidth * cropHeight * 3 / 2
        val croppedData = ByteArray(croppedSize)

        // Crop Y plane
        for (y in 0 until cropHeight) {
            val srcPos = (cropY + y) * width + cropX
            val dstPos = y * cropWidth
            System.arraycopy(data, srcPos, croppedData, dstPos, cropWidth)
        }

        // Crop UV plane
        val uvHeight = cropHeight / 2
        val uvWidth = cropWidth / 2
        val yPlaneSize = width * height
        val uvPlaneSize = width * height / 2

        for (y in 0 until uvHeight) {
            val srcPos = yPlaneSize + (cropY / 2 + y) * width + cropX
            val dstPos = cropWidth * cropHeight + y * cropWidth
            System.arraycopy(data, srcPos, croppedData, dstPos, cropWidth)
        }

        return croppedData
    }

    fun nv21ToBitmap(data: ByteArray, width: Int, height: Int): Bitmap? {
        // Convert NV21 to YuvImage
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        // Compress YuvImage to JPEG
        val out = ByteArrayOutputStream()
        val rect = Rect(0, 0, width, height)
        yuvImage.compressToJpeg(rect, 100, out)
        // Convert JPEG data to Bitmap
        val jpegData = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
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
                    mEffectDataList.forEachIndexed { _, cameraEffect ->
                        cameraEffect.effect?.let {
//                            if (it is EffectImageLevel){
//                                addRenderEffect(it)
//                            }
//                            if (it is EffectSaturation){
//                                addRenderEffect(it)
//                            }

//                            if (it is EffectSharpness){
//                                addRenderEffect(it)
//                            }

//                            if (it is EffectHue){
//                                addRenderEffect(it)
//                            }

//                            if (it is EffectSharpen){
//                                addRenderEffect(it)
//                            }

                            addRenderEffect(it)
                        }
                    }

                    addPreviewDataCallBack(object :IPreviewDataCallBack{
                        override fun onPreviewData(
                            data: ByteArray?,
                            width: Int,
                            height: Int,
                            format: IPreviewDataCallBack.DataFormat
                        ) {
//                            XLogger.d("新的数据来了------->${format} width:${width} height:${height}")
                            data?.let {
                                val scale = 0.3f
                                val newWidth = (width * scale).toInt()
                                val newHeight = (height * scale).toInt()
                                val newData = cropNV21(
                                    data = data,
                                    cropX = 100,
                                    cropY = 100,
                                    width = width,
                                    height = height,
                                    cropWidth = newWidth,
                                    cropHeight = newHeight
                                )
                                val bitmap = nv21ToBitmap(newData, newWidth, newHeight)
                                bitmap?.let {
                                    lifecycleScope.launch(Dispatchers.Main) {
//                                        whitebalance.setTemperature((2000..8000).random().toFloat())
                                        mViewBinding.imageView.setImageBitmap(it)
                                    }
                                }
                                //RGBA
                            }
                        }
                    })
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