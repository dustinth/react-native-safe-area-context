package com.th3rdwave.safeareacontext;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

public class NavigationUtils {
  /**
   * 判断是否是全面屏
   */
  private volatile static boolean mHasCheckAllScreen;
  private volatile static boolean mIsAllScreenDevice;

  public static boolean isAllScreenDevice(Context context) {
    if (mHasCheckAllScreen) {
      return mIsAllScreenDevice;
    }
    mHasCheckAllScreen = true;
    mIsAllScreenDevice = false;
    // 低于 API 21的，都不会是全面屏。。。
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      return false;
    }
    try {
      WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      if (windowManager != null) {
        Display display = context.getDisplay();
        Point point = new Point();
        display.getRealSize(point);
        float width, height;
        if (point.x < point.y) {
          width = point.x;
          height = point.y;
        } else {
          width = point.y;
          height = point.x;
        }
        if (height / width >= 1.97f) {
          mIsAllScreenDevice = true;
        }
      }
      return mIsAllScreenDevice;
    } catch (Throwable e) {
      return false;
    }
  }
}
