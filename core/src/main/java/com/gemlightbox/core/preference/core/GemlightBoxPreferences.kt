package com.gemlightbox.core.preference.core

import android.content.Context
import android.text.Layout
import androidx.lifecycle.MutableLiveData
import com.gemlightbox.core.preference.BasePreferences
import com.gemlightbox.core.preference.core.Preference.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

abstract class GemlightBoxPreferences(context: Context) : BasePreferences(context) {

    private val IIS_URL: String = "IIS_URL"
    private val IIS_WORKPLACE: String = "IIS_WORKPLACE"
    private val IIS_LANGUAGE: String = "IIS_LANGUAGE"
    private val IIS_USERNAME: String = "IIS_USERNAME"
    private val IIS_PASSWORD: String = "IIS_PASSWORD"
    private val IIS_PICTURE_FOLDER: String = "IIS_PICTURE_FOLDER"
    private val VIDEO_MODE: String = "VIDEO_MODE"
    private val VIDEO_SPEED: String = "VIDEO_SPEED"

    private val MY_PREFERENCES = "CameraPickPreferences"


    private val AMAZON_INITIAL_FOLDER: String = "AMAZON_INITIAL_FOLDER"
    private val AMAZON_BUCKET_PLACEMENT: String = "AMAZON_BUCKET_PLACEMENT"

    private val SHARE_LINK_FIRST_TIME: String = "SHARE_LINK_FIRST_TIME"
    private val SHARE_LINK_DIALOG_SHOW: String = "SHARE_LINK_DIALOG_SHOW"
    private val SHARE_LINK_NOTIFICATION_BETA: String = "SHARE_LINK_NOTIFICATION_BETA"
    private val ENABLE_RESET_BUTTON: String = "ENABLE_RESET_BUTTON"
    private val CONFIRM_EMAIL_SHOW: String = "CONFIRM_EMAIL_SHOW"
    private val BROKEN_BLINK_FILES_REMOVED_TEST: String = "BROKEN_BLINK_FILES_REMOVED_TEST"

    var galleryLayoutManagerType: String
        set(value) = sp.put { putString(GALLERY_LAYOUT_MANAGER_TYPE, value) }
        get() = sp.getString(GALLERY_LAYOUT_MANAGER_TYPE, "grid")!!

    var catalogsLayoutManagerType: String
        set(value) = sp.put { putString(CATALOGS_LAYOUT_MANAGER_TYPE, value) }
        get() = sp.getString(CATALOGS_LAYOUT_MANAGER_TYPE, "grid")!!

    var deviceToAutoconnect: String?
        set(value) = sp.put { putString(DEVICE_TO_AUTOCONNECT, value) }
        get() = sp.getString(DEVICE_TO_AUTOCONNECT, null)

    var isAutoconnectMode: Boolean
        set(value) = sp.put { putBoolean(AUTO_CONNECT_BLUETOOTH, value) }
        get() = sp.getBoolean(AUTO_CONNECT_BLUETOOTH, false)


    var isEditSaveDialogEnabled: Boolean
        set(value) = sp.put { putBoolean(EDIT_SAVE_DIALOG_ENABLE, value) }
        get() = sp.getBoolean(EDIT_SAVE_DIALOG_ENABLE, true)

    var userEmail: String
        set(value) = sp.put { putString(EMAIL_KEY, value) }
        get() = sp.getString(EMAIL_KEY, "")!!

    var tempEmail: String
        set(value) = sp.put { putString(TEMP_EMAIL_KEY, value) }
        get() = sp.getString(TEMP_EMAIL_KEY, "")!!

    var shouldCheckEmail: Boolean
        set(value) = sp.put { putBoolean(CHECK_SYNCED_ITEMS, value) }
        get() = sp.getBoolean(CHECK_SYNCED_ITEMS, false)

    var authToken: String
        set(value) = sp.put { putString(TOKEN_KEY, value) }
        get() = if (sp.getString(TOKEN_KEY, "").isNullOrEmpty()) "" else sp.getString(
            TOKEN_KEY,
            ""
        )!!

    var apiToken: String
        set(value) = sp.put { putString(API_TOKEN_KEY, value) }
        get() = if (sp.getString(API_TOKEN_KEY, "").isNullOrEmpty()) "" else sp.getString(
            API_TOKEN_KEY,
            ""
        )!!

    var isOnBoardComplete: Boolean
        set(value) = sp.put { putBoolean(ON_BOARD_COMPLETE, value) }
        get() = sp.getBoolean(ON_BOARD_COMPLETE, false)

    var isDrawSkuEnabled: Boolean
        set(value) = sp.put { putBoolean(IS_DRAW_SKU, value) }
        get() = sp.getBoolean(IS_DRAW_SKU, false)

    var watermarkText: String
        set(value) = sp.put { putString(WATERMARK_TEXT, value) }
        get() = sp.getString(WATERMARK_TEXT, "") ?: ""


    var skuTextSize: Float
        set(value) = sp.put { putFloat(SKU_SIZE, value) }
        get() = sp.getFloat(SKU_SIZE, 0f)

    var skuContainerSize: Float
        set(value) = sp.put { putFloat(SKU_CONTAINER_SIZE, value) }
        get() = sp.getFloat(SKU_CONTAINER_SIZE, 0f)

    var skuAlign: Layout.Alignment
        set(value) = sp.put { putString(SKU_ALIGN, value.toString()) }
        get() {
            return if (sp.getString(SKU_ALIGN, "").isNullOrEmpty()) {
                Layout.Alignment.ALIGN_CENTER
            } else {
                Layout.Alignment.valueOf(sp.getString(SKU_ALIGN, "")!!)
            }
        }

    var skuTypeface: String
        set(value) = sp.put { putString(SKU_TYPEFACE, value) }
        get() = sp.getString(SKU_TYPEFACE, "")!!

    var skuTextColor: Int
        set(value) = sp.put { putInt(SKU_COLOR, value) }
        get() = sp.getInt(SKU_COLOR, 0)

    var skuXCoordinate: Float
        set(value) = sp.put { putFloat(SKU_X_COORDINATE, value) }
        get() = sp.getFloat(SKU_X_COORDINATE, 0f)

    var skuYCoordinate: Float
        set(value) = sp.put { putFloat(SKU_Y_COORDINATE, value) }
        get() = sp.getFloat(SKU_Y_COORDINATE, 0f)

    var watermarkTextSize: Float
        set(value) = sp.put { putFloat(WATERMARK_SIZE, value) }
        get() = sp.getFloat(WATERMARK_SIZE, 0f)

    var watermarkAlign: Layout.Alignment
        set(value) = sp.put { putString(WATERMARK_ALIGN, value.toString()) }
        get() {
            return if (sp.getString(WATERMARK_ALIGN, "").isNullOrEmpty()) {
                Layout.Alignment.ALIGN_CENTER
            } else {
                Layout.Alignment.valueOf(sp.getString(WATERMARK_ALIGN, "")!!)
            }
        }

    var watermarkTypeface: String
        set(value) = sp.put { putString(WATERMARK_TYPEFACE, value) }
        get() = sp.getString(WATERMARK_TYPEFACE, "")!!

    var watermarkTextColor: Int
        set(value) = sp.put { putInt(WATERMARK_COLOR, value) }
        get() = sp.getInt(WATERMARK_COLOR, 0)

    var watermarkOpacity: Int
        set(value) = sp.put { putInt(WATERMARK_OPACITY, value) }
        get() = sp.getInt(WATERMARK_OPACITY, 100)


    var isCameraFirstTime: Boolean
        set(value) = sp.put { putBoolean(TUTORIALS_CAMERA_FIRST_TIME, value) }
        get() = sp.getBoolean(TUTORIALS_CAMERA_FIRST_TIME, true)

    var showCameraAlertDialog: Boolean
        set(value) = sp.put { putBoolean(SHOW_CAMERA_ALERT_DIALOG, value) }
        get() = sp.getBoolean(SHOW_CAMERA_ALERT_DIALOG, true)

    var isVideoInMaxResolution: Boolean
        set(value) = sp.put { putBoolean(SAVE_VIDEOS_IN_4K, value) }
        get() = sp.getBoolean(SAVE_VIDEOS_IN_4K, false)

    var isTutorialsFirstTime: Boolean
        set(value) = sp.put { putBoolean(TUTORIALS_FIRST_TIME, value) }
        get() = sp.getBoolean(TUTORIALS_FIRST_TIME, false)
    var subscribeStatus: Boolean
        set(value) = sp.put { putBoolean(SUBSCRIBE_STATUS, value) }
        get() = sp.getBoolean(SUBSCRIBE_STATUS, false)
    var isSubaccount: Boolean
        set(value) = sp.put { putBoolean(IS_SUBACCOUNT, value) }
        get() = sp.getBoolean(IS_SUBACCOUNT, false)
    var verificationStatus: Boolean
        set(value) = sp.put { putBoolean(VERIFICATION_STATUS, value) }
        get() = sp.getBoolean(VERIFICATION_STATUS, false)
    var isBurstEnabled: Boolean
        set(value) = sp.put { putBoolean(BURST_ENABLED, value) }
        get() = sp.getBoolean(BURST_ENABLED, true)
    var isBurstMacroEnabled: Boolean
        set(value) = sp.put { putBoolean(BURST_ENABLED_MACRO, value) }
        get() = sp.getBoolean(BURST_ENABLED_MACRO, true)
    var isBurstModelEnabled: Boolean
        set(value) = sp.put { putBoolean(BURST_MODEL_ENABED, value) }
        get() = sp.getBoolean(BURST_MODEL_ENABED, false)
    var isBurstRemoveBackgroundImagesEnabled: Boolean
        set(value) = sp.put { putBoolean(BURST_REMOVE_BACKGROUND_IMAGES_ENABLED, value) }
        get() = sp.getBoolean(BURST_REMOVE_BACKGROUND_IMAGES_ENABLED, false)
    var isBurstRemoveBackgroundVideoEnabled: Boolean
        set(value) = sp.put { putBoolean(BURST_REMOVE_BACKGROUND_VIDEO_ENABLED, value) }
        get() = sp.getBoolean(BURST_REMOVE_BACKGROUND_VIDEO_ENABLED, false)
    var isBurstAiRetouchImagesEnabled: Boolean
        set(value) = sp.put { putBoolean(BURST_AI_RETOUCH_IMAGES_ENABLED, value) }
        get() = sp.getBoolean(BURST_AI_RETOUCH_IMAGES_ENABLED, false)
    var isBurstAiDescEnabled: Boolean
        set(value) = sp.put { putBoolean(BURST_AI_DESC_ENABLED, value) }
        get() = sp.getBoolean(BURST_AI_DESC_ENABLED, false)

    var storageIsFull: Boolean
        set(value) {
            sp.put { putBoolean(STORAGE_IS_FULL, value) }
            storageIsFullLiveData.postValue(value)
        }
        get() = sp.getBoolean(STORAGE_IS_FULL, false)

    var storageIsFullLiveData = MutableLiveData(storageIsFull)

    var showFullStorageDialog: Boolean
        set(value) = sp.put { putBoolean(SHOW_FULL_STORAGE_DIALOG, value) }
        get() = sp.getBoolean(SHOW_FULL_STORAGE_DIALOG, true)
    var verifyItemShown: Boolean
        set(value) = sp.put { putBoolean(VERIFY_ITEM_SHOWN, value) }
        get() = sp.getBoolean(VERIFY_ITEM_SHOWN, false)
    var isUploadWithoutWifi: Boolean
        set(value) = sp.put { putBoolean(UPLOAD_WITHOUT_WIFI, value) }
        get() = sp.getBoolean(UPLOAD_WITHOUT_WIFI, false)
    var isEnhanceImage: Boolean
        set(value) = sp.put { putBoolean(ENHANCE_IMAGE, value) }
        get() = sp.getBoolean(ENHANCE_IMAGE, false)

    var workManagerId: String
        set(value) = sp.put { putString(WORKMANAGER_ID, value) }
        get() = sp.getString(WORKMANAGER_ID, "")?:""

    var amazonSecret: String?
        set(value) = sp.put { putString(SECRET, value) }
        get() = sp.getString(SECRET, "")

    var isNativeCamera: Boolean
        set(value) = sp.put { putBoolean(SAFE_MODE_KEY, value) }
        get() = sp.getBoolean(SAFE_MODE_KEY, false)

    var initialFolder: String?
        set(value) = sp.put { putString(AMAZON_INITIAL_FOLDER, value) }
        get() = sp.getString(AMAZON_INITIAL_FOLDER, "")

    var cloudStorageFullShowDialogTime: Long
        set(value) = sp.put { putLong(CLOUD_STORAGE_FULL_DIALOG_SHOW_TIME, value) }
        get() = sp.getLong(CLOUD_STORAGE_FULL_DIALOG_SHOW_TIME, 0)



    var skuList: List<String>
        set(value) {
            val skuHashSet: HashSet<String> = HashSet<String>(skuList)
            skuHashSet.addAll(value)

            val sku = StringBuilder()
            for (s in skuHashSet) sku.append(s).append(",")
            if (sku.isNotEmpty()) sku.deleteCharAt(sku.length - 1)
            sp.put { putString(TOKEN_SKU, String(sku)) }
        }
        get() {
            val sku: String = sp.getString(TOKEN_SKU, "")!!
            return if (sku.isNotEmpty()) listOf(*sku.split(",").toTypedArray()) else emptyList()
        }

    var appVersionCode: Int
        set(value) = sp.put { putInt(APP_VERSION_CODE, value) }
        get() = sp.getInt(APP_VERSION_CODE, 0)

    var isSecretModeEnable: Boolean
        set(value) = sp.put { putBoolean(SECRET_MODE, value) }
        get() = sp.getBoolean(SECRET_MODE, false)


    var isLogoPositionChanged: Boolean
        set(value) = sp.put { putBoolean(LOGO_POSITION_CHANGED, value) }
        get() = sp.getBoolean(LOGO_POSITION_CHANGED, false)

    var isOnlineGalleryEnabled: Boolean
        set(value) = sp.put {
            putBoolean(ONLINE_GALLERY_ENABLED, value)
            onlineGalleryLiveData.postValue(value)
        }
        get() = sp.getBoolean(ONLINE_GALLERY_ENABLED, false)

    var onlineGalleryLiveData = MutableLiveData(isOnlineGalleryEnabled)

    val hasEclipseSetting: Boolean
        get() = sp.contains(ENABLE_ECLIPSE)

    var imageEditPath: String
        set(value) = sp.put { putString(EDIT_IMAGE_PATH, value) }
        get() = sp.getString(EDIT_IMAGE_PATH, "")!!


    var isMassage: Boolean
        set(value) = sp.put { putBoolean(MESSAGE, value) }
        get() = sp.getBoolean(MESSAGE, true)

    var showEclipseDialog: Boolean
        set(value) = sp.put { putBoolean(SHOW_ECLIPSE_DIALOG, value) }
        get() = sp.getBoolean(SHOW_ECLIPSE_DIALOG, true)

    var showMacroDialog: Boolean
        set(value) = sp.put { putBoolean(SHOW_MACRO_DIALOG, value) }
        get() = sp.getBoolean(SHOW_MACRO_DIALOG, true)

    var showGemLoupeDialog: Boolean
        set(value) = sp.put { putBoolean(SHOW_GEM_LOUPE_DIALOG, value) }
        get() = sp.getBoolean(SHOW_GEM_LOUPE_DIALOG, true)

    var showOnlineGallerySubaccountDialog: Boolean
        set(value) = sp.put { putBoolean(SHOW_ONLINE_GALLERY_SUBACCOUNT_DIALOG, value) }
        get() = sp.getBoolean(SHOW_ONLINE_GALLERY_SUBACCOUNT_DIALOG, true)

    var iis_url: String
        set(value) = sp.put { putString(IIS_URL, value) }
        get() = sp.getString(IIS_URL, "")!!

    var iis_workplace: String
        set(value) = sp.put { putString(IIS_WORKPLACE, value) }
        get() = sp.getString(IIS_WORKPLACE, "")!!

    var iis_language: String
        set(value) = sp.put { putString(IIS_LANGUAGE, value) }
        get() = sp.getString(IIS_LANGUAGE, "en")!!

    var iis_username: String
        set(value) = sp.put { putString(IIS_USERNAME, value) }
        get() = sp.getString(IIS_USERNAME, "")!!

    var iis_password: String
        set(value) = sp.put { putString(IIS_PASSWORD, value) }
        get() = sp.getString(IIS_PASSWORD, "")!!

    var isSharelinkFirstTime: Boolean
        set(value) = sp.put { putBoolean(SHARE_LINK_FIRST_TIME, value) }
        get() = sp.getBoolean(SHARE_LINK_FIRST_TIME, true)

    var isSharelinkShowAgain: Boolean
        set(value) = sp.put { putBoolean(SHARE_LINK_DIALOG_SHOW, value) }
        get() = sp.getBoolean(SHARE_LINK_DIALOG_SHOW, true)

    var isSharelinkBetaNotification: Boolean
        set(value) = sp.put { putBoolean(SHARE_LINK_NOTIFICATION_BETA, value) }
        get() = sp.getBoolean(SHARE_LINK_NOTIFICATION_BETA, true)

    var iis_picture_folders: Map<String, String>
        set(value) = sp.put { putString(IIS_PICTURE_FOLDER, Gson().toJson(value)) }
        get() = Gson().fromJson(sp.getString(IIS_PICTURE_FOLDER, "")!!) ?: emptyMap()

    var videoMode: String
        set(value) = sp.put { putString(VIDEO_MODE, value) }
        get() = Gson().fromJson(sp.getString(VIDEO_MODE, "360")!!) ?: "360"

    var videoSpeed: String
        set(value) = sp.put { putString(VIDEO_SPEED, value) }
        get() = Gson().fromJson(sp.getString(VIDEO_SPEED, "9")!!) ?: "9"

    var isBrokenBlinkFilesRemovedTest: Boolean
        set(value) = sp.put { putBoolean(BROKEN_BLINK_FILES_REMOVED_TEST, value) }
        get() = sp.getBoolean(BROKEN_BLINK_FILES_REMOVED_TEST, false)



    var isResetButtonEnabled: Boolean
        set(value) = sp.put { putBoolean(ENABLE_RESET_BUTTON, value) }
        get() = sp.getBoolean(ENABLE_RESET_BUTTON, false)

    var showConfirmEmail: Boolean
        set(value) = sp.put { putBoolean(CONFIRM_EMAIL_SHOW, value) }
        get() = sp.getBoolean(CONFIRM_EMAIL_SHOW, true)

    var filesRenamed: Boolean
        set(value) = sp.put { putBoolean(FILES_RENAMED, value) }
        get() = sp.getBoolean(FILES_RENAMED, true)

    var syncFilesChanged: Boolean
        set(value) = sp.put { putBoolean(SYNC_FILES_CHANGED, value) }
        get() = sp.getBoolean(SYNC_FILES_CHANGED, false)

    var loggedWithQR: Boolean
        set(value) = sp.put { putBoolean(LOGGED_WITH_QR, value) }
        get() = sp.getBoolean(LOGGED_WITH_QR, false)

    var bgRemoveTimes: Int
        set(value) = sp.put { putInt(BG_REMOVE_TIMES, value) }
        get() = sp.getInt(BG_REMOVE_TIMES, 0)

    var lastBgRemoveTime: Long
        set(value) = sp.put { putLong(LAST_BG_REMOVE_TIME, value) }
        get() = sp.getLong(LAST_BG_REMOVE_TIME, 0)

    var isWebLinkTutorialShown: Boolean
        set(value) = sp.put { putBoolean(WEB_LINK_TUTORIAL_SHOWN, value) }
        get() = sp.getBoolean(WEB_LINK_TUTORIAL_SHOWN, false)


    var appLastCheckUpdateVersion:Int
        set(value) = sp.put { putInt(APP_UPDATE_LAST_VERSION, value) }
        get() = sp.getInt(APP_UPDATE_LAST_VERSION, 0)


    var appLastCheckTime:Long
        set(value) = sp.put { putLong(APP_UPDATE_LAST_CHECK_TIME, value) }
        get() = sp.getLong(APP_UPDATE_LAST_CHECK_TIME, 0)

    private inline fun <reified T> Gson.fromJson(json: String) =
        this.fromJson<T>(json, object : TypeToken<T>() {}.type)

    fun clearSkuSticker() {
        sp.put {
            remove(SKU_TYPEFACE)
            remove(SKU_COLOR)
            remove(SKU_ALIGN)
            remove(SKU_SIZE)
            remove(SKU_STICKER)
        }
    }

    fun clearWatermarkSticker() {
        sp.put {
            remove(WATERMARK_ALIGN)
            remove(WATERMARK_COLOR)
            remove(WATERMARK_OPACITY)
            remove(WATERMARK_SIZE)
            remove(WATERMARK_STICKER)
            remove(WATERMARK_TEXT)
            remove(WATERMARK_TYPEFACE)
        }
    }


    fun clearLogo() {
        sp.put {
            remove(LOGO_LIST)
            remove(LOGO_ENABLE)
            remove(LOGO_INDEX)
        }
    }

    fun removeSubscription() {
        sp.put {
            remove(SUBSCRIBE_STATUS)
        }
    }

    fun removeEmail() {
        sp.put {
            remove(EMAIL_KEY)
        }
    }

    fun removeToken() {
        sp.put {
            remove(TOKEN_KEY)
            remove(API_TOKEN_KEY)
        }
    }

    fun removeBurstSettings() {
        sp.put {
            remove(BURST_MODEL_ENABED)
            remove(BURST_REMOVE_BACKGROUND_IMAGES_ENABLED)
            remove(BURST_REMOVE_BACKGROUND_VIDEO_ENABLED)
            remove(BURST_AI_RETOUCH_IMAGES_ENABLED)
            remove(BURST_AI_DESC_ENABLED)
        }
    }

    abstract var device: String

    abstract var watermarkSticker: FloatArray?

    abstract var skuSticker: FloatArray?
}

