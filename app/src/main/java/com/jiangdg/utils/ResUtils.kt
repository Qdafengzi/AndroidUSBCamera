package com.jiangdg.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat


object ResUtils {


    fun getColor(context: Context,@ColorRes colorRes: Int): Int {
        return ContextCompat.getColor(context, colorRes)
    }

    fun getString(context: Context,@StringRes stringRes: Int): String {
        return context.getString(stringRes)
    }

    fun getString(context: Context,@StringRes stringRes: Int, from: Any?): String {
        return context.getString(stringRes, from)
    }

    fun getString(context: Context,@StringRes id: Int, vararg formatArgs: Any?): String {
        return context.getString(id, *formatArgs)
    }

    fun getDrawable(context: Context,@DrawableRes drawableRes: Int): Drawable? {
        return ContextCompat.getDrawable(context, drawableRes)
    }

    fun getDimensPx(context: Context,id: Int): Int {
        return context.resources.getDimensionPixelSize(id)
    }


    fun getTypeFace(context: Context,font: String): Typeface {
        return Typeface.createFromAsset(context.assets, "font/$font.ttf")
    }

    /**
     * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
     */
    fun dp2px(context: Context,dpValue: Float): Int {
        //获取手机的屏幕的密度
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    fun px2dp(context: Context,pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * px   sp
     */
    fun px2sp(context: Context,pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    fun sp2px(context: Context,spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    /**
     * 宽和高
     */
    val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels
    val screenHeight: Int
        get() =  Resources.getSystem().displayMetrics.heightPixels

    fun getDisplay(activity: Activity): DisplayMetrics {
        val outMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = activity.display
            display?.getRealMetrics(outMetrics)
        } else {
            @Suppress("DEPRECATION")
            val display = activity.windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(outMetrics)
        }
        return outMetrics
    }


    fun getScreenWidth(context: Context): Int {
        val resources: Resources = context.resources
        val dm: DisplayMetrics = resources.displayMetrics
        return dm.widthPixels
    }
}