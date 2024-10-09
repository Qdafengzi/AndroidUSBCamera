package com.gemlightbox.core.preference.core

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.params.RggbChannelVector
import android.util.Size
import com.gemlightbox.core.entity.ShootType
import com.gemlightbox.core.preference.BasePreferences
import com.google.gson.Gson

abstract class CameraPreferences(context: Context) : BasePreferences(context) {

    var filterSharpen: Int
        set(value) = sp.put { putInt(Preference.SHARPEN_PROGRESS, value) }
        get() = sp.getInt(Preference.SHARPEN_PROGRESS, 80)

    var savedFilter: Int
        set(value) = sp.put { putInt(Preference.FILTER_SETTING, value) }
        get() = sp.getInt(Preference.FILTER_SETTING, 0)

    var isCameraOverlayVisible: Boolean
        set(value) = sp.put { putBoolean(Preference.OVERLAY_VISIBILITY_SETTING, value) }
        get() = sp.getBoolean(Preference.OVERLAY_VISIBILITY_SETTING, true)

    var isRatioEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.RATIO_SETTING, value) }
        get() = sp.getBoolean(Preference.RATIO_SETTING, true)

    var ratio: Int
        set(value) = sp.put { putInt(Preference.RATIO_TYPE, value) }
        get() = sp.getInt(Preference.RATIO_TYPE, 0)

    var ratioWidth: Int
        set(value) = sp.put { putInt(Preference.RATIO_WIDTH, value) }
        get() = sp.getInt(Preference.RATIO_WIDTH, 1)

    var ratioHeight: Int
        set(value) = sp.put { putInt(Preference.RATIO_HEIGHT, value) }
        get() = sp.getInt(Preference.RATIO_HEIGHT, 1)

    var zoomLvl: Int
        get() = sp.getInt(Preference.ZOOM_LVL, 14)
        set(value) = sp.put { putInt(Preference.ZOOM_LVL, value) }

    var logoList: String?
        set(value) = sp.put { putString(Preference.LOGO_LIST, value) }
        get() = sp.getString(Preference.LOGO_LIST, null)

    var logoIndex: Int
        set(value) = sp.put { putInt(Preference.LOGO_INDEX, value) }
        get() = sp.getInt(Preference.LOGO_INDEX, 0)

    var isLogoEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.LOGO_ENABLE, value) }
        get() = sp.getBoolean(Preference.LOGO_ENABLE, false)

    var isWatermarkEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.IS_WATERMARK, value) }
        get() = sp.getBoolean(Preference.IS_WATERMARK, false)

    var isWhiteBalance: Boolean
        set(value) = sp.put { putBoolean(Preference.WHITE_BALANCE_ENABLE, value) }
        get() = sp.getBoolean(Preference.WHITE_BALANCE_ENABLE, false)

    var whiteBalanceTemp: Int
        set(value) = sp.put { putInt(Preference.WHITE_BALANCE_TEMP, value) }
        get() = sp.getInt(Preference.WHITE_BALANCE_TEMP, 100)

    var whiteBalanceTint: Int
        set(value) = sp.put { putInt(Preference.WHITE_BALANCE_TINT, value) }
        get() = sp.getInt(Preference.WHITE_BALANCE_TINT, 50)

    var zoomLvlV2: Rect?
        set(value) = sp.put { putString(Preference.ZOOM_LVL_V2, Gson().toJson(value)) }
        get() = Gson().fromJson(sp.getString(Preference.ZOOM_LVL_V2, null), Rect::class.java)

    var fullSizePreview: Boolean
        set(value) = sp.put { putBoolean(Preference.FULL_SIZE_PREVIEW, value) }
        get() = sp.getBoolean(Preference.FULL_SIZE_PREVIEW, false)

    var photoCameraViewZoomLvl: Float
        set(value) = sp.put { putFloat(Preference.PHOTO_CAMERA_VIEW_ZOOM_LVL, value) }
        get() = sp.getFloat(Preference.PHOTO_CAMERA_VIEW_ZOOM_LVL, 1f)

    var isRenderscriptEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.ENABLE_RENDERSCRIPT, value) }
        get() = sp.getBoolean(Preference.ENABLE_RENDERSCRIPT, true)

    var whiteBalanceCameraTest: RggbChannelVector?
        set(value) = sp.put {
            if (value != null) putString(
                Preference.WHITE_BALANCE_CAMERA_TEST,
                Gson().toJson(value)
            ) else remove(Preference.WHITE_BALANCE_CAMERA_TEST)
        }
        get() = Gson().fromJson(
            sp.getString(Preference.WHITE_BALANCE_CAMERA_TEST, null),
            RggbChannelVector::class.java
        )

    var externalCameraWidth: Int
        set(value) = sp.put {
            putInt(Preference.EXTERNAL_CAMERA_WIDTH, value)
        }
        get() = sp.getInt(Preference.EXTERNAL_CAMERA_WIDTH, 3840)

    var externalCameraHeight: Int
        set(value) = sp.put {
            putInt(Preference.EXTERNAL_CAMERA_HEIGHT, value)
        }
        get() = sp.getInt(Preference.EXTERNAL_CAMERA_HEIGHT, 2160)

    var isStabilizeCamera: Boolean
        set(value) = sp.put { putBoolean(Preference.CAMERA_STABILIZATION, value) }
        get() = sp.getBoolean(Preference.CAMERA_STABILIZATION, false)

    var isManualFocusEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.MANUAL_FOCUS, value) }
        get() = sp.getBoolean(Preference.MANUAL_FOCUS, false)

    var isEclipseEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.ENABLE_ECLIPSE, value) }
        get() = sp.getBoolean(Preference.ENABLE_ECLIPSE, true)

    var isGemLoupeEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.ENABLE_GEMLOUPE, value) }
        get() = sp.getBoolean(Preference.ENABLE_GEMLOUPE, false)

    var isPhotoInMaxResolution: Boolean
        set(value) = sp.put { putBoolean(Preference.SAVE_PHOTOS_IN_MAX_RESOLUTION, value) }
        get() = sp.getBoolean(Preference.SAVE_PHOTOS_IN_MAX_RESOLUTION, false)

    var isCameraXEnabled: Boolean
        set(value) = sp.put { putBoolean(Preference.CAMERA_X_ENABLED, value) }
        get() = sp.getBoolean(Preference.CAMERA_X_ENABLED, false)

    var externalZoomLvl: Int
        set(value) = sp.put { putInt(Preference.EXTERNAL_ZOOM_LVL, value) }
        get() = sp.getInt(Preference.EXTERNAL_ZOOM_LVL, 0)

    fun clearRatioSettings() {
        sp.put {
            remove(Preference.RATIO_TYPE)
            remove(Preference.RATIO_WIDTH)
            remove(Preference.RATIO_HEIGHT)
        }
    }

    fun clearCameraSettings() {
        sp.put {
            remove(Preference.ZOOM_LVL)
            remove(Preference.ZOOM_LVL_V2)
            remove(Preference.CONTRAST_PROGRESS)
            remove(Preference.SHARPEN_PROGRESS)
            remove(Preference.FILTER_TONE_AUTO)
            remove(Preference.FILTER_TONE_ECLIPSE)
            remove(Preference.FILTER_TONE_MACRO)
            remove(Preference.WHITE_BG_AUTO)
            remove(Preference.WHITE_BG_ECLIPSE)
            remove(Preference.WHITE_BG_MACRO)
        }
    }

    fun clearZoomLvl() {
        sp.put {
            remove(Preference.ZOOM_LVL)
            remove(Preference.ZOOM_LVL_V2)
        }
    }

    abstract var shootType: ShootType

    abstract var brightness: Int

    abstract var tone: Int

    abstract fun isLogoEmpty(): Boolean
}