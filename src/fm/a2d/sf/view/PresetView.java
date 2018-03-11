package fm.a2d.sf.view;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Button;
import fm.a2d.sf.R;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings("UnusedReturnValue")
public class PresetView extends Button {

  private int mIndex;
  private String mFrequency;

  public PresetView(Context context) {
    super(context);

    setLayoutParams(new ViewGroup.LayoutParams((int) context.getResources().getDimension(R.dimen.preset_item_width), ViewGroup.LayoutParams.MATCH_PARENT));

    setHapticFeedbackEnabled(true);
    setTextSize(context.getResources().getDimension(R.dimen.preset_item_text_size));
  }


  public PresetView populate(int index, String frequency) {
    mFrequency = frequency;
    mIndex = index;

    setText(isEmpty() ? "+" : mFrequency);

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

  public boolean isEmpty() {
    return mFrequency == null || mFrequency.isEmpty();
  }

}