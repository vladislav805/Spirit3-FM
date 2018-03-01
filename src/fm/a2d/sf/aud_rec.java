package fm.a2d.sf;

import android.content.Context;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.Thread.State;

public class aud_rec {
    private static int m_obinits = 1;
    private static final String m_rec_directory_default = "/Music/fm";
    private static final String m_rec_filename_default = "fm_";
    private static final int rec_buf_size = 1048576;
    private int m_channels = 2;
    private com_api m_com_api = null;
    private Context m_context = null;
    private boolean m_need_finish = false;
    private RandomAccessFile m_rand_acc_file = null;
    private BufferedOutputStream m_rec_bos = null;
    private int m_rec_data_size = 0;
    private String m_rec_directory = m_rec_directory_default;
    private File m_rec_file = null;
    private String m_rec_filename = m_rec_filename_default;
    private FileOutputStream m_rec_fos = null;
    private int m_samplerate = 44100;
    private byte[] rec_buf_data = new byte[1048576];
    private int rec_buf_head = 0;
    private int rec_buf_tail = 0;
    private final Runnable rec_write_runnable = new C00011();
    private Thread rec_write_thread = null;
    private boolean rec_write_thread_active = false;
    private boolean rec_write_thread_waiting = false;

    class C00011 implements Runnable {
        C00011() {
        }

        public void run() {
            com_uti.logd("rec_write_runnable run()");
            int cur_buf_tail = aud_rec.this.rec_buf_tail;
            if (aud_rec.this.rec_buf_data == null) {
                aud_rec.this.rec_buf_data = new byte[1048576];
            }
            if (aud_rec.this.rec_buf_data == null) {
                aud_rec.this.rec_write_thread_active = false;
            }
            while (aud_rec.this.rec_write_thread_active) {
                try {
                    cur_buf_tail = aud_rec.this.rec_buf_tail;
                    int len = 0;
                    if (cur_buf_tail == aud_rec.this.rec_buf_head) {
                        Thread.sleep(1000);
                    } else {
                        if (cur_buf_tail > aud_rec.this.rec_buf_head) {
                            len = cur_buf_tail - aud_rec.this.rec_buf_head;
                        } else if (cur_buf_tail < aud_rec.this.rec_buf_head) {
                            len = 1048576 - aud_rec.this.rec_buf_head;
                        }
                        if (len == 0) {
                            com_uti.logd("len = 0 rec_write_runnable run() len: " + len + "  rec_buf_head: " + aud_rec.this.rec_buf_head + "  cur_buf_tail: " + cur_buf_tail + "  rec_buf_size: " + 1048576);
                        } else if (len < 0) {
                            com_uti.loge("len < 0 rec_write_runnable run() len: " + len + "  rec_buf_head: " + aud_rec.this.rec_buf_head + "  cur_buf_tail: " + cur_buf_tail + "  rec_buf_size: " + 1048576);
                        } else {
                            com_uti.logd("len > 0 rec_write_runnable run() len: " + len + "  rec_buf_head: " + aud_rec.this.rec_buf_head + "  cur_buf_tail: " + cur_buf_tail + "  rec_buf_size: " + 1048576);
                            if (aud_rec.this.m_rand_acc_file == null) {
                                aud_rec.this.m_rand_acc_file = new RandomAccessFile(aud_rec.this.m_rec_file, "rw");
                            }
                            if (aud_rec.this.m_rec_bos != null) {
                                aud_rec.this.m_rec_bos.write(aud_rec.this.rec_buf_data, aud_rec.this.rec_buf_head, len);
                                aud_rec.access$712(aud_rec.this, len);
                            }
                            com_uti.logd("Wrote len: " + len + "  to rec_buf_head: " + aud_rec.this.rec_buf_head);
                            aud_rec.access$312(aud_rec.this, len);
                            if (aud_rec.this.rec_buf_head < 0 || aud_rec.this.rec_buf_head >= 1048576) {
                                aud_rec.this.rec_buf_head = 0;
                            }
                            com_uti.logd("new rec_buf_head: " + aud_rec.this.rec_buf_head);
                        }
                    }
                } catch (InterruptedException e) {
                    com_uti.logd("rec_write_runnable run() throwable InterruptedException");
                } catch (Throwable e2) {
                    com_uti.loge("rec_write_runnable run() throwable: " + e2);
                    e2.printStackTrace();
                }
            }
            com_uti.logd("rec_write_runnable run() done");
            if (aud_rec.this.m_need_finish) {
                aud_rec.this.m_need_finish = false;
                aud_rec.this.audio_record_finish();
            }
        }
    }

    static /* synthetic */ int access$312(aud_rec x0, int x1) {
        int i = x0.rec_buf_head + x1;
        x0.rec_buf_head = i;
        return i;
    }

    static /* synthetic */ int access$712(aud_rec x0, int x1) {
        int i = x0.m_rec_data_size + x1;
        x0.m_rec_data_size = i;
        return i;
    }

    public aud_rec(Context context, int samplerate, int channels, com_api svc_com_api) {
        StringBuilder append = new StringBuilder().append("m_obinits: ");
        int i = m_obinits;
        m_obinits = i + 1;
        com_uti.logd(append.append(i).toString());
        com_uti.logd("constructor context: " + context + "  samplerate: " + samplerate + "  channels: " + channels + "  svc_com_api: " + svc_com_api);
        this.m_context = context;
        this.m_samplerate = samplerate;
        this.m_channels = channels;
        this.m_com_api = svc_com_api;
    }

    public void audio_record_write(byte[] aud_buf, int len) {
        if (this.m_rec_data_size < 0 && (this.m_rec_data_size + len) + 36 > -4) {
            com_uti.loge("!!! Max m_rec_data_size: " + this.m_rec_data_size + "  len: " + len);
            this.m_com_api.audio_record_state = "Stop";
            this.m_need_finish = true;
        }
        if (!this.m_com_api.audio_record_state.equals("Stop")) {
            audio_record_write_rec_buf(aud_buf, len);
        }
    }

    public String audio_record_state_set(String state) {
        if (state.equals("Toggle")) {
            if (this.m_com_api.audio_record_state.equals("Stop")) {
                state = "Start";
            } else {
                state = "Stop";
            }
        }
        if (state.equals("Stop")) {
            audio_record_stop();
        } else if (state.equals("Start")) {
            audio_record_start();
        }
        return this.m_com_api.audio_record_state;
    }

    private void audio_record_finish() {
        try {
            if (this.m_rec_data_size != 0) {
                wav_write_final();
            }
            this.m_rec_data_size = 0;
            if (this.m_rec_bos != null) {
                this.m_rec_bos.close();
                this.m_rec_bos = null;
            }
            if (this.m_rec_fos != null) {
                this.m_rec_fos.close();
                this.m_rec_fos = null;
            }
        } catch (Throwable e) {
            com_uti.loge("throwable: " + e);
            e.printStackTrace();
        }
        this.m_rand_acc_file = null;
    }

    private void audio_record_stop() {
        com_uti.logd("audio_record_state: " + this.m_com_api.audio_record_state);
        this.rec_write_thread_active = false;
        if (this.rec_write_thread != null) {
            this.rec_write_thread.interrupt();
        }
        this.m_com_api.audio_record_state = "Stop";
    }

    private boolean audio_record_start() {
        com_uti.logd("audio_record_state: " + this.m_com_api.audio_record_state);
        if (!this.m_com_api.audio_record_state.equals("Stop")) {
            return false;
        }
        this.m_rec_file = null;
        this.m_rec_directory = com_uti.prefs_get(this.m_context, "audio_record_directory", m_rec_directory_default);
        File record_dir = new File(com_uti.getExternalStorageDirectory().getPath() + this.m_rec_directory);
        if (!(record_dir.exists() || record_dir.mkdirs())) {
            com_uti.loge("record_start Directory " + record_dir + " creation error");
        }
        if (!record_dir.canWrite()) {
            record_dir = com_uti.getExternalStorageDirectory();
        }
        if (record_dir.canWrite()) {
            this.m_rec_filename = com_uti.prefs_get(this.m_context, "audio_record_filename", m_rec_filename_default);
            String rec_filename = (this.m_rec_filename + com_uti.utc_timestamp_get()) + ".wav";
            try {
                com_uti.logd("record_dir: " + record_dir + "  rec_filename: " + rec_filename);
                this.m_rec_file = new File(record_dir, rec_filename);
                com_uti.logd("m_rec_file: " + this.m_rec_file);
                this.m_rec_file.createNewFile();
                try {
                    this.m_rec_fos = new FileOutputStream(this.m_rec_file, true);
                    this.m_rec_bos = new BufferedOutputStream(this.m_rec_fos, 131072);
                    this.m_rec_data_size = 0;
                    if (wav_write_header()) {
                        if (!this.rec_write_thread_active) {
                            this.rec_write_thread = new Thread(this.rec_write_runnable, "rec_write");
                            com_uti.logd("rec_write_thread: " + this.rec_write_thread);
                            if (this.rec_write_thread == null) {
                                com_uti.loge("rec_write_thread == null");
                                audio_record_finish();
                                return false;
                            }
                            this.rec_write_thread_active = true;
                            try {
                                if (this.rec_write_thread.getState() == State.NEW || this.rec_write_thread.getState() == State.TERMINATED) {
                                    this.rec_write_thread.start();
                                }
                            } catch (Throwable e) {
                                com_uti.loge("Throwable: " + e);
                                e.printStackTrace();
                                audio_record_finish();
                                this.m_com_api.audio_record_state = "Stop";
                                return false;
                            }
                        }
                        this.m_com_api.audio_record_state = "Start";
                        this.m_need_finish = true;
                        return true;
                    }
                    audio_record_finish();
                    return false;
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
        com_uti.loge("record_start record_start can't write to record_dir: " + record_dir);
        return false;
    }

    private void audio_record_write_rec_buf(byte[] aud_buf, int len) {
        int have1_len = 0;
        int have2_len = 0;
        int cur_buf_head = this.rec_buf_head;
        if (this.rec_buf_data == null || aud_buf == null || len <= 0 || aud_buf.length < len) {
            com_uti.loge("!!! start len: " + len + "  aud_buf: " + aud_buf + "  rec_buf_data: " + this.rec_buf_data);
            return;
        }
        if (cur_buf_head < 0 || cur_buf_head > 1048576) {
            com_uti.loge("!!!  cur_buf_head: " + cur_buf_head);
            cur_buf_head = 0;
            this.rec_buf_head = 0;
        }
        if (this.rec_buf_tail < 0 || this.rec_buf_tail > 1048576) {
            com_uti.loge("!!!  rec_buf_tail: " + this.rec_buf_tail);
            this.rec_buf_tail = 0;
        }
        if (this.rec_buf_tail == cur_buf_head) {
            have1_len = 0;
            have2_len = 0;
        } else if (this.rec_buf_tail > cur_buf_head) {
            have1_len = this.rec_buf_tail - cur_buf_head;
            have2_len = 0;
        } else if (this.rec_buf_tail < cur_buf_head) {
            have1_len = 1048576 - cur_buf_head;
            have2_len = this.rec_buf_tail;
        }
        if (have1_len < 0 || have2_len < 0) {
            com_uti.loge("!!! Negative have len: " + len + "  have1_len: " + have1_len + "  have2_len: " + have2_len + "  cur_buf_head: " + cur_buf_head + "  rec_buf_tail: " + this.rec_buf_tail + "  rec_buf_size: " + 1048576);
        } else if ((have1_len + have2_len) + len >= 1048576) {
            com_uti.loge("!!! Attempt to exceed record buf len: " + len + "  have1_len: " + have1_len + "  have2_len: " + have2_len + "  cur_buf_head: " + cur_buf_head + "  rec_buf_tail: " + this.rec_buf_tail + "  rec_buf_size: " + 1048576);
            if (this.rec_write_thread != null) {
                this.rec_write_thread.interrupt();
            }
        } else {
            int writ1_len;
            int writ2_len;
            if (this.rec_buf_tail + len <= 1048576) {
                writ1_len = len;
                writ2_len = 0;
            } else {
                writ1_len = 1048576 - this.rec_buf_tail;
                writ2_len = len - writ1_len;
            }
            if (writ1_len > 0) {
                int ctr;
                for (ctr = 0; ctr < writ1_len; ctr++) {
                    this.rec_buf_data[this.rec_buf_tail + ctr] = aud_buf[ctr];
                }
                com_uti.logd("Wrote writ1_len: " + writ1_len + "  to rec_buf_tail: " + this.rec_buf_tail);
                this.rec_buf_tail += writ1_len;
                if (this.rec_buf_tail >= 1048576 || this.rec_buf_tail < 0) {
                    this.rec_buf_tail = 0;
                }
                com_uti.logd("new rec_buf_tail: " + this.rec_buf_tail);
                if (writ2_len > 0) {
                    for (ctr = 0; ctr < writ2_len; ctr++) {
                        this.rec_buf_data[ctr] = aud_buf[ctr + writ1_len];
                    }
                    this.rec_buf_tail = writ2_len;
                    com_uti.logd("Wrote writ2_len: " + writ2_len + "  to rec_buf_tail 0 / BEGIN  new rec_buf_tail: " + this.rec_buf_tail);
                }
            }
            if (this.rec_buf_tail >= 1048576 || this.rec_buf_tail < 0) {
                this.rec_buf_tail = 0;
            }
            com_uti.logd("final new rec_buf_tail: " + this.rec_buf_tail);
        }
    }

    private void wav_write_bytes(byte[] buf, int idx, int bytes, int value) {
        if (bytes > 0) {
            buf[idx + 0] = (byte) ((value >> 0) & 255);
        }
        if (bytes > 1) {
            buf[idx + 1] = (byte) ((value >> 8) & 255);
        }
        if (bytes > 2) {
            buf[idx + 2] = (byte) ((value >> 16) & 255);
        }
        if (bytes > 3) {
            buf[idx + 3] = (byte) ((value >> 24) & 255);
        }
    }

    private boolean wav_write_header() {
        byte[] wav_header = com_uti.str_to_ba("RIFF....WAVEfmt sc1safncsamrbytrbabsdatasc2s");
        com_uti.logd("wav_header.length: " + wav_header.length);
        wav_write_bytes(wav_header, 4, 4, this.m_rec_data_size + 36);
        wav_write_bytes(wav_header, 16, 4, 16);
        wav_write_bytes(wav_header, 20, 2, 1);
        wav_write_bytes(wav_header, 22, 2, this.m_channels);
        wav_write_bytes(wav_header, 24, 4, this.m_samplerate);
        wav_write_bytes(wav_header, 28, 4, (this.m_samplerate * this.m_channels) * 2);
        wav_write_bytes(wav_header, 32, 2, this.m_channels * 2);
        wav_write_bytes(wav_header, 34, 2, 16);
        wav_write_bytes(wav_header, 40, 4, this.m_rec_data_size);
        try {
            this.m_rec_bos.write(wav_header);
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    private void wav_write_final() {
        try {
            if (this.m_rec_file != null) {
                if (this.m_rand_acc_file == null) {
                    this.m_rand_acc_file = new RandomAccessFile(this.m_rec_file, "rw");
                }
                byte[] buf = new byte[4];
                wav_write_bytes(buf, 0, 4, this.m_rec_data_size + 36);
                this.m_rand_acc_file.seek(4);
                this.m_rand_acc_file.write(buf);
                wav_write_bytes(buf, 0, 4, this.m_rec_data_size);
                this.m_rand_acc_file.seek(40);
                this.m_rand_acc_file.write(buf);
                this.m_rand_acc_file.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
