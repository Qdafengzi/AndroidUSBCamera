package com.jiangdg.demo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.hardware.camera2.CameraAccessException
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
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
import com.jiangdg.ausbc.render.effect.EffectBrightness
import com.jiangdg.ausbc.render.effect.EffectContrast
import com.jiangdg.ausbc.render.effect.EffectGamma
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
import com.jiangdg.demo.encoder.MediaEncoder
import com.jiangdg.demo.encoder.RecordListener
import com.jiangdg.utils.ResUtils
import com.jiangdg.utils.XLogger
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer
import kotlin.math.min


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

    fun updateZoom(zoom: Float) {
        _cameraUIState.update {
            it.copy(zoom = zoom)
        }
    }

}

open class CameraDemoFragment : CameraFragment() {
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
        val request = CameraRequest.Builder()
            .setPreviewWidth(3840) // camera preview width
            .setPreviewHeight(2160) // camera preview height
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
        val surfaceView = AspectRatioSurfaceView(requireContext())
        val width = ResUtils.dp2px(this.requireContext(),1080f)
        val height = ResUtils.dp2px(this.requireContext(),1080f)


        surfaceView.holder.addCallback(object :SurfaceHolder.Callback{
            override fun surfaceCreated(holder: SurfaceHolder) {

            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                XLogger.d("surfaceChanged:${width}*${height}")
//                surfaceSizeChanged(width, height)

//                surfaceView.setAspectRatio(2160,2160)

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

        })
        return surfaceView


//        val textureView = AspectRatioTextureView(requireContext())
//        val width = ResUtils.dp2px(this.requireContext(),1080f)
//        val height = ResUtils.dp2px(this.requireContext(),1080f)
//        textureView.surfaceTextureListener = object :SurfaceTextureListener{
//            override fun onSurfaceTextureAvailable(
//                surface: SurfaceTexture,
//                width: Int,
//                height: Int
//            ) {
//
//            }
//
//            override fun onSurfaceTextureSizeChanged(
//                surface: SurfaceTexture,
//                width: Int,
//                height: Int
//            ) {
//                updatePreview(textureView)
//            }
//
//            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                return true
//            }
//
//            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//
//            }
//
//        }
//        return textureView


//        return AspectRatioSurfaceView(requireContext())
    }


    private fun updatePreview(textureView: TextureView) {
        try {
            // 设置裁切变换
            val matrix: Matrix = Matrix()
            val viewRect = RectF(0f, 0f, textureView.width.toFloat(), textureView.height.toFloat())
            val bufferRect = RectF(0f, 0f, 3840f, 2160f)
            val centerX = viewRect.centerX()
            val centerY = viewRect.centerY()

            // 计算裁切区域（保持1:1比例）
            val size = min(3840.0, 2160.0).toInt()
            bufferRect[((3840 - size) / 2).toFloat(), ((2160 - size) / 2).toFloat(), ((3840 + size) / 2).toFloat()] =
                ((2160 + size) / 2).toFloat()

            matrix.setRectToRect(bufferRect, viewRect, Matrix.ScaleToFit.CENTER)
            matrix.postRotate(90f, centerX, centerY)
            textureView.setTransform(matrix)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun surfaceSizeChanged(surfaceWidth: Int, surfaceHeight: Int) {
        getCurrentCamera()?.setRenderSize(surfaceWidth, surfaceHeight)
    }

    val viewModel = CameraViewModel()

     protected val gpuImageMovieWriter by lazy {
         GPUImageMovieWriter()
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

            CameraEffect(
                EffectGamma.ID,
                "Gamma",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectGamma(requireActivity()),
                coverResId = R.mipmap.filter0
            ),

            CameraEffect(
                EffectBrightness.ID,
                "Brightness",
                CameraEffect.CLASSIFY_ID_FILTER,
                effect = EffectBrightness(requireActivity()),
                coverResId = R.mipmap.filter0
            ),

//            CameraEffect(
//                EffectCrop.ID,
//                "EffectCrop",
//                CameraEffect.CLASSIFY_ID_FILTER,
//                effect = EffectCrop(requireActivity()),
//                coverResId = R.mipmap.filter0
//            ),
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

        val filterGroup = GPUImageFilterGroup()
        filterGroup.addFilter(gpuImageMovieWriter)
        mViewBinding.imageView.filter = filterGroup
        gpuImageMovieWriter.setFrameRate(30)
        mViewBinding.imageView.setRenderMode(GPUImageView.RENDERMODE_CONTINUOUSLY)
        gpuImageMovieWriter.gpuImageErrorListener = GPUImageMovieWriter.GPUImageErrorListener { XLogger.d("渲染错误：") }

        mViewBinding.compose.setContent {
            val scrollState = rememberScrollState()
            // Create a nested scroll connection to handle nested scroll scenarios
            val nestedScrollConnection = remember {
                object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {}
            }


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = scrollState, enabled = true)
                    .nestedScroll(connection = nestedScrollConnection)
                    .background(color = Color.White),
            ) {
                Filters1()
                TakePictureView()
            }
        }
    }

    @Composable
    fun Filters1() {
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
                    if (cameraEffect.effect is EffectHue) {
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
                    if (cameraEffect.effect is EffectSharpen) {
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
                    if (cameraEffect.effect is EffectImageLevel) {
                        (cameraEffect.effect as EffectImageLevel).setMin(
                            progress,
                            1.0f,
                            0.62f
                        )
                    }
                }
            }
        )


        val saturationSliderValue = remember { mutableFloatStateOf(1f) }
        SliderView(
            name = "Saturation",
            range = 0f..2f,
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
                    if (cameraEffect.effect is EffectWhiteBalance) {
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
                    if (cameraEffect.effect is EffectWhiteBalance) {
                        (cameraEffect.effect as EffectWhiteBalance).setTint(progress)
                    }
                }
            }
        )

        val brightnessSliderValue = remember { mutableFloatStateOf(0f) }
        SliderView(
            name = "Brightness  by Camera",
            range = cameraUIState.minBrightness..cameraUIState.maxBrightness,
            sliderValue = brightnessSliderValue,
            onValueChange = { progress ->
                brightnessSliderValue.floatValue = progress
                (getCurrentCamera() as? CameraUVC)?.setBrightness(progress.toInt())
            }
        )

        val brightness2SliderValue = remember { mutableFloatStateOf(1f) }
        SliderView(
            name = "Brightness by filter",
            range = -1f..1f,
            sliderValue = brightness2SliderValue,
            onValueChange = { progress ->
                brightness2SliderValue.floatValue = progress
                mEffectDataList.forEachIndexed { _, cameraEffect ->
                    if (cameraEffect.effect is EffectBrightness) {
                        (cameraEffect.effect as EffectBrightness).setBrightness(progress)
                    }
                }
            }
        )

        val gammaSliderValue = remember { mutableFloatStateOf(1f) }
        SliderView(
            name = "Gamma",
            range = 0.0f..3f,
            sliderValue = gammaSliderValue,
            onValueChange = { progress ->
                gammaSliderValue.floatValue = progress
                //(getCurrentCamera() as? CameraUVC)?.setGamma(progress.toInt())
                mEffectDataList.forEachIndexed { _, cameraEffect ->
                    if (cameraEffect.effect is EffectGamma) {
                        (cameraEffect.effect as EffectGamma).setGamma(progress)
                    }
                }
            }
        )

//                val zoomSliderValue = remember { mutableFloatStateOf(1f) }
//                SliderView(
//                    name = "Zoom",
//                    range = 1f..10f,
//                    sliderValue = zoomSliderValue,
//                    onValueChange = { progress ->
//                        zoomSliderValue.floatValue = progress
//                        (getCurrentCamera() as? CameraUVC)?.setZoom(progress.toInt())
//                    }
//                )

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
        val scope = rememberCoroutineScope()

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                var bitmap  by  remember {
                    mutableStateOf<Bitmap?>(null)
                }

                Button(
                    onClick = {
//                    mEffectDataList.forEachIndexed { _, cameraEffect ->
//                        if (cameraEffect.effect is EffectCrop) {
//                            (cameraEffect.effect as EffectCrop).setAspectRatio(1f,1f)
//                        }
//                    }
//                    (getCurrentCamera() as? CameraUVC)?.captureImage(object : ICaptureCallBack {
//                        override fun onBegin() {
//                            XLogger.e("拍照 开始---- }")
//                        }
//
//                        override fun onError(error: String?) {
//                            XLogger.e("拍照 错误----》${error}")
//                        }
//
//                        override fun onComplete(path: String?) {
//                            path?.let {
//                                XLogger.d("拍照 完成:${path}")
//                                refreshGallery2(this@CameraDemoFragment.requireContext(), path)
//                            }
//                        }
//                    })
                        val bit = mViewBinding.imageView.capture()
                        bitmap= bit



                    }) {
                    Text("拍照")
                }

                if (bitmap != null) {
                    Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = null,
                        modifier = Modifier.widthIn(
                        max=100.dp
                    ),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }

            RecordButton()
        }
    }

    private fun createFolder() {
        val folder =
            File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)!!.path + "/.media")

        if (!folder.exists()) folder.mkdirs()
    }
    private fun createFile(ext: String = ".jpg"): File {
        return File(
            requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)!!.path
                    + "/.media" + "/"
                    + System.currentTimeMillis()
                    + ext
        )
    }

    @Composable
    fun RecordButton() {
        val scope = rememberCoroutineScope()

        Button(onClick = {
//                (getCurrentCamera() as? CameraUVC)?.captureVideoStart(object : ICaptureCallBack {
//                    override fun onBegin() {
//
//                    }
//
//                    override fun onError(error: String?) {
//
//                    }
//
//                    override fun onComplete(path: String?) {
//
//                    }
//                }, path = "")
            scope.launch(Dispatchers.IO) {
//                createFolder()
//                val file = createFile(".mp4")

                val videoFile = File(requireContext().cacheDir, "${System.currentTimeMillis()}.mp4")
                videoFile.createNewFile()
                gpuImageMovieWriter.apply {
                    drawVideo = true
                    prepareRecording(
                        videoFile.absolutePath,
                        mViewBinding.imageView.measuredWidth,
                        mViewBinding.imageView.measuredHeight
                    )
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
            gpuImageMovieWriter.stopRecording { XLogger.d("暂停了") }
        }) {
            Text("暂停")
        }
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
//                    setSaturation()
//                    setSharpness()
//                    setGain()
//                    setGamma()
//                    setContrast()
//                    setHue()

                    val previewSize = getSuitableSize(2160, 2160)
                    XLogger.d("获取最佳的预览尺寸：${previewSize.width} * ${previewSize.height}")
                    val isSupport = isPreviewSizeSupported(previewSize)
                    XLogger.d("是否支持：${isSupport}")
                    val allPreviewSize = getAllPreviewSizes()

                    allPreviewSize.forEach {
                        XLogger.d("各种分辨率：${it.width} * ${it.height}")
                    }
//                    updateResolution(1080,1920)
//                    setRenderSize()


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

//                            addRenderEffect(it)
                        }
                    }


//                    updateResolution(1920,1920)
                    setEncodeDataCallBack(object : IEncodeDataCallBack {
                        override fun onEncodeData(
                            type: IEncodeDataCallBack.DataType,
                            buffer: ByteBuffer,
                            offset: Int,
                            size: Int,
                            timestamp: Long
                        ) {
                            XLogger.d("编码:${type.name}")
                        }
                    })

                    addPreviewDataCallBack(object : IPreviewDataCallBack {
                        override fun onPreviewData(
                            data: ByteArray?,
                            width: Int,
                            height: Int,
                            format: IPreviewDataCallBack.DataFormat
                        ) {
//                            XLogger.d("新的数据来了------->${format} ${data?.size} width:${width} height:${height}")
                            data?.let {
                                mViewBinding.imageView.updatePreviewFrame(data,width,height)
                                return@let
                                val scale = 0.5f
                                val newWidth = (width * scale).toInt()
                                val newHeight = (height * scale).toInt()
                                val newData = cropNV21(
                                    data = data,
                                    cropX = (width * (1 - scale) / 2f).toInt(),
                                    cropY = (height * (1 - scale) / 2f).toInt(),
                                    width = width,
                                    height = height,
                                    cropWidth = newWidth,
                                    cropHeight = newHeight
                                )
                                mViewBinding.imageView.updatePreviewFrame(newData,newWidth,newHeight)
                            }
                        }
                    })
                    setEncodeDataCallBack(object : IEncodeDataCallBack {
                        override fun onEncodeData(
                            type: IEncodeDataCallBack.DataType,
                            buffer: ByteBuffer,
                            offset: Int,
                            size: Int,
                            timestamp: Long
                        ) {
                            logWithInterval("数据来了：${size} $timestamp")
                        }
                    })
                    val min = getBrightnessMin() ?: 0
                    val max = getBrightnessMax() ?: 100
                    val zoom = (getCurrentCamera() as? CameraUVC)?.getZoom() ?: 0f
                    viewModel.updateZoom(zoom.toFloat())
                    viewModel.updateBrightness(min, max)
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

    var lastPrintTime = System.currentTimeMillis()

    fun logWithInterval(text: String) {
        if ((System.currentTimeMillis() - lastPrintTime) > 1000) {
            XLogger.d(text)
            lastPrintTime = System.currentTimeMillis()
        }
    }
}