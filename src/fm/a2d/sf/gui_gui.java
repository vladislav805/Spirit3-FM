package fm.a2d.sf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import fm.a2d.sf.helper.L;
import fm.a2d.sf.helper.Utils;
import fm.a2d.sf.view.FrequencySeekView;
import fm.a2d.sf.view.PresetView;
import fm.a2d.sf.view.VisualizerView;

import java.util.Locale;

import static android.view.View.TEXT_ALIGNMENT_CENTER;

public class gui_gui implements View.OnClickListener, View.OnLongClickListener {

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
  private Typeface mDigitalFont;

  private LinearLayout mViewListPresets = null;

  // Info
  private TextView mViewRSSI = null;
  private TextView mViewStereo = null;
  private TextView mViewFrequency = null;
  private TextView mViewRecordDuration = null;
  private TextView mViewName = null;

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

  private ImageView mViewAudioOut = null; // ImageView for Speaker/Headset toggle
  private ImageView mViewSignal = null;

  // Seek frequency line
  private HorizontalScrollView mViewLineFrequency = null;
  private FrequencySeekView mViewSeekFrequency = null;

  // Presets
  private PresetView[] mPresetViews;



//  private String last_rt = "";
  private int mLastAudioSessionId = 0;

  private void log(String s) {
    L.w(L.T.GUI, s);
  }

  // Code:
  public gui_gui(Context c, com_api the_com_api) { // Constructor
    log("GUI created");

    mContext = c;
    mActivity = (Activity) c;
    mApi = the_com_api;
  }

  // Lifecycle API

  public boolean setState(String state) {
    log("GUI set state = " + state);
    boolean ret = false;
    if (state.equals("start"))
      ret = gui_start();
    else if (state.equals("stop"))
      ret = gui_stop();
    return ret;
  }

  private boolean gui_stop() {
    log("GUI stop");
    if (mVisualizerDisabled)
      log("gui_stop: mVisualizerDisabled = true");
    else
      gui_vis_stop();
    return true;
  }


  private boolean gui_start() {
    log("GUI initialize views...");

    // !! Hack for s2d comms to allow network activity on UI thread
    com_uti.strict_mode_set(false);

    mActivity.requestWindowFeature(Window.FEATURE_NO_TITLE); // No title to save screen space
    mActivity.setContentView(R.layout.activity_main); // Main Layout

    mDisplayMetrics = new DisplayMetrics();
    mActivity.getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
    mActivity.findViewById(R.id.main_wrap).setLayoutParams(new LinearLayout.LayoutParams(mDisplayMetrics.widthPixels, ViewGroup.LayoutParams.MATCH_PARENT));

    mDigitalFont = Typeface.createFromAsset(mContext.getAssets(), "fonts/digital-number.ttf");

    mViewRSSI = (TextView) mActivity.findViewById(R.id.tv_rssi);
    mViewStereo = (TextView) mActivity.findViewById(R.id.tv_most);

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
    mViewFrequency.setOnLongClickListener(this);

    mViewPlayToggle = (ImageView) mActivity.findViewById(R.id.iv_play_toggle);
    mViewPlayToggle.setOnClickListener(this);
    mViewPlayToggle.setOnLongClickListener(this);

    /*mViewMute = (ImageView) mActivity.findViewById(R.id.iv_mute);
    mViewMute.setOnClickListener(this);*/

    mViewSignal = (ImageView) mActivity.findViewById(R.id.iv_signal);
    mViewListPresets = (LinearLayout) mActivity.findViewById(R.id.preset_list);
    mViewLineFrequency = (HorizontalScrollView) mActivity.findViewById(R.id.seek_scroll_frequency);

    mViewSeekFrequency = (FrequencySeekView) mActivity.findViewById(R.id.sb_freq_seek);
    mViewSeekFrequency.setMinMaxValue(875, 1080)
                      .setOnSeekBarChangeListener(mOnSeekFrequencyChanged);

    mViewAudioOut = (ImageView) mActivity.findViewById(R.id.iv_audio_out);
    mViewAudioOut.setOnClickListener(this);

    mViewFrequency.setTypeface(mDigitalFont);
    mViewRSSI.setTypeface(mDigitalFont);

    mViewName = (TextView) mActivity.findViewById(R.id.curr_name_station);

    setupPresets();

    updateUIViewsByPowerState(false);

    if (!Utils.hasPref(mContext, C.GUI_START_FIRST_TIME)) {
      Utils.setPrefLong(mContext, C.GUI_START_FIRST_TIME, System.currentTimeMillis());
    }

    if (Utils.hasPref(mContext, C.TUNER_FREQUENCY)) {
      setFrequencyText(Utils.getPrefInt(mContext, C.TUNER_FREQUENCY) / 1000f);
    }

    int startGuiCount = Utils.getPrefInt(mContext, C.GUI_START_COUNT, 0);
    Utils.setPrefInt(mContext, C.GUI_START_COUNT, ++startGuiCount);

    boolean autoStart = Utils.getPrefBoolean(mContext, C.PREFERENCE_AUTO_START, true);

    if (autoStart) {
      setTunerBand(Utils.getPrefString(mContext, C.TUNER_BAND, "EU"));

      ((MainActivity) mActivity).openDialogIntro();

      Utils.sendIntent(mContext, C.AUDIO_STATE, C.AUDIO_STATE_START); // Start audio service
    }

    updateUIViewsByPowerState(autoStart); // !!!! Move later to Radio API callback

    loadPreferenceVisualState();
    /*audio_output_load_prefs();
    audio_stereo_load_prefs();
    tuner_stereo_load_prefs();*/

    log("initialize views successfully");

    return true;
  }

  private SeekBar.OnSeekBarChangeListener mOnSeekFrequencyChanged = new SeekBar.OnSeekBarChangeListener() {

    private Double current;

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (!fromUser) {
        return;
      }

      current = mViewSeekFrequency.getValue() / 10;

      setFrequencyText(current);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) { }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
      if (!mApi.isTunerStarted()) {
        return; // Not Consumed
      }

      Double d = current * 1000;

      setFrequency(d.intValue());
    }
  };

  /**
   * Enables/disables buttons based on power
   */
  private void updateUIViewsByPowerState(boolean power) {
    if (!power) {
      // Set all displayable text fields to initial OFF defaults
      resetAllViews();
    }

    mViewFrequency.setShadowLayer(power ? 20 : 0, 0, 0, mContext.getResources().getColor(R.color.primary_blue_shadow));

    int primaryColor = mContext.getResources().getColor(R.color.primary_blue);
    if (!power) {
      primaryColor = (primaryColor & 0x00ffffff) | (0x88 << 24);
    }

    float alpha = power ? 1f : .6f;

    mViewFrequency.setTextColor(primaryColor);

    // Power button is always enabled
    mViewPrevious.setEnabled(power);
    mViewNext.setEnabled(power);
    mViewSeekUp.setEnabled(power);
    mViewSeekDown.setEnabled(power);
    mViewRecord.setEnabled(power);
    mViewSeekFrequency.setEnabled(power);

    mViewPrevious.setAlpha(alpha);
    mViewNext.setAlpha(alpha);
    mViewSeekDown.setAlpha(alpha);
    mViewSeekUp.setAlpha(alpha);
    mViewRecord.setAlpha(alpha);
    mViewSignal.setAlpha(alpha);
    mViewAudioOut.setAlpha(alpha);
    mViewName.setAlpha(alpha);
    mViewSeekFrequency.setAlpha(alpha);
    mViewRSSI.setAlpha(alpha);


    for (int idx = 0; idx < com_api.PRESET_COUNT; idx++) { // For all presets...
      if (mPresetViews[idx] != null) {
        mPresetViews[idx].setEnabled(power);
      }
    }
  }


  // Visualizer:

  private void gui_vis_start(int audio_sessid) {
    try {
      log("DEPRECATED: gui_vis_start / m_gui_vis: " + mVisualizerView + " audio_sessid: " + audio_sessid);
      mVisualizerView = (VisualizerView) mActivity.findViewById(R.id.gui_vis);
      if (mVisualizerView != null) {
        mVisualizerView.vis_start(audio_sessid);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void gui_vis_stop() {
    try {
      log("DEPRECATED: gui_vis_stop / m_gui_vis: " + mVisualizerView);
      if (mVisualizerView != null) {
        mVisualizerView.vis_stop();
        mVisualizerView = null;
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void visualizer_state_set(String state) {
    log("DEPRECATED: visualizer_state_set(" + state + ")");
    if (state.equalsIgnoreCase("Start")) {
      mVisualizerDisabled = false;
      mActivity.findViewById(R.id.vis).setVisibility(View.VISIBLE);
      int audio_sessid = com_uti.int_get(mApi.audio_sessid);
      //if (audio_sessid > 0) {
        do_gui_vis_start(audio_sessid);
      //}
    } else {
      mVisualizerDisabled = true;
      mActivity.findViewById(R.id.vis).setVisibility(View.INVISIBLE);
      gui_vis_stop();
    }
    com_uti.prefs_set(mContext, "gui_visualizer_state", state);
  }


  private void setupPresets() {
    log("setup presets...");
    mPresetViews = new PresetView[C.PRESET_COUNT];
    for (int idx = 0; idx < mPresetViews.length; idx++) { // For all presets...
      int freq = Utils.getPrefInt(mContext, C.PRESET_KEY + idx, 0);
      String title = Utils.getPrefString(mContext, C.PRESET_KEY_NAME + idx, "");
      mPresetViews[idx] = new PresetView(mContext);
      mPresetViews[idx].populate(idx, freq, title).setListeners(mOnClickPresetListener, mMenuPresetListener);
      mViewListPresets.addView(mPresetViews[idx]);
    }
    log("setup presets successfully");
  }


  private void resetAllViews() {
    mViewStereo.setText("");
    mViewRSSI.setText("0");
//    m_tv_ps.setText("");
//    m_tv_picl.setText("");
//    m_tv_ptyn.setText("");
//    m_tv_rt.setText("");
//    m_tv_rt.setSelected(true); // Need for marquis
//    m_tv_rt.setText("");
  }



  /**
   * Изменение частотного диапазона
   */
  private void setTunerBand(String band) {
    log("GUI setTunerBand: " + band);
    com_uti.setTunerBand(band); // To setup band values; different process than service
    //Utils.sendIntent(mContext, C.TUNER_BAND, band);
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
    freqEditView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
    freqEditView.setText(String.valueOf(mApi.getIntFrequencyKHz() / 1000f));

    freqEditView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    freqEditView.setGravity(Gravity.CENTER_HORIZONTAL);

    dialog
        .setTitle(mContext.getString(R.string.dialog_frequency_title))
        .setView(textEntryView)
        .setPositiveButton(mContext.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            String mhzStr = freqEditView.getEditableText().toString().replace(",", ".");
            try {
              Double mhz = Double.valueOf(mhzStr) * 1000;
              setFrequency(mhz.intValue());
            } catch (NumberFormatException e) {
              showToast(mContext.getString(R.string.frequency_invalid_format));
            }
          }
        })
        .setNegativeButton(mContext.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
          }
        }).create().show();
  }

  /**
   * Изменение текущей частоты
   */
  private void setFrequency(int freq) {
    log("Request set frequency = " + freq);
    // If an empty string...
    if (freq == 0) {
      log("frequency == 0");
      return;
    }
    log("setFrequency: " + freq);

    if (freq >= mFrequencyLow && freq <= mFrequencyHigh) {
      // If tuner disabled...
      if (!mApi.isTunerStarted()) {
        log("Set frequency failed because tuner is off");
      }

      Utils.sendIntent(mContext, C.TUNER_FREQUENCY, String.valueOf(freq));

      float f = freq / 1000f;
      showToast(mContext.getString(R.string.toast_frequency_changed, f));
    } else {
      log("setFrequency invalid: " + freq);
      showToast(mContext.getString(R.string.toast_frequency_invalid));
    }
  }

  private void setFrequencyText(Number i) {
    mViewFrequency.setText(String.format(Locale.ENGLISH, "%5.1f", i.floatValue()));
  }

  /**
   * Событие изменения частоты
   */
  private void onFrequencyChanged(float frequency) {
    log("onFrequencyChanged(" + frequency + ")");

    setFrequencyText(frequency);

    int val = (int) (frequency * 10 - 875);
    mViewSeekFrequency.setProgress(val);

    int x = (mViewLineFrequency.getChildAt(0).getWidth() * val / 205) - (mDisplayMetrics.widthPixels / 2);

    mViewLineFrequency.smoothScrollTo(x, 0);

    PresetView currentPreset = null;
    for (PresetView pv : mPresetViews) {
      if (!pv.isEmpty() && pv.getFrequency() == (int) (frequency * 1000)) {
        currentPreset = pv;
        break;
      }
    }

    mViewName.setText(currentPreset != null ? currentPreset.getTitle() : "");
  }

  private Toast mToast;

  /**
   * Показ сообщения пользователю
   */
  @SuppressLint("ShowToast")
  private void showToast(String txt) {
    if (mToast == null) {
      mToast = Toast.makeText(mContext, txt, Toast.LENGTH_SHORT);
    } else {
      mToast.setText(txt);
    }

    mToast.show();
  }





  //
  private void do_gui_vis_start(int audio_sessid) {
    log("DEPRECATED do_gui_vis_start / audio_sessid: " + audio_sessid);
    if (mVisualizerDisabled) {
      log("DEPRECATED mVisualizerDisabled = true");
    } else {
      gui_vis_start(audio_sessid);
    }
  }

  private int mLastFrequency;
  private String mLastRecord;
  private long mLastRecordStart;

  private boolean mJustStarted = true;

  public void onRestartTuner() {
    mJustStarted = true;
  }

  // Radio API Callback:
  public void onReceivedUpdates(Intent intent) {
    // Audio Session ID:

    log("GUI onReceivedUpdates: " + intent);
    int audio_sessid = com_uti.int_get(mApi.audio_sessid);
    if (audio_sessid != 0 && mLastAudioSessionId != audio_sessid) { // If audio session ID has changed...
      mLastAudioSessionId = audio_sessid;
      log("onRU: api.audio_sid: " + mApi.audio_sessid + "; new audio_sid: " + audio_sessid);

      // If no session, do nothing (or stop visual and EQ)
      do_gui_vis_start(audio_sessid);
    }

    if (mApi.audio_state.equals(C.AUDIO_STATE_START) && mJustStarted) {
      ((MainActivity) mActivity).hideIntroDialog();
      setFrequency(mApi.getIntFrequencyKHz());
      mJustStarted = false;
    }

    if (mLastFrequency != mApi.getIntFrequencyKHz()) {
      mLastFrequency = mApi.getIntFrequencyKHz();
      onFrequencyChanged(mLastFrequency / 1000f);
    }


    updateUIViewsByPowerState(mApi.isTunerStarted());
    setPlayToggleButtonState(mApi.audio_state);
    setRecordAudioState(mApi.audio_record_state);
    updateSignalStretch(mApi.getRssi());

    if (mApi.audio_output.equals(C.AUDIO_OUTPUT_SPEAKER)) {
      mViewAudioOut.setImageResource(R.drawable.ic_speaker);
    } else {
      mViewAudioOut.setImageResource(R.drawable.ic_headset);
    }

    /*if (!mApi.audio_record_state.equals(mLastRecord)) {

      switch (mApi.audio_record_state.toLowerCase()) {
        case C.RECORD_STATE_START:
          mLastRecordStart = com_uti.ms_get();
          mViewRecordDuration.setVisibility(View.VISIBLE);
          updateTimeStringRecording();
          break;

        case C.RECORD_STATE_STOP:
          mViewRecordDuration.setVisibility(View.GONE);
          break;
      }
    } else if (mApi.audio_record_state.equals(C.RECORD_STATE_START)) {
      updateTimeStringRecording();
    }*/



    /*if (mApi.tuner_most.equalsIgnoreCase("Mono"))
      mViewStereo.setText("");
    else if (mApi.tuner_most.equalsIgnoreCase("Stereo"))
      mViewStereo.setText("S");
    else
      mViewStereo.setText("");*/


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

  private void updateTimeStringRecording() {
    Long s = com_uti.ms_get() - mLastRecordStart;
    mViewRecordDuration.setText(Utils.getTimeStringBySeconds(s.intValue()));
  }

  private void setPlayToggleButtonState(String state) {
    switch (state.toLowerCase()) {
      case C.AUDIO_STATE_STARTING:
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
    if (state.equals(C.RECORD_STATE_START)) {
      Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.anim_recording);
      mViewRecord.setImageResource(R.drawable.ic_record_press);
      mViewRecord.startAnimation(animation);
    } else {
      mViewRecord.setImageResource(R.drawable.ic_record);
      mViewRecord.clearAnimation();
    }
  }

  private int[] SIGNAL_EDGES = new int[] {15, 25, 35, 45};
  private int[] SIGNAL_RES = new int[] {R.drawable.ic_signal_0, R.drawable.ic_signal_1, R.drawable.ic_signal_2, R.drawable.ic_signal_3, R.drawable.ic_signal_4};

  /**
   * Обновление уровня сигнала
   */
  private void updateSignalStretch(int f) {
    try {
      int resId = SIGNAL_RES[4];

      for (int i = 0; i < SIGNAL_EDGES.length; ++i) {
        if (f < SIGNAL_EDGES[i]) {
          resId = SIGNAL_RES[i];
          break;
        }
      }

      mViewSignal.setImageResource(resId);
      mViewRSSI.setText(String.format(Locale.ENGLISH, "%3d", mApi.tuner_rssi));
    } catch (Exception ignore) {}
  }


  /**
   * UI buttons and other controls
   */
  private View.OnClickListener mOnClickPresetListener = new View.OnClickListener() { // Tune to preset
    public void onClick(View v) {
      PresetView preset = (PresetView) v;

      if (preset.isEmpty()) { // If no preset yet...
        setPreset(preset, mApi.getIntFrequencyKHz(), "");
      } else {
        setFrequency(preset.getFrequency());
      }
    }
  };

  /**
   * Save preset in memory
   */
  private void setPreset(PresetView preset, int frequency, String title) {
    preset.populate(frequency, title);

    Utils.setPrefInt(mContext, C.PRESET_KEY + preset.getIndex(), frequency);
    Utils.setPrefString(mContext, C.PRESET_KEY_NAME + preset.getIndex(), title);
    Utils.sendIntent(mContext, C.EVENT_PRESET_UPDATED, "1");
  }

  /**
   * Long click: Show preset change options
   */
  private PresetView.OnMenuPresetSelected mMenuPresetListener = new PresetView.OnMenuPresetSelected() {
    public void onClick(int action, PresetView v) {
      switch (action) {
        case PresetView.MENU_CREATE:
          if (v.isEmpty()) {
            openRenamePresetDialog(v, mApi.getIntFrequencyKHz());
          } else {
            setPreset(v, mApi.getIntFrequencyKHz(), v.getTitle());
          }
          break;

        case PresetView.MENU_REPLACE:
          setPreset(v, mApi.getIntFrequencyKHz(), null);
          break;

        case PresetView.MENU_REMOVE:
          setPreset(v, 0, null);
          break;

        case PresetView.MENU_RENAME:
          openRenamePresetDialog(v);
          break;
      }
    }
  };

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
     /* case R.id.iv_mute:
        AudioManager m_am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (m_am != null) {
          // Display volume change
          m_am.setStreamVolume(AudioManager.STREAM_MUSIC, m_am.getStreamVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI);
        }
        break;*/

      case R.id.iv_play_toggle:
        mApi.key_set(C.AUDIO_STATE, C.AUDIO_STATE_TOGGLE);
        break;

      case R.id.iv_record:
        mApi.key_set(C.RECORD_STATE, C.RECORD_STATE_TOGGLE);
        break;

      case R.id.iv_audio_out: //-> m_iv_out ???? / TODO: Speaker/headset  NOT USED NOW
        mApi.key_set(C.AUDIO_OUTPUT, C.AUDIO_OUTPUT_TOGGLE);
        break;

      case R.id.tv_freq:
        openDialogChangeFrequency();
        break;

      case R.id.iv_seekdn:
        mApi.key_set(C.TUNER_SCAN_STATE, C.TUNER_SCAN_DOWN);
        break;

      case R.id.iv_seekup:
        mApi.key_set(C.TUNER_SCAN_STATE, C.TUNER_SCAN_UP);
        break;

      case R.id.iv_prev:
        mApi.key_set(C.TUNER_FREQUENCY, C.TUNER_FREQUENCY_DOWN);
        break;

      case R.id.iv_next:
        mApi.key_set(C.TUNER_FREQUENCY, C.TUNER_FREQUENCY_UP);
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
        Utils.sendIntent(mContext, C.TUNER_STATE, C.TUNER_STATE_STOP);
        return true;

      case R.id.tv_freq:
        mActivity.startActivityForResult(new Intent(mContext, PrefActivity.class), 0);
        return true;

    }
    return false;
  }

  private void openRenamePresetDialog(final PresetView v) {
    openRenamePresetDialog(v, v.getFrequency());
  }

  private void openRenamePresetDialog(final PresetView v, final int frequency) {
    final LayoutInflater factory = LayoutInflater.from(mContext);
    final AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
    String title = v.getTitle();

    final View textEntryView = factory.inflate(R.layout.edit_number, null);

    if (title == null) {
      title = "";
    }

    final EditText et = (EditText) textEntryView.findViewById(R.id.edit_number);

    et.setFilters(new InputFilter[] {new InputFilter.LengthFilter(C.PRESET_NAME_MAX_LENGTH)});
    et.setText(title);
    et.setSelection(title.length(), title.length());

    et.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
    et.setGravity(Gravity.CENTER_HORIZONTAL);

    ab.setTitle(R.string.preset_rename_title)
        .setView(textEntryView)
        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            setPreset(v, frequency, et.getText().toString().trim());
          }
        })
        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
          }
        })
        .create()
        .show();

  }


  private String tuner_stereo_load_prefs() {
    return com_uti.prefs_get(mContext, "tuner_stereo", "");
  }

  private String audio_stereo_load_prefs() {
    return com_uti.prefs_get(mContext, "audio_stereo", "");
  }

  private String audio_output_load_prefs() {
    return com_uti.prefs_get(mContext, C.AUDIO_OUTPUT, "");
  }

  private String audio_output_set_nonvolatile(String value) {  // Called only by speaker/headset checkbox change
    log("DEPRECATED: audio_output_set_nonvolatile / value: " + value);
    mApi.key_set(C.AUDIO_OUTPUT, value);
    return value; // No error
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
    log("GUI: setVisualState(" + state + ")");
    if (state.equalsIgnoreCase("Start")) {
      mVisualizerDisabled = false;

      int audio_sessid = com_uti.int_get(mApi.audio_sessid);
      if (audio_sessid > 0)
        do_gui_vis_start(audio_sessid);
    } else {
      mVisualizerDisabled = true;

      gui_vis_stop();
    }
    return state; // No error
  }

  public void onClickView(View view) {

    int id = view.getId();
    switch (id) {
      case R.id.cb_visu:
        boolean is = ((CheckBox) view).isChecked();
        visualizer_state_set(is ? "Start" : "stop");
        break;
    }
  }
}
