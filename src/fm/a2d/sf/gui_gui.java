package fm.a2d.sf;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import android.media.AudioManager;

import android.util.DisplayMetrics;
import android.os.Bundle;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import fm.a2d.sf.view.PresetView;

import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.ROTATION_Y;
import static android.view.View.TEXT_ALIGNMENT_CENTER;

// GUI


public class gui_gui implements gui_gap {

  private static int stat_constrs = 1;
  private int audio_stream = AudioManager.STREAM_MUSIC;

  private Activity m_gui_act = null;
  private Context m_context = null;
  private com_api m_com_api = null;

  private gui_vis m_gui_vis;
  private boolean gui_vis_disabled = true;

  private Typeface m_digital_font;


  // User Interface:
  private Animation m_ani_button = null;

  private LinearLayout m_lv_presets = null;

  // Text:
  private TextView m_tv_rssi = null;
  private TextView m_tv_state = null;
  private TextView m_tv_most = null;
  private TextView m_tv_band = null;
  private TextView m_tv_freq = null;

  // RDS data:
  private TextView m_tv_picl = null;
  private TextView m_tv_ps = null;
  private TextView m_tv_ptyn = null;
  private TextView m_tv_rt = null;

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

  private SeekBar m_sb_freq = null;

  // Presets
  private int m_presets_curr = 0;
  private PresetView[] m_preset_views;
  private String[] m_preset_freq;   // Frequencies
  private String[] m_preset_name;   // Names


  private int pixel_width = 480;
  private int pixel_height = 800;
  private float mDensity = 1.5f;


  // Dial:                                    !!!! Dial power bounce problem, accidental 2+ hit
  private android.os.Handler delay_dial_handler = null;
  private Runnable delay_dial_runnable = null;

  private gui_dia m_dial = null;
  private long last_rotate_time = 0;

  private double freq_at_210 = 85200;
  private double freq_percent_factor = 251.5;

  private int last_dial_freq = -1;

  // Color:
  private int lite_clr = Color.WHITE;
  private int dark_clr = Color.GRAY;
  private int blue_clr = Color.BLUE;

  private Dialog intro_dialog = null;

  private String last_rt = "";
  private int last_int_audio_sessid = 0;


  // Code:

  public gui_gui(Context c, com_api the_com_api) { // Constructor
    com_uti.logd("stat_constrs: " + stat_constrs++);

    m_context = c;
    m_gui_act = (Activity) c;
    m_com_api = the_com_api;

    m_preset_freq = new String[com_api.PRESET_COUNT];
    m_preset_name = new String[com_api.PRESET_COUNT];

    for (int i = 0; i < com_api.PRESET_COUNT; ++i) {
      m_preset_name[i] = "";
      m_preset_freq[i] = "";
    }
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

    DisplayMetrics dm = new DisplayMetrics();
    m_gui_act.getWindowManager().getDefaultDisplay().getMetrics(dm);
    pixel_width = dm.widthPixels;
    pixel_height = dm.heightPixels;
    mDensity = m_context.getResources().getDisplayMetrics().density;
    com_uti.logd("pixel_width: " + pixel_width + "  pixel_height: " + pixel_height + "  mDensity: " + mDensity);


    m_gui_act.requestWindowFeature(Window.FEATURE_NO_TITLE); // No title to save screen space
    m_gui_act.setContentView(R.layout.gui_gui_layout); // Main Layout

/*  Programmatic harder than XML (?? Should do before setContentView !! ??

//    LinearLayout main_linear_layout =  (LinearLayout) m_gui_act.findViewById (R.id.main_hll);
    LinearLayout                            gui_pg1_layout          =  (LinearLayout) m_gui_act.findViewById (R.id.gui_pg1_layout);
    //FrameLayout                             new_frame_layout        = (FrameLayout) m_gui_act.findViewById (R.id.new_fl);//new FrameLayout (m_context);
    FrameLayout                             new_frame_layout        = new FrameLayout (m_context);
    FrameLayout.LayoutParams new_frame_layout_params = new android.widget.FrameLayout.LayoutParams ((int) (pixel_width / mDensity), ViewGroup.LayoutParams.MATCH_PARENT);
    //com_uti.loge ("gui_pg1_layout: " + gui_pg1_layout + "  new_frame_layout_params: " + new_frame_layout_params);
    new_frame_layout.addView (gui_pg1_layout, new_frame_layout_params);
//  To:     new_frame_layout    FrameLayout  View       with new_frame_layout_params FrameLayout.LayoutParams
//  add:    gui_pg1_layout   LinearLayout View

    LinearLayout gui_pg2_layout =  (LinearLayout) m_gui_act.findViewById (R.id.gui_pg2_layout);
    FrameLayout                 old_frame_layout = (FrameLayout) m_gui_act.findViewById (R.id.old_fl);//new FrameLayout (m_context);
    FrameLayout.LayoutParams    old_frame_layout_params = new android.widget.FrameLayout.LayoutParams ((int) (pixel_width / mDensity), ViewGroup.LayoutParams.MATCH_PARENT);
    old_frame_layout.addView (gui_pg2_layout, old_frame_layout_params);

//    main_linear_layout.addView (gui_pg1_layout);
//    main_linear_layout.addView (gui_pg2_layout);
*/

    m_gui_act.findViewById(R.id.new_fl).setLayoutParams(new LayoutParams(pixel_width, ViewGroup.LayoutParams.MATCH_PARENT));

    dial_init();

    m_digital_font = Typeface.createFromAsset(m_context.getAssets(), "fonts/digital-number.ttf");

    m_ani_button = AnimationUtils.loadAnimation(m_context, R.anim.ani_button);// Set button animation

    m_tv_rssi = (TextView) m_gui_act.findViewById(R.id.tv_rssi);
    m_tv_state = (TextView) m_gui_act.findViewById(R.id.tv_state);  // Phase
    m_tv_most = (TextView) m_gui_act.findViewById(R.id.tv_most);

    m_tv_band = (TextView) m_gui_act.findViewById(R.id.tv_band);

    m_tv_picl = (TextView) m_gui_act.findViewById(R.id.tv_picl);
    m_tv_ps = (TextView) m_gui_act.findViewById(R.id.tv_ps);
    m_tv_ptyn = (TextView) m_gui_act.findViewById(R.id.tv_ptyn);
    m_tv_rt = (TextView) m_gui_act.findViewById(R.id.tv_rt);

    m_iv_seekdn = (ImageView) m_gui_act.findViewById(R.id.iv_seekdn);
    m_iv_seekdn.setOnClickListener(click_lstnr);

    m_iv_seekup = (ImageView) m_gui_act.findViewById(R.id.iv_seekup);
    m_iv_seekup.setOnClickListener(click_lstnr);

    m_iv_record = (ImageView) m_gui_act.findViewById(R.id.iv_record);
    m_iv_record.setOnClickListener(click_lstnr);

    m_iv_prev = (ImageView) m_gui_act.findViewById(R.id.iv_prev);
    m_iv_prev.setOnClickListener(click_lstnr);


    m_iv_next = (ImageView) m_gui_act.findViewById(R.id.iv_next);
    m_iv_next.setOnClickListener(click_lstnr);


    m_tv_freq = (TextView) m_gui_act.findViewById(R.id.tv_freq);
    m_tv_freq.setOnClickListener(click_lstnr);

    m_iv_paupla = (ImageView) m_gui_act.findViewById(R.id.iv_playpause);
    m_iv_paupla.setOnClickListener(click_lstnr);


    m_iv_stop = (ImageView) m_gui_act.findViewById(R.id.iv_stop);
    m_iv_stop.setOnClickListener(click_lstnr);


    m_iv_pause = (ImageView) m_gui_act.findViewById(R.id.iv_pause);
    m_iv_pause.setOnClickListener(click_lstnr);


    m_iv_mute = (ImageView) m_gui_act.findViewById(R.id.iv_mute);
    m_iv_mute.setOnClickListener(click_lstnr);


    m_iv_menu = (ImageView) m_gui_act.findViewById(R.id.iv_menu);
    m_iv_menu.setOnClickListener(click_lstnr);


    m_iv_signal = (ImageView) m_gui_act.findViewById(R.id.iv_signal);
    m_lv_presets = (LinearLayout) m_gui_act.findViewById(R.id.preset_list);

    m_tv_freq.setTypeface(m_digital_font);
    m_tv_rssi.setTypeface(m_digital_font);

    m_sb_freq = (SeekBar) m_gui_act.findViewById(R.id.sb_freq_seek);
    m_sb_freq.setMax(205);
    m_sb_freq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

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

        String freq = String.valueOf(current / 10.0f);

        freq_set(freq);
      }
    });



    try {
      lite_clr = Color.parseColor("#ffffffff"); // lite like PS
      dark_clr = Color.parseColor("#ffa3a3a3"); // grey like RT
      blue_clr = Color.parseColor("#ff32b5e5"); // ICS Blue
    } catch (Throwable e) {
      e.printStackTrace();
    }
    m_tv_rt.setTextColor(lite_clr);
    m_tv_ps.setTextColor(lite_clr);

    preset_setup();

    gui_pwr_update(false);

    long curr_time = com_uti.ms_get();
    long radio_gui_first_time = com_uti.long_get(com_uti.prefs_get(m_context, "radio_gui_first_time", ""));
    if (radio_gui_first_time <= 0L) {
      radio_gui_first_time = curr_time;
      com_uti.prefs_set(m_context, "radio_gui_first_time", "" + curr_time);
    }

    int radio_gui_start_count = com_uti.prefs_get(m_context, "radio_gui_start_count", 0);
    radio_gui_start_count++;
    com_uti.prefs_set(m_context, "radio_gui_start_count", radio_gui_start_count);


    if (radio_gui_start_count <= 1) {                                   // If first 1 runs...
      com_uti.loge("Setting band EU");
      tuner_band_set_non_volatile("EU");

      m_gui_act.showDialog(DLG_INTRO);                                 // Show the intro dialog
    } else {
      String band = com_uti.prefs_get(m_context, "tuner_band", "EU");
      tuner_band_set(band);

      m_gui_act.showDialog(DLG_POWER);                                 // Show the power dialog
      //starting_gui_dlg_show (true);
    }

    m_com_api.key_set("audio_state", "start");                         // Start audio service

    gui_pwr_update(true); // !!!! Move later to Radio API callback

    visual_state_load_prefs();
    audio_output_load_prefs();
    audio_stereo_load_prefs();
    tuner_stereo_load_prefs();

    return (true);
  }


  private void setFrequencyText(String s) {
    if (s.length() == 4) {
      s = " " + s;
    }
    m_tv_freq.setText(s);

  }


  private void dial_init() {

    // Dial Relative Layout:
    RelativeLayout freq_dial_relative_layout = (RelativeLayout) m_gui_act.findViewById(R.id.freq_dial);
    android.widget.RelativeLayout.LayoutParams lp_dial = new android.widget.RelativeLayout.LayoutParams(android.widget.RelativeLayout.LayoutParams.MATCH_PARENT, android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);   // WRAP_CONTENT
    //int dial_size = (pixel_width * 3) / 4;
    int dial_size = (pixel_width * 7) / 8;
    m_dial = new gui_dia(m_context, R.drawable.freq_dial_needle, -1, dial_size, dial_size); // Get dial instance/RelativeLayout view
    lp_dial.addRule(RelativeLayout.CENTER_IN_PARENT);
    freq_dial_relative_layout.addView(m_dial, lp_dial);

    // Dial internal Power Relative Layout:
    android.widget.RelativeLayout.LayoutParams lp_power;
    lp_power = new android.widget.RelativeLayout.LayoutParams(android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT, android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
    lp_power.addRule(RelativeLayout.CENTER_IN_PARENT);

    m_iv_pwr = new ImageView(m_context);
    m_iv_pwr.setImageResource(R.drawable.dial_power_off);
    freq_dial_relative_layout.addView(m_iv_pwr, lp_power);

    m_dial.listener_set(new gui_dia.gui_dia_listener() {                      // Setup listener for state_chngd() and dial_chngd()

      public boolean prev_go() {
        if (!m_com_api.tuner_state.equalsIgnoreCase("start")) {
          com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
          return (false);                                               // Not Consumed
        }

        m_com_api.key_set("tuner_freq", "down");
        return (true);                                                  // Consumed
      }

      public boolean next_go() {
        if (!m_com_api.tuner_state.equalsIgnoreCase("start")) {
          com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
          return (false);                                               // Not Consumed
        }

        m_com_api.key_set("tuner_freq", "up");
        return (true);                                                  // Consumed
      }

      public boolean state_chngd() {
        com_uti.logd("via gui_dia m_com_api.audio_state: " + m_com_api.audio_state);
        if (m_com_api.audio_state.equalsIgnoreCase("start"))
          m_com_api.key_set("tuner_state", "stop");
        else
          m_com_api.key_set("audio_state", "start");
        return (true);                                                  // Consumed
      }

      public boolean freq_go() {
        com_uti.logd("via gui_dia");
        if (!m_com_api.tuner_state.equalsIgnoreCase("start")) {
          com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
          return (false);                                               // Not Consumed
        }
        if (last_dial_freq < 87500 || last_dial_freq > 108000)
          return (false);                                               // Not Consumed
        m_com_api.key_set("tuner_freq", "" + last_dial_freq);
        return (true);                                                  // Consumed
      }

      private int last_dial_freq = 0;

      public boolean dial_chngd(double angle) {
        if (!m_com_api.tuner_state.equalsIgnoreCase("start")) {
          com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
          return (false);                                               // Not Consumed
        }
        long curr_time = com_uti.ms_get();
        int freq = dial_freq_get(angle);
        com_uti.logd("via gui_dia angle: " + angle + "  freq: " + freq);
        freq += 25;
        freq /= 50;
        freq *= 50;
        if (freq < 87500 || freq > 108000)
          return (false);                                               // Not Consumed
        freq = com_uti.tnru_freq_enforce(freq);
        com_uti.logd("via gui_dia freq: " + freq + "  curr_time: " + curr_time + "  last_rotate_time: " + last_rotate_time);
        dial_freq_set(freq);   // !! Better to set fast !!
//        if (last_rotate_time >= 0 || last_rotate_time + 1000 > curr_time) {
//          com_uti.logd ("via gui_dia NOW freq set");
//          last_rotate_time = com_uti.ms_get ();
//          m_com_api.key_set ("tuner_freq", "" + freq);
        last_dial_freq = freq;
//        }
//        else
//          com_uti.logd ("via gui_dia DELAY freq set");

        if (delay_dial_handler != null) {
          if (delay_dial_runnable != null)
            delay_dial_handler.removeCallbacks(delay_dial_runnable);
        } else
          /*android.os.Handler*/ delay_dial_handler = new android.os.Handler();

        delay_dial_runnable = new Runnable() {

          //delay_dial_handler.postDelayed (new Runnable () {
          public void run() {
            m_com_api.key_set("tuner_freq", "" + last_dial_freq);
          }
          //}, 200);    // 0.2 second delay
        };

        delay_dial_handler.postDelayed(delay_dial_runnable, 50);

        return true; // Consumed
      }
    });

    m_dial.setVisibility(GONE);
  }

/*  Original top  =   0 degrees =  97.0 MHz
    ""       right=  90 degrees = 104.6 MHz
    ""       left = 270 degrees =  89.2 MHz

90 deg = 7.8
90 deg = 7.6

85 - 109    -> 87.5 - 108, middle = 97.75

Rotate counter by 0.75 MHz = 8.766 degrees

*/


  private int dial_freq_get(double angle) {
    double percent = (angle + 150) / 3;
    return ((int) (freq_percent_factor * percent + freq_at_210));
  }


  private void dial_freq_set(int freq) {
    if (last_dial_freq == freq)                                         // Optimize
      return;
    if (m_dial == null)
      return;
    last_dial_freq = freq;
    double percent = (freq - freq_at_210) / freq_percent_factor;
    double angle = (percent * 3) - 150;
    m_dial.dial_angle_set(angle);


    int val = (freq / 100) - 875;
    m_sb_freq.setProgress(val);
  }


  // Visualizer:

  private void gui_vis_stop() {
    try {
      com_uti.logd("m_gui_vis: " + this.m_gui_vis);
      if (this.m_gui_vis != null) {
        this.m_gui_vis.vis_stop();
        this.m_gui_vis = null;
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void gui_vis_start(int audio_sessid) {
    try {
      com_uti.logd("m_gui_vis: " + m_gui_vis + "  audio_sessid: " + audio_sessid);
      m_gui_vis = (gui_vis) m_gui_act.findViewById(R.id.gui_vis);
      if (m_gui_vis == null) {
        com_uti.loge("m_gui_vis == null");
      } else {
        m_gui_vis.vis_start(audio_sessid);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private String visualizer_state_set(String state) {
    com_uti.logd("state: " + state);
    if (state.equals("Start")) {
      this.gui_vis_disabled = false;
      ((LinearLayout) this.m_gui_act.findViewById(R.id.vis)).setVisibility(0);
      this.m_dial.setVisibility(4);
      this.m_iv_pwr.setVisibility(4);
      ((ImageView) this.m_gui_act.findViewById(R.id.frequency_bar)).setVisibility(4);
      int audio_sessid = com_uti.int_get(this.m_com_api.audio_sessid);
      if (audio_sessid > 0) {
        do_gui_vis_start(audio_sessid);
      }
    } else {
      this.gui_vis_disabled = true;
      ((LinearLayout) this.m_gui_act.findViewById(R.id.vis)).setVisibility(4);
      this.m_dial.setVisibility(0);
      this.m_iv_pwr.setVisibility(0);
      ((ImageView) this.m_gui_act.findViewById(R.id.frequency_bar)).setVisibility(0);
      gui_vis_stop();
    }
    com_uti.prefs_set(this.m_context, "gui_visualizer_state", state);
    return state;
  }

  private static final String TAG = "GUI_GUI";


  private void preset_setup() {
    m_preset_views = new PresetView[com_api.PRESET_COUNT];
    for (int idx = 0; idx < m_preset_views.length; idx++) { // For all presets...
      String name = com_uti.prefs_get(m_context, "radio_name_prst_" + idx, "");
      String freq = com_uti.prefs_get(m_context, "radio_freq_prst_" + idx, "");

      Log.i(TAG, "preset_setup: for index " + idx);

      m_preset_views[idx] = new PresetView(m_context);

      m_preset_views[idx].populate(idx, freq).setListeners(mOnClickPresetListener, mOnLongClickPresetListener);

      m_preset_freq[idx] = freq;
      m_preset_name[idx] = name;

      m_lv_presets.addView(m_preset_views[idx]);
    }

    com_uti.logd("m_presets_curr: " + m_presets_curr);
  }


  private void text_default() {
    m_tv_state.setText("");
    m_tv_most.setText("");
    m_tv_band.setText("");

    m_tv_rssi.setText("");
    m_tv_ps.setText("");
    m_tv_picl.setText("");
    m_tv_ptyn.setText("");
    m_tv_rt.setText("");
    m_tv_rt.setSelected(true);      // Need for marquis
    //m_tv_rt.setText     ("");
  }


  // Dialog methods:

  private static final int DLG_INTRO = 1;    // First time show this
  private static final int DLG_POWER = 2;    // Every subsequent startup show this
  private static final int DLG_FREQ_SET = 3;
  private static final int DLG_MENU = 4;

  public Dialog gap_dialog_create(int id, Bundle args) {                            // Create a dialog by calling specific *_dialog_create function    ; Triggered by showDialog (int id);
    //public DialogFragment gap_dialog_create (int id, Bundle args) {
    //com_uti.logd ("id: " + id + "  args: " + args);
    Dialog ret = null;
    //DialogFragment ret = null;
    switch (id) {
      case DLG_INTRO:
      case DLG_POWER:
        ret = intro_dialog_create();
        intro_dialog = ret;
        break;
      case DLG_MENU:
        ret = menu_dialog_create();
        break;
      case DLG_FREQ_SET:
        ret = freq_set_dialog_create(id);
        break;
    }
    //com_uti.logd ("dialog: " + ret);
    return (ret);
  }



  private Dialog menu_dialog_create() {
    return new AlertDialog.Builder(m_context)
            .setTitle("Spirit2 " + com_uti.app_version_get(m_context))
            .setMessage("Select Support / Buy Spirit2")
            .create();
  }

  private Dialog intro_dialog_create() {
    AlertDialog.Builder dialog = new AlertDialog.Builder(m_context)
            .setTitle("Spirit2 " + com_uti.app_version_get(m_context));

    String intro_msg;

    if (!com_uti.file_get("/system/bin/su") && !com_uti.file_get("/system/xbin/su")) {
      intro_msg = "Sorry... \"NO SU\" Error. Spirit2 REQUIRES JB+ & ROOT & Qualcomm.\n\nThanks! :)";
    } else {
      intro_msg = "Welcome to Spirit2! :)\n\nPlease wait while it starts...";
    }

    dialog.setMessage(intro_msg);

    return dialog.create();
  }

  private void tuner_band_set(String band) {
    m_com_api.tuner_band = band;
    com_uti.tnru_band_set(band); // To setup band values; different process than service
    m_com_api.key_set("tuner_band", band);
  }

  private void tuner_band_set_non_volatile(String band) {
    tuner_band_set(band);
    m_com_api.key_set("tuner_band", band);
  }

  // Regional settings:
  private static final int m_freq_lo = 87500;
  private static final int m_freq_hi = 108000;

  // Set new frequency
  private Dialog freq_set_dialog_create(final int id) {
    com_uti.logd("id: " + id);
    LayoutInflater factory = LayoutInflater.from(m_context);
    AlertDialog.Builder dialog = new AlertDialog.Builder(m_context);

    View textEntryView = factory.inflate(R.layout.edit_number, null);

    final EditText freqEditView = (EditText) textEntryView.findViewById(R.id.edit_number);
    freqEditView.setTypeface(m_digital_font);
    freqEditView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
    freqEditView.setText(m_com_api.tuner_freq);

    return dialog
      .setTitle("Set Frequency")
      .setView(textEntryView)
      .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
          freq_set(freqEditView.getEditableText().toString().replace(",", "."));
        }
      })
      .setNegativeButton("Cancel"/*R.string.alert_dialog_cancel*/, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int whichButton) {
        }
      }).create();
  }

  private void freq_set(String nFreq) {

    // If an empty string...
    if (nFreq.isEmpty()) {
      com_uti.loge("nFreq: " + nFreq);
      return;
    }

    Float ffreq = 0f;
    try {
      ffreq = Float.valueOf(nFreq);
    } catch (Throwable e) {
      com_uti.loge("ffreq = Float.valueOf (nFreq); failed");
    }

    int freq = (int) (ffreq * 1000);

    if (freq < 0.1) {
      return;
    } else if (freq >= m_freq_lo * 10 && freq <= m_freq_hi * 10) {      // For 760 - 1080
      freq /= 10;
    } else if (freq >= m_freq_lo * 100 && freq <= m_freq_hi * 100) {    // For 7600 - 10800
      freq /= 100;
    } else if (freq >= m_freq_lo * 1000 && freq <= m_freq_hi * 1000) {  // For 76000 - 108000
      freq /= 1000;
    }
    if (freq >= m_freq_lo && freq <= m_freq_hi) {
      showToast(String.format(Locale.ENGLISH, "Frequency changing to: %.1f MHz", (float) (freq / 1000.0f)));
      m_com_api.key_set("tuner_freq", "" + freq);
    } else {
      com_uti.loge("Frequency invalid: " + ffreq);
      showToast("Invalid frequency");
    }
  }

  private void showToast(String text) {
    Toast.makeText(m_context, text, Toast.LENGTH_SHORT).show();
  }


  // Enables/disables buttons based on power
  private void gui_pwr_update(boolean pwr) {
    if (m_iv_pwr != null) {
      m_iv_pwr.setImageResource(pwr ? R.drawable.dial_power_on : R.drawable.dial_power_off);
    }

    if (!pwr) {
      // Set all displayable text fields to initial OFF defaults
      text_default();
    }

    // Power button is always enabled
    m_iv_seekup.setEnabled(pwr);
    m_iv_seekdn.setEnabled(pwr);
    m_iv_record.setEnabled(true);
    m_tv_rt.setEnabled(pwr);

    for (int idx = 0; idx < com_api.PRESET_COUNT; idx++) { // For all presets...
      if (m_preset_views[idx] != null) {
        m_preset_views[idx].setEnabled(pwr);
      }
    }
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
  public void gap_radio_update(Intent intent) {
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
    gui_pwr_update(m_com_api.tuner_state.equalsIgnoreCase("start"));

    //if (false) {
//    if (need_freq_result) {
    //if (radio_update_last_dial_freq

    // Freq Button:
    //int ifreq = com_uti.int_get (m_com_api.tuner_freq);
    //int ifreq = m_com_api.int_tuner_freq;

    int ifreq = (int) (com_uti.double_get(m_com_api.tuner_freq) * 1000);
    ifreq = com_uti.tnru_freq_fix(ifreq + 25);                       // Must fix due to floating point rounding need, else 106.1 = 106.099

//      if (last_dial_freq != ifreq) {
//        if (last_dial_freq_change_start_time + 500 > com_uti.ms_get ()) {
    String freq = null;
    if (ifreq >= 50000 && ifreq < 500000) {
      dial_freq_set(ifreq);
      freq = ("" + (double) ifreq / 1000);
    }
    if (freq != null) {
      setFrequencyText(freq);
    }
    //need_freq_result = false;
    //last_dial_freq_change_start_time = -1;
//        }
//        else if (last_dial_freq_change_start_time < 0)
//          last_dial_freq_change_start_time = com_uti.ms_get ();
//      }

//    }

    m_tv_rssi.setText(String.format("%4s", m_com_api.tuner_rssi));
    m_tv_band.setText(m_com_api.tuner_band);

    signal_stretch_set();

    if (m_com_api.tuner_most.equalsIgnoreCase("Mono"))
      m_tv_most.setText("");
    else if (m_com_api.tuner_most.equalsIgnoreCase("Stereo"))
      m_tv_most.setText("S");
    else
      m_tv_most.setText("");

    m_tv_state.setText("" + m_com_api.tuner_state + " " + m_com_api.audio_state);

    m_tv_picl.setText(m_com_api.tuner_rds_picl);

    m_tv_ps.setText(m_com_api.tuner_rds_ps);
    //m_tv_ps2.setText (m_com_api.tuner_rds_ps);

    //m_com_api.tuner_rds_ptyn = com_uti.tnru_rds_ptype_get (m_com_api.tuner_band, com_uti.int_get (m_com_api.tuner_rds_pt));
    m_tv_ptyn.setText(m_com_api.tuner_rds_ptyn);

    if (!last_rt.equalsIgnoreCase(m_com_api.tuner_rds_rt)) {
      //com_uti.loge ("rt changed: " + m_com_api.tuner_rds_rt);
      last_rt = m_com_api.tuner_rds_rt;
      m_tv_rt.setText(m_com_api.tuner_rds_rt);
      //m_tv_rt.setMarqueeRepeatLimit (-1);  // Forever
      m_tv_rt.setSelected(true);
    }

  }

  private int SIGNAL_EDGES[] = new int[] {50, 350, 750, 900};
  private int SIGNAL_RES[] = new int[] {R.drawable.ic_signal_0, R.drawable.ic_signal_1, R.drawable.ic_signal_2, R.drawable.ic_signal_3, R.drawable.ic_signal_4};

  private void signal_stretch_set() {
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
    } catch (Exception ignore) {}
  }


  // UI buttons and other controls

  private View.OnClickListener mOnClickPresetListener = new View.OnClickListener() { // Tune to preset
    public void onClick(View v) {

      int idx = ((PresetView) v).getIndex();



      /*for (int idx = 0; idx < com_api.max_presets; idx++) { // For all presets...
        if (v == m_preset_ib[idx]) { // If this preset...
          try {*/
            if (m_preset_freq[idx].isEmpty()) { // If no preset yet...
              preset_set(idx); // Set it
            } else {
              freq_set(m_preset_freq[idx]); // m_preset_tv[idx].getText();
            }
          /*} catch (Throwable e) {
            e.printStackTrace();
          }
          return;
        }
      }*/
    }
  };

  private void preset_set(int idx) {
    String freq_text = m_com_api.tuner_freq;

    m_preset_views[idx].populate(freq_text);
    m_preset_freq[idx] = m_com_api.tuner_freq;

    // !! Current implementation requires simultaneous (одновременно)
    m_com_api.key_set("radio_name_prst_" + idx, "" + freq_text, "radio_freq_prst_" + idx, m_com_api.tuner_freq);

    //m_preset_ib [idx].setEnabled (false);
    //m_preset_ib[idx].setImageResource(R.drawable.transparent);  // R.drawable.btn_preset
  }

  // Long click: Show preset change options
  private View.OnLongClickListener mOnLongClickPresetListener = new View.OnLongClickListener() {
    public boolean onLongClick(View v) {
      preset_set(((PresetView) v).getIndex());
      v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      return true; // Consume long click
    }
  };




  private View.OnClickListener click_lstnr = new View.OnClickListener() {
    public void onClick(View v) {

      com_uti.logd("view: " + v);

      if (v == m_iv_mute) {
        AudioManager m_am = (AudioManager) m_context.getSystemService(Context.AUDIO_SERVICE);
        if (m_am != null) {
          int svol = m_am.getStreamVolume(audio_stream);
          m_am.setStreamVolume(audio_stream, svol, AudioManager.FLAG_SHOW_UI);// Display volume change
        }
      } else if (v == m_iv_paupla) {
        /*if (m_com_api.audio_state.equalsIgnoreCase ("start"))
          m_com_api.key_set ("audio_state", "pause");
        else if (m_com_api.audio_state.equalsIgnoreCase ("pause"))
          m_com_api.key_set ("audio_state", "start");
        else
          m_com_api.key_set ("audio_state", "start");*/                 // Full power up like widget

        m_com_api.key_set("audio_state", "Toggle");
      } else if (v == m_iv_stop) {
        m_com_api.key_set("tuner_state", "Stop"); // Full power down/up

      } else if (v == m_iv_record) {
        m_com_api.key_set("audio_record_state", "Toggle");
      } else if (v == m_iv_out) {                                          // Speaker/headset  NOT USED NOW
        m_com_api.key_set("audio_output", "toggle");

      } else if (v == m_tv_freq) { // Frequency direct entry
        m_gui_act.showDialog(DLG_FREQ_SET);

      } else if (v == m_iv_menu) {
        m_gui_act.showDialog(DLG_MENU);

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


  // implements gui_dlg_lstnr :
/*
  public void on_pos () {
    com_uti.logd ("");
  }
  public void on_neu () {
    com_uti.logd ("");
  }
  public void on_neg () {
    com_uti.logd ("");
  }
*/
/*
  private gui_dlg start_dlg_frag = null;
  private gui_dlg stop_dlg_frag  = null;
  private boolean starting_gui_dlg_active = false;
  private boolean stopping_gui_dlg_active = false;

  private void starting_gui_dlg_show (boolean start) {
    //start_dlg_frag.dismiss ();    // Dismiss previous

    // DialogFragment.show() will take care of adding the fragment in a transaction.  We also want to remove any currently showing dialog, so make our own transaction and take care of that here.
    FragmentTransaction ft = m_gui_act.getFragmentManager ().beginTransaction ();
    Fragment prev = m_gui_act.getFragmentManager ().findFragmentByTag ("start_stop_dialog");
    if (prev != null) {
        ft.remove (prev);
    }
    ft.addToBackStack (null);

    if (start) {
      start_dlg_frag = gui_dlg.init (R.drawable.img_icon_128, m_com_api.radio_phase, m_com_api.radio_error, null, null, m_context.getString (android.R.string.cancel));//"", m_context.getString (android.R.string.ok), "", null);//"");
      start_dlg_frag.setgui_dlg_lstnr (this);//m_gui_act);
      starting_gui_dlg_active = true;
      start_dlg_frag.show (m_gui_act.getFragmentManager (), "start_stop_dialog");
      start_dlg_frag.show (ft, "start_stop_dialog");
    }
    else
      starting_gui_dlg_active = false;
  }
*/
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
/*
  private void stopping_gui_dlg_show (boolean start) {
    // DialogFragment.show() will take care of adding the fragment in a transaction.  We also want to remove any currently showing dialog, so make our own transaction and take care of that here.
    FragmentTransaction ft = m_gui_act.getFragmentManager ().beginTransaction ();
    Fragment prev = m_gui_act.getFragmentManager ().findFragmentByTag ("start_stop_dialog");
    if (prev != null) {
        ft.remove (prev);
    }
    ft.addToBackStack (null);

    if (start) {
      stop_dlg_frag = gui_dlg.init (android.R.drawable.stat_sys_headset, "Stopping", null, null, null, null);//"", m_context.getString (android.R.string.ok), "", null);//"");
      stop_dlg_frag.setgui_dlg_lstnr (this);//m_gui_act);
      stopping_gui_dlg_active = true;
      //stop_dlg_frag.show (m_gui_act.getFragmentManager (), "start_stop_dialog");
      stop_dlg_frag.show (ft, "start_stop_dialog");
    }
    else
      stopping_gui_dlg_active = false;
  }
*/


  private String tuner_stereo_load_prefs() {
    String value = com_uti.prefs_get(m_context, "tuner_stereo", "");
    return (value);
  }

  private String audio_stereo_load_prefs() {
    String value = com_uti.prefs_get(m_context, "audio_stereo", "");
    return (value);
  }

  private String audio_output_load_prefs() {
    String value = com_uti.prefs_get(m_context, "audio_output", "");
    return (value);
  }

  private String audio_output_set_nonvolatile(String value) {  // Called only by speaker/headset checkbox change
    com_uti.logd("value: " + value);
    m_com_api.key_set("audio_output", value);
    return (value);                                                     // No error
  }


  private String visual_state_load_prefs() {
    String pref = com_uti.prefs_get(m_context, "visual_state", "");
    String ret = "";
    if (!pref.equals("")) {
      ret = visual_state_set(pref);

    }
    return (ret);
  }

  private String visual_state_set_nonvolatile(String state) {
    String ret = visual_state_set(state);
    com_uti.prefs_set(m_context, "visual_state", state);
    return (ret);
  }

  private String visual_state_set(String state) {
    com_uti.logd("state: " + state);
    if (state.equalsIgnoreCase("Start")) {
      gui_vis_disabled = false;

      m_dial.setVisibility(View.INVISIBLE);
      m_iv_pwr.setVisibility(View.INVISIBLE);
      ((ImageView) m_gui_act.findViewById(R.id.frequency_bar)).setVisibility(View.INVISIBLE);

      int audio_sessid = com_uti.int_get(m_com_api.audio_sessid);
      if (audio_sessid > 0)
        do_gui_vis_start(audio_sessid);
    } else {
      gui_vis_disabled = true;

      m_dial.setVisibility(View.VISIBLE);
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

  private void cb_af(boolean checked) {
    com_uti.logd("checked: " + checked);
    if (checked)
      m_com_api.key_set("tuner_rds_af_state", "Start");
    else
      m_com_api.key_set("tuner_rds_af_state", "Stop");
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