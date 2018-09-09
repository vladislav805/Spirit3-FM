package fm.a2d.sf.helper;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

@SuppressWarnings({"deprecation", "unused", "SameParameterValue"})
public class AudioHelper {
  private static final String TAG = "AudioHelper";

  private AudioSystem _audioSystem = null;
  private Context _context;

  private AudioSystem audioSystem() {
    if (this._audioSystem == null) {
      this._audioSystem = new AudioSystem();
    }
    return this._audioSystem;
  }

  public AudioHelper(Context inContext) {
    _context = inContext;
  }

  public void makeSpeaker() {
    makeSpeakerAvailable(false);
    makeBluetoothA2dpAvailable();
    makeWiredHeadphonesAvailable(false, false);
    makeWiredHeadsetAvailable();
    makeAuxDigitalAvailable();
    makeDockDigitalAvailable();
    makeDockAnalogAvailable();
    makeBluetoothScoAvailable(true, true);
    makeSpeakerAvailable(true);
  }

  public void makeWiredHeadPhones() {
    makeWiredHeadphonesAvailable(true, false);
    makeBluetoothA2dpAvailable();
    makeAuxDigitalAvailable();
    makeWiredHeadsetAvailable();
    makeWiredHeadphonesAvailable(true, true);
  }

  public void makeEarpiece() {
    makeSpeakerAvailable(false);
    makeBluetoothA2dpAvailable();
    makeBluetoothScoAvailable(false, false);
    makeWiredHeadphonesAvailable(false, false);
    makeWiredHeadsetAvailable();
    makeAuxDigitalAvailable();
    makeDockDigitalAvailable();
    makeDockAnalogAvailable();
    setEarpieceForceUsed(true);
  }


  private void makeBluetoothScoAvailable(boolean value, boolean mic) {
    String address = "";
    if (value) {
      makeAudioAvailable(Phone.DEVICE_OUT_BLUETOOTH_SCO, true, false, address);
    } else {
      makeAudioAvailable(Phone.DEVICE_OUT_BLUETOOTH_SCO, false, false, address);
      makeAudioAvailable(Phone.DEVICE_OUT_BLUETOOTH_SCO_HEADSET, false, false, address);
      makeAudioAvailable(Phone.DEVICE_OUT_BLUETOOTH_SCO_CARKIT, false, false, address);
    }
    if (mic) {
      makeAudioAvailable(Phone.DEVICE_IN_BLUETOOTH_SCO_HEADSET, value, false, address);
    }
    audioManager().isBluetoothScoOn();
  }

  private void makeBluetoothA2dpAvailable() {
    String address = "";
    makeAudioAvailable(Phone.DEVICE_OUT_BLUETOOTH_A2DP, false, false, address);
    makeAudioAvailable(Phone.DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES, false, false, address);
    makeAudioAvailable(Phone.DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER, false, false, address);
    audioManager().setBluetoothA2dpOn(false);
    audioManager().isBluetoothA2dpOn();
  }

  private AudioManager audioManager(Context context) {
    if (context != null) {
      return (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    if (getContext() != null) {
      return (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }
    return null;
  }

  private AudioManager audioManager() {
    return audioManager(getContext());
  }

  public Context getContext() {
    if (this._context == null || this._context.getApplicationContext() == null) {
      return this._context;
    }
    return this._context.getApplicationContext();
  }

  public void setContext(Context value) {
    if (value == null) {
      this._context = null;
    } else if (value.getApplicationContext() != null) {
      this._context = value.getApplicationContext();
    } else {
      this._context = value;
    }
  }




  private void makeAuxDigitalAvailable() {
    makeAudioAvailable("DEVICE_OUT_AUX_DIGITAL", false, false, "");
  }

  private void makeWiredHeadsetAvailable() {
    makeAudioAvailable(Phone.DEVICE_OUT_WIRED_HEADSET, false, false, "");
    makeAudioAvailable(Phone.DEVICE_IN_WIRED_HEADSET, false, false, "");
  }

  private boolean isAudioAvailable(String sKey) {
    return getAudioConstantIntValue(sKey) != 0 && isAudioAvailable(getAudioConstantIntValue(sKey));
  }

  private boolean isAudioAvailable(int iKey) {
    return audioSystem().getDeviceConnectionState(iKey, "") == 1;
  }

  private int getAudioConstantIntValue(String sName) {
    return audioSystem().getConstantIntValue(sName);
  }

  public boolean isWiredHeadsetAvailable() {
    return isAudioAvailable(Phone.DEVICE_OUT_WIRED_HEADSET);
  }

  private void makeWiredHeadphonesAvailable(boolean value, boolean bReset) {
    makeAudioAvailable(Phone.DEVICE_OUT_WIRED_HEADPHONE, value, bReset, "");
  }

  public boolean isWiredHeadphonesAvailable() {
    return isAudioAvailable(Phone.DEVICE_OUT_WIRED_HEADPHONE);
  }

  private void makeSpeakerAvailable(boolean bReset) {
    makeAudioAvailable(Phone.DEVICE_OUT_SPEAKER, true, bReset, "");
  }

  public boolean isSpeakerAvailable() {
    return isAudioAvailable(Phone.DEVICE_OUT_SPEAKER);
  }

  public void makeEarpieceAvailable(boolean value, boolean bReset) {
    makeAudioAvailable(Phone.DEVICE_OUT_EARPIECE, value, bReset, "");
  }

  public boolean isEarpieceAvailable() {
    return isAudioAvailable(Phone.DEVICE_OUT_EARPIECE);
  }

  private void makeDockDigitalAvailable() {
    makeAudioAvailable(Phone.DEVICE_OUT_DGTL_DOCK_HEADSET, false, false, "");
  }

  private void makeDockAnalogAvailable() {
    makeAudioAvailable(Phone.DEVICE_OUT_ANLG_DOCK_HEADSET, false, false, "");
  }

  private void makeAudioAvailable(String sKey, boolean bValue, boolean bReset, String sAddress) {
    if (getAudioConstantIntValue(sKey) != 0) {
      makeAudioAvailable(getAudioConstantIntValue(sKey), bValue, bReset, sAddress);
    }
  }

  private void makeAudioAvailable(int iKey, boolean bValue, boolean bReset, String sAddress) {
    if (iKey != 0) {
      if (bValue) {
        if (bReset && audioSystem().getDeviceConnectionState(iKey, sAddress) == getAudioConstantIntValue("DEVICE_STATE_AVAILABLE")) {
          audioSystem().setDeviceConnectionState(iKey, getAudioConstantIntValue("DEVICE_STATE_UNAVAILABLE"), sAddress, "");
        }
        if (audioSystem().getDeviceConnectionState(iKey, sAddress) == getAudioConstantIntValue("DEVICE_STATE_UNAVAILABLE")) {
          audioSystem().setDeviceConnectionState(iKey, getAudioConstantIntValue("DEVICE_STATE_AVAILABLE"), sAddress, "");
        }
      } else if (bReset || audioSystem().getDeviceConnectionState(iKey, sAddress) == getAudioConstantIntValue("DEVICE_STATE_AVAILABLE")) {
        audioSystem().setDeviceConnectionState(iKey, getAudioConstantIntValue("DEVICE_STATE_UNAVAILABLE"), sAddress, "");
      }
    }
  }

  private void setEarpieceForceUsed(boolean value) {
    Log.d(TAG, " setEarpieceForceUsed  = " + value);
    //if (!isInCall()) {
      Log.d(TAG, "setEarpieceForceUsed()   SetMode Before =  " + audioManager().getMode());
      if (value) {
        if (audioManager().getMode() <= 0) {
          audioManager().setMode(2);
        }
      } else if (audioManager().getMode() != 0) {
        audioManager().setMode(0);
      }
      Log.d(TAG, "setEarpieceForceUsed()   SetMode After =  " + audioManager().getMode());
    //}
  }


}