LOCAL_PATH:= $(call my-dir)

    # ssd alsa utility:
include $(CLEAR_VARS)
LOCAL_MODULE    := libssd
LOCAL_SRC_FILES := ssd.c
include $(BUILD_EXECUTABLE)

    # s2d daemon:
include $(CLEAR_VARS)
LOCAL_MODULE    := libs2d
LOCAL_SRC_FILES := s2d.c
include $(BUILD_EXECUTABLE)

    # JNI utilities
include $(CLEAR_VARS)
LOCAL_MODULE:= jut
LOCAL_SRC_FILES:= jut.c
include $(BUILD_SHARED_LIBRARY)

    # QualComm Tuner Plugins:
include $(CLEAR_VARS)
LOCAL_SRC_FILES:= plug/tnr_qcv.c
LOCAL_MODULE:= libs2t_qcv
include $(BUILD_SHARED_LIBRARY)