package fm.a2d.sf;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;

// Media button and other remote controls: Lockscreen, AVRCP & future components

// Broadcast Intent handler:
//      android.media.AUDIO_BECOMING_NOISY:     Headphones disconnected
//      android.intent.action.MEDIA_BUTTON:     Headphone Media button pressed
// Declared as receiver in AndroidManifest.xml and passed to (un)registerMediaButtonEventReceiver

public class ServiceMediaReceiver extends BroadcastReceiver {

  /**
   * Need empty constructor since system will start via AndroidManifest.xml, before app ever starts
   */
  public ServiceMediaReceiver() {
  }


  /**
   * Media buttons
   */
  @Override
  public void onReceive(Context context, Intent intent) {
    try {


      String action = intent.getAction();
      if (action == null) {
        return;
      }
//      if (action.equalsIgnoreCase ("fm.a2d.sf.result.get")) {
//        radio_update (context, intent);
//        return;
//      }

      // radio_update above must happen before state checking to see if media button events can be sent to svc_svc
      /*if (gui_act.m_com_api.tuner_state.equalsIgnoreCase("stop")) {
        com_uti.logd("tuner_state == stop, no action");
        return;
      }*/
      // TODO: check above

      switch (action) {

        case android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY:
          com_uti.logd("audio noisy");
          //gui_act.m_com_api.key_set("audio_state", "pause");
          break;

        case Intent.ACTION_NEW_OUTGOING_CALL:
          stopRadio(context);

        case Intent.ACTION_MEDIA_BUTTON:
          handle_key_event(context, (KeyEvent) intent.getExtras ().get (Intent.EXTRA_KEY_EVENT));
          break;

        default:
          TelephonyManager tm = (TelephonyManager) context.getSystemService(Service.TELEPHONY_SERVICE);
          switch (tm.getCallState()) {

            case TelephonyManager.CALL_STATE_RINGING:
              stopRadio(context);
              break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
              stopRadio(context);
              break;

            case TelephonyManager.CALL_STATE_IDLE:

              break;

          }

      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void stopRadio(Context ctx) {
    send(ctx, "tuner_state", "Stop");
  }

  private void send(Context ctx, String key, String value) {
    Intent intent = new Intent(svc_svc.ACTION_SET);
    intent.setClass(ctx, svc_svc.class);
    intent.putExtra(key, value);
    ctx.startService(intent);
  }

  private void handle_key_event(Context context, KeyEvent key_event) {
  }

}
