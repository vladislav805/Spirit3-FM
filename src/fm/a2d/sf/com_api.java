// Radio Service API:
package fm.a2d.sf;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import fm.a2d.sf.helper.L;
import fm.a2d.sf.helper.Utils;

public class com_api {

  private Context mContext;
  private static int curr_pending_intent_num = 0;

  // Radio statuses:
  public String radio_phase = "Pre Init";
  public String radio_cdown = "999";
  public String radio_error = "";

  public static final int PRESET_COUNT = 30;

  // Audio:
  public String audio_state = C.AUDIO_STATE_STOP;
  public String audio_output = C.AUDIO_OUTPUT_HEADSET;
  public String audio_stereo = "stereo";
  public String audio_record_state = C.RECORD_STATE_STOP;
  public String audio_sessid = "0";

  // Tuner:
  //    CFG = Saved in config
  //    ... = ephemeral non-volatile
  //        api = get from FM API
  //        set = get from set of this variable
  //        mul = multiple/both
  //        ... = RO
  // RW CFG api for tuner_freq & tuner_thresh consistency issues: CFG vs chip current values
  public String tuner_state = C.TUNER_STATE_STOP;          // RW ... api States:   stop, start, pause, resume

  public final String tuner_band = "EU";       // RW CFG set Values:   US, EU, JAPAN, CHINA, EU_50K_OFFSET     (Set before Tuner Start)


  //public String tuner_freq = "";//-1";       // RW CFG api Values:   String  form: 50.000 - 499.999  (76-108 MHz)

  public int int_tuner_freq = 0;//-1;          // ""                 Integer form in kilohertz
  public String tuner_stereo = "stereo";       // RW CFG set Values:   mono, stereo, switch, blend, ... ?
  public String tuner_thresh = "";             // RW CFG api Values:   Seek/scan RSSI threshold
  public String tuner_scan_state = "stop";     // RW ... set States:   down, up, scan, stop

  public String tuner_rds_state = "stop";      // RW CFG set States:   on, off
  public String tuner_rds_af_state = "stop";   // RW CFG set States:   on, off
  public String tuner_rds_ta_state = "stop";   // RW CFG set States:   on, off

  public String tuner_extra_cmd = "";          // RW ... set Values:   Extra command
  public String tuner_extra_resp = "";         // ro ... ... Values:   Extra command response

  public int tuner_rssi = 0;//999";        // ro ... ... Values:   RSSI: 0 - 1000
  public String tuner_qual = "";//SN 99";      // ro ... ... Values:   SN 99, SN 30
  public String tuner_most = "";//Mono";       // ro ... ... Values:   mono, stereo, 1, 2, blend, ... ?      1.5 ?

  public String tuner_rds_pi = "";//-1";       // ro ... ... Values:   0 - 65535
  public String tuner_rds_picl = "";//WKBW";   // ro ... ... Values:   North American Call Letters or Hex PI for tuner_rds_pi
  public String tuner_rds_pt = "";//-1";       // ro ... ... Values:   0 - 31
  public String tuner_rds_ptyn = "";           // ro ... ... Values:   Describes tuner_rds_pt (English !)
  public String tuner_rds_ps = "Spirit3";      // ro ... ... Values:   RBDS 8 char info or RDS Station
  public String tuner_rds_rt = "";             // ro ... ... Values:   64 char
  //OBNOXIOUS !!     "Analog 2 Digital radio ; Thanks for Your Support... :)";

  public String tuner_rds_af = "";             // ro ... ... Values:   Space separated array of AF frequencies
  public String tuner_rds_ms = "";             // ro ... ... Values:   0 - 65535   M/S Music/Speech switch code
  public String tuner_rds_ct = "";             // ro ... ... Values:   14 char CT Clock Time & Date

  public String tuner_rds_tmc = "";            // ro ... ... Values:   Space separated array of shorts
  public String tuner_rds_tp = "";             // ro ... ... Values:   0 - 65535   TP Traffic Program Identification code
  public String tuner_rds_ta = "";             // ro ... ... Values:   0 - 65535   TA Traffic Announcement code
  public String tuner_rds_taf = "";            // ro ... ... Values:   0 - 2^32-1  TAF TA Frequency

  private static void log(String s) {
    L.w(L.T.API, s);
  }

  public com_api(Context context) { // Context constructor
    mContext = context;
    log("context: " + context);
  }

  private SparseArray<String> mPresetNames;

  public String getPresetNameByFrequency(int khz) {
    if (mPresetNames == null) {
      loadPresets();
    }

    return mPresetNames.get(khz);
  }

  private void loadPresets() {
    mPresetNames = new SparseArray<>();
    for (int i = 0; i < C.PRESET_COUNT; ++i) {
      int freq = Utils.getPrefInt(mContext, C.PRESET_KEY + i);
      String name = Utils.getPrefString(mContext, C.PRESET_KEY_NAME + i);
      if (freq > 0) {
        mPresetNames.put(freq, name);
      }
    }
  }


  public void key_set(String key, String val) {
    log("Set [" + key + "] = '" + val + "'");
    Intent intent = new Intent(MainService.ACTION_SET);
    intent.setClass(mContext, MainService.class);
    intent.putExtra(key, val);
    mContext.startService(intent);
  }

  private static final String DEFAULT_DETECT = "default_detect";

  public void radio_update(Intent intent) {
    Bundle extras = intent.getExtras();

    if (extras == null) {
      return;
    }

    String new_radio_phase = extras.getString("radio_phase", DEFAULT_DETECT);
    if (!new_radio_phase.equalsIgnoreCase(DEFAULT_DETECT))
      radio_phase = new_radio_phase;
    String new_radio_cdown = extras.getString("radio_cdown", DEFAULT_DETECT);
    if (!new_radio_cdown.equalsIgnoreCase(DEFAULT_DETECT))
      radio_cdown = new_radio_cdown;
    String new_radio_error = extras.getString("radio_error", DEFAULT_DETECT);
    if (!new_radio_error.equalsIgnoreCase(DEFAULT_DETECT))
      radio_error = new_radio_error;

    String new_audio_state = extras.getString(C.AUDIO_STATE, DEFAULT_DETECT);//stop");
    String new_audio_output = extras.getString(C.AUDIO_OUTPUT, DEFAULT_DETECT);//headset");
    String new_audio_stereo = extras.getString("audio_stereo", DEFAULT_DETECT);//Stereo");
    String new_audio_record_state = extras.getString(C.RECORD_STATE, DEFAULT_DETECT);//stop");
    String new_audio_sessid = extras.getString("audio_sessid", DEFAULT_DETECT);
    if (!new_audio_state.equalsIgnoreCase(DEFAULT_DETECT))
      audio_state = new_audio_state;
    if (!new_audio_output.equalsIgnoreCase(DEFAULT_DETECT))
      audio_output = new_audio_output;
    if (!new_audio_stereo.equalsIgnoreCase(DEFAULT_DETECT))
      audio_stereo = new_audio_stereo;
    if (!new_audio_record_state.equalsIgnoreCase(DEFAULT_DETECT))
      audio_record_state = new_audio_record_state;
    if (!new_audio_sessid.equalsIgnoreCase(DEFAULT_DETECT))
      audio_sessid = new_audio_sessid;

    String new_tuner_state = extras.getString(C.TUNER_STATE, DEFAULT_DETECT);
    if (!new_tuner_state.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_state = new_tuner_state;


    String new_tuner_stereo = extras.getString("tuner_stereo", DEFAULT_DETECT);
    String new_tuner_thresh = extras.getString("tuner_thresh", DEFAULT_DETECT);
    String new_tuner_scan_state = extras.getString(C.TUNER_SCAN_STATE, DEFAULT_DETECT);


    int new_tuner_freq = extras.getInt(C.TUNER_FREQUENCY, 0);
    if (new_tuner_freq != 0)
      int_tuner_freq = new_tuner_freq;


    if (!new_tuner_stereo.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_stereo = new_tuner_stereo;
    if (!new_tuner_thresh.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_thresh = new_tuner_thresh;
    if (!new_tuner_scan_state.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_scan_state = new_tuner_scan_state;

    String new_tuner_rds_state = extras.getString("tuner_rds_state", DEFAULT_DETECT);
    String new_tuner_rds_af_state = extras.getString("tuner_rds_af_state", DEFAULT_DETECT);
    String new_tuner_rds_ta_state = extras.getString("tuner_rds_ta_state", DEFAULT_DETECT);
    if (!new_tuner_rds_state.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rds_state = new_tuner_rds_state;
    if (!new_tuner_rds_af_state.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rds_af_state = new_tuner_rds_af_state;
    if (!new_tuner_rds_ta_state.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rds_ta_state = new_tuner_rds_ta_state;

    String new_tuner_extra_cmd = extras.getString("tuner_extra_cmd", DEFAULT_DETECT);
    String new_tuner_extra_resp = extras.getString("tuner_extra_resp", DEFAULT_DETECT);
    if (!new_tuner_extra_cmd.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_extra_cmd = new_tuner_extra_cmd;
    if (!new_tuner_extra_resp.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_extra_resp = new_tuner_extra_resp;

    String new_tuner_rssi = extras.getString("tuner_rssi", DEFAULT_DETECT);
    String new_tuner_qual = extras.getString("tuner_qual", DEFAULT_DETECT);
    String new_tuner_most = extras.getString("tuner_most", DEFAULT_DETECT);
    if (!new_tuner_rssi.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rssi = Utils.parseInt(new_tuner_rssi);
    if (!new_tuner_qual.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_qual = new_tuner_qual;
    if (!new_tuner_most.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_most = new_tuner_most;
/*
    String new_tuner_rds_pt = extras.getString("tuner_rds_pt", DEFAULT_DETECT);
    String new_tuner_rds_ptyn = extras.getString("tuner_rds_ptyn", DEFAULT_DETECT);
    String new_tuner_rds_ps = extras.getString("tuner_rds_ps", DEFAULT_DETECT);
    String new_tuner_rds_rt = extras.getString("tuner_rds_rt", DEFAULT_DETECT);
    if (!new_tuner_rds_pt.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rds_pt = new_tuner_rds_pt;
    if (!new_tuner_rds_ptyn.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rds_ptyn = new_tuner_rds_ptyn;
    if (!new_tuner_rds_ps.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rds_ps = new_tuner_rds_ps;
    if (!new_tuner_rds_rt.equalsIgnoreCase(DEFAULT_DETECT))
      tuner_rds_rt = new_tuner_rds_rt;*/

  }

  public static PendingIntent createPendingIntent(Context context, String key, String val) {
    Intent intent = new Intent(MainService.ACTION_SET).setClass(context, MainService.class).putExtra(key, val);
    return PendingIntent.getService(context, ++curr_pending_intent_num, intent, PendingIntent.FLAG_UPDATE_CURRENT);
  }

  public int getIntFrequencyKHz() {
    return int_tuner_freq;
  }

  public boolean isStereo() {
    return !tuner_most.equalsIgnoreCase("mono");
  }

  public int getRssi() {
    return tuner_rssi;
  }

  public static final int TUNER_UNKNOWN = 0;
  public static final int TUNER_START = 1;
  public static final int TUNER_STOP = 2;
  public static final int TUNER_PAUSE = 3;
  public static final int TUNER_RESUME = 4;

  public int getTunerState() {
    switch (tuner_state.toLowerCase()) {
      case "start": return TUNER_START;
      case "stop": return TUNER_STOP;
      case "pause": return TUNER_PAUSE;
      case "resume": return TUNER_RESUME;
      default: return TUNER_UNKNOWN;
    }
  }

  public boolean isTunerStarted() {
    return getTunerState() == TUNER_START;
  }



}

