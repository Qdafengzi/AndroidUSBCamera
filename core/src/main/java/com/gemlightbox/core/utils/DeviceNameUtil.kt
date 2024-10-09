package com.gemlightbox.core.utils

import android.os.Build
import java.util.*

class DeviceNameUtil {
    companion object {
        fun getDeviceName() : String {
            return ("${Build.MANUFACTURER} ${Build.MODEL}").lowercase(Locale.getDefault())
        }
    }
}