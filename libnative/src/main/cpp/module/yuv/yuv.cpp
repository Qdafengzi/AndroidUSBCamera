/**
 *  yuv handle
 *
 *  NV21：YYYYYYYY VUVU
 *  YV12：YYYYYYYY VV UU
 *  YUV420sp：YYYYYYYY UVUV
 *  YUV420p：YYYYYYYY UU VV
 *
 * @author Created by jiangdg on 2022/2/18
 */
#include "yuv.h"
#include <cstring>
#include <algorithm>

void *yuv420spToNv21Internal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + 2*i+1] = srcData[yLength + 2 * i];
        destData[yLength + 2*i] = srcData[yLength + 2*i+1];
    }
    return nullptr;
}

void *nv21ToYuv420spInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + 2 * i] = srcData[yLength + 2*i+1];
        destData[yLength + 2*i+1] = srcData[yLength + 2*i];
    }
    return nullptr;
}

void *nv21ToYuv420spWithMirrorInternal(char* srcData, char* destData, int width, int height) {
    return nullptr;
}

void *nv21ToYuv420pInternal(char* srcData, char* destData, int width, int height) {
    int yLength = width * height;
    int uLength = yLength / 4;
    memcpy(destData,srcData,yLength);
    for(int i=0; i<yLength/4; i++) {
        destData[yLength + i] = srcData[yLength + 2*i + 1];
        destData[yLength + uLength + i] = srcData[yLength + 2*i];
    }
    return nullptr;
}

void *nv21ToYuv420pWithMirrorInternal(char *srcData, char *destData, int width, int height) {
    return nullptr;
}

void *cropNv21Internal(char* srcData, int width, int height, float aspectRatio, char* desData, int& newWidth, int& newHeight) {
    // 根据宽高比计算新的宽度和高度
    if (aspectRatio == 1.0f) {
        // 1:1 宽高比
        newWidth = std::min(width, height);
        newHeight = newWidth;
    } else {
        if (static_cast<float>(width) / height > aspectRatio) {
            newHeight = height;
            newWidth = static_cast<int>(height * aspectRatio);
        } else {
            newWidth = width;
            newHeight = static_cast<int>(width / aspectRatio);
        }
    }

    // 计算裁剪起始点
    int cropX = (width - newWidth) / 2;
    int cropY = (height - newHeight) / 2;

    // 遍历裁剪区域的高度
    for (int y = 0; y < newHeight; ++y) {
        // 裁剪Y平面
        int srcYPos = (cropY + y) * width + cropX;
        int dstYPos = y * newWidth;
        std::memcpy(desData + dstYPos, srcData + srcYPos, newWidth);

        // 裁剪UV平面（UV平面每两行处理一次）
        if (y % 2 == 0) {
            int srcUVPos = width * height + ((cropY / 2) + (y / 2)) * width + cropX;
            int dstUVPos = newWidth * newHeight + (y / 2) * newWidth;
            std::memcpy(desData + dstUVPos, srcData + srcUVPos, newWidth);
        }
    }
}