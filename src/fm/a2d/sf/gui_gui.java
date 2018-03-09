package fm.a2d.sf;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.media.AudioManager;
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

public class gui_gui implements gui_gap, View.OnClickListener, View.OnLongClickListener {

  private static final String TAG = "GUI_GUI";

  // References
  private Activity mActivity;
  private Context mContext;
  private com_api mApi;

  // Metrics
  private DisplayMetrics mDisplayMetrics;

  // Visualizer
  private VisualizerView mVisualizerView;
  private boolean mVisualizerDisabled = true;

  // TODO: User Interface:
  private Animation m_ani_button = null;
  private Typeface mDigitalFont;

  private LinearLayout mViewListPresets = null;

  // Info
  private TextView mViewRSSI = null;
  private TextView mViewState = null;
  private TextView mViewStereo = null;
  private TextView mViewBand = null;
  private TextView mViewFrequency = null;
  private TextView mViewRecordDuration = null;

  // RDS data:
//  private TextView m_tv_picl = null;
//  private TextView m_tv_ps = null;
//  private TextView m_tv_ptyn = null;
//  private TextView m_tv_rt = null;

  // ImageView Buttons:
  private ImageView mViewRecord = null;
  private ImageView mViewSeekUp = null;
  private ImageView mViewSeekDown = null;

  // Navigation
  private ImageView mViewPrevious = null;
  private ImageView mViewNext = null;

  // Control
  private ImageView mViewPlayToggle = null;
  private ImageView mViewMute = null;

  private ImageView m_iv_out = null; // ImageView for Speaker/Headset toggle
  private ImageView m_iv_pwr = null;
  private ImageView mViewSignal = null;

  // Seek frequency line
  private HorizontalScrollView mViewLineFrequency = null;
  private SeekBar mViewSeekFrequency = null;

  // Presets
  private PresetView[] mPresetViews;

  private Dialog mIntroDialog = null;

//  private String last_rt = "";
  private int mLastAudioSessionId = 0;

  // Code:
  public gui_gui(Context c, com_api the_com_api) { // Constructor
    mContext = c;
    mActivity = (Activity) c;
    mApi = the_com_api;
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
    if (mVisualizerDisabled)
      com_uti.logd("mVisualizerDisabled = true");
    else
      gui_vis_stop();
    return (true);
  }


  private boolean gui_start() {

    // !! Hack for s2d comms to allow network activity on UI thread
    com_uti.strict_mode_set(false);

    mActivity.requestWindowFeature(Window.FEATURE_NO_TITLE); // No title to save screen space
    mActivity.setContentView(R.layout.gui_gui_layout); // Main Layout

    mDisplayMetrics = new DisplayMetrics();
    mActivity.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
    mActivity.findViewById(R.id.new_fl).setLayoutParams(new LinearLayout.LayoutParams(mDisplayMetrics.widthPixels, ViewGroup.LayoutParams.MATCH_PARENT));

    mDigitalFont = Typeface.createFromAsset(mContext.getAssets(), "fonts/digital-number.ttf");

    m_ani_button = AnimationUtils.loadAnimation(mContext, R.anim.ani_button);// Set button animation

    mViewRSSI = (TextView) mActivity.findViewById(R.id.tv_rssi);
    mViewState = (TextView) mActivity.findViewById(R.id.tv_state);  // Phase
    mViewStereo = (TextView) mActivity.findViewById(R.id.tv_most);
    mViewBand = (TextView) mActivity.findViewById(R.id.tv_band);

//    m_tv_picl = (TextView) mActivity.findViewById(R.id.tv_picl);
//    m_tv_ps = (TextView) mActivity.findViewById(R.id.tv_ps);
//    m_tv_ptyn = (TextView) mActivity.findViewById(R.id.tv_ptyn);
//    m_tv_rt = (TextView) mActivity.findViewById(R.id.tv_rt);

    mViewSeekDown = (ImageView) mActivity.findViewById(R.id.iv_seekdn);
    mViewSeekDown.setOnClickListener(this);

    mViewSeekUp = (ImageView) mActivity.findViewById(R.id.iv_seekup);
    mViewSeekUp.setOnClickListener(this);

    mViewRecord = (ImageView) mActivity.findViewById(R.id.iv_record);
    mViewRecord.setOnClickListener(this);

    mViewRecordDuration = (TextView) mActivity.findViewById(R.id.tv_record_duration);

    mViewPrevious = (ImageView) mActivity.findViewById(R.id.iv_prev);
    mViewPrevious.setOnClickListener(this);

    mViewNext = (ImageView) mActivity.findViewById(R.id.iv_next);
    mViewNext.setOnClickListener(this);

    mViewFrequency = (TextView) mActivity.findViewById(R.id.tv_freq);
    mViewFrequency.setOnClickListener(this);

    mViewPlayToggle = (ImageView) mActivity.findViewById(R.id.iv_play_toggle);
    mViewPlayToggle.setOnClickListener(this);
    mViewPlayToggle.setOnLongClickListener(this);

    mViewMute = (ImageView) mActivity.findViewById(R.id.iv_mute);
    mViewMute.setOnClickListener(this);

    mViewSignal = (ImageView) mActivity.findViewById(R.id.iv_signal);
    mViewListPresets = (LinearLayout) mActivity.findViewById(R.id.preset_list);
    mViewLineFrequency = (HorizontalScrollView) mActivity.findViewById(R.id.seek_scroll_frequency);

    mViewSeekFrequency = (SeekBar) mActivity.findViewById(R.id.sb_freq_seek);
    mViewSeekFrequency.setMax(205);
    mViewSeekFrequency.setOnSeekBarChangeListener(mOnSeekFrequencyChanged);

    mViewFrequency.setTypeface(mDigitalFont);
    mViewRSSI.setTypeface(mDigitalFont);

    setupPresets();

    updateUIViewsByPowerState(false);

    if (com_uti.long_get(com_uti.prefs_get(mContext, C.GUI_START_FIRST_TIME, "")) <= 0L) {
      com_uti.prefs_set(mContext, C.GUI_START_FIRST_TIME, String.valueOf(com_uti.ms_get()));
    }

    int startGuiCount = com_uti.prefs_get(mContext, C.GUI_START_COUNT, 0);
    startGuiCount++;

    if (startGuiCount <= 1) { // If first 1 runs...
      mApi.key_set(C.TUNER_BAND, "EU");
    } else {
      setTunerBand(com_uti.prefs_get(mContext, C.TUNER_BAND, "EU"));
    }

    onStarted(startGuiCount);

    mApi.key_set("audio_state", "start"); // Start audio service

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
      if (!mApi.isTunerStarted()) {
        return; // Not Consumed
      }

      setFrequency(String.valueOf(current / 10.0f));
    }
  };

  /**
   * Вызывается когда в первый раз происходит запуск приложения
   */
  private void onStarted(int count) {
    openDialogIntro(count);
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

    mViewFrequency.setShadowLayer(power ? 20 : 0, 0, 0, mContext.getResources().getColor(R.color.primary_blue_shadow));

    int color = mContext.getResources().getColor(R.color.primary_blue);
    if (!power) {
      //noinspection NumericOverflow
      color = (color & 0x00ffffff) | (0x88 << 24);
    }
    mViewFrequency.setTextColor(color);

    // Power button is always enabled
    mViewSeekUp.setEnabled(power);
    mViewSeekDown.setEnabled(power);
    mViewRecord.setEnabled(true);
//    m_tv_rt.setEnabled(power);

    for (int idx = 0; idx < com_api.PRESET_COUNT; idx++) { // For all presets...
      if (mPresetViews[idx] != null) {
        mPresetViews[idx].setEnabled(power);
      }
    }
  }


  // Visualizer:

  private void gui_vis_start(int audio_sessid) {
    try {
      com_uti.logd("m_gui_vis: " + mVisualizerView + "  audio_sessid: " + audio_sessid);
      mVisualizerView = (VisualizerView) mActivity.findViewById(R.id.gui_vis);
      if (mVisualizerView == null) {
        showToast("VisualizerView not found");
      } else {
        showToast("vis start");
        mVisualizerView.vis_start(audio_sessid);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void gui_vis_stop() {
    try {
      com_uti.logd("m_gui_vis: " + this.mVisualizerView);
      if (this.mVisualizerView != null) {
        this.mVisualizerView.vis_stop();
        this.mVisualizerView = null;
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void visualizer_state_set(String state) {
    com_uti.logd("state: " + state);
    if (state.equalsIgnoreCase("Start")) {
      mVisualizerDisabled = false;
      mActivity.findViewById(R.id.vis).setVisibility(View.VISIBLE);
      //m_iv_pwr.setVisibility(View.INVISIBLE);
      //mActivity.findViewById(R.id.frequency_bar).setVisibility(View.INVISIBLE);
      int audio_sessid = com_uti.int_get(mApi.audio_sessid);
      showToast("sessid: " + audio_sessid);
      //if (audio_sessid > 0) {
        do_gui_vis_start(audio_sessid);
      //}
    } else {
      mVisualizerDisabled = true;
      mActivity.findViewById(R.id.vis).setVisibility(View.INVISIBLE);
      //m_iv_pwr.setVisibility(View.VISIBLE);
      //mActivity.findViewById(R.id.frequency_bar).setVisibility(View.VISIBLE);
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

      mViewListPresets.addView(mPresetViews[idx]);
    }
  }


  private void resetAllViews() {
    mViewState.setText("");
    mViewStereo.setText("");
    mViewBand.setText("");
    mViewRSSI.setText("");
//    m_tv_ps.setText("");
//    m_tv_picl.setText("");
//    m_tv_ptyn.setText("");
//    m_tv_rt.setText("");
//    m_tv_rt.setSelected(true); // Need for marquis
//    m_tv_rt.setText("");
  }

  /**
   * Открытие интро
   */
  private void openDialogIntro(@SuppressWarnings("unused") int count) {
    AlertDialog.Builder dialog = new AlertDialog.Builder(mContext)
            .setTitle("Spirit3 " + com_uti.app_version_get(mContext));

    String intro_msg;

    /*if (!com_uti.file_get("/system/bin/su") && !com_uti.file_get("/system/xbin/su")) {
      intro_msg = "Sorry... \"NO SU\" Error. Spirit2 REQUIRES JB+ & ROOT & Qualcomm.\n\nThanks! :)";
    } else {*/
      intro_msg = "Welcome to Spirit3! :)\n\nPlease wait while it starts...";
    //}
    mIntroDialog = dialog.setCancelable(false).setIcon(R.drawable.ic_radio).setMessage(intro_msg).create();
    mIntroDialog.show();
  }

  /**
   * Изменение частотного диапазона
   */
  private void setTunerBand(String band) {
    mApi.tuner_band = band;
    com_uti.tnru_band_set(band); // To setup band values; different process than service
    mApi.key_set(C.TUNER_BAND, band);
  }

  /**
   * Regional settings:
   */
  private static final int mFrequencyLow = 87500;
  private static final int mFrequencyHigh = 108000;

  /**
   * Открытие диалога изменения текущей частоты
   */
  private void openDialogChangeFrequency() {
    LayoutInflater factory = LayoutInflater.from(mContext);
    AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);

    View textEntryView = factory.inflate(R.layout.edit_number, null);

    final EditText freqEditView = (EditText) textEntryView.findViewById(R.id.edit_number);
    freqEditView.setTypeface(mDigitalFont);
    freqEditView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
    freqEditView.setText(mApi.getStringFrequencyMHz());

    dialog
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
        }).create().show();
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
    if (!mApi.isTunerStarted()) {
      return;
    }

    int freq;
    try {
      freq = (int) (Float.valueOf(nFreq) * 1000);
    } catch (Throwable e) {
      com_uti.loge("fFrequency = Float.valueOf(nFreq); failed");
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
      mApi.key_set("tuner_freq", String.valueOf(freq));
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
    mViewFrequency.setText(s);
  }

  /**
   * Событие изменения частоты
   */
  private void onFrequencyChanged(float frequency) {
    setFrequencyText(String.valueOf(frequency));

    int val = (int) (frequency * 10 - 875);
    mViewSeekFrequency.setProgress(val);


    int x = (mViewLineFrequency.getChildAt(0).getWidth() * val  / 205) - (mDisplayMetrics.widthPixels / 2);

    mViewLineFrequency.smoothScrollTo(x, 0);
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
    if (mVisualizerDisabled) {
      com_uti.logd("mVisualizerDisabled = true");
    } else {
      gui_vis_start(audio_sessid);
    }
  }

  // Radio API Callback:
  public void onReceivedUpdates(Intent intent) {
    // Audio Session ID:

    int audio_sessid = com_uti.int_get(mApi.audio_sessid);
    if (audio_sessid != 0 && mLastAudioSessionId != audio_sessid) { // If audio session ID has changed...
      mLastAudioSessionId = audio_sessid;
      com_uti.logd("mApi.audio_sessid: " + mApi.audio_sessid + "  audio_sessid: " + audio_sessid);
      // If no session, do nothing (or stop visual and EQ)
      do_gui_vis_start(audio_sessid);
    }

    if (mApi.audio_state.equalsIgnoreCase("Start")) {
      if (mIntroDialog != null) {
        mIntroDialog.dismiss();
        mIntroDialog = null;
        setFrequency(mApi.getStringFrequencyMHz());
      }
    }

    // Buttons:

    setPlayToggleButtonState(mApi.audio_state);
    setRecordAudioState(mApi.audio_record_state);

    // Speaker/Headset:     NOT USED NOW
    if (mApi.audio_output.equalsIgnoreCase("speaker")) {                                  // Else if speaker..., Pressing button goes to headset
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.stat_sys_headset);//ic_volume_bluetooth_ad2p);
//com_uti.loge ("Speaker Mode");
    } else {                                                              // Pressing button goes to speaker
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.ic_lock_silent_mode_off);
//com_uti.loge ("Headset Mode");
    }

/* TODO: set duration
    if (mApi.audio_record_state.equals(C.RECORD_STATE_START)) {

    }
 */

    // Power:
    updateUIViewsByPowerState(mApi.isTunerStarted());

    int ifreq = (int) (com_uti.double_get(mApi.tuner_freq) * 1000);
    ifreq = com_uti.tnru_freq_fix(ifreq + 25); // Must fix due to floating point rounding need, else 106.1 = 106.099

    String freq = null;
    if (ifreq >= 50000 && ifreq < 500000) {
      freq = String.valueOf((double) ifreq / 1000);
    }
    if (freq != null) {
      setFrequencyText(freq);
      // TODO: it here
      //onFrequencyChanged(mApi.getFloatFrequencyMHz());
    }

    mViewBand.setText(mApi.tuner_band);

    updateSignalStretch();

    if (mApi.tuner_most.equalsIgnoreCase("Mono"))
      mViewStereo.setText("");
    else if (mApi.tuner_most.equalsIgnoreCase("Stereo"))
      mViewStereo.setText("S");
    else
      mViewStereo.setText("");
/*
    mViewState.setText("" + mApi.tuner_state + " " + mApi.audio_state);
    m_tv_picl.setText(mApi.tuner_rds_picl);
    m_tv_ps.setText(mApi.tuner_rds_ps);
    m_tv_ps2.setText (mApi.tuner_rds_ps);
    mApi.tuner_rds_ptyn = com_uti.tnru_rds_ptype_get (mApi.tuner_band, com_uti.int_get (mApi.tuner_rds_pt));
    m_tv_ptyn.setText(mApi.tuner_rds_ptyn);
    if (!last_rt.equalsIgnoreCase(mApi.tuner_rds_rt)) {
      //com_uti.loge ("rt changed: " + mApi.tuner_rds_rt);
      last_rt = mApi.tuner_rds_rt;
      m_tv_rt.setText(mApi.tuner_rds_rt);
      //m_tv_rt.setMarqueeRepeatLimit (-1);  // Forever
      m_tv_rt.setSelected(true);
    }
*/
  }

  private void setPlayToggleButtonState(String state) {
    switch (state.toLowerCase()) {
      case C.AUDIO_STATE_STARTING:
        com_uti.loge("Audio starting");
        mViewPlayToggle.setEnabled(false);
        mViewPlayToggle.setImageResource(R.drawable.ic_pause);
        break;

      case C.AUDIO_STATE_START:
        mViewPlayToggle.setEnabled(true);
        mViewPlayToggle.setImageResource(R.drawable.ic_pause);
        break;

      case C.AUDIO_STATE_PAUSE:
        mViewPlayToggle.setEnabled(true);
        mViewPlayToggle.setImageResource(R.drawable.ic_play);
        break;

      case C.AUDIO_STATE_STOPPING:
        mViewPlayToggle.setEnabled(false);
      case C.AUDIO_STATE_STOP:
      default:
        mViewPlayToggle.setImageResource(R.drawable.ic_play);
    }
  }

  private void setRecordAudioState(String state) {
    mViewRecord.setImageResource(state.equals(C.RECORD_STATE_START) ? R.drawable.btn_record_press : R.drawable.btn_record);
  }

  private int SIGNAL_EDGES[] = new int[] {50, 350, 750, 900};
  private int SIGNAL_RES[] = new int[] {R.drawable.ic_signal_0, R.drawable.ic_signal_1, R.drawable.ic_signal_2, R.drawable.ic_signal_3, R.drawable.ic_signal_4};

  /**
   * Обновление уровня сигнала
   */
  private void updateSignalStretch() {
    try {
      int f = Integer.valueOf(mApi.tuner_rssi);

      int resId = SIGNAL_RES[4];

      for (int i = 0; i < SIGNAL_EDGES.length; ++i) {
        if (f < SIGNAL_EDGES[i]) {
          resId = SIGNAL_RES[i];
          break;
        }
      }

      mViewSignal.setImageResource(resId);
      mViewRSSI.setText(String.format("%4s", mApi.tuner_rssi));
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
    preset.populate(mApi.getStringFrequencyMHz());

    // !! Current implementation requires simultaneous (одновременно)
    mApi.key_set(
            "radio_name_prst_" + preset.getIndex(), mApi.getStringFrequencyMHz(),
            "radio_freq_prst_" + preset.getIndex(), mApi.getStringFrequencyMHz()
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

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.iv_mute:
        AudioManager m_am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (m_am != null) {
          // Display volume change
          m_am.setStreamVolume(AudioManager.STREAM_MUSIC, m_am.getStreamVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
        }
        break;

      case R.id.iv_play_toggle:
        mApi.key_set("audio_state", "Toggle");
        break;

      case R.id.iv_record:
        mApi.key_set("audio_record_state", C.RECORD_STATE_TOGGLE);
        break;

      //case R.id.iv_out: -> m_iv_out ???? / TODO: Speaker/headset  NOT USED NOW
      //  mApi.key_set("audio_output", "toggle");

      case R.id.tv_freq:
        openDialogChangeFrequency();
        break;

      case R.id.iv_seekdn:
        mApi.key_set("tuner_scan_state", "down");
        break;

      case R.id.iv_seekup:
        mApi.key_set("tuner_scan_state", "up");
        break;

      case R.id.iv_prev:
        mApi.key_set("tuner_freq", "down");
        break;

      case R.id.iv_next:
        mApi.key_set("tuner_freq", "up");
        break;


    }
  }

/*
    if (mApi.audio_output.equalsIgnoreCase ("speaker")) {                                  // Else if speaker..., Pressing button goes to headset
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.stat_sys_headset);//ic_volume_bluetooth_ad2p);
    }
    else {                                                              // Pressing button goes to speaker
      //if (m_iv_out != null)
      //  m_iv_out.setImageResource (android.R.drawable.ic_lock_silent_mode_off);
    }
*/


  @Override
  public boolean onLongClick(View v) {
    switch (v.getId()) {

      case R.id.iv_play_toggle:
        mApi.key_set("tuner_state", "Stop");
        return true;

    }
    return false;
  }


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
    mApi.key_set("audio_output", value);
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
      mVisualizerDisabled = false;

      m_iv_pwr.setVisibility(View.INVISIBLE);
      ((ImageView) mActivity.findViewById(R.id.frequency_bar)).setVisibility(View.INVISIBLE);

      int audio_sessid = com_uti.int_get(mApi.audio_sessid);
      if (audio_sessid > 0)
        do_gui_vis_start(audio_sessid);
    } else {
      mVisualizerDisabled = true;

      m_iv_pwr.setVisibility(View.VISIBLE);
      ((ImageView) mActivity.findViewById(R.id.frequency_bar)).setVisibility(View.VISIBLE);

      gui_vis_stop();
    }
    return (state);                                                     // No error
  }


  private void cb_tuner_stereo(boolean checked) {
    com_uti.logd("checked: " + checked);
    String val = "Stereo";
    if (!checked)
      val = "Mono";
    mApi.key_set("tuner_stereo", val);
  }

  private void cb_audio_stereo(boolean checked) {
    com_uti.logd("checked: " + checked);
    String val = "Stereo";
    if (!checked)
      val = "Mono";
    mApi.key_set("audio_stereo", val);
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