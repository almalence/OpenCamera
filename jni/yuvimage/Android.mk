LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk
    
LOCAL_MODULE    := yuvimage
LOCAL_SRC_FILES := yuvimage.cpp
LOCAL_STATIC_LIBRARIES := jpeg
LOCAL_LDLIBS := -llog \
	$(call host-path, $(LOCAL_PATH)/../prebuilt/$(TARGET_ARCH_ABI)/libandroid_runtime.so)

include $(BUILD_SHARED_LIBRARY)