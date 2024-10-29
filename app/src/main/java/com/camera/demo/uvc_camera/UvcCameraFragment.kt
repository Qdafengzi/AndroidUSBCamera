package com.camera.demo.uvc_camera

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.graphics.YuvImage
import android.hardware.usb.UsbDevice
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
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
import com.camera.demo.CustomFPS
import com.camera.demo.GPUImageMovieWriter
import com.camera.demo.NV21ToBitmap
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
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.Size
import com.serenegiant.usb.UVCCamera.UVC_VS_FRAME_MJPEG
import jp.co.cyberagent.android.gpuimage.GPUImageView
import jp.co.cyberagent.android.gpuimage.filter.GPUGrayWorldBalanceFilter
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.text.DecimalFormat
import java.util.concurrent.CopyOnWriteArrayList

open class UvcCameraFragment : Fragment() {
    var mRenderWidth = 1920
    var mRenderHeight = 1920

    val DEFAULT_WIDTH: Int = 3840
    val DEFAULT_HEIGHT: Int = 2880


    var mCurrentAspectWithP = 1
    var mCurrentAspectHeightP = 1
    private var mCameraHelper: ICameraHelper? = null
    private var mCustomFPS: CustomFPS? = null

    private val mViewModel = UvcCameraViewModel()

    private val mGpuImageMovieWriter by lazy {
        GPUImageMovieWriter(object :CameraXListener{
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

        })
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

    private val mNV21ToBitmap by lazy {
        NV21ToBitmap(requireContext())
    }

    var previewConfigured = false
    private lateinit var mBinding: FragmentDemo02Binding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentDemo02Binding.inflate(inflater, container, false)
        return mBinding.root
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.aspectView.setZoomCallback {zoom->
            XLogger.d("zoom--->")
//
//            mCameraHelper?.uvcControl?.irisAbsolute
//            mCameraHelper?.uvcControl?.focusAuto = true
//            mCameraHelper?.uvcControl?.updateZoomAbsoluteLimit()
//            mCameraHelper?.uvcControl?.zoomAbsolute = zoom
//            mBinding.aspectView.scaleX = zoom.toFloat()
//            mBinding.aspectView.scaleY = zoom.toFloat()
        }
        mBinding.aspectView.surfaceTextureListener = object :TextureView.SurfaceTextureListener{
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
    }

    protected var size = 1080

    fun setViewRatio(ratioWidth: Int, ratioHeight: Int) {
        if (!previewConfigured){
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
//            mCameraHelper?.previewSize = Size(UVC_VS_FRAME_MJPEG,1920,1080,30, listOf(30,60))

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

//    private val mFrameCallback = IFrameCallback { frame ->
//        if (frame != null && mPreviewSize != null) {
//            if (mCustomFPS != null) {
//                //Refresh FPS
//                mCustomFPS?.doFrame()
//            }
//
//            //
//            val nv21 = ByteArray(frame.remaining())
//            frame[nv21, 0, nv21.size]
//            //
////                Bitmap bitmap = mNv21ToBitmap.nv21ToBitmap(nv21, size.width, size.height);
//            XLogger.d("画面的尺寸：${mPreviewSize?.width}*${mPreviewSize?.height}")
//            val bitmap = convertRGBXToBitmap(nv21, mPreviewSize!!.width, mPreviewSize!!.height)
//
//            if (!previewConfigured && bitmap != null) {
//                size = if (bitmap.width > 2160) bitmap.width else 2160
//                XLogger.d("size 大小:${size} ${bitmap.width}* ${bitmap.height}")
//                previewConfigured = true
//                setViewRatio(
//                    mCurrentAspectWithP,
//                    mCurrentAspectHeightP
//                )
//            }
//
//            lifecycleScope.launch(Dispatchers.Main) {
//                mBinding.gpuImageView.setImage(bitmap)
//            }
//        }
//    }

    private fun convertRGBXToBitmap(rgbxData: ByteArray, width: Int, height: Int): Bitmap {
        // 每个像素四个字节：RGBX
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val r = rgbxData[i * 4].toInt() and 0xFF // 红色通道
            val g = rgbxData[i * 4 + 1].toInt() and 0xFF // 绿色通道
            val b = rgbxData[i * 4 + 2].toInt() and 0xFF // 蓝色通道

            // int x = rgbxData[i * 4 + 3] & 0xFF; // 未使用的字节

            // 构建 ARGB 颜色，A（透明度）设为 255（不透明）
            pixels[i] = 0xFF shl 24 or (r shl 16) or (g shl 8) or b
        }

        // 创建一个 Bitmap 对象
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return bitmap
    }

    var mPreviewSize: Size? = null

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
                supportedSizeList.forEach {size->
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

                this@UvcCameraFragment.mPreviewSize = previewSize
                if (previewSize != null) {
//                    val width = previewSize!!.width
//                    val height = previewSize!!.height
                    //auto aspect ratio
//                    mBinding.aspectView.setAspectRatio(width, height)
                    XLogger.d("预览画面的大小:${previewSize.width}*${previewSize.height}")
                    mBinding.aspectView.setAspectRatio(previewSize.width,previewSize.height)
                }

                addSurface(mBinding.aspectView.surfaceTexture, false)
                //setFrameCallback(mFrameCallback, UVCCamera.PIXEL_FORMAT_RGBX)
                initFPS()
            }
        }

        override fun onCameraClose(device: UsbDevice) {
            XLogger.d("uvc camera life onCameraClose")
            if (mCameraHelper != null) {
                mCameraHelper!!.removeSurface(mBinding.aspectView.surfaceTexture)
            }

            clearFPS()
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

    private fun initFPS() {
        val decimal = DecimalFormat(" #.0' fps'")
        mCustomFPS = CustomFPS()
        mCustomFPS?.addListener { fps ->
            //XLogger.d("FPS:${decimal.format(fps)}")
        }
    }

    private fun clearFPS() {
        if (mCustomFPS != null) {
            mCustomFPS!!.release()
            mCustomFPS = null
        }
    }

    fun yuv420ToBitmap(yuv420Data: ByteArray, width: Int, height: Int): Bitmap? {
        // 创建 YuvImage 对象
        val yuvImage = YuvImage(yuv420Data, ImageFormat.NV21, width, height, null)

        // 使用 ByteArrayOutputStream 将 YuvImage 转换为 Bitmap
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, outputStream)

        // 将输出流转换为 Bitmap
        val bitmapArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.size)
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


    private fun setFilter(haveFilter: Boolean = true) {
        mBinding.gpuImageView.setRenderMode(GPUImageView.RENDERMODE_WHEN_DIRTY)
        val gpuImageFilters = CopyOnWriteArrayList<GPUImageFilter>()
        gpuImageFilters.add(mGpuImageMovieWriter)
        if (haveFilter) {
            gpuImageFilters.add(mGpuImageLevelsFilter)
            gpuImageFilters.add(mGPUImageBrightnessFilter)
            gpuImageFilters.add(mGPUImageWhiteBalanceFilter)
            gpuImageFilters.add(mGPUImageSharpenFilter)
            gpuImageFilters.add(mGPUImageGammaFilter)
            gpuImageFilters.add(mGPUImageSaturationFilter)
            gpuImageFilters.add(mGPUImageExposureFilter)
            gpuImageFilters.add(mGPUImageContrastFilter)
            gpuImageFilters.add(mGPUImageHueFilter)
            gpuImageFilters.add(mGPUGrayWorldFilter)
//            gpuImageFilters.add(mGPUPerfectReflectorBalanceFilter)
        }

        mBinding.gpuImageView.apply {
            this.filter = GPUImageFilterGroup(gpuImageFilters)
            setDrawVideoListener {
                mGpuImageMovieWriter.drawVideo = true
                mGpuImageMovieWriter.onInit()
            }
            requestRender()
        }
    }

    private fun setContent() {
        mBinding.compose.setContent {
            DeviceInfoDialog()
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = scrollState, enabled = true)
                    .background(color = Color.White),
            ) {

                ButtonView()
                Row() {
                    DeviceInfoView()
                    ResetView()
                    AutoBalanceView()
                    AutoFocus()
                    HideFilterView()
                }
                ChangeResolutionView()

                AspectRatioView()
                SliderContent()
            }
        }
    }

    @Composable
    fun ChangeResolutionView() {
        val scope = rememberCoroutineScope()
        val cameraOtherUIState = mViewModel.cameraOtherUIState.collectAsState().value
        val supportedSizeList = cameraOtherUIState.supportedSizeList
        var show  by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()){
                TextButton(
                    onClick = {
                        show = true
                    }) {
                    Text("分辨率")
                }

                TextButton(onClick = {
                   scope.launch(Dispatchers.IO) {
                       val bitmap = mBinding.aspectView.bitmap
                       bitmap?.apply {
                           val array = Utils.calculateAverageColor(bitmap)
                           withContext(Dispatchers.Main) {
                               XLogger.d("Grayscale Algorithm")
                               mGPUGrayWorldFilter.setGrayWorldFactors(array[0], array[1], array[2])
                               mBinding.gpuImageView.requestRender()
                           }
                       }
                   }
                }) {
                    Text("GrayWorld")
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
                            val size = Size(UVC_VS_FRAME_MJPEG, it.width, it.height, it.fps, it.fpsList)
                            mCameraHelper?.apply {
                                stopPreview()
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 2.dp, top = 4.dp)
        ) {
            SliderByUvcContent(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 2.dp)
                    .border(width = 0.5.dp, color = Color.Magenta)
            )
            SliderByFilterContent(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 2.dp)
                    .border(width = 0.5.dp, color = Color.Magenta)
            )
        }
    }

    @Composable
    fun SliderByUvcContent(modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            ZoomAndFocus()
            BrightnessAndExposure()
            SharpnessAndSaturation()
            GammaAndContrast()
            HueAnd()
        }
    }


    @Composable
    fun ZoomAndFocus() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
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
            sliderValue = zoomSliderValue,
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
            sliderValue = focusValue,
            onValueChange = { progress ->
                focusValue.floatValue = progress
                mCameraHelper?.uvcControl?.focusAbsolute = progress.toInt()
            }
        )
    }

    @Composable
    fun BrightnessAndExposure() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
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
            sliderValue = brightnessSliderValue,
            onValueChange = { progress ->
                brightnessSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.brightnessPercent = progress.toInt()
            }
        )

        SliderView(
            name = "Exposure",
            range = 1f..100f,
            sliderValue = exposureValue,
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
    fun GammaAndContrast() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
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
            sliderValue = gammaSliderValue,
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
            sliderValue = contrastSliderValue,
            onValueChange = { progress ->
                contrastSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.apply {
                    contrast = progress.toInt()
                }
            }
        )
    }

    @Composable
    fun SharpnessAndSaturation() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
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
            sliderValue = sharpnessSliderValue,
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
            sliderValue = saturationSliderValue,
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
        Column(modifier = modifier) {
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

            val brightness2SliderValue = remember { mutableFloatStateOf(0f) }
            SliderView(
                name = "Brightness",
                range = -1f..1f,
                sliderValue = brightness2SliderValue,
                onValueChange = { progress ->
                    brightness2SliderValue.floatValue = progress
                    mGPUImageBrightnessFilter.setBrightness(progress)
                }
            )

            val sharpnessValue = remember { mutableFloatStateOf(0f) }
            SliderView(
                name = "Sharpness",
                range = -4f..4f,
                sliderValue = sharpnessValue,
                onValueChange = { progress ->
                    sharpnessValue.floatValue = progress
                    mGPUImageSharpenFilter.setSharpness(progress)
                }
            )

            val exposureValue = remember { mutableFloatStateOf(0f) }
            SliderView(
                name = "Exposure",
                range = -10f..10f,
                sliderValue = exposureValue,
                onValueChange = { progress ->
                    exposureValue.floatValue = progress
                    mGPUImageExposureFilter.setExposure(progress)
                }
            )


            val gammaValue = remember { mutableFloatStateOf(1f) }
            SliderView(
                name = "Gamma",
                range = 0f..3f,
                sliderValue = gammaValue,
                onValueChange = { progress ->
                    gammaValue.floatValue = progress
                    mGPUImageGammaFilter.setGamma(progress)
                }
            )

            val saturationValue = remember { mutableFloatStateOf(1f) }
            SliderView(
                name = "Saturation",
                range = 0f..2f,
                sliderValue = saturationValue,
                onValueChange = { progress ->
                    saturationValue.floatValue = progress
                    mGPUImageSaturationFilter.setSaturation(progress)
                }
            )
            val contrastValue = remember { mutableFloatStateOf(1f) }
            SliderView(
                name = "Contrast",
                range = 0f..4f,
                sliderValue = contrastValue,
                onValueChange = { progress ->
                    contrastValue.floatValue = progress
                    mGPUImageContrastFilter.setContrast(progress)
                }
            )

            val hueValue = remember { mutableFloatStateOf(0f) }
            SliderView(
                name = "Hue",
                range = 0f..360f,
                sliderValue = hueValue,
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
//                                mBinding.aspectView.setAspectRatio(it.widthP,it.heightP)
//                                setViewRatio(it.widthP, it.heightP)
//                                mBinding.gpuImageView.setRatio(it.widthP.toFloat() / it.heightP)
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
        Button(onClick = {
            scope.launch {
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
                    mGPUGrayWorldFilter.setGrayWorldFactors(1.0f,1.0f,1.0f)
                    previewConfigured = false

                    //todo：重新添加白平衡的滤镜，直接设置值会有异常颜色
//                    mGPUImageWhiteBalanceFilter.setTemperature(5000f)
//                    mGPUImageWhiteBalanceFilter.setTint(50f)
                }
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
            Icon(imageVector = Icons.Default.Info, contentDescription = "")
        }
    }


    @Composable
    fun HueAnd() {
        val cameraUIState = mViewModel.cameraUIState.collectAsState().value
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
            sliderValue = hueSliderValue,
            onValueChange = { progress ->
                hueSliderValue.floatValue = progress
                mCameraHelper?.uvcControl?.huePercent = progress.toInt()
//                (getCurrentCamera() as? CameraUVC)?.setHue(progress.toInt())
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
        var bitmap by remember {
            mutableStateOf<Bitmap?>(null)
        }
        Column {
            Button(
                onClick = {
                    val bit = mBinding.gpuImageView.capture()
                    bitmap = bit
                    //把图片保存到相册
                    saveImageToGallery1(bit,System.currentTimeMillis().toString())

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


    private fun saveImageToGallery1(bitmap: Bitmap, albumName: String) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val write = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (write) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + albumName)
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + albumName
                val image = File(imagesDir, filename)
                put(MediaStore.MediaColumns.DATA, image.absolutePath)
            }
        }

        val uri = requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            uri?.let {
                requireContext().contentResolver.openOutputStream(it).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                }
            } ?: throw IOException("Failed to create new MediaStore record.")
        } catch (e: Exception) {
            e.printStackTrace()
        }finally {
//            MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
            requireContext().sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
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
                    setFilter()
                    mGpuImageMovieWriter.apply {
                        drawVideo = true
                        prepareRecording(
                            videoFile.absolutePath,
                            mBinding.gpuImageView.measuredWidth,
                            mBinding.gpuImageView.measuredHeight,
                        ){ok->
                            if (ok){
                                startRecording()
                            }
                        }
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
                        mGpuImageMovieWriter.stopRecording(object :RecordListener{
                            override fun onStop() {
                                XLogger.d("录制已停止")
                                val videoFile = File(requireContext().cacheDir, "record.mp4")
                                saveVideoToGallery(requireContext(), videoFile, videoFile.name)
                                XLogger.d("录制的视频:${videoFile.absolutePath}")
                            }

                        })
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
                        XLogger.d("Frame Rate: $frameRate fps")
                    } else {
                        XLogger.d("Frame Rate information not available.")
                    }
                    break // 找到视频轨道后无需继续
                }
            }
        } catch (e: java.lang.Exception) {
            XLogger.d("Error extracting video info" + e.message)
        } finally {
            extractor.release()
        }
    }


    fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri: Uri? = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (uri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = context.contentResolver.openOutputStream(uri)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                outputStream?.close()
            }
        }
        return uri
    }

    fun saveVideoToGallery(context: Context, videoFile: File, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }

        val uri: Uri? = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (uri != null) {
            var outputStream: OutputStream? = null
            var inputStream: FileInputStream? = null
            try {
                outputStream = context.contentResolver.openOutputStream(uri)
                inputStream = FileInputStream(videoFile)

                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream?.write(buffer, 0, bytesRead)
                }
            } catch (e: Exception) {
                XLogger.d("录制的视频 error:${e.message}")
                e.printStackTrace()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }
        return uri
    }

    var lastPrintTime = System.currentTimeMillis()

    fun logWithInterval(text: String) {
        if ((System.currentTimeMillis() - lastPrintTime) > 3000) {
            XLogger.d(text)
            lastPrintTime = System.currentTimeMillis()
        }
    }
}