LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := almalence-mp4editor
LOCAL_SRC_FILES := almalence-mp4editor.cpp
LOCAL_STATIC_LIBRARIES := bento4
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)