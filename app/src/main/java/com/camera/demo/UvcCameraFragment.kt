package com.camera.demo

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.camera.demo.databinding.FragmentDemo01Binding
import com.camera.utils.XLogger
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.base.CameraFragment
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.IEncodeDataCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.CameraUVC
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.render.env.RotateType
import com.jiangdg.ausbc.widget.AspectRatioSurfaceView
import com.jiangdg.ausbc.widget.IAspectRatio
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLevelsFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer


data class UvcCameraUIState(
    val autoWhiteBalance: Boolean = true,
    val autoFocus: Boolean = true,
    val contrastMin: Float = 0f,
    val contrastMax: Float = 0f,
    val contrast: Float = 0f,
    val sharpness: Float = 0f,
    val sharpnessMax: Float = 0f,
    val sharpnessMin: Float = 0f,
    val hue: Float = 0f,
    val hueMax: Float = 0f,
    val hueMin: Float = 0f,
    val gain: Float = 0f,
    val gainMax: Float = 0f,
    val gainMin: Float = 0f,
    val gamma: Float = 0f,
    val gammaMax: Float = 0f,
    val gammaMin: Float = 0f,
    val saturation: Float = 0f,
    val saturationMax: Float = 0f,
    val saturationMin: Float = 0f,
    val brightness: Float = 0f,
    val brightnessMax: Float = 0f,
    val brightnessMin: Float = 0f,
    val zoom: Float = 1f,
    val zoomMax: Float = 1f,
    val zoomMin: Float = 1f,
    val exposure: Float = 1f,
    val exposureMax: Float = 1f,
    val exposureMin: Float = 1f,
    val focus: Float = 1f,
    val focusMax: Float = 1f,
    val focusMin: Float = 1f,

)

class UvcCameraViewModel : ViewModel() {
    private val _cameraUIState = MutableStateFlow(UvcCameraUIState())
    val cameraUIState = _cameraUIState.asStateFlow()
    fun updateParams(state :UvcCameraUIState){
        _cameraUIState.update {
            state
        }
    }

}

open class UvcCameraFragment : CameraFragment() {

    var mRenderWidth = 1920
    var mRenderHeight = 1920

    private val mLifecycleOwner by lazy {
        lifecycleScope
    }
    private val mViewModel = UvcCameraViewModel()

    private val mGpuImageMovieWriter by lazy {
        GPUImageMovieWriter()
    }
    private val mGPUImageWhiteBalanceFilter by lazy{
        GPUImageWhiteBalanceFilter()
    }

    private val mGPUImageBrightnessFilter by lazy{
        GPUImageBrightnessFilter()
    }

    private val mGpuImageLevelsFilter by lazy {
        GPUImageLevelsFilter()
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

//        val layoutParams = mViewBinding.imageView.layoutParams as ConstraintLayout.LayoutParams
//        layoutParams.width = (ResUtils.screenWidth * 300 / 375f).toInt()
//        layoutParams.height = ResUtils.screenWidth
//        mViewBinding.imageView.layoutParams = layoutParams

        mViewBinding.imageView.setRenderMode(GPUImageView.RENDERMODE_WHEN_DIRTY)
        val gpuImageFilters = mutableListOf<GPUImageFilter>()
        gpuImageFilters.add(mGpuImageMovieWriter)
        gpuImageFilters.add(mGpuImageLevelsFilter)
        gpuImageFilters.add(mGPUImageWhiteBalanceFilter)
        gpuImageFilters.add(mGPUImageBrightnessFilter)

        mViewBinding.imageView.filter = GPUImageFilterGroup(gpuImageFilters)
        mViewBinding.imageView.setDrawVideoListener {
            mGpuImageMovieWriter.drawVideo = true
        }
        mGpuImageMovieWriter.setFrameRate(30)

        mGpuImageMovieWriter.gpuImageErrorListener =
            GPUImageMovieWriter.GPUImageErrorListener { XLogger.d("渲染错误：") }

        setContent()

    }

    private fun setContent(){
        mViewBinding.compose.setContent {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = scrollState, enabled = true)
                    .background(color = Color.White),
            ) {
                FiltersView()
                ButtonView()
                Row(){
                    ResetView()
                    AutoBalanceView()
                }
                DeviceInfoView()
            }
        }
    }

    @Composable
    fun AutoBalanceView() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
        var autoWhiteBalance by remember { mutableStateOf(cameraUIState.autoWhiteBalance) }
        Row(verticalAlignment = Alignment.CenterVertically) {
           Text("AutoWhiteBalance")
           Switch(checked = autoWhiteBalance,onCheckedChange = {
               autoWhiteBalance = it
               (getCurrentCamera() as? CameraUVC)?.setAutoWhiteBalance(it)
           })
       }
    }

    @Composable
    fun ResetView() {
        val scope = rememberCoroutineScope()
        Button(onClick = {
            scope.launch {
                val camera = (getCurrentCamera() as? CameraUVC)
                camera?.apply {
                    resetGamma()
                    resetHue()
                    resetContrast()
                    resetSharpness()
                    resetSaturation()
                    resetBrightness()
                    resetZoom()
                    resetGain()
                    resetAutoFocus()
                    //camera.setAutoFocus(false)
                }
            }
        }) {
            Text("Reset")
        }
    }

    @Composable
    fun DeviceInfoView() {
        val scope = rememberCoroutineScope()
        var deviceInfo by  remember { mutableStateOf("") }
        Column {
            Button(onClick = {
                scope.launch {
                    val camera = (getCurrentCamera() as? CameraUVC)
                    camera?.let {
                        val usbManager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
                        val usbDeviceConnection: UsbDeviceConnection = usbManager.openDevice(camera.device)
                        val info = "info：" +
                                "\nmanufacturerName:${camera.device.manufacturerName}" +
                                "\ndeviceId:${camera.device.deviceId}" +
                                "\ndeviceName:${camera.device.deviceName}" +
                                "\ndeviceClass:${camera.device.deviceClass}" +
                                "\ndeviceSubclass:${camera.device.deviceSubclass}" +
                                "\ndeviceProtocol:${camera.device.deviceProtocol}" +
                                "\nserialNumber:${camera.device.serialNumber}" +
                                "\nproductId:${camera.device.productId}" +
                                "\nproductName:${camera.device.productName}" +
                                "\nversion:${camera.device.version}" +
                                "\nvendorId:${camera.device.vendorId}"
                        deviceInfo = info
                    }
                }
            }) {
                Text("info")
            }
            Text(deviceInfo)
        }
    }

    override fun getCameraRequest(): CameraRequest {
        /**
         *  3840 * 2160
         *  3840 * 2880
         *  3264 * 2448
         *  2592 * 1944
         *  2048 * 1536
         *
         *  1600 * 1200
         *  1920 * 1080
         *  1280 * 960
         *  1280 * 720
         *  1024 * 768
         *  800 * 600
         *  640 * 480
         *  640 * 360
         *  720 * 576
         */
//        val width = ResUtils.dp2px(this.requireContext(),3840f)
//        val height = ResUtils.dp2px(this.requireContext(),2880f)
        //ResUtils.screenWidth ,(ResUtils.screenWidth *9f/16f).toInt()

        //三星的 1920* 1080 30fps
        //3840*2880 16FPS
        val request = CameraRequest.Builder()
//            .setPreviewWidth(1920) // camera preview width
//            .setPreviewHeight(1080) // camera preview height
//            .setPreviewWidth(720) // camera preview width
//            .setPreviewHeight(480) // camera preview height
            .setRenderMode(CameraRequest.RenderMode.OPENGL) // camera render mode
            .setDefaultRotateType(RotateType.ANGLE_0) // rotate camera image when opengl mode
            .setAudioSource(CameraRequest.AudioSource.SOURCE_AUTO) // set audio source
//            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_YUYV) // //todo:YUYV格式的很卡
            .setPreviewFormat(CameraRequest.PreviewFormat.FORMAT_MJPEG) // set preview format, MJPEG recommended
            .setAspectRatioShow(true) // aspect render,default is true
            .setCaptureRawImage(true) // capture raw image picture when opengl mode
            .setRawPreviewData(true)  // preview raw image when opengl mode
//            .setDefaultEffect(Effect)
            .create()

        return request

    }

    override fun getCameraView(): IAspectRatio {
        val view = AspectRatioSurfaceView(requireContext())
        view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                XLogger.d("AspectRatioSurfaceView------> surfaceCreated")
//                holder.setFormat(PixelFormat.TRANSPARENT)
//                drawEmpty(holder)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                XLogger.d("AspectRatioSurfaceView------> surfaceChanged")
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                XLogger.d("AspectRatioSurfaceView------> surfaceDestroyed")
            }
        })
        return view
    }

    private fun drawEmpty(holder: SurfaceHolder) {
        val canvas = holder.lockCanvas()
        canvas?.let {
            it.drawColor(Color.Transparent.toArgb()) // Clear with a transparent color
            holder.unlockCanvasAndPost(it)
        }
    }


    @Composable
    fun FiltersView() {
        XLogger.d("Filters2 刷新")
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value

        val focusValue = remember { mutableFloatStateOf(cameraUIState.focus) }
        SliderView(
            name = "Focus",
            range = cameraUIState.focusMin..cameraUIState.focusMax,
            sliderValue = focusValue,
            onValueChange = { progress ->
                focusValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setFocus(progress.toInt())
            }
        )



        // TODO：无作用
        val exposureValue = remember { mutableFloatStateOf(cameraUIState.exposure) }
        SliderView(
            name = "Exposure",
            range = cameraUIState.exposureMin..cameraUIState.exposureMax,
            sliderValue = exposureValue,
            onValueChange = { progress ->
                exposureValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setExposure(progress.toInt())
            }
        )

        val contrastSliderValue = remember { mutableFloatStateOf(cameraUIState.contrast) }
        SliderView(
            name = "Contrast",
            range = cameraUIState.contrastMin..cameraUIState.contrastMax,
            sliderValue = contrastSliderValue,
            onValueChange = { progress ->
                contrastSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setContrast(progress.toInt())
            }
        )


        val hueSliderValue = remember { mutableFloatStateOf(cameraUIState.hue) }
        SliderView(
            name = "Hue",
            range = cameraUIState.hueMin..cameraUIState.hueMax,
            sliderValue = hueSliderValue,
            onValueChange = { progress ->
                hueSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setHue(progress.toInt())
            }
        )

        // TODO：无作用
        val sharpnessSliderValue = remember { mutableFloatStateOf(cameraUIState.sharpness) }
        SliderView(
            name = "Sharpness",
            range = cameraUIState.sharpnessMin..cameraUIState.sharpnessMax,
            sliderValue = sharpnessSliderValue,
            onValueChange = { progress ->
                sharpnessSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setSharpness(progress.toInt())
            }
        )

        val toneSliderValue = remember { mutableFloatStateOf(0f) }
        SliderView(
            name = "Tone",
            range = -0.3f..0.3f,
            sliderValue = toneSliderValue,
            onValueChange = { progress ->
                toneSliderValue.floatValue = progress
                mGpuImageLevelsFilter.setMin(
                    progress,
                    1f, 1f, 0f, 1f
                )
            }
        )


        val saturationSliderValue = remember { mutableFloatStateOf(cameraUIState.saturation) }
        SliderView(
            name = "Saturation",
            range = cameraUIState.saturationMin..cameraUIState.saturationMax,
            sliderValue = saturationSliderValue,
            onValueChange = { progress ->
                saturationSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setSaturation(progress.toInt())
            }
        )

        val temperatureSliderValue = remember { mutableFloatStateOf(5000f) }
        SliderView(
            name = "temperature",
            range = 2000f..9000f,
            sliderValue = temperatureSliderValue,
            onValueChange = { progress ->
                temperatureSliderValue.floatValue = progress
                mGPUImageWhiteBalanceFilter.setTemperature(progress)
            }
        )

        val tintSliderValue = remember { mutableFloatStateOf(50f) }
        SliderView(
            name = "tint",
            range = 0f..100f,
            sliderValue = tintSliderValue,
            onValueChange = { progress ->
                tintSliderValue.floatValue = progress
                mGPUImageWhiteBalanceFilter.setTint(progress)
            }
        )

        val brightnessSliderValue = remember { mutableFloatStateOf(cameraUIState.brightness) }
        SliderView(
            name = "Brightness  by Camera",
            range = cameraUIState.brightnessMin..cameraUIState.brightnessMax,
            sliderValue = brightnessSliderValue,
            onValueChange = { progress ->
                brightnessSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setBrightness(progress.toInt())
            }
        )

        val brightness2SliderValue = remember { mutableFloatStateOf(0f) }
        SliderView(
            name = "Brightness by filter",
            range = -1f..1f,
            sliderValue = brightness2SliderValue,
            onValueChange = { progress ->
                brightness2SliderValue.floatValue = progress
                mGPUImageBrightnessFilter.setBrightness(progress)
            }
        )

        val gammaSliderValue = remember { mutableFloatStateOf(cameraUIState.gamma) }
        SliderView(
            name = "Gamma",
            range = cameraUIState.gammaMin..cameraUIState.gammaMax,
            sliderValue = gammaSliderValue,
            onValueChange = { progress ->
                gammaSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setGamma(progress.toInt())

            }
        )

        val zoomSliderValue = remember { mutableFloatStateOf(cameraUIState.zoom) }
        SliderView(
            name = "Zoom",
            range = cameraUIState.zoomMin..cameraUIState.zoomMax,
            sliderValue = zoomSliderValue,
            onValueChange = { progress ->
                zoomSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setZoom(progress.toInt())
            }
        )

    }


    fun refreshGallery2(context: Context, filePath: String) {
        MediaScannerConnection.scanFile(context, arrayOf(filePath), null) { path, uri ->
            // MediaScannerConnection is complete
            XLogger.d("拍照完成:${path}")
        }
    }

    @Composable
    fun ButtonView() {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TakePhotoView()
            RecordButton()
        }
    }

    @Composable
    fun TakePhotoView() {
        Column {
            var bitmap by remember {
                mutableStateOf<Bitmap?>(null)
            }

            Button(
                onClick = {
                    val bit = mViewBinding.imageView.capture()
                    bitmap = bit
                }) {
                Text("拍照")
            }

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.widthIn(
                        max = 100.dp
                    ),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }



    @Composable
    fun RecordButton() {
        val scope = rememberCoroutineScope()
        var isRecording by remember { mutableStateOf(false) }
        var recordingTime by remember { mutableIntStateOf(0) }
        val timerActive = remember { mutableStateOf(false) }

        // 使用 LaunchedEffect 启动计时器
        LaunchedEffect(timerActive.value) {
            if (timerActive.value) {
                while (timerActive.value) {
                    delay(1000)
                    recordingTime++
                }
            }
        }

        fun startRecording() {
            XLogger.d("录制开始了")
            isRecording = true
            recordingTime = 0 // 重置录制时间
            timerActive.value = true
            scope.launch {
                // 在 IO Dispatcher 中进行文件操作
                withContext(Dispatchers.IO) {
                    val videoFile = File(requireContext().cacheDir, "record.mp4")
                    try {
                        if (videoFile.exists()) {
                            videoFile.delete()
                        }
                        videoFile.createNewFile()
                    } catch (e: Exception) {
                        XLogger.d("录制失败: ${e.message}")
                    }
                    mGpuImageMovieWriter.apply {
                        drawVideo = true
                        prepareRecording(
                            videoFile.absolutePath,
                            mRenderWidth,
                            mRenderHeight,
                        )
                        delay(200)
                        startRecording(object : GPUImageMovieWriter.StartRecordListener {
                            override fun onRecordStart() {
                                XLogger.d("录制-------->onRecordStart")
                            }

                            override fun onRecordError(e: Exception?) {
                                XLogger.d("录制-------->onRecordError:${e?.message}")
                            }
                        })
                    }
                }
            }
        }

        fun stopRecording() {
            if (isRecording) {
                isRecording = false
                timerActive.value = false
                scope.launch {
                    withContext(Dispatchers.IO) {
                        mGpuImageMovieWriter.stopRecording {
                            XLogger.d("录制已停止")
                            val videoFile = File(requireContext().cacheDir, "record.mp4")
                            getVideoFrameRate(videoFile.absolutePath)
                        }
                    }
                }
            }
        }

        Column {
            Button(onClick = {
                if (!isRecording) {
                    // 开始录制
                    startRecording()
                } else {
                    // 停止录制
                    stopRecording()
                }
            }) {
                Text(if (isRecording) "$recordingTime 秒" else "开始录制")
            }


        }
    }

    fun getVideoFrameRate(videoFilePath: String?) {
        val extractor = MediaExtractor()
        try {
            // 设置数据源
            extractor.setDataSource(videoFilePath!!)
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime!!.startsWith("video/")) {
                    // 获取帧率
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        val frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                       XLogger.d( "Frame Rate: $frameRate fps")
                    } else {
                        XLogger.d( "Frame Rate information not available.")
                    }
                    break // 找到视频轨道后无需继续
                }
            }
        } catch (e: java.lang.Exception) {
            XLogger.d( "Error extracting video info"+ e.message)
        } finally {
            extractor.release()
        }
    }



    @Composable
    fun RecordButton1() {
        val scope = rememberCoroutineScope()


        XLogger.d("RecordButton")
        Button(onClick = {
            scope.launch(Dispatchers.IO) {
                val videoFile = File(requireContext().cacheDir, "record.mp4")
                if (videoFile.exists()) {
                    videoFile.delete()
                }
                videoFile.createNewFile()
                mGpuImageMovieWriter.apply {
                    drawVideo = true
                    prepareRecording(
                        videoFile.absolutePath,
                        mRenderWidth,
                        mRenderHeight,
                    )
                    delay(200)
                    startRecording(object : GPUImageMovieWriter.StartRecordListener {
                        override fun onRecordStart() {
                            XLogger.d("录制-------->onRecordStart")
                        }

                        override fun onRecordError(e: Exception?) {
                            XLogger.d("录制-------->onRecordError:${e?.message}")
                        }
                    })
                }
            }
        }) {
            Text("录像")
        }

        Button(onClick = {
            scope.launch(Dispatchers.IO) {
                mGpuImageMovieWriter.stopRecording {
                    XLogger.d("暂停了")
                }
            }

        }) {
            Text("暂停")
        }
    }


    override fun onCameraState(self: MultiCameraClient.ICamera, code: ICameraStateCallBack.State, msg: String?) {
        val camera = (getCurrentCamera() as? CameraUVC)
        when (code) {
            ICameraStateCallBack.State.OPENED -> {
//                mViewBinding.imageView.onResume()
                XLogger.d("mCameraClient 相机打开-----》")
                camera?.apply {
//                    camera.sendCameraCommand()

                    setAutoFocus(false)//画面不晃动
                    setAutoWhiteBalance(true)

                    val autoWhiteBalance = getAutoWhiteBalance()?:true
                    val autoFocus = getAutoFocus()?:true
                    val contrastMin = getContrastMin()?.toFloat()?:0f
                    val contrastMax = getContrastMax()?.toFloat()?:0f
                    val contrast = getContrast()?.toFloat()?:0f
                    val sharpness = getSharpness()?.toFloat()?:0f
                    val sharpnessMax = getSharpnessMax()?.toFloat()?:0f
                    val sharpnessMin = getSharpnessMin()?.toFloat()?:0f
                    val hue = getHue()?.toFloat()?:0f
                    val hueMax = getHueMax()?.toFloat()?:0f
                    val hueMin = getHueMin()?.toFloat()?:0f
                    val gain = getGain()?.toFloat()?:0f
                    val gainMax = getGainMax()?.toFloat()?:0f
                    val gainMin = getGainMin()?.toFloat()?:0f
                    val gamma = getGamma()?.toFloat()?:0f
                    val gammaMax = getGammaMax()?.toFloat()?:0f
                    val gammaMin = getGammaMin()?.toFloat()?:0f
                    val saturation = getSaturation()?.toFloat()?:0f
                    val saturationMax = getSaturationMax()?.toFloat()?:0f
                    val saturationMin = getSaturationMin()?.toFloat()?:0f
                    val brightness = getBrightness()?.toFloat()?:0f
                    val brightnessMax = getBrightnessMax()?.toFloat()?:0f
                    val brightnessMin = getBrightnessMin()?.toFloat()?:0f
                    val zoom = getZoom()?.toFloat()?:1f
                    val zoomMax = getZoomMax()?.toFloat()?:1f
                    val zoomMin = getZoomMin()?.toFloat()?:1f
                    val exposure = getExposure()?.toFloat()?:1f
                    val exposureMax = getExposureMax()?.toFloat()?:1f
                    val exposureMin = getExposureMin()?.toFloat()?:1f
                    val focus = getFocus()?.toFloat()?:1f
                    val focusMax = getFocusMax()?.toFloat()?:1f
                    val focusMin = getFocusMin()?.toFloat()?:1f

                    val uiState = UvcCameraUIState(
                        autoWhiteBalance = autoWhiteBalance,
                        autoFocus = autoFocus,
                        zoom = zoom,
                        zoomMax = zoomMax,
                        zoomMin = zoomMin,
                        brightness = brightness,
                        brightnessMax = brightnessMax,
                        brightnessMin = brightnessMin,
                        sharpness = sharpness,
                        sharpnessMax = sharpnessMax,
                        sharpnessMin = sharpnessMin,
                        contrast = contrast,
                        contrastMax = contrastMax,
                        contrastMin = contrastMin,
                        hue = hue,
                        hueMax = hueMax,
                        hueMin = hueMin,
                        saturation = saturation,
                        saturationMax = saturationMax,
                        saturationMin = saturationMin,
                        gain = gain,
                        gainMax = gainMax,
                        gainMin = gainMin,
                        gamma = gamma,
                        gammaMax = gammaMax,
                        gammaMin = gammaMin,
                        exposureMax = exposureMax,
                        exposureMin = exposureMin,
                        exposure = exposure,
                        focusMax = focusMax,
                        focusMin = focusMin,
                        focus = focus
                    )

                    XLogger.d("参数:${uiState}")
                    mViewModel.updateParams(state = uiState)

                    val allPreviewSize = getAllPreviewSizes()
                    val maxResolution = allPreviewSize.maxByOrNull { it.width * it.height }
                    XLogger.d("最大的分辨率：${maxResolution?.width} * ${maxResolution?.height}")
                    maxResolution?.let {
                        //todo:如何更新预览尺寸
//                        this.setRenderSize(it.width,it.height)
//                        updateResolution(it.width,it.height)
                    }
                    setEncodeDataCallBack(object : IEncodeDataCallBack {
                        override fun onEncodeData(type: IEncodeDataCallBack.DataType, buffer: ByteBuffer, offset: Int, size: Int, timestamp: Long) {
                            XLogger.d("编码:${type.name}")
                        }
                    })

                    addPreviewDataCallBack(object : IPreviewDataCallBack {
                        override fun onPreviewData(data: ByteArray?, width: Int, height: Int, format: IPreviewDataCallBack.DataFormat) {
//                            XLogger.d("新的数据来了------->${format} ${data?.size} width:${width} height:${height}")
                            mLifecycleOwner.launch(Dispatchers.IO) {
                                data?.let {
                                    mViewBinding.imageView.updatePreviewFrame(
                                        data,
                                        width,
                                        height
                                    )
                                    //省点模式
                                    mViewBinding.imageView.requestRender()
                                    return@launch


//                                    val newData = ByteArray(width *height * 3 / 2) // 足够大的缓冲区
//                                    val newDimensions = IntArray(2)
//                                    // 调用native方法
//                                    YUVUtils.cropNv21(
//                                        data,
//                                        width,
//                                        height,
//                                        300/375f,
//                                        newData,
//                                        newDimensions
//                                    )
//                                    newData?.let {
//                                        logWithInterval("new data------->${format} ${newData.size} width:${newDimensions[0]} height:${newDimensions[1]}")
//                                        if (mRenderWidth != newDimensions[0]) {
//                                            mRenderWidth = newDimensions[0]
//                                        }
//                                        if (mRenderHeight != newDimensions[1]) {
//                                            mRenderHeight = newDimensions[1]
//                                        }
//
//                                        mViewBinding.imageView.updatePreviewFrame(
//                                            newData,
//                                            newDimensions[0],
//                                            newDimensions[1]
//                                        )
//                                        //省点模式
//                                        mViewBinding.imageView.requestRender()
//                                    }

                                    val newData = cropNV21(data, width, height, 300 / 375f)
                                    newData?.let {
                                        logWithInterval("new data------->${format} ${newData.data.size} width:${newData.width} height:${newData.height}")
                                        if (mRenderWidth != newData.width) {
                                            mRenderWidth = newData.width
                                        }
                                        if (mRenderHeight != newData.height) {
                                            mRenderHeight = newData.height
                                        }

                                        mViewBinding.imageView.updatePreviewFrame(
                                            newData.data,
                                            newData.width,
                                            newData.height
                                        )
                                        //省点模式
                                        mViewBinding.imageView.requestRender()
                                    }
                                }
                            }
                        }
                    })
                    setEncodeDataCallBack(object : IEncodeDataCallBack {
                        override fun onEncodeData(type: IEncodeDataCallBack.DataType, buffer: ByteBuffer, offset: Int, size: Int, timestamp: Long) {
                            logWithInterval("数据来了：${size} $timestamp")
                        }
                    })
//                    val min = getBrightnessMin() ?: 0
//                    val max = getBrightnessMax() ?: 100
//                    mViewModel.updateZoom(zoom.toFloat())
//                    mViewModel.updateBrightness(min, max)
//                XLogger.d("亮度 最大:${max} 最小：${min} zoom:${zoom}")
                }

            }

            ICameraStateCallBack.State.CLOSED -> {
                // TODO:关闭
                XLogger.d("mCameraClient 相机关闭-----》")
//                camera?.closeCamera()
//                mViewBinding.imageView.onPause()



            }

            ICameraStateCallBack.State.ERROR -> {
                XLogger.d("mCameraClient 相机错误-----》")
                //todo: error
            }
        }
    }



    data class CroppedNV21(
        val data: ByteArray,
        val width: Int,
        val height: Int
    )

    fun cropNV21(
        data: ByteArray?,
        width: Int,
        height: Int,
        aspectRatio: Float
    ): CroppedNV21? {
        if (data == null) return null

        // Calculate the new width and height based on the aspect ratio
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio == 1.0f) {
            // For 1:1 aspect ratio
            newWidth = minOf(width, height)
            newHeight = newWidth
        } else {
            if (width.toFloat() / height > aspectRatio) {
                newHeight = height
                newWidth = (height * aspectRatio).toInt()
            } else {
                newWidth = width
                newHeight = (width / aspectRatio).toInt()
            }
        }

        // Calculate the starting points for cropping
        val cropX = (width - newWidth) / 2
        val cropY = (height - newHeight) / 2

        // Calculate size of the cropped frame
        val croppedSize = newWidth * newHeight * 3 / 2
        val croppedData = ByteArray(croppedSize)

        // Iterate over the height of the crop area
        for (y in 0 until newHeight) {
            // Crop Y plane
            val srcYPos = (cropY + y) * width + cropX
            val dstYPos = y * newWidth
            System.arraycopy(data, srcYPos, croppedData, dstYPos, newWidth)

            // Crop UV plane (only process every second row for UV)
            if (y % 2 == 0) {
                val srcUVPos = width * height + ((cropY / 2) + (y / 2)) * width + cropX
                val dstUVPos = newWidth * newHeight + (y / 2) * newWidth
                System.arraycopy(data, srcUVPos, croppedData, dstUVPos, newWidth)
            }
        }

        return CroppedNV21(croppedData, newWidth, newHeight)
    }

    var lastPrintTime = System.currentTimeMillis()

    fun logWithInterval(text: String) {
        if ((System.currentTimeMillis() - lastPrintTime) > 3000) {
            XLogger.d(text)
            lastPrintTime = System.currentTimeMillis()
        }
    }
}