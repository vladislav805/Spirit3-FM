package fm.a2d.sf;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * vlad805 (c) 2019
 */
public class MyApp extends Application {

  /**
   * Called when the application is starting, before any activity, service, or receiver objects (excluding content providers) have been created.
   */
  public void onCreate() {
    super.onCreate();

    initLogCatWriter();
  }

  private void initLogCatWriter() {
    if (C.DEBUG && isExternalStorageWritable()) {

      File appDirectory = new File(Environment.getExternalStorageDirectory() + "/Spirit3/");
      File logDirectory = new File(appDirectory + "/log");
      File logFile = new File(logDirectory, "logcat" + System.currentTimeMillis() + ".txt");

      boolean mkdirs = true;

      // create app folder
      if (!appDirectory.exists()) {
        mkdirs = appDirectory.mkdir();
      }

      // create log folder
      if (!logDirectory.exists()) {
        mkdirs = mkdirs || logDirectory.mkdir();
      }

      if (!mkdirs) {
        return;
      }

      // clear the previous logcat and then write the new one to the file
      try {
        Runtime.getRuntime().exec("logcat -c");
        Runtime.getRuntime().exec("logcat -f " + logFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /* Checks if external storage is available for read and write */
  private boolean isExternalStorageWritable() {
    return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
  }

}
