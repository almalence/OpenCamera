LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := almashot-clr
LOCAL_SRC_FILES := almashot-clr.cpp
LOCAL_STATIC_LIBRARIES := almalib gomp utils-image
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)
