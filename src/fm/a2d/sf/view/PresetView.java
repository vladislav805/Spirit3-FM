package fm.a2d.sf.view;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings("UnusedReturnValue")
public class PresetView extends Button {

  private Context mContext;

  private float mDensity;

  private int mIndex;
  private String mFrequency;

  public PresetView(Context context) {
    super(context);

    mContext = context;

    mDensity = context.getResources().getDisplayMetrics().density;

    setLayoutParams(new ViewGroup.LayoutParams((int) (80 * mDensity), ViewGroup.LayoutParams.MATCH_PARENT));

    setHapticFeedbackEnabled(true);
  }


  public PresetView populate(int index, String frequency) {
    mFrequency = frequency;
    mIndex = index;

    setText(mFrequency.isEmpty() ? "+" : mFrequency);

    return this;
  }

  public PresetView populate(String frequency) {
    return populate(mIndex, frequency);
  }

  public PresetView setListeners(OnClickListener onClick, OnLongClickListener onLongClick) {
    setOnClickListener(onClick);
    setOnLongClickListener(onLongClick);
    return this;
  }

  public int getIndex() {
    return mIndex;
  }

  public String getFrequency() {
    return mFrequency;
  }

}