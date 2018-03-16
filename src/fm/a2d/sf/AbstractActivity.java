package fm.a2d.sf;

import android.view.View;
import android.os.Bundle;
import android.app.Dialog;
import android.content.Intent;

public interface AbstractActivity {
  public boolean setState(String state);
  public void onReceivedUpdates(Intent intent);
  public void onClickView(View v);
}