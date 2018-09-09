package fm.a2d.sf.helper;

import android.util.Log;

import java.lang.reflect.Method;

/**
 * vlad805 (c) 2018
 */
class AudioSystem {

  private static final String TAG = ReflectionUtil.class.getSimpleName();
  private static Class<?> audioSystem = null;

  private Class<?> getAudioSystem() {
    if (audioSystem == null) {
      try {
        audioSystem = ReflectionUtil.getClass("android.media.AudioSystem");
      } catch (Exception e) {
        Log.e(TAG, "getAudioSystem failed: " + e.toString());
      }
    }
    return audioSystem;
  }

  void setDeviceConnectionState(int device, int state, String address, String device_name) {
    try {
      Method setDeviceConnectionState = ReflectionUtil.getMethod(getAudioSystem(), "setDeviceConnectionState", Integer.TYPE, Integer.TYPE, String.class, String.class);
      ReflectionUtil.invoke(getAudioSystem(), null, setDeviceConnectionState, device, state, address, device_name);
    } catch (Exception e) {
      Log.e(TAG, "setDeviceConnectionState failed: " + e.toString());
    }
  }

  int getDeviceConnectionState(int device, String address) {
    try {
      Method getDeviceConnectionState = ReflectionUtil.getMethod(getAudioSystem(), "getDeviceConnectionState", Integer.TYPE, String.class);
      return (Integer) ReflectionUtil.invoke(getAudioSystem(), 0, getDeviceConnectionState, device, address);
    } catch (Exception e) {
      Log.e(TAG, "getDeviceConnectionState failed: " + e.toString());
      return 0;
    }
  }

  int getConstantIntValue(String constantName) {
    try {
      return (Integer) ReflectionUtil.getDeclaredField(getAudioSystem(), constantName, 0, Integer.TYPE);
    } catch (Exception e) {
      Log.e(TAG, "getConstantValue failed: " + e.toString());
      return 0;
    }
  }
}