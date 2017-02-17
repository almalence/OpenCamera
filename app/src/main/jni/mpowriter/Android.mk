LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := mpo-writer
LOCAL_SRC_FILES := mpo-writer.cpp
LOCAL_STATIC_LIBRARIES := jpeg gomp
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)
