LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := almashot-fstacking
LOCAL_SRC_FILES := almashot-fstacking.cpp
LOCAL_CFLAGS += -DLOG_ON
LOCAL_STATIC_LIBRARIES := almalib gomp
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)
