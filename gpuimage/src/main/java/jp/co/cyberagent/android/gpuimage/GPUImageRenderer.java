package jp.co.cyberagent.android.gpuimage;

/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil.TEXTURE_NO_ROTATION;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Camera.PreviewCallback;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import com.gemlightbox.core.utils.XLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.Queue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.util.OpenGlUtils;
import jp.co.cyberagent.android.gpuimage.util.Rotation;
import jp.co.cyberagent.android.gpuimage.util.TextureRotationUtil;

public class GPUImageRenderer implements GLSurfaceView.Renderer, GLTextureView.Renderer {
    private static final int NO_IMAGE = -1;
    public static final float[] CUBE = {
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private GPUImageFilter filter;

    public final Object surfaceChangedWaiter = new Object();

    private int glTextureId = NO_IMAGE;
    private final FloatBuffer glCubeBuffer;
    private final FloatBuffer glTextureBuffer;
    private IntBuffer glRgbBuffer;

    private int outputWidth;
    private int outputHeight;
    private int imageWidth;
    private int imageHeight;
    private int addedPadding;

    private final Queue<Runnable> runOnDraw;
    private final Queue<Runnable> runOnDrawEnd;
    private Rotation rotation;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private GPUImage.ScaleType scaleType = GPUImage.ScaleType.CENTER_CROP;

    private float backgroundRed = 0;
    private float backgroundGreen = 0;
    private float backgroundBlue = 0;

    private PreviewCallback previewCallback;
    private DrawVideoListener drawVideoListener;

    public GPUImageRenderer(final GPUImageFilter filter) {
        this.filter = filter;
        runOnDraw = new LinkedList<>();
        runOnDrawEnd = new LinkedList<>();

        glCubeBuffer = ByteBuffer.allocateDirect(CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        glCubeBuffer.put(CUBE).position(0);

        glTextureBuffer = ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        setRotation(Rotation.NORMAL, false, false);
    }

    @Override
    public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
        GLES30.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, 1);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        filter.ifNeedInit();
    }


    public void imageOnSurfaceChanged(final int startX, final int startY, final int width, final int height) {
        XLogger.d("onSurfaceChanged------->width:" + width + " h" + height);
        outputWidth = width;
        outputHeight = height;
        GLES30.glViewport(startX, startY, width, height);
        GLES30.glUseProgram(filter.getProgram());
        filter.onOutputSizeChanged(width, height);
        adjustImageScaling();
        synchronized (surfaceChangedWaiter) {
            surfaceChangedWaiter.notifyAll();
        }
    }

    @Override
    public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
        XLogger.d("onSurfaceChanged------->width:"+width+" h"+height);
        outputWidth = width;
        outputHeight = height;
        GLES30.glViewport(0, 0, width, height);
        GLES30.glUseProgram(filter.getProgram());
        filter.onOutputSizeChanged(width, height);
        adjustImageScaling();
        synchronized (surfaceChangedWaiter) {
            surfaceChangedWaiter.notifyAll();
        }
    }

    @Override
    public void onDrawFrame(final GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        runAll(runOnDraw);
        filter.onDraw(glTextureId, glCubeBuffer, glTextureBuffer);
        runAll(runOnDrawEnd);
    }

    /**
     * Sets the background color
     *
     * @param red   red color value
     * @param green green color value
     * @param blue  red color value
     */
    public void setBackgroundColor(float red, float green, float blue) {
        backgroundRed = red;
        backgroundGreen = green;
        backgroundBlue = blue;
    }

    private void runAll(Queue<Runnable> queue) {
        synchronized (queue) {
            while (!queue.isEmpty()) {
                queue.poll().run();
            }
        }
    }

    public void onPreviewFrame1(final byte[] data, final int width, final int height) {
        if (glRgbBuffer == null || glRgbBuffer.capacity() < width * height) {
            // Allocate a new IntBuffer if necessary
            glRgbBuffer = IntBuffer.allocate(width * height);
        }

        // Convert byte[] to IntBuffer
        glRgbBuffer.clear();  // Clear the buffer before putting new data

        for (int i = 0; i < data.length; i += 4) {
            int r = data[i] & 0xFF;
            int g = data[i + 1] & 0xFF;
            int b = data[i + 2] & 0xFF;
            int a = data[i + 3] & 0xFF;
            int rgba = (r << 24) | (g << 16) | (b << 8) | a;
            glRgbBuffer.put(rgba);
        }

        glRgbBuffer.position(0);  // Reset buffer position to the beginning

        if (runOnDraw.isEmpty()) {
            runOnDraw(() -> {
                glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

                if (imageWidth != width || imageHeight != height) {
                    imageWidth = width;
                    imageHeight = height;
                    adjustImageScaling();
                }

                if (drawVideoListener != null) {
                    drawVideoListener.runVideoDraw();
                }
            });
        }
    }


    /**
     * 原始
     * @param data
     * @param width
     * @param height
     */
    public void onPreviewFrame(final byte[] data, final int width, final int height) {
        XLogger.d("onPreviewFrame----."+width +" "+height);
        if (glRgbBuffer == null) {
            glRgbBuffer = IntBuffer.allocate(width * height);
        }
        if (runOnDraw.isEmpty()) {
            runOnDraw(() -> {
                GPUImageNativeLibrary.YUVtoRBGA(data, width, height, glRgbBuffer.array());
                glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

                if (imageWidth != width) {
                    imageWidth = width;
                    imageHeight = height;
                    adjustImageScaling();
                }

                if (drawVideoListener != null) {
                    drawVideoListener.runVideoDraw();
                }
            });
        }
    }

    public void onPreviewFrame2(final byte[] data, final int width, final int height) {
        // 检查是否需要初始化 glRgbBuffer
        if (glRgbBuffer == null || glRgbBuffer.capacity() != width * height) {
            glRgbBuffer = IntBuffer.allocate(width * height);
        }

        if (runOnDraw.isEmpty()) {
            runOnDraw(() -> {
                // 将 RGBA 数据转换为 IntBuffer
                // 每个像素 4 字节 (RGBA) 转换为一个 int
                // 使用 ByteBuffer.wrap(data) 将字节数组包装成 ByteBuffer，然后转换为 IntBuffer
                ByteBuffer.wrap(data).asIntBuffer().get(glRgbBuffer.array());

                // 加载纹理
                glTextureId = OpenGlUtils.loadTexture(glRgbBuffer, width, height, glTextureId);

                // 检查是否需要调整图像缩放
                if (imageWidth != width) {
                    imageWidth = width;
                    imageHeight = height;
                    adjustImageScaling();
                }

                // 执行视频绘制监听器
                if (drawVideoListener != null) {
                    drawVideoListener.runVideoDraw();
                }
            });
        }
    }

    public void setFilter(final GPUImageFilter filter) {
        runOnDraw(() -> {
            final GPUImageFilter oldFilter = GPUImageRenderer.this.filter;
            GPUImageRenderer.this.filter = filter;
            if (oldFilter != null) {
                oldFilter.destroy();
            }
            GPUImageRenderer.this.filter.ifNeedInit();
            GLES30.glUseProgram(GPUImageRenderer.this.filter.getProgram());
            GPUImageRenderer.this.filter.onOutputSizeChanged(outputWidth, outputHeight);
        });
    }

    public void deleteImage() {
        runOnDraw(() -> {
            XLogger.d("setRatio---------->  2222");
            GLES30.glDeleteTextures(1, new int[]{glTextureId}, 0);
            glTextureId = NO_IMAGE;
        });
    }

    public void setImageBitmap(final Bitmap bitmap) {
        setImageBitmap(bitmap, true);
    }

    public void setImageBitmap(final Bitmap bitmap, final boolean recycle) {
        if (bitmap == null) {
            return;
        }

        runOnDraw(() -> {
            Bitmap resizedBitmap = null;
            if (bitmap.getWidth() % 2 == 1) {
                resizedBitmap = Bitmap.createBitmap(bitmap.getWidth() + 1, bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas can = new Canvas(resizedBitmap);
                can.drawARGB(0x00, 0x00, 0x00, 0x00);
                can.drawBitmap(bitmap, 0, 0, null);
                addedPadding = 1;
            } else {
                addedPadding = 0;
            }

            glTextureId = OpenGlUtils.loadTexture(resizedBitmap != null ? resizedBitmap : bitmap, glTextureId, recycle);
            if (resizedBitmap != null) {
                resizedBitmap.recycle();
            }
            imageWidth = bitmap.getWidth();
            imageHeight = bitmap.getHeight();
            adjustImageScaling();
            if (drawVideoListener != null) {
                drawVideoListener.runVideoDraw();
            }
        });
    }

    public void setScaleType(GPUImage.ScaleType scaleType) {
        this.scaleType = scaleType;
    }

    protected int getFrameWidth() {
        return outputWidth;
    }

    protected int getFrameHeight() {
        return outputHeight;
    }

    private void adjustImageScaling() {
        float outputWidth = this.outputWidth;
        float outputHeight = this.outputHeight;
        if (rotation == Rotation.ROTATION_270 || rotation == Rotation.ROTATION_90) {
            outputWidth = this.outputHeight;
            outputHeight = this.outputWidth;
        }

        float ratio1 = outputWidth / imageWidth;
        float ratio2 = outputHeight / imageHeight;
        float ratioMax = Math.max(ratio1, ratio2);
        int imageWidthNew = Math.round(imageWidth * ratioMax);
        int imageHeightNew = Math.round(imageHeight * ratioMax);

        float ratioWidth = imageWidthNew / outputWidth;
        float ratioHeight = imageHeightNew / outputHeight;

        float[] cube = CUBE;
        float[] textureCords = TextureRotationUtil.getRotation(rotation, flipHorizontal, flipVertical);
        if (scaleType == GPUImage.ScaleType.CENTER_CROP) {
            float distHorizontal = (1 - 1 / ratioWidth) / 2;
            float distVertical = (1 - 1 / ratioHeight) / 2;
            textureCords = new float[]{
                    addDistance(textureCords[0], distHorizontal), addDistance(textureCords[1], distVertical),
                    addDistance(textureCords[2], distHorizontal), addDistance(textureCords[3], distVertical),
                    addDistance(textureCords[4], distHorizontal), addDistance(textureCords[5], distVertical),
                    addDistance(textureCords[6], distHorizontal), addDistance(textureCords[7], distVertical),
            };
        } else {
            cube = new float[]{
                    CUBE[0] / ratioHeight, CUBE[1] / ratioWidth,
                    CUBE[2] / ratioHeight, CUBE[3] / ratioWidth,
                    CUBE[4] / ratioHeight, CUBE[5] / ratioWidth,
                    CUBE[6] / ratioHeight, CUBE[7] / ratioWidth,
            };
        }

        glCubeBuffer.clear();
        glCubeBuffer.put(cube).position(0);
        glTextureBuffer.clear();
        glTextureBuffer.put(textureCords).position(0);
    }

    private float addDistance(float coordinate, float distance) {
        return coordinate == 0.0f ? distance : 1 - distance;
    }

    public void setRotationCamera(final Rotation rotation, final boolean flipHorizontal,
                                  final boolean flipVertical) {
        setRotation(rotation, flipVertical, flipHorizontal);
    }

    public void setRotation(final Rotation rotation) {
        this.rotation = rotation;
        adjustImageScaling();
    }

    public void setRotation(final Rotation rotation,
                            final boolean flipHorizontal, final boolean flipVertical) {
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        setRotation(rotation);
    }

    public Rotation getRotation() {
        return rotation;
    }

    public boolean isFlippedHorizontally() {
        return flipHorizontal;
    }

    public boolean isFlippedVertically() {
        return flipVertical;
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (runOnDraw) {
            runOnDraw.add(runnable);
        }
    }

    protected void runOnDrawEnd(final Runnable runnable) {
        synchronized (runOnDrawEnd) {
            runOnDrawEnd.add(runnable);
        }
    }

    public void setDrawVideoListener(DrawVideoListener drawVideoListener) {
        this.drawVideoListener = drawVideoListener;
    }

    public interface DrawVideoListener {
        void runVideoDraw();
    }
}

