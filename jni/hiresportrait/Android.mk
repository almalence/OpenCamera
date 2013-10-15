LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := hiresportrait
LOCAL_SRC_FILES := hiresportrait.cpp
LOCAL_STATIC_LIBRARIES := almalib gomp
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)