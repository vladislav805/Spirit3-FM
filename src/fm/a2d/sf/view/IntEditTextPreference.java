package fm.a2d.sf.view;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.View;

/**
 * vlad805 (c) 2019
 */
public class IntEditTextPreference extends EditTextPreference {

  public IntEditTextPreference(Context context) {
    super(context);
    init();
  }

  public IntEditTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public IntEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    getEditText().setSelectAllOnFocus(true);
  }

  @Override
  protected String getPersistedString(String defaultReturnValue) {
    return String.valueOf(getPersistedInt(-1));
  }

  @Override
  protected boolean persistString(String value) {
    return persistInt(Integer.valueOf(value));
  }

  @Override
  protected void onAttachedToActivity() {
    super.onAttachedToActivity();
    setSummary(getPersistedString("def"));
  }

  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    setSummary(getPersistedString("def"));
  }
}