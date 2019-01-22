package fm.a2d.sf;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;
import fm.a2d.sf.helper.L;
import fm.a2d.sf.helper.Utils;

import java.util.Locale;
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
  private Notification mNotification = null;

  // No relevance to NOTIFICATION_ID except to uniquely identify notification !! Spirit1 uses same !!
  private static final int NOTIFICATION_ID = 2112;

  private boolean mNeedStartForeground = true;
  private Timer mIntervalTunerStateTimer = null;
  private NotificationManager mNotificationManager = null;

  private void log(String s) {
    L.w(L.T.SERVICE_MAIN, s);
  }

  private PendingIntent pendingToggle;
  private PendingIntent pendingKill;
  private PendingIntent pendingRecord;
  private PendingIntent pendingPrev;
  private PendingIntent pendingNext;

  @Override
  public void onCreate() { // When service newly created...
    log("onCreate");

    try {
      //com_uti.strict_mode_set(true); // Enable strict mode; disabled for now
      com_uti.strict_mode_set(false);  // !!!! Disable strict mode so we can send network packets from Java

      mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

      notif_state_set(true);

      if (mApi == null) {
        mApi = new com_api(this); // Instantiate Common API class
        log("api == null; created - " + mApi);
      }

      mAudioAPI = new svc_aud(this, this, mApi); // Instantiate audio class
      mTunerAPI = new ServiceTuner(this, this, mApi);  // Instantiate tuner class
    } catch (Throwable e) {
      e.printStackTrace();
    }

    pendingToggle = com_api.createPendingIntent(mContext, C.AUDIO_STATE, C.AUDIO_STATE_TOGGLE);
    pendingKill = com_api.createPendingIntent(mContext, C.TUNER_STATE, C.TUNER_STATE_STOP);
    pendingRecord = com_api.createPendingIntent(mContext, C.RECORD_STATE, C.RECORD_STATE_TOGGLE);
    pendingPrev = com_api.createPendingIntent(mContext, C.TUNER_SCAN_STATE, C.TUNER_SCAN_DOWN);
    pendingNext = com_api.createPendingIntent(mContext, C.TUNER_SCAN_STATE, C.TUNER_SCAN_UP);
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
    log("onStartCommand: intent=" + intent + "; flags=" + flags + "; startId=" + startId);

    try {
      if (intent == null) {
        log("intent == null");
        return START_STICKY;
      }

      String action = intent.getAction();
      if (action == null) {
        log("action == null");
        return START_STICKY;
      }

      if (!action.equalsIgnoreCase(ACTION_SET)) {
        log("action: " + action);
        return START_STICKY;
      }

      Bundle extras = intent.getExtras();
      log("extras: " + extras);
      if (extras == null) {
        return START_STICKY;
      }

      String val;

      val = extras.getString(C.NOTIFICATION_TYPE, "");
      if (!val.isEmpty()) {
        showNotificationAndStartForeground(false);
        return START_STICKY;
      }

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
        mAudioAPI.setAudioRecordState(val);
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

    int ifreq = com_uti.int_get(freq_khz);
    if (ifreq >= 50000 && ifreq < 500000) {
      mApi.int_tuner_freq = ifreq;
    }
    log("putTunerExtra: api.int_tuner_freq: " + mApi.getIntFrequencyKHz());
    intent.putExtra(C.TUNER_FREQUENCY, mApi.getIntFrequencyKHz());

    //intent.putExtra ("tuner_stereo",       mTunerAPI.getTunerValue ("tuner_stereo"));
    //intent.putExtra ("tuner_thresh",       mTunerAPI.getTunerValue ("tuner_thresh"));
    //intent.putExtra ("tuner_scan_state",   mTunerAPI.getTunerValue ("tuner_scan_state"));

    //intent.putExtra ("tuner_rds_state",    mTunerAPI.getTunerValue ("tuner_rds_state"));
    //intent.putExtra ("tuner_rds_af_state", mTunerAPI.getTunerValue ("tuner_rds_af_state"));
    //intent.putExtra ("tuner_rds_ta_state", mTunerAPI.getTunerValue ("tuner_rds_ta_state"));

    //intent.putExtra ("tuner_extra_cmd",    mTunerAPI.getTunerValue ("tuner_extra_cmd"));
    //intent.putExtra ("tuner_extra_resp",   mTunerAPI.getTunerValue ("tuner_extra_resp"));

    intent.putExtra("tuner_rssi",         String.valueOf(mApi.getRssi())); //mTunerAPI.getTunerValue ("tuner_rssi"));
    //intent.putExtra ("tuner_qual",         mTunerAPI.getTunerValue ("tuner_qual"));
    intent.putExtra("tuner_most",         "stereo");      //mTunerAPI.getTunerValue ("tuner_most"));

    intent.putExtra("tuner_rds_pi",       mApi.tuner_rds_pi);    //mTunerAPI.getTunerValue ("tuner_rds_pi"));
    intent.putExtra("tuner_rds_picl",     mApi.tuner_rds_picl);  //mTunerAPI.getTunerValue ("tuner_rds_picl"));
    //intent.putExtra ("tuner_rds_pt",       mApi.tuner_rds_pt);  //mTunerAPI.getTunerValue ("tuner_rds_pt"));
    intent.putExtra("tuner_rds_ptyn",     mApi.tuner_rds_ptyn);  //mTunerAPI.getTunerValue ("tuner_rds_ptyn"));
    intent.putExtra("tuner_rds_ps",       mApi.tuner_rds_ps);    //mTunerAPI.getTunerValue ("tuner_rds_ps"));
    intent.putExtra("tuner_rds_rt",       mApi.tuner_rds_rt);    //mTunerAPI.getTunerValue ("tuner_rds_rt"));

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

    log("sendRadioStatus: audio_state: " + mApi.audio_state + "; audio_output: " + mApi.audio_output + "; audio_stereo: " + mApi.audio_stereo + "; audio_record_state: " + mApi.audio_record_state);

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

    // Phase/Countdown/Error stuff:

  private void phase_cdown_set(String phase, int cdown) {
    log("phase_cdown_set / phase=" + phase + "; cdown=" + cdown);
    mApi.radio_phase = phase;
    mApi.radio_cdown = "" + cdown;
    //sendRadioStatus (); // Update widgets, apps, etc. (displays_update too intense ?)
  }

  private void phase_error_set(String phase, String error) {
    log("phase_error_set / phase=" + phase + "; error=" + error);
    mApi.radio_phase = phase;
    mApi.radio_error = error;
    mApi.radio_cdown = "0";
    //sendRadioStatus (); // Update widgets, apps, etc. (displays_update too intense ?)
  }

  // AUDIO:
  private String audio_state_set(String state) { // Called only by onStartCommand()
    log("audio_state_set(" + state + ")");

    if (state.equals(C.AUDIO_STATE_TOGGLE)) { // TOGGLE:
      if (mApi.audio_state.equals(C.AUDIO_STATE_START))
        state = C.AUDIO_STATE_PAUSE;
      else
        state = C.AUDIO_STATE_START;
      log("audio_state_set(" + state + ") <= after replace toggle");
    }


    if (state.equals(C.AUDIO_STATE_START)) { // If Audio Start...
      log("set audio_state=start");
      if (!mApi.audio_state.equals(C.AUDIO_STATE_START)) { // If audio not started (Could be stopped or paused)
        String stereo = com_uti.prefs_get(mContext, "audio_stereo", "stereo"); // FIXME
        mAudioAPI.audio_stereo_set(stereo); // Set audio stereo from prefs, before audio is started

        if (mApi.isTunerStarted()) { // If tuner started
          mAudioAPI.audio_state_set(C.AUDIO_STATE_START); // Set Audio State synchronously
        } else { // Else if tuner not started
          mApi.audio_state = C.AUDIO_STATE_STARTING; // Signal tuner state callback that audio needs to be started
          tuner_state_set(C.TUNER_STATE_START); // Start tuner first, audio will start later via callback
        }
      }
    } else { // Else for Audio Stop or Pause...
      log("set audio_state=" + state);
      mAudioAPI.audio_state_set(state); // Set Audio State synchronously
    }

    return mApi.audio_state; // Return current audio state
  }

  // Callback called by svc_aud: audio_start(), audio_stop(), audio_pause()
  public void cb_audio_state(String audio_state) { // Audio state changed callback from svc_aud
    log("cb_audio_state(" + audio_state + ")");

    switch (audio_state) {
      case C.AUDIO_STATE_START:
        String audio_output = com_uti.prefs_get(mContext, C.AUDIO_OUTPUT, C.AUDIO_OUTPUT_HEADSET);
        mAudioAPI.audio_output_set(audio_output); // Set Audio Output from prefs

        remote_state_set(true); // Remote State = Playing
        break;

      case C.AUDIO_STATE_STOP:  // If audio state = Stop...
        remote_state_set(false); // Remote State = Stopped = Media buttons not needed ?
        break;

      case C.AUDIO_STATE_PAUSE:  // If audio state = Pause...
        // Remote State = Still Playing
        break;
    }
    displays_update(); // Update all displays/data sinks
  }


  // TUNER:
  private String tuner_state_set(String state) { // Called only by onStartCommand(), (maybe onDestroy in future)
    log("tuner_state_set(" + state + ")");

    if (state.equals(C.TUNER_STATE_TOGGLE)) {
      state = mApi.isTunerStarted() ? C.TUNER_STATE_STOP : C.TUNER_STATE_START;
      log("tuner_state_set(" + state + ") <== after resolve toggle");
    }

    switch (state.toLowerCase()) {
      case C.TUNER_STATE_START:
        // One shot Poll timer for file creates, SU commands, Bluedroid Init, then start tuner
        mIntervalTunerStateTimer = new Timer("t_api start", true);
        mIntervalTunerStateTimer.schedule(new IntervalTunerStateHandler(), 10); // Once after 0.01 seconds.
        return mApi.tuner_state;

      case C.TUNER_STATE_STOP:
        mAudioAPI.audio_state_set(C.AUDIO_STATE_STOP); // Set Audio State  synchronously to Stop
        mTunerAPI.setTunerValue(C.TUNER_STATE, C.TUNER_STATE_STOP); // Set Tuner State asynchronously to Stop
        return mApi.tuner_state; // Return new tuner state
    }

    // Else if not stop...
    return state;
  }


    // Callback: tuner_state_chngd & tuner_state_set in TunerAPIInterface/tnr_afm calls cb_tuner_state indirectly w/ Stop, Starting, Pause
  private void cb_tuner_state(String tuner_state) {
    log("cb_tuner_state(" + tuner_state + ")");
    if (tuner_state.equalsIgnoreCase ("starting") || tuner_state.equalsIgnoreCase("start")) {  // If tuner = Start or Starting...
      mApi.tuner_state = "start"; // Tuner State = Start
      tuner_prefs_init(); // Load tuner prefs
      if (mApi.audio_state.equals(C.AUDIO_STATE_STARTING)) { // If Audio starting...
        mAudioAPI.audio_state_set(C.AUDIO_STATE_START); // Set Audio State synchronously
      }
      return;
    }
    if (tuner_state.equalsIgnoreCase("stopping") || tuner_state.equalsIgnoreCase("stop")) {   // If tuner = Stop or Stopping...
      mApi.tuner_state = "stop"; // Tuner State = Stop
      //mTunerAPI = null;
      log("cb_tuner_state / Before stopSelf()");
      stopSelf(); // Stop this service entirely
      log("cb_tuner_state / After stopSelf()");
    }
  }

  private class IntervalTunerStateHandler extends TimerTask {

    public void run () {
      int ret;

      phase_cdown_set("Files Init", 5);
      ret = files_init(); // /data/data/fm.a2d.sf/files/: busybox, ssd, s.wav, b1.bin, b2.bin
      if (ret != 0) {
        log("files_init IGNORE Errors: " + ret);
        //phase_error_set ("Files Init", "File Errors: " + ret);
        //return;
      }

      log("TimerTask: Starting Tuner...");
      phase_cdown_set("Tuner Start", 20);

      mTunerAPI.setTunerValue(C.RADIO_NOP, C.TUNER_STATE_START); // 1st packet always fails, so this is a NOP
      mTunerAPI.setTunerValue(C.TUNER_STATE, C.TUNER_STATE_START); // This starts the daemon

      if (mIntervalTunerStateTimer != null) {
        mIntervalTunerStateTimer.cancel(); // Stop one shot poll timer
      }

      log("TimerTask: tuner done");
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


  /**
   * To fix float problems with 106.1 becoming 106099;
   * send frequency in KHz
   * @param freq MHz
   */
  private void tuner_freq_set(String freq) {
    log("tuner_freq_set(" + freq + ")");
    int ifreq = com_uti.tnru_band_new_freq_get(freq, mApi.int_tuner_freq); // Deal with up, down, etc.
    //int ifreq = com_uti.tnru_khz_get (freq);
    ifreq += 25;        // Round up...
    ifreq = ifreq / 50; // Nearest 50 KHz
    ifreq *= 50;        // Back to KHz scale
    log("tfs / ifreq: " + ifreq);

    mTunerAPI.setTunerValue(C.TUNER_FREQUENCY, String.valueOf(ifreq)); // Set frequency
  }


  // Tuner API callbacks:

  // Single Tuner Sub-MainService callback expands to other functions:
  public void cb_tuner_key(String key, String val) {
    log("cb_tuner_key / key=" + key + "; val=" + val);

    if (com_uti.device == com_uti.DEV_QCV && mAudioAPI.audio_blank_get()) { // If we need to kickstart audio...
      log("!!!!!!!!!!!!!!!!!!!!!!!!! Kickstarting stalled audio !!!!!!!!!!!!!!!!!!!!!!!!!!");

      // Set Stereo (Frequency also works, and others ?)
      //mTunerAPI.setTunerValue ("tuner_stereo", mApi.tuner_stereo);

      mTunerAPI.setTunerValue(C.TUNER_FREQUENCY, String.valueOf(mApi.getIntFrequencyKHz())); // Set Frequency
      mAudioAPI.audio_blank_set(false);
    }

    if (key != null) { // FIXME switch
      if (key.equalsIgnoreCase (C.TUNER_STATE))
        cb_tuner_state(val);
      else if (key.equals(C.TUNER_FREQUENCY))
        cb_tuner_freq(val);
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

    log("cb_tuner_freq(" + freq + ")");

    mApi.tuner_stereo = "";

    mApi.tuner_rssi = 0;     // ro ... ... Values:   RSSI: 0 - 1000
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
    //mApi.tuner_freq = com_uti.tnru_mhz_get (new_freq);
    mApi.int_tuner_freq = new_freq;

    displays_update();

    com_uti.prefs_set(mContext, C.TUNER_FREQUENCY, new_freq);
  }
  private void cb_tuner_rssi(String rssi) {
    displays_update ();
  }

  // Qual:
  private void cb_tuner_qual(String qual) {}

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
    log("files_init: starting...");

    //String bsb_full_filename = com_uti.file_create (mContext, R.raw.busybox,  "busybox",       true);
    //    String ssd_full_filename = "";
    //if (com_uti.ssd_via_sys_run)
    //      ssd_full_filename = com_uti.file_create (mContext, R.raw.ssd,           "ssd",           true);
    //String wav_full_filename = com_uti.file_create (mContext, R.raw.s_wav,    "s.wav",         false);             // Not executable


    String filename = com_uti.file_create (mContext, R.raw.spirit_sh,  "99-spirit.sh");
//    String bb1_full_filename = com_uti.file_create (mContext, R.raw.b1_bin,     "b1.bin",        false);             // Not executable
//    String bb2_full_filename = com_uti.file_create (mContext, R.raw.b2_bin,     "b2.bin",        false);             // Not executable

        // Check:
    int ret = 0;
        //if (! com_uti.access_get (bsb_full_filename, false, false, true)) { // rwX
        //  log("error unexecutable busybox utility");
        //  ret ++;
        //}
        //      if (/*com_uti.ssd_via_sys_run &&*/ ! com_uti.access_get (ssd_full_filename, false, false, true)) { // rwX
        //        log("error unexecutable ssd utility");                  // !!!! Get this sometimes, so must ignore errors.
        //        ret ++;
        //      }

      if (!com_uti.access_get(filename, false, false, true)) { // rwX
        log("error unexecutable addon.d script 99-spirit.sh");
        ret ++;
      }
/*
      if (! com_uti.access_get (bb1_full_filename, true, false, false)) { // Rwx
        log("error inaccessible bb1 file");
        ret ++;
      }
      if (! com_uti.access_get (bb2_full_filename, true, false, false)) { // Rwx
        log("error inaccessible bb2 file");
        ret ++;
      }
*/
    log("files_init: {done ret}=" + ret);
    return ret;
  }


  // Remote control client, for lockscreen and BT AVRCP:
  private void remote_state_set (boolean state) { // Called only by cb_audio_state w/ state=true for start, false for stop
    // empty
  }

  // Notification shade
  private void notif_radio_update () { // Called only by displays_update() which is called only by cb_* callbacks
    if (mApi.audio_state.equalsIgnoreCase(C.AUDIO_STATE_START)) { // If audio started...
      if (mNeedStartForeground) {
        mNeedStartForeground = false;
        log("notif_radio_update: startForeground");
        showNotificationAndStartForeground(true);
      } else {
        showNotificationAndStartForeground(false);
      }
    } else {
      mNeedStartForeground = true;
      //notif_state_set (false);
      //stopForeground (true);  // !! Need FG for lockscreen play !! So can't unset foreground when paused,,,   Only in onDestroy
//stopForeground (false);   // !!!! ???? Can stopForeground but keep active notification ??
    }
  }
 
  // Start/stop service = foreground, FM Notification in status bar, Expanded message in "Notifications" window.
  // Called only by onCreate w/ state = true and onDestroy w/ state = false
  private void notif_state_set (boolean state) {
    log("notif_state_set: state=" + state);

    // Notifications off, go to idle non-foreground state
    if (!state) {
      stopForeground(true); // MainService not in foreground state and remove notification (true)
      return;
    }

    ///*    !!!! Need this or bad audio !!!!
    showNotificationAndStartForeground(true);
  }

  private void showNotificationAndStartForeground(boolean needStartForeground) {
    Intent mainInt = new Intent(mContext, MainActivity.class);
    mainInt.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
    PendingIntent pendingMain = PendingIntent.getActivity(mContext, 0, mainInt, PendingIntent.FLAG_UPDATE_CURRENT);

    Notification.Builder notify = new Notification.Builder(this)
        .setColor(getResources().getColor(R.color.primary_blue))
        .setSmallIcon(R.drawable.ic_radio)
        .setContentIntent(pendingMain)
        .setPriority(Notification.PRIORITY_HIGH)
        .setOngoing(true);

    switch (Utils.getPrefInt(this, C.NOTIFICATION_TYPE)) {

      case C.NOTIFICATION_TYPE_CUSTOM:
        createCustomNotification(notify, pendingMain);
        break;

      case C.NOTIFICATION_TYPE_CLASSIC:
      default:
        createClassicNotification(notify);
        break;
    }


    mNotification = notify.build();

    if (needStartForeground) {
      startForeground(NOTIFICATION_ID, mNotification); // Now in audio foreground
    }

    mNotificationManager.notify(NOTIFICATION_ID, mNotification);
  }

  private boolean mNotificationColorRecordBlinkState = false;

  private void createClassicNotification(Notification.Builder notify) {

    if (mApi != null) {
      boolean isRecord = mApi.audio_record_state.equals(C.RECORD_STATE_START);
      String labelToggle = getString(mApi.isTunerStarted()
          ? R.string.notification_button_pause
          : R.string.notification_button_play
      );
      String labelRecord = getString(isRecord
          ? R.string.notification_button_record_stop
          : R.string.notification_button_record_start
      );

      String recordText = isRecord ? getString(R.string.notification_text_recording) : "";

      if (isRecord && mAudioAPI.getRecorder() != null) {
        recordText += " " + Utils.getTimeStringBySeconds(mAudioAPI.getRecorder().getCurrentDuration());
      }

      String name = mApi.getPresetNameByFrequency(mApi.getIntFrequencyKHz());
      String freq = String.format(Locale.ENGLISH, "%.1fMHz%s", mApi.getIntFrequencyKHz() / 1000f, recordText);

      notify
          .addAction(R.drawable.ic_pause, labelToggle, pendingToggle)
          .addAction(R.drawable.ic_stop, getString(R.string.notification_button_stop), pendingKill)
          .addAction(R.drawable.ic_record, labelRecord, pendingRecord)
          .setVisibility(Notification.VISIBILITY_PUBLIC)
          .setContentTitle(getString(R.string.application_name))
          .setContentText(freq)
          .setShowWhen(false);

      if (name != null) {
        notify.setContentInfo(name);
      }

      if (isRecord) {
        if (mNotificationColorRecordBlinkState) {
          notify.setColor(0xffff0000);
          notify.setSmallIcon(R.drawable.ic_recording);
        }
        mNotificationColorRecordBlinkState = !mNotificationColorRecordBlinkState;
      }
    }
  }

  private void createCustomNotification(Notification.Builder notify, PendingIntent pendingMain) {
    RemoteViews layout = new RemoteViews(getPackageName(), R.layout.notification_small);

    if (mApi != null) {
      boolean isRecord = mApi.audio_record_state.equals(C.RECORD_STATE_START);
      layout.setImageViewResource(R.id.notification_record, !isRecord ? R.drawable.ic_record : R.drawable.ic_record_press);
      layout.setTextViewText(R.id.notification_frequency, String.format(Locale.ENGLISH, "%.1f", mApi.getIntFrequencyKHz() / 1000f));
    }

    layout.setOnClickPendingIntent(R.id.notification_icon, pendingMain);
    layout.setOnClickPendingIntent(R.id.notification_frequency, pendingMain);
    layout.setOnClickPendingIntent(R.id.notification_pause, pendingKill);
    layout.setOnClickPendingIntent(R.id.notification_record, pendingRecord);
    layout.setOnClickPendingIntent(R.id.notification_prev, pendingPrev);
    layout.setOnClickPendingIntent(R.id.notification_next, pendingNext);
    notify.setContent(layout);
  }
}