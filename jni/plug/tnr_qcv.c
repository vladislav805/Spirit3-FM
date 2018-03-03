#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <pthread.h>
#include <unistd.h>
#include <dirent.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <stdarg.h>
#include <math.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/videodev2.h>
#include <android/log.h>
#include <sys/system_properties.h>

#define LOGTAG "s2tnr_qcv"

#include "jni.h"
#include "inc/android_fmradio.h"
#include "plug.c"


#define EVT_LOCK_BYPASS // Locking causes problems at beginning due to blocking iris_buf_get()

#define loge(...)  fm_log_print(ANDROID_LOG_ERROR, LOGTAG,__VA_ARGS__)
#define logd(...)  fm_log_print(ANDROID_LOG_DEBUG, LOGTAG,__VA_ARGS__)

// Functions called from this chip specific code to generic code:
int ms_sleep(int ms);


int fm_log_print(int prio, const char * tag, const char * fmt, ...);
int __system_property_set(const char *key, const char *value);

extern int extra_log;// = 0;


// FM Chip specific code

// Обработчик/дескриптор /dev/radio0
int dev_hndl = -1;

// Частоты
// 0 = EU, 1 = US
int intern_band = 0;

extern int curr_freq_val;
extern int curr_freq_inc;
extern int curr_freq_lo;
extern int curr_freq_hi;

// #define V4L2_CAP_RDS_CAPTURE            0x00000100  /* RDS data capture */
    // #define V4L2_CAP_VIDEO_OUTPUT_OVERLAY   0x00000200  /* Can do video output overlay */
// #define V4L2_CAP_HW_FREQ_SEEK           0x00000400  /* Can do hardware frequency seek  */
// #define V4L2_CAP_RDS_OUTPUT             0x00000800  /* Is an RDS encoder */

// #define V4L2_CAP_TUNER                  0x00010000  /* has a tuner */
// #define V4L2_CAP_AUDIO                  0x00020000  /* has audio support */
// #define V4L2_CAP_RADIO                  0x00040000  /* is a radio device */
// #define V4L2_CAP_MODULATOR              0x00080000  /* has a modulator */

// #define V4L2_CAP_READWRITE              0x01000000  /* read/write systemcalls */
// #define V4L2_CAP_ASYNCIO                0x02000000  /* async I/O */
// #define V4L2_CAP_STREAMING              0x04000000  /* streaming I/O ioctls */

// #define V4L2_CAP_DEVICE_CAPS            0x80000000  /* sets device capabilities field */

#define FM_RX_RDS_GRP_RT_EBL 1
#define FM_RX_RDS_GRP_PS_EBL 2
#define FM_RX_RDS_GRP_AF_EBL 4
#define FM_RX_RDS_GRP_PS_SIMPLE_EBL 16

#include <sys/system_properties.h>

int sys_run(char * cmd) {
  int ret = system(cmd);
  logd("sys_run ret: %d  cmd: \"%s\"", ret, cmd);
  return (ret);
}

/**
 * Инициализация блока радио
 */
int chip_imp_api_on(int freq_lo, int freq_hi, int freq_inc) {
  logd("chip_imp_api_on freq_lo: %d  freq_hi: %d  freq_inc: %d", freq_lo, freq_hi, freq_inc);
  curr_freq_lo = freq_lo;
  curr_freq_hi = freq_hi;
  curr_freq_inc= freq_inc;

  sys_run((char *) "setprop hw.fm.mode normal >/dev/null 2>/dev/null ; setprop hw.fm.version 0 >/dev/null 2>/dev/null ; setprop ctl.start fm_dl >/dev/null 2>/dev/null");

  if (file_get("/system/lib/modules/radio-iris-transport.ko")) {
    sys_run((char *) "insmod /system/lib/modules/radio-iris-transport.ko >/dev/null 2>/dev/null");//>/sdcard/s2z1 2>/sdcard/s2z2");
  }

  char value[4] = {0x41, 0x42, 0x43, 0x44};

  int i = 0;
  int init_success = 0;

  for (i = 0; i < 600; i ++) {
    __system_property_get("hw.fm.init", value);
    if (value[0] == '1') {
      init_success = 1;
      break;
    } else {
      ms_sleep(10);
    }
  }

  if (init_success == 1) {
    logd("chip_imp_api_on init success after %d milliseconds", i);
  } else {
    loge("chip_imp_api_on init error 0x%x after %d milliseconds", value, i);
  }

  ms_sleep(1000);

  //dev_hndl = open ("/dev/radio0", O_RDONLY | O_NONBLOCK);
  dev_hndl = open("/dev/radio0", O_RDWR | O_NONBLOCK);
  if (dev_hndl < 0) {
    loge("chip_imp_api_on error opening qualcomm /dev/radio0: %3.3d", errno);
    return (-1);
  }
  logd("chip_imp_api_on qualcomm /dev/radio0: %3.3d", dev_hndl);
  ms_sleep(1000);
  return (0);


/*
int ret = ioctl (dev_hndl, VIDIOC_QUERYCAP, & v4l_cap);
if (ret < 0) {
  logd ("chip_imp_api_on VIDIOC_QUERYCAP error: %d", ret);
//    close (dev_hndl);
//    return (0);
}
logd ("chip_imp_api_on VIDIOC_QUERYCAP ret: %d  cap: 0x%x  drv: %s  card: %s  bus: %s  ver: 0x%x", ret, v4l_cap.capabilities, v4l_cap.driver, v4l_cap.card, v4l_cap.bus_info, v4l_cap.version);
    // chip_imp_api_on VIDIOC_QUERYCAP ret: 0  cap: 0x00050000  drv: radio-tavarua  card: Qualcomm FM Radio Transceiver  bus: I2C       ver: 0x0
    // chip_imp_api_on VIDIOC_QUERYCAP ret: 0  cap: 0x00050000  drv: radio-tavarua  card: Qualcomm FM Radio Transceiver  bus: I2C       ver: 0x2010204
    // chip_imp_api_on VIDIOC_QUERYCAP ret: 0  cap: 0x010d0d00  drv: CG2900 Driver  card: CG2900 FM Radio                bus: platform  ver: 0x10100

if ( ! (v4l_cap.capabilities & V4L2_CAP_TUNER) ) {
  logd ("chip_imp_api_on no V4L2_CAP_TUNER !!!!");
//    close (dev_hndl);
//    return (0);
}
*/
}

/**
 * Выключение
 */
int chip_imp_api_off () {
  if (dev_hndl >= 0) {
    close (dev_hndl);
  }
  return (0);
}


// V4L support:
const char * cid_iris_private[] = {
  "SRCHMODE",
  "SCANDWELL",
  "SRCHON",
  "STATE",
  "TRANSMIT_MODE",
  "RDSGROUP_MASK",
  "REGION",
  "SIGNAL_TH",
  "SRCH_PTY",
  "SRCH_PI",
  "SRCH_CNT",
  "EMPHASIS",
  "RDS_STD",
  "SPACING",
  "RDSON",
  "RDSGROUP_PROC",
  "LP_MODE",
  "ANTENNA",
  "RDSD_BUF",
  "PSALL",

  "TX_SETPSREPEATCOUNT",
  "STOP_RDS_TX_PS_NAME",
  "STOP_RDS_TX_RT",
  "IOVERC",
  "INTDET",
  "MPX_DCC",
  "AF_JUMP",
  "RSSI_DELTA",
  "HLSI", // 0x800001d

  /*Diagnostic commands*/
  "SOFT_MUTE",
  "RIVA_ACCS_ADDR",
  "RIVA_ACCS_LEN",
  "RIVA_PEEK",
  "RIVA_POKE",
  "SSBI_ACCS_ADDR",
  "SSBI_PEEK",
  "SSBI_POKE",
  "TX_TONE",
  "RDS_GRP_COUNTERS",
  "SET_NOTCH_FILTER", // 0x8000028
  "SET_AUDIO_PATH",   // TAVARUA specific command
  "DO_CALIBRATION",
  "SRCH_ALGORITHM",   // TAVARUA specific command
  "GET_SINR",
  "INTF_LOW_THRESHOLD",
  "INTF_HIGH_THRESHOLD",
  "SINR_THRESHOLD",
  "SINR_SAMPLES",          // 0x8000030


 };

enum v4l2_cid_iris_private_iris_t {
  V4L2_CID_PRIVATE_IRIS_SRCHMODE = (0x08000000 + 1),  // = 0
  V4L2_CID_PRIVATE_IRIS_SCANDWELL,
  V4L2_CID_PRIVATE_IRIS_SRCHON,           // = 1 ? Stuck searching ?
  V4L2_CID_PRIVATE_IRIS_STATE,            // = 1
  V4L2_CID_PRIVATE_IRIS_TRANSMIT_MODE,    // 0x08000005 Tx only
  V4L2_CID_PRIVATE_IRIS_RDSGROUP_MASK,
  V4L2_CID_PRIVATE_IRIS_REGION,
  V4L2_CID_PRIVATE_IRIS_SIGNAL_TH,        // 0x08000008
  V4L2_CID_PRIVATE_IRIS_SRCH_PTY,
  V4L2_CID_PRIVATE_IRIS_SRCH_PI,
  V4L2_CID_PRIVATE_IRIS_SRCH_CNT,
  V4L2_CID_PRIVATE_IRIS_EMPHASIS,
  V4L2_CID_PRIVATE_IRIS_RDS_STD,          // 0x0800000d = 1
  V4L2_CID_PRIVATE_IRIS_SPACING,
  V4L2_CID_PRIVATE_IRIS_RDSON,            // 0x0800000f = 1
  V4L2_CID_PRIVATE_IRIS_RDSGROUP_PROC,    // 0x08000010 = 56 = 0x38 = 0x07 << RDS_CONFIG_OFFSET (3)
  V4L2_CID_PRIVATE_IRIS_LP_MODE,
  V4L2_CID_PRIVATE_IRIS_ANTENNA,          // = 1
  V4L2_CID_PRIVATE_IRIS_RDSD_BUF,
  V4L2_CID_PRIVATE_IRIS_PSALL,            // 0x8000014 = 56, ? Bug, copied RDSGROUP_PROC instead of boolean "pass all ps strings"
  V4L2_CID_PRIVATE_IRIS_TX_SETPSREPEATCOUNT,              // START TX controls:
  V4L2_CID_PRIVATE_IRIS_STOP_RDS_TX_PS_NAME,
  V4L2_CID_PRIVATE_IRIS_STOP_RDS_TX_RT,
  V4L2_CID_PRIVATE_IRIS_IOVERC,
  V4L2_CID_PRIVATE_IRIS_INTDET,
  V4L2_CID_PRIVATE_IRIS_MPX_DCC,
  V4L2_CID_PRIVATE_IRIS_AF_JUMP,
  V4L2_CID_PRIVATE_IRIS_RSSI_DELTA,
  V4L2_CID_PRIVATE_IRIS_HLSI,             // 0x800001d
  V4L2_CID_PRIVATE_IRIS_SOFT_MUTE,                        // START Diagnostic commands:
  V4L2_CID_PRIVATE_IRIS_RIVA_ACCS_ADDR,
  V4L2_CID_PRIVATE_IRIS_RIVA_ACCS_LEN,
  V4L2_CID_PRIVATE_IRIS_RIVA_PEEK,
  V4L2_CID_PRIVATE_IRIS_RIVA_POKE,
  V4L2_CID_PRIVATE_IRIS_SSBI_ACCS_ADDR,
  V4L2_CID_PRIVATE_IRIS_SSBI_PEEK,
  V4L2_CID_PRIVATE_IRIS_SSBI_POKE,
  V4L2_CID_PRIVATE_IRIS_TX_TONE,
  V4L2_CID_PRIVATE_IRIS_RDS_GRP_COUNTERS,
  V4L2_CID_PRIVATE_IRIS_SET_NOTCH_FILTER, // 0x8000028
  V4L2_CID_PRIVATE_IRIS_SET_AUDIO_PATH,   // TAVARUA specific command
  V4L2_CID_PRIVATE_IRIS_DO_CALIBRATION,
  V4L2_CID_PRIVATE_IRIS_SRCH_ALGORITHM,   // TAVARUA specific command
  V4L2_CID_PRIVATE_IRIS_GET_SINR,
  V4L2_CID_PRIVATE_INTF_LOW_THRESHOLD,
  V4L2_CID_PRIVATE_INTF_HIGH_THRESHOLD,
  V4L2_CID_PRIVATE_SINR_THRESHOLD,
  V4L2_CID_PRIVATE_SINR_SAMPLES,          // 0x8000030

  /*using private CIDs under userclass*/
  V4L2_CID_PRIVATE_IRIS_READ_DEFAULT = 0x00980928,
  V4L2_CID_PRIVATE_IRIS_WRITE_DEFAULT,
  V4L2_CID_PRIVATE_IRIS_SET_CALIBRATION,
};

//V4L2 CONTROLS FOR FM DRIVER
enum FM_V4L2_PRV_CONTROLS
{
    V4L2_CID_PRV_BASE = 0x8000000,
    V4L2_CID_PRV_SRCHMODE,
    V4L2_CID_PRV_SCANDWELL,
    V4L2_CID_PRV_SRCHON,
    V4L2_CID_PRV_STATE,
    V4L2_CID_PRV_TRANSMIT_MODE,
    V4L2_CID_PRV_RDSGROUP_MASK,
    V4L2_CID_PRV_REGION,
    V4L2_CID_PRV_SIGNAL_TH,
    V4L2_CID_PRV_SRCH_PTY,
    V4L2_CID_PRV_SRCH_PI,
    V4L2_CID_PRV_SRCH_CNT,
    V4L2_CID_PRV_EMPHASIS,
    V4L2_CID_PRV_RDS_STD,
    V4L2_CID_PRV_CHAN_SPACING,
    V4L2_CID_PRV_RDSON,
    V4L2_CID_PRV_RDSGROUP_PROC,
    V4L2_CID_PRV_LP_MODE,
    V4L2_CID_PRV_INTDET = V4L2_CID_PRV_BASE + 25,
    V4L2_CID_PRV_AF_JUMP = V4L2_CID_PRV_BASE + 27,
    V4L2_CID_PRV_SOFT_MUTE = V4L2_CID_PRV_BASE + 30,
    V4L2_CID_PRV_AUDIO_PATH = V4L2_CID_PRV_BASE + 41,
    V4L2_CID_PRV_SINR = V4L2_CID_PRV_BASE + 44,
    V4L2_CID_PRV_ON_CHANNEL_THRESHOLD = V4L2_CID_PRV_BASE + 0x2D,
    V4L2_CID_PRV_OFF_CHANNEL_THRESHOLD,
    V4L2_CID_PRV_SINR_THRESHOLD,
    V4L2_CID_PRV_SINR_SAMPLES,
    V4L2_CID_PRV_SPUR_FREQ,
    V4L2_CID_PRV_SPUR_FREQ_RMSSI,
    V4L2_CID_PRV_SPUR_SELECTION,
    V4L2_CID_PRV_AF_RMSSI_TH = V4L2_CID_PRV_BASE + 0x36,
    V4L2_CID_PRV_AF_RMSSI_SAMPLES,
    V4L2_CID_PRV_GOOD_CH_RMSSI_TH,
    V4L2_CID_PRV_SRCHALGOTYPE,
    V4L2_CID_PRV_CF0TH12,
    V4L2_CID_PRV_SINRFIRSTSTAGE,
    V4L2_CID_PRV_RMSSIFIRSTSTAGE,
    V4L2_CID_PRV_SOFT_MUTE_TH,
    V4L2_CID_PRV_IRIS_RDSGRP_RT,
    V4L2_CID_PRV_IRIS_RDSGRP_PS_SIMPLE,
    V4L2_CID_PRV_IRIS_RDSGRP_AFLIST,
    V4L2_CID_PRV_IRIS_RDSGRP_ERT,
    V4L2_CID_PRV_IRIS_RDSGRP_RT_PLUS,
    V4L2_CID_PRV_IRIS_RDSGRP_3A,

    V4L2_CID_PRV_IRIS_READ_DEFAULT = V4L2_CTRL_CLASS_USER + 0x928,
    V4L2_CID_PRV_IRIS_WRITE_DEFAULT,
    V4L2_CID_PRV_SET_CALIBRATION = V4L2_CTRL_CLASS_USER + 0x92A,
    HCI_FM_HELIUM_SET_SPURTABLE = 0x0098092D,
    HCI_FM_HELIUM_GET_SPUR_TBL  = 0x0098092E,
    V4L2_CID_PRV_IRIS_FREQ,
    V4L2_CID_PRV_IRIS_SEEK,
    V4L2_CID_PRV_IRIS_UPPER_BAND,
    V4L2_CID_PRV_IRIS_LOWER_BAND,
    V4L2_CID_PRV_IRIS_AUDIO_MODE,
    V4L2_CID_PRV_IRIS_RMSSI,

    V4L2_CID_PRV_ENABLE_SLIMBUS = 0x00980940,
};


/*
        .vidioc_querycap              = iris_vidioc_querycap,
        .vidioc_queryctrl             = iris_vidioc_queryctrl,
        .vidioc_g_ctrl                = iris_vidioc_g_ctrl,
        .vidioc_s_ctrl                = iris_vidioc_s_ctrl,
        .vidioc_g_tuner               = iris_vidioc_g_tuner,
        .vidioc_s_tuner               = iris_vidioc_s_tuner,
        .vidioc_g_frequency           = iris_vidioc_g_frequency,
        .vidioc_s_frequency           = iris_vidioc_s_frequency,
        .vidioc_s_hw_freq_seek        = iris_vidioc_s_hw_freq_seek,
        .vidioc_dqbuf                 = iris_vidioc_dqbuf,
        .vidioc_g_fmt_type_private    = iris_vidioc_g_fmt_type_private,
        .vidioc_s_ext_ctrls           = iris_vidioc_s_ext_ctrls,
        .vidioc_g_ext_ctrls           = iris_vidioc_g_ext_ctrls,
*/

// V4l2 structures:
struct v4l2_capability   v4l_cap = {0};
struct v4l2_tuner        v4l_tuner = {0};
struct v4l2_control      v4l_ctrl = {0};
struct v4l2_frequency    v4l_freq = {0};
struct v4l2_hw_freq_seek v4l_seek;

#ifndef DEF_BUF
  #define DEF_BUF 512 // Raised from 256 so we can add headers to 255-256 byte buffers
#endif

/**
 * Вывод данных буфера в логи
 */
int buf_display(char * buf, int size) {
  char asc[4 * DEF_BUF] = "evt: ";
  char byte_asc[4] = " xx";

  for (int ctr = 0; ctr < size && ctr < DEF_BUF; ctr++) {
    byte_asc[1] = '0' + ((0xF0 & buf[ctr]) >> 4);
    if (byte_asc[1] > '9') {
      byte_asc[1] -= '0';
      byte_asc[1] += 'A';
      byte_asc[1] -= 10;
    }
    byte_asc[2] = '0' + ((0x0F & buf[ctr]) >> 0);
    if (byte_asc[2] > '9') {
      byte_asc[2] -= '0';
      byte_asc[2] += 'A';
      byte_asc[2] -= 10;
    }
    strlcat(asc, byte_asc, sizeof(asc));
  }
  logd("%s", asc);
  return 0;
}

/**
 * Получение буфера на ???
 */
int iris_buf_get(char * buf, int buf_len, int type) {
  int ret = 0;

  struct v4l2_requestbuffers reqbuf;
  memset(&reqbuf, 0, sizeof(reqbuf));
  reqbuf.type = V4L2_BUF_TYPE_PRIVATE;
  reqbuf.memory = V4L2_MEMORY_USERPTR;

  struct v4l2_buffer v4l2_buf;
  memset(&v4l2_buf, 0, sizeof(v4l2_buf));
  v4l2_buf.index = type;
  v4l2_buf.type = reqbuf.type;
  v4l2_buf.length = buf_len; // 128;
  v4l2_buf.m.userptr = (unsigned long) buf;

  ret = ioctl(dev_hndl, VIDIOC_DQBUF, &v4l2_buf);

  if (ret < 0) {
    return (-1);
  }

  return v4l2_buf.bytesused;
}


#define MISC_BUF_SIZE    128

// Types:
#define BUF_SRCH_LIST   0  // stationList
#define BUF_EVENTS      1
#define BUF_RT_RDS      2 // посмотреть позже
#define BUF_PS_RDS      3
#define BUF_RAW_RDS     4
#define BUF_AF_LIST     5
#define BUF_MAX         6

// From radio-iris.h:
enum iris_buf_t {
  IRIS_BUF_SRCH_LIST,
  IRIS_BUF_EVENTS,
  IRIS_BUF_RT_RDS,
  IRIS_BUF_PS_RDS,
  IRIS_BUF_RAW_RDS,
  IRIS_BUF_AF_LIST,
  IRIS_BUF_PEEK,
  IRIS_BUF_SSBI_PEEK,
  IRIS_BUF_RDS_CNTRS,
  IRIS_BUF_RD_DEFAULT,
  IRIS_BUF_CAL_DATA,
  IRIS_BUF_RT_PLUS,
  IRIS_BUF_ERT,
  IRIS_BUF_MAX
};

extern int curr_pi; // Program ID. Canada non-national = 0xCxxx.   // 88.5: 0x163e, 106.1: 0xc448, 105.3: 0xc87d, 106.9: 0xdc09, 91.5: 0xb102, 89.1: 0, 89.9: 0x15d6, 93.9: 0xccb6
extern int cand_pi; //
extern int conf_pi; //

extern int curr_pt; // -1 // 88.5: 5 = Rock   106.1: 6 = Classic Rock
extern int cand_pt; // -1
extern int conf_pt; // -1

extern int need_pi_chngd; // 0;
extern int need_pt_chngd; // 0;
extern int need_ps_chngd; // 0;
extern int need_rt_chngd; // 0;

// Confirmed PS: When a new Current PS matches Candidate PS, the candidate is considered confirmed and copied here where the App can retrieve it.
extern char conf_ps[9];// ="        ";

// Confirmed RT
extern char conf_rt[65];// ="                                                                ";

int curr_stereo = 0;
int iris_rds_ok_dbg = 1;


/**
 * Получение идентификатора (что за идентификатор?)
 */
const char * id_get(int id) {
  if (id >= V4L2_CID_PRIVATE_IRIS_SRCHMODE && id <= V4L2_CID_PRIVATE_SINR_SAMPLES) { // 0x8000030
    int idx = id - V4L2_CID_PRIVATE_IRIS_SRCHMODE;

    const char * id_asc = cid_iris_private[idx];

    return id_asc;
  } else {
    return "Unk";
  }
}

/**
 * Get the value of a control
 * https://linuxtv.org/downloads/v4l-dvb-apis/uapi/v4l/vidioc-g-ctrl.html
 */
int chip_ctrl_get(int id) {
  v4l_ctrl.id = id;
  int ret = ioctl(dev_hndl, VIDIOC_G_CTRL, &v4l_ctrl);
  int value = v4l_ctrl.value;
  if (ret < 0) {
    loge("chip_ctrl_get VIDIOC_G_CTRL error: %3d  id: %40.40s (0x%x)", errno, id_get(id), id);
    return -1;
  } else {
    logd("chip_ctrl_get VIDIOC_G_CTRL OK:         id: %40.40s (0x%x)  value: %3d (0x%2.2x)", id_get(id), id, value, value);
  }
  return value;
}

/**
 * Set the value of a control
 * https://linuxtv.org/downloads/v4l-dvb-apis/uapi/v4l/vidioc-g-ctrl.html
 */
int chip_ctrl_set(int id, int value) {
  v4l_ctrl.value = value;
  v4l_ctrl.id = id;

  int ret = ioctl(dev_hndl, VIDIOC_S_CTRL, &v4l_ctrl);

  if (ret < 0) {
    loge ("chip_ctrl_set VIDIOC_S_CTRL error: %d  id: 0x%x  value: %d", errno, id, value);
    //return (-1);
  } else {
    logd ("chip_ctrl_set VIDIOC_S_CTRL OK id: %d  value: %d", id, value);
  }

  return ret;
}

// Internal functions:
int band_setup();

// Chip API:
char prop_buf[DEF_BUF] = "";

char * prop_get(const char * prop) {
  __system_property_get(prop, prop_buf);
  logd("props_log %32.32s: %s", prop, prop_buf);
  return prop_buf;
}


int v4l_transmit = 0;
int v4l_antenna = 0;

/**
 * Включение чипа
 */
int chip_imp_pwr_on(int pwr_rds) {
  int ret = 0;
  logd("chip_imp_pwr_on");
  if (dev_hndl <= 0) {
    loge("dev_hndl <= 0");
    return -1;
  }

/*  ret = chip_stro_req_set (3); // Mono, to set index = 0 ?
  if (ret) {
    ret = chip_stro_req_set (0); // Stereo
    if (ret) {
      //return (ret);
      loge ("chip_imp_pwr_on chip_stro_req_seterror: %d", ret);                 // Continue since we use chip_imp_pwr_on () to detect V4L now and this is not vital
    }
  }*/
    //chip_imp_mute_set (1);                                                      // Mute for now   !! Hangs when not on yet

  #define V4L2_CTRL_CLASS_FM_TX      0x009b0000      /* FM Modulator controls */
  #define V4L2_CID_FM_TX_CLASS_BASE  (V4L2_CTRL_CLASS_FM_TX | 0x900)
  //#define V4L2_CID_FM_TX_CLASS       (V4L2_CTRL_CLASS_FM_TX | 1)
  #define V4L2_CID_TUNE_POWER_LEVEL  (V4L2_CID_FM_TX_CLASS_BASE + 113)

  int new_state = 1; // Rx
  v4l_transmit = 0; // !!

  if (v4l_transmit) {
    new_state = 2; // Tx
  }

  /**
   * 0 = FM_OFF
   * 1 = FM_RECV
   * 2 = FM_TRANS
   * 3 = FM_RESET
   */
  if (chip_ctrl_set(V4L2_CID_PRIVATE_IRIS_STATE, new_state) < 0) {
    loge("chip_imp_pwr_on PRIVATE_IRIS_STATE 1 error for: %d", new_state);
  } else {
    logd("chip_imp_pwr_on PRIVATE_IRIS_STATE 1 success for: %d", new_state);
  }

  //chip_ctrl_get (V4L2_CID_TUNE_POWER_LEVEL);
  chip_ctrl_set (V4L2_CID_TUNE_POWER_LEVEL, 7);
  //chip_ctrl_get (V4L2_CID_TUNE_POWER_LEVEL);

  v4l_tuner.index = 0; // Tuner index = 0
  v4l_tuner.type = V4L2_TUNER_RADIO;
  memset(v4l_tuner.reserved, 0, sizeof(v4l_tuner.reserved));

  // !! Need power on first ??

  /**
   * https://linuxtv.org/downloads/v4l-dvb-apis/uapi/v4l/vidioc-g-tuner.html
   */
  ret = ioctl(dev_hndl, VIDIOC_G_TUNER, &v4l_tuner);
  if (ret < 0) {
    loge("chip_imp_pwr_on VIDIOC_G_TUNER errno: %d", errno);
    // Not anymore (Must return error since we use chip_powr_on() to detect V4L now)
    if (errno == EINVAL) {
      return -1;
    }
  } else {
    logd("chip_imp_pwr_on VIDIOC_G_TUNER success name: %s  type: %d  cap: 0x%x  lo: %d  hi: %d  sc: %d  am: %d  sig: %d  afc: %d", v4l_tuner.name, v4l_tuner.type, v4l_tuner.capability, v4l_tuner.rangelow , v4l_tuner.rangehigh, v4l_tuner.rxsubchans, v4l_tuner.audmode, v4l_tuner.signal, v4l_tuner.afc);

    // chip_imp_pwr_on VIDIOC_G_TUNER success name: FM  type: 1  cap: 0x1  lo: 1400000  hi: 1728000  sc: 3  am: 0  sig: 0  afc: 0
    // chip_imp_pwr_on VIDIOC_G_TUNER success name: FM  type: 1  cap: 0x1  lo: 1400000  hi: 1728000  sc: 3  am: 1  sig: 0  afc: 0
    // 1400/16 = 87.5, 1728/16 = 108

    // chip_imp_pwr_on VIDIOC_G_TUNER success name: FM  type: 1  cap: 0x1  lo: 0  hi: 0  sc: 3  am: 1  sig: 0  afc: 0
    // type: 1 = V4L2_TUNER_RADIO
    // cap:  1 = V4L2_TUNER_CAP_LOW (62.5 Hz)       !! No V4L2_TUNER_CAP_STEREO	= 0x0010 ?? !! But have mono/stereo in sc
    // !! lo/hi = 0 ??
    // sc = V4L2_TUNER_SUB_MONO	0x0001	| V4L2_TUNER_SUB_STEREO	0x0002

    // am: V4L2_TUNER_MODE_STEREO   1
    // am: V4L2_TUNER_MODE_MONO     0

    //chip_imp_mute_set(1); // Mute for now

    //ms_sleep (100);
  }

  ret = chip_ctrl_get(V4L2_CID_PRIVATE_IRIS_ANTENNA);
  v4l_antenna = 0;

  char* device = prop_get("ro.product.device");

  if (!strncasecmp(prop_get("ro.product.manufacturer"), "SONY", 4)) {
    if (!strncasecmp(device, "C2", 2) || !strncasecmp(device, "S39", 3) || !strncasecmp(device, "C19", 3) || !strncasecmp(device, "NICKI", 5) || !strncasecmp(device, "TAOSHAN", 7)) {
      v4l_antenna = 2;
    } else {
      v4l_antenna = 3;
    }
  }

  if (v4l_antenna >= 2) {
    // 0 = External/headset antenna for OneS/XL/Evo 4G/All others
    // 1 = internal for Xperia T - Z1
    ret = chip_ctrl_set(V4L2_CID_PRIVATE_IRIS_ANTENNA, v4l_antenna - 2);
    logd ("chip_imp_pwr_on PRIVATE_IRIS_ANTENNA ret: %d  v4l_antenna: %d", ret, v4l_antenna);
  }

  band_setup();

  logd("chip_imp_pwr_on done");

  logd("############################################################################");
  logd("RDS support = ?");
  logd("############################################################################");
  return 0;
}

/**
 * Выключение чипа
 */
int chip_imp_pwr_off(int pwr_rds) {
  int ret = 0;
  logd ("chip_imp_pwr_off");
  if (dev_hndl <= 0) {
    loge("dev_hndl <= 0");
    return -1;
  }
  chip_imp_mute_set(1); // Mute

  logd("chip_imp_pwr_off chip_imp_mute_set done");

  // The close() powers down       !!! NO !!! Need the below:
  // ?? !! Actually keeps running on AT&T OneX(L), mute will cover it.
  /**
   * 0 = FM_OFF, 1 = FM_RECV, 2 = FM_TRANS, 3 = FM_RESET
   */
  if (chip_ctrl_set (V4L2_CID_PRIVATE_IRIS_STATE, 0) < 0) {
    loge ("chip_imp_pwr_off PRIVATE_IRIS_STATE 0 error");
  } else {
    logd ("chip_imp_pwr_off PRIVATE_IRIS_STATE 0 success");
  }

  ms_sleep(2000);
  logd("chip_imp_pwr_off done");
  return 0;
}

/**
 * Изменение текущей частоты
 * freq = 10 KHz resolution (for example, 76 MHz = 7600, 108 MHz = 10800)
 */
int chip_imp_freq_set(int freq) {
  logd("chip_imp_freq_set: %3.3d", freq);
  int ret = 0;
  if (dev_hndl <= 0) {
    loge("dev_hndl <= 0");
    return -1;
  }

  v4l_freq.tuner = 0; // Tuner index = 0
  v4l_freq.type = V4L2_TUNER_RADIO;
  v4l_freq.frequency = freq * 16; // in units of 62.5 Hz ALWAYS for FM tuner. 62.5 KHz is too coarse for FM -> 250 KHz, Need 50 KHz
  memset(v4l_freq.reserved, 0, sizeof(v4l_freq.reserved));

  /**
   * https://linuxtv.org/downloads/v4l-dvb-apis/uapi/v4l/vidioc-g-frequency.html
   */
  ret = ioctl(dev_hndl, VIDIOC_S_FREQUENCY, &v4l_freq);

  if (ret < 0) {
    loge ("chip_imp_freq_set VIDIOC_S_FREQUENCY errno: %d", errno);
    return (-1);
  }

  curr_freq_val = freq;
  logd("chip_imp_freq_set VIDIOC_S_FREQUENCY success");
  return 0;
}

/**
 * "Мьют", отключение звука
 */
int chip_imp_mute_set(int mute) {
  int ret = 0;
  logd("chip_imp_mute_set: %d", mute);
  if (dev_hndl <= 0) {
    loge("dev_hndl <= 0");
   return -1;
  }

  /**
   * 0 = unmute
   * 1 = mute
   */
  if (chip_ctrl_set(V4L2_CID_AUDIO_MUTE, mute) < 0) {
    loge("chip_imp_mute_set MUTE error");
  } else {
    logd("chip_imp_mute_set MUTE success");
  }

  return 0;
}

/**
 * Изменение стерео-режима
 */
int chip_imp_stro_set(int stro) { //
  int ret = 0;
  logd("chip_imp_stro_set: %3.3d", stro);
  if (dev_hndl <= 0) {
    loge ("dev_hndl <= 0");
    return -1;
  }

  v4l_tuner.index = 0; // Tuner index = 0
  v4l_tuner.type = V4L2_TUNER_RADIO;
  memset(v4l_tuner.reserved, 0, sizeof(v4l_tuner.reserved));
  /**
   * https://linuxtv.org/downloads/v4l-dvb-apis/uapi/v4l/vidioc-g-tuner.html?highlight=v4l2_tuner_mode_stereo
   */
  v4l_tuner.audmode = stro ? V4L2_TUNER_MODE_STEREO : V4L2_TUNER_MODE_MONO;

  ret = ioctl(dev_hndl, VIDIOC_S_TUNER, &v4l_tuner);
  if (ret < 0) {
    loge("chip_stro_req_set VIDIOC_S_TUNER errno: %d", errno);
    return -1;
  }

  logd("chip_stro_req_set VIDIOC_S_TUNER success");
  return (0);
}

/**
 * Изменение громкости звука
 * Все тело функции -- три куска комментариев, кроме кода возврата
 */
int chip_imp_vol_set (int vol) {
  return (0);
}

/**
 * Получение текущей частоты
 */
int chip_imp_freq_get() {
  int ret = 0;
  int freq = 88500;

  if (dev_hndl <= 0) {
    loge ("dev_hndl <= 0");
    return freq;
  }

  v4l_freq.tuner = 0; // Tuner index = 0
  v4l_freq.type = V4L2_TUNER_RADIO;
  memset(v4l_freq.reserved, 0, sizeof(v4l_freq.reserved));

  /**
   * https://linuxtv.org/downloads/v4l-dvb-apis/uapi/v4l/vidioc-g-frequency.html
   */
  ret = ioctl(dev_hndl, VIDIOC_G_FREQUENCY, &v4l_freq);

  if (ret < 0) {
    loge("chip_imp_freq_get VIDIOC_G_FREQUENCY errno: %d", errno);
    return -1;
  }

  freq = v4l_freq.frequency / 16;
  curr_freq_val = freq;

  if (extra_log) {
    logd ("chip_imp_freq_get VIDIOC_G_FREQUENCY success: %d", freq);
  }
  return freq;
}

int fake_rssi = -1;

/**
 * Получение RSSI (received signal strength indicator)
 * Показатель уровня принимаемого сигнала
 */
int chip_imp_rssi_get() {
  if (fake_rssi >= 0) { // If reporting return value from chip_imp_extra_cmd...
    return fake_rssi;
  }

  int rssi = -7;

  v4l_tuner.index = 0; // Tuner index = 0
  v4l_tuner.type = V4L2_TUNER_RADIO;

  memset(v4l_tuner.reserved, 0, sizeof(v4l_tuner.reserved));

  int ret = ioctl(dev_hndl, VIDIOC_G_TUNER, &v4l_tuner);

  if (ret < 0) {
    static int times = 0;
    if (times++ % 10 == 0) {
      loge ("chip_imp_rssi_get VIDIOC_G_TUNER errno: %d", errno);
    }
    return -1;
  }

  rssi = v4l_tuner.signal;
  rssi -= 150; // 151 seen before proper power up ?      2014: ?  150 (0x96) = -106 to 205 (0xCD) = -51
  rssi *= 3;
  rssi /= 2; // Multiply by 1.5 to scale similar to other chips / Broadcom (-144)
  // RSSI -16 on N4 and OF means v4l_tuner.signal = 139
  return rssi;
}

/**
 * Получение состояния стерео
 */
int chip_imp_stro_get() {
  v4l_tuner.index = 0; // Tuner index = 0
  v4l_tuner.type = V4L2_TUNER_RADIO;
  memset(v4l_tuner.reserved, 0, sizeof(v4l_tuner.reserved));

  int ret = ioctl(dev_hndl, VIDIOC_G_TUNER, &v4l_tuner);

  if (ret < 0) {
    loge("chip_imp_stro_get VIDIOC_G_TUNER errno: %d", errno);
    // Not anymore (Must return error since we use chip_powr_on () to detect V4L now)
    if (errno == EINVAL) {
      return -1;
    }
  } else {
    // am: V4L2_TUNER_MODE_STEREO   1
    // am: V4L2_TUNER_MODE_MONO     0
    curr_stereo = v4l_tuner.audmode;
  }
  return curr_stereo;
}


// From radio-iris-commands.h :
enum iris_evt_t {
  IRIS_EVT_RADIO_READY,
  IRIS_EVT_TUNE_SUCC,
  IRIS_EVT_SEEK_COMPLETE,
  IRIS_EVT_SCAN_NEXT,
  IRIS_EVT_NEW_RAW_RDS,
  IRIS_EVT_NEW_RT_RDS,
  IRIS_EVT_NEW_PS_RDS,
  IRIS_EVT_ERROR,
  IRIS_EVT_BELOW_TH,
  IRIS_EVT_ABOVE_TH,
  IRIS_EVT_STEREO,
  IRIS_EVT_MONO,
  IRIS_EVT_RDS_AVAIL,
  IRIS_EVT_RDS_NOT_AVAIL,
  IRIS_EVT_NEW_SRCH_LIST,
  IRIS_EVT_NEW_AF_LIST,
  IRIS_EVT_TXRDSDAT,
  IRIS_EVT_TXRDSDONE,
  IRIS_EVT_RADIO_DISABLED,
  IRIS_EVT_NEW_ODA,
  IRIS_EVT_NEW_RT_PLUS,
  IRIS_EVT_NEW_ERT,
};


  int chip_imp_events_process (unsigned char * rds_grpd) {
    int ret = 0;
logd("============ CHIP IMP EVENTS PROCESS ===============");
    //logd ("chip_imp_events_process before iris_rds_buf_handle (BUF_RT_RDS)");
//    ret = iris_rds_buf_handle (BUF_RT_RDS);
/*
    if (ret < 0) {
      loge ("chip_imp_events_process BUF_RT_RDS errno: %d", errno);
      //return (-1);                                                      // No RDS
    }
    //else
    //  logd ("chip_imp_events_process BUF_RT_RDS success: %d", ret);
*/

    //logd ("chip_imp_events_process before iris_rds_buf_handle (BUF_PS_RDS)");
//    ret = iris_rds_buf_handle (BUF_PS_RDS);
/*
    if (ret < 0) {
      loge ("chip_imp_events_process BUF_PS_RDS errno: %d", errno);
      //return (-1);                                                      // No RDS
    }
    //else
    //  logd ("chip_imp_events_process BUF_PS_RDS success: %d", ret);
*/
    //return (-1);                                                        // No RDS; Already called rds_group_process(), possibly multiple times.


    char misc_buf [MISC_BUF_SIZE];
    memset(misc_buf, 0, sizeof (misc_buf));
logd ("chip_imp_events_process before iris_buf_get()");
    ret = iris_buf_get (misc_buf, sizeof (misc_buf), BUF_EVENTS);   // Get events...
    if (ret < 0) {
      loge ("chip_imp_events_process EVENTS errno: %d", errno);
      return (-1);                                                      // No RDS; Already called rds_group_process(), possibly multiple times.
    }
logd ("chip_imp_events_process EVENTS success: %d", ret);
buf_display (misc_buf, ret);
    int event = misc_buf [0];
    switch (event) {
      case IRIS_EVT_RADIO_READY:    // 0
        logd ("Got IRIS_EVT_RADIO_READY");
        break;
      case IRIS_EVT_TUNE_SUCC:      // 1
        logd ("Got IRIS_EVT_TUNE_SUCC");
        break;
      case IRIS_EVT_SEEK_COMPLETE:
        logd ("Got IRIS_EVT_SEEK_COMPLETE");
        break;
      case IRIS_EVT_SCAN_NEXT:
        logd ("Got IRIS_EVT_SCAN_NEXT");
        break;
      case IRIS_EVT_NEW_RAW_RDS:
        logd ("Got IRIS_EVT_NEW_RAW_RDS");
        break;
      case IRIS_EVT_NEW_RT_RDS:
logd ("Got IRIS_EVT_NEW_RT_RDS");
        break;
      case IRIS_EVT_NEW_PS_RDS:
logd ("Got IRIS_EVT_NEW_PS_RDS");
        break;
      case IRIS_EVT_ERROR:
        logd ("Got IRIS_EVT_ERROR");
        break;
      case IRIS_EVT_BELOW_TH:
        logd ("Got IRIS_EVT_BELOW_TH");
        break;
      case IRIS_EVT_ABOVE_TH:
        logd ("Got IRIS_EVT_ABOVE_TH");
        break;
      case IRIS_EVT_STEREO:
        logd ("Got IRIS_EVT_STEREO");
        curr_stereo = 1;
        break;
      case IRIS_EVT_MONO:
        logd ("Got IRIS_EVT_MONO");
        curr_stereo = 0;
        break;
      case IRIS_EVT_RDS_AVAIL:
        logd ("Got IRIS_EVT_RDS_AVAIL");
        break;
      case IRIS_EVT_RDS_NOT_AVAIL:
        logd ("Got IRIS_EVT_RDS_NOT_AVAIL");    //????
        break;
      case IRIS_EVT_NEW_SRCH_LIST:
        logd ("Got IRIS_EVT_NEW_SRCH_LIST");
        break;
      case 15:
        logd ("Got IRIS_EVT_NEW_AF_LIST");
        break;
      case 16:
        loge ("IRIS_EVT_TXRDSDAT");
        break;
      case 17:
        loge ("Got IRIS_EVT_TXRDSDONE");
        break;
      case 18:
        loge ("Got IRIS_EVT_RADIO_DISABLED");
        break;
      case 19:
        loge ("Got IRIS_EVT_NEW_ODA");
        break;
      case 20:
        loge ("Got IRIS_EVT_NEW_RT_PLUS,");
        break;
      case 21:
        loge ("Got IRIS_EVT_NEW_ERT");
        break;

      default:
        loge ("Unknown event: %d", event);
        break;
    }

    return (-1);                                                        // No RDS; Already called rds_group_process(), possibly multiple times.
  }

// Seek:
/* Search options */
enum search_t {
	SEEK,
	SCAN,
	SCAN_FOR_STRONG,
	SCAN_FOR_WEAK,
	RDS_SEEK_PTY,
	RDS_SCAN_PTY,
	RDS_SEEK_PI,
	RDS_AF_JUMP,
};


#define VIDIOC_S_HW_FREQ_SEEK _IOW('V', 82, struct v4l2_hw_freq_seek)

/**
 * Автоматическая hardware-перемотка, поиск частоты
 * [dir == 0] = <-
 * [dir != 0] = ->
 */
int chip_imp_seek_start(int dir) {
  int ret = 0;
  logd ("chip_imp_seek_start dir: %3.3d", dir);
  //chip_imp_seek_stop ();

  int orig_freq = chip_imp_freq_get();
  v4l_seek.tuner = 0; // Tuner index = 0
  v4l_seek.type = V4L2_TUNER_RADIO;
  memset(v4l_seek.reserved, 0, sizeof(v4l_seek.reserved));

  v4l_seek.wrap_around = 1;   // ? yes
  v4l_seek.seek_upward = dir; // ? always down ? If non-zero, seek upward from the current frequency, else seek downward.
  v4l_seek.spacing = 0;//curr_freq_inc * 1000; // ? 0 ok
  //v4l2_hw_freq_seek

  /**
   * https://linuxtv.org/downloads/v4l-dvb-apis/uapi/v4l/vidioc-s-hw-freq-seek.html
   */
  ret = ioctl(dev_hndl, VIDIOC_S_HW_FREQ_SEEK, &v4l_seek);

  if (ret < 0) {
    loge("chip_imp_seek_start VIDIOC_S_HW_FREQ_SEEK error: %d", ret);
    return -1;
  }

  logd ("chip_imp_seek_start VIDIOC_S_HW_FREQ_SEEK success");
  ms_sleep(300); // Wait a bit to ensure change  (100 ms OK normally, 500 for change end ?
  int new_freq = 0;
  int ctr;
  for (ctr = 0; ctr < 50 && new_freq != orig_freq; ctr++) { // 5 seconds max
    if (new_freq >= 50000 && new_freq <= 150000) {
      orig_freq = new_freq;
    }
    ms_sleep(101);
    new_freq = chip_imp_freq_get();
  }
  logd("chip_imp_seek_start complete tenths of a second: %d  orig_freq: %d", ctr, orig_freq);

  return new_freq;
}

/**
 * Остановка перемотки
 */
int chip_imp_seek_stop() {
  logd("chip_imp_seek_stop");
  if (dev_hndl <= 0) {
    loge("dev_hndl <= 0");
    return -1;
  }
  return 0;
}


    // Band stuff:

  int band_set (int low , int high, int band) {                 // ? Do we need to stop/restart RDS power in reg 0x00 ? Or rbds_set to flush ?
    logd ("band_set low: %3.3d  high: %3.3d  band: %3.3d", low, high, band);
/*                .id            = V4L2_CID_PRIVATE_IRIS_REGION,
                .type          = V4L2_CTRL_TYPE_INTEGER,
                .name          = "radio standard",
                .minimum       = 0,
                .maximum       = 2,
                .step          = 1,
                .default_value = 0,
*/
    int ret = 0;
    if (dev_hndl <= 0) {
      loge ("dev_hndl <= 0");
      return (-1);
    }
    int v4reg = 1;    // EU
    if (low < 87500)
      v4reg = 2;      // Japan
    else if (band)
      v4reg = 0;      // US
    if (chip_ctrl_set (V4L2_CID_PRIVATE_IRIS_REGION, v4reg) < 0)
      loge ("band_set PRIVATE_IRIS_REGION error band: %d", band);
    else
      logd ("band_set PRIVATE_IRIS_REGION success band: %d", band);
    return (0);
  }
  int freq_inc_set (int inc) {
    logd ("freq_inc_set: %3.3d", inc);
    int ret = 0;
    if (dev_hndl <= 0) {
      loge ("dev_hndl <= 0");
      return (-1);
    }
    int v4spac = 0;                                                     // Must be 0, 1 or 2
    if (inc <= 50)
      v4spac = 2;
    else if (inc <= 100)
      v4spac = 1;
    if (chip_ctrl_set (V4L2_CID_PRIVATE_IRIS_SPACING, v4spac) < 0)
      loge ("freq_inc_set PRIVATE_IRIS_SPACING error inc: %d", inc);
    else
      logd ("freq_inc_set PRIVATE_IRIS_SPACING success inc: %d", inc);
    return (0);
  }
  int emph75_set (int emph75) {
    logd ("emph75_set: %3.3d", emph75);
/*                .id            = V4L2_CID_PRIVATE_IRIS_EMPHASIS,
                .type          = V4L2_CTRL_TYPE_BOOLEAN,
                .name          = "Emphasis",
                .minimum       = 0,
                .maximum       = 1,
                .default_value = 0,
*/
    if (dev_hndl <= 0) {
      loge ("dev_hndl <= 0");
      return (-1);
    }
    int v4emph = 0;
    if (emph75 == 0)
      v4emph = 1;
    if (chip_ctrl_set (V4L2_CID_PRIVATE_IRIS_EMPHASIS, v4emph) < 0)
      loge ("chip_emph75_set PRIVATE_IRIS_EMPHASIS error emph75: %d", emph75);
    else
      logd ("chip_emph75_set PRIVATE_IRIS_EMPHASIS success emph75: %d", emph75);
    return (0);
  }

/*int is_rds_supported() {
  int res = 0;

  v4l2_cap.
}*/

const int MAX_RT_LEN = 64;
const int STD_BUF_SIZE = 256;
const int RT_LEN_IND = 0;
const char LAST_CTRL_CHAR = 31;
const char FIRST_NON_PRNT_CHAR = 127;
const char SPACE_CHAR = 32;
const int RT_DATA_OFFSET_IND = 5;
const int RT_A_B_FLAG_IND = 4;

int chip_imp_rds_rt_get(char *rt, int *rt_len) {
  int ret = 0;
  int len = 0;
  char raw_rds[STD_BUF_SIZE];

  ret = iris_buf_get(raw_rds, STD_BUF_SIZE, RT_IND);
  if (ret <= 0) {
      return FM_FAILURE;
  } else {
      if (rt != NULL) {
          if ((raw_rds[RT_LEN_IND] > 0) && (raw_rds[RT_LEN_IND] <= MAX_RT_LEN)) {
              for (len = 0; len < raw_rds[RT_LEN_IND]; len++) {
                 rt[len] = raw_rds[RT_DATA_OFFSET_IND + len];
                 logd("Rt byte[%d]: %d\n", len, rt[len]);
                 if ((rt[len] <= LAST_CTRL_CHAR) || (rt[len] >= FIRST_NON_PRNT_CHAR)) {
                     rt[len] = SPACE_CHAR;
                     continue;
                 }
              }
              if (len < (MAX_RT_LEN - 1)) {
                  rt[len] = '\0';
                  *rt_len = len + 1;
              } else {
                  *rt_len = len;
              }
              logd("Rt is: %s\n", rt);
              logd("RT text A / B: %d\n", raw_rds[RT_A_B_FLAG_IND]);
          } else {
              return FM_FAILURE;
          }
      } else {
          return FM_FAILURE;
      }
  }
  need_pt_chngd = false;
  logd("%s, [rt_len=%d]\n", __func__, *rt_len);
  return FM_SUCCESS;
}

int chip_set_control(int id, int val) {
  int ret;
  struct v4l2_control control;

  control.value = val;
  control.id = id;

  for(int i = 0; i < 3; i++) {
    ret = ioctl(dev_hndl, VIDIOC_S_CTRL, &control);
    if (ret < IOCTL_SUCC) {
      ret = FM_FAILURE;
    } else {
      ret = FM_SUCCESS;
      break;
    }
  }
  return ret;
}

int chip_get_control(int id, long &val) {
    int ret;
    struct v4l2_control control;

    control.id = id;
    ret = ioctl(dev_hndl, VIDIOC_G_CTRL, &control);
    if (ret < IOCTL_SUCC) {
        return FM_FAILURE;
    } else {
        val = control.value;
        return FM_SUCCESS;
    }
}

//enable, disable value to receive data of a RDS group
//return FM_SUCCESS on success, FM_FAILURE on failure
int rds_processing_group_set(int grps) {
    int ret;
    long mask;

    if (dev_hndl) {
       ret = get_control(V4L2_CID_PRV_RDSGROUP_PROC, mask);

       if (ret < 0) {
          return -1;
       }

       mask &= 0xC7;
       mask |= ((grps & 0x07) << 3);

       ret = set_control(V4L2_CID_PRV_RDSGROUP_PROC, (int) mask);
    } else {
       ret = -1;
    }
    return ret;
}

int enable_rds() {
    int ret = -1;

    logd("enable_rds");
    if (dev_hndl) {
        ret = set_control(V4L2_CID_PRV_RDSON, 1);

        if (ret < 0) {
            logd("RDS ON failed\n");
            return -1;
        }

        ret = rds_processing_group_set(FM_RX_RDS_GRP_RT_EBL);
        if (ret < 0) {
            logd("Set RDS grp processing\n");
            return -1;
        }

        ret = 1;
        rds_enabled = 1;
    } else {
        logd("enable_rds: not in proper state cur_fm_state = ");
        return ret;
    }
    return ret;
}

//Disable all RDS data processing
//RT, ERT, RT PLUS, PS
int disable_rds(void) {
    int ret = -1;

    if (dev_hndl) {
        ret = set_control(V4L2_CID_PRV_RDSON, 2);
        if (ret < 0) {
            logd("Disable RDS failed");
            return -1;
        }

        ret = 0;
        rds_enabled = 0;
    }

    return ret;
}

int toggle_rds(int onoff) {
    int ret = 0;

    if (onoff) {
        ret = enable_rds();
    } else {
        ret = disable_rds();
    }

    if (ret < 0) {
        logd("%s, failed\n", __func__);
    }

    logd("%s, [onoff=%d] [ret=%d]\n", __func__, onoff, ret);
    return ret;
}



int rds_has_support_get() {
  int ret;

  struct v4l2_capability v4l_cap;
  //v4l_cap.tuner = 0; // Tuner index = 0
  //v4l_cap.type = V4L2_TUNER_RADIO;
  memset(v4l_cap.reserved, 0, sizeof(v4l_cap.reserved));

  ret = ioctl(dev_hndl, VIDIOC_QUERYCAP, &v4l_cap);
  if (ret < 0) {
      return -1;
  } else {
      return v4l_cap.capabilities;
  }
}



/*
  int rbds_set (int rbds) {
    logd ("rbds_set: %3.3d", rbds);
    curr_rbds = rbds;
    int ret = 0;
    logd ("chip_rbds_set: %d", rbds);
    if (dev_hndl <= 0) {
      loge ("dev_hndl <= 0");
     return (-1);
    }
    band_set (curr_freq_lo, curr_freq_hi);
    return (0);
  }
*/

/**
 * Установка диапазона частот
 */
int band_setup() {
  curr_freq_inc = intern_band ? 200 : 100;
  band_set(curr_freq_lo, curr_freq_hi, intern_band);
  freq_inc_set(curr_freq_inc);
  emph75_set(intern_band);
  //rbds_set (intern_band);
}


    // Disabled internal functions:
/*  int chip_info_log () {
int chip_get () {
  if (! file_get ("/dev/radio0")) {                                      // If /dev/radio0 doesn't exist...
    logd ("chip_get no /dev/radio0");
    return (0);
  }
  logd ("chip_get have /dev/radio0");

  //int rad_hndl = open ("/dev/radio0", O_RDWR);
  int rad_hndl = open ("/dev/radio0", O_RDONLY | O_NONBLOCK);
  if (rad_hndl < 0) {
    loge ("chip_get error opening /dev/radio0: %d", errno);
    return (0);
  }
  logd ("chip_get /dev/radio0: %d", rad_hndl);

  int ret = ioctl (rad_hndl, VIDIOC_QUERYCAP, & v4l_cap);
  if (ret < 0) {
    logd ("chip_get VIDIOC_QUERYCAP error: %d", ret);
    close (rad_hndl);
    return (0);
  }
  logd ("chip_get VIDIOC_QUERYCAP ret: %d  cap: 0x%x  drv: %s  card: %s  bus: %s  ver: 0x%x", ret, v4l_cap.capabilities, v4l_cap.driver, v4l_cap.card, v4l_cap.bus_info, v4l_cap.version);
      // chip_get VIDIOC_QUERYCAP ret: 0  cap: 0x00050000  drv: radio-tavarua  card: Qualcomm FM Radio Transceiver  bus: I2C       ver: 0x0
      // chip_get VIDIOC_QUERYCAP ret: 0  cap: 0x00050000  drv: radio-tavarua  card: Qualcomm FM Radio Transceiver  bus: I2C       ver: 0x2010204
      // chip_get VIDIOC_QUERYCAP ret: 0  cap: 0x010d0d00  drv: CG2900 Driver  card: CG2900 FM Radio                bus: platform  ver: 0x10100

    // M8 w/ CM11:
      //v4_get VIDIOC_QUERYCAP ret: 0  cap: 0x50000  drv: radio-iris  card: Qualcomm FM Radio Transceiver  bus:   ver: 0x30400


  if ( ! (v4l_cap.capabilities & V4L2_CAP_TUNER) ) {
    logd ("chip_get no V4L2_CAP_TUNER !!!!");
//    close (rad_hndl);
//    return (0);
  }
  close (rad_hndl);
  logd ("chip_get have V4L");


  return (1);
}
  }*/

// #define V4L2_CAP_RDS_CAPTURE            0x00000100  /* RDS data capture */
    // #define V4L2_CAP_VIDEO_OUTPUT_OVERLAY   0x00000200  /* Can do video output overlay */
// #define V4L2_CAP_HW_FREQ_SEEK           0x00000400  /* Can do hardware frequency seek  */
// #define V4L2_CAP_RDS_OUTPUT             0x00000800  /* Is an RDS encoder */

// #define V4L2_CAP_TUNER                  0x00010000  /* has a tuner */
// #define V4L2_CAP_AUDIO                  0x00020000  /* has audio support */
// #define V4L2_CAP_RADIO                  0x00040000  /* is a radio device */
// #define V4L2_CAP_MODULATOR              0x00080000  /* has a modulator */

// #define V4L2_CAP_READWRITE              0x01000000  /* read/write systemcalls */
// #define V4L2_CAP_ASYNCIO                0x02000000  /* async I/O */
// #define V4L2_CAP_STREAMING              0x04000000  /* streaming I/O ioctls */

// #define V4L2_CAP_DEVICE_CAPS            0x80000000  /* sets device capabilities field */


/*

  char versionStr [40];
  char value = 0;

   // Driver Version = Same as ChipId
       //Conver the integer to string
  sprintf (versionStr, "%d", v4l_cap.version );
  __system_property_set ("hw.fm.version", versionStr);
    //Set the mode for soc downloader
  __system_property_set ("hw.fm.mode", "normal");

  __system_property_set ("ctl.start", "fm_dl");
  sleep(1);
  int i = 0, init_success = 0;
  for (i = 0; i < 6;i ++) {
    __system_property_get ("hw.fm.init", & value);//, NULL);
    if (value == '1') {
      init_success = 1;
      break;
    }
    else {
      sleep(1);
    }
  }
  loge ("init_success: %d after %d seconds", init_success, i);
/* Close:
    __system_property_set ("ctl.stop", "fm_dl");
    close(fd); */

/*  int rds_ready_get () {
    status_rssi_t sr = {0};
    int ret = ioctl (dev_hndl, Si4709_IOC_STATUS_RSSI_GET, & sr);
    if (ret < 0) {
      loge ("rds_ready_get IOCTL                    Si4709_IOC_STATUS_RSSI_GET error: %3.3d   rds_ready: %3.3d  rds_synced: %3.3d  seek_tune_complete: %3.3d  seekfail_bandlimit: %3.3d\
          afc_railed: %3.3d  block_error_a: %3.3d stereo: %3.3d  rssi: %3.3d", ret, sr.rdsr, sr.rdss, sr.stc, sr.sfbl, sr.afcrl, sr.blera, sr.st, sr.rssi);
      return (0);
    }
    if (sls_status_chip_imp_rssi_get_cnt ++ % 1200 == 0)                           // Every 2 minutes
      logd ("rds_ready_get                          Si4709_IOC_STATUS_RSSI_GET success: %3.3d  rds_ready: %3.3d  rds_synced: %3.3d  seek_tune_complete: %3.3d  seekfail_bandlimit: %3.3d\
          afc_railed: %3.3d  block_err_a: %3.3d stereo: %3.3d  rssi: %3.3d", ret, sr.rdsr, sr.rdss, sr.stc, sr.sfbl, sr.afcrl, sr.blera, sr.st, sr.rssi);
    if (sr.rdsr)                                                          // If RDS data ready...
      return (1);
    return (0);
  }*/


extern int RSSI_FACTOR;

int chip_imp_extra_cmd(const char * command, char ** parameters) {
  if (command == NULL) {
    return -1;
  }

  int full_val = atoi(command);       // full_val = -2^31 ... +2^31 - 1       = Over 9 digits
  int ctrl = (full_val / 1000) - 200; // ctrl = hi 3 digits - 200     (control to write to)
  int val  = (full_val % 1000);       // val  = lo 3 digits           (value to write)

  logd("chip_imp_extra_cmd command: %s  full_val: %d  ctrl: %d  val: %d", command, full_val, ctrl, val);

  if (val == 990) {
    intern_band = 0; // EU
    band_setup();
    return 0;
  } else if (val == 991) {
    intern_band = 1; // US
    band_setup();
    return 0;
  }

  if (ctrl >= 1 && ctrl <= 999) {
    ctrl += 0x08000000;
    int ret = chip_ctrl_get(ctrl);
    if (ret >= 0) {
      RSSI_FACTOR = 1;
      fake_rssi = ret + 100; // Report return value from chip_ctrl_get
    }

    logd ("chip_imp_extra_cmd get ret: %d  ctrl: %x", ret, ctrl);

    if (val < 900 && val >= 0) { // Val from 0 - 899 are for chip_ctrl_set, 900-999 are for get only
      ret = chip_ctrl_set(ctrl, val);
      logd("chip_imp_extra_cmd set ret: %d  ctrl: %x", ret, ctrl);
    }
  }

  return 0;
}

/* Private Control range: 1 - 48 decimal or 0x01 - 0x30
        V4L2_CID_PRIVATE_IRIS_SRCHMODE = (0x08000000 + 1),  // = 0
        V4L2_CID_PRIVATE_SINR_SAMPLES,          // 0x8000030
*/

void qc_test() {
  int argc = 0;
  char ** argv = NULL;

  //int src_mo = 0;
  int sft_mu = 0;
  int sig_th = 0;
  int snr_th = 0;
  int ihi_th = 0;
  int ilo_th = 0;
  //src_mo = chip_ctrl_get (V4L2_CID_PRIVATE_IRIS_SRCHMODE);
  sft_mu = chip_ctrl_get (V4L2_CID_PRIVATE_IRIS_SOFT_MUTE);
  sig_th = chip_ctrl_get (V4L2_CID_PRIVATE_IRIS_SIGNAL_TH);
  snr_th = chip_ctrl_get (V4L2_CID_PRIVATE_SINR_THRESHOLD);
  ihi_th = chip_ctrl_get (V4L2_CID_PRIVATE_INTF_HIGH_THRESHOLD);
  ilo_th = chip_ctrl_get (V4L2_CID_PRIVATE_INTF_LOW_THRESHOLD);
//D/v4l chip_ctrl_get VIDIOC_G_CTRL OK:         id:                                SOFT_MUTE 30 (0x800001e)  value:   1 (0x01)  0/1     BAD: 2
//D/v4l chip_ctrl_get VIDIOC_G_CTRL OK:         id:                                SIGNAL_TH  8 (0x8000008)  value:   0 (0x00)  0
//D/v4l chip_ctrl_get VIDIOC_G_CTRL OK:         id:                           SINR_THRESHOLD 47 (0x800002f)  value:   7 (0x07)  3/7
//D/v4l chip_ctrl_get VIDIOC_G_CTRL OK:         id:                      INTF_HIGH_THRESHOLD 46 (0x800002e)  value: 115 (0x73)  115
//D/v4l chip_ctrl_get VIDIOC_G_CTRL OK:         id:                       INTF_LOW_THRESHOLD 45 (0x800002d)  value: 109 (0x6d)  109

//D/v4l chip_ctrl_set VIDIOC_S_CTRL OK:         id:                                SOFT_MUTE 30 (0x800001e)  value:   1 (0x01)

  if (argc > 1)
    sft_mu = atoi (argv [1]);
  if (argc > 2)
    sig_th = atoi (argv [2]);
  if (argc > 3)
    snr_th = atoi (argv [3]);
  if (argc > 4)
    ihi_th = atoi (argv [4]);
  if (argc > 5)
    ilo_th = atoi (argv [5]);

  logd ("sft_mu: %d", sft_mu);

  //src_mo = 1;
  //if (argc > 1)
  //  chip_ctrl_set (V4L2_CID_PRIVATE_IRIS_SRCHMODE, src_mo);

  if (sft_mu != -1000000 && argc > 1)
//    if (sft_mu != -1000000)
    chip_ctrl_set (V4L2_CID_PRIVATE_IRIS_SOFT_MUTE, sft_mu);


  if (sig_th != -1000000 && argc > 2)
    chip_ctrl_set (V4L2_CID_PRIVATE_IRIS_SIGNAL_TH, sig_th);
  if (snr_th != -1000000 && argc > 3)
    chip_ctrl_set (V4L2_CID_PRIVATE_SINR_THRESHOLD, snr_th);
  if (ihi_th != -1000000 && argc > 4)
    chip_ctrl_set (V4L2_CID_PRIVATE_INTF_HIGH_THRESHOLD, ihi_th);
  if (ilo_th != -1000000 && argc > 5)
    chip_ctrl_set (V4L2_CID_PRIVATE_INTF_LOW_THRESHOLD, ilo_th);

}