package fm.a2d.sf;

import android.content.Intent;
import android.view.View;

public interface AbstractActivity {
  public boolean setState(String state);
  public void onReceivedUpdates(Intent intent);
  public void onClickView(View v);
}