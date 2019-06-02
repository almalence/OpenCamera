LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := utils-image
LOCAL_SRC_FILES := ImageConversionUtils.cpp
LOCAL_STATIC_LIBRARIES := jpeg gomp
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)
