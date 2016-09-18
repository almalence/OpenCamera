LOCAL_PATH := $(call my-dir)

include $(LOCAL_PATH)/../Flags.mk
  
LOCAL_MODULE    := YuvBitmap
LOCAL_SRC_FILES := YuvBitmap.cpp
LOCAL_LDLIBS := \
	-L. -Ljni/$(TARGET_ARCH_ABI) \
	-llog \
	-landroid \
	-ljnigraphics
	
include $(BUILD_SHARED_LIBRARY)