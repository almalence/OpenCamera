LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := histogram
LOCAL_SRC_FILES := histogram.cpp
LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)
