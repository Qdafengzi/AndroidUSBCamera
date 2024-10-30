package com.camera.demo

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.camera.utils.XLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream

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

    fun convertRGBXToBitmap(rgbxData: ByteArray, width: Int, height: Int): Bitmap {
        // 每个像素四个字节：RGBX
        val pixels = IntArray(width * height)

        for (i in pixels.indices) {
            val r = rgbxData[i * 4].toInt() and 0xFF // 红色通道
            val g = rgbxData[i * 4 + 1].toInt() and 0xFF // 绿色通道
            val b = rgbxData[i * 4 + 2].toInt() and 0xFF // 蓝色通道

            // int x = rgbxData[i * 4 + 3] & 0xFF; // 未使用的字节

            // 构建 ARGB 颜色，A（透明度）设为 255（不透明）
            pixels[i] = 0xFF shl 24 or (r shl 16) or (g shl 8) or b
        }

        // 创建一个 Bitmap 对象
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return bitmap
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val uri: Uri? = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (uri != null) {
            var outputStream: OutputStream? = null
            try {
                outputStream = context.contentResolver.openOutputStream(uri)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                outputStream?.close()
            }
        }
        return uri
    }

    fun saveVideoToGallery(context: Context, videoFile: File, fileName: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES)
        }

        val uri: Uri? = context.contentResolver.insert(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (uri != null) {
            var outputStream: OutputStream? = null
            var inputStream: FileInputStream? = null
            try {
                outputStream = context.contentResolver.openOutputStream(uri)
                inputStream = FileInputStream(videoFile)

                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream?.write(buffer, 0, bytesRead)
                }
            } catch (e: Exception) {
                XLogger.d("录制的视频 error:${e.message}")
                e.printStackTrace()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }
        return uri
    }

    fun getVideoFrameRate(videoFilePath: String?) {
        val extractor = MediaExtractor()
        try {
            // 设置数据源
            extractor.setDataSource(videoFilePath!!)
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime!!.startsWith("video/")) {
                    // 获取帧率
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        val frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                        XLogger.d("Frame Rate: $frameRate fps")
                    } else {
                        XLogger.d("Frame Rate information not available.")
                    }
                    break // 找到视频轨道后无需继续
                }
            }
        } catch (e: java.lang.Exception) {
            XLogger.d("Error extracting video info" + e.message)
        } finally {
            extractor.release()
        }
    }

    fun saveImageToGallery1(context: Context,bitmap: Bitmap, albumName: String) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val write = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (write) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + albumName)
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + File.separator + albumName
                val image = File(imagesDir, filename)
                put(MediaStore.MediaColumns.DATA, image.absolutePath)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        try {
            uri?.let {
                context.contentResolver.openOutputStream(it).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream!!)
                }
            } ?: throw IOException("Failed to create new MediaStore record.")
        } catch (e: Exception) {
            e.printStackTrace()
        }finally {
//            MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
            context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        }
    }

    fun refreshGallery2(context: Context, filePath: String) {
        MediaScannerConnection.scanFile(context, arrayOf(filePath), null) { path, uri ->
            // MediaScannerConnection is complete
            XLogger.d("拍照完成:${path}")
        }
    }

    fun yuv420ToBitmap(yuv420Data: ByteArray, width: Int, height: Int): Bitmap? {
        // 创建 YuvImage 对象
        val yuvImage = YuvImage(yuv420Data, ImageFormat.NV21, width, height, null)

        // 使用 ByteArrayOutputStream 将 YuvImage 转换为 Bitmap
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 100, outputStream)

        // 将输出流转换为 Bitmap
        val bitmapArray = outputStream.toByteArray()
        return BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.size)
    }

    data class CroppedNV21(
        val data: ByteArray,
        val width: Int,
        val height: Int
    )

    fun cropNV21(
        data: ByteArray?,
        width: Int,
        height: Int,
        aspectRatio: Float
    ): CroppedNV21? {
        if (data == null) return null

        // Calculate the new width and height based on the aspect ratio
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio == 1.0f) {
            // For 1:1 aspect ratio
            newWidth = minOf(width, height)
            newHeight = newWidth
        } else {
            if (width.toFloat() / height > aspectRatio) {
                newHeight = height
                newWidth = (height * aspectRatio).toInt()
            } else {
                newWidth = width
                newHeight = (width / aspectRatio).toInt()
            }
        }

        // Calculate the starting points for cropping
        val cropX = (width - newWidth) / 2
        val cropY = (height - newHeight) / 2

        // Calculate size of the cropped frame
        val croppedSize = newWidth * newHeight * 3 / 2
        val croppedData = ByteArray(croppedSize)

        // Iterate over the height of the crop area
        for (y in 0 until newHeight) {
            // Crop Y plane
            val srcYPos = (cropY + y) * width + cropX
            val dstYPos = y * newWidth
            System.arraycopy(data, srcYPos, croppedData, dstYPos, newWidth)

            // Crop UV plane (only process every second row for UV)
            if (y % 2 == 0) {
                val srcUVPos = width * height + ((cropY / 2) + (y / 2)) * width + cropX
                val dstUVPos = newWidth * newHeight + (y / 2) * newWidth
                System.arraycopy(data, srcUVPos, croppedData, dstUVPos, newWidth)
            }
        }

        return CroppedNV21(croppedData, newWidth, newHeight)
    }
}