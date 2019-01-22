package fm.a2d.sf.helper;

import android.content.Context;
import android.content.SharedPreferences;
import fm.a2d.sf.C;

import java.util.Locale;

/**
 * vlad805 (c) 2019
 */
public class Utils {

  private static SharedPreferences getReadPrefs(Context c) {
    return c.getSharedPreferences(C.DEFAULT_PREFERENCES, Context.MODE_MULTI_PROCESS);
  }

  private static SharedPreferences.Editor getWritePrefs(Context c) {
    return getReadPrefs(c).edit();
  }

  public static boolean hasPref(Context c, String key) {
    return getReadPrefs(c).contains(key);
  }

  public static String getPrefString(Context c, String key) {
    return getPrefString(c, key, null);
  }

  public static String getPrefString(Context c, String key, String def) {
    return getReadPrefs(c).getString(key, def);
  }

  public static void setPrefString(Context c, String key, String value) {
    getWritePrefs(c).putString(key, value).apply();
  }

  public static int getPrefInt(Context c, String key) {
    return getPrefInt(c, key, Integer.MAX_VALUE);
  }

  public static int getPrefInt(Context c, String key, int def) {
    return getReadPrefs(c).getInt(key, def);
  }

  public static void setPrefInt(Context c, String key, int value) {
    getWritePrefs(c).putInt(key, value).apply();
  }

  public static int parseInt(String v) {
    try {
      return Integer.valueOf(v);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static String getTimeStringBySeconds(int number) {
    double second = Math.floor(number % 60);
    double minute = Math.floor(number / 60f % 60f);
    double hour = Math.floor(number / 60f / 60f % 60f);

    return (hour > 0 ? hour + ":" : "") + String.format(Locale.ENGLISH, "%02.0f:%02.0f", minute, second);
  }

}
