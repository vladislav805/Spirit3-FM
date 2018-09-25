package fm.a2d.sf;

import java.util.TimerTask;
import java.util.Timer;

import android.annotation.SuppressLint;
import android.content.Context;
import fm.a2d.sf.helper.L;

// Tuner Sub-service

@SuppressWarnings("WeakerAccess")
public class ServiceTuner {

  private MainService mTuner;
  private com_api mApi;

  private boolean mNeedPolling = true;
  private boolean mIsPolling = false;
  private Timer mPollingTimer;
  private int last_poll_freq = -1;
  private int last_poll_rssi = -1;
  private String last_poll_most = "-1";

  // Context & Tuner API callback constructor
  public ServiceTuner(Context c, MainService cb_tnr, com_api svc_com_api) {
    com_uti.logd ("constructor context: " + c + "  cb_tnr: " + cb_tnr);
    mTuner = cb_tnr;
    mApi = svc_com_api;
  }


  // Tuner API:

  public String getTunerValue(String key) {
    if (key == null)
      return ("");

    else if (key.equalsIgnoreCase ("test"))
      return (com_uti.s2d_get (key));

    else if (key.equalsIgnoreCase(C.TUNER_STATE))
      return mApi.tuner_state;

    else if (mApi.isTunerStarted())
      return (com_uti.s2d_get (key));

        // Else if not on, use cached info:
    else if (key.equalsIgnoreCase (C.TUNER_BAND))
      return mApi.tuner_band;
    else if (key.equalsIgnoreCase (C.TUNER_FREQUENCY))
      return mApi.getStringFrequencyMHz();
    else if (key.equalsIgnoreCase ("tuner_stereo"))
      return (mApi.tuner_stereo);
    else if (key.equalsIgnoreCase ("tuner_thresh"))
      return (mApi.tuner_thresh);
    else if (key.equals(C.TUNER_SCAN_STATE))
      return mApi.tuner_scan_state;

    /*else if (key.equalsIgnoreCase ("tuner_rds_state"))
      return (mApi.tuner_rds_state);
    else if (key.equalsIgnoreCase ("tuner_rds_af_state"))
      return (mApi.tuner_rds_af_state);
    else if (key.equalsIgnoreCase ("tuner_rds_ta_state"))
      return (mApi.tuner_rds_ta_state);*/

    else if (key.equalsIgnoreCase ("tuner_extra_cmd"))
      return (mApi.tuner_extra_cmd);
    else if (key.equalsIgnoreCase ("tuner_extra_resp"))
      return (mApi.tuner_extra_resp);

    else if (key.equalsIgnoreCase ("tuner_rssi"))
      return (mApi.tuner_rssi);
    else if (key.equalsIgnoreCase ("tuner_qual"))
      return (mApi.tuner_qual);
    else if (key.equalsIgnoreCase ("tuner_most"))
      return (mApi.tuner_most);

    /*else if (key.equalsIgnoreCase ("tuner_rds_pi"))
      return (mApi.tuner_rds_pi);
    else if (key.equalsIgnoreCase ("tuner_rds_picl"))
      return (mApi.tuner_rds_picl);
    else if (key.equalsIgnoreCase ("tuner_rds_pt"))
      return (mApi.tuner_rds_pt);
    else if (key.equalsIgnoreCase ("tuner_rds_ptyn"))
      return (mApi.tuner_rds_ptyn);
    else if (key.equalsIgnoreCase ("tuner_rds_ps"))
      return (mApi.tuner_rds_ps);
    else if (key.equalsIgnoreCase ("tuner_rds_rt"))
      return (mApi.tuner_rds_rt);

    else if (key.equalsIgnoreCase ("tuner_rds_af"))
      return (mApi.tuner_rds_af);
    else if (key.equalsIgnoreCase ("tuner_rds_ms"))
      return (mApi.tuner_rds_ms);
    else if (key.equalsIgnoreCase ("tuner_rds_ct"))
      return (mApi.tuner_rds_ct);
    else if (key.equalsIgnoreCase ("tuner_rds_tmc"))
      return (mApi.tuner_rds_tmc);
    else if (key.equalsIgnoreCase ("tuner_rds_tp"))
      return (mApi.tuner_rds_tp);
    else if (key.equalsIgnoreCase ("tuner_rds_ta"))
      return (mApi.tuner_rds_ta);
    else if (key.equalsIgnoreCase ("tuner_rds_taf"))
      return (mApi.tuner_rds_taf);*/

    else
      return ("0");  //return ("");

  }

  public void setTunerValue(String key, String val) {
    if (key == null) {
      return;
    }

    L.getInstance().write("ST::setTunerValue(" + key + ", " + val + "); isTunerStart = " + mApi.isTunerStarted());

    if (key.equals(C.TUNER_STATE)) { // If tuner_state...
      setTunerState(val);
    } else if (key.equalsIgnoreCase ("radio_nop")) {// If radio_nop...
      com_uti.s2d_set(key, val);
    }

    //else if (key.equalsIgnoreCase (C.TUNER_BAND))
    //  return (tuner_band_set (val));

    else if (mApi.isTunerStarted())          // If tuner_state = Start...
    {
      com_uti.s2d_set(key, val);
    } else if (key.equalsIgnoreCase(C.TUNER_FREQUENCY)) {
      mApi.tuner_freq = val;
    } else if (key.equalsIgnoreCase ("tuner_stereo")) {
      mApi.tuner_stereo = val;
    } else if (key.equalsIgnoreCase ("tuner_thresh")) {
      mApi.tuner_thresh = val;
    } else if (key.equals(C.TUNER_SCAN_STATE)) {
      mApi.tuner_scan_state = val;
    } else if (key.equalsIgnoreCase ("tuner_rds_state")) {
      mApi.tuner_rds_state = val;
    } else if (key.equalsIgnoreCase ("tuner_rds_af_state")) {
      mApi.tuner_rds_af_state = val;
    } else if (key.equalsIgnoreCase ("tuner_rds_ta_state")) {
      mApi.tuner_rds_ta_state = val;
    } else if (key.equalsIgnoreCase ("tuner_extra_cmd")) {
      mApi.tuner_extra_cmd = val;
    }

  }

/* Test Freq codes:
boolean hci = false;
int port = 0;
int freq = com_uti.int_get (val);
if (freq >= 90000 && freq < 100000) {
port = freq - 90000;    // port = 0 - 9999
}
if (freq >= 100000 && freq < 108000) {
hci = true;
port = freq - 100000;    // port = 0 - 7999
}
com_uti.logd ("FREQ CODE freq: " + freq + "  hci: " + hci + "  port: " + port);
*/


  private boolean isTunerStarted() {
    return mApi.tuner_state.equalsIgnoreCase(C.TUNER_STATE_START);
  }

  private boolean isTunerStarting() {
    return mApi.tuner_state.equalsIgnoreCase(C.TUNER_STATE_STARTING);
  }

  private boolean isTunerStopped() {
    return mApi.tuner_state.equalsIgnoreCase(C.TUNER_STATE_STOP);
  }

  @SuppressLint("SdCardPath")
  private String setTunerState(String state) {
    com_uti.logd("state: " + state + "; mApi.tuner_state: " + mApi.tuner_state);

    L.getInstance().write("ST::setTunerState(" + state + ")");

    if (state.equals(C.TUNER_STATE_START)) {
      if (!isTunerStarted() && !isTunerStarting()) { // If not already started or starting
        mApi.tuner_state = C.TUNER_STATE_STARTING; // !! Already set

        int ret = com_uti.sys_run("killall libs2d.so 1>/dev/null 2>/dev/null ; /data/data/fm.a2d.sf/lib/libs2d.so " + com_uti.device + "  1>/dev/null 2>/dev/null", true);

        com_uti.logd("daemon kill/start ret: " + ret);

        com_uti.ms_sleep(200); // !!!! MUST have delay here
        com_uti.loge("800 ms delay starting..., fix later");
        //com_uti.ms_sleep(400); // Extra stock HTC One M7 ?
        //                ^ was 600

        mApi.tuner_state = com_uti.s2d_set(C.TUNER_STATE, C.TUNER_STATE_START); // State = Start
        com_uti.logd("start tuner_state: " + mApi.tuner_state);
        pollStart(); // Start polling for changes
      }
    } else if (state.equals(C.TUNER_STATE_STOP)) {
      if (!isTunerStopped()) {
        pollStop(); // Stop polling for changes
        mApi.tuner_state = C.TUNER_STATE_STOPPING;

        com_uti.s2d_set(C.TUNER_STATE, C.TUNER_STATE_STOP);
        com_uti.ms_sleep(100); // Wait 500 ms for s2d daemon to stop, before killing (which may kill network socket or tuner access)
        //                ^ was 500

        mApi.tuner_state = C.TUNER_STATE_STOP;
      }
    }

    mTuner.cb_tuner_key(C.TUNER_STATE, mApi.tuner_state);

    return mApi.tuner_state;
  }

  private void pollStart() {
    if (!mNeedPolling || mIsPolling) {
      return;
    }

    // Start poll timer so volume will be set before FM chip power up, and applied at chip power up.
    mPollingTimer = new Timer("Poll", true);
    mPollingTimer.schedule(new PollTunerHandler(), 2000, 1000); // After 3 (was) seconds every 500 ms
    mIsPolling = true;
  }

  private void pollStop() {
    if (!mNeedPolling || !mIsPolling) {
      return;
    }
    if (mPollingTimer != null) {
      mPollingTimer.cancel(); // Stop poll timer
    }
    mIsPolling = false;
  }

  private class PollTunerHandler extends TimerTask {
    public void run() {
      // Done if state not started
      if (!mApi.isTunerStarted()) {
        return;
      }

      // Frequency:
      String tempFrequencyStr = com_uti.s2d_get(C.TUNER_FREQUENCY);

      int tempFrequencyInt = com_uti.int_get(tempFrequencyStr);

      if (tempFrequencyInt >= 65000) {
        mApi.tuner_freq = tempFrequencyStr;
        mApi.int_tuner_freq = com_uti.int_get(mApi.tuner_freq);

        if (last_poll_freq != mApi.int_tuner_freq) {
          last_poll_freq = mApi.int_tuner_freq;
          mTuner.cb_tuner_key(C.TUNER_FREQUENCY, mApi.tuner_freq); // Inform change
        }
      }

      // RSSI:
      mApi.tuner_rssi = com_uti.s2d_get("tuner_rssi");
      if (last_poll_rssi != (last_poll_rssi = com_uti.int_get(mApi.tuner_rssi)))
        mTuner.cb_tuner_key("tuner_rssi", mApi.tuner_rssi);                        // Inform change

      // MOST:
      mApi.tuner_most = com_uti.s2d_get("tuner_most");
      if (!last_poll_most.equals(mApi.tuner_most))
        mTuner.cb_tuner_key("tuner_most", last_poll_most = mApi.tuner_most);       // Inform change

      // RDS ps:
      /*mApi.tuner_rds_ps = com_uti.s2d_get("tuner_rds_ps");
      if (!last_poll_rds_ps.equals(mApi.tuner_rds_ps))
        mTuner.cb_tuner_key("tuner_rds_ps", last_poll_rds_ps = mApi.tuner_rds_ps); // Inform change

      // RDS rt:
      mApi.tuner_rds_rt = com_uti.s2d_get("tuner_rds_rt                                                                    ").trim();    // !!!! Must have ~ 64 characters due to s2d design.
      if (!last_poll_rds_rt.equals(mApi.tuner_rds_rt))
        mTuner.cb_tuner_key("tuner_rds_rt", last_poll_rds_rt = mApi.tuner_rds_rt); // Inform change

      // RDS pi:
      mApi.tuner_rds_pi = com_uti.s2d_get("tuner_rds_pi");
      int rds_pi = com_uti.int_get(mApi.tuner_rds_pi);
      if (last_poll_rds_pi != rds_pi) {
        last_poll_rds_pi = rds_pi;
        mApi.tuner_rds_picl = com_uti.tnru_rds_picl_get(mApi.tuner_band, rds_pi);
        mTuner.cb_tuner_key("tuner_rds_pi", mApi.tuner_rds_pi);                    // Inform change
      }

      // RDS pt:
      mApi.tuner_rds_pt = com_uti.s2d_get("tuner_rds_pt");
      int rds_pt = com_uti.int_get(mApi.tuner_rds_pt);
      if (last_poll_rds_pt != rds_pt) {
        last_poll_rds_pt = rds_pt;
        mApi.tuner_rds_pt = mApi.tuner_rds_ptyn = com_uti.tnru_rds_ptype_get(mApi.tuner_band, rds_pt);
        mTuner.cb_tuner_key("tuner_rds_pt", mApi.tuner_rds_pt);                    // Inform change
      }*/
    }
  }

}

