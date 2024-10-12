/**
 * yuv handle header
 *
 * @author Created by jiangdg on 2022/2/18
 */

#ifndef ANDROIDUSBCAMERA_YUV_H
#define ANDROIDUSBCAMERA_YUV_H
#ifdef __cplusplus
extern "C" {
#endif
void *yuv420spToNv21Internal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420spInternal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420spWithMirrorInternal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420pInternal(char* srcData, char* destData, int width, int height);
void *nv21ToYuv420pWithMirrorInternal(char* srcData, char* destData, int width, int height);
void *cropNv21Internal(char *srcData, int width, int height, float aspectRatio, char *desData,int *newWidth, int *newHeight);

#ifdef __cplusplus
};
#endif
#endif //ANDROIDUSBCAMERA_YUV_H
