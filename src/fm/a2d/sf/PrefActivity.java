package fm.a2d.sf;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;
import fm.a2d.sf.helper.Utils;

import java.io.File;

/**
 * vlad805 (c) 2019
 */
@SuppressWarnings({"deprecation", "FieldCanBeLocal", "SameParameterValue"})
public class PrefActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

  private String[] mKeysForRestartTuner = {"tuner_most", "audio_sample_rate", "pref_audio_source"};
  private final String[] mPrefsWithListener = {
      "pref_start_native_service",
      "pref_version",
      "pref_kill_native_service",
      "pref_debug_info",
      "pref_force_tuner_state",
      "pref_force_audio_state"
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    PreferenceManager prefMgr = getPreferenceManager();
    prefMgr.setSharedPreferencesName(C.DEFAULT_PREFERENCES);
    prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

    addPreferencesFromResource(R.xml.pref_screen);

    ActionBar ab = getActionBar();

    if (ab != null) {
      ab.setDisplayHomeAsUpEnabled(true);
    }

    try {
      String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
      findPreference("pref_version").setSummary(getString(R.string.preference_summary_version, versionName, C.BUILD));
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }

    showDeviceInfo();

    getSharedPreferences(C.DEFAULT_PREFERENCES, MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
  }

  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

    for (String k : mKeysForRestartTuner) {
      if (key.equals(k)) {
        setResult(RESULT_OK);
        break;
      }
    }
  }

  @Override
  protected void onStop() {
    getSharedPreferences(C.DEFAULT_PREFERENCES, MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);
    super.onStop();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void showDeviceInfo() {
    for (String key : mPrefsWithListener) {
      findPreference(key).setOnPreferenceClickListener(this);
    }
  }

  private int mVersionClicked = 0;
  private Toast mLastToast;
  private final int VER_TOAST_NOTIFY = 5;
  private final int VER_TOAST_DONE = 10;

  @Override
  public boolean onPreferenceClick(Preference preference) {

    switch (preference.getKey()) {
      case "pref_debug_info":
        showDialog("Device info", new Test().toString());
        break;

      case "pref_version":
        if (++mVersionClicked > VER_TOAST_NOTIFY) {
          if (mVersionClicked < VER_TOAST_DONE) {
            showVerToast("Осталось " + (VER_TOAST_DONE - mVersionClicked));
          } else {
            showVerToast("Хватит");
          }
        }
        break;

      case "pref_kill_native_service":
        Utils.sendIntent(this, C.TUNER_KILL, "ok");
        break;

      case "pref_force_tuner_state":
        showItemsDialog("tuner_state", new String[] {"start", "stop"});
        break;

      case "pref_force_audio_state":
        showItemsDialog("audio_state", new String[] {"start", "stop"});
        break;
    }

    return true;
  }

  private void showItemsDialog(final String key, final String[] items) {
    AlertDialog.Builder ab = new AlertDialog.Builder(this);
    ab.setTitle(key)
        .setItems(items, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Toast.makeText(PrefActivity.this, key + " = " + items[which] + " (" + which + ")", Toast.LENGTH_LONG).show();
            Utils.sendIntent(PrefActivity.this, key, items[which]);
          }
        })
        .setPositiveButton(android.R.string.cancel, null)
        .create()
        .show();
  }

  private void showVerToast(String text) {
    if (mLastToast != null) {
      mLastToast.cancel();
      mLastToast = null;
    }
    mLastToast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
    mLastToast.show();
  }

  private void showDialog(String title, CharSequence text) {
    AlertDialog.Builder ab = new AlertDialog.Builder(this);
    ab.setTitle(title).setMessage(text).setPositiveButton(android.R.string.cancel, null).create().show();
  }


  public class Test {

    private StringBuilder sb;

    private Test() {
      sb = new StringBuilder();
    }

    @Override
    public String toString() {
      addRow("Build", Build.BOARD);
      addRow("Brand", Build.BRAND);
      addRow("Device", Build.DEVICE);
      addRow("Hardware", Build.HARDWARE);
      addRow("ID", Build.ID);
      addRow("Manufacturer", Build.MANUFACTURER);
      addRow("Model", Build.MODEL);
      addRow("Product", Build.PRODUCT);
      addRow("SDK", String.valueOf(Build.VERSION.SDK_INT));
      addRow("su?", com_uti.sys_run("echo 1", true) >= 0);
      addRowFile("/dev/radio0");
      addRowFile("/dev/fmradio");
      addRowFile("/dev/ttyHS99");
      addRowFile("/dev/ttyHS0");
      addRowFile("/system/lib/modules/radio-iris-transport.ko");
      addRow("Native data", com_uti.s2d_get("test_data"));

      return sb.toString();
    }

    private void addRowFile(String path) {
      addRow(path, new File(path).exists());
    }

    private void addRow(String name, String value) {
      sb.append(name).append(" = ").append(value).append("\n");
    }

    private void addRow(String name, boolean value) {
      addRow(name, String.valueOf(value));
    }

  }

}
