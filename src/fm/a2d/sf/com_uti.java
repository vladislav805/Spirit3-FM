// General utility functions

package fm.a2d.sf;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.StrictMode;
import android.util.Log;
import fm.a2d.sf.helper.L;
import fm.a2d.sf.helper.Utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;


@SuppressWarnings("WeakerAccess")
public final class com_uti {

  private static int stat_constrs = 1;

  public static boolean ssd_via_sys_run = false;
  private static String sys_cmd = "";
  private static boolean lg2_stock_set = false;
  private static boolean lg2_stock = false;
  private static boolean htc_have = false;
  private static boolean htc_have_set = false;
  private static boolean htc_stock_set = false;
  private static boolean htc_stock = false;

  //                                      Tuner   Audio
  public static final int DEV_UNK = -1;
  public static final int DEV_GEN = 0;  // gen      gen
  public static final int DEV_GS1 = 1;  // ssl      gs1
  public static final int DEV_GS2 = 2;  // ssl      gs2
  public static final int DEV_GS3 = 3;  // ssl      gs3
  public static final int DEV_QCV = 4;  // qcv      qcv
  public static final int DEV_ONE = 5;  // bch      one
  public static final int DEV_LG2 = 6;  // bch      lg2
  public static final int DEV_XZ2 = 7;  // bch      xz2
  public static final int DEV_SDR = 8;  // sdr      sdr

  public static String m_device = "";
  public static String m_board = "";
  public static String m_manufacturer = "";

  public static int device = device_get();//DEV_UNK;

  private static int dg_s2d_cmd_sem = 0;
  private static boolean dg_s2d_log = true;//false;
  //private static boolean enable_non_audio = true;


  private static final int DEF_BUF = 512;
  private static boolean s2d_cmd_log = false;

  private static int band_freq_lo = 87500;
  private static int band_freq_hi = 108000;
  private static int band_freq_inc = 100;
  private static int band_freq_odd = 0;

  private static void log(String s) {
    L.w(L.T.UTILS, s);
  }

  public com_uti() { // Default constructor
    log("com_uti: started " + stat_constrs++);

    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      public void uncaughtException(Thread aThread, Throwable aThrowable) {
        log("!!!!!!!! Uncaught exception: " + aThrowable);
      }
    });
  }

  public static void strict_mode_set(boolean strict_mode) {
    if (!strict_mode) {
      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
              .permitAll()
              .build());
      return;
    }

    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .detectAll() // For all detectable problems
            .penaltyLog()
            .build());

    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()
            //.penaltyDeath ()
            //.penaltyDialog ()   ????
            .build());
  }


  private static final int max_log_char = 7;//8;

  public static boolean logd_enable = true;
  public static boolean loge_enable = true;

  /**
   * @deprecated
   */
  public static void logd(String text) {
    if (!logd_enable) {
      return;
    }

    StackTraceElement stack_trace_el = new Exception().getStackTrace()[1];
    String tag = stack_trace_el.getFileName().substring(0, max_log_char);
    int idx = tag.indexOf(".");
    if (idx > 0 && idx < max_log_char) {
      tag = tag.substring(0, idx);
    }
    Log.d("sf" + tag, /*method2 + ":" +*/ stack_trace_el.getMethodName() + ": " + text);
  }

  /**
   * @deprecated
   */
  public static void loge(String text) {
    if (!loge_enable)
      return;
    final StackTraceElement stack_trace_el = new Exception().getStackTrace()[1];
    //final StackTraceElement   stack_trace_el2 = new Exception ().getStackTrace () [2];
    String tag = stack_trace_el.getFileName().substring(0, max_log_char);
    int idx = tag.indexOf(".");
    if (idx > 0 && idx < max_log_char)
      tag = tag.substring(0, idx);
    String method = stack_trace_el.getMethodName();
    //String method2 = stack_trace_el2.getMethodName ();
    Log.e("sf" + tag, /*method2 + ":" +*/ method + ": " + text);
  }


  // The API for Integer.valueOf(String) says the String is interpreted exactly as if it were given to Integer.parseInt(String).
  // However, valueOf(String) returns a new Integer() object whereas parseInt(String) returns a primitive int.

  public static long long_get(String val) {
    return (long_get(val, 0));
  }

  public static long long_get(String val, long def) {
    try {
      if (val == null) {
        return def;
      }
      if (val.isEmpty()) {
        return def;
      }
      return Long.parseLong(val);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return def;
  }

  public static int int_get(String val) {
    return int_get(val, 0); // Get integer, default = 0
  }

  public static int int_get(String val, int def) {
    try {
      if (val == null) {
        return def;
      }
      if (val.isEmpty()) {
        return def;
      }
      return (int) Double.parseDouble(val);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return def;
  }

  public static double double_get(String val) {
    return double_get(val, 0);
  }

  public static double double_get(String val, double def) {
    try {
      if (val == null)
        return (def);
      if (val.equals(""))
        return (def);
      double dval = Float.parseFloat(val);
      return (dval);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return (def);
  }

  // Time:

  public static void ms_sleep(int ms) {
    if (ms > 10) {
      log("sleep " + ms + "ms");
    }
    try {
      Thread.sleep(ms); // Wait ms milliseconds
    } catch (InterruptedException ignore) { }
  }

  public static long tmr_ms_get() {
    return System.nanoTime() / 1000000;
  }

  public static long ms_get() {
    return System.currentTimeMillis();
  }

  public static String file_create(Context context, int id, String filename) {
    // When daemon running !!!! java.io.FileNotFoundException: /data/data/com.WHATEVER.fm/files/sprtd (Text file busy)

    if (context == null) {
      return "";
    }

    String full_filename = context.getFilesDir() + "/" + filename;
    try {
      InputStream ins = context.getResources().openRawResource(id); // Open raw resource file as input
      int size = ins.available(); // Get input file size (actually bytes that can be read without indefinite wait)

      if (size > 0 && file_size_get(full_filename) == size) {
        // If file already exists and size is unchanged... (assumes size will change on update !!)

        log("file exists size unchanged"); // !! Have to deal with updates !! Could check filesize, assuming filesize always changes.
        // Could use indicator file w/ version in file name... SSD running problem for update ??
        // Hypothetically, permissions may not be set for ssd due to sh failure

        //!! Disable to re-write all non-EXE w/ same permissions and all EXE w/ permissions 755 !!!!        return (full_filename);                                         // Done

      }

      byte[] buffer = new byte[size];  // Allocate a buffer
      ins.read(buffer);                // Read entire file into buffer. (Largest file is s.wav = 480,044 bytes)
      ins.close();                     // Close input file

      FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE); // | MODE_WORLD_WRITEABLE      // NullPointerException here unless permissions 755
      // Create/open output file for writing
      fos.write(buffer);                                               // Copy input to output file
      fos.close();                                                     // Close output file

      com_uti.sys_run("chmod 755 " + full_filename + " 1>/dev/null 2>/dev/null", false); // Set execute permission; otherwise rw-rw----
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

    return full_filename;
  }

  public static int sys_run(String cmd, boolean su) { // !! Crash in logs if any output to stderr !!
    try {
      String prefix = su ? "su" : "sh";
      Process p = Runtime.getRuntime().exec(prefix);
      DataOutputStream os = new DataOutputStream(p.getOutputStream());

      log(">>> " + prefix + " " + cmd);
      os.writeBytes(cmd + "\n");
      os.writeBytes("exit\n");
      os.flush();

      int exitValue = p.waitFor();
      log("exitValue: " + exitValue);

      return exitValue;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }


/* There are four overloaded versions of the exec() command:
    public Process exec(String command);
    public Process exec(String [] cmdArray);
    public Process exec(String command, String [] envp);
    public Process exec(String [] cmdArray, String [] envp);

For each of these methods, a command -- and possibly a set of arguments -- is passed to an operating-system-specific function call.
This subsequently creates an operating-system-specific process (a running program) with a reference to a Process class returned to the Java VM.
The Process class is an abstract class, because a specific subclass of Process exists for each operating system.

You can pass three possible input parameters into these methods:
    A single string that represents both the program to execute and any arguments to that program
    An array of strings that separate the program from its arguments
    An array of environment variables

Pass in the environment variables in the form name=value. If you use the version of exec() with a single string for both the program and its arguments,
note that the string is parsed using white space as the delimiter via the StringTokenizer class.

The first pitfall relating to Runtime.exec() is the IllegalThreadStateException. The prevalent first test of an API is to code its most obvious methods.
For example, to execute a process that is external to the Java VM, we use the exec() method. To see the value that the external process returns,
we use the exitValue() method on the Process class. In our first example, we will attempt to execute the Java compiler (javac.exe): 

    public static void main(String args[])
    {
        try
        {            
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("javac");
            int exitVal = proc.exitValue();
            System.out.println("Process exitValue: " + exitVal);
        } catch (Throwable t)
          {
            t.printStackTrace();
          }
    }

If an external process has not yet completed, the exitValue() method will throw an IllegalThreadStateException

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("javac");
            int exitVal = proc.waitFor();


Why Runtime.exec() hangs:  Because some native platforms only provide limited buffer size for standard input and output streams,
failure to promptly write the input stream or read the output stream of the subprocess may cause the subprocess to block, and even deadlock.

*/


  public static long file_size_get(String filename) {
    File ppFile = null;
    long ret = -1;
    try {
      ppFile = new File(filename);
      if (ppFile.exists())
        ret = ppFile.length();
    } catch (Exception e) {
      //e.printStackTrace ();
    }
    logd("ret: " + ret + "  \'" + filename + "\'");
    return (ret);
  }

  public static boolean file_get(String filename, boolean log) {
    File ppFile = null;
    boolean exists = false;
    try {
      ppFile = new File(filename);
      if (ppFile.exists())
        exists = true;
    } catch (Exception e) {
      //e.printStackTrace ();
      exists = false;                                                   // Exception means no file
    }
    if (log)
      logd("exists: " + exists + "  \'" + filename + "\'");
    return (exists);
  }

  public static boolean file_get(String filename) {
    return file_get(filename, true);
  }

  public static boolean access_get(String filename, boolean read, boolean write, boolean execute) {
    File ppFile = null;
    boolean exists = false, access = false;
    try {
      ppFile = new File(filename);
      if (ppFile.exists()) {
        exists = true;
        access = true;
        if (read & !ppFile.canRead())
          access = false;
        if (write & !ppFile.canWrite())
          access = false;
        if (execute & !ppFile.canExecute())
          access = false;
      }
    } catch (Exception e) {
      loge("exception: " + e + "  filename: " + filename);
      e.printStackTrace();
      exists = false;                                                   // Exception = no file (always ?)
    }
    logd("filename: \'" + filename + "\'  exists: " + exists + "  access: " + access);
    return (exists & access);
  }

  public static boolean motog_get() {
    return m_device.startsWith("FALCON") || m_device.startsWith("PEREGRINE") || m_device.startsWith("TITAN") || m_device.startsWith("XT102") || m_device.startsWith("XT103") || m_device.startsWith("XT104") || m_device.startsWith("XT106");
  }

  public static boolean htc_one_onemini_get() {
    boolean ret = false;
    if (m_device.startsWith("M7"))
      ret = true;
    else if (m_device.startsWith("M4") && htc_have_get())
      ret = true;
    else if (m_device.startsWith("HTC_M4"))
      ret = true;
    return (ret);
  }

  public static boolean htc_stock_get() {
    if (!htc_stock_set) {
      htc_stock_set = true;

      if (htc_one_onemini_get()) {                                     // Special case for HTC One
        if (file_get("/system/framework/com.htc.fusion.fx.jar") || file_get("/system/framework/com.htc.lockscreen.fusion.jar"))
          htc_stock = true;
      } else if (file_get("/system/framework/com.htc.framework.jar") || file_get("/system/framework/framework-htc-res.apk") || file_get("/system/framework/HTCDev.jar") || file_get("/system/framework/HTCExtension.jar"))
        htc_stock = true;
    }
    return (htc_stock);
  }

  public static boolean htc_have_get() {
    if (!htc_have_set) {
      htc_have_set = true;
      if (file_get("/sys/class/htc_accessory/fm/flag"))
        htc_have = true;
      else if (m_manufacturer.startsWith("HTC"))
        htc_have = true;
      else if (htc_stock_get())
        htc_have = true;
    }
    return (htc_have);
  }

/* OMNIROM:

GT-N7100    T03G
GT-I9300    i9300
GT-N7000    n7000
GT-I9100    i9100

HTCONE      m7ul

GT-I9000    GT-I9000        Mackay. Unofficial ?

Evo 4G LTE  jewel
*/

  private static int device_get() {
    String arch = System.getProperty("os.arch"); // armv7l
    if (arch != null)
      log(arch);
    String name = System.getProperty("os.name"); // Linux
    if (name != null)
      log(name);
    String vers = System.getProperty("os.version"); // 3.0.60-g8a65ee9
    if (vers != null)
      log(vers);

    log(android.os.Build.BOARD);        // aries
    log(android.os.Build.BRAND);        // samsung          SEMC
    log(android.os.Build.DEVICE);       // GT-I9000
    log(android.os.Build.HARDWARE);     // aries            st-ericsson
    log(android.os.Build.ID);           // JOP40D
    log(android.os.Build.MANUFACTURER); // samsung          Sony
    log(android.os.Build.MODEL);        // GT-I9000
    log(android.os.Build.PRODUCT);      // GT-I9000

    //if (android.os.Build.DEVICE.toUpperCase (Locale.getDefault ()).equals "GT-I900")

    if (m_board.equals(""))
      m_board = android.os.Build.BOARD.toUpperCase(Locale.getDefault());

    if (m_device.equals(""))
      m_device = android.os.Build.DEVICE.toUpperCase(Locale.getDefault());

    if (m_manufacturer.equals(""))
      m_manufacturer = android.os.Build.MANUFACTURER.toUpperCase(Locale.getDefault());


    int dev = DEV_UNK;

    if (m_manufacturer.startsWith("SONY") && file_get("/system/lib/libbt-fmrds.so"))     // ? Z2/Z3 need to be more specific ?
      dev = (DEV_XZ2);
    else if (m_device.startsWith("SGP5") || m_device.startsWith("SOT") || m_device.startsWith("SO-05") || m_device.startsWith("D65") || m_device.startsWith("SO-03") || m_device.startsWith("CASTOR") || m_device.startsWith("SIRIUS") ||
            m_device.startsWith("D66") || m_device.startsWith("D58") || m_device.startsWith("LEO"))
      dev = (DEV_XZ2);

//C65, C66, C69
      // NECCASIO G'zOne Commando 4G LTE– C811
    else if (m_device.startsWith("C811") || m_device.startsWith("EVITA") || m_device.startsWith("VILLE") || m_device.startsWith("JEWEL")//) // || m_device.startsWith ("SCORPION")) //_MINI_U"))
            || m_device.startsWith("C2") || m_device.startsWith("C21") || m_device.startsWith("C19") || m_device.startsWith("C6") || m_device.startsWith("SGP3") || m_device.startsWith("LT29") || m_device.startsWith("LT30") ||
            //  || m_device.startsWith ("C65") || m_device.startsWith ("C66") || m_device.startsWith ("C69")    // Japan Xperia Z1  SO-01f & SOL23 (Australia also SOL23)
            m_device.startsWith("YUGA") || m_device.startsWith("ODIN") || m_device.startsWith("TSUBASA") || m_device.startsWith("HAYABUSA") || m_device.startsWith("MINT") || m_device.startsWith("POLLUX") || m_device.startsWith("HONAMI") ||
            m_device.startsWith("GEE"))    // LG Optimus G/G Pro
      dev = (DEV_QCV);

    else if (m_device.startsWith("GT-I9000") || m_device.startsWith("GT-I9010") || m_device.equals("GALAXYS") || m_device.startsWith("GALAXYSM") || m_device.startsWith("SC-02B") || m_device.startsWith("YP"))
      dev = (DEV_GS1);

    else if (m_device.startsWith("GT-I91") || m_device.startsWith("I91") || m_device.startsWith("N70") || m_device.startsWith("GT-N70") || m_device.equals("GALAXYS2") || m_device.startsWith("SC-02C") || m_device.startsWith("GALAXYN"))
      dev = (DEV_GS2);

    else if (m_device.startsWith("M8") || m_device.startsWith("HTC_M8"))                                // HTC One M8 2014
      dev = (DEV_QCV);

    else if (m_device.startsWith("M7C"))                               // HTC One dual sim 802w/802d/802t
      dev = (DEV_QCV);

    else if (htc_one_onemini_get())
      dev = (DEV_ONE);

    else if (m_manufacturer.contains("XIAOMI"))
      dev = DEV_QCV;

    else if (motog_get())
      dev = (DEV_QCV);

    else if (m_board.startsWith("GALBI") || m_device.startsWith("G2") || m_device.startsWith("LS980") || m_device.startsWith("D80") || m_device.startsWith("ZEE"))  // "zee" RayGlobe Flex - ls980
      dev = (DEV_LG2);


    if (dev == DEV_UNK) {
      // From android_fmradio.cpp

      // Qualcomm is always V4L and has this FM /sys directory
      if (com_uti.file_get("/dev/radio0") && com_uti.file_get("/sys/devices/platform/APPS_FM.6")) {
        dev = DEV_QCV;  // Redundant, see 
      }
      log("DEV_UNK fix -> dev: " + dev);
    }

    log("device_get: dev=" + dev);
    return (dev);
  }

  /**
   * @deprecated
   */
  public static String prefs_get(Context context, String key, String def) {
    String res = Utils.getPrefString(context, key);
    return res != null ? res : def;
  }

  /**
   * @deprecated
   */
  public static void prefs_set(Context context, String key, String val) {
    Utils.setPrefString(context, key, val);
  }



  // Hidden Audiosystem stuff:
/*
  public static int setParameters (final String keyValuePairs) {
    int ret = -7;
    com_uti.logd ("keyValuePairs: " + keyValuePairs);
    try {
      Class<?> audioSystem = Class.forName ("android.media.AudioSystem");        // Use reflection
      Method setParameters = audioSystem.getMethod ("setParameters", String.class);
      ret = (Integer) setParameters.invoke (audioSystem, keyValuePairs);
      com_uti.logd ("ret: " + ret); 
    }
    catch (Exception e) {
      com_uti.loge ("Could not set audio system parameters: " + e);
    }
    return (ret);
  }

  public static final int FOR_MEDIA = 1;
  public static final int FORCE_NONE = 0;
  public static final int FORCE_SPEAKER = 1;
  public static int setForceUse (final int usage, final int config) {
    int ret = -9;
    com_uti.logd ("usage: " + usage + "  config: " + config); 
    try {
      //Method setForceUse = AudioManager.class.getMethod ("setForceUse", int.class, int.class);
      Class<?> audioSystem = Class.forName ("android.media.AudioSystem");        // Use reflection
      Method setForceUse = audioSystem.getMethod ("setForceUse", int.class, int.class);
      ret = (Integer) setForceUse.invoke (audioSystem, usage, config);
      com_uti.logd ("ret: " + ret);
    }
    catch (Exception e) {
      com_uti.loge ("exception: " + e);
    }
    return (ret);
  }
*/

  // Conversions:
/*
  private static byte [] sa_to_ba_hard_way (short [] sa, int size) {           // Byte array to short array; size is in shorts
    byte [] ba = new byte [size * 2];
    for (int i = 0; i < size; i ++) {
      byte lb = (byte) sa [i];
      byte hb = (byte) (sa [i] >> 8);
      ba [(i * 2) + 0] = lb;
      ba [(i * 2) + 1] = hb;
    }
    return (ba);
  }

  private static short [] ba_to_sa_hard_way (byte [] ba, int size) {           // Short array to byte array; size is in shorts
    short [] sa = new short [size];
    for (int i = 0; i < size; i ++) {
      //short s = (short) (ba [(i * 2)] + (ba [(i * 2) + 1] << 8));
      short s = (short) (ba [(i * 2)] << 8);
      sa [i] = s;
    }
    return (sa);
  }
*/

  /**
   * String to byte array
   */
  public static byte[] stringToByteArray(String s) {
    //s += "�";     // RDS test ?
    char[] buffer = s.toCharArray();
    byte[] content = new byte[buffer.length];
    for (int i = 0; i < content.length; i++) {
      content[i] = (byte) buffer[i];
      //if (content [i] == -3) {            // ??
      //  loge ("s: " + s);//content [i]);
      //  content [i] = '~';
      //}
    }
    return content;
  }

  /**
   * Byte array to string
   */
  public static String byteArrayToString(byte[] b) {
    String s = "";
    try {
      s = new String(b, StandardCharsets.UTF_8);
    } catch (Exception e) {
      //e.printStackTrace ();
    }
    return s;
  }

  public static String ba_to_hexstr(byte[] ba) {
    StringBuilder hex = new StringBuilder();

    for (byte aBa : ba) {
      hex.append(hex_get(aBa));
    }

    return hex.toString();
  }

  public static String hex_get(byte b) {
    //String hex_get (int b) {
    //String str = "";
    //b &= 0x0f;
    //char c1 = (char)((b&0x00F0)>>4);
    //char c2 = (char)((b&0x000F));
    byte c1 = (byte) ((b & 0x00F0) >> 4);
    byte c2 = (byte) ((b & 0x000F));

    //char[] buffer = new char[2];
    byte[] buffer = new byte[2];

    if (c1 < 10)
      buffer[0] = (byte) (c1 + '0');
    else
      buffer[0] = (byte) (c1 + 'A' - 10);
    if (c2 < 10)
      buffer[1] = (byte) (c2 + '0');
    else
      buffer[1] = (byte) (c2 + 'A' - 10);

    //if (hex_times ++ < 4) {
    //  com_uti.logd ("zx hex_get byte: " + b + "  c1: " + c1 +  "  c2: " + c2 +  "  buffer0: " + buffer[0] +  "  buffer1: " + buffer[1] );
    //}

    return new String(buffer);
  }

  public static String hex_get(short s) {
    byte byte_lo = (byte) (s & 0xFF);
    byte byte_hi = (byte) (s >> 8 & 0xFF);
    return (hex_get(byte_hi) + hex_get(byte_lo));
  }


  @SuppressLint("SdCardPath")
  public static File getExternalStorageDirectory() {
    //String path = Environment.getExternalStorageDirectory().getPath();
    //String path = Environment.getExternalStorageDirectory().toString();
    //ret = Environment.getExternalStorageDirectory(); // Stopped working for some 4.2+ due to multi-user and 0
    return new File("/sdcard/");
  }


  /**
   * Datagram command API
   */

  private static final int RADIO_NOP_TIMEOUT = 100;

  private static DatagramSocket ds = null;

  /**
   * Отправка команды нативной программе через сокеты
   * @param cmd_len длина команды
   * @param cmd_buf команда
   * @param res_len длина ответа
   * @param res_buf ответ
   * @param rx_tmo таймаут в мс
   * @return код возврата
   */
  private static int dg_s2d_cmd(int cmd_len, byte[] cmd_buf, int res_len, byte[] res_buf, int rx_tmo) {
    int len = 0;

    String cmd = com_uti.byteArrayToString(cmd_buf).substring(0, cmd_len);

    dg_s2d_log = com_uti.file_get("/mnt/sdcard/sf/s2d_log", false);

    if (dg_s2d_log) {
      //log("Before sem++ cmd: " + cmd + "  res_len: " + res_len + "  rx_tmo: " + rx_tmo);
    }

    dg_s2d_cmd_sem++;
    while (dg_s2d_cmd_sem != 1) {
      dg_s2d_cmd_sem--;
      com_uti.ms_sleep(1);
      dg_s2d_cmd_sem++;
    }

    try {
      DatagramPacket dps;

      if (ds == null || rx_tmo == 100 || rx_tmo == 15000) {
        ds = new DatagramSocket(0);//2122);
      }

      ds.setSoTimeout(rx_tmo);

      //if (!loop_set) {
      //boolean loop_set = true;
      //InetAddress loop = InetAddress.getByName("127.0.0.1");
      //}

      // Send
      dps = new DatagramPacket(cmd_buf, cmd_len, InetAddress.getByName("127.0.0.1"), 2122);
      ds.send(dps);
      ds.receive(dps);
      // java.net.PortUnreachableException Caused by: libcore.io.ErrnoException: recvfrom failed: ECONNREFUSED (Connection refused)
      // java.net.SocketTimeoutException
      //log("After  receive() cmd: " + cmd);

      byte[] receivedBuffer = dps.getData();

      len = dps.getLength();

      if (dg_s2d_log) {
        log("After getLength() cmd: " + cmd + "  len: " + len);
        if (rx_tmo == 1002) {
          log("hexstr res: " + com_uti.ba_to_hexstr(receivedBuffer).substring(0, len * 2));
        } else {
          log("   str res: " + com_uti.byteArrayToString(receivedBuffer).substring(0, len));
        }
      }

      // 0 is valid length, eg empty RT
      if (len < 0) {
        log("s2d_cmd: cmd=" + cmd + "; len=" + len);
      } else {
        System.arraycopy(receivedBuffer, 0, res_buf, 0, len);
      }
    } catch (SocketTimeoutException e) {
      if (rx_tmo != RADIO_NOP_TIMEOUT) {
        log("s2d_cmd: java.net.SocketTimeoutException: rx_tmo: " + rx_tmo + "  cmd: " + cmd);
      }
    } catch (Throwable e) {
      log("s2d_cmd [exception]: type=" + e + "; rx_tmo=" + rx_tmo + "; cmd='" + cmd + "'");
      e.printStackTrace();
    }
    dg_s2d_cmd_sem--;
    return len;
  }

  /**
   * Получение значения параметра от нативного кода
   * @param key ключ параметра
   * @return значение от API
   */
  public static String s2d_get(String key) {
    return s2d_cmd("g " + key);
  }

  /**
   * Изменение параметра в нативном коде
   * @param key ключ параметра
   * @param val новое значение
   * @return ответ от нативного API
   */
  public static String s2d_set(String key, String val) {
    String res = s2d_cmd("s " + key + " " + val);
    log("s2d_set: [" + key + "] = '" + val + "' ===> '" + res + "'");
    return res;
  }

  private static long cmdN = 0;

  /**
   * Якобы всегда падает первый запрос. Поэтому при старте тюнера
   * вызывается ни на что не влияющий s radio_nop start, который
   * выбрасывает SocketTimeout. Для него же сделан и таймаут меньше
   */
  private static String s2d_cmd(String command) {
    int commandLength = command.length();
    if (s2d_cmd_log) {
      log("s2d cmd: len=" + commandLength + "; cmd=\"" + command + "\"");
    }

    long mss = System.currentTimeMillis();

    byte[] commandBuffer = com_uti.stringToByteArray(command);
    commandLength = commandBuffer.length;

    log(String.format(Locale.ENGLISH, "cmd%d: --> \"%s\"; length = %d", cmdN, command, commandLength));

    int resultLength = DEF_BUF;
    byte[] resultBuffer = new byte[resultLength];

    int rx_timeout = 3000;

    if (command.equalsIgnoreCase("s tuner_state start")) {
      rx_timeout = 15000;
    } else if (command.equals("s radio_nop start")) {
      rx_timeout = RADIO_NOP_TIMEOUT; // Always fails so make it short
    }

    resultLength = dg_s2d_cmd(commandLength, commandBuffer, resultLength, resultBuffer, rx_timeout);

    // Avoid showing 999 for RT when result is zero length string "" (actually 1 byte long for zero) "999";
    String result = "";

    if (resultLength > 0 && resultLength <= DEF_BUF) {
      result = com_uti.byteArrayToString(resultBuffer);
      result = result.substring(0, resultLength);     // Remove extra data

      L.w(L.T.UTILS, String.format(Locale.ENGLISH, "cmd%d: <-- \"%s\"; len = %d; dur = %dms", cmdN++, result, resultLength, System.currentTimeMillis() - mss));
      if (s2d_cmd_log) {
        log("res: \"" + result + "\"");
      }
    } else if (resultLength == 0) {
      result = ""; // Empty string
    } else {
      log("res_len=" + resultLength + "; cmd='" + command + "'");
    }
    return result;
  }


/*
  public static int s2d_audio_data_get (int device, int samplerate, int channels, int buf_len, byte[] buffer) {
    int len = 0;
    //if (s2d_audio_data_get_num % 10 == 0)
    //  s2d_audio_data_get_log = true;
    //else
    //  s2d_audio_data_get_log = false;
    //s2d_audio_data_get_num ++;
    if (s2d_cmd_log)
      com_uti.logd ("device: " + device + "  samplerate: " + samplerate + "  channels: " + channels + "  buf_len: " + buf_len);

    int rx_tmo = 1002;
    String cmd = "g audio_data                                                    ";    // 64 bytes
    int x = 0;
    for (x = 0; x < ((1280/64) -1); x++)
      cmd += "                                                                ";        // 64 bytes
    int cmd_len = cmd.length ();
    byte [] cmd_buf = com_uti.stringToByteArray (cmd);
    cmd_len = cmd_buf.length;

    len = dg_s2d_cmd (cmd_len, cmd_buf, buf_len, buffer, rx_tmo);

    if (s2d_cmd_log) {
      if (len > 0)
        com_uti.logd ("len: " + len + "  buffer[16]: " + buffer [16]);
      else
        com_uti.loge ("len: " + len);
    }

    return (len);
  }
*/

/*
    try {                                                       // !!!! Thread may already be started
      if (pcm_write_thread.getState () == java.lang.Thread.State.NEW || pcm_write_thread.getState () == java.lang.Thread.State.TERMINATED) {
        com_uti.logd ("thread priority: " + pcm_write_thread.getPriority ());   // Get 5
        //pcm_write_thread.setPriority (Thread.MAX_PRIORITY - 1);   // ?? android.os.Process.THREAD_PRIORITY_AUDIO = -16        THREAD_PRIORITY_URGENT_AUDIO = -19
        //com_uti.logd ("thread priority: " + pcm_write_thread.getPriority ());   // Get 9
        pcm_write_thread.start ();
      }
    }
    catch (Throwable e) {
      com_uti.loge ("Throwable: " + e);
      e.printStackTrace ();
      return (-1);
    }
*/
  // The maximum priority value allowed for a thread. This corresponds to (but does not have the same value as) android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY.
  // Constant Value: 10
/*https://gitorious.org/freebroid/dalvik/source/962f896e1eeb159a6a2ac7a560708939cbb15575:vm/Thread.c
static const int kNiceValues[10] = {
ANDROID_PRIORITY_LOWEST,                // 1 (MIN_PRIORITY)
ANDROID_PRIORITY_BACKGROUND + 6,
ANDROID_PRIORITY_BACKGROUND + 3,
ANDROID_PRIORITY_BACKGROUND,
ANDROID_PRIORITY_NORMAL,                // 5 (NORM_PRIORITY)
ANDROID_PRIORITY_NORMAL - 2,
ANDROID_PRIORITY_NORMAL - 4,
ANDROID_PRIORITY_URGENT_DISPLAY + 3,
ANDROID_PRIORITY_URGENT_DISPLAY + 2,
ANDROID_PRIORITY_URGENT_DISPLAY         // 10 (MAX_PRIORITY)
};*/

/*> setThreadPriority
http://www.netmite.com/android/mydroid/frameworks/base/include/utils/threads.h
    ANDROID_PRIORITY_LOWEST         =  19,
    ANDROID_PRIORITY_BACKGROUND     =  10,      /* use for background tasks 
    ANDROID_PRIORITY_NORMAL         =   0,      /* most threads run at normal priority
    ANDROID_PRIORITY_FOREGROUND     =  -2,      /* threads currently running a UI that the user is interacting with
    ANDROID_PRIORITY_DISPLAY        =  -4,      /* the main UI thread has a slightly more favorable priority
    ANDROID_PRIORITY_URGENT_DISPLAY =  -8,      /* ui service treads might want to run at a urgent display (uncommon)
    ANDROID_PRIORITY_AUDIO          = -16,      /* all normal audio threads
    ANDROID_PRIORITY_URGENT_AUDIO   = -19,      /* service audio threads (uncommon)
    ANDROID_PRIORITY_HIGHEST        = -20,      /* should never be used in practice. regular process might not be allowed to use this level

    ANDROID_PRIORITY_DEFAULT        = ANDROID_PRIORITY_NORMAL,
    ANDROID_PRIORITY_MORE_FAVORABLE = -1,
    ANDROID_PRIORITY_LESS_FAVORABLE = +1,
*/


  // Tuner FM utility functions:
  public static void setTunerBand(String band) {
    com_uti.band_freq_lo = 87500;
    com_uti.band_freq_hi = 108000;
    com_uti.band_freq_inc = 100;
    com_uti.band_freq_odd = 0;

    if (band.equalsIgnoreCase("EU_50K_OFFSET")) {
      com_uti.band_freq_inc = 50;
    }

    log("low=" + com_uti.band_freq_lo + "; high=" + com_uti.band_freq_hi + "; inc=" + com_uti.band_freq_inc + "; odd=" + band_freq_odd);
  }


  private static int band_freq_updn_get(int int_tuner_freq, boolean up) {
    int new_freq;
    if (up)
      new_freq = com_uti.tnru_freq_enforce(int_tuner_freq + com_uti.band_freq_inc);
    else
      new_freq = com_uti.tnru_freq_enforce(int_tuner_freq - com_uti.band_freq_inc);
    return (new_freq);
  }

  public static String tnru_mhz_get(int freq) {
    freq += 25;                                                         // Round up...
    freq = freq / 50;                                                   // Nearest 50 KHz
    freq *= 50;                                                         // Back to KHz scale
    double dfreq = (double) freq;
    dfreq = java.lang.Math.rint(dfreq);
    dfreq /= 1000;
    return String.valueOf(dfreq);
  }

  /**
   * Used
   */
  public static int tnru_khz_get(String freq) {
    double dfreq = com_uti.double_get(freq);
    if (dfreq >= 50000 && dfreq <= 499999)    // If 50,000 - 499,999  KHz...
      dfreq *= 1;                             // -> Khz
    else if (dfreq >= 5000 && dfreq <= 49999) // If 5,000 - 49,999    x 0.01 MHz...
      dfreq *= 10;                            // -> Khz
    else if (dfreq >= 500 && dfreq <= 4999)   // If 500 - 4,999       x 0.10 MHz...
      dfreq *= 100;                           // -> Khz
    else if (dfreq >= 50 && dfreq <= 499)     // If 50 - 499          MHz...
      dfreq *= 1000;                          // -> Khz
    return (int) dfreq;
  }

  public static int tnru_band_new_freq_get(String freq, int int_tuner_freq) {
    int ifreq;
    if (freq.equalsIgnoreCase("down"))
      ifreq = com_uti.band_freq_updn_get(int_tuner_freq, false);
    else if (freq.equalsIgnoreCase("up"))
      ifreq = com_uti.band_freq_updn_get(int_tuner_freq, true);
    else
      ifreq = com_uti.tnru_khz_get(freq);
    return ifreq;
  }

  public static int tnru_freq_fix(int freq) {
    // w/ Odd:  107900-108099 -> 107900     = Add 100, Divide by 200, then multiply by 200, then subtract 100
    // w/ Even: 108000-108199 -> 108000     = Divide by 200, then multiply by 200  (freq_inc)
    // w/ Odd:   87500- 87699 ->  87500     = Add 100, Divide by 200, then multiply by 200, then subtract 100
    // w/ Even:  87600- 87799 ->  87600     = Divide by 200, then multiply by 200  (freq_inc)

    if (com_uti.band_freq_odd != 0) {
      freq += com_uti.band_freq_inc / 2;    // 87700
      freq /= com_uti.band_freq_inc;
      freq *= com_uti.band_freq_inc;        // 87600
      freq -= com_uti.band_freq_inc / 2;    // 87500
      //log ("US ODD freq: "  + freq);
    } else {
      freq /= com_uti.band_freq_inc;
      freq *= com_uti.band_freq_inc;
      //log ("EU ALL freq: "  + freq);
    }
    return freq;
  }

  public static int tnru_freq_enforce(int freq) {
    if (freq < com_uti.band_freq_lo)
      freq = com_uti.band_freq_hi;
    if (freq > com_uti.band_freq_hi)
      freq = com_uti.band_freq_lo;
    return com_uti.tnru_freq_fix(freq);
  }

/*
// 97.9 CHIN    = 51755
// 96.5 Capitale= 49948

//AF: number: 3   freq: 92100
//AltFreq group: e3 2e 43 61 


// !!! Exception for 88.5       0x163e = KCJM
// !!! Exception for 89.9       0x15d6 = KCFM

// These first 2 do nothing now; over-ride with US callsigns
    if (pi== 0x163e)
      cs = "88.5 CILV";//"Ott 88.5 CILV";
//89.1 nothing but group 0B with spaces.
    else if (pi== 0x15d6)
      cs = "89.9 HOT";//"Ott 89.9 HOT";

    else if (pi== 0xb404)                                            // Ottawa Canadian numbers from  0xb20c - 0xcE0d  / 0xdc09
      cs="90.7 PREMIERE";//"Ott 90.7 PREMIERE";
//CBC R1 above 91.5
//93.1 nothing.
    else if (pi== 0xccb6)
      cs="93.9 BOB";//"Ott 93.9 BOB";
//94.5 nothing.
    else if (pi== 0xc7cd)
      cs="94.9 RDETENTE";//"Ott 94.9 RDETENTE";
//95.7 unscannable ??? ??? !!!!
//97.1...distant
//99.1 nothing.
    else if (pi== 0xca44)
      cs="100.3 Majic100";//"Ott 100.3 Majic100";
    else if (pi== 0xb205)
      cs="102.5 ESPACE M";//"Ott 102.5 ESPACE M";
//CBC R2 above 103.3 nothing ?
    else if (pi== 0xce0d)
      cs="104.1 NRJ";//"Ott 104.1 NRJ";
    else if (pi== 0xc87d)
      cs="105.3 KISS CISS";//"Ott 105.3 KISS CISS";
    else if (pi== 0xc448)
      cs="106.1 CHEZ";//"Ott 106.1 CHEZ";
    else if (pi== 0xdc09)
      cs="106.9 BEAR CKQB";//"Ott 106.9 BEAR CKQB";
    else
      cs = null;
    return (cs);
  }
*/

// https://www.cgran.org/browser/projects/simple_fm_rcv/trunk/handle_rds.c

  // Canadian station normally use call letters from the CFAA-CFZZ and CHAA-CKZZ blocks. IE. CFxx, CHxx, CIxx, CJxx, CKxx = 5 * 26 * 27 = 3510  (0xC000 - 0xCDB5)
/*
C000 - CFFF assigned to Canada. Allows AF switching, but no regionalization. PI codes C0xx, and Cx00 are excluded from use.
Same for Mexico except uses 0xFxxx

B_01 - B_FF, D_01 - D_FF, E_01 - E_FF
assigned for national networks in US, Canada, and Mexico. Regionalization allowed. NRSC to provide assignments for all three countries.
It should be noted that operation in this region is the same as it is for all RDS PI codes.
*/

}