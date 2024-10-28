package com.camera.demo.uvc_camera

import android.hardware.usb.UsbDevice
import androidx.lifecycle.ViewModel
import com.camera.demo.uvc_data.CameraOtherState
import com.camera.demo.uvc_data.UvcCameraUIState
import com.serenegiant.usb.Size
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UvcCameraViewModel : ViewModel() {
    private val _cameraUIState = MutableStateFlow(UvcCameraUIState())
    val cameraUIState = _cameraUIState.asStateFlow()

    private val _cameraOtherUIState = MutableStateFlow(CameraOtherState())
    val cameraOtherUIState = _cameraOtherUIState.asStateFlow()

    fun updateParams(state: UvcCameraUIState) {
        _cameraUIState.update {
            state
        }
    }

    fun showDeviceDialog(show: Boolean) {
        _cameraOtherUIState.update {
            it.copy(showDeviceInfoDialog = show)
        }
    }

    fun updateDeviceInfo(device: UsbDevice) {
        _cameraOtherUIState.update {
            it.copy(device = device)
        }
    }

    fun updateSupportSize(supportedSizeList: List<Size>) {
        _cameraOtherUIState.update {
            it.copy(supportedSizeList = supportedSizeList)
        }
    }
}