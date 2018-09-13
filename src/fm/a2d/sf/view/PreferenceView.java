package fm.a2d.sf.view;

import android.content.Context;
import android.view.View;
import android.widget.*;
import fm.a2d.sf.R;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings("UnusedReturnValue")
public class PreferenceView extends FrameLayout {

  //private OnChangeListener mOnChangeListener;
  private int mPrefId;
  private TextView mLabel;
  private CheckBox mCheck;

  public interface OnChangeListener {
    public void onChange(int pref, boolean value, PreferenceView v);
  }

  public PreferenceView(Context context) {
    super(context);

    View view = inflate(context, R.layout.item_preference, null);

    setClickable(true);
    setFocusable(true);
    setBackgroundColor(android.R.attr.selectableItemBackground);

    addView(view);

    mLabel = (TextView) view.findViewById(R.id.pref_text);
    mCheck = (CheckBox) view.findViewById(R.id.pref_check);

    mLabel.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        mCheck.toggle();
      }
    });
  }

  public PreferenceView setInfo(int prefId, String title, boolean state, final OnChangeListener listener) {
    mPrefId = prefId;
    mLabel.setText(title);
    mCheck.setChecked(state);
    mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (listener != null) {
          listener.onChange(getPrefId(), isChecked, PreferenceView.this);
        }
      }
    });
    return this;
  }

  public int getPrefId() {
    return mPrefId;
  }
}