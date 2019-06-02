LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk

LOCAL_MODULE    := almashot-dro
LOCAL_SRC_FILES := almashot-dro.cpp
LOCAL_STATIC_LIBRARIES := almalib gomp utils-image
LOCAL_LDLIBS := -ldl -lz -llog

include $(BUILD_SHARED_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE := gbufferlib
#LOCAL_SRC_FILES := ../prebuilt/$(TARGET_ARCH_ABI)/libgbuffer.so
#include $(PREBUILT_SHARED_LIBRARY)
