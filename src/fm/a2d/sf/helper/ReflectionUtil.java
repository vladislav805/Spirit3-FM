package fm.a2d.sf.helper;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * vlad805 (c) 2018
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class ReflectionUtil {
  private static final String TAG = ReflectionUtil.class.getSimpleName();

  static Class<?> getClass(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  static Method getMethod(Class<?> targetClass, String name, Class<?>... parameterTypes) {
    Method method = null;
    if (!(targetClass == null || name.isEmpty())) {
      try {
        method = targetClass.getMethod(name, parameterTypes);
      } catch (SecurityException | NoSuchMethodException e) {
        Log.e(TAG, e.toString());
      }
    }
    return method;
  }

  public static Field getField(Class<?> targetClass, String name) {
    Field field = null;
    if (!(targetClass == null || name.isEmpty())) {
      try {
        field = targetClass.getField(name);
      } catch (SecurityException | NoSuchFieldException e) {
        Log.e(TAG, e.toString());
      }
    }
    return field;
  }

  public static Constructor<?> getConstructor(Class<?> targetClass, Class<?>... parameterTypes) {
    Constructor<?> constructor = null;
    if (targetClass != null) {
      try {
        constructor = targetClass.getConstructor(parameterTypes);
      } catch (SecurityException | NoSuchMethodException e) {
        Log.e(TAG, e.toString());
      }
    }
    return constructor;
  }

  public static Object newInstance(Constructor<?> constructor, Object... args) {
    Object obj = null;
    if (constructor != null) {
      try {
        obj = constructor.newInstance(args);
      } catch (Exception e) {
        Log.e(TAG, "Exception in newInstance: " + e.getClass().getSimpleName());
      }
    }
    return obj;
  }

  static Object invoke(Object receiver, Object defaultValue, Method method, Object... args) {
    if (method != null) {
      try {
        defaultValue = method.invoke(receiver, args);
      } catch (Exception e) {
        Log.e(TAG, "Exception in invoke: " + e.getClass().getSimpleName());
      }
    }
    return defaultValue;
  }

  public static Object getFieldValue(Object receiver, Object defaultValue, Field field) {
    if (field != null) {
      try {
        defaultValue = field.get(receiver);
      } catch (Exception e) {
        Log.e(TAG, "Exception in getFieldValue: " + e.getClass().getSimpleName());
      }
    }
    return defaultValue;
  }

  public static void setFieldValue(Object receiver, Field field, Object value) {
    if (field != null) {
      try {
        field.set(receiver, value);
      } catch (Exception e) {
        Log.e(TAG, "Exception in setFieldValue: " + e.getClass().getSimpleName());
      }
    }
  }

  static Object getDeclaredField(Class<?> targetClass, String fieldName, Object defaultValue, Object returnType) {
    if (fieldName == null || fieldName.length() == 0) {
      return 0;
    }
    try {
      return targetClass.getDeclaredField(fieldName).get(returnType);
    } catch (Exception e) {
      Log.e(TAG, "Exception in getDeclaredField: " + e.getClass().getSimpleName() + " = " + e.toString());
      return defaultValue;
    }
  }
}