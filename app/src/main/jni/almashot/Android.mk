LOCAL_PATH := $(call my-dir)

# Almalence AlmaShot library
include $(CLEAR_VARS)
LOCAL_MODULE := almalib-static
ifeq ($(wildcard $(MY_CORE_PATH)/almashot/$(TARGET_ARCH_ABI)/libalmalib.a),) 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libalmalib_eval.a
else 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libalmalib.a
endif
LOCAL_EXPORT_C_INCLUDES := $(MY_CORE_PATH)/include/almashot
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
include $(LOCAL_PATH)/../Flags.mk
LOCAL_MODULE := almalib
# without this stub.cpp shared library will not build (seem like gcc will be used instead of g++)
LOCAL_SRC_FILES := stub.cpp
LOCAL_WHOLE_STATIC_LIBRARIES :=  almalib-static
LOCAL_STATIC_LIBRARIES :=  gomp opencv_features2d opencv_imgproc opencv_calib3d opencv_video opencv_flann opencv_core opencv_tbb
LOCAL_LDLIBS := -ldl -lz -llog -lEGL -lGLESv2
LOCAL_EXPORT_C_INCLUDES := $(MY_CORE_PATH)/include/almashot
include $(BUILD_SHARED_LIBRARY)
