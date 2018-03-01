#define LOGTAG "s2jut...."

#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <jni.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#define NELEM(x) (sizeof (x)/sizeof (*(x)))
#include <math.h>
#include <fcntl.h>
#include <android/log.h>
#include <stdarg.h>
#include <sys/time.h>
#include <sys/resource.h>

#define  loge(...)  fm_log_print(ANDROID_LOG_ERROR, LOGTAG,__VA_ARGS__)
#define  logd(...)  fm_log_print(ANDROID_LOG_DEBUG, LOGTAG,__VA_ARGS__)
#define DEF_BUF 256

int no_log = 0;
void* log_hndl = NULL;
const char * copyright = "Copyright (c) 2011-2014 Michael A. Reid. All rights reserved.";
char prop_buf[DEF_BUF] = "";

int (*do_log)(int prio, const char * tag, const char * fmt, va_list ap);
int fm_log_print(int prio, const char * tag, const char * fmt, ...) {

	if (no_log) {
		return -1;
	}

	va_list ap;
	va_start(ap, fmt);

	if (log_hndl == NULL) {
		log_hndl = dlopen("liblog.so", RTLD_LAZY);
		if (log_hndl == NULL) {
			no_log = 1; // Don't try again
			return -1;
		}
		do_log = dlsym(log_hndl, "__android_log_vprint");
		if (do_log == NULL) {
			no_log = 1; // Don't try again
			return -1;
		}
	}
	do_log(prio, tag, fmt, ap);
	return 0;
}



jint Java_fm_a2d_sf_svc_1aud_native_1priority_1set(JNIEnv * env, jobject thiz, jint new_priority) {
	logd("native_priority_set new_priority: %d", new_priority);
	int priority = 0;
	priority = getpriority(PRIO_PROCESS, 0);
	logd("native_priority_set priority: %d  errno: %d", priority, errno);

	priority = setpriority(PRIO_PROCESS, 0, new_priority);//-19);
	logd("native_priority_set priority: %d  errno: %d", priority, errno);

	priority = getpriority(PRIO_PROCESS, 0);
	logd("native_priority_set priority: %d  errno: %d", priority, errno);
	return 0;
}

jint Java_fm_a2d_sf_svc_1aud_native_1prop_1get(JNIEnv * env, jobject thiz, jint prop) {
//jint Java_fm_a2d_sf_com_1uti_native_1prop_1get (JNIEnv * env, jobject thiz, jint prop) {
	__system_property_get("ro.modversion", prop_buf);
	logd("native_prop_get %d: %s", prop, prop_buf);
	return !strncasecmp(prop_buf, "omni", 4); // If OmniROM
}