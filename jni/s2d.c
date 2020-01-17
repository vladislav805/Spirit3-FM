#include <dlfcn.h>
#include <stdio.h>
#include <getopt.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>
#include <dirent.h>
#include <termios.h>
#include <string.h>
#include <signal.h>
#include <sys/system_properties.h>
#include <sys/ioctl.h>
#include <pthread.h>
#include <android/log.h>
#include <stdarg.h>
#include <zconf.h>

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wpointer-sign"
#define DEF_BUF 512    // Raised from 256 so we can add headers to 255-256 byte buffers


int s2d_cmd_log = 1;//0;

unsigned char logtag[16] = "s2l......";   // Default "s2l" = JNI library

const char* copyright = "Copyright (c) 2011-2014 Michael A. Reid. All rights reserved.";

#define loge(...) fm_log_print(ANDROID_LOG_ERROR, logtag, __VA_ARGS__)
#define logd(...) fm_log_print(ANDROID_LOG_DEBUG, logtag, __VA_ARGS__)

int tuner_server_work_func(char* cmd_buf, int cmd_len, char* res_buf, int res_max);
int server_work_func(char* cmd_buf, int cmd_len, char* res_buf, int res_max);

int no_log = 0;
void* log_hndl = NULL;

int (*do_log)(int prio, const char* tag, const char* fmt, va_list ap);

int fm_log_print(int prio, const char* tag, const char* fmt, ...) {
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

int ms_sleep(int ms) {
  if (ms > 10) {
    loge ("ms_sleep ms: %d", ms);
  }
  usleep(ms * 1000); // ?? Use nanosleep to avoid SIGALRM ??
  return 0;
}

// Return 1 if file, or directory, or device node etc. exists
int file_get(const char* file) {
  struct stat sb;
  return (stat(file, &sb) == 0);
}


  char prop_buf    [DEF_BUF] = "";
  char * prop_get (const char * prop) {
    __system_property_get (prop, prop_buf);
    logd ("props_log %32.32s: %s", prop, prop_buf);
    return (prop_buf);
  }

int curr_radio_device_int = -1;

#define DEV_QCV 4


// STE API support:
#include "plug/inc/android_fmradio.h"

static bool lib_name_get (char* lib_name, size_t max_size) {
  strncpy(lib_name, "/data/data/fm.a2d.sf/lib/libs2t_qcv.so", max_size);
  return true;
}

void* lib_fd;
struct fmradio_vendor_methods_t * tnr_funcs;

bool lib_load() {
  char lib_name[DEF_BUF] = "/system/lib/libs2t.so";

  if (file_get("/mnt/sdcard/sf/sys_bin")) {
    loge ("Using: %s", lib_name);
  } else if (!lib_name_get(lib_name, sizeof(lib_name))) { // Read library directory and find matching library
    loge ("Can't get lib_name for curr_radio_device_int: %d", curr_radio_device_int);
    return false;
  }

  lib_fd = dlopen(lib_name, RTLD_LAZY); // Load the library
  if (lib_fd == NULL) {
    loge ("Could not load library '%s'", lib_name);
    return false;
  } else {
    logd("Loaded library %s  lib_fd: %d", lib_name, lib_fd);
  }

  // Now we have loaded the library, check for function
  fmradio_reg_func_t reg_func = (fmradio_reg_func_t) dlsym(lib_fd, FMRADIO_REGISTER_FUNC);
  if (reg_func == NULL) {
    loge ("Could not find symbol '%s' in loaded library '%s'", FMRADIO_REGISTER_FUNC, lib_name);
    dlclose(lib_fd);
    return false;
  }

  unsigned int magic_val = 0;
  if (reg_func(&magic_val, tnr_funcs) != 0) { // Register
    loge("Loaded function '%s' returned unsuccessful", FMRADIO_REGISTER_FUNC);
    dlclose(lib_fd);
    return false;
  }

  if (magic_val != FMRADIO_SIGNATURE) { // Ensure correct function was called
    loge("Loaded function '%s' returned successful but failed setting magic value", FMRADIO_REGISTER_FUNC);
    dlclose(lib_fd);
    return false;
  }
  return true; // Success
}

void lib_unload() {
  if (lib_fd != NULL) {
    dlclose(lib_fd);
    free(tnr_funcs);
    tnr_funcs = NULL;
  }
}

// Client/Server:
// Unix datagrams requires other write permission for /dev/socket, or somewhere else (ext not FAT on sdcard) writable.

//#define CS_AF_UNIX     // Use network sockets to avoid filesystem permission issues w/ Unix Domain Address Family sockets
#define CS_DGRAM         // Use datagrams, not streams/sessions
#define CS_RX_TMO 1000   // 1 second
//#define CS_RX_TMO 100  // 100 milliseconds

#ifdef  CS_AF_UNIX       // For Address Family UNIX sockets
  #include <sys/un.h>
  #define DEF_API_SRVSOCK    "/dev/socket/srv_sf"
  #define DEF_API_CLISOCK    "/dev/socket/cli_sf"
  char api_srvsock [DEF_BUF] = DEF_API_SRVSOCK;
  char api_clisock [DEF_BUF] = DEF_API_CLISOCK;
  #define CS_FAM   AF_UNIX
#else                    // For Address Family NETWORK sockets
  #include <netinet/in.h>
  #include <netdb.h>
  //#define CS_PORT     2122    //1221
  int CS_PORT = 2122;
  #define CS_FAM AF_INET
#endif

#ifdef  CS_DGRAM
  #define CS_SOCK_TYPE SOCK_DGRAM
#else
  #define CS_SOCK_TYPE SOCK_STREAM
#endif



int sock_rx_tmo_set(int fd, int tmo) { // tmo = timeout in milliseconds
  struct timeval tv = {0, 0};
  tv.tv_sec = tmo / 1000; // Timeout in seconds
  tv.tv_usec = (tmo % 1000) * 1000;
  int ret = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (struct timeval *) & tv, sizeof(struct timeval));
  if (ret != 0) {
    loge ("timeout_set setsockopt SO_RCVTIMEO errno: %d", errno);
  }
  return 0;
}


// IPC API
int client_cmd(unsigned char * cmd_buf, int cmd_len, unsigned char * res_buf, int res_max) {
  logd("CS_PORT: %d  cmd_buf: \"%s\"  cmd_len: %d", CS_PORT, cmd_buf, cmd_len);
  static int sockfd = -1;
  int res_len, written;
  static socklen_t srv_len;
  #ifdef CS_AF_UNIX
      static struct sockaddr_un  srv_addr;
      #ifdef CS_DGRAM
          #define CS_DGRAM_UNIX
          struct sockaddr_un  cli_addr; // Unix datagram sockets must be bound; no ephemeral sockets.
          socklen_t cli_len;
      #endif
  #else
      //struct hostent *hp;
      struct sockaddr_in  srv_addr,cli_addr;
      socklen_t cli_len;
  #endif

  if (sockfd < 0) {
    if ((sockfd = socket (CS_FAM, CS_SOCK_TYPE, 0)) < 0) { // Get an ephemeral, unbound socket
      loge("client_cmd: socket errno: %d", errno);
      return 0; // "Error socket";
    }
    #ifdef CS_DGRAM_UNIX // Unix datagram sockets must be bound; no ephemeral sockets.
      unlink(api_clisock); // Remove any lingering client socket
      bzero((char *) & cli_addr, sizeof(cli_addr));
      cli_addr.sun_family = AF_UNIX;
      strncpy(cli_addr.sun_path, api_clisock, sizeof(cli_addr.sun_path));
      cli_len = strlen(cli_addr.sun_path) + sizeof(cli_addr.sun_family);

      if (bind(sockfd, (struct sockaddr *) & cli_addr,cli_len) < 0) {
        loge("client_cmd: bind errno: %d", errno);
        close(sockfd);
        sockfd = -1;
        return 0; // "Error bind"
        // OK to continue w/ Internet Stream but since this is Unix Datagram and we ran unlink (), let's fail
      }
    #endif
  }
  //!! Can move inside above
  // Setup server address
  bzero((char *)&srv_addr, sizeof(srv_addr));
  #ifdef CS_AF_UNIX
    srv_addr.sun_family = AF_UNIX;
    strlcpy(srv_addr.sun_path, api_srvsock, sizeof(srv_addr.sun_path));
    srv_len = strlen(srv_addr.sun_path) + sizeof(srv_addr.sun_family);
  #else
    srv_addr.sin_family = AF_INET;
    srv_addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    srv_addr.sin_port = htons(CS_PORT);
    srv_len = sizeof(struct sockaddr_in);
  #endif


  // Send cmd_buf and get res_buf
  #ifdef CS_DGRAM
    written = sendto(sockfd, cmd_buf, cmd_len, 0, (const struct sockaddr *)&srv_addr,srv_len);
    if (written != cmd_len) {  // Dgram buffers should not be segmented
      loge ("client_cmd: sendto errno: %d", errno);
      #ifdef CS_DGRAM_UNIX
        unlink(api_clisock);
      #endif
      close(sockfd);
      sockfd = -1;
      return 0; // Error sendto
    }

    sock_rx_tmo_set(sockfd, CS_RX_TMO);
    res_len = recvfrom(sockfd, res_buf, res_max, 0, (struct sockaddr *)&srv_addr, &srv_len);
    if (res_len <= 0) {
      loge("client_cmd: recvfrom errno: %d", errno);
      #ifdef  CS_DGRAM_UNIX
        unlink(api_clisock);
      #endif
      close(sockfd);
      sockfd = -1;
      return -1;
      // return (0); // Error recvfrom
    }
    #ifndef CS_AF_UNIX
      // !! ?? Don't need this ?? If srv_addr still set from sendto, should restrict recvfrom to localhost anyway ?
      if (srv_addr.sin_addr.s_addr != htonl(INADDR_LOOPBACK)) {
        loge("client_cmd: Unexpected suspicious packet from host");// %s", inet_ntop(srv_addr.sin_addr.s_addr)); //inet_ntoa(srv_addr.sin_addr.s_addr));
      }
    #endif
  #else
    if (connect(sockfd, (struct sockaddr *) &srv_addr, srv_len) < 0) {
      loge("client_cmd: connect errno: %d", errno);
      close(sockfd);
      sockfd = -1;
      return 0; // Error connect
    }
    written = write(sockfd, cmd_buf, cmd_len); // Write the command packet
    if (written != cmd_len) { // Small buffers under 256 bytes should not be segmented ?
      loge("client_cmd: write errno: %d", errno);
      close(sockfd);
      sockfd = -1;
      return 0; // Error write
    }

    sock_rx_tmo_set(sockfd, CS_RX_TMO);

    res_len = read(sockfd, res_buf, res_max)); // Read response
    if (res_len <= 0) {
      loge("client_cmd: read errno: %d", errno);
      close(sockfd);
      sockfd = -1;
      return 0; // Error read
    }
  #endif
  //hex_dump ("", 32, res_buf, n);
  #ifdef CS_DGRAM_UNIX
    unlink(api_clisock);
  #endif
  //close(sockfd);
  return res_len;
}

#define HD_MW 256
static void hex_dump(const char * prefix, int width, unsigned char * buf, int len) {
  char tmp[3 * HD_MW + 8] = ""; // Handle line widths up to HD_MW
  char line[3 * HD_MW + 8] = "";
  if (width > HD_MW) {
    width = HD_MW;
  }
  int i, n;
  line[0] = 0;

  if (prefix) {
    strlcpy(line, prefix, sizeof(line));
  }

  for (i = 0, n = 1; i < len; i ++, n ++) {
    snprintf(tmp, sizeof(tmp), "%2.2x ", buf[i]);
    strncat(line, tmp, sizeof(line));
    if (n == width) {
      n = 0;
      logd(line);
      line[0] = 0;
      if (prefix) {
        strlcpy(line, prefix, sizeof(line));
      }
    } else if (i == len - 1 && n) {
      logd (line);
    }
  }
}



int exiting = 0;

#define RES_DATA_MAX 1280

/**
 * Run until exiting != 0, passing incoming commands to server_work_func() and responding with the results
 */
int server_work() {
  int sockfd = -1;
  int newsockfd = -1;
  int cmd_len = 0;
  int ctr = 0;
  socklen_t cli_len = 0, srv_len = 0;

  #ifdef  CS_AF_UNIX
    struct sockaddr_un cli_addr = {0}, srv_addr = {0};
    srv_len = strlen (srv_addr.sun_path) + sizeof (srv_addr.sun_family);
  #else
    struct sockaddr_in  cli_addr = {0}, srv_addr = {0};
    //struct hostent *hp;
  #endif

  unsigned char cmd_buf[DEF_BUF] = {0};

  // system("chmod 666 /dev");            // !! Need su if in JNI
  // system("chmod 666 /dev/socket");

  #ifdef CS_AF_UNIX
    unlink(api_srvsock);
  #endif

  if ((sockfd = socket(CS_FAM, CS_SOCK_TYPE, 0)) < 0) {
    loge("server_work socket  errno: %d", errno);
    return -1;
  }

  bzero((char *) &srv_addr, sizeof(srv_addr));

  #ifdef CS_AF_UNIX
    srv_addr.sun_family = AF_UNIX;
    strncpy(srv_addr.sun_path, api_srvsock, sizeof(srv_addr.sun_path));
    srv_len = strlen(srv_addr.sun_path) + sizeof(srv_addr.sun_family);
  #else
    srv_addr.sin_family = AF_INET;
    srv_addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK); //INADDR_ANY;
    /*hp = gethostbyname("localhost");
    if (hp== 0) {
      loge ("Error gethostbyname  errno: %d", errno);
      return (-2);
    }
    bcopy((char *)hp->h_addr, (char *)&srv_addr.sin_addr, hp->h_length);*/
    srv_addr.sin_port = htons(CS_PORT);
    srv_len = sizeof(struct sockaddr_in);
  #endif

  #ifdef CS_AF_UNIX
    logd("srv_len: %d  fam: %d  path: %s", srv_len, srv_addr.sun_family, srv_addr.sun_path);
  #else
    logd("srv_len: %d  fam: %d  addr: 0x%x  port: %d", srv_len, srv_addr.sin_family, ntohl (srv_addr.sin_addr.s_addr), ntohs (srv_addr.sin_port));
  #endif

  if (bind(sockfd, (struct sockaddr *) &srv_addr, srv_len) < 0) {
    loge ("Error bind  errno: %d", errno);

    #ifdef CS_AF_UNIX
      return -3;
    #endif

    #ifdef CS_DGRAM
      return -3;
    #endif

    loge("Inet stream continuing despite bind error"); // OK to continue w/ Internet Stream
  }

  // Get command from client
  #ifndef CS_DGRAM
    if (listen(sockfd, 5)) { // Backlog= 5; likely don't need this
      loge("Error listen errno: %d", errno);
      return -4;
    }
  #endif

  logd ("server_work Ready");

  while (!exiting) {
    bzero((char *) &cli_addr, sizeof(cli_addr)); // ?? Don't need this ?
    //cli_addr.sun_family = CS_FAM;
    cli_len = sizeof(cli_addr);

    #ifdef CS_DGRAM
      cmd_len = recvfrom(sockfd, cmd_buf, sizeof(cmd_buf), 0, (struct sockaddr *) &cli_addr, &cli_len);
      if (cmd_len <= 0) {
        loge("Error recvfrom  errno: %d", errno);
        ms_sleep(100); // Sleep 0.1 second
        continue;
      }
      #ifndef CS_AF_UNIX
        if (cli_addr.sin_addr.s_addr != htonl(INADDR_LOOPBACK)) {
          //loge ("Unexpected suspicious packet from host %s", inet_ntop (cli_addr.sin_addr.s_addr));
          loge("Unexpected suspicious packet from host");// %s", inet_ntoa (cli_addr.sin_addr.s_addr));
        }
      #endif
    #else
      newsockfd = accept(sockfd, (struct sockaddr *) &cli_addr, &cli_len);
      if (newsockfd < 0) {
        loge("Error accept  errno: %d", errno);
        ms_sleep(100); // Sleep 0.1 second
        continue;
      }
      #ifndef CS_AF_UNIX
        if (cli_addr.sin_addr.s_addr != htonl(INADDR_LOOPBACK) ) {
          loge ("Unexpected suspicious packet from host");// %s", inet_ntoa (cli_addr.sin_addr.s_addr));
        }
      #endif
      cmd_len = read(newsockfd, cmd_buf, sizeof(cmd_buf));
      if (cmd_len <= 0) {
        loge("Error read errno: %d", errno);
        ms_sleep(100);   // Sleep 0.1 second
        close(newsockfd);
        ms_sleep(100);   // Sleep 0.1 second
        continue;
      }
    #endif

    unsigned char res_buf[RES_DATA_MAX] = {0};
    int res_len = 0;

    cmd_buf[cmd_len] = 0; // Null terminate for string usage
    res_len = server_work_func(cmd_buf, cmd_len, res_buf, sizeof(res_buf)); // Do server command function and provide response

    if (res_len < 0) { // If error
      res_len = 2;
      res_buf[0] = '?';
      res_buf[1] = '\n';
      res_buf[2] = 0;
    }

    // Send response
    #ifdef CS_DGRAM
      if (sendto(sockfd, res_buf, res_len, 0, (struct sockaddr *) &cli_addr, cli_len) != res_len) {
        loge ("Error sendto  errno: %d  res_len: %d", errno, res_len);
        ms_sleep(100); // Sleep 0.1 second
      }
    #else
      if (write(newsockfd, res_buf, res_len) != res_len) {
        loge ("Error write  errno: %d", errno);
        ms_sleep(100); // Sleep 0.1 second
      }
      close(newsockfd);
    #endif
  }
  close(sockfd);
  #ifdef CS_AF_UNIX
    unlink(api_srvsock);
  #endif

  return 0;
}


int freq_inc = 100;   // EU

int curr_tuner_freq_int         = -7;
int curr_tuner_rssi_int         = -7;
int curr_tuner_stereo_int       = 0;
int curr_tuner_rds_pi_int       = -7;
int curr_tuner_rds_pt_int       = -7; // was -7

char curr_radio_dai_state    [16]= "stop";
char curr_tuner_band         [16]= "EU";

char curr_tuner_state        [16]= "stop";
char curr_tuner_scan_state   [16]= "stop";
char curr_tuner_rds_state    [16]= "stop";
char curr_tuner_rds_af_state [16]= "stop";

char curr_tuner_freq         [16]= "-7";//"107900";
char curr_tuner_stereo       [16]= "stereo";
char curr_tuner_thresh       [16]= "-7";
char curr_tuner_rssi         [16]= "RSSI";
char curr_tuner_most         [16]= "-7";

char curr_tuner_rds_pi       [16]= "-7";//"";
char curr_tuner_rds_pt       [16]= "-7";//"-";
char curr_tuner_rds_ps       [16]= "RDS PS";//"-";
char curr_tuner_rds_rt       [96]= "RDS RT";//"-";

void cb_tuner_change(char * key, char * val) {
//  logd ("cb_tuner_change key: %s  val: %s", key, val);
}

char cval[16] = "-3";
char * itoa(int val) {
  snprintf(cval, sizeof(cval) - 1, "%d", val);
  return cval;
}

char * itostereo(int stereo) {
  return stereo ? "stereo" : "mono";
}

void cb_tnr_stereo(int stereo) {
  //logd ("cb_tnr_stereo stereo: %d", stereo);
  if (curr_tuner_stereo_int != stereo) {
    curr_tuner_stereo_int = stereo;
    strncpy(curr_tuner_stereo, itostereo(stereo), sizeof(curr_tuner_stereo));
    cb_tuner_change("tuner_stereo", curr_tuner_stereo);
  }
}

void cb_tnr_rds(struct fmradio_rds_bundle_t * rb, int freq) {
  //logd ("cb_tnr_rds rds_bundle psn: \"%s\"  rt: \"%s\"  ct: \"%d\"  ptyn: \"%d\"", rb->psn, rb->rt, rb->ct, rb->ptyn);
  //logd ("cb_tnr_rds rds_bundle: %p  freq: %d  pi: %d  tp: %d  pty: %d  ta: %d  ms: %d  taf: %d  num_afs: %d", rb, freq, rb->pi, rb->tp, rb->pty, rb->ta, rb->ms, rb->taf, rb->num_afs);
  //for (ctr = 0; ctr < RDS_NUMBER_OF_TMC; ctr ++)
  //  logd ("cb_tnr_rds rds_bundle tmc %2.2d: %d", rb->tmc [ctr]);

  //logd ("cb_tnr_rds freq: %d  rds_bundle  pi: %d  pty: %d psn: \"%s\"  rt: \"%s\"  num_afs: %d", freq, rb->pi, rb->pty, rb->psn, rb->rt, rb->num_afs);

  for (int ctr = 0; ctr < rb->num_afs; ctr ++)
    logd ("cb_tnr_rds rds_bundle af %2.2d: %d", rb->af[ctr]);

  if (curr_tuner_freq_int != freq) {
    curr_tuner_freq_int = freq;
    strncpy(curr_tuner_freq, itoa(curr_tuner_freq_int), sizeof(curr_tuner_freq));
    cb_tuner_change("tuner_freq", curr_tuner_freq);
  }

  if (curr_tuner_rds_pi_int != rb->pi) {
    curr_tuner_rds_pi_int = rb->pi;
    strncpy(curr_tuner_rds_pi, itoa(curr_tuner_rds_pi_int), sizeof(curr_tuner_rds_pi));
    cb_tuner_change("tuner_rds_pi", curr_tuner_rds_pi);
  }

  if (curr_tuner_rds_pt_int != rb->pty) {
    curr_tuner_rds_pt_int = rb->pty;
    strncpy(curr_tuner_rds_pt, itoa(curr_tuner_rds_pt_int), sizeof(curr_tuner_rds_pt));
    cb_tuner_change("tuner_rds_pt", curr_tuner_rds_pt);
  }

  if (strncmp(curr_tuner_rds_ps, rb->psn, sizeof(curr_tuner_rds_ps))) {
    strncpy(curr_tuner_rds_ps, rb->psn, sizeof(curr_tuner_rds_ps));
    cb_tuner_change("tuner_rds_ps", rb->psn);
  }

  //logd("Radiotext: \"%s\"", rb->rt);
  //    space_trim (rb->rt);
  //logd ("Radiotext: \"%s\"", rb->rt);
  if (strncmp(curr_tuner_rds_rt, rb->rt, sizeof(curr_tuner_rds_rt))) {
    strncpy(curr_tuner_rds_rt, rb->rt, sizeof(curr_tuner_rds_rt));
    cb_tuner_change("tuner_rds_rt", rb->rt);
  }
}

void cb_tnr_rssi(int rssi) {
  logd("cb_tnr_rssi rssi: %d", rssi); // rssi: (760 - 347) / 19 = 21.74. Thus internal range thus is 0 - 46
  if (curr_tuner_rssi_int != rssi) {
    curr_tuner_rssi_int = rssi;
    strncpy(curr_tuner_rssi, itoa(curr_tuner_rssi_int), sizeof(curr_tuner_rssi));
    cb_tuner_change("tuner_rssi", curr_tuner_rssi);
  }
}

void cb_tnr_rds_af(int freq, enum fmradio_switch_reason_t reason) {
  logd("cb_tnr_rds_af freq: %d  reason: %d", freq, reason);
}
void cb_tnr_state(enum fmradio_reset_reason_t reason) {
  logd("cb_tnr_state reason: %d", reason);
  strncpy(curr_tuner_state, "stop", sizeof(curr_tuner_state));
  cb_tuner_change("tuner_state", curr_tuner_state);
}

struct fmradio_vendor_callbacks_t * g_cbp;

int tuner_initialized = 0;

int tuner_init() {

  tuner_initialized = 0;

  // For Qualcomm FM/combo chips:
  //system ("su -c \"setprop hw.fm.mode normal ; setprop hw.fm.version 0 ; setprop ctl.start fm_dl\"");

  // We run as root now. :)
  //system ("setprop hw.fm.mode normal ; setprop hw.fm.version 0 ; setprop ctl.start fm_dl");

  int ret = -1;

  tnr_funcs = calloc(1, sizeof(struct fmradio_vendor_methods_t)); // Setup functions

  if (tnr_funcs == NULL) {
    return -1;
  }

  if (!lib_load()) {
    loge("lib_load errno: %d", errno);
    return -1;
  }

  //funcs_display(); // Show functions

  g_cbp = calloc(1, sizeof(struct fmradio_vendor_callbacks_t)); // Setup callbacks
  if (g_cbp == NULL) {
    return -1;
  }

  g_cbp->on_playing_in_stereo_changed   = cb_tnr_stereo;
  g_cbp->on_rds_data_found              = cb_tnr_rds;
  g_cbp->on_signal_strength_changed     = cb_tnr_rssi;
  g_cbp->on_automatic_switch            = cb_tnr_rds_af;
  g_cbp->on_forced_reset                = cb_tnr_state;

  tuner_initialized = 1;
  return 0;
}


  #include <stdbool.h>
  #define boolean bool
  //#define false 0
  //#define true  1
  #define byte unsigned char

  #include "alsa.c"

  char * audio_dev_name = "/dev/snd/controlC0";
  int snd_fd = -1;
  static int native_alsa_cmd (int type, char * key, int value) {
    if (snd_fd < 0) {
      snd_fd = open (audio_dev_name, O_NONBLOCK | O_RDWR ); //O_RDWR, 0);//S_IRWXU | S_IRWXG | S_IRWXO);// // O_RDONLY, O_WRONLY, or O_RDWR
      if (snd_fd < 0) {
        printf ("Error opening device1 %s errno: %s (%d)\n", audio_dev_name, strerror (errno), errno);
        return (-2);
      }
      printf ("snd_fd: %d\n", snd_fd);
    }
    int ret = m4_do (snd_fd, type, key, value);                          // Func, ID, value
    return (ret);
  }

  static int alsa_bool_set (char * key, int value) {
    int ret = native_alsa_cmd (1, key, value);
    return (ret);
  }
  static int alsa_int_set (char * key, int value) {
    int ret = native_alsa_cmd (2, key, value);
    return (ret);
  }
  static int alsa_enum_set (char * key, int value) {
    int ret = native_alsa_cmd (3, key, value);
    return (ret);
  }
  static int alsa_long_set(char *key, long value) { // неправильно
    return native_alsa_cmd(2, key, value);
  }

static int sys_run(char* cmd) {
  int ret = system(cmd); // !! Binaries like ssd that write to stdout cause C system() to crash !
  logd("sys_run ret: %d; cmd: \"%s\"", ret, cmd);
  return ret;
}

static char sys_cmd[32768] = {0};

static int sys_commit() {
  int ret = sys_run(sys_cmd); // Run
  sys_cmd[0] = 0;             // Done, so zero
  return ret;
}


// AFE_PCM_TX ???
// https://stackoverflow.com/questions/21024851/redirecting-audio-creating-alternate-sound-paths-in-android
int qcv_digital_input_on() {
  alsa_bool_set("MultiMedia1 Mixer INTERNAL_FM_TX", 1);
  alsa_bool_set("MultiMedia1 Mixer SLIM_0_TX", 0);                 // Turn off microphone path

  // START FROM LATEST VERSION
  //alsa_bool_set("MultiMedia1 Mixer SLIM_0_TX", 0); // Turn off microphone path to MM 1 (already was)
  //alsa_enum_set("SLIM_0_TX Channels", 1);          // 2 Set SLIMBus TX channels     to "2"
  //alsa_enum_set("SLIM_0_TX Channels", 0);
  //alsa_enum_set("SLIM_0_TX Channels", 1);          // 2 Set SLIMBus TX channels     to "2"

  // MotoG, Z1 re-enable camcorder microphone sometimes !
  alsa_long_set("ADC1 Volume", 0);
  alsa_long_set("ADC2 Volume", 0);
  alsa_long_set("ADC3 Volume", 0);
  //alsa_long_set("ADC4 Volume", 0);                 // ADC4 not used on MOG
  alsa_long_set("DEC1 Volume", 0);
  alsa_long_set("DEC2 Volume", 0);                   // Xperia Z1
  //if (!msm8226_get()) {                              // If not MotoG msm8226 chipset...
    alsa_long_set("ADC4 Volume", 0);                 // Xperia Z1
    alsa_long_set("ADC5 Volume", 0);                 // Xperia Z1
    //alsa_long_set("ADC6 Volume", 0);
    alsa_long_set("DEC3 Volume", 0);                 // Xperia Z1
    //alsa_long_set("DEC4 Volume", 0);
    alsa_long_set("DEC5 Volume", 0);
    //alsa_long_set ("DEC6 Volume", 0);
    //alsa_long_set ("DEC7 Volume", 0);
    //alsa_long_set ("DEC8 Volume", 0);
    //alsa_long_set ("DEC9 Volume", 0);
 // }

  // END FROM LATEST VERSION
  ms_sleep(100);
  return 0;
}
int qcv_digital_input_off() {

  // START FROM LATEST VERSION
  alsa_long_set("ADC1 Volume", 19);
  alsa_long_set("ADC2 Volume", 19);
  alsa_long_set("ADC3 Volume", 19);
  alsa_long_set("DEC1 Volume", 84);
  alsa_long_set("DEC2 Volume", 84);
 // if (!msm8226_get()) {
    alsa_long_set("ADC4 Volume", 19);
    alsa_long_set("ADC5 Volume", 19);
    alsa_long_set("DEC3 Volume", 84);
    alsa_long_set("DEC5 Volume", 84);
  //}
  // END FROM LATEST VERSION

  alsa_bool_set("MultiMedia1 Mixer INTERNAL_FM_TX", 0);
  alsa_bool_set("MultiMedia1 Mixer SLIM_0_TX", 1);                 // Turn on microphone path
  ms_sleep(100);
  return 0;
}

int dev_digital_input_on() {
  return qcv_digital_input_on();
}

int dev_digital_input_off() {
  return qcv_digital_input_off();
}

char* set_radio_dai_state(char* dai_state) {
  if (!strncasecmp(dai_state, "start", 5)) {
    logd("dai Start: %d", dev_digital_input_on());
  } else if (!strncasecmp(dai_state, "stop", 4)) {
    logd("dai Stop: %d", dev_digital_input_off());
  } else {
    logd("dai Unknown: %s", dai_state);
  }
  return dai_state;
}

int server_work_func(char* cmd_buf, int cmd_len, char* res_buf, int res_max) {
  s2d_cmd_log = !!file_get("/mnt/sdcard/sf/s2d_log");

  if (s2d_cmd_log) {
    logd ("server_work_func cmd_len: %d  cmd_buf: \"%s\"", cmd_len, cmd_buf);
  }

  int res_len = tuner_server_work_func(cmd_buf, cmd_len, res_buf, res_max);

  if (s2d_cmd_log) {
    logd ("res_len: %d  res_buf: \"%s\"", res_len, res_buf);
  }

  return res_len;
}


int tuner_server_work_func(char* cmd_buf, int cmd_len, char* res_buf, int res_max) {
  int terminate = 0;

  int ret = 0;
  char key[DEF_BUF] = {0};
  size_t klen = 0;
  char* ckey = &cmd_buf[2]; // Command/compare key

  if (cmd_buf == NULL) {
    loge ("!!!!!!!!!!!!");
  } else if (cmd_buf[0] == 'q') { // Quit
    terminate = 1;
  } else if (cmd_buf[0] == 'z') { // Sleep
    int secs = 1;
    if (strlen(cmd_buf) > 2) {
      secs = atoi(ckey);
    }
    sleep(secs);
  } else if (strlen(cmd_buf) < 3) { // Ignore short lines that aren't 'q' or 'z'
  }

  // Set
  if (cmd_buf != NULL && strlen(cmd_buf) > 2 && cmd_buf[0] == 's') {
    /**
     * `s key value`
     * val = address of "key" ; add klen later when determined
     */
    char* val = &cmd_buf[3];
    char cval[DEF_BUF] = {0}; // Compare value filled in when testing each possible value, for defined states
    size_t clen = 0;

    if (strcpy(key, "tuner_band") && (klen = strlen (key)) && !strncmp(ckey, key, klen)) { // Tuner Band USED TO HAVE TO be set before Tuner State Start
      val += klen;
      if (strcpy(cval, "EU") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // EU
        freq_inc = 100;
      } else if (strcpy(cval, "US") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // US
        freq_inc = 200;
      } else {
        cval[0] = 0;
      }
      if (cval[0]) {
        strncpy(curr_tuner_band, cval, sizeof(curr_tuner_band));
        //logd ("set_band: %d", tnr_funcs->set_band (NULL, atoi (val + klen)));         freq_inc used in tuner start
        if (freq_inc < 200) {
          tnr_funcs->send_extra_command(NULL, "990", NULL, NULL);
        } else {
          tnr_funcs->send_extra_command(NULL, "991", NULL, NULL);
        }
      }
    } else if (strcpy(key, "tuner_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) { // Tuner State
      val += klen;
      if (strcpy(cval, "start") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Start
        ret = 0;
        if (tuner_initialized == 0) {
          ret = tuner_init();
          if (ret) {
            loge("tuner_init: %d", ret);
          } else {
            logd("tuner_init: %d", ret);
          }
        }

        if (ret == 0) {
          // Start receive: Enable API, power on chip, start rx_thread
          logd("rx_start: %d", ret = tnr_funcs->rx_start(NULL, g_cbp, 87500, 108000, 106100, freq_inc));
        }

        if (ret != 0) {
          strncpy(cval, "stop", sizeof(cval));
        }
      } else if (tuner_initialized && strcpy(cval, "stop") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Stop
        terminate = 1;
      } else if (tuner_initialized && strcpy(cval, "pause") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Pause
        logd("pause: %d", tnr_funcs->pause(NULL));
      } else if (tuner_initialized && strcpy(cval, "resume") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Resume
        logd("resume: %d", tnr_funcs->resume(NULL));
      } else {
        cval[0] = 0;
      }

      if (cval[0]) {
        strncpy(curr_tuner_state, cval, sizeof(curr_tuner_state));
      }
    } else if (tuner_initialized == 0) {  // Remaining set commands require tuner_initialized:
    } else if (strcpy(key, "tuner_scan_state") && (klen = strlen (key)) && !strncmp(ckey, key, klen)) { // Tuner Scan State
      val += klen;
      if (strcpy(cval, "up") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) {
        logd("scan: %d", ret = tnr_funcs->scan(NULL, 1));
      } else if (strcpy(cval, "down") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) {
        logd("scan: %d", ret = tnr_funcs->scan(NULL, 0));
      } else if (strcpy(cval, "stop") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) {
        logd("stop_scan: %d", ret = tnr_funcs->stop_scan(NULL));
      } else {
        cval[0] = 0;
      }

      if (cval[0]) {
        strncpy(curr_tuner_scan_state, cval, sizeof(curr_tuner_scan_state));
      }
    } else if (strcpy(key, "tuner_rds_state") && (klen = strlen (key)) && !strncmp(ckey, key, klen)) { // Tuner RDS State
      val += klen;
      int rds = 0;
      if (strcpy(cval, "start") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Start
        rds = 1;
      } else if (strcpy(cval, "stop") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Stop
        rds = 0;
      } else {
        cval[0] = 0;
      }

      if (cval[0]) {
        strncpy(curr_tuner_rds_state, cval, sizeof(curr_tuner_rds_state));
        logd("set_rds_reception: %d", ret = tnr_funcs->set_rds_reception(NULL, rds));
      }
    } else if (strcpy(key, "tuner_rds_af_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) { // Tuner RDS AF State
      val += klen;
      int af = 0;
      if (strcpy(cval, "start") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Start
        af = 1;
      } else {
        cval[0] = 0;
      }

      if (cval[0]) {
        strncpy(curr_tuner_rds_af_state, cval, sizeof(curr_tuner_rds_af_state));
        logd("set_automatic_af_switching: %d", ret = tnr_funcs->set_automatic_af_switching(NULL, af));
      }
    } else if (strcpy(key, "tuner_stereo") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      val += klen;
      if (strcpy(cval, "stereo") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Stereo
        logd("set_force_mono 0: %d", tnr_funcs->set_force_mono(NULL, 0));
      } else if (strcpy(cval, "mono") && (clen = strlen(cval)) && !strncasecmp(val, cval, clen)) { // Mono
        logd("set_force_mono 1: %d", tnr_funcs->set_force_mono(NULL, 1));
      } else {
        cval[0] = 0;
      }

      if (cval[0]) {
        strncpy(curr_tuner_stereo, cval, sizeof(curr_tuner_stereo));
      }
    } else if (strcpy(key, "tuner_freq") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      val += klen;
      logd("set_frequency: %d", tnr_funcs->set_frequency(NULL, atoi(val)));
      curr_tuner_freq_int = atoi(val);
      strncpy(curr_tuner_freq, val, sizeof(curr_tuner_freq));
    } else if (strcpy(key, "tuner_thresh") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      val += klen;
      logd("set_threshold: %d", tnr_funcs->set_threshold(NULL, atoi(val)));
      strncpy(curr_tuner_thresh, val, sizeof(curr_tuner_thresh));
    } else if (strcpy(key, "radio_dai_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      val += klen;
      logd("radio_dai_state: %s", set_radio_dai_state(val));
      strncpy(curr_radio_dai_state, val, sizeof(curr_radio_dai_state));
    }

                                 // After set completes:
    cmd_buf[0] = 'g';            // Response is same as get() for specified variable
    if (terminate) {             // If terminating...
      cmd_buf = "g tuner_state"; // Response = get tuner_state   (!! Reassigns char * !!)
      strncpy(curr_tuner_state, "stop", sizeof(curr_tuner_state));
      exiting = 1;
    }
  }

  // Get (or Set response)
  if (cmd_buf != NULL && strlen(cmd_buf) > 2 && cmd_buf[0] == 'g') {
    strncpy(res_buf, "-9999", res_max); // Default = "g key -9999"
    if (0);
    else if (strcpy(key, "radio_dai_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_radio_dai_state);
    } else if (strcpy(key, "tuner_band") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_band);
    } else if (strcpy(key, "tuner_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_state);
    } else if (strcpy(key, "tuner_scan_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_scan_state);
    } else if (strcpy(key, "tuner_rds_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_rds_state);
    } else if (strcpy(key, "tuner_rds_af_state") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_rds_af_state);
    } else if (strcpy(key, "tuner_freq") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      if (tuner_initialized) {
        curr_tuner_freq_int = tnr_funcs->get_frequency(NULL);
      }
      strncpy(curr_tuner_freq, itoa(curr_tuner_freq_int), sizeof(curr_tuner_freq));
      snprintf(res_buf, res_max -1, "%s", curr_tuner_freq);
    } else if (strcpy(key, "tuner_stereo") && (klen = strlen (key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_stereo);
    } else if (strcpy(key, "tuner_thresh") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      if (tuner_initialized) {
        strncpy(curr_tuner_thresh, itoa(tnr_funcs->get_threshold(NULL)), sizeof(curr_tuner_thresh));
      }
      snprintf(res_buf, res_max - 1, "%s", curr_tuner_thresh);
    } else if (strcpy(key, "tuner_rssi") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      if (tuner_initialized) {
        curr_tuner_freq_int = tnr_funcs->get_signal_strength(NULL);
      }
      strncpy(curr_tuner_rssi, itoa(curr_tuner_freq_int), sizeof(curr_tuner_rssi));
      snprintf(res_buf, res_max - 1, "%s", curr_tuner_rssi);
    } else if (strcpy(key, "tuner_most") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      if (tuner_initialized) {
        strncpy(curr_tuner_most, itostereo(tnr_funcs->is_playing_in_stereo(NULL)), sizeof(curr_tuner_most));
      }
      snprintf(res_buf, res_max -1, "%s", curr_tuner_most);
    } else if (strcpy(key, "tuner_rds_pi") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_rds_pi);
    } else if (strcpy(key, "tuner_rds_pt") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_rds_pt);
    } else if (strcpy(key, "rds_ps") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_rds_ps);
    } else if (strcpy(key, "rds_rt") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      snprintf(res_buf, res_max -1, "%s", curr_tuner_rds_rt);
    } else if (strcpy(key, "test_data") && (klen = strlen(key)) && !strncmp(ckey, key, klen)) {
      char* st = (char*) malloc(500);
      tnr_funcs->get_test_data(&st);
      snprintf(res_buf, res_max -1, "%s", st);
      free(st);
    }

    if (strlen(res_buf) <- 0) {
      strncpy(res_buf, "-4949", res_max);
    }

    return strlen(res_buf);
  }

  strncpy(res_buf, "-555", res_max);
  return strlen(res_buf);
}



/*
  void info_display () {
    //int ret = 0;
    logd ("get_frequency:             %d", tnr_funcs->get_frequency (NULL));
    logd ("get_signal_strength:       %d", tnr_funcs->get_signal_strength (NULL));
    logd ("is_playing_in_stereo:      %d", tnr_funcs->is_playing_in_stereo (NULL));
    //logd ("is_rds_data_supported:     %d", tnr_funcs->is_rds_data_supported (NULL));
    logd ("is_tuned_to_valid_channel: %d", tnr_funcs->is_tuned_to_valid_channel (NULL));
    logd ("get_threshold:             %d", tnr_funcs->get_threshold (NULL));       // 0x1f4
  }
*/



int s2d_cmd(int cmd_len, char* cmd_buf, int res_len, char* res_buf) {
  res_len = client_cmd(cmd_buf, cmd_len, res_buf, res_len); // Send cmd_buf and get response to res_buf
  if (res_len > 0 && res_len <= DEF_BUF) {
    memcpy(cmd_buf, res_buf, res_len);
    return res_len;
  }
  cmd_buf[0] = '7';
  cmd_buf[1] = 0;
  return 2;
}

  //#include <readline/readline.h>
  //#include <readline/history.h>

  #include <signal.h>
  static struct sigaction old_sa[NSIG];
  void android_sigaction (int signal, siginfo_t * info, void * reserved) {
    //if (_hndl > 0)
    //  close (_hndl);
    old_sa [signal].sa_handler (signal);                                // Original signal handler
  }

  void signal_catch_start () {
	// Try to catch crashes...
    struct sigaction handler;
    memset (& handler, 0, sizeof (sigaction));
    handler.sa_sigaction = android_sigaction;
    handler.sa_flags = SA_RESETHAND;

    #define CATCHSIG(X) sigaction(X, &handler, &old_sa[X])
    CATCHSIG (SIGILL);
    CATCHSIG (SIGABRT);
    CATCHSIG (SIGBUS);
    CATCHSIG (SIGFPE);
    CATCHSIG (SIGSEGV);
    CATCHSIG (SIGSTKFLT);
    CATCHSIG (SIGPIPE);
  }

/**
 * Main function
 */
int main(int argc, char ** argv) {
  if (argc > 1) {
    strncpy(logtag, "s2d......", sizeof(logtag));
  } else {
    strncpy(logtag, "s2c......", sizeof(logtag));
  }

  logd("Spirit FM Radio s2d utility version 2014, Nov 2"); // !! Need automatic version
  logd(copyright);                                         // Copyright

  exiting = 0;

  char store_line[DEF_BUF] = {0};
  char * line = store_line;

  if (argc > 1) {
    logd("Server mode argv [1]: %s", argv[1]);
    curr_radio_device_int = atoi(argv[1]);
    logd("Server mode curr_radio_device_int: %d", curr_radio_device_int);

    int ret = daemon(0, 0);
    logd ("daemon() ret: %d", ret);

    //signal_catch_start ();

    while (!exiting) {
      server_work();
      if (!exiting) { // Must be an error
        sleep(3);
      }
    }
  } else {
    logd("Client mode");

    while (!exiting && line != NULL) {
      //line = readline (store_line);       // Called must free malloc'd memory
      //line = gets (store_line);
      line = fgets(store_line, sizeof(store_line), stdin);
      //logd ("line: %p", line);

      if (line == NULL) {
      } else {
        int cmd_len = strlen(line);// + 1;
        if (cmd_len > 0 && cmd_len < sizeof(store_line)) {
          char * cmd_buf = line;
          if (cmd_buf[cmd_len - 1] == '\r' || cmd_buf[cmd_len - 1] == '\n') {
            cmd_buf [cmd_len - 1] = 0;
          }
          logd("To client_cmd cmd_len: %d; cmd_buf: \"%s\"", cmd_len, cmd_buf);
          unsigned char res_buf[RES_DATA_MAX] = {0};
          int res_len = client_cmd(cmd_buf, cmd_len + 1, res_buf, sizeof(res_buf));  // Send command at line / store_line and get response to res_buf
          logd("res_len: %d  res_buf: \"%s\"", res_len, res_buf);
        }
      }
    }
    return 0;
  }

  // Here because done. Turn FM off and exit.
  if (tuner_initialized) {
    int ret = tnr_funcs->reset(NULL);
    logd("main reset: %d", ret);
    //lib_unload ();
    //tuner_initialized = 0;
  }

  logd("main done");
  return 0;
}
#pragma clang diagnostic pop