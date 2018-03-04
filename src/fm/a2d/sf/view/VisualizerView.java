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
    private static int m_obinits = 1;
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

    class C00141 implements OnDataCaptureListener {
        C00141() {
        }

        public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            VisualizerView.this.m_wave_data = bytes;
            VisualizerView.this.invalidate();
        }

        public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            VisualizerView.this.m_freq_data = bytes;
            VisualizerView.this.invalidate();
        }
    }

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
        this.m_wave_points = new float[16384];
        this.m_freq_points = new float[16384];
        this.m_audio_sessid = -3;
        this.matr = new Matrix();
        this.wave_interp = false;
        this.wave_avg_shared = false;
        this.freq_interp = true;
        this.freq_avg_shared = true;
        this.num_throwable = 0;
        StringBuilder append = new StringBuilder().append("m_obinits: ");
        int i = m_obinits;
        m_obinits = i + 1;
        com_uti.logd(append.append(i).toString());
        try {
            com_uti.logd("context: " + context + "  attrs: " + attrs + "  defStyle: " + defStyle);
            this.m_wave_data = null;
            this.m_freq_data = null;
            this.freq_paint = new Paint();
            this.freq_paint.setStrokeWidth(0.0f);
            this.freq_paint.setAntiAlias(false);
            this.freq_paint.setColor(Color.argb(255, 181, 0, 255));
            this.wave_paint = new Paint();
            this.wave_paint.setStrokeWidth(0.0f);
            this.wave_paint.setAntiAlias(false);
            this.wave_paint.setColor(Color.argb(255, 0, 235, 165));
            this.line_paint = new Paint();
            this.line_paint.setStrokeWidth(0.0f);
            this.line_paint.setAntiAlias(false);
            this.line_paint.setColor(Color.argb(255, 255, 255, 192));
        } catch (Throwable e) {
            com_uti.loge("Throwable: " + e);
        }
    }

    public void vis_start(int audio_sessid) {
        try {
            com_uti.logd("audio_sessid: " + audio_sessid + "  m_audio_sessid: " + this.m_audio_sessid + "  m_visualizer: " + this.m_visualizer);
            this.m_audio_sessid = audio_sessid;
            if (this.m_visualizer != null) {
                com_uti.logd("m_visualizer active, stopping first");
                vis_stop();
            }
            this.m_visualizer = new Visualizer(audio_sessid);
            if (this.m_visualizer == null) {
                com_uti.loge("m_visualizer == null");
                return;
            }
            int min = Visualizer.getCaptureSizeRange()[0];
            int max = Visualizer.getCaptureSizeRange()[1];
            com_uti.logd("min: " + min + "  max: " + max);
            this.m_visualizer.setEnabled(false);
            this.m_visualizer.setCaptureSize(max);
            this.m_visualizer.setDataCaptureListener(new C00141(), Visualizer.getMaxCaptureRate() / 2, true, true);
            this.m_visualizer.setEnabled(true);
        } catch (Throwable e) {
            com_uti.loge("Throwable: " + e);
        }
    }

    public void vis_stop() {
        try {
            com_uti.logd("m_audio_sessid: " + this.m_audio_sessid + "  m_visualizer: " + this.m_visualizer);
            if (this.m_visualizer != null) {
                this.m_visualizer.release();
            } else {
                com_uti.loge("Already stopped !!");
            }
            this.m_visualizer = null;
        } catch (Throwable e) {
            com_uti.loge("Throwable: " + e);
        }
    }

    protected void onDraw(Canvas canvas) {
        try {
            super.onDraw(canvas);
            this.width = getWidth();
            this.height = getHeight();
            if (this.m_bitmap == null) {
                this.m_bitmap = Bitmap.createBitmap(canvas.getWidth(), canvas.getHeight(), Config.ARGB_8888);
            }
            if (this.m_canvas == null) {
                this.m_canvas = new Canvas(this.m_bitmap);
            }
            this.m_canvas.drawColor(0, Mode.CLEAR);
            if (!(this.m_wave_data == null && this.m_freq_data == null)) {
                scale_draw(canvas);
            }
            if (this.m_wave_data != null) {
                wave_render(this.m_canvas, this.m_wave_data);
            }
            if (this.m_freq_data != null) {
                freq_render(this.m_canvas, this.m_freq_data);
            }
            canvas.drawBitmap(this.m_bitmap, this.matr, null);
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
                int y = (int) ((double) (((((byte) (wave_data[idx] + 128)) + 128) * this.height) / 512));
                if (y < 0) {
                    y = 0;
                }
                if (y >= this.height / 2) {
                    y = (this.height / 2) - 1;
                }
                int x = (this.width * idx) / max_points;
                if (x >= this.width) {
                    x = this.width - 1;
                }
                if (last_x <= 0 || x != last_x) {
                    cnt_y = 0;
                    int next_x = last_x + 1;
                    int loops = x - next_x;
                    if (!this.wave_interp || last_x < 0 || x <= next_x) {
                        if (x + 1 < this.width) {
                            this.m_wave_points[pidx + 0] = (float) x;
                            this.m_wave_points[pidx + 2] = (float) (x + 1);
                            this.m_wave_points[pidx + 1] = (float) y;
                            this.m_wave_points[pidx + 3] = (float) y;
                            if (last_y >= 0) {
                                this.m_wave_points[pidx + 3] = (float) last_y;
                            }
                            pidx += 4;
                        }
                    } else {
                        for (int loop = 0; loop < loops - 1; loop++) {
                            this.m_wave_points[pidx + 0] = (float) (next_x + loop);
                            this.m_wave_points[pidx + 2] = (float) ((next_x + loop) + 1);
                            this.m_wave_points[pidx + 1] = (float) y;
                            this.m_wave_points[pidx + 3] = (float) y;
                            if (last_y >= 0) {
                                this.m_wave_points[pidx + 3] = (float) ((((last_y - y) * loop) / loops) + last_y);
                            }
                            pidx += 4;
                        }
                    }
                    last_x = x;
                    last_y = y;
                } else if (this.wave_avg_shared) {
                    cnt_y++;
                    this.m_wave_points[(pidx - 4) + 3] = (float) ((y + last_y) / cnt_y);
                }
            }
            canvas.drawLines(this.m_wave_points, this.wave_paint);
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
        int idx;
        float[] dbPoints = new float[128];
        for (idx = 0; idx < 10; idx++) {
            int x;
            if (idx == 9) {
                x = this.width - 1;
            } else {
                x = (this.width * idx) / 9;
            }
            dbPoints[((idx * 4) + 0) + 0] = (float) x;
            dbPoints[((idx * 4) + 0) + 1] = 0.0f;
            dbPoints[((idx * 4) + 0) + 2] = (float) x;
            dbPoints[((idx * 4) + 0) + 3] = (float) (this.height - 1);
        }
        int dbidx = 0 + 10;
        for (idx = 0; idx < 9; idx++) {
            int y;
            if (idx == 8) {
                y = this.height - 1;
            } else {
                y = (this.height * idx) / 8;
            }
            dbPoints[((idx * 4) + 40) + 0] = 0.0f;
            dbPoints[((idx * 4) + 40) + 1] = (float) y;
            dbPoints[((idx * 4) + 40) + 2] = (float) (this.width - 1);
            dbPoints[((idx * 4) + 40) + 3] = (float) y;
        }
        dbidx = 9 + 10;
        canvas.drawLines(dbPoints, this.line_paint);
    }

    private void freq_render(Canvas canvas, byte[] freq_data) {
        try {
            int points = freq_data.length / 2;
            int max_points = points;
            int pidx = 0;
            int last_x = -1;
            int last_y = -1;
            int cnt_y = 0;
            double freq_log_points = freq_log((double) points);
            for (int i = 0; i < max_points; i++) {
                int x;
                byte real = freq_data[(i * 2) + 0];
                byte imag = freq_data[(i * 2) + 1];
                int y = (int) Math.round((((10.0d * amp_log((double) ((real * real) + (imag * imag)))) - 5.0d) * ((double) this.height)) / 40.0d);
                if (y < 0) {
                    y = 0;
                } else {
                    if (y >= this.height) {
                        y = this.height - 1;
                    }
                }
                if (false) {
                    x = (this.width * i) / max_points;
                } else {
                    x = (int) ((((double) this.width) * freq_log((double) i)) / freq_log_points);
                }
                if (x < 0) {
                    x = 0;
                }
                if (x >= this.width) {
                    x = this.width - 1;
                }
                if (last_x <= 0 || x != last_x) {
                    cnt_y = 0;
                    int next_x = last_x + 1;
                    int loops = x - next_x;
                    if (!this.freq_interp || last_x < 0 || x <= next_x) {
                        this.m_freq_points[pidx + 0] = (float) x;
                        this.m_freq_points[pidx + 2] = (float) x;
                        this.m_freq_points[pidx + 1] = (float) this.height;
                        this.m_freq_points[pidx + 3] = (float) (this.height - y);
                        pidx += 4;
                    } else {
                        if (loops >= 8) {
                            loops /= 2;
                        }
                        for (int ctr = 0; ctr < loops; ctr++) {
                            this.m_freq_points[pidx + 0] = (float) (next_x + ctr);
                            this.m_freq_points[pidx + 2] = (float) (next_x + ctr);
                            this.m_freq_points[pidx + 1] = (float) (this.height + 0);
                            this.m_freq_points[pidx + 3] = (float) (this.height - last_y);
                            pidx += 4;
                        }
                    }
                    last_x = x;
                    last_y = y;
                } else if (this.freq_avg_shared) {
                    cnt_y++;
                    this.m_freq_points[(pidx - 4) + 3] = (float) (this.height - ((y + last_y) / cnt_y));
                }
            }
            canvas.drawLines(this.m_freq_points, this.freq_paint);
        } catch (Throwable e) {
            int i2 = this.num_throwable;
            this.num_throwable = i2 + 1;
            if (i2 < 10) {
                com_uti.loge("Throwable: " + e);
                e.printStackTrace();
            }
        }
    }
}
