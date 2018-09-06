package fm.a2d.sf.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.OnDataCaptureListener;
import android.util.AttributeSet;
import android.view.View;
import fm.a2d.sf.com_uti;

public class VisualizerView extends View {
  private boolean freq_avg_shared;
  private boolean freq_interp;
  private Paint freq_paint;
  private int height;
  private Paint line_paint;
  private int m_audio_sessid;
  private Bitmap m_bitmap;
  private Canvas m_canvas;
  private byte[] m_freq_data;
  private float[] m_freq_points;
  private Visualizer m_visualizer;
  private byte[] m_wave_data;
  private float[] m_wave_points;
  private Matrix matr;
  private int num_throwable;
  private boolean wave_avg_shared;
  private boolean wave_interp;
  private Paint wave_paint;
  private int width;

  private OnDataCaptureListener mDataCaptureListener = new OnDataCaptureListener() {
    @Override
    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
      m_wave_data = waveform;
      invalidate();
    }

    @Override
    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
      m_freq_data = fft;
      invalidate();
    }
  };

  public VisualizerView(Context context) {
    this(context, null, 0);
    try {
      com_uti.logd("context: " + context);
    } catch (Throwable e) {
      com_uti.loge("Throwable: " + e);
    }
  }

  public VisualizerView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
    try {
      com_uti.logd("context: " + context + "  attrs: " + attrs);
    } catch (Throwable e) {
      com_uti.loge("Throwable: " + e);
    }
  }

  public VisualizerView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs);
    m_wave_points = new float[16384];
    m_freq_points = new float[16384];
    m_audio_sessid = -3;
    matr = new Matrix();
    wave_interp = false;
    wave_avg_shared = false;
    freq_interp = true;
    freq_avg_shared = true;
    num_throwable = 0;
    try {
      com_uti.logd("context: " + context + "  attrs: " + attrs + "  defStyle: " + defStyle);
      m_wave_data = null;
      m_freq_data = null;
      freq_paint = new Paint();
      freq_paint.setStrokeWidth(0.0f);
      freq_paint.setAntiAlias(false);
      freq_paint.setColor(Color.argb(255, 181, 0, 255));

      wave_paint = new Paint();
      wave_paint.setStrokeWidth(0.0f);
      wave_paint.setAntiAlias(false);
      wave_paint.setColor(Color.argb(255, 0, 235, 165));

      line_paint = new Paint();
      line_paint.setStrokeWidth(0.0f);
      line_paint.setAntiAlias(false);
      line_paint.setColor(Color.argb(255, 255, 255, 192));
    } catch (Throwable e) {
      com_uti.loge("Throwable: " + e);
    }
  }

  public void vis_start(int audio_sessid) {
    try {
      com_uti.logd("audio_sessid: " + audio_sessid + "  m_audio_sessid: " + m_audio_sessid + "  m_visualizer: " + m_visualizer);
      m_audio_sessid = audio_sessid;
      if (m_visualizer != null) {
        com_uti.logd("m_visualizer active, stopping first");
        vis_stop();
      }
      m_visualizer = new Visualizer(audio_sessid);
      int min = Visualizer.getCaptureSizeRange()[0];
      int max = Visualizer.getCaptureSizeRange()[1];
      com_uti.logd("min: " + min + "  max: " + max);
      m_visualizer.setEnabled(false);
      m_visualizer.setCaptureSize(max);
      m_visualizer.setDataCaptureListener(mDataCaptureListener, Visualizer.getMaxCaptureRate() / 2, true, true);
      m_visualizer.setEnabled(true);
    } catch (Throwable e) {
      com_uti.loge("Throwable: " + e);
    }
  }

  public void vis_stop() {
    try {
      com_uti.logd("m_audio_sessid: " + m_audio_sessid + "  m_visualizer: " + m_visualizer);
      if (m_visualizer != null) {
        m_visualizer.release();
      } else {
        com_uti.loge("Already stopped !!");
      }
      m_visualizer = null;
    } catch (Throwable e) {
      com_uti.loge("Throwable: " + e);
    }
  }

  protected void onDraw(Canvas canvas) {
    try {
      super.onDraw(canvas);
      width = getWidth();
      height = getHeight();
      if (m_bitmap == null) {
        m_bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Config.ARGB_8888);
      }
      if (m_canvas == null) {
        m_canvas = new Canvas(m_bitmap);
      }
      m_canvas.drawColor(0, Mode.CLEAR);
      if (!(m_wave_data == null && m_freq_data == null)) {
        scale_draw(canvas);
      }
      if (m_wave_data != null) {
        wave_render(m_canvas, m_wave_data);
      }
      if (m_freq_data != null) {
        freq_render(m_canvas, m_freq_data);
      }
      canvas.drawBitmap(m_bitmap, matr, null);
    } catch (Throwable e) {
      com_uti.loge("Throwable: " + e);
    }
  }

  private void wave_render(Canvas canvas, byte[] wave_data) {
    try {
      int max_points = wave_data.length;
      int last_x = -1;
      int last_y = -1;
      int pidx = 0;
      int cnt_y = 0;
      for (int idx = 0; idx < max_points; idx++) {
        int y = (int) ((double) (((((byte) (wave_data[idx] + 128)) + 128) * height) / 512));
        if (y < 0) {
          y = 0;
        }
        if (y >= height / 2) {
          y = (height / 2) - 1;
        }
        int x = (width * idx) / max_points;
        if (x >= width) {
          x = width - 1;
        }
        if (last_x <= 0 || x != last_x) {
          cnt_y = 0;
          int next_x = last_x + 1;
          int loops = x - next_x;
          if (!wave_interp || last_x < 0 || x <= next_x) {
            if (x + 1 < width) {
              m_wave_points[pidx] = (float) x;
              m_wave_points[pidx + 2] = (float) (x + 1);
              m_wave_points[pidx + 1] = (float) y;
              m_wave_points[pidx + 3] = (float) y;
              if (last_y >= 0) {
                m_wave_points[pidx + 3] = (float) last_y;
              }
              pidx += 4;
            }
          } else {
            for (int loop = 0; loop < loops - 1; loop++) {
              m_wave_points[pidx] = (float) (next_x + loop);
              m_wave_points[pidx + 2] = (float) ((next_x + loop) + 1);
              m_wave_points[pidx + 1] = (float) y;
              m_wave_points[pidx + 3] = (float) y;
              if (last_y >= 0) {
                m_wave_points[pidx + 3] = (float) ((((last_y - y) * loop) / loops) + last_y);
              }
              pidx += 4;
            }
          }
          last_x = x;
          last_y = y;
        } else if (wave_avg_shared) {
          cnt_y++;
          m_wave_points[(pidx - 4) + 3] = (float) ((y + last_y) / cnt_y);
        }
      }
      canvas.drawLines(m_wave_points, wave_paint);
    } catch (Throwable e) {
      com_uti.loge("Throwable: " + e);
    }
  }

  private double amp_log(double in) {
    return Math.log10(in);
  }

  private double freq_log(double in) {
    return Math.log(in) / Math.log(2.0d);
  }

  private void scale_draw(Canvas canvas) {
    float[] dbPoints = new float[128];
    for (int idx = 0; idx < 10; idx++) {
      int x = idx == 9 ? width - 1 : width * idx / 9;
      dbPoints[idx * 4] = (float) x;
      dbPoints[(idx * 4) + 1] = 0.0f;
      dbPoints[(idx * 4) + 2] = (float) x;
      dbPoints[(idx * 4) + 3] = (float) (height - 1);
    }
    for (int idx = 0; idx < 9; idx++) {
      int y = idx == 8 ? height - 1 : height * idx / 8;
      dbPoints[(idx * 4) + 40] = 0.0f;
      dbPoints[((idx * 4) + 40) + 1] = (float) y;
      dbPoints[((idx * 4) + 40) + 2] = (float) (width - 1);
      dbPoints[((idx * 4) + 40) + 3] = (float) y;
    }
    canvas.drawLines(dbPoints, line_paint);
  }

  private void freq_render(Canvas canvas, byte[] freq_data) {
    try {
      int points = freq_data.length / 2;
      int pidx = 0;
      int last_x = -1;
      int last_y = -1;
      int cnt_y = 0;
      double freq_log_points = freq_log((double) points);
      for (int i = 0; i < points; i++) {
        int x;
        byte real = freq_data[(i * 2)];
        byte imag = freq_data[(i * 2) + 1];
        int y = (int) Math.round((((10.0d * amp_log((double) ((real * real) + (imag * imag)))) - 5.0d) * ((double) height)) / 40.0d);
        if (y < 0) {
          y = 0;
        } else {
          if (y >= height) {
            y = height - 1;
          }
        }
        x = (int) ((((double) width) * freq_log((double) i)) / freq_log_points);
        if (x < 0) {
          x = 0;
        }
        if (x >= width) {
          x = width - 1;
        }
        if (last_x <= 0 || x != last_x) {
          cnt_y = 0;
          int next_x = last_x + 1;
          int loops = x - next_x;
          if (!freq_interp || last_x < 0 || x <= next_x) {
            m_freq_points[pidx] = (float) x;
            m_freq_points[pidx + 2] = (float) x;
            m_freq_points[pidx + 1] = (float) height;
            m_freq_points[pidx + 3] = (float) (height - y);
            pidx += 4;
          } else {
            if (loops >= 8) {
              loops /= 2;
            }
            for (int ctr = 0; ctr < loops; ctr++) {
              m_freq_points[pidx] = (float) (next_x + ctr);
              m_freq_points[pidx + 2] = (float) (next_x + ctr);
              m_freq_points[pidx + 1] = (float) height;
              m_freq_points[pidx + 3] = (float) (height - last_y);
              pidx += 4;
            }
          }
          last_x = x;
          last_y = y;
        } else if (freq_avg_shared) {
          cnt_y++;
          m_freq_points[(pidx - 4) + 3] = (float) (height - ((y + last_y) / cnt_y));
        }
      }
      canvas.drawLines(m_freq_points, freq_paint);
    } catch (Throwable e) {
      int i2 = num_throwable;
      num_throwable = i2 + 1;
      if (i2 < 10) {
        com_uti.loge("Throwable: " + e);
        e.printStackTrace();
      }
    }
  }
}