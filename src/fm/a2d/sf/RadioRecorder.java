package fm.a2d.sf;

import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.Thread.State;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RadioRecorder {
  private static String mDirectory;
  private int mChannels;
  private com_api mApi;
  private boolean mNeedFinish = false;

  private RandomAccessFile mRandomAccessFile = null;
  private BufferedOutputStream mBufferOutStream = null;
  private FileOutputStream mFileOutStream = null;
  private File mRecordFile = null;

  @SuppressWarnings("UnusedAssignment")
  private int mSampleRate = 44100;

  private int mRecordDataSize = 0;
  private byte[] mRecordBufferData = new byte[1048576];
  private int mRecordBufferHead = 0;
  private int mRecordBufferTail = 0;

  private boolean mRecordWriteThreadActive = false;
  private Thread mRecordThread = null;

  private final Runnable mRecordWriteRunnable = new Runnable() {

    public void run() {
      com_uti.logd("mRecordWriteRunnable run()");
      int cur_buf_tail;
      if (mRecordBufferData == null) {
        mRecordBufferData = new byte[1048576];
      }
      if (mRecordBufferData == null) {
        mRecordWriteThreadActive = false;
      }
      while (mRecordWriteThreadActive) {
        try {
          cur_buf_tail = mRecordBufferTail;
          int len = 0;
          if (cur_buf_tail == mRecordBufferHead) {
            Thread.sleep(1000);
          } else {
            if (cur_buf_tail > mRecordBufferHead) {
              len = cur_buf_tail - mRecordBufferHead;
            } else if (cur_buf_tail < mRecordBufferHead) {
              len = 1048576 - mRecordBufferHead;
            }
            if (len == 0) {
              com_uti.logd("len = 0 mRecordWriteRunnable run() len: " + len + "  mRecordBufferHead: " + mRecordBufferHead + "  cur_buf_tail: " + cur_buf_tail + "  rec_buf_size: " + 1048576);
            } else if (len < 0) {
              com_uti.loge("len < 0 mRecordWriteRunnable run() len: " + len + "  mRecordBufferHead: " + mRecordBufferHead + "  cur_buf_tail: " + cur_buf_tail + "  rec_buf_size: " + 1048576);
            } else {
              com_uti.logd("len > 0 mRecordWriteRunnable run() len: " + len + "  mRecordBufferHead: " + mRecordBufferHead + "  cur_buf_tail: " + cur_buf_tail + "  rec_buf_size: " + 1048576);
              if (mRandomAccessFile == null) {
                mRandomAccessFile = new RandomAccessFile(mRecordFile, "rw");
              }
              if (mBufferOutStream != null) {
                mBufferOutStream.write(mRecordBufferData, mRecordBufferHead, len);
                mRecordDataSize += len;
              }
              com_uti.logd("Wrote len: " + len + "  to mRecordBufferHead: " + mRecordBufferHead);
              mRecordBufferHead += len;
              if (mRecordBufferHead < 0 || mRecordBufferHead >= 1048576) {
                mRecordBufferHead = 0;
              }
              com_uti.logd("new mRecordBufferHead: " + mRecordBufferHead);
            }
          }
        } catch (InterruptedException e) {
          com_uti.logd("mRecordWriteRunnable run() throwable InterruptedException");
        } catch (Throwable e2) {
          com_uti.loge("mRecordWriteRunnable run() throwable: " + e2);
          e2.printStackTrace();
        }
      }
      com_uti.logd("mRecordWriteRunnable run() done");
      if (mNeedFinish) {
        mNeedFinish = false;
        audio_record_finish();
      }
    }
  };

  @SuppressWarnings("WeakerAccess")
  public RadioRecorder(Context context, int sampleRate, int channels, com_api api) {
    com_uti.logd("constructor context: " + context + "  sampleRate: " + sampleRate + "  channels: " + channels + "  api: " + api);
    mSampleRate = sampleRate;
    mChannels = channels;
    mApi = api;
  }

  public void write(byte[] buffer, int length) {
    if (!mRecordWriteThreadActive) {
      return;
    }

    if (mRecordDataSize < 0 && (mRecordDataSize + length) + 36 > -4) {
      com_uti.loge("!!! Max mRecordDataSize: " + mRecordDataSize + "  length: " + length);
      mApi.audio_record_state = "Stop";
      mNeedFinish = true;
    }
    if (!mApi.audio_record_state.equals("Stop")) {
      writeBuffer(buffer, length);
    }
  }

  public void setState(String state) {
    if (state.equalsIgnoreCase("Toggle")) {
      if (mApi.audio_record_state.equals("Stop")) {
        state = "Start";
      } else {
        state = "Stop";
      }
    }
    if (state.equals("Stop")) {
      stop();
    } else if (state.equals("Start")) {
      start();
    }
  }

  private void audio_record_finish() {
    try {
      if (mRecordDataSize != 0) {
        writeFinal();
      }
      mRecordDataSize = 0;
      if (mBufferOutStream != null) {
        mBufferOutStream.close();
        mBufferOutStream = null;
      }
      if (mFileOutStream != null) {
        mFileOutStream.close();
        mFileOutStream = null;
      }
    } catch (Throwable e) {
      com_uti.loge("throwable: " + e);
      e.printStackTrace();
    }
    mRandomAccessFile = null;
  }

  public void stop() {
    com_uti.logd("audio_record_state: " + mApi.audio_record_state);
    mRecordWriteThreadActive = false;
    if (mRecordThread != null) {
      mRecordThread.interrupt();
    }
    mApi.audio_record_state = "Stop";
  }

  public boolean start() {
    if (!mApi.audio_record_state.equals("Stop")) {
      return false;
    }

    mDirectory = "/Music/FM" + File.separator + new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    mRecordFile = null;

    File recordDirectory = new File(com_uti.getExternalStorageDirectory().getPath() + mDirectory);

    if (!(recordDirectory.exists() || recordDirectory.mkdirs())) {
      com_uti.loge("record_start Directory " + recordDirectory + " creation error");
    }

    if (!recordDirectory.canWrite()) {
      recordDirectory = com_uti.getExternalStorageDirectory();
    }
    if (!recordDirectory.canWrite()) {
      com_uti.logd("cannot write");
      return false;
    }

    String filename = getFilename();
    try {
      mRecordFile = new File(recordDirectory, filename);
      com_uti.logd("CREATE; dir = " + recordDirectory + "; filename = " + filename);
      //noinspection ResultOfMethodCallIgnored
      mRecordFile.createNewFile();
      try {
        mFileOutStream = new FileOutputStream(mRecordFile, true);
        mBufferOutStream = new BufferedOutputStream(mFileOutStream, 131072);

        mRecordDataSize = 0;

        if (!writeWavHeader()) {
          audio_record_finish();
          return false;
        }

        if (!mRecordWriteThreadActive) {
          mRecordThread = new Thread(mRecordWriteRunnable, "rec_write");
          com_uti.logd("mRecordThread: " + mRecordThread);

          if (mRecordThread == null) {
            com_uti.loge("mRecordThread == null");
            audio_record_finish();
            return false;
          }

          mRecordWriteThreadActive = true;

          try {
            if (mRecordThread.getState() == State.NEW || mRecordThread.getState() == State.TERMINATED) {
              mRecordThread.start();
            }
          } catch (Throwable e) {
            com_uti.loge("Throwable: " + e);
            e.printStackTrace();
            audio_record_finish();
            mApi.audio_record_state = "Stop";
            return false;
          }
        }
        mApi.audio_record_state = "Start";
        mNeedFinish = true;
        return true;


      } catch (Throwable e2) {
        e2.printStackTrace();
        return false;
      }
    } catch (Throwable e22) {
      com_uti.loge("record_start unable to create file: " + e22);
      e22.printStackTrace();
      return false;
    }

  }

  private void writeBuffer(byte[] buffer, int length) {
    int have1_len = 0;
    int have2_len = 0;
    int cur_buf_head = mRecordBufferHead;

    if (mRecordBufferData == null || buffer == null || length <= 0 || buffer.length < length) {
      //noinspection ImplicitArrayToString
      com_uti.loge("!!! start length: " + length + "  buffer: " + buffer + "  mRecordBufferData: " + mRecordBufferData);
      return;
    }

    if (cur_buf_head < 0 || cur_buf_head > 1048576) {
      com_uti.loge("!!!  cur_buf_head: " + cur_buf_head);
      cur_buf_head = 0;
      mRecordBufferHead = 0;
    }
    if (mRecordBufferTail < 0 || mRecordBufferTail > 1048576) {
      com_uti.loge("!!!  mRecordBufferTail: " + mRecordBufferTail);
      mRecordBufferTail = 0;
    }
    if (mRecordBufferTail == cur_buf_head) {
      have1_len = 0;
      have2_len = 0;
    } else if (mRecordBufferTail > cur_buf_head) {
      have1_len = mRecordBufferTail - cur_buf_head;
      have2_len = 0;
    } else if (mRecordBufferTail < cur_buf_head) {
      have1_len = 1048576 - cur_buf_head;
      have2_len = mRecordBufferTail;
    }

    if (have1_len < 0) {
      com_uti.loge("!!! Negative have length: " + length + "  have1_len: " + have1_len + "  have2_len: " + have2_len + "  cur_buf_head: " + cur_buf_head + "  mRecordBufferTail: " + mRecordBufferTail + "  rec_buf_size: " + 1048576);
    } else if (have1_len + have2_len + length >= 1048576) {
      com_uti.loge("!!! Attempt to exceed record buf length: " + length + "  have1_len: " + have1_len + "  have2_len: " + have2_len + "  cur_buf_head: " + cur_buf_head + "  mRecordBufferTail: " + mRecordBufferTail + "  rec_buf_size: " + 1048576);
      if (mRecordThread != null) {
        mRecordThread.interrupt();
      }
    } else {
      int writ1_len;
      int writ2_len;

      if (mRecordBufferTail + length <= 1048576) {
        writ1_len = length;
        writ2_len = 0;
      } else {
        writ1_len = 1048576 - mRecordBufferTail;
        writ2_len = length - writ1_len;
      }

      if (writ1_len > 0) {
        System.arraycopy(buffer, 0, mRecordBufferData, mRecordBufferTail, writ1_len);

        com_uti.logd("Wrote writ1_len: " + writ1_len + "  to mRecordBufferTail: " + mRecordBufferTail);

        mRecordBufferTail += writ1_len;
        if (mRecordBufferTail >= 1048576 || mRecordBufferTail < 0) {
          mRecordBufferTail = 0;
        }
        com_uti.logd("new mRecordBufferTail: " + mRecordBufferTail);
        if (writ2_len > 0) {
          System.arraycopy(buffer, writ1_len, mRecordBufferData, 0, writ2_len);
          mRecordBufferTail = writ2_len;
          com_uti.logd("Wrote writ2_len: " + writ2_len + "  to mRecordBufferTail 0 / BEGIN  new mRecordBufferTail: " + mRecordBufferTail);
        }
      }
      if (mRecordBufferTail >= 1048576 || mRecordBufferTail < 0) {
        mRecordBufferTail = 0;
      }
      com_uti.logd("final new mRecordBufferTail: " + mRecordBufferTail);
    }
  }

  private void writeBytes(byte[] buffer, int index, int bytes, int value) {
    if (bytes > 0) {
      buffer[index] = (byte) (value & 255);
    }
    if (bytes > 1) {
      buffer[index + 1] = (byte) ((value >> 8) & 255);
    }
    if (bytes > 2) {
      buffer[index + 2] = (byte) ((value >> 16) & 255);
    }
    if (bytes > 3) {
      buffer[index + 3] = (byte) ((value >> 24) & 255);
    }
  }

  private boolean writeWavHeader() {
    byte[] header = com_uti.str_to_ba("RIFF....WAVEfmt sc1safncsamrbytrbabsdatasc2s");
    com_uti.logd("wavHeader.length: " + header.length);
    writeBytes(header, 4, 4, mRecordDataSize + 36);
    writeBytes(header, 16, 4, 16);
    writeBytes(header, 20, 2, 1);
    writeBytes(header, 22, 2, mChannels);
    writeBytes(header, 24, 4, mSampleRate);
    writeBytes(header, 28, 4, (mSampleRate * mChannels) * 2);
    writeBytes(header, 32, 2, mChannels * 2);
    writeBytes(header, 34, 2, 16);
    writeBytes(header, 40, 4, mRecordDataSize);
    try {
      mBufferOutStream.write(header);
      return true;
    } catch (Throwable e) {
      e.printStackTrace();
      return false;
    }
  }

  private void writeFinal() {
    try {
      if (mRecordFile != null) {
        if (mRandomAccessFile == null) {
          mRandomAccessFile = new RandomAccessFile(mRecordFile, "rw");
        }
        byte[] buffer = new byte[4];
        writeBytes(buffer, 0, 4, mRecordDataSize + 36);
        mRandomAccessFile.seek(4);
        mRandomAccessFile.write(buffer);
        writeBytes(buffer, 0, 4, mRecordDataSize);
        mRandomAccessFile.seek(40);
        mRandomAccessFile.write(buffer);
        mRandomAccessFile.close();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    mRecordBufferData = null;
  }

  private String getFilename() {
    Date now = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("hhmmss", Locale.getDefault());
    float freq = Integer.valueOf(mApi.tuner_freq) / 1000;
    return String.format(Locale.ENGLISH, "FM-%.1f-%s.wav", freq, sdf.format(now));
  }
}