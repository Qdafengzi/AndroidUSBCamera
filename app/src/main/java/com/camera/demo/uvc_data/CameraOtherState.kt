package com.camera.demo.uvc_data

import android.hardware.usb.UsbDevice
import com.serenegiant.usb.Size

data class CameraOtherState(
    val device: UsbDevice? = null,
    val showDeviceInfoDialog: Boolean = false,
    val supportedSizeList: List<Size> = listOf(),
)