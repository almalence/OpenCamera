LOCAL_PATH := $(call my-dir)

MY_CORE_PATH := $(LOCAL_PATH)


# -------------------- Pre-built commonly-used libraries

# GNU Open MP library
include $(CLEAR_VARS)
LOCAL_MODULE := gomp
LOCAL_SRC_FILES := prebuilt/$(TARGET_ARCH_ABI)/libgomp.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include/openmp
include $(PREBUILT_STATIC_LIBRARY)

# libjpeg-turbo library
include $(CLEAR_VARS)
LOCAL_MODULE := jpeg
LOCAL_SRC_FILES := prebuilt/$(TARGET_ARCH_ABI)/libjpeg.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/include/libjpeg
include $(PREBUILT_STATIC_LIBRARY)

# Exiv2 library
#include $(CLEAR_VARS)
#LOCAL_MODULE    := exiv2
#LOCAL_SRC_FILES :=  prebuilt/$(TARGET_ARCH_ABI)/libexiv2.so
#include $(PREBUILT_SHARED_LIBRARY)

# Open CV libraries
include $(CLEAR_VARS)
LOCAL_MODULE := opencv_features2d
LOCAL_SRC_FILES := prebuilt/OpenCV/libs/$(TARGET_ARCH_ABI)/libopencv_features2d.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_imgproc
LOCAL_SRC_FILES := prebuilt/OpenCV/libs/$(TARGET_ARCH_ABI)/libopencv_imgproc.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_calib3d
LOCAL_SRC_FILES := prebuilt/OpenCV/libs/$(TARGET_ARCH_ABI)/libopencv_calib3d.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_video
LOCAL_SRC_FILES := prebuilt/OpenCV/libs/$(TARGET_ARCH_ABI)/libopencv_video.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_flann
LOCAL_SRC_FILES := prebuilt/OpenCV/libs/$(TARGET_ARCH_ABI)/libopencv_flann.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_core
LOCAL_SRC_FILES := prebuilt/OpenCV/libs/$(TARGET_ARCH_ABI)/libopencv_core.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := opencv_tbb
LOCAL_SRC_FILES := prebuilt/OpenCV/3rdparty/libs/$(TARGET_ARCH_ABI)/libtbb.a
include $(PREBUILT_STATIC_LIBRARY)


# -------------------- Plugins / helpers native code

# Almalence Almashot shared library
include $(MY_CORE_PATH)/almashot/Android.mk

# Image Conversion and other utilities - implementation
include $(MY_CORE_PATH)/utils/Android.mk

# Image Conversion and other utilities - interface to Java
include $(MY_CORE_PATH)/utils-jni/Android.mk

# DRO plugin
include $(MY_CORE_PATH)/dro/Android.mk

# Bestshot plugin
include $(MY_CORE_PATH)/bestshot/Android.mk

# Night plugin
include $(MY_CORE_PATH)/nightprocessing/Android.mk

# HDR plugin
include $(MY_CORE_PATH)/hdrprocessing/Android.mk

# Object Removal plugin
include $(MY_CORE_PATH)/objectremoval/Android.mk

# Sequence photo plugin
include $(MY_CORE_PATH)/sequence/Android.mk

# Group shot plugin
include $(MY_CORE_PATH)/groupshot/Android.mk

# Panorama plugin
include $(MY_CORE_PATH)/panorama/Android.mk

# High-resolution portrait plugin
include $(MY_CORE_PATH)/hiresportrait/Android.mk

# Pre-shot plugin
include $(MY_CORE_PATH)/preshot/Android.mk

# Histogram plugin
include $(MY_CORE_PATH)/histogram/Android.mk

# yuvimage helper
include $(MY_CORE_PATH)/yuvimage/Android.mk

# swapheap helper (move data between native and java heaps)
include $(MY_CORE_PATH)/swapheap/Android.mk
