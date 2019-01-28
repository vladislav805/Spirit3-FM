// Radio Service API:
package fm.a2d.sf;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import fm.a2d.sf.helper.L;
import fm.a2d.sf.helper.Utils;

import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public class com_api {

  private Context mContext;
  private static int curr_pending_intent_num = 0;

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

  public int tuner_rssi = 0;//999";        // ro ... ... Values:   RSSI: 0 - 1000
  public String tuner_qual = "";//SN 99";      // ro ... ... Values:   SN 99, SN 30
  public String tuner_most = "";//Mono";       // ro ... ... Values:   mono, stereo, 1, 2, blend, ... ?      1.5 ?

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

  public void loadPresets() {
    if (mPresetNames != null) {
      mPresetNames.clear();
    } else {
      mPresetNames = new SparseArray<>();
    }
    for (int i = 0; i < C.PRESET_COUNT; ++i) {
      int freq = Utils.getPrefInt(mContext, C.PRESET_KEY + i);
      if (freq > 0) {
        String name = Utils.getPrefString(mContext, C.PRESET_KEY_NAME + i);
        log(String.format(Locale.ENGLISH, "%2d. %s", freq, name));
        mPresetNames.put(freq, name);
      }
    }
    log("Presets loaded");
  }

  private static final String DEFAULT_DETECT = "default_detect";

  public void updateInfo(Intent intent) {
    Bundle extras = intent.getExtras();

    if (extras == null) {
      return;
    }

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







    int new_tuner_freq = extras.getInt(C.TUNER_FREQUENCY, 0);
    if (new_tuner_freq != 0) {
      int_tuner_freq = new_tuner_freq;
    }

    String new_tuner_stereo = extras.getString("tuner_stereo", null);
    if (new_tuner_stereo != null) {
      tuner_stereo = new_tuner_stereo;
    }

    String new_tuner_thresh = extras.getString("tuner_thresh", null);
    if (new_tuner_thresh != null) {
      tuner_thresh = new_tuner_thresh;
    }

    String new_tuner_scan_state = extras.getString(C.TUNER_SCAN_STATE, null);
    if (new_tuner_scan_state != null) {
      tuner_scan_state = new_tuner_scan_state;
    }


    int rssi = extras.getInt(C.TUNER_RSSI, -1);
    if (rssi != -1) {
      tuner_rssi = rssi;
    }

    String new_tuner_qual = extras.getString("tuner_qual", null);
    if (new_tuner_qual != null) {
      tuner_qual = new_tuner_qual;
    }

    String new_tuner_most = extras.getString("tuner_most", null);
    if (new_tuner_most != null) {
      tuner_most = new_tuner_most;
    }
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

