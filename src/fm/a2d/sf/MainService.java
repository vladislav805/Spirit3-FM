package fm.a2d.sf;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

// MainService class implements Tuner API callbacks & MainService Audio API callback
@SuppressWarnings("WeakerAccess")
public class MainService extends Service implements ServiceTunerCallback, ServiceAudioCallback {

  // Also see AndroidManifest.xml.

  // Action: Commands sent to this service
  //

  // Result: Broadcast multiple status/state data items to all registered listeners; apps, widgets, etc.
  //"fm.a2d.sf.result.get"

  // No constructor

  public static final String ACTION_SET = "fm.a2d.sf.action.set";
  public static final String ACTION_GET = "fm.a2d.sf.result.get";

  // Instance data:
  private Context mContext = this;
  private com_api mApi = null;
  private ServiceTuner mTunerAPI = null;
  private svc_aud mAudioAPI = null;


  // Create a new Notification object
  private Notification mynot       = null;

  // No relevance to NOTIFICATION_ID except to uniquely identify notification !! Spirit1 uses same !!
  private static final int NOTIFICATION_ID = 2112;

  private boolean need_startfg= true;

  private String[] plst_freq   = new String[com_api.PRESET_COUNT];
  private int                   preset_curr = 0;
  private int                   preset_num  = 0;

  private Timer                 tuner_state_start_tmr = null;

  private NotificationManager mNotificationManager = null;

  @Override
  public void onCreate() { // When service newly created...
    try {
      //com_uti.strict_mode_set(true); // Enable strict mode; disabled for now
      com_uti.strict_mode_set(false);  // !!!! Disable strict mode so we can send network packets from Java

      mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

      notif_state_set(true);

      if (mApi == null) {
        mApi = new com_api(this); // Instantiate Common API class
        com_uti.logd("mApi: " + mApi);
      }

      mAudioAPI = new svc_aud(this, this, mApi); // Instantiate audio class
      mTunerAPI = new ServiceTuner(this, this, mApi);  // Instantiate tuner class
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onDestroy() {
    notif_state_set(false);
    super.onDestroy();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null; // Binding not allowed ; no direct call API, must use Intents
  }

  // Handle command intents sent via startService()
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    com_uti.logd ("intent: " + intent + "  flags: " + flags + "  startId: " + startId);

    try {
      if (intent == null) {
        com_uti.loge("intent == null");
        return START_STICKY;
      }

      String action = intent.getAction();
      if (action == null) {
        com_uti.loge("action == null");
        return START_STICKY;
      }

      if (!action.equalsIgnoreCase(ACTION_SET)) {
        com_uti.loge("action: " + action);
        return START_STICKY;
      }

      Bundle extras = intent.getExtras();
      com_uti.logd("extras: " + extras);
      if (extras == null) {
        return START_STICKY;
      }

      String val;

      // Tuner:
      val = extras.getString(C.TUNER_STATE, "");
      if (!val.isEmpty()) {
        tuner_state_set(val);
      }

// tuner_scan_state
      val = extras.getString(C.TUNER_SCAN_STATE, "");
      if (!val.isEmpty()) {
        mTunerAPI.setTunerValue(C.TUNER_SCAN_STATE, val);
      }

      val = extras.getString(C.TUNER_FREQUENCY, "");
      if (!val.isEmpty()) {
        tuner_freq_set(val);
      }

      val = extras.getString("tuner_stereo", "");
      if (!val.isEmpty()) {
        mTunerAPI.setTunerValue("tuner_stereo", val);
        com_uti.prefs_set(mContext, "tuner_stereo", val);
      }

      val = extras.getString("tuner_extra_cmd", "");
      if (!val.isEmpty()) {
        mTunerAPI.setTunerValue("tuner_extra_cmd", val);
      }

      val = extras.getString("tuner_rds_af_state", "");
      if (!val.isEmpty()) {
        tuner_rds_af_state_set(val);
      }


      // Audio:
      val = extras.getString(C.AUDIO_STATE, "");
      if (!val.isEmpty()) {
        audio_state_set(val);
      }

      val = extras.getString(C.AUDIO_OUTPUT, "");
      if (!val.isEmpty()) {
        mAudioAPI.audio_output_set(val);
      }

      val = extras.getString(C.RECORD_STATE, "");
      if (!val.isEmpty()) {
        mAudioAPI.audio_record_state_set(val);
        if (val.equals(C.RECORD_STATE_STOP)) {
          Toast.makeText(this, getString(R.string.toast_record_stop), Toast.LENGTH_SHORT).show();
        }
      }

      val = extras.getString("audio_stereo", "");
      if (!val.isEmpty()) {
        mAudioAPI.audio_stereo_set(val);
        com_uti.prefs_set(mContext, "audio_stereo", val);
      }

      sendRadioStatus(); // Return results
    } catch (Throwable e) {
      e.printStackTrace();
    }

    return START_STICKY;
  }

  // For state and status updates:
  private void displays_update() { // Update all "displays"
    // Update widgets, apps, etc. and get resulting Intent
    mApi.radio_update(sendRadioStatus()); // Get current data in Radio API using Intent (To update all dynamic/external data)
    notif_radio_update(); // Update notification shade
  }


  private void putTunerExtra(Intent intent) {
    intent.putExtra(C.TUNER_STATE, mApi.tuner_state);//mTunerAPI.getTunerValue(C.TUNER_STATE));
    intent.putExtra(C.TUNER_BAND, mApi.tuner_band);
    String freq_khz = mTunerAPI.getTunerValue(C.TUNER_FREQUENCY);
    int ifreq = com_uti.int_get (freq_khz);
    if (ifreq >= 50000 && ifreq < 500000) {
      mApi.tuner_freq = String.valueOf((double) ifreq / 1000);
      mApi.int_tuner_freq = ifreq;
    }
    com_uti.logx ("mApi.tuner_freq: " + mApi.getStringFrequencyMHz() + "  mApi.int_tuner_freq: " + mApi.getIntFrequencyKHz());
    intent.putExtra (C.TUNER_FREQUENCY, mApi.getStringFrequencyMHz());

    //intent.putExtra ("tuner_stereo",       mTunerAPI.getTunerValue ("tuner_stereo"));
    //intent.putExtra ("tuner_thresh",       mTunerAPI.getTunerValue ("tuner_thresh"));
    //intent.putExtra ("tuner_scan_state",   mTunerAPI.getTunerValue ("tuner_scan_state"));

    //intent.putExtra ("tuner_rds_state",    mTunerAPI.getTunerValue ("tuner_rds_state"));
    //intent.putExtra ("tuner_rds_af_state", mTunerAPI.getTunerValue ("tuner_rds_af_state"));
    //intent.putExtra ("tuner_rds_ta_state", mTunerAPI.getTunerValue ("tuner_rds_ta_state"));

    //intent.putExtra ("tuner_extra_cmd",    mTunerAPI.getTunerValue ("tuner_extra_cmd"));
    //intent.putExtra ("tuner_extra_resp",   mTunerAPI.getTunerValue ("tuner_extra_resp"));

    intent.putExtra ("tuner_rssi",         String.valueOf(mApi.getRssi()));      //mTunerAPI.getTunerValue ("tuner_rssi"));
    //intent.putExtra ("tuner_qual",         mTunerAPI.getTunerValue ("tuner_qual"));
    intent.putExtra ("tuner_most",         mApi.isStereo() ? "stereo" : "mono");      //mTunerAPI.getTunerValue ("tuner_most"));

    intent.putExtra ("tuner_rds_pi",       mApi.tuner_rds_pi);    //mTunerAPI.getTunerValue ("tuner_rds_pi"));
    intent.putExtra ("tuner_rds_picl",     mApi.tuner_rds_picl);  //mTunerAPI.getTunerValue ("tuner_rds_picl"));
    //intent.putExtra ("tuner_rds_pt",       mApi.tuner_rds_pt);  //mTunerAPI.getTunerValue ("tuner_rds_pt"));
    intent.putExtra ("tuner_rds_ptyn",     mApi.tuner_rds_ptyn);  //mTunerAPI.getTunerValue ("tuner_rds_ptyn"));
    intent.putExtra ("tuner_rds_ps",       mApi.tuner_rds_ps);    //mTunerAPI.getTunerValue ("tuner_rds_ps"));
    intent.putExtra ("tuner_rds_rt",       mApi.tuner_rds_rt);    //mTunerAPI.getTunerValue ("tuner_rds_rt"));

    //intent.putExtra ("tuner_rds_af",       mTunerAPI.getTunerValue ("tuner_rds_af"));
    //intent.putExtra ("tuner_rds_ms",       mTunerAPI.getTunerValue ("tuner_rds_ms"));
    //intent.putExtra ("tuner_rds_ct",       mTunerAPI.getTunerValue ("tuner_rds_ct"));
    //intent.putExtra ("tuner_rds_tmc",      mTunerAPI.getTunerValue ("tuner_rds_tmc"));
    //intent.putExtra ("tuner_rds_tp",       mTunerAPI.getTunerValue ("tuner_rds_tp"));
    //intent.putExtra ("tuner_rds_ta",       mTunerAPI.getTunerValue ("tuner_rds_ta"));
    //intent.putExtra ("tuner_rds_taf",      mTunerAPI.getTunerValue ("tuner_rds_taf"));
  }

  private Intent sendRadioStatus() { // Send all radio state & status info

    mAudioAPI.audio_sessid_get(); // Better to update here ?

    com_uti.logx("audio_state: " + mApi.audio_state + "; audio_output: " + mApi.audio_output + "; audio_stereo: " + mApi.audio_stereo + "; audio_record_state: " + mApi.audio_record_state);

    Intent intent = new Intent(ACTION_GET);

    intent.putExtra("radio_phase", mApi.radio_phase);
    intent.putExtra("radio_cdown", mApi.radio_cdown);
    intent.putExtra("radio_error", mApi.radio_error);

    intent.putExtra(C.AUDIO_STATE, mApi.audio_state);
    intent.putExtra(C.AUDIO_OUTPUT, mApi.audio_output);
    intent.putExtra("audio_stereo", mApi.audio_stereo);
    intent.putExtra(C.RECORD_STATE, mApi.audio_record_state);
    intent.putExtra("audio_sessid", mApi.audio_sessid);

    if (mTunerAPI == null) {
      intent.putExtra(C.TUNER_STATE, C.TUNER_STATE_STOP);
    } else {
      putTunerExtra(intent);
    }
    try {
      mContext.sendBroadcast(intent); // Send Broadcast with all info
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return intent;
  }


    // Presets:

  private int station_index_get (String freq) {//int freq) {
    preset_curr_fix ();
    if (preset_num > 0) {
      if (freq.equals (plst_freq [preset_curr])) {
        return (preset_curr);
      }
      for (int ctr = 0; ctr < preset_num; ctr ++) {
        if (freq.equals (plst_freq [preset_curr])) {
          return (ctr);
        }
      }
    }
    return (-1);
  }

  private int preset_curr_fix () {
    if (preset_num < 0)
      preset_num = 0;
    if (preset_num >= com_api.PRESET_COUNT)
      preset_num = com_api.PRESET_COUNT;

    if (preset_curr < 0)
      preset_curr = preset_num - 1;
    if (preset_curr < 0)
      preset_curr = 0;

    if (preset_curr >= preset_num)
      preset_curr = 0;
    return (preset_curr);
  }


    // Phase/Countdown/Error stuff:

  private void phase_cdown_set (String phase, int cdown) {
    com_uti.logd ("phase: " + phase + "  cdown: " + cdown);
    mApi.radio_phase = phase;
    mApi.radio_cdown = "" + cdown;
    //sendRadioStatus ();                                            // Update widgets, apps, etc. (displays_update too intense ?)
  }

  private void phase_error_set (String phase, String error) {
    com_uti.loge ("phase: " + phase + "  error: " + error);
    mApi.radio_phase = phase;
    mApi.radio_error = error;
    mApi.radio_cdown = "0";
    //sendRadioStatus ();                                            // Update widgets, apps, etc. (displays_update too intense ?)
  }


    // AUDIO:

  private String audio_state_set (String state) {                       // Called only by onStartCommand()
    com_uti.logd ("state: " + state);
    if (state.equalsIgnoreCase ("toggle")) {                            // TOGGLE:
      if (mApi.audio_state.equalsIgnoreCase ("start"))
        state = "pause";
      else
        state = "start";
    }
    if (state.equalsIgnoreCase ("start")) {                             // If Audio Start...
      if (! mApi.audio_state.equalsIgnoreCase ("start")) {         // If audio not started (Could be stopped or paused)
        String stereo = com_uti.prefs_get (mContext, "audio_stereo", "Stereo");
        mAudioAPI.audio_stereo_set (stereo);                            // Set audio stereo from prefs, before audio is started

        if (mApi.isTunerStarted()) {         // If tuner started
          mAudioAPI.audio_state_set ("Start");                          // Set Audio State synchronously
        }
        else {                                                          // Else if tuner not started
          mApi.audio_state = "Starting";                           // Signal tuner state callback that audio needs to be started
          tuner_state_set ("Start");                                    // Start tuner first, audio will start later via callback
        }
      }
    }
    else                                                                // Else for Audio Stop or Pause...
      mAudioAPI.audio_state_set (state);                                // Set Audio State synchronously

    return (mApi.audio_state);                                     // Return current audio state
  }


  // Callback called by svc_aud: audio_start(), audio_stop(), audio_pause()
  public void cb_audio_state(String audio_state) { // Audio state changed callback from svc_aud
    com_uti.logd("audio_state: " + audio_state);

    if (audio_state.equalsIgnoreCase("start")) { // If audio state = Start...
      String audio_output = com_uti.prefs_get(mContext, C.AUDIO_OUTPUT, C.AUDIO_OUTPUT_HEADSET);
      mAudioAPI.audio_output_set(audio_output); // Set Audio Output from prefs

      remote_state_set(true); // Remote State = Playing
    } else if (audio_state.equalsIgnoreCase("stop")) { // If audio state = Stop...
      remote_state_set(false); // Remote State = Stopped = Media buttons not needed ?
    } else if (audio_state.equalsIgnoreCase ("pause")) { // If audio state = Pause...
      // Remote State = Still Playing
    }
    displays_update(); // Update all displays/data sinks
  }


  // TUNER:
  private String tuner_state_set(String state) { // Called only by onStartCommand(), (maybe onDestroy in future)
    com_uti.logd ("state: " + state);

    if (state.equalsIgnoreCase(C.TUNER_STATE_TOGGLE)) {
      state = mApi.isTunerStarted() ? C.TUNER_STATE_STOP : C.TUNER_STATE_START;
    }

    switch (state.toLowerCase()) {
      case C.TUNER_STATE_START:
        // One shot Poll timer for file creates, SU commands, Bluedroid Init, then start tuner
        tuner_state_start_tmr = new Timer("t_api start", true);
        tuner_state_start_tmr.schedule(new tuner_state_start_tmr_hndlr(), 10); // Once after 0.01 seconds.
        return mApi.tuner_state;

      case C.TUNER_STATE_STOP:
        mAudioAPI.audio_state_set("Stop"); // Set Audio State  synchronously to Stop
        mTunerAPI.setTunerValue(C.TUNER_STATE, C.TUNER_STATE_STOP); // Set Tuner State asynchronously to Stop
        return mApi.tuner_state; // Return new tuner state
    }

    // Else if not stop...
    return state;
  }


    // Callback: tuner_state_chngd & tuner_state_set in TunerAPIInterface/tnr_afm calls cb_tuner_state indirectly w/ Stop, Starting, Pause
  private void cb_tuner_state (String tuner_state) {
    com_uti.logd ("tuner_state: " + tuner_state);
    if (tuner_state.equalsIgnoreCase ("starting") || tuner_state.equalsIgnoreCase ("start")) {  // If tuner = Start or Starting...
      mApi.tuner_state = "start";                                  // Tuner State = Start
      tuner_prefs_init ();                                              // Load tuner prefs
      if (mApi.audio_state.equalsIgnoreCase ("starting")) {        // If Audio starting...
        mAudioAPI.audio_state_set ("Start");                            // Set Audio State synchronously
      }
      return;
    }
    if (tuner_state.equalsIgnoreCase ("stopping") || tuner_state.equalsIgnoreCase ("stop")) {   // If tuner = Stop or Stopping...
      mApi.tuner_state = "stop";                                   // Tuner State = Stop
      //mTunerAPI = null;
      com_uti.logd ("Before stopSelf()");
      stopSelf ();                                                      // Stop this service entirely
      com_uti.logd ("After  stopSelf()");
    }
  }

  private class tuner_state_start_tmr_hndlr extends TimerTask {

    public void run () {
      int ret;

      phase_cdown_set("Files Init", 5);
      ret = files_init(); // /data/data/fm.a2d.sf/files/: busybox, ssd, s.wav, b1.bin, b2.bin
      if (ret != 0) {
        com_uti.loge ("files_init IGNORE Errors: " + ret);
        //phase_error_set ("Files Init", "File Errors: " + ret);
        //return;
      }

      com_uti.logd ("Starting Tuner...");
      phase_cdown_set ("Tuner Start", 20);

      mTunerAPI.setTunerValue("radio_nop",   "Start");                     // 1st packet always fails, so this is a NOP

      mTunerAPI.setTunerValue(C.TUNER_STATE, C.TUNER_STATE_START);                     // This starts the daemon

      if (tuner_state_start_tmr != null)
        tuner_state_start_tmr.cancel ();                                // Stop one shot poll timer

      com_uti.logd ("done");
    }
  }


  // Other non state machine tuner stuff:
  private void tuner_rds_af_state_set (String val) {
    mTunerAPI.setTunerValue("tuner_rds_af_state", val);
    com_uti.prefs_set (mContext, "tuner_rds_af_state", val);
  }

  private void tuner_prefs_init () { // Load tuner prefs
    String band = com_uti.prefs_get(mContext, C.TUNER_BAND, "EU");
    mTunerAPI.setTunerValue(C.TUNER_BAND, band);
    com_uti.setTunerBand(band);

    String stereo = com_uti.prefs_get(mContext, "tuner_stereo", "Stereo");
    mTunerAPI.setTunerValue("tuner_stereo", stereo);

    //tuner_rds_af_state_set(com_uti.prefs_get (mContext, "tuner_rds_af_state", "stop")); // !! Always rewrites pref

    int freq = com_uti.prefs_get(mContext, C.TUNER_FREQUENCY, 87500);
    tuner_freq_set(String.valueOf(freq)); // Set initial frequency
  }


  // To fix float problems with 106.1 becoming 106099
  private void tuner_freq_set(String freq) {
    com_uti.logd ("freq: " + freq);
    int ifreq = com_uti.tnru_band_new_freq_get(freq, mApi.int_tuner_freq); // Deal with up, down, etc.
    //int ifreq = com_uti.tnru_khz_get (freq);
    ifreq += 25;        // Round up...
    ifreq = ifreq / 50; // Nearest 50 KHz
    ifreq *= 50;        // Back to KHz scale
    com_uti.logd ("ifreq: " + ifreq);

    mTunerAPI.setTunerValue(C.TUNER_FREQUENCY, String.valueOf(ifreq)); // Set frequency
  }


  // Tuner API callbacks:

  // Single Tuner Sub-MainService callback expands to other functions:
  public void cb_tuner_key(String key, String val) {
    com_uti.logx ("key: " + key + "  val: " + val);

    if (com_uti.device == com_uti.DEV_QCV && mAudioAPI.audio_blank_get ()) {   // If we need to kickstart audio...
      com_uti.loge ("!!!!!!!!!!!!!!!!!!!!!!!!! Kickstarting stalled audio !!!!!!!!!!!!!!!!!!!!!!!!!!");

      // Set Stereo (Frequency also works, and others ?)
      //mTunerAPI.setTunerValue ("tuner_stereo", mApi.tuner_stereo);

      mTunerAPI.setTunerValue(C.TUNER_FREQUENCY, mApi.getStringFrequencyMHz()); // Set Frequency
      mAudioAPI.audio_blank_set (false);
    }

    if (key != null) {
      if (key.equalsIgnoreCase (C.TUNER_STATE))
        cb_tuner_state(val);
      else if (key.equals(C.TUNER_FREQUENCY))
        cb_tuner_freq (val);
      else if (key.equalsIgnoreCase ("tuner_rssi"))
        cb_tuner_rssi (val);
      else if (key.equalsIgnoreCase ("tuner_qual"))
        cb_tuner_qual (val);
      else if (key.equalsIgnoreCase ("tuner_rds_pi"))
        cb_tuner_rds_pi (val);
      else if (key.equalsIgnoreCase ("tuner_rds_pt"))
        cb_tuner_rds_pt (val);
      else if (key.equalsIgnoreCase ("tuner_rds_ps"))
        cb_tuner_rds_ps (val);
      else if (key.equalsIgnoreCase ("tuner_rds_rt"))
        cb_tuner_rds_rt (val);
    }
  }

  // Frequency callback
  private void cb_tuner_freq (String freq) {

    com_uti.logd ("freq: " + freq);

    mApi.tuner_stereo = "";

    mApi.tuner_rssi = "";     // ro ... ... Values:   RSSI: 0 - 1000
    mApi.tuner_qual = "";     // ro ... ... Values:   SN 99, SN 30
    //mApi.tuner_most = "";   // ro ... ... Values:   mono, stereo, 1, 2, blend, ... ?      1.5 ?
    mApi.tuner_rds_pi = "";   // ro ... ... Values:   0 - 65535
    mApi.tuner_rds_picl = ""; // ro ... ... Values:   North American Call Letters or Hex PI for tuner_rds_pi
    mApi.tuner_rds_pt = "";   // ro ... ... Values:   0 - 31
    mApi.tuner_rds_ptyn = ""; // ro ... ... Values:   Describes tuner_rds_pt (English !)
    mApi.tuner_rds_ps = "";   // ro ... ... Values:   RBDS 8 char info or RDS Station
    mApi.tuner_rds_rt = "";   // ro ... ... Values:   64 char
    mApi.tuner_rds_af = "";   // ro ... ... Values:   Space separated array of AF frequencies
    mApi.tuner_rds_ms = "";   // ro ... ... Values:   0 - 65535   M/S Music/Speech switch code
    mApi.tuner_rds_ct = "";   // ro ... ... Values:   14 char CT Clock Time & Date
    mApi.tuner_rds_tmc = "";  // ro ... ... Values:   Space separated array of shorts
    mApi.tuner_rds_tp = "";   // ro ... ... Values:   0 - 65535   TP Traffic Program Identification code
    mApi.tuner_rds_ta = "";   // ro ... ... Values:   0 - 65535   TA Traffic Announcement code
    mApi.tuner_rds_taf = "";  // ro ... ... Values:   0 - 2^32-1  TAF TA Frequency

    int new_freq = com_uti.tnru_freq_fix(25 + com_uti.tnru_khz_get(freq));
    mApi.tuner_freq = com_uti.tnru_mhz_get (new_freq);
    mApi.int_tuner_freq = new_freq;

    displays_update();

    com_uti.prefs_set (mContext, C.TUNER_FREQUENCY, new_freq);
  }
  private void cb_tuner_rssi (String rssi) {
    displays_update ();
  }
// Qual:
  private void cb_tuner_qual (String qual) {
  }
// RDS:
  private void cb_tuner_rds_pi (String pi) {
    displays_update ();
  }
  private void cb_tuner_rds_pt (String pt) {
    displays_update ();
  }
  private void cb_tuner_rds_ps (String ps) {
    displays_update ();
  }
  private void cb_tuner_rds_rt (String rt) {
    displays_update ();
  }


  // Hardware / API dependent part of MainService:
  private int files_init() {
    com_uti.logd ("starting...");

    //String bsb_full_filename = com_uti.file_create (mContext, R.raw.busybox,  "busybox",       true);
    //    String ssd_full_filename = "";
    //if (com_uti.ssd_via_sys_run)
    //      ssd_full_filename = com_uti.file_create (mContext, R.raw.ssd,           "ssd",           true);
    //String wav_full_filename = com_uti.file_create (mContext, R.raw.s_wav,    "s.wav",         false);             // Not executable


    String add_full_filename = com_uti.file_create (mContext, R.raw.spirit_sh,  "99-spirit.sh");
//    String bb1_full_filename = com_uti.file_create (mContext, R.raw.b1_bin,     "b1.bin",        false);             // Not executable
//    String bb2_full_filename = com_uti.file_create (mContext, R.raw.b2_bin,     "b2.bin",        false);             // Not executable

        // Check:
    int ret = 0;
        //if (! com_uti.access_get (bsb_full_filename, false, false, true)) { // rwX
        //  com_uti.loge ("error unexecutable busybox utility");
        //  ret ++;
        //}
        //      if (/*com_uti.ssd_via_sys_run &&*/ ! com_uti.access_get (ssd_full_filename, false, false, true)) { // rwX
        //        com_uti.loge ("error unexecutable ssd utility");                  // !!!! Get this sometimes, so must ignore errors.
        //        ret ++;
        //      }

      if (! com_uti.access_get (add_full_filename, false, false, true)) { // rwX
        com_uti.loge ("error unexecutable addon.d script 99-spirit.sh");
        ret ++;
      }
/*
      if (! com_uti.access_get (bb1_full_filename, true, false, false)) { // Rwx
        com_uti.loge ("error inaccessible bb1 file");
        ret ++;
      }
      if (! com_uti.access_get (bb2_full_filename, true, false, false)) { // Rwx
        com_uti.loge ("error inaccessible bb2 file");
        ret ++;
      }
*/
    com_uti.logd ("done ret: " + ret);
    return ret;
  }




    // Remote control client, for lockscreen and BT AVRCP:

  private void remote_state_set (boolean state) { // Called only by cb_audio_state w/ state=true for start, false for stop
  }

  // Notification shade
  private void notif_radio_update () { // Called only by displays_update() which is called only by cb_* callbacks
    if (mApi.audio_state.equalsIgnoreCase("start")) { // If audio started...
      if (need_startfg) {
        need_startfg = false;
        com_uti.logd ("startForeground");
        showNotificationAndStartForeground(true);
      } else {
        showNotificationAndStartForeground(false);
      }
    } else {
      need_startfg = true;
      //notif_state_set (false);
      //stopForeground (true);                                          // !! Need FG for lockscreen play !! So can't unset foreground when paused,,,   Only in onDestroy
//stopForeground (false);   // !!!! ???? Can stopForeground but keep active notification ??
    }
  }
 
   // Start/stop service = foreground, FM Notification in status bar, Expanded message in "Notifications" window.

  private void notif_state_set (boolean state) {                        // Called only by onCreate w/ state = true and onDestroy w/ state = false
    com_uti.logd ("state: " + state);
    if (!state) { // Notifications off, go to idle non-foreground state
      stopForeground (true); // MainService not in foreground state and remove notification (true)
      return;
    }

    ///*    !!!! Need this or bad audio !!!!
    showNotificationAndStartForeground(true);
  }

  private void showNotificationAndStartForeground(boolean needStartForeground) {
    Intent mainInt = new Intent(mContext, MainActivity.class);
    mainInt.setAction("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER");

    PendingIntent pendingMain = PendingIntent.getActivity(mContext, 0, mainInt, PendingIntent.FLAG_UPDATE_CURRENT);
    PendingIntent pendingKill = com_api.createPendingIntent(mContext, C.TUNER_STATE, C.TUNER_STATE_STOP);
    PendingIntent pendingRecord = com_api.createPendingIntent(mContext, C.RECORD_STATE, C.RECORD_STATE_TOGGLE);
    PendingIntent pendingPrev = com_api.createPendingIntent(mContext, C.TUNER_SCAN_STATE, C.TUNER_SCAN_DOWN);
    PendingIntent pendingNext = com_api.createPendingIntent(mContext, C.TUNER_SCAN_STATE, C.TUNER_SCAN_UP);

    RemoteViews layout = new RemoteViews(getPackageName(), R.layout.notification_small);

    Notification.Builder notify = new Notification.Builder(this)
        //.setContentTitle(mContext.getString(R.string.application_name))
        //.setContentText(getString(R.string.notification_starting))
        .setColor(getResources().getColor(R.color.primary_blue))
        .setSmallIcon(R.drawable.ic_radio)
        .setContentIntent(pendingMain)
        .setContent(layout)
        .setOngoing(true);
    if (mApi != null) {
      boolean isRecord = mApi.audio_record_state.equals(C.RECORD_STATE_START);

      if (isRecord) {
        layout.setImageViewResource(R.id.notification_record, R.drawable.btn_record_press);
      }

      layout.setTextViewText(R.id.notification_frequency, mApi.getStringFrequencyMHz());

      /*String labelToggle = getString(mApi.isTunerStarted()
              ? R.string.notification_button_pause
              : R.string.notification_button_play
      );
      String labelRecord = getString(isRecord
              ? R.string.notification_button_record_stop
              : R.string.notification_button_record_start
      );

      String recordText = isRecord ? "; recording" : "";

      if (isRecord && mAudioAPI.getRecorder() != null) {
        recordText += " " + com_uti.getTimeStringBySeconds(mAudioAPI.getRecorder().getCurrentDuration());
      }

      notify
          .addAction(R.drawable.ic_pause, labelToggle, pendingToggle)
          .addAction(R.drawable.ic_stop, getString(R.string.notification_button_stop), pendingKill)
          .addAction(R.drawable.btn_record, labelRecord, pendingRecord)
          .setContentText(String.format("%sMHz%s", mApi.getStringFrequencyMHz(), recordText));*/
    }

    layout.setOnClickPendingIntent(R.id.notification_icon, pendingMain);
    layout.setOnClickPendingIntent(R.id.notification_frequency, pendingMain);
    layout.setOnClickPendingIntent(R.id.notification_pause, pendingKill);
    layout.setOnClickPendingIntent(R.id.notification_record, pendingRecord);
    layout.setOnClickPendingIntent(R.id.notification_prev, pendingPrev);
    layout.setOnClickPendingIntent(R.id.notification_next, pendingNext);

    mynot = notify.build();

    if (needStartForeground) {
      startForeground(NOTIFICATION_ID, mynot); // Now in audio foreground
    }

    mNotificationManager.notify(NOTIFICATION_ID, mynot);
  }
}