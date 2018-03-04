package fm.a2d.sf;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;

import android.media.AudioManager;

import android.os.Bundle;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import fm.a2d.sf.view.PresetView;
import fm.a2d.sf.view.VisualizerView;

import java.util.Locale;

import static android.view.View.TEXT_ALIGNMENT_CENTER;

public class gui_gui implements gui_gap {

  private static final String TAG = "GUI_GUI";

  private static int stat_constrs = 1;
  private int audio_stream = AudioManager.STREAM_MUSIC;

  private Activity m_gui_act;
  private Context mContext;
  private com_api m_com_api;

  private DisplayMetrics mDisplayMetrics;

  private VisualizerView m_visualizerView;
  private boolean gui_vis_disabled = true;

  private Typeface mDigitalFont;


  // TODO: User Interface:
  private Animation m_ani_button = null;

  private LinearLayout m_lv_presets = null;

  // Text:
  private TextView m_tv_rssi = null;
  private TextView m_tv_state = null;
  private TextView m_tv_most = null;
  private TextView m_tv_band = null;
  private TextView m_tv_freq = null;

  // RDS data:
//  private TextView m_tv_picl = null;
//  private TextView m_tv_ps = null;
//  private TextView m_tv_ptyn = null;
//  private TextView m_tv_rt = null;

  // ImageView Buttons:
  private ImageView m_iv_record = null;
  private ImageView m_iv_seekup = null;
  private ImageView m_iv_seekdn = null;

  private ImageView m_iv_prev = null;
  private ImageView m_iv_next = null;

  private ImageView m_iv_paupla = null;
  private ImageView m_iv_stop = null;
  private ImageView m_iv_pause = null;
  private ImageView m_iv_mute = null;
  private ImageView m_iv_menu = null;
  private ImageView m_iv_out = null; // ImageView for Speaker/Headset toggle
  private ImageView m_iv_pwr = null;
  private ImageView m_iv_signal = null;

  private HorizontalScrollView m_hsv_freq = null;
  private SeekBar m_sb_freq = null;

  // Presets
  private PresetView[] mPresetViews;

  private Dialog intro_dialog = null;

//  private String last_rt = "";
  private int last_int_audio_sessid = 0;


  // Code:

  public gui_gui(Context c, com_api the_com_api) { // Constructor
    com_uti.logd("stat_constrs: " + stat_constrs++);

    mContext = c;
    m_gui_act = (Activity) c;
    m_com_api = the_com_api;
  }

  // Lifecycle API

  public boolean gap_state_set(String state) {
    boolean ret = false;
    if (state.equalsIgnoreCase("start"))
      ret = gui_start();
    else if (state.equalsIgnoreCase("stop"))
      ret = gui_stop();
    return (ret);
  }

  private boolean gui_stop() {
    //stopBroadcastListener ();
    if (gui_vis_disabled)
      com_uti.logd("gui_vis_disabled = true");
    else
      gui_vis_stop();
    return (true);
  }


  private boolean gui_start() {

    // !! Hack for s2d comms to allow network activity on UI thread
    com_uti.strict_mode_set(false);

    m_gui_act.requestWindowFeature(Window.FEATURE_NO_TITLE); // No title to save screen space
    m_gui_act.setContentView(R.layout.gui_gui_layout); // Main Layout

    mDisplayMetrics = new DisplayMetrics();
    m_gui_act.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
    m_gui_act.findViewById(R.id.new_fl).setLayoutParams(new LinearLayout.LayoutParams(mDisplayMetrics.widthPixels, ViewGroup.LayoutParams.MATCH_PARENT));

    mDigitalFont = Typeface.createFromAsset(mContext.getAssets(), "fonts/digital-number.ttf");

    m_ani_button = AnimationUtils.loadAnimation(mContext, R.anim.ani_button);// Set button animation

    m_tv_rssi = (TextView) m_gui_act.findViewById(R.id.tv_rssi);
    m_tv_state = (TextView) m_gui_act.findViewById(R.id.tv_state);  // Phase
    m_tv_most = (TextView) m_gui_act.findViewById(R.id.tv_most);

    m_tv_band = (TextView) m_gui_act.findViewById(R.id.tv_band);

//    m_tv_picl = (TextView) m_gui_act.findViewById(R.id.tv_picl);
//    m_tv_ps = (TextView) m_gui_act.findViewById(R.id.tv_ps);
//    m_tv_ptyn = (TextView) m_gui_act.findViewById(R.id.tv_ptyn);
//    m_tv_rt = (TextView) m_gui_act.findViewById(R.id.tv_rt);

    m_iv_seekdn = (ImageView) m_gui_act.findViewById(R.id.iv_seekdn);
    m_iv_seekdn.setOnClickListener(mOnClickListener);

    m_iv_seekup = (ImageView) m_gui_act.findViewById(R.id.iv_seekup);
    m_iv_seekup.setOnClickListener(mOnClickListener);

    m_iv_record = (ImageView) m_gui_act.findViewById(R.id.iv_record);
    m_iv_record.setOnClickListener(mOnClickListener);

    m_iv_prev = (ImageView) m_gui_act.findViewById(R.id.iv_prev);
    m_iv_prev.setOnClickListener(mOnClickListener);

    m_iv_next = (ImageView) m_gui_act.findViewById(R.id.iv_next);
    m_iv_next.setOnClickListener(mOnClickListener);

    m_tv_freq = (TextView) m_gui_act.findViewById(R.id.tv_freq);
    m_tv_freq.setOnClickListener(mOnClickListener);

    m_iv_paupla = (ImageView) m_gui_act.findViewById(R.id.iv_playpause);
    m_iv_paupla.setOnClickListener(mOnClickListener);

    m_iv_stop = (ImageView) m_gui_act.findViewById(R.id.iv_stop);
    m_iv_stop.setOnClickListener(mOnClickListener);

    m_iv_pause = (ImageView) m_gui_act.findViewById(R.id.iv_pause);
    m_iv_pause.setOnClickListener(mOnClickListener);

    m_iv_mute = (ImageView) m_gui_act.findViewById(R.id.iv_mute);
    m_iv_mute.setOnClickListener(mOnClickListener);

    m_iv_menu = (ImageView) m_gui_act.findViewById(R.id.iv_menu);
    m_iv_menu.setOnClickListener(mOnClickListener);

    m_iv_signal = (ImageView) m_gui_act.findViewById(R.id.iv_signal);
    m_lv_presets = (LinearLayout) m_gui_act.findViewById(R.id.preset_list);
    m_hsv_freq = (HorizontalScrollView) m_gui_act.findViewById(R.id.seek_scroll_frequency);

    m_tv_freq.setTypeface(mDigitalFont);
    m_tv_rssi.setTypeface(mDigitalFont);

    m_sb_freq = (SeekBar) m_gui_act.findViewById(R.id.sb_freq_seek);
    m_sb_freq.setMax(205);
    m_sb_freq.setOnSeekBarChangeListener(mOnSeekFrequencyChanged);

    setupPresets();

    updateUIViewsByPowerState(false);

    long curr_time = com_uti.ms_get();
    long radio_gui_first_time = com_uti.long_get(com_uti.prefs_get(mContext, "radio_gui_first_time", ""));
    if (radio_gui_first_time <= 0L) {
      com_uti.prefs_set(mContext, "radio_gui_first_time", String.valueOf(curr_time));
    }

    int radio_gui_start_count = com_uti.prefs_get(mContext, "radio_gui_start_count", 0);
    radio_gui_start_count++;

    if (radio_gui_start_count <= 1) { // If first 1 runs...
      m_com_api.key_set("tuner_band", "EU");
    } else {
      setTunerBand(com_uti.prefs_get(mContext, "tuner_band", "EU"));
    }

    onStarted(radio_gui_start_count);

    m_com_api.key_set("audio_state", "start"); // Start audio service

    updateUIViewsByPowerState(true); // !!!! Move later to Radio API callback

    loadPreferenceVisualState();
    audio_output_load_prefs();
    audio_stereo_load_prefs();
    tuner_stereo_load_prefs();

    return true;
  }

  private SeekBar.OnSeekBarChangeListener mOnSeekFrequencyChanged = new SeekBar.OnSeekBarChangeListener() {

    private int current;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (!fromUser) {
        return;
      }

      current = progress + 875;

      setFrequencyText(String.valueOf((float) current / 10));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
      if (!m_com_api.tuner_state.equalsIgnoreCase("start")) {
        return; // Not Consumed
      }

      setFrequency(String.valueOf(current / 10.0f));
    }
  };

  /**
   * Вызывается когда в первый раз происходит запуск приложения
   */
  private void onStarted(int count) {
    m_gui_act.showDialog(count <= 1 ? DLG_INTRO : DLG_POWER);
  }


  /**
   * Enables/disables buttons based on power
   */
  private void updateUIViewsByPowerState(boolean power) {
    if (m_iv_pwr != null) {
      m_iv_pwr.setImageResource(power ? R.drawable.dial_power_on : R.drawable.dial_power_off);
    }

    if (!power) {
      // Set all displayable text fields to initial OFF defaults
      resetAllViews();
    }

    // Power button is always enabled
    m_iv_seekup.setEnabled(power);
    m_iv_seekdn.setEnabled(power);
    m_iv_record.setEnabled(true);
//    m_tv_rt.setEnabled(power);

    for (int idx = 0; idx < com_api.PRESET_COUNT; idx++) { // For all presets...
      if (mPresetViews[idx] != null) {
        mPresetViews[idx].setEnabled(power);
      }
    }
  }


  // Visualizer:

  private void gui_vis_stop() {
    try {
      com_uti.logd("m_gui_vis: " + this.m_visualizerView);
      if (this.m_visualizerView != null) {
        this.m_visualizerView.vis_stop();
        this.m_visualizerView = null;
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void gui_vis_start(int audio_sessid) {
    try {
      com_uti.logd("m_gui_vis: " + m_visualizerView + "  audio_sessid: " + audio_sessid);
      m_visualizerView = (VisualizerView) m_gui_act.findViewById(R.id.gui_vis);
      if (m_visualizerView == null) {
        showToast("VisualizerView not found");
      } else {
        m_visualizerView.vis_start(audio_sessid);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void visualizer_state_set(String state) {
    com_uti.logd("state: " + state);
    if (state.equals("Start")) {
      gui_vis_disabled = false;
      m_gui_act.findViewById(R.id.vis).setVisibility(View.VISIBLE);
      m_iv_pwr.setVisibility(View.INVISIBLE);
      m_gui_act.findViewById(R.id.frequency_bar).setVisibility(View.INVISIBLE);
      int audio_sessid = com_uti.int_get(m_com_api.audio_sessid);
      if (audio_sessid > 0) {
        do_gui_vis_start(audio_sessid);
      }
    } else {
      gui_vis_disabled = true;
      m_gui_act.findViewById(R.id.vis).setVisibility(View.INVISIBLE);
      m_iv_pwr.setVisibility(View.VISIBLE);
      m_gui_act.findViewById(R.id.frequency_bar).setVisibility(View.VISIBLE);
      gui_vis_stop();
    }
    com_uti.prefs_set(mContext, "gui_visualizer_state", state);
  }


  private void setupPresets() {
    mPresetViews = new PresetView[com_api.PRESET_COUNT];
    for (int idx = 0; idx < mPresetViews.length; idx++) { // For all presets...
      String freq = com_uti.prefs_get(mContext, "radio_freq_prst_" + idx, "");

      Log.i(TAG, "setupPresets: for index " + idx);

      mPresetViews[idx] = new PresetView(mContext);

      mPresetViews[idx].populate(idx, freq).setListeners(mOnClickPresetListener, mOnLongClickPresetListener);

      m_lv_presets.addView(mPresetViews[idx]);
    }
  }


  private void resetAllViews() {
    m_tv_state.setText("");
    m_tv_most.setText("");
    m_tv_band.setText("");
    m_tv_rssi.setText("");
//    m_tv_ps.setText("");
//    m_tv_picl.setText("");
//    m_tv_ptyn.setText("");
//    m_tv_rt.setText("");
//    m_tv_rt.setSelected(true); // Need for marquis
//    m_tv_rt.setText("");
  }


  // Dialog methods:

  private static final int DLG_INTRO = 1;    // First time show this
  private static final int DLG_POWER = 2;    // Every subsequent startup show this
  private static final int DLG_FREQ_SET = 3;

  public Dialog gap_dialog_create(int id, Bundle args) {                            // Create a dialog by calling specific *_dialog_create function    ; Triggered by showDialog (int id);
    //public DialogFragment gap_dialog_create (int id, Bundle args) {
    //com_uti.logd ("id: " + id + "  args: " + args);
    Dialog ret = null;
    //DialogFragment ret = null;
    switch (id) {
      case DLG_INTRO:
      case DLG_POWER:
        ret = openDialogIntro();
        intro_dialog = ret;
        break;
      case DLG_FREQ_SET:
        ret = openDialogChangeFrequency();
        break;
    }
    return (ret);
  }

  /**
   * Открытие интро
   */
  private Dialog openDialogIntro() {
    AlertDialog.Builder dialog = new AlertDialog.Builder(mContext)
            .setTitle("Spirit2 " + com_uti.app_version_get(mContext));

    String intro_msg;

    /*if (!com_uti.file_get("/system/bin/su") && !com_uti.file_get("/system/xbin/su")) {
      intro_msg = "Sorry... \"NO SU\" Error. Spirit2 REQUIRES JB+ & ROOT & Qualcomm.\n\nThanks! :)";
    } else {*/
      intro_msg = "Welcome to Spirit2! :)\n\nPlease wait while it starts...";
    //}

    dialog.setMessage(intro_msg);

    return dialog.create();
  }

  /**
   * Изменение частотного диапазона
   */
  private void setTunerBand(String band) {
    m_com_api.tuner_band = band;
    com_uti.tnru_band_set(band); // To setup band values; different process than service
    m_com_api.key_set("tuner_band", band);
  }

  /**
   * Regional settings:
   */
  private static final int mFrequencyLow = 87500;
  private static final int mFrequencyHigh = 108000;

  /**
   * Открытие диалога изменения текущей частоты
   */
  private Dialog openDialogChangeFrequency() {
    LayoutInflater factory = LayoutInflater.from(mContext);
    AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);

    View textEntryView = factory.inflate(R.layout.edit_number, null);

    final EditText freqEditView = (EditText) textEntryView.findViewById(R.id.edit_number);
    freqEditView.setTypeface(mDigitalFont);
    freqEditView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
    freqEditView.setText(m_com_api.tuner_freq);

    return dialog
      .setTitle(mContext.getString(R.string.dialog_frequency_title))
      .setView(textEntryView)
      .setPositiveButton(mContext.getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          setFrequency(freqEditView.getEditableText().toString().replace(",", "."));
        }
      })
      .setNegativeButton(mContext.getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
        }
      }).create();
  }

  /**
   * Изменение текущей частоты
   */
  private void setFrequency(String nFreq) {

    // If an empty string...
    if (nFreq.isEmpty()) {
      return;
    }

    // If tuner disabled...
    if (!m_com_api.tuner_state.equalsIgnoreCase("start")) {
      return;
    }

    int freq;
    try {
      freq = (int) (Float.valueOf(nFreq) * 1000);
    } catch (Throwable e) {
      com_uti.loge("fFrequency = Float.valueOf (nFreq); failed");
      return;
    }

    if (freq <= 0) {
      freq = 0;
    } else if (freq >= mFrequencyLow * 10 && freq <= mFrequencyHigh * 10) {      // For 760 - 1080
      freq /= 10;
    } else if (freq >= mFrequencyLow * 100 && freq <= mFrequencyHigh * 100) {    // For 7600 - 10800
      freq /= 100;
    } else if (freq >= mFrequencyLow * 1000 && freq <= mFrequencyHigh * 1000) {  // For 76000 - 108000
      freq /= 1000;
    }

    if (freq >= mFrequencyLow && freq <= mFrequencyHigh) {
      m_com_api.key_set("tuner_freq", String.valueOf(freq));
      float f = freq / 1000.0f;
      showToast(String.format(Locale.ENGLISH, "Frequency changed to: %.1f MHz", f));
      onFrequencyChanged(f);
    } else {
      com_uti.loge("Frequency invalid: " + freq);
      showToast("Invalid frequency");
    }
  }

  private void setFrequencyText(String s) {
    if (s.length() == 4) {
      s = " " + s;
    }
    m_tv_freq.setText(s);
  }

  /**
   * Событие изменения частоты
   */
  private void onFrequencyChanged(float frequency) {
    setFrequencyText(String.valueOf(frequency));

    int val = (int) (frequency * 10 - 875);
    m_sb_freq.setProgress(val);


    int x = (m_hsv_freq.getChildAt(0).getWidth() * val  / 205) - (mDisplayMetrics.widthPixels / 2);

    m_hsv_freq.smoothScrollTo(x, 0);
  }

  /**
   * Показ сообщения пользователю
   */
  private void showToast(String text) {
    Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
  }





  //
  private void do_gui_vis_start(int audio_sessid) {
    com_uti.logd("audio_sessid: " + audio_sessid);
    if (gui_vis_disabled) {
      com_uti.logd("gui_vis_disabled = true");
    } else {
      gui_vis_start(audio_sessid);
    }
  }

  // Radio API Callback:
  public void onReceivedUpdates(Intent intent) {
    // Audio Session ID:

    int audio_sessid = com_uti.int_get(m_com_api.audio_sessid);
    if (audio_sessid != 0 && last_int_audio_sessid != audio_sessid) {                        // If audio session ID has changed...
      last_int_audio_sessid = audio_sessid;
      com_uti.logd("m_com_api.audio_sessid: " + m_com_api.audio_sessid + "  audio_sessid: " + audio_sessid);
      // If no session, do nothing (or stop visual and EQ)
      do_gui_vis_start(audio_sessid);
    }

    if (m_com_api.audio_state.equalsIgnoreCase("Start")) {
      if (intro_dialog != null) {
        intro_dialog.dismiss();
        intro_dialog = null;
        setFrequency(m_com_api.tuner_freq);
      }
    }

    // Buttons:

    // Mode Buttons at bottom:
    // Mute/Unmute:
    if (m_com_api.audio_state.equalsIgnoreCase("starting")) {
      com_uti.loge("Audio starting");
      m_iv_paupla.setImageResource(R.drawable.sel_pause);
    } else if (m_com_api.audio_state.equalsIgnoreCase("start")) {
      m_iv_paupla.setImageResource(R.drawable.sel_pause);
    } else if (m_com_api.audio_state.equalsIgnoreCase("pause")) {
      m_iv_paupla.setImageResource(R.drawable.btn_play);
    } else if (m_com_api.audio_state.equalsIgnoreCase("stop")) {
      m_iv_paupla.setImageResource(R.drawable.btn_play);
    } else if (m_com_api.audio_state.equalsIgnoreCase("stopping")) {
      m_iv_paupla.setImageResource(R.drawable.btn_play);
    } else {
      m_iv_paupla.setImageResource(R.drawable.btn_play);//sel_pause);
    }

    if (m_com_api.audio_record_state.equals("Start")) {
      m_iv_record.setImageResource(R.drawable.btn_record_press);
    } else {
      m_iv_record.setImageResource(R.drawable.btn_record);
    }

    // Speaker/Headset:     NOT USED NOW
    if (m_com_api.audio_output.equalsIgnoreCase("speaker")) {                                  // Else if speaker..., Pressing button goes to headset
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.stat_sys_headset);//ic_volume_bluetooth_ad2p);
//com_uti.loge ("Speaker Mode");
    } else {                                                              // Pressing button goes to speaker
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.ic_lock_silent_mode_off);
//com_uti.loge ("Headset Mode");
    }


    // Power:
    updateUIViewsByPowerState(m_com_api.tuner_state.equalsIgnoreCase("start"));

    int ifreq = (int) (com_uti.double_get(m_com_api.tuner_freq) * 1000);
    ifreq = com_uti.tnru_freq_fix(ifreq + 25); // Must fix due to floating point rounding need, else 106.1 = 106.099

    String freq = null;
    if (ifreq >= 50000 && ifreq < 500000) {
      freq = ("" + (double) ifreq / 1000);
    }
    if (freq != null) {
      setFrequencyText(freq);
    }

    m_tv_band.setText(m_com_api.tuner_band);

    updateSignalStretch();

    if (m_com_api.tuner_most.equalsIgnoreCase("Mono"))
      m_tv_most.setText("");
    else if (m_com_api.tuner_most.equalsIgnoreCase("Stereo"))
      m_tv_most.setText("S");
    else
      m_tv_most.setText("");
/*
    m_tv_state.setText("" + m_com_api.tuner_state + " " + m_com_api.audio_state);
    m_tv_picl.setText(m_com_api.tuner_rds_picl);
    m_tv_ps.setText(m_com_api.tuner_rds_ps);
    m_tv_ps2.setText (m_com_api.tuner_rds_ps);
    m_com_api.tuner_rds_ptyn = com_uti.tnru_rds_ptype_get (m_com_api.tuner_band, com_uti.int_get (m_com_api.tuner_rds_pt));
    m_tv_ptyn.setText(m_com_api.tuner_rds_ptyn);
    if (!last_rt.equalsIgnoreCase(m_com_api.tuner_rds_rt)) {
      //com_uti.loge ("rt changed: " + m_com_api.tuner_rds_rt);
      last_rt = m_com_api.tuner_rds_rt;
      m_tv_rt.setText(m_com_api.tuner_rds_rt);
      //m_tv_rt.setMarqueeRepeatLimit (-1);  // Forever
      m_tv_rt.setSelected(true);
    }
*/
  }

  private int SIGNAL_EDGES[] = new int[] {50, 350, 750, 900};
  private int SIGNAL_RES[] = new int[] {R.drawable.ic_signal_0, R.drawable.ic_signal_1, R.drawable.ic_signal_2, R.drawable.ic_signal_3, R.drawable.ic_signal_4};

  /**
   * Обновление уровня сигнала
   */
  private void updateSignalStretch() {
    try {
      int f = Integer.valueOf(m_com_api.tuner_rssi);

      int resId = SIGNAL_RES[4];

      for (int i = 0; i < SIGNAL_EDGES.length; ++i) {
        if (f < SIGNAL_EDGES[i]) {
          resId = SIGNAL_RES[i];
          break;
        }
      }

      m_iv_signal.setImageResource(resId);
      m_tv_rssi.setText(String.format("%4s", m_com_api.tuner_rssi));
    } catch (Exception ignore) {}
  }


  /**
   * UI buttons and other controls
   */
  private View.OnClickListener mOnClickPresetListener = new View.OnClickListener() { // Tune to preset
    public void onClick(View v) {
      PresetView preset = (PresetView) v;

      if (preset.isEmpty()) { // If no preset yet...
        setPreset(preset);
      } else {
        setFrequency(preset.getFrequency());
      }
    }
  };

  /**
   * Save preset in memory
   */
  private void setPreset(PresetView preset) {
    preset.populate(m_com_api.tuner_freq);

    // !! Current implementation requires simultaneous (одновременно)
    m_com_api.key_set(
            "radio_name_prst_" + preset.getIndex(), m_com_api.tuner_freq,
            "radio_freq_prst_" + preset.getIndex(), m_com_api.tuner_freq
    );
  }

  /**
   * Long click: Show preset change options
   */
  private View.OnLongClickListener mOnLongClickPresetListener = new View.OnLongClickListener() {
    public boolean onLongClick(View v) {
      setPreset((PresetView) v);
      v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      return true;
    }
  };




  private View.OnClickListener mOnClickListener = new View.OnClickListener() {
    public void onClick(View v) {
      if (v == m_iv_mute) {
        AudioManager m_am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (m_am != null) {
          // Display volume change
          m_am.setStreamVolume(audio_stream, m_am.getStreamVolume(audio_stream), AudioManager.FLAG_SHOW_UI);
        }

      } else if (v == m_iv_paupla) {
        m_com_api.key_set("audio_state", "Toggle");

      } else if (v == m_iv_stop) {
        m_com_api.key_set("tuner_state", "Stop");

      } else if (v == m_iv_record) {
        m_com_api.key_set("audio_record_state", "Toggle");

      } else if (v == m_iv_out) { // TODO: Speaker/headset  NOT USED NOW
        m_com_api.key_set("audio_output", "toggle");

      } else if (v == m_tv_freq) { // Frequency direct entry
        m_gui_act.showDialog(DLG_FREQ_SET);

      } else if (v == m_iv_seekdn) { // Seek down
        m_com_api.key_set("tuner_scan_state", "down");

      } else if (v == m_iv_seekup) { // Seek up
        m_com_api.key_set("tuner_scan_state", "up");

      } else if (v == m_iv_prev) {
        m_com_api.key_set("tuner_freq", "down");

      } else if (v == m_iv_next) {
        m_com_api.key_set("tuner_freq", "up");

      }

    }
  };

/*
    if (m_com_api.audio_output.equalsIgnoreCase ("speaker")) {                                  // Else if speaker..., Pressing button goes to headset
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.stat_sys_headset);//ic_volume_bluetooth_ad2p);
    }
    else {                                                              // Pressing button goes to speaker
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.ic_lock_silent_mode_off);
    }
*/



  private String tuner_stereo_load_prefs() {
    String value = com_uti.prefs_get(mContext, "tuner_stereo", "");
    return (value);
  }

  private String audio_stereo_load_prefs() {
    String value = com_uti.prefs_get(mContext, "audio_stereo", "");
    return (value);
  }

  private String audio_output_load_prefs() {
    String value = com_uti.prefs_get(mContext, "audio_output", "");
    return (value);
  }

  private String audio_output_set_nonvolatile(String value) {  // Called only by speaker/headset checkbox change
    com_uti.logd("value: " + value);
    m_com_api.key_set("audio_output", value);
    return (value); // No error
  }


  private void loadPreferenceVisualState() {
    String pref = com_uti.prefs_get(mContext, "visual_state", "");
    if (!pref.isEmpty()) {
      setVisualState(pref);
    }
  }

  private String visual_state_set_nonvolatile(String state) {
    String ret = setVisualState(state);
    com_uti.prefs_set(mContext, "visual_state", state);
    return (ret);
  }

  private String setVisualState(String state) {
    com_uti.logd("state: " + state);
    if (state.equalsIgnoreCase("Start")) {
      gui_vis_disabled = false;

      m_iv_pwr.setVisibility(View.INVISIBLE);
      ((ImageView) m_gui_act.findViewById(R.id.frequency_bar)).setVisibility(View.INVISIBLE);

      int audio_sessid = com_uti.int_get(m_com_api.audio_sessid);
      if (audio_sessid > 0)
        do_gui_vis_start(audio_sessid);
    } else {
      gui_vis_disabled = true;

      m_iv_pwr.setVisibility(View.VISIBLE);
      ((ImageView) m_gui_act.findViewById(R.id.frequency_bar)).setVisibility(View.VISIBLE);

      gui_vis_stop();
    }
    return (state);                                                     // No error
  }


  private void cb_tuner_stereo(boolean checked) {
    com_uti.logd("checked: " + checked);
    String val = "Stereo";
    if (!checked)
      val = "Mono";
    m_com_api.key_set("tuner_stereo", val);
  }

  private void cb_audio_stereo(boolean checked) {
    com_uti.logd("checked: " + checked);
    String val = "Stereo";
    if (!checked)
      val = "Mono";
    m_com_api.key_set("audio_stereo", val);
  }

  public void gap_gui_clicked(View view) {

    int id = view.getId();
    com_uti.logd("id: " + id + "  view: " + view);
    switch (id) {
      case R.id.cb_visu:
        if (((CheckBox) view).isChecked()) {
            visualizer_state_set("Start");
        } else {
            visualizer_state_set("Stop");
        }
        break;
    }
  }
}