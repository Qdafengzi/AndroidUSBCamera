package com.gemlightbox.core.preference

import android.content.Context
import com.gemlightbox.core.entity.ShootType
import com.gemlightbox.core.logo.LogoItem
import com.gemlightbox.core.preference.core.CameraPreferences
import com.gemlightbox.core.preference.core.Preference.*
import com.gemlightbox.core.utils.CameraFilterConstants
import com.gemlightbox.ext.toObject

class CameraPreferencesImpl(context: Context) : CameraPreferences(context) {

    override var shootType: ShootType
        set(value) = sp.edit().putString(CAMERA_SHOOT_TYPE, value.name).apply()
        get() = when (sp.getString(CAMERA_SHOOT_TYPE, null)) {
            ShootType.VIDEOS.name -> ShootType.VIDEOS
            else -> ShootType.PHOTOS
        }

    override var brightness: Int
        get() {
            when (savedFilter) {
                CameraFilterConstants.FILTER_TYPE_ECLIPSE, CameraFilterConstants.FILTER_TYPE_PRO_ECLIPSE -> return sp.getInt(
                    WHITE_BG_ECLIPSE,
                    50
                )
                CameraFilterConstants.FILTER_TYPE_MACRO -> return sp.getInt(
                    WHITE_BG_MACRO,
                    50
                )
                CameraFilterConstants.FILTER_TYPE_GEMLOUPE -> return sp.getInt(
                    WHITE_BG_GEMLOUPE,
                    50
                )
            }
            return sp.getInt(WHITE_BG_AUTO, 50)
        }
        set(value) {
            when (savedFilter) {
                CameraFilterConstants.FILTER_TYPE_AUTO, CameraFilterConstants.FILTER_TYPE_PRO -> sp.put {
                    putInt(WHITE_BG_AUTO, value)
                }
                CameraFilterConstants.FILTER_TYPE_ECLIPSE, CameraFilterConstants.FILTER_TYPE_PRO_ECLIPSE -> sp.put {
                    putInt(WHITE_BG_ECLIPSE, value)
                }
                CameraFilterConstants.FILTER_TYPE_MACRO -> sp.put {
                    putInt(WHITE_BG_MACRO, value)
                }
                CameraFilterConstants.FILTER_TYPE_GEMLOUPE -> sp.put {
                    putInt(WHITE_BG_GEMLOUPE, value)
                }
            }
        }

    override var tone: Int
        get() {
            when (savedFilter) {
                CameraFilterConstants.FILTER_TYPE_ECLIPSE, CameraFilterConstants.FILTER_TYPE_PRO_ECLIPSE -> return sp.getInt(
                    FILTER_TONE_ECLIPSE,
                    30
                )
                CameraFilterConstants.FILTER_TYPE_MACRO -> return sp.getInt(
                    FILTER_TONE_MACRO,
                    30
                )
            }
            return sp.getInt(FILTER_TONE_AUTO, 30)
        }
        set(value) {
            when (savedFilter) {
                CameraFilterConstants.FILTER_TYPE_AUTO, CameraFilterConstants.FILTER_TYPE_PRO -> sp.put {
                    putInt(FILTER_TONE_AUTO, value)
                }
                CameraFilterConstants.FILTER_TYPE_ECLIPSE, CameraFilterConstants.FILTER_TYPE_PRO_ECLIPSE -> sp.put {
                    putInt(FILTER_TONE_ECLIPSE, value)
                }
                CameraFilterConstants.FILTER_TYPE_MACRO -> sp.put {
                    putInt(FILTER_TONE_MACRO, value)
                }
            }
        }

    override fun isLogoEmpty(): Boolean = logoList?.toObject<List<LogoItem>>()?.isEmpty() ?: true
}