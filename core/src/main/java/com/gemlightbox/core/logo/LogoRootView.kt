package com.gemlightbox.core.logo

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.gemlightbox.core.R

class LogoRootView(context: Context?) : FrameLayout(context!!) {

    var _xDelta: Int = 0
    var _yDelta: Int = 0
    var oldX: Float = 0.toFloat()
    var oldY: Float = 0.toFloat()
    var oldRotate = 0f

    val iv_logo: LogoImageView
    val iv_scale: ImageView
    val iv_delete: ImageView
    val iv_rotate: ImageView
    val iv_select_position: ImageView
    val iv_selected_background: ImageView

    var logoBitmap: Bitmap?
        set(value) {
            iv_logo.logoBitmap = value
        }
        get() = iv_logo.logoBitmap

    init {
        inflate(context, R.layout.view_logo_root, this)
        iv_scale = findViewById(R.id.iv_scale)
        iv_logo = findViewById(R.id.iv_logo)
        iv_delete = findViewById(R.id.iv_delete)
        iv_rotate = findViewById(R.id.iv_rotate)
        iv_select_position = findViewById(R.id.iv_select_position)
        iv_selected_background = findViewById(R.id.iv_selected_background)
    }


    fun hideAddictiveButtons() {
        iv_scale.gone()
        iv_delete.gone()
        iv_rotate.gone()
        iv_select_position.gone()
        iv_selected_background.gone()
    }

    fun showDefaultButtons() {
        iv_scale.visible()
        iv_delete.visible()
        iv_rotate.visible()
        iv_select_position.gone()
        iv_selected_background.visible()
    }


    private fun View.gone() {
        this.visibility = View.GONE
    }

    private fun View.visible() {
        this.visibility = View.VISIBLE
    }
}