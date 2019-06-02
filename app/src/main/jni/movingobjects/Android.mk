LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := almashot-moving
LOCAL_SRC_FILES := almashot-moving.cpp
LOCAL_STATIC_LIBRARIES := almalib gomp utils-image
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)
