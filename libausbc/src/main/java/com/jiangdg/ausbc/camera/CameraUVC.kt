/*
 * Copyright 2017-2023 Jiangdg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jiangdg.ausbc.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.provider.MediaStore
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.jiangdg.ausbc.MultiCameraClient
import com.jiangdg.ausbc.MultiCameraClient.Companion.CAPTURE_TIMES_OUT_SEC
import com.jiangdg.ausbc.MultiCameraClient.Companion.MAX_NV21_DATA
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.callback.ICaptureCallBack
import com.jiangdg.ausbc.callback.IPreviewDataCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import com.jiangdg.ausbc.camera.bean.PreviewSize
import com.jiangdg.ausbc.utils.CameraUtils
import com.jiangdg.ausbc.utils.Logger
import com.jiangdg.ausbc.utils.MediaUtils
import com.jiangdg.ausbc.utils.Utils
import com.jiangdg.utils.XLogWrapper
import com.jiangdg.uvc.IFrameCallback
import com.jiangdg.uvc.UVCCamera
import java.io.File
import java.util.concurrent.TimeUnit

/** UVC Camera
 *
 * @author Created by jiangdg on 2023/1/15
 */
class CameraUVC(ctx: Context, device: UsbDevice) : MultiCameraClient.ICamera(ctx, device) {
    private var mUvcCamera: UVCCamera? = null
    private val mCameraPreviewSize by lazy {
        arrayListOf<PreviewSize>()
    }

    private val frameCallBack = IFrameCallback { frame ->
        frame?.apply {
            position(0)
            val data = ByteArray(capacity())
            get(data)
            mCameraRequest?.apply {
                if (data.size != previewWidth * previewHeight * 3 / 2) {
                    return@IFrameCallback
                }

                //下面的代码只是返回预览帧的callback
                // for preview callback
                mPreviewDataCbList.forEach { cb ->
                    cb?.onPreviewData(data, previewWidth, previewHeight, IPreviewDataCallBack.DataFormat.NV21)
                }
                // for image
                if (mNV21DataQueue.size >= MAX_NV21_DATA) {
                    mNV21DataQueue.removeLast()
                }
                mNV21DataQueue.offerFirst(data)
                // for video
                // avoid preview size changed
                putVideoData(data)
            }
        }
    }

    fun determineImageFormat(data: ByteArray, width: Int, height: Int): String {
        val size = data.size

        return if (size == width * height * 4) {
            "RGBA"
        } else if (size == width * height * 3) {
            "RGB"
        } else if (size == width * height * 2) {
            "RGB565"
        } else if (size.toDouble() == width * height * 1.5) {
            // Check further for NV21/NV12
            // For NV21 (YUV420SP): VU interleaved format
            "YUV420 (NV21 or NV12)"
        } else {
            "Unknown"
        }
    }


    fun cropNV21(data: ByteArray, previewWidth: Int, previewHeight: Int, cropWidth: Int, cropHeight: Int, startX: Int, startY: Int): ByteArray {
        val croppedData = ByteArray(cropWidth * cropHeight * 3 / 2)
        var outputIndex = 0

        // Crop Y plane
        for (i in 0 until cropHeight) {
            val inputOffset = (startY + i) * previewWidth + startX
            System.arraycopy(data, inputOffset, croppedData, outputIndex, cropWidth)
            outputIndex += cropWidth
        }

        // Crop UV plane
        val uvStart = previewWidth * previewHeight
        for (i in 0 until cropHeight / 2) {
            val inputOffset = uvStart + (startY / 2 + i) * previewWidth + startX
            System.arraycopy(data, inputOffset, croppedData, outputIndex, cropWidth)
            outputIndex += cropWidth
        }

        return croppedData
    }


    override fun getAllPreviewSizes(aspectRatio: Double?): MutableList<PreviewSize> {
        val previewSizeList = arrayListOf<PreviewSize>()
        val isMjpegFormat = mCameraRequest?.previewFormat == CameraRequest.PreviewFormat.FORMAT_MJPEG
        if (isMjpegFormat && (mUvcCamera?.supportedSizeList?.isNotEmpty() == true)) {
            mUvcCamera?.supportedSizeList
        }  else {
            mUvcCamera?.getSupportedSizeList(UVCCamera.FRAME_FORMAT_YUYV)
        }?.let { sizeList ->
            if (sizeList.size > mCameraPreviewSize.size) {
                mCameraPreviewSize.clear()
                sizeList.forEach { size->
                    val width = size.width
                    val height = size.height
                    mCameraPreviewSize.add(PreviewSize(width, height))
                }
            }
            if (Utils.debugCamera) {
                Logger.i(TAG, "aspect ratio = $aspectRatio, supportedSizeList = $sizeList")
            }
            mCameraPreviewSize
        }?.onEach { size ->
            val width = size.width
            val height = size.height
            val ratio = width.toDouble() / height
            if (aspectRatio == null || aspectRatio == ratio) {
                previewSizeList.add(PreviewSize(width, height))
            }
        }
        return previewSizeList
    }

    override fun <T> openCameraInternal(cameraView: T) {
        if (Utils.isTargetSdkOverP(ctx) && !CameraUtils.hasCameraPermission(ctx)) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "Has no CAMERA permission.")
            Logger.e(TAG,"open camera failed, need Manifest.permission.CAMERA permission when targetSdk>=28")
            return
        }
        if (mCtrlBlock == null) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "Usb control block can not be null ")
            return
        }
        // 1. create a UVCCamera
        val request = mCameraRequest!!
        try {
            mUvcCamera = UVCCamera().apply {
                open(mCtrlBlock)
            }
        } catch (e: Exception) {
            closeCamera()
            postStateEvent(ICameraStateCallBack.State.ERROR, "open camera failed ${e.localizedMessage}")
            Logger.e(TAG, "open camera failed.", e)
        }

        // 2. set preview size and register preview callback
        var previewSize = getSuitableSize(request.previewWidth, request.previewHeight).apply {
            mCameraRequest!!.previewWidth = width
            mCameraRequest!!.previewHeight = height
        }
        val previewFormat = if (mCameraRequest?.previewFormat == CameraRequest.PreviewFormat.FORMAT_YUYV) {
            UVCCamera.FRAME_FORMAT_YUYV
        } else {
            UVCCamera.FRAME_FORMAT_MJPEG
        }
        try {
            Logger.i(TAG, "getSuitableSize: $previewSize")
            if (! isPreviewSizeSupported(previewSize)) {
                closeCamera()
                postStateEvent(ICameraStateCallBack.State.ERROR, "unsupported preview size")
                Logger.e(TAG, "open camera failed, preview size($previewSize) unsupported-> ${mUvcCamera?.supportedSizeList}")
                return
            }
            initEncodeProcessor(previewSize.width, previewSize.height)
            // if give custom minFps or maxFps or unsupported preview size
            // this method will fail
            mUvcCamera?.setPreviewSize(
                previewSize.width,
                previewSize.height,
                MIN_FS,
                MAX_FPS,
                previewFormat,
                UVCCamera.DEFAULT_BANDWIDTH
            )
        } catch (e: Exception) {
            try {
                previewSize = getSuitableSize(request.previewWidth, request.previewHeight).apply {
                    mCameraRequest!!.previewWidth = width
                    mCameraRequest!!.previewHeight = height
                }
                if (! isPreviewSizeSupported(previewSize)) {
                    postStateEvent(ICameraStateCallBack.State.ERROR, "unsupported preview size")
                    closeCamera()
                    Logger.e(TAG, "open camera failed, preview size($previewSize) unsupported-> ${mUvcCamera?.supportedSizeList}")
                    return
                }
                Logger.e(TAG, " setPreviewSize failed(format is $previewFormat), try to use other format...")
                mUvcCamera?.setPreviewSize(
                    previewSize.width,
                    previewSize.height,
                    MIN_FS,
                    MAX_FPS,
                    if (previewFormat == UVCCamera.FRAME_FORMAT_YUYV) {
                        UVCCamera.FRAME_FORMAT_MJPEG
                    } else {
                        UVCCamera.FRAME_FORMAT_YUYV
                    },
                    UVCCamera.DEFAULT_BANDWIDTH
                )

            } catch (e: Exception) {
                closeCamera()
                postStateEvent(ICameraStateCallBack.State.ERROR, "err: ${e.localizedMessage}")
                Logger.e(TAG, " setPreviewSize failed, even using yuv format", e)
                return
            }
        }
        // if not opengl render or opengl render with preview callback
        // there should opened
        if (!isNeedGLESRender || mCameraRequest!!.isRawPreviewData || mCameraRequest!!.isCaptureRawImage) {
            mUvcCamera?.setFrameCallback(frameCallBack, UVCCamera.PIXEL_FORMAT_YUV420SP)
        }
        // 3. start preview
        when(cameraView) {
            is Surface -> {
                mUvcCamera?.setPreviewDisplay(cameraView)
            }
            is SurfaceTexture -> {
                mUvcCamera?.setPreviewTexture(cameraView)
            }
            is SurfaceView -> {
                mUvcCamera?.setPreviewDisplay(cameraView.holder)
            }
            is TextureView -> {
                mUvcCamera?.setPreviewTexture(cameraView.surfaceTexture)
            }
            else -> {
                throw IllegalStateException("Only support Surface or SurfaceTexture or SurfaceView or TextureView or GLSurfaceView--$cameraView")
            }
        }

        mUvcCamera?.autoFocus = true
        mUvcCamera?.autoWhiteBalance = true
        mUvcCamera?.startPreview()
        mUvcCamera?.updateCameraParams()
        isPreviewed = true
        postStateEvent(ICameraStateCallBack.State.OPENED)
        if (Utils.debugCamera) {
            Logger.i(TAG, " start preview, name = ${device.deviceName}, preview=$previewSize")
        }
    }

    override fun closeCameraInternal() {
        postStateEvent(ICameraStateCallBack.State.CLOSED)
        isPreviewed = false
        releaseEncodeProcessor()
        mUvcCamera?.destroy()
        mUvcCamera = null
        if (Utils.debugCamera) {
            Logger.i(TAG, " stop preview, name = ${device.deviceName}")
        }
    }

    override fun captureImageInternal(savePath: String?, callback: ICaptureCallBack) {
        mSaveImageExecutor.submit {
//            if (! CameraUtils.hasStoragePermission(ctx)) {
//                mMainHandler.post {
//                    callback.onError("have no storage permission")
//                }
//                Logger.e(TAG,"open camera failed, have no storage permission")
//                return@submit
//            }
            if (! isPreviewed) {
                mMainHandler.post {
                    callback.onError("camera not previewing")
                }
                Logger.i(TAG, "captureImageInternal failed, camera not previewing")
                return@submit
            }
            val data = mNV21DataQueue.pollFirst(CAPTURE_TIMES_OUT_SEC, TimeUnit.SECONDS)
            if (data == null) {
                mMainHandler.post {
                    callback.onError("Times out")
                }
                Logger.i(TAG, "captureImageInternal failed, times out.")
                return@submit
            }
            mMainHandler.post {
                callback.onBegin()
            }
            val date = mDateFormat.format(System.currentTimeMillis())
            val title = savePath ?: "IMG_AUSBC_$date"
            val displayName = savePath ?: "$title.jpg"
            val path = savePath ?: "$mCameraDir/$displayName"
            val location = Utils.getGpsLocation(ctx)
            val width = mCameraRequest!!.previewWidth
            val height = mCameraRequest!!.previewHeight
            val ret = MediaUtils.saveYuv2Jpeg(path, data, width, height)
            if (! ret) {
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
                mMainHandler.post {
                    callback.onError("save yuv to jpeg failed.")
                }
                Logger.w(TAG, "save yuv to jpeg failed.")
                return@submit
            }
            val values = ContentValues()
            values.put(MediaStore.Images.ImageColumns.TITLE, title)
            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, displayName)
            values.put(MediaStore.Images.ImageColumns.DATA, path)
            values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date)
            values.put(MediaStore.Images.ImageColumns.LONGITUDE, location?.longitude)
            values.put(MediaStore.Images.ImageColumns.LATITUDE, location?.latitude)
            ctx.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            mMainHandler.post {
                callback.onComplete(path)
            }
            if (Utils.debugCamera) { Logger.i(TAG, "captureImageInternal save path = $path") }
        }
    }

    /**
     * Is mic supported
     *
     * @return true camera support mic
     */
    fun isMicSupported() = CameraUtils.isCameraContainsMic(this.device)

    /**
     * Send camera command
     *
     * This method cannot be verified, please use it with caution
     */
    fun sendCameraCommand(command: Int) {
        mCameraHandler?.post {
            mUvcCamera?.sendCommand(command)
        }
    }

    /**
     * Set auto focus
     *
     * @param enable true enable auto focus
     */
    fun setAutoFocus(enable: Boolean) {
        mUvcCamera?.autoFocus = enable
    }

    /**
     * Get auto focus
     *
     * @return true enable auto focus
     */
    fun getAutoFocus() = mUvcCamera?.autoFocus

    /**
     * Reset auto focus
     */
    fun resetAutoFocus() {
        mUvcCamera?.resetFocus()
    }

    /**
     * Set auto white balance
     *
     * @param autoWhiteBalance true enable auto white balance
     */
    fun setAutoWhiteBalance(autoWhiteBalance: Boolean) {
        mUvcCamera?.autoWhiteBalance = autoWhiteBalance
    }

    /**
     * Get auto white balance
     *
     * @return true enable auto white balance
     */
    fun getAutoWhiteBalance() = mUvcCamera?.autoWhiteBalance


    fun setFocus(focus: Int) {
        mUvcCamera?.focus = focus
    }

    fun getFocus() = mUvcCamera?.focus

    fun getFocusMax() = mUvcCamera?.focusMax
    fun getFocusMin() = mUvcCamera?.focusMin


    /**
     * Set zoom
     *
     * @param zoom zoom value, 0 means reset
     */
    fun setZoom(zoom: Int) {
        mUvcCamera?.zoom = zoom
    }

    /**
     * Get zoom
     */
    fun getZoom() = mUvcCamera?.zoom

    /**
     * Reset zoom
     */
    fun resetZoom() {
        mUvcCamera?.resetZoom()
    }


    fun getZoomMax() = mUvcCamera?.zoomMax

    fun getZoomMin() = mUvcCamera?.zoomMin


    fun getExposure() = mUvcCamera?.exposure

    fun setExposure(exposure: Int) {
        mUvcCamera?.exposure = exposure
    }

    fun resetExposure() {
        mUvcCamera?.resetExposure()
    }

    fun getExposureMax() = mUvcCamera?.exposureMax
    fun getExposureMin() = mUvcCamera?.exposureMin


    fun getExposureModel() = mUvcCamera?.exposureModel

    fun setExposureModel(model: Int) {
        mUvcCamera?.exposureModel = model
    }

    fun resetExposureModel() {
        mUvcCamera?.resetExposureModel()
    }

    fun getExposureModelMax() = mUvcCamera?.exposureModelMax
    fun getExposureModelMin() = mUvcCamera?.exposureModelMin

    /**
     * Set gain
     *
     * @param gain gain value, 0 means reset
     */
    fun setGain(gain: Int) {
        mUvcCamera?.gain = gain
    }

    /**
     * Get gain
     */
    fun getGain() = mUvcCamera?.gain

    /**
     * Reset gain
     */
    fun resetGain() {
        mUvcCamera?.resetGain()
    }

    fun getGainMax() = mUvcCamera?.gainMax

    fun getGainMin() = mUvcCamera?.gainMin


    /**
     * Set gamma
     *
     * @param gamma gamma value, 0 means reset
     */
    fun setGamma(gamma: Int) {
        mUvcCamera?.gamma = gamma
    }

    /**
     * Get gamma
     */
    fun getGamma() = mUvcCamera?.gamma

    /**
     * Reset gamma
     */
    fun resetGamma() {
        mUvcCamera?.resetGamma()
    }

    fun getGammaMax() = mUvcCamera?.gammaMax

    fun getGammaMin() = mUvcCamera?.gammaMin


    /**
     * Set brightness
     *
     * @param brightness brightness value, 0 means reset
     */
    fun setBrightness(brightness: Int) {
        mUvcCamera?.brightness = brightness
    }

    /**
     * Get brightness
     */
    fun getBrightness() = mUvcCamera?.brightness
    
    fun getBrightnessMax() = mUvcCamera?.brightnessMax

    fun getBrightnessMin() = mUvcCamera?.brightnessMin
    
    /**
     * Reset brightnes
     */
    fun resetBrightness() {
        mUvcCamera?.resetBrightness()
    }

    /**
     * Set contrast
     *
     * @param contrast contrast value, 0 means reset
     */
    fun setContrast(contrast: Int) {
        mUvcCamera?.contrast = contrast
    }

    /**
     * Get contrast
     */
    fun getContrast() = mUvcCamera?.contrast

    fun getContrastMax() = mUvcCamera?.contrastMax

    fun getContrastMin() = mUvcCamera?.contrastMin

    /**
     * Reset contrast
     */
    fun resetContrast() {
        mUvcCamera?.resetContrast()
    }

    /**
     * Set sharpness
     *
     * @param sharpness sharpness value, 0 means reset
     */
    fun setSharpness(sharpness: Int) {
        mUvcCamera?.sharpness = sharpness
    }

    /**
     * Get sharpness
     */
    fun getSharpness() = mUvcCamera?.sharpness

    /**
     * Reset sharpness
     */
    fun resetSharpness() {
        mUvcCamera?.resetSharpness()
    }

    fun getSharpnessMax() = mUvcCamera?.sharpnessMax

    fun getSharpnessMin() = mUvcCamera?.sharpnessMin


    /**
     * Set saturation
     *
     * @param saturation saturation value, 0 means reset
     */
    fun setSaturation(saturation: Int) {
        mUvcCamera?.saturation = saturation
    }

    /**
     * Get saturation
     */
    fun getSaturation() = mUvcCamera?.saturation

    /**
     * Reset saturation
     */
    fun resetSaturation() {
        mUvcCamera?.resetSaturation()
    }

    fun getSaturationMax() = mUvcCamera?.saturationMax

    fun getSaturationMin() = mUvcCamera?.saturationMin


    /**
     * Set hue
     *
     * @param hue hue value, 0 means reset
     */
    fun setHue(hue: Int) {
        mUvcCamera?.hue = hue
    }

    /**
     * Get hue
     */
    fun getHue() = mUvcCamera?.hue

    /**
     * Reset saturation
     */
    fun resetHue() {
        mUvcCamera?.resetHue()
    }

    fun getHueMax() = mUvcCamera?.hueMax

    fun getHueMin() = mUvcCamera?.hueMin


    companion object {
        private const val TAG = "CameraUVC"
        private const val MIN_FS = 1
        private const val MAX_FPS = 61
    }
}