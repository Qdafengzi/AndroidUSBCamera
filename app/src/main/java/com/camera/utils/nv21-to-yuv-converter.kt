import android.graphics.ImageFormat
import android.media.Image

object ImageFormatConverter {

    @Throws(IllegalStateException::class, NullPointerException::class)
    fun generateNV21Data(`$this$generateNV21Data`: Image): ByteArray {
        val crop = `$this$generateNV21Data`.cropRect
        val format = `$this$generateNV21Data`.format
        val width = crop.width()
        val height = crop.height()
        val planes = `$this$generateNV21Data`.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        var var10000 = planes[0]
        val rowData = ByteArray(var10000.rowStride)
        var channelOffset = 0
        var outputStride = 1
        var i = 0

        val var11 = planes.size
        while (i < var11) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }

                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }

                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            var10000 = planes[i]
            val buffer = var10000.buffer
            var10000 = planes[i]
            val rowStride = var10000.rowStride
            var10000 = planes[i]
            val pixelStride = var10000.pixelStride
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            var row = 0

            val var19 = h
            while (row < var19) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, w]
                    channelOffset += w
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    var col = 0

                    val var22 = w
                    while (col < var22) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                        ++col
                    }
                }

                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
                ++row
            }
            ++i
        }

        return data
    }

    /**
     * 将NV21格式转换为YUV420P格式
     * 
     * @param nv21 输入的NV21格式数据
     * @param width 图像宽度
     * @param height 图像高度
     * @return YUV420P格式的ByteArray
     */
    fun nv21ToYuv420p(nv21: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val yuv = ByteArray(frameSize * 3 / 2)
        
        // 复制Y平面
        System.arraycopy(nv21, 0, yuv, 0, frameSize)
        
        // 转换UV平面
        var i = 0
        for (j in frameSize until frameSize * 3 / 2 step 2) {
            yuv[frameSize + i] = nv21[frameSize + j + 1]  // U
            yuv[frameSize + i + frameSize / 4] = nv21[frameSize + j]  // V
            i++
        }
        
        return yuv
    }

    /**
     * 将YUV420P格式转换回NV21格式
     * 
     * @param yuv 输入的YUV420P格式数据
     * @param width 图像宽度
     * @param height 图像高度
     * @return NV21格式的ByteArray
     */
    fun yuv420pToNv21(yuv: ByteArray, width: Int, height: Int): ByteArray {
        val frameSize = width * height
        val nv21 = ByteArray(frameSize * 3 / 2)
        
        // 复制Y平面
        System.arraycopy(yuv, 0, nv21, 0, frameSize)
        
        // 转换UV平面
        var i = 0
        for (j in frameSize until frameSize * 3 / 2 step 2) {
            nv21[frameSize + j + 1] = yuv[frameSize + i]  // U
            nv21[frameSize + j] = yuv[frameSize + i + frameSize / 4]  // V
            i++
        }
        
        return nv21
    }
}
