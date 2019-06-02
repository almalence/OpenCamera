LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := almashot-sequence
LOCAL_SRC_FILES := almashot-sequence.cpp
LOCAL_STATIC_LIBRARIES := almalib gomp utils-image
LOCAL_LDLIBS := -L. -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)
