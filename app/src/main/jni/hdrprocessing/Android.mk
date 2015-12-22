LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := almashot-hdr
LOCAL_SRC_FILES := almashot-hdr.cpp
LOCAL_STATIC_LIBRARIES := almalib gomp utils-image
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)
