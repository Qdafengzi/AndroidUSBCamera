package com.camera.demo.uvc_camera

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.camera.demo.GPUImageMovieWriter
import com.camera.demo.SliderView
import com.camera.demo.Utils
import com.camera.demo.databinding.FragmentDemo02Binding
import com.camera.demo.encoder.CameraXListener
import com.camera.demo.encoder.RecordListener
import com.camera.demo.uvc_data.AspectRatioData
import com.camera.demo.uvc_data.ExposureModel
import com.camera.demo.uvc_data.UvcCameraUIState
import com.camera.utils.XLogger
import com.herohan.uvcapp.CameraHelper
import com.herohan.uvcapp.ICameraHelper
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera.UVC_VS_FRAME_MJPEG
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilterGroup
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGammaFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageLevelsFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageWhiteBalanceFilter
import jp.co.cyberagent.android.gpuimage.filter.custom.GPUGrayWorldBalanceFilter
import jp.co.cyberagent.android.gpuimage.filter.custom.GPUImagePerfectReflectorBalanceFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.min


open class UvcCameraFragment : Fragment() {
    private var mCurrentAspectWithP = 1
    private var mCurrentAspectHeightP = 1
    private var mCameraHelper: ICameraHelper? = null
    private var mPreviewSize: Size? = null
    private var size = 1080
    private val mViewModel = UvcCameraViewModel()


    private val mCameraXListener = object : CameraXListener {
        override fun click() {
        }

        override fun recordStart() {
        }

        override fun recordStop() {
        }

        override fun recordPrepareError(msg: String) {
        }

        override fun recordError(msg: String) {
        }

        override fun stopError(msg: String) {
        }

        override fun captureError() {
        }

    }

    private val mGpuImageMovieWriter by lazy {
        GPUImageMovieWriter(mCameraXListener)
    }
    private val mGPUImageWhiteBalanceFilter by lazy {
        GPUImageWhiteBalanceFilter()
    }

    private val mGPUImageBrightnessFilter by lazy {
        GPUImageBrightnessFilter()
    }

    private val mGpuImageLevelsFilter by lazy {
        GPUImageLevelsFilter()
    }

    private val mGPUImageSharpenFilter by lazy {
        GPUImageSharpenFilter()
    }

    private val mGPUImageGammaFilter by lazy {
        GPUImageGammaFilter()
    }

    private val mGPUImageSaturationFilter by lazy {
        GPUImageSaturationFilter()
    }

    private val mGPUImageExposureFilter by lazy {
        GPUImageExposureFilter()
    }

    private val mGPUImageContrastFilter by lazy {
        GPUImageContrastFilter()
    }

    private val mGPUImageHueFilter by lazy {
        GPUImageHueFilter()
    }

    private val mGPUGrayWorldFilter by lazy {
        GPUGrayWorldBalanceFilter()
    }

    private val mGPUImagePerfectReflectorBalanceFilter by lazy {
        GPUImagePerfectReflectorBalanceFilter()
    }

    private val mGPUImageFilters = CopyOnWriteArrayList<GPUImageFilter>()
    private var previewConfigured = false
    private lateinit var mBinding: FragmentDemo02Binding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDemo02Binding.inflate(inflater, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        mBinding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                mCameraHelper?.addSurface(holder.surface, false)
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                XLogger.d("尺寸改变:${width}*${height}")
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                mCameraHelper?.removeSurface(holder.surface)
//            }
//        })
        setFilter()
        setContent()
        initListener()
    }

    private var mScaleFactor = 1.0f
    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    mScaleFactor *= detector.scaleFactor
                    // Limit the scale factor to reasonable bounds
                    mScaleFactor = max(1f, min(mScaleFactor, 15.0f))
                    mBinding.gpuImageView.surfaceView.scaleX = mScaleFactor
                    mBinding.gpuImageView.surfaceView.scaleY = mScaleFactor
                    mGpuImageMovieWriter.scaleFactor = mScaleFactor
                    return true
                }
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListener() {
        mBinding.gpuImageView.surfaceView.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            true
        }

//        val view = mBinding.gpuImageView.surfaceView as GPUImageView.GPUImageGLSurfaceView
//        view.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                XLogger.d("surfaceCreated------>")
//                mCameraHelper?.addSurface(view, false)
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                XLogger.d("尺寸改变:${width}*${height}")
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                mCameraHelper?.removeSurface(view)
//            }
//        })


//        val view = mBinding.gpuImageView.surfaceView as GPUImageView.GPUImageGLTextureView
//        view.addSurfaceTextureListener(object :TextureView.SurfaceTextureListener{
//            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//                mCameraHelper?.addSurface(surface, false)
//            }
//
//            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//                XLogger.d("尺寸改变:${width}*${height}")
//            }
//
//            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                mCameraHelper?.removeSurface(surface)
//                return true
//            }
//
//            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
//                XLogger.d("onSurfaceTextureUpdated----->")
//            }
//
//        })
//        view.holder.addCallback(object : SurfaceHolder.Callback {
//            override fun surfaceCreated(holder: SurfaceHolder) {
//                XLogger.d("surfaceCreated------>")
//                mCameraHelper?.addSurface(view, false)
//            }
//
//            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
//                XLogger.d("尺寸改变:${width}*${height}")
//            }
//
//            override fun surfaceDestroyed(holder: SurfaceHolder) {
//                mCameraHelper?.removeSurface(view)
//            }
//        })

        mBinding.aspectView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                mCameraHelper?.addSurface(surface, false)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                XLogger.d("尺寸改变:${width}*${height}")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                mCameraHelper?.removeSurface(surface)
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap = mBinding.aspectView.bitmap
                    withContext(Dispatchers.Main) {
                        if (!previewConfigured && bitmap != null) {
                            size = if (bitmap.width > 2160) bitmap.width else 2160
                            XLogger.d("size 大小:${size} ${bitmap.width}* ${bitmap.height}")
                            previewConfigured = true
                            setViewRatio(
                                mCurrentAspectWithP,
                                mCurrentAspectHeightP
                            )
                        }
                        bitmap?.let {
                            XLogger.d("bitmap的大小:${bitmap.width}*${bitmap.height}")
                            mBinding.gpuImageView.setImage(bitmap)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        initCameraHelper()
    }

    override fun onStop() {
        super.onStop()
        clearCameraHelper()
    }

    private fun initCameraHelper() {
        if (mCameraHelper == null) {
            mCameraHelper = CameraHelper()
            mCameraHelper?.setStateCallback(mStateListener)
        }
    }

    private fun clearCameraHelper() {
        if (mCameraHelper != null) {
            mCameraHelper?.release()
            mCameraHelper = null
        }
    }

    private fun selectDevice(device: UsbDevice) {
        mCameraHelper?.selectDevice(device)
    }

    fun setViewRatio(ratioWidth: Int, ratioHeight: Int) {
        if (!previewConfigured) {
            XLogger.d("setViewRatio 还未初始化-----》")
            return
        }
//        XLogger.d("setViewRatio========>ratioWidth:${ratioWidth} ratioHeight:${ratioHeight} size:${size}  width:${width} height:${height}")
        mBinding.gpuImageView.let { gpuImageView ->
            var x = -(size - mBinding.container.width) / 2
            var y = -(size - mBinding.container.width) / 2

            val scale = mBinding.container.width.toFloat() / size
            XLogger.d("setViewRatio width${gpuImageView.width} width:${mBinding.container.width}  size：${size} scale${scale}")


            var width = size
            var height = size

            if (ratioWidth > ratioHeight) {
                height = height * ratioHeight / ratioWidth
                y += width / 2 - height / 2
            } else if (ratioWidth < ratioHeight) {
                width = width * ratioWidth / ratioHeight
                x += height / 2 - width / 2
            }

            val params = gpuImageView.layoutParams as FrameLayout.LayoutParams
            params.width = width
            params.height = height
            gpuImageView.layoutParams = params
            gpuImageView.setRenderMode(GPUImageView.RENDERMODE_WHEN_DIRTY)
            gpuImageView.scaleX = scale
            gpuImageView.scaleY = scale
            gpuImageView.x = x.toFloat()
            gpuImageView.y = y.toFloat()

            XLogger.d("setViewRatio========>${width} *${height} scale:${scale} x:${x} y:${y}")
        }
    }


    private val mStateListener: ICameraHelper.StateCallback = object : ICameraHelper.StateCallback {
        override fun onAttach(device: UsbDevice) {
            XLogger.d("uvc camera life onAttach")
            selectDevice(device)
        }

        override fun onDeviceOpen(device: UsbDevice, isFirstOpen: Boolean) {
            XLogger.d("uvc camera life onDeviceOpen")
            mCameraHelper?.openCamera()
            mViewModel.updateDeviceInfo(device)
        }

        override fun onCameraOpen(device: UsbDevice) {
            XLogger.d("uvc camera life onCameraOpen")
            cameraOpened()
        }

        override fun onCameraClose(device: UsbDevice) {
            XLogger.d("uvc camera life onCameraClose")
            if (mCameraHelper != null) {
                mCameraHelper!!.removeSurface(mBinding.aspectView.surfaceTexture)
            }
        }

        override fun onDeviceClose(device: UsbDevice) {
            XLogger.d("uvc camera life onDeviceClose")
        }

        override fun onDetach(device: UsbDevice) {
            XLogger.d("uvc camera life onDetach")
        }

        override fun onCancel(device: UsbDevice) {
            XLogger.d("uvc camera life onCancel")
        }
    }

    private fun cameraOpened() {
        mCameraHelper?.apply {
            val uvcConfig = UvcCameraUIState(
                autoWhiteBalance = uvcControl.whiteBalanceAuto,
                autoFocus = uvcControl.focusAuto,
                contrast = uvcControl.contrast.toFloat(),
                contrastMin = uvcControl.contrastMin.toFloat(),
                contrastMax = uvcControl.contrastMax.toFloat(),
                sharpness = uvcControl.sharpness.toFloat(),
                sharpnessMin = uvcControl.sharpnessMin.toFloat(),
                sharpnessMax = uvcControl.sharpnessMax.toFloat(),
                hue = uvcControl.hue.toFloat(),
                hueMin = uvcControl.hueMin.toFloat(),
                hueMax = uvcControl.hueMax.toFloat(),
                gain = uvcControl.gain.toFloat(),
                gainMin = uvcControl.gainMin.toFloat(),
                gainMax = uvcControl.gainMax.toFloat(),
                gamma = uvcControl.gamma.toFloat(),
                gammaMin = uvcControl.gammaMin.toFloat(),
                gammaMax = uvcControl.gammaMax.toFloat(),
                saturation = uvcControl.saturation.toFloat(),
                saturationMin = uvcControl.saturationMin.toFloat(),
                saturationMax = uvcControl.saturationMax.toFloat(),
                zoom = uvcControl.zoomAbsolute.toFloat(),
                zoomMax = uvcControl.zoomAbsoluteMax.toFloat(),
                zoomMin = uvcControl.zoomAbsoluteMin.toFloat(),
                brightness = uvcControl.brightness.toFloat(),
                brightnessMin = uvcControl.brightnessMin.toFloat(),
                brightnessMax = uvcControl.brightnessMax.toFloat(),
                exposure = uvcControl.exposureTimeAbsolute.toFloat(),
                exposureMin = uvcControl.exposureTimeMin.toFloat(),
                exposureMax = uvcControl.exposureTimeMax.toFloat(),
                exposureModel = uvcControl.autoExposureMode,
                exposureModelMin = uvcControl.autoExposureModeMin,
                exposureModelMax = uvcControl.autoExposureModeMax,
                focus = uvcControl.focusAbsolute.toFloat(),
                focusMin = uvcControl.focusAbsoluteMin.toFloat(),
                focusMax = uvcControl.focusAbsoluteMax.toFloat(),
            )

            mViewModel.updateParams(uvcConfig)
            XLogger.d("config:${uvcConfig}")

            val newSupportSizeList = mutableListOf<Size>()
            supportedSizeList.forEach { size ->
                if (size.fps >= 15) {
                    val count = newSupportSizeList.count {
                        it.width == size.width && it.height == size.height
                    }
                    if (count == 0) {
                        newSupportSizeList.add(size)
                        XLogger.d("支持的分辨率:${size.width} * ${size.height} ${size.fps} ${size.fpsList}")
                    }
                }
            }
            mViewModel.updateSupportSize(newSupportSizeList)

            startPreview()

            uvcControl.apply {
                focusAuto = false
            }

            if (previewSize != null) {
                mPreviewSize = previewSize
                XLogger.d("预览画面的大小:${previewSize.width}*${previewSize.height}")
                mBinding.aspectView.setAspectRatio(previewSize.width, previewSize.height)
            }

            addSurface(mBinding.aspectView.surfaceTexture, false)
//            setFrameCallback(mFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX)
        }
    }


//    private val mFrameCallback = IFrameCallback { frame ->
//        if (frame != null && mPreviewSize != null) {
//            val nv21 = ByteArray(frame.remaining())
//            frame[nv21, 0, nv21.size]
//            //
////                Bitmap bitmap = mNv21ToBitmap.nv21ToBitmap(nv21, size.width, size.height);
//            XLogger.d("画面的尺寸：${mPreviewSize?.width}*${mPreviewSize?.height}")
//            val bitmap = convertRGBXToBitmap(nv21, mPreviewSize!!.width, mPreviewSize!!.height)
//            lifecycleScope.launch(Dispatchers.Main) {
//                if (!previewConfigured && bitmap != null) {
//                    size = if (bitmap.width > 2160) bitmap.width else 2160
//                    XLogger.d("size 大小:${size} ${bitmap.width}* ${bitmap.height}")
//                    previewConfigured = true
//                    setViewRatio(
//                        mCurrentAspectWithP,
//                        mCurrentAspectHeightP
//                    )
//                }
//                bitmap?.let {
//                    XLogger.d("bitmap的大小:${bitmap.width}*${bitmap.height}")
//                    mBinding.gpuImageView.setImage(bitmap)
//                }
//            }
//        }
//    }

    private fun setFilter(haveFilter: Boolean = true) {
        mGPUImageFilters.clear()
        mBinding.gpuImageView.setRenderMode(GPUImageView.RENDERMODE_WHEN_DIRTY)
        if (haveFilter) {
            mGPUImageFilters.add(mGpuImageLevelsFilter)
            mGPUImageFilters.add(mGPUImageBrightnessFilter)
            mGPUImageFilters.add(mGPUImageWhiteBalanceFilter)
            mGPUImageFilters.add(mGPUImageSharpenFilter)
            mGPUImageFilters.add(mGPUImageGammaFilter)
            mGPUImageFilters.add(mGPUImageSaturationFilter)
            mGPUImageFilters.add(mGPUImageExposureFilter)
            mGPUImageFilters.add(mGPUImageContrastFilter)
            mGPUImageFilters.add(mGPUImageHueFilter)
            mGPUImageFilters.add(mGPUGrayWorldFilter)
            mGPUImageFilters.add(mGPUImagePerfectReflectorBalanceFilter)
        }
        //！！！一定要放在最后面 要不然没效果
        mGPUImageFilters.add(mGpuImageMovieWriter)

        mBinding.gpuImageView.apply {
            filter = GPUImageFilterGroup(mGPUImageFilters)
            setDrawVideoListener {
                mGpuImageMovieWriter.drawVideo = true
                mGpuImageMovieWriter.onInit()
            }
            requestRender()
        }
    }

    private fun setContent() {
        mBinding.compose.setContent {
            val state = rememberBottomSheetScaffoldState()
            BottomSheetScaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = state,
                sheetContent = {
                    SliderContent()
                },
                sheetElevation = 5.dp,
                sheetGesturesEnabled = true,
                sheetPeekHeight = 0.dp,
                sheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                sheetBackgroundColor = Color(0xFFEAE7E7),
            ) {
                MainContent(state)
            }
        }
    }

    @Composable
    fun MainContent(state: BottomSheetScaffoldState) {
        val scope = rememberCoroutineScope()
        DeviceInfoDialog()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState(), enabled = true)
                .background(color = Color.White),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TakePhotoView()
                RecordButton()
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                DeviceInfoView()
                ResetView()
            }
            Row {
                AutoBalanceView()
                AutoFocus()
                HideFilterView()
            }

            ChangeResolutionView()
            WhiteBalanceAlgorithm()
            AspectRatioView()

            Button(onClick = {
                scope.launch {
                    state.bottomSheetState.expand()
                }
            }) {
                Text("Slider")
            }
        }
    }

    @Composable
    fun WhiteBalanceAlgorithm() {
        val scope = rememberCoroutineScope()
        Row {
            TextButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    val bitmap = mBinding.aspectView.bitmap
                    bitmap?.apply {
                        val array = Utils.calculateAverageColor(bitmap)
                        withContext(Dispatchers.Main) {
                            XLogger.d("Grayscale Algorithm")
                            mGPUGrayWorldFilter.setGrayWorldFactors(
                                array[0],
                                array[1],
                                array[2]
                            )
                            mBinding.gpuImageView.requestRender()
                        }
                    }
                }
            }) {
                Text("GrayWorld")
            }

            TextButton(onClick = {
                scope.launch(Dispatchers.IO) {
                    val bitmap = mBinding.aspectView.bitmap
                    bitmap?.apply {
                        val array = Utils.calculateAverageColor(bitmap)
                        withContext(Dispatchers.Main) {
                            mGPUImagePerfectReflectorBalanceFilter.setPerfectReflectorFactors(
                                array[0],
                                array[1],
                                array[2]
                            )
                            mBinding.gpuImageView.requestRender()
                        }
                    }
                }
            }) {
                Text("PerfectReflector")
            }
        }
    }

    @Composable
    fun ChangeResolutionView() {
        val cameraOtherUIState = mViewModel.cameraOtherUIState.collectAsState().value
        val supportedSizeList = cameraOtherUIState.supportedSizeList
        var show by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = {
                    show = true
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("分辨率")
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "",
                            tint = Color.Black
                        )
                    }
                }
            }
            DropdownMenu(expanded = show, onDismissRequest = {
                show = false
            }, modifier = Modifier.width(100.dp)) {
                supportedSizeList.forEach {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${it.width}*${it.height}",
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        },
                        onClick = {
                            show = false
                            XLogger.d("改变分辨率 ${it.width}*${it.height}")
                            val size =
                                Size(UVC_VS_FRAME_MJPEG, it.width, it.height, it.fps, it.fpsList)
                            mCameraHelper?.apply {
                                stopPreview()
                                mPreviewSize = size
                                previewSize = size
                                startPreview()
                                mBinding.aspectView.setAspectRatio(size.width, size.height)
                            }
                        },
                    )
                }
            }
        }
    }

    @Composable
    fun HideFilterView() {
        var haveFilter by remember { mutableStateOf(true) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("NoFilter")
            Switch(checked = haveFilter, onCheckedChange = {
                haveFilter = it
                setFilter(haveFilter)
            })
        }
    }

    @Composable
    fun DeviceInfoDialog() {
        val cameraOtherUIState = mViewModel.cameraOtherUIState.collectAsState().value
        if (cameraOtherUIState.showDeviceInfoDialog) {
            AlertDialog(
                onDismissRequest = {},
                confirmButton = {
                    TextButton(onClick = {
                        mViewModel.showDeviceDialog(false)
                    }) {
                        Text(text = "OK")
                    }
                },
                text = {
                    Text(
                        text = """
                        manufacturerName: ${cameraOtherUIState.device?.manufacturerName}
                        deviceId: ${cameraOtherUIState.device?.deviceId}
                        deviceName: ${cameraOtherUIState.device?.deviceName}
                        deviceProtocol: ${cameraOtherUIState.device?.deviceProtocol}
                        deviceClass: ${cameraOtherUIState.device?.deviceClass}
                        deviceSubclass: ${cameraOtherUIState.device?.deviceSubclass}
                        productId: ${cameraOtherUIState.device?.productId}
                        productName: ${cameraOtherUIState.device?.productName}
                        serialNumber: ${cameraOtherUIState.device?.serialNumber}
                        vendorId: ${cameraOtherUIState.device?.vendorId}
                        version: ${cameraOtherUIState.device?.version}
                    """.trimIndent(),
                    )
                },
                title = {
                    Text(text = "Device Info", fontWeight = FontWeight.Bold)
                },
            )
        }
    }


    @Composable
    fun SliderContent() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 2.dp, top = 20.dp, bottom = 20.dp)
        ) {
            SliderByUvcContent(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 2.dp)
                    .border(width = 0.5.dp, color = Color.Magenta),
                cameraUIState
            )
            SliderByFilterContent(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 2.dp)
                    .border(width = 0.5.dp, color = Color.Magenta),
            )
        }
    }

    @Composable
    fun SliderByUvcContent(modifier: Modifier = Modifier, cameraUIState: UvcCameraUIState) {
        Column(modifier = modifier) {
            ZoomAndFocus(cameraUIState)
            BrightnessAndExposure(cameraUIState)
            SharpnessAndSaturation(cameraUIState)
            GammaAndContrast(cameraUIState)
            HueAnd(cameraUIState)
        }
    }


    @Composable
    fun ZoomAndFocus(cameraUIState: UvcCameraUIState) {
        val zoomSliderValue = remember { mutableFloatStateOf(1f) }
        val focusValue = remember { mutableFloatStateOf(1f) }
        LaunchedEffect(cameraUIState.zoomMin, cameraUIState.focusMin) {
            cameraUIState.apply {
                if ((zoomMax - zoomMin) != 0f) {
                    XLogger.d("zoomMax:${zoomMax} zoomMin:${zoomMin}")
                    zoomSliderValue.floatValue = 100 * (zoom - zoomMin) / (zoomMax - zoomMin)
                }

                if ((focusMax - focusMin) != 0f) {
                    focusValue.floatValue = 100 * (focus - focusMin) / (focusMax - focusMin)
                }
            }
        }

        SliderView(
            name = "Zoom",
            range = 1f..100f,
            defaultValue = zoomSliderValue.floatValue,
            onValueChange = { progress ->
                zoomSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.apply {
                    updateZoomAbsoluteLimit()
                    XLogger.d("zoom isZoomAbsoluteEnable:${isZoomAbsoluteEnable}")
//                    zoomAbsolutePercent = progress.toInt()
                    zoomAbsolute = progress.toInt()
                }
            }
        )
        SliderView(
            name = "Focus",
            range = 1f..100f,
            defaultValue = focusValue.floatValue,
            onValueChange = { progress ->
                focusValue.floatValue = progress
                mCameraHelper?.uvcControl?.focusAbsolute = progress.toInt()
            }
        )
    }

    @Composable
    fun BrightnessAndExposure(cameraUIState: UvcCameraUIState) {
        val brightnessSliderValue = remember { mutableFloatStateOf(1f) }
        val exposureValue = remember { mutableFloatStateOf(1f) }
        LaunchedEffect(cameraUIState.brightnessMin, cameraUIState.exposureMin) {
            cameraUIState.apply {
                if ((brightnessMax - brightnessMin) != 0f) {
                    brightnessSliderValue.floatValue =
                        100 * (brightness - brightnessMin) / (brightnessMax - brightnessMin)
                }

                if ((exposureMax - exposureMin) != 0f) {
                    exposureValue.floatValue =
                        100 * (exposure - exposureMin) / (exposureMax - exposureMin)
                }
            }
        }

        SliderView(
            name = "Brightness",
            range = 1f..100f,
            defaultValue = brightnessSliderValue.floatValue,
            onValueChange = { progress ->
                brightnessSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.brightnessPercent = progress.toInt()
            }
        )

        SliderView(
            name = "Exposure",
            range = 1f..100f,
            defaultValue = exposureValue.floatValue,
            onValueChange = { progress ->
                exposureValue.floatValue = progress
                //调成手动模式
                XLogger.d("ae min:${cameraUIState.exposureMin} max${cameraUIState.exposureMax}")

                mCameraHelper?.uvcControl?.apply {
                    autoExposureMode = ExposureModel.MANUAL_MODEL.value
                    exposureTimeAbsolute = progress.toInt()
                }
            }
        )
    }

    @Composable
    fun GammaAndContrast(cameraUIState: UvcCameraUIState) {
        val gammaSliderValue = remember { mutableFloatStateOf(1f) }
        val contrastSliderValue = remember { mutableFloatStateOf(1f) }

        LaunchedEffect(cameraUIState.gammaMin, cameraUIState.contrastMin) {
            cameraUIState.apply {
                if ((gammaMax - gammaMin) != 0f) {
                    gammaSliderValue.floatValue = 100 * (gamma - gammaMin) / (gammaMax - gammaMin)
                }

                if ((contrastMax - contrastMin) != 0f) {
                    contrastSliderValue.floatValue =
                        100 * (contrast - contrastMin) / (contrastMax - contrastMin)
                }
            }
        }


        SliderView(
            name = "Gamma",
            range = 1f..100f,
            defaultValue = gammaSliderValue.floatValue,
            onValueChange = { progress ->
                gammaSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.apply {
                    gamma = progress.toInt()
                }
            }
        )

        SliderView(
            name = "Contrast",
            range = 1f..100f,
            defaultValue = contrastSliderValue.floatValue,
            onValueChange = { progress ->
                contrastSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.apply {
                    contrast = progress.toInt()
                }
            }
        )
    }

    @Composable
    fun SharpnessAndSaturation(cameraUIState: UvcCameraUIState) {
        val sharpnessSliderValue = remember { mutableFloatStateOf(1f) }
        val saturationSliderValue = remember { mutableFloatStateOf(1f) }

        LaunchedEffect(cameraUIState.sharpnessMin, cameraUIState.saturationMin) {
            cameraUIState.apply {
                if ((sharpnessMax - sharpnessMin) != 0f) {
                    sharpnessSliderValue.floatValue =
                        100 * (sharpness - sharpnessMin) / (sharpnessMax - sharpnessMin)
                }

                if ((saturationMax - saturationMin) != 0f) {
                    saturationSliderValue.floatValue =
                        100 * (saturation - saturationMin) / (saturationMax - saturationMin)
                }
            }
        }


        SliderView(
            name = "Sharpness",
            range = 1f..100f,
            defaultValue = sharpnessSliderValue.floatValue,
            onValueChange = { progress ->
                sharpnessSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.apply {
                    sharpnessPercent = progress.toInt()
                }
            }
        )

        SliderView(
            name = "Saturation",
            range = 1f..100f,
            defaultValue = saturationSliderValue.floatValue,
            onValueChange = { progress ->
                saturationSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.apply {
                    saturation = progress.toInt()
                }
            }
        )
    }


    @Composable
    fun SliderByFilterContent(modifier: Modifier = Modifier) {
        val temperatureSliderValue = remember { mutableFloatStateOf(5000f) }
        val tintSliderValue = remember { mutableFloatStateOf(50f) }
        val toneSliderValue = remember { mutableFloatStateOf(0f) }
        val brightness2SliderValue = remember { mutableFloatStateOf(0f) }
        val sharpnessValue = remember { mutableFloatStateOf(0f) }
        val exposureValue = remember { mutableFloatStateOf(0f) }
        val gammaValue = remember { mutableFloatStateOf(1f) }
        val saturationValue = remember { mutableFloatStateOf(1f) }
        val contrastValue = remember { mutableFloatStateOf(1f) }
        val hueValue = remember { mutableFloatStateOf(0f) }

        Column(modifier = modifier) {
            SliderView(
                name = "temperature",
                range = 2000f..9000f,
                defaultValue = temperatureSliderValue.floatValue,
                onValueChange = { progress ->
                    temperatureSliderValue.floatValue = progress
                    mGPUImageWhiteBalanceFilter.setTemperature(progress)
                }
            )

            SliderView(
                name = "tint",
                range = 0f..100f,
                defaultValue = tintSliderValue.floatValue,
                onValueChange = { progress ->
                    tintSliderValue.floatValue = progress
                    mGPUImageWhiteBalanceFilter.setTint(progress)
                }
            )

            SliderView(
                name = "Tone",
                range = -0.3f..0.3f,
                defaultValue = toneSliderValue.floatValue,
                onValueChange = { progress ->
                    toneSliderValue.floatValue = progress
                    mGpuImageLevelsFilter.setMin(
                        progress,
                        1f, 1f, 0f, 1f
                    )
                }
            )

            SliderView(
                name = "Brightness",
                range = -1f..1f,
                defaultValue = brightness2SliderValue.floatValue,
                onValueChange = { progress ->
                    brightness2SliderValue.floatValue = progress
                    mGPUImageBrightnessFilter.setBrightness(progress)
                }
            )

            SliderView(
                name = "Sharpness",
                range = -4f..4f,
                defaultValue = sharpnessValue.floatValue,
                onValueChange = { progress ->
                    sharpnessValue.floatValue = progress
                    mGPUImageSharpenFilter.setSharpness(progress)
                }
            )

            SliderView(
                name = "Exposure",
                range = -10f..10f,
                defaultValue = exposureValue.floatValue,
                onValueChange = { progress ->
                    exposureValue.floatValue = progress
                    mGPUImageExposureFilter.setExposure(progress)
                }
            )

            SliderView(
                name = "Gamma",
                range = 0f..3f,
                defaultValue = gammaValue.floatValue,
                onValueChange = { progress ->
                    gammaValue.floatValue = progress
                    mGPUImageGammaFilter.setGamma(progress)
                }
            )

            SliderView(
                name = "Saturation",
                range = 0f..2f,
                defaultValue = saturationValue.floatValue,
                onValueChange = { progress ->
                    saturationValue.floatValue = progress
                    mGPUImageSaturationFilter.setSaturation(progress)
                }
            )

            SliderView(
                name = "Contrast",
                range = 0f..4f,
                defaultValue = contrastValue.floatValue,
                onValueChange = { progress ->
                    contrastValue.floatValue = progress
                    mGPUImageContrastFilter.setContrast(progress)
                }
            )

            SliderView(
                name = "Hue",
                range = 0f..360f,
                defaultValue = hueValue.floatValue,
                onValueChange = { progress ->
                    hueValue.floatValue = progress
                    mGPUImageHueFilter.setHue(progress)
                }
            )
        }
    }


    @Composable
    fun AspectRatioView() {
        val scope = rememberCoroutineScope()
        val aspectList = mutableListOf<AspectRatioData>()
        aspectList.add(AspectRatioData(1, 1))
        aspectList.add(AspectRatioData(16, 9))
        aspectList.add(AspectRatioData(9, 16))
        aspectList.add(AspectRatioData(4, 5))
        aspectList.add(AspectRatioData(4, 3))
        aspectList.add(AspectRatioData(2, 1))
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFF3F51B5))
        ) {
            aspectList.forEach {
                item {
                    TextButton(onClick = {
                        mCurrentAspectWithP = it.widthP
                        mCurrentAspectHeightP = it.heightP
                        scope.launch(Dispatchers.IO) {
                            withContext(Dispatchers.Main) {
                                previewConfigured = false
                                size = 1080
                            }
                        }
                    }) {
                        Text(text = "${it.widthP}:${it.heightP}", color = Color.White)
                    }
                }
            }
        }
    }


    @Composable
    fun AutoBalanceView() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
        var autoWhiteBalance by remember { mutableStateOf(cameraUIState.autoWhiteBalance) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("WB")
            Switch(checked = autoWhiteBalance, onCheckedChange = {
                autoWhiteBalance = it
                mCameraHelper?.uvcControl?.apply {
                    whiteBalanceAuto = it
                }
            })
        }
    }

    @Composable
    fun AutoFocus() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
        var autoFocus by remember { mutableStateOf(cameraUIState.autoFocus) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AFocus")
            Switch(checked = autoFocus, onCheckedChange = {
                autoFocus = it
                mCameraHelper?.uvcControl?.apply {
                    focusAuto = it
                }
            })
        }
    }

    @Composable
    fun ResetView() {
        val scope = rememberCoroutineScope()
        Button(modifier = Modifier.padding(start = 10.dp), onClick = {
            scope.launch {
                reset()
            }
        }) {
            Text("Reset")
        }
    }

    @Composable
    fun DeviceInfoView() {
        val scope = rememberCoroutineScope()
        IconButton(onClick = {
            scope.launch {
                mViewModel.showDeviceDialog(show = true)
            }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Info")
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "",
                    tint = Color.Black
                )
            }
        }
    }


    @Composable
    fun HueAnd(cameraUIState: UvcCameraUIState) {
        val hueSliderValue = remember { mutableFloatStateOf(1f) }
        LaunchedEffect(cameraUIState.hueMin) {
            cameraUIState.apply {
                if ((hueMax - hueMin) != 0f) {
                    hueSliderValue.floatValue = 100 * (hue - hueMin) / (hueMax - hueMin)
                }
            }
        }

        SliderView(
            name = "Hue",
            range = 1f..100f,
            defaultValue = hueSliderValue.floatValue,
            onValueChange = { progress ->
                hueSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.huePercent = progress.toInt()
//                (getCurrentCamera() as? CameraUVC)?.setHue(progress.toInt())
            }
        )
    }

    @Composable
    fun TakePhotoView() {
        var bitmap by remember {
            mutableStateOf<Bitmap?>(null)
        }
        Column {
            Button(
                onClick = {
                    val bit = mBinding.gpuImageView.capture()
                    bitmap = bit
                    //把图片保存到相册
                    Utils.saveImageToGallery1(requireContext(), bit, albumName = "GemHubUVC")
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
            scope.launch(Dispatchers.IO) {
                // 在 IO Dispatcher 中进行文件操作
                val videoFile = File(requireContext().cacheDir, "record.mp4")
                try {
                    if (videoFile.exists()) {
                        videoFile.delete()
                    }
                    videoFile.createNewFile()
                } catch (e: Exception) {
                    XLogger.d("录制失败: ${e.message}")
                }
//                    setFilter()
                //todo:set 数据
                mGpuImageMovieWriter.onInit()

                mGpuImageMovieWriter.apply {
                    prepareRecording(
                        videoFile.absolutePath,
                        mBinding.gpuImageView.measuredWidth,
                        mBinding.gpuImageView.measuredHeight,
                    ) { ok ->
                        if (ok) {
                            mGpuImageMovieWriter.startRecording()
                        }
                    }
                }
            }
        }

        fun stopRecording() {
            if (isRecording) {
                isRecording = false
                timerActive.value = false
                scope.launch(Dispatchers.IO) {
                    mGpuImageMovieWriter.stopRecording(object : RecordListener {
                        override fun onStop() {
                            val videoFile = File(requireContext().cacheDir, "record.mp4")
                            Utils.saveVideoToGallery(requireContext(), videoFile, videoFile.name)
                            XLogger.d("录制的视频:${videoFile.absolutePath}")
                        }
                    })
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

    private fun reset() {
        mCameraHelper?.uvcControl?.apply {
            resetExposureTimeAbsolute()
            resetAutoExposureMode()
            resetPowerlineFrequency()
            resetGain()
            resetBrightness()
            resetContrast()
            resetSaturation()
            resetSharpness()
            resetWhiteBalanceAuto()
            resetWhiteBalance()
            resetBacklightComp()
            resetZoomAbsolute()
            resetPanAbsolute()
            resetFocusAuto()
            resetFocusAbsolute()
            resetGamma()
            resetHue()
            mViewModel.resetUvcFilter()
            mCameraHelper?.uvcControl?.focusAuto = false

            mGPUImageGammaFilter.setGamma(1f)
            mGPUImageHueFilter.setHue(0f)
            mGPUImageContrastFilter.setContrast(1.0f)
            mGPUImageSharpenFilter.setSharpness(0f)
            mGPUImageExposureFilter.setExposure(0f)
            mGPUImageSaturationFilter.setSaturation(1f)
            mGPUImageBrightnessFilter.setBrightness(0f)
            mGpuImageLevelsFilter.setMin(0f, 1f, 1f)
            mGPUGrayWorldFilter.setGrayWorldFactors(1.0f, 1.0f, 1.0f)
            previewConfigured = false
            //todo：重新添加白平衡的滤镜，直接设置值会有异常颜色
            //mGPUImageWhiteBalanceFilter.setTemperature(5000f)
            //mGPUImageWhiteBalanceFilter.setTint(50f)
        }

    }
}