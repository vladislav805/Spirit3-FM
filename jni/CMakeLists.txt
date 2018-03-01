cmake_minimum_required(VERSION 3.9)
project(jni)

set(CMAKE_CXX_STANDARD 11)

add_executable(jni
		plug/inc/android_fmradio.h
		plug/inc/fmradio.h
		plug/inc/hci.h
		plug/inc/hci_lib.h
		plug/acc_hci.c
		plug/plug.c
		plug/tnr_bch.c
		plug/tnr_gen.c
		plug/tnr_qcv.c
		plug/tnr_sdr.c
		plug/tnr_ssl.c
		alsa.c
		asound.h
		bt-hci.c
		hcd.c
		jut.c
		man_ver.h
		s2d.c
		ssd.c
		utils.c)
