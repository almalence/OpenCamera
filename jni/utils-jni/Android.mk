LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := utils-jni
LOCAL_SRC_FILES := ImageConversion.cpp
LOCAL_STATIC_LIBRARIES := utils-image gomp
LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)
