package com.gemlightbox.core.preference

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.gemlightbox.core.preference.core.GemlightBoxPreferences
import com.gemlightbox.core.preference.core.Preference
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONArray

class GemlightBoxPreferencesImpl(val app: Application) : GemlightBoxPreferences(app) {

    override var device: String
        set(value) = sp.put { putString(Preference.DEVICE_ID, value) }
        @SuppressLint("HardwareIds")
        get() {
            var deviceId: String? = sp.getString(Preference.DEVICE_ID, "")
            if (deviceId.isNullOrEmpty()) {
                deviceId = "ID " + Settings.Secure.getString(
                    app.contentResolver,
                    Settings.Secure.ANDROID_ID
                )
                device = deviceId
            }
            return deviceId
        }
    override var watermarkSticker: FloatArray?
        get() {
            val f = FloatArray(9)
            try {
                if (sp.getString(Preference.WATERMARK_STICKER, "").isNullOrEmpty()) {
                    return null
                }
                val jsonArray2 = JSONArray(sp.getString(Preference.WATERMARK_STICKER, ""))
                for (i in 0 until jsonArray2.length()) {
                    f[i] = java.lang.Float.valueOf(jsonArray2.getString(i))
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                e.printStackTrace()
            }
            return f
        }
        set(value) {
            val jsonArray = JSONArray()
            for (num in value!!) {
                jsonArray.put(num.toString())
            }

            sp.put { putString(Preference.WATERMARK_STICKER, jsonArray.toString()) }

        }
    override var skuSticker: FloatArray?
        get() {
            val f = FloatArray(9)
            try {
                if (sp.getString(Preference.SKU_STICKER, "").isNullOrEmpty()) {
                    return null
                }
                val jsonArray2 = JSONArray(sp.getString(Preference.SKU_STICKER, ""))
                for (i in 0 until jsonArray2.length()) {
                    f[i] = java.lang.Float.valueOf(jsonArray2.getString(i))
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
                e.printStackTrace()
            }
            return f
        }
        set(value) {
            val jsonArray = JSONArray()
            for (num in value!!) {
                jsonArray.put(num.toString())
            }

            sp.put { putString(Preference.SKU_STICKER, jsonArray.toString()) }

        }
}