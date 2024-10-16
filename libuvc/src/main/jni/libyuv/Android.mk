LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := yuv
LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS := -Wall -fexceptions
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)/include
# LOCAL_SHARED_LIBRARIES += jpeg-turbo1500_static
# LOCAL_CFLAGS += -DHAVE_JPEG

LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := \
	source/compare.cc           \
    source/compare_common.cc    \
    source/convert.cc           \
    source/convert_jpeg.cc      \
    source/convert_argb.cc      \
    source/convert_from.cc      \
    source/convert_from_argb.cc \
    source/convert_to_argb.cc   \
    source/convert_to_i420.cc   \
    source/cpu_id.cc            \
    source/planar_functions.cc  \
    source/rotate.cc            \
    source/rotate_any.cc        \
    source/rotate_argb.cc       \
    source/rotate_common.cc     \
    source/row_any.cc           \
    source/row_common.cc        \
    source/scale.cc				\
    source/scale_any.cc         \
    source/scale_argb.cc        \
    source/scale_common.cc      \
    source/video_common.cc

ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), x86 x86_64))
    LOCAL_SRC_FILES += \
        source/compare_gcc.cc     \
        source/rotate_gcc.cc      \
        source/row_gcc.cc         \
        source/scale_gcc.cc

else ifeq ($(TARGET_ARCH_ABI),$(filter $(TARGET_ARCH_ABI), armeabi armeabi-v7a))
    LOCAL_CFLAGS += -DLIBYUV_NEON
    LOCAL_SRC_FILES += \
        source/compare_neon.cc    \
        source/rotate_neon.cc     \
        source/row_neon.cc        \
        source/scale_neon.cc

else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_CFLAGS += -DLIBYUV_NEON
    LOCAL_SRC_FILES += \
        source/compare_neon64.cc    \
        source/rotate_neon64.cc     \
        source/row_neon64.cc        \
        source/scale_neon64.cc 

endif

include $(BUILD_SHARED_LIBRARY)
