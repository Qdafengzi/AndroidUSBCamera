package com.camera.demo

import android.graphics.Bitmap

object Utils {

    fun calculateAverageColor(bitmap: Bitmap): FloatArray {
        var totalRed: Long = 0
        var totalGreen: Long = 0
        var totalBlue: Long = 0
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = width * height

        // 遍历图像的每一个像素
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = bitmap.getPixel(x, y)
                totalRed += ((color shr 16) and 0xff).toLong() // 红色通道
                totalGreen += ((color shr 8) and 0xff).toLong() // 绿色通道
                totalBlue += (color and 0xff).toLong() // 蓝色通道
            }
        }

        // 计算每个通道的平均值
        val avgRed = totalRed.toFloat() / totalPixels
        val avgGreen = totalGreen.toFloat() / totalPixels
        val avgBlue = totalBlue.toFloat() / totalPixels

        return floatArrayOf(avgRed, avgGreen, avgBlue)
    }
}