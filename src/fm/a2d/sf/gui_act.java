package fm.a2d.sf;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.media.AudioManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

// GUI Activity:

public class gui_act extends Activity {

  public static com_api m_com_api = null;
  private static int stat_creates = 1;
  private static BroadcastReceiver mBroadcastListener = null;

  private gui_gap m_gui = null;
  private Context mContext = null;

  // Lifecycle:

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    com_uti.logd("stat_creates: " + stat_creates++);
    mContext = this;

    if (m_com_api == null) {
      m_com_api = new com_api(mContext);
    }

    // Must be done from an Activity
    setVolumeControlStream(AudioManager.STREAM_MUSIC);
    gui_start();
    initBroadcastListener();
  }

  // Create        Start,Resume       Pause,Resume        Pause,Stop,Restart       Start,Resume

  // Launch:   Create      Start       Resume
  // Home:                                         Pause       Stop
  // Return:   Restart     Start       Resume
  // Back:                                         Pause       Stop        Destroy


  // !! Resume can happen with the FM power off, so try not to do things needing power on (this was about onResume)
  // Restart comes between Stop and Start or when returning to the app (this was about onRestart)

  @Override
  public void onDestroy() {
    // One of these caused crashes:
    stopBroadcastListener();
    gui_stop();

    // super.onDestroy dismisses any dialogs or cursors the activity was managing. If the logic in onDestroy has something to do with these things, then order may matter.
    super.onDestroy();
  }

  private void gui_start() {
    try {
      m_gui = new gui_gui(mContext, m_com_api); // Instantiate UI
      if (!m_gui.gap_state_set("start")) { // Start UI. If error...
        com_uti.loge("gui_start error");
        m_gui = null;
      } else {
        com_uti.logd("gui_start OK");
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void gui_stop() {
    try {
      if (m_gui == null)
        com_uti.loge("already stopped");
      else if (!m_gui.gap_state_set("stop"))                          // Stop UI. If error...
        com_uti.loge("gui_stop error");
      else
        com_uti.logd("gui_stop OK");
      m_gui = null;
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }


  private void initBroadcastListener() {
    if (mBroadcastListener == null) {
      mBroadcastListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          String action = intent.getAction();

          com_uti.logx("intent: " + intent + "  action: " + action);

          if (!action.equalsIgnoreCase("fm.a2d.sf.result.get")) {
            return;
          }

          if (m_com_api != null && m_gui != null) {
            m_com_api.radio_update(intent);
            m_gui.onReceivedUpdates(intent);
          }
        }
      };

      IntentFilter intFilter = new IntentFilter();
      intFilter.addAction("fm.a2d.sf.result.get"); // Can add more actions if needed
      intFilter.addCategory(Intent.CATEGORY_DEFAULT);
      Intent last_sticky_state_intent = null;
      if (mContext != null) {
        // No permission, no handler scheduler thread.
        last_sticky_state_intent = mContext.registerReceiver(mBroadcastListener, intFilter, null, null);
      }

      if (last_sticky_state_intent != null) {
        com_uti.logd("bcast intent last_sticky_state_intent: " + last_sticky_state_intent);
        //mBroadcastListener.onReceive (mContext, last_sticky_state_intent);  // Like a resend of last audio status update
      }
    }
  }

  private void stopBroadcastListener() {
    if (mBroadcastListener != null) {                                        // Remove the State listener
      if (mContext != null)
        mContext.unregisterReceiver(mBroadcastListener);
      mBroadcastListener = null;
    }
  }

  public void gap_gui_clicked(View v) {
    m_gui.gap_gui_clicked(v);
  }

}