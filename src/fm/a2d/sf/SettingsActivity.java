package fm.a2d.sf;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import fm.a2d.sf.view.PreferenceView;

/**
 * vlad805 (c) 2018
 */
public class SettingsActivity extends Activity implements PreferenceView.OnChangeListener {

  private LinearLayout mRoot;

  private static final int PREF_NOTIFICATION_TYPE = 0xfca0;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    mRoot = (LinearLayout) findViewById(R.id.pref_root);
    init();
  }

  private void init() {
    mRoot.addView(new PreferenceView(this)
        .setInfo(PREF_NOTIFICATION_TYPE, "New custom notification", com_uti.prefs_get(this, C.NOTIFICATION_TYPE, C.NOTIFICATION_TYPE_CLASSIC) != 0, this)
    );
  }


  @Override
  public void onChange(int pref, boolean value, PreferenceView v) {
    switch (pref) {
      case PREF_NOTIFICATION_TYPE:
        com_uti.prefs_set(this, C.NOTIFICATION_TYPE, value ? C.NOTIFICATION_TYPE_CUSTOM : C.NOTIFICATION_TYPE_CLASSIC);
        new com_api(this).key_set(C.NOTIFICATION_TYPE, "update");
        break;
    }
  }
}