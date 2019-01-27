package fm.a2d.sf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import fm.a2d.sf.helper.L;
import fm.a2d.sf.helper.Utils;

// Media button and other remote controls: Lockscreen, AVRCP & future components

// Broadcast Intent handler:
//      android.media.AUDIO_BECOMING_NOISY:     Headphones disconnected
//      android.intent.action.MEDIA_BUTTON:     Headphone Media button pressed
// Declared as receiver in AndroidManifest.xml and passed to (un)registerMediaButtonEventReceiver

public class ServiceMediaReceiver extends BroadcastReceiver {

  /**
   * Need empty constructor since system will start via AndroidManifest.xml, before app ever starts
   */
  public ServiceMediaReceiver() {}

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

      // radio_update above must happen before state checking to see if media button events can be sent to MainService
      /*if (MainActivity.mApi.tuner_state.equalsIgnoreCase("stop")) {
        com_uti.logd("tuner_state == stop, no action");
        return;
      }*/
      // TODO: check above

      L.w(L.T.SERVICE_MEDIA_RECEIVER, "onReceive; action = " + action);
      switch (action) {

        case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
          //MainActivity.mApi.key_set(C.AUDIO_STATE, C.AUDIO_STATE_PAUSE);
          break;

        case Intent.ACTION_NEW_OUTGOING_CALL:
          Utils.sendIntent(context, C.TUNER_STATE, C.TUNER_STATE_STOP);
          break;

        default:
          TelephonyManager tm = (TelephonyManager) context.getSystemService(android.app.Service.TELEPHONY_SERVICE);

          if (tm == null) {
            return;
          }

          switch (tm.getCallState()) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
            case TelephonyManager.CALL_STATE_RINGING:
              Utils.sendIntent(context, C.TUNER_STATE, C.TUNER_STATE_STOP);
              break;

            case TelephonyManager.CALL_STATE_IDLE:
              // TODO: restore play if was playing
              break;
          }
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }
}
