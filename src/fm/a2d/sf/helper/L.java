package fm.a2d.sf.helper;

import android.os.Environment;
import fm.a2d.sf.C;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/**
 * vlad805 (c) 2018
 */
public class L {

  private static final L INSTANCE = new L();

  private OutputStream mWriter;

  private L() {
    if (C.DEBUG) {
      init();
    }
  }

  public static L getInstance() {
    return INSTANCE;
  }

  private void init() {
    if (mWriter == null) {
      File f = new File(String.format(Locale.ENGLISH, "%s/fmlog%d.txt", Environment.getExternalStorageDirectory(), System.currentTimeMillis()));
      try {
        if (f.createNewFile()) {
          mWriter = new FileOutputStream(f);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void killWriter() {
    if (mWriter != null) {
      try {
        mWriter.close();
      } catch (IOException ignore) {}
    }
  }

  public L write(String w) {
    if (C.DEBUG) {
      try {
        mWriter.write(w.getBytes());
        mWriter.write("\n".getBytes());
        mWriter.flush();
      } catch (IOException ignore) {}
    }
    return this;
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    killWriter();
  }
}
