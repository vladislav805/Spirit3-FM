package fm.a2d.sf.view;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import fm.a2d.sf.R;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings("UnusedReturnValue")
public class PresetView extends Button {

  private int mIndex;
  private String mFrequency;

  private static final int MENU_REMOVE = 0x1e0e;
  private static final int MENU_RENAME = 0x1eae;
  private static final int MENU_CREATE = 0x1add;

  public PresetView(Context context) {
    super(context);

    setMaxLines(1);
    setLayoutParams(new ViewGroup.LayoutParams((int) context.getResources().getDimension(R.dimen.preset_item_width), ViewGroup.LayoutParams.MATCH_PARENT));

    setHapticFeedbackEnabled(true);
    setTextSize(COMPLEX_UNIT_DIP, context.getResources().getDimension(R.dimen.preset_item_text_size));
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
    setOnLongClickListener(new OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        onMenuOpen();
        return true;
      }
    });
    return this;
  }

  private void onMenuOpen() {
    Context c = getContext();
    PopupMenu popupMenu = new PopupMenu(c, this);
    if (isEmpty()) {
      popupMenu.getMenu().add(1, MENU_CREATE, 1, c.getString(R.string.popup_preset_create));
    } else {
      popupMenu.getMenu().add(1, MENU_RENAME, 1, c.getString(R.string.popup_preset_rename));
      popupMenu.getMenu().add(1, MENU_REMOVE, 2, c.getString(R.string.popup_preset_remove));
    }
    popupMenu.show();
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