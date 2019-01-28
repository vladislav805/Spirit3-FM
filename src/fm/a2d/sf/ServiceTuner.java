package fm.a2d.sf;

import android.annotation.SuppressLint;
import android.content.Context;
import fm.a2d.sf.helper.L;
import fm.a2d.sf.helper.Utils;

import java.util.Timer;
import java.util.TimerTask;

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

  private static void log(String s) {
    L.w(L.T.SERVICE_TUNER, s);
  }

  // Context & Tuner API callback constructor
  public ServiceTuner(Context c, MainService cb_tnr, com_api svc_com_api) {
    log("context=" + c + "; cb_tnr=" + cb_tnr);
    mTuner = cb_tnr;
    mApi = svc_com_api;
  }

  /**
   * Возврашает текущую частоту.
   * Если включен тюнер - делаетя запрос к тюнеру, иначе
   * воозвращается последнее кэшированное значение
   * @return KHz
   */
  public int getCurrentFrequency() {
    if (mApi.isTunerStarted()) {
      mApi.int_tuner_freq = Utils.parseInt(com_uti.s2d_get(C.TUNER_FREQUENCY));
    }
    return mApi.int_tuner_freq;
  }

  /**
   * Все команды для тюнера, в native-часть
   * @param key команда
   * @param val значение
   */
  public void setTunerValue(String key, String val) {
    if (key == null) {
      return;
    }

    log("setTunerValue(" + key + ", " + val + "); isTunerStart = " + mApi.isTunerStarted());

    switch (key) {
      case C.TUNER_STATE:
        setTunerState(val);
        break;

      case C.RADIO_NOP:
        com_uti.s2d_set(C.RADIO_NOP, "start");
        break;
    }

    if (mApi.isTunerStarted()) {
      com_uti.s2d_set(key, val);
      return;
    }

    switch (key) {
      case C.TUNER_FREQUENCY:
        mApi.int_tuner_freq = Utils.parseInt(val);
        break;

      case "tuner_stereo":
        mApi.tuner_stereo = val;
        break;

      case "tuner_thresh":
        mApi.tuner_thresh = val;
        break;

      case C.TUNER_SCAN_STATE:
        mApi.tuner_scan_state = val;
        break;
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
  private void setTunerState(String state) {
    log("setTunerState(" + state + ") with tuner_state=" + mApi.tuner_state);

    switch (state) {
      case C.TUNER_STATE_START:
        if (!isTunerStarted() && !isTunerStarting()) { // If not already started or starting
          start();
        }
        break;

      case C.TUNER_STATE_STOP:
        if (!isTunerStopped()) {
          stop();
        }
        break;
    }

    mTuner.onUpdateTunerKey(C.TUNER_STATE, mApi.tuner_state);
  }

  private void start() {
    mApi.tuner_state = C.TUNER_STATE_STARTING; // !! Already set

    int ret = com_uti.sys_run("killall libs2d.so 1>/dev/null 2>/dev/null ; /data/data/fm.a2d.sf/lib/libs2d.so " + com_uti.device + " 1>/dev/null 2>/dev/null", true);

    log("daemon (kill) and start; result = " + ret);

    com_uti.ms_sleep(200); // !!!! MUST have delay here
    log("800 ms delay starting..., fix later");

    mApi.tuner_state = com_uti.s2d_set(C.TUNER_STATE, C.TUNER_STATE_START); // State = Start
    log("start tuner_state: " + mApi.tuner_state);
    pollStart(); // Start polling for changes
  }

  private void stop() {
    pollStop(); // Stop polling for changes
    mApi.tuner_state = C.TUNER_STATE_STOPPING;

    com_uti.s2d_set(C.TUNER_STATE, C.TUNER_STATE_STOP);
    com_uti.ms_sleep(400);
    //                ^ was 500
    // Wait 500 ms for s2d daemon to stop, before killing (which may kill network socket or tuner access)



    mApi.tuner_state = C.TUNER_STATE_STOP;
  }

  public int killNative() {
    int ret = com_uti.sys_run("killall libs2d.so 1>/dev/null 2>/dev/null", true);

    log("killing native server; ret = " + ret);

    return ret;
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
      int tuner_freq = Utils.parseInt(com_uti.s2d_get(C.TUNER_FREQUENCY));

      // RSSI:
      mApi.tuner_rssi = Utils.parseInt(com_uti.s2d_get(C.TUNER_RSSI));

      // MOST:
      mApi.tuner_most = com_uti.s2d_get("tuner_most");

      if (tuner_freq >= 65000) {
        mApi.int_tuner_freq = tuner_freq;

        if (last_poll_freq != mApi.int_tuner_freq) {
          last_poll_freq = mApi.int_tuner_freq;
          mTuner.onUpdateTunerKey(C.TUNER_FREQUENCY, String.valueOf(tuner_freq)); // Inform change
        }
      }


      if (last_poll_rssi != mApi.tuner_rssi) {
        last_poll_rssi = mApi.tuner_rssi;
        mTuner.onUpdateTunerKey(C.TUNER_RSSI, String.valueOf(mApi.tuner_rssi)); // Inform change
      }


      if (!last_poll_most.equals(mApi.tuner_most)) {
        mTuner.onUpdateTunerKey("tuner_most", last_poll_most = mApi.tuner_most); // Inform change
      }
    }
  }
}
