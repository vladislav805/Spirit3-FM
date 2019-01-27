package fm.a2d.sf.helper;

import android.util.Log;

/**
 * vlad805 (c) 2018
 */
public class L {

  public enum T {
    ACTIVITY,
    SERVICE_MAIN,
    SERVICE_AUDIO,
    SERVICE_TUNER,
    SERVICE_MEDIA_RECEIVER,
    GUI,
    RECORDER,
    UTILS,
    API,
    FM;
  }

  public static void w(T t, String s) {
    Log.i(t.name(), s);
  }
}
