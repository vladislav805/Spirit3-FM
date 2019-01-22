package fm.a2d.sf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import fm.a2d.sf.helper.Utils;
import fm.a2d.sf.view.PreferenceView;

import java.io.File;

/**
 * vlad805 (c) 2018
 */
public class SettingsActivity extends Activity implements PreferenceView.OnChangeListener {

  private LinearLayout mRoot;

  private boolean mNeedSettings = true;

  private static final int PREF_NOTIFICATION_TYPE = 0xfca0;
  private static final int PREF_WRITE_LOGS = 0x10c;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    Intent i = getIntent();

    if (i != null) {
      if (i.hasExtra(C.NO_PREFERENCES) && i.getBooleanExtra(C.NO_PREFERENCES, false)) {
        mNeedSettings = false;
      }
    }

    mRoot = (LinearLayout) findViewById(R.id.pref_root);
    init();
  }

  private void init() {

    if (mNeedSettings) {
     /* mRoot.addView(new PreferenceView(this)
          .setInfo(PREF_NOTIFICATION_TYPE, getString(R.string.pref_notification_custom), Utils.getPrefInt(this, C.NOTIFICATION_TYPE) != 0, this)
      );*/

      /*mRoot.addView(new PreferenceView(this)
          .setInfo(PREF_WRITE_LOGS, getString(R.string.pref_write_logs), com_uti.prefs_get(this, C.WRITE_LOGS, C.WRITE_LOGS_YES) != 0, this)
      );*/
    }

    mRoot.addView(new TestView(this));
  }


  @Override
  public void onChange(int pref, boolean value, PreferenceView v) {
    switch (pref) {
      case PREF_NOTIFICATION_TYPE:
        Utils.setPrefInt(this, C.NOTIFICATION_TYPE, value ? C.NOTIFICATION_TYPE_CUSTOM : C.NOTIFICATION_TYPE_CLASSIC);
        new com_api(this).key_set(C.NOTIFICATION_TYPE, "update");
        break;
    }
  }

  public class TestView extends TextView {

    private StringBuilder sb;

    public TestView(Context context) {
      super(context);

      sb = new StringBuilder();
      setText(makeString());
      setPadding(40, 0, 40, 0);
    }

    private String makeString() {


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
      addRowFile("/system/bin/su");
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
      sb.append(name).append(": ").append(value).append("\n");
    }

    private void addRow(String name, boolean value) {
      addRow(name, String.valueOf(value));
    }

  }
}