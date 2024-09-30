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
package com.jiangdg.ausbc.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.jiangdg.ausbc.utils.Logger
import kotlin.math.abs

/** Adaptive SurfaceView
 * Aspect ratio (width:height, such as 4:3, 16:9).
 *
 * @author Created by jiangdg on 2022/01/23
 */
class AspectRatioSurfaceView: SurfaceView, IAspectRatio {

    private var mAspectRatio = -1.0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(context, attributeSet, defStyleAttr){

    }

    private fun applyCropTransform(viewWidth: Int, viewHeight: Int, previewSize: Size, targetRatioWidth: Float, targetRatioHeight: Float) {
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize.width.toFloat(), previewSize.height.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        // Calculate the target aspect ratio
        val targetAspectRatio = targetRatioWidth / targetRatioHeight
        val bufferAspectRatio = bufferRect.width() / bufferRect.height()

        if (bufferAspectRatio > targetAspectRatio) {
            // Wider than target
            val scale = viewHeight.toFloat() / bufferRect.height()
            matrix.setRectToRect(bufferRect, viewRect, Matrix.ScaleToFit.CENTER)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postTranslate((viewWidth - viewHeight * targetAspectRatio) / 2f, 0f)
        } else {
            // Taller than target
            val scale = viewWidth.toFloat() / bufferRect.width()
            matrix.setRectToRect(bufferRect, viewRect, Matrix.ScaleToFit.CENTER)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postTranslate(0f, (viewHeight - viewWidth / targetAspectRatio) / 2f)
        }
    }

    override fun setAspectRatio(width: Int, height: Int) {
        post {
//            val orientation = context.resources.configuration.orientation
//        // 处理竖屏和横屏情况
//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            setAspectRatio(height.toDouble() / width)
//            return
//        }
            setAspectRatio(width.toDouble() / height)
        }
    }

    override fun getSurfaceWidth(): Int  = measuredWidth

    override fun getSurfaceHeight(): Int  = measuredHeight

    override fun getSurface(): Surface = holder.surface

    override fun postUITask(task: () -> Unit) {
        post {
            task()
        }
    }

    private fun setAspectRatio(aspectRatio: Double) {
        if (aspectRatio < 0 || mAspectRatio == aspectRatio) {
            return
        }
        mAspectRatio = aspectRatio
        Logger.i(TAG, "AspectRatio = $mAspectRatio")
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var initialWidth = MeasureSpec.getSize(widthMeasureSpec)
        var initialHeight = MeasureSpec.getSize(heightMeasureSpec)
        val horizontalPadding = paddingLeft - paddingRight
        val verticalPadding = paddingTop - paddingBottom
        initialWidth -= horizontalPadding
        initialHeight -= verticalPadding
        // 比较预览与TextureView(内容)纵横比
        // 如果有变化，重新设置TextureView尺寸
        val viewAspectRatio = initialWidth.toDouble() / initialHeight
        val diff = mAspectRatio / viewAspectRatio - 1
        var wMeasureSpec = widthMeasureSpec
        var hMeasureSpec = heightMeasureSpec
        if (mAspectRatio > 0 && abs(diff) > 0.01) {
            // diff > 0， 按宽缩放
            // diff < 0， 按高缩放
            if (diff > 0) {
                initialHeight = (initialWidth / mAspectRatio).toInt()
            } else {
                initialWidth = (initialHeight * mAspectRatio).toInt()
            }
            // 重新设置TextureView尺寸
            // 注意加回padding大小
            initialWidth += horizontalPadding
            initialHeight += verticalPadding
            wMeasureSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
            hMeasureSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
        }
        super.onMeasure(wMeasureSpec, hMeasureSpec)
    }

    companion object {
        private const val TAG = "AspectRatioTextureView"
    }
}