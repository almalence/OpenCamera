LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := swapheap
LOCAL_SRC_FILES := swapheap.cpp
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
