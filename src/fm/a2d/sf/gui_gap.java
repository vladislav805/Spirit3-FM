package fm.a2d.sf;

import android.view.View;
import android.os.Bundle;
import android.app.Dialog;
import android.content.Intent;

public interface gui_gap {
  public boolean gap_state_set(String state);
  public Dialog gap_dialog_create(int id, Bundle args);
  public void onReceivedUpdates(Intent intent);
  public void gap_gui_clicked(View v);
}