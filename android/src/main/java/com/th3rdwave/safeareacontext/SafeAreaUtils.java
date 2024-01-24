package com.th3rdwave.safeareacontext;

import android.app.Service;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* package */ class SafeAreaUtils {

  private static boolean hasHuaweiNotchIn(Context context) {
    boolean ret = false;
    try {
      ClassLoader cl = context.getClassLoader();
      Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
      Method get = HwNotchSizeUtil.getMethod("hasNotchInScreen");
      ret = (boolean) get.invoke(HwNotchSizeUtil);
    } catch (ClassNotFoundException e) {
      Log.e("AskSky", "hasNotchInScreen ClassNotFoundException");
    } catch (NoSuchMethodException e) {
      Log.e("AskSky", "hasNotchInScreen NoSuchMethodException");
    } catch (Exception e) {
      Log.e("AskSky", "hasNotchInScreen Exception");
    }
    return ret;
  }

  private static boolean isVivoNotchScreen(Context context) {
    ClassLoader classLoader = context.getClassLoader();
    try {
      Class mClass = classLoader.loadClass("android.util.FtFeature");
      Method mMethod = mClass.getMethod("isFeatureSupport", Integer.TYPE);
      return (boolean) mMethod.invoke(mClass, 0x00000020);
    } catch (ClassNotFoundException e) {
      return false;
    } catch (NoSuchMethodException e) {
      return false;
    } catch (IllegalAccessException e) {
      return false;
    } catch (InvocationTargetException e) {
      return false;
    }
  }


  private static boolean isOppoNotchScreen(Context context) {
    return context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
  }

  private static int getCommonNotchHeight(Context context) {
    int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
    int statusBarHeight = -1;
    if (resId > 0) {
      statusBarHeight = context.getResources().getDimensionPixelSize(resId);
    }

    if (statusBarHeight < 0) {
      int result = 0;
      try {
        Class<?> clazz = Class.forName("com.android.internal.R$dimen");
        Object obj = clazz.newInstance();
        Field field = clazz.getField("status_bar_height");
        int resourceId = Integer.parseInt(field.get(obj).toString());
        result = context.getResources().getDimensionPixelSize(resourceId);
      } catch (Exception e) {
      } finally {
        statusBarHeight = result;
      }
    }

    //Use 25dp if no status bar height found
    if (statusBarHeight < 0) {
      statusBarHeight = dip2px(context, 25);
    }
    return statusBarHeight;
  }

  private static int dip2px(Context context, float dpValue) {
    float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dpValue * scale + 0.5f);
  }

  private static int getHuaweiNotchHeight(Context context) {
    int[] ret = new int[]{0, 0};
    try {
      ClassLoader cl = context.getClassLoader();
      Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
      Method get = HwNotchSizeUtil.getMethod("getNotchSize");
      ret = (int[]) get.invoke(HwNotchSizeUtil);
    } catch (ClassNotFoundException e) {
    } catch (NoSuchMethodException e) {
    } catch (Exception e) {
    } finally {
      return ret[1];
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  private static boolean isNotchScreen(View rootView) {
    WindowInsets windowInsets = rootView.getRootWindowInsets();
    if (windowInsets == null) {
      return false;
    }

    DisplayCutout displayCutout = windowInsets.getDisplayCutout();
    if (displayCutout == null || displayCutout.getBoundingRects() == null) {
      return false;
    }
    return true;
  }

  @RequiresApi(api = Build.VERSION_CODES.P)
  private static int getPNotchHeight(View rootView) {
    int notchHeight = 0;
    WindowInsets windowInsets = rootView.getRootWindowInsets();
    if (windowInsets == null) {
      return 0;
    }

    DisplayCutout displayCutout = windowInsets.getDisplayCutout();
    if (displayCutout == null || displayCutout.getBoundingRects() == null) {
      return 0;
    }

    notchHeight = displayCutout.getSafeInsetTop();

    return notchHeight;
  }

  private static int getNotchHeight(View rootView) {
    Context context = rootView.getContext();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if (isNotchScreen(rootView)) {
        return getPNotchHeight(rootView);
      }
      return 0;
    }
    // 小米
    int resourceId = context.getResources().getIdentifier("notch_height", "dimen", "android");
    if (resourceId > 0) {
      return context.getResources().getDimensionPixelSize(resourceId);
    }
    // 华为
    if (hasHuaweiNotchIn(context)) {
      return getHuaweiNotchHeight(context);
    }
    // OPPO
    if (isOppoNotchScreen(context) || isVivoNotchScreen(context)) {
      return getCommonNotchHeight(context);
    }
    return 0;
  }

  private static int getNavigationHeight(Context context) {
    if (context == null) {
      return 0;
    }
    Resources resources = context.getResources();
    int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
    int height = 0;
    if (resourceId > 0) {
      //获取NavigationBar的高度
      height = resources.getDimensionPixelSize(resourceId);
    }
    return height;
  }

  // 这里只判断NavigationBar高度为0时, 再次确认是否开启了手势导航或者隐藏了导航栏;
  private static boolean isSoftNavigationBarShow(Context context, float topStatusBarHeight) {
    String checkSettingName = "navigationbar_is_min";
    String brand = Build.BRAND;
    if (brand == null || brand.equals("")) {
      brand = Build.MANUFACTURER;
    }
    if (brand != null) {
      brand = brand.toLowerCase();
      if (brand.contains("huawei")) {
        checkSettingName = "navigationbar_is_min";
      } else if (brand.contains("xiaomi")) {
        checkSettingName = "force_fsg_nav_bar";
      } else if (brand.equalsIgnoreCase("vivo")) {
        checkSettingName = "navigation_gesture_on";
      } else if (brand.equalsIgnoreCase("oppo")) {
        checkSettingName = "hide_navigationbar_enable";
      } else {
        checkSettingName = "navigationbar_is_min";
      }
    }
    int navigationBarIsMin = 0;
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      navigationBarIsMin = Settings.System.getInt(context.getContentResolver(), checkSettingName, 0);
    } else {
      // VIVO 或者 OPPO
      if ("navigation_gesture_on".equals(checkSettingName) || "hide_navigationbar_enable".equals(checkSettingName)) {
        navigationBarIsMin = Settings.Secure.getInt(context.getContentResolver(), checkSettingName, 0);
      } else {
        try {
          return isHasNavigationBar(context, topStatusBarHeight);
        } catch (Throwable e) {
          return false;
        }
      }
    }
    return navigationBarIsMin == 0;
  }

  private static boolean isHasNavigationBar(Context context, float topStatusBarHeight) {
    WindowManager windowManager = (WindowManager) context.getSystemService(Service.WINDOW_SERVICE);
    Display d = windowManager.getDefaultDisplay();

    DisplayMetrics realDisplayMetrics = new DisplayMetrics();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
      d.getRealMetrics(realDisplayMetrics);
    }
    int realHeight = realDisplayMetrics.heightPixels;
    int realWidth = realDisplayMetrics.widthPixels;

    DisplayMetrics displayMetrics = new DisplayMetrics();
    d.getMetrics(displayMetrics);
    int displayHeight = displayMetrics.heightPixels;
    int displayWidth = displayMetrics.widthPixels;

    // 部分无良厂商的手势操作，显示高度 + 导航栏高度，竟然大于物理高度，对于这种情况，直接默认未启用导航栏
    if (displayHeight + getNavigationBarHeight(context) > realHeight) return false;

    return realWidth - displayWidth > 0 || (realHeight - displayHeight > 0 && Math.abs(realHeight - displayHeight - topStatusBarHeight) > 0.01);
  }

  private static int getNavigationBarHeight(Context context) {
    int result = 0;
    Resources resources = context.getResources();
    int resourceId =
      resources.getIdentifier("navigation_bar_height", "dimen", "android");
    if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId);
    return result;
  }

  private static @Nullable
  EdgeInsets getRootWindowInsetsCompat(View rootView) {
    Context context = rootView.getContext();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      WindowInsets insets = rootView.getRootWindowInsets();
      if (insets == null) {
        return null;
      }
      float top = insets.getSystemWindowInsetTop();
      if (top <= 0) {
        top = getNotchHeight(rootView);
      }
      float bottom = Math.min(insets.getSystemWindowInsetBottom(), insets.getStableInsetBottom());
      if (bottom <= 0 && NavigationUtils.isAllScreenDevice(rootView.getContext())) {
        if (isSoftNavigationBarShow(context, top)) {
          int softNavigationBarBottom = getNavigationHeight(context);
          bottom = softNavigationBarBottom > 0 ? softNavigationBarBottom : bottom;
        }
      }
      return new EdgeInsets(
        top,
        insets.getSystemWindowInsetRight(),
        // System insets are more reliable to account for notches but the
        // system inset for bottom includes the soft keyboard which we don't
        // want to be consistent with iOS. Using the min value makes sure we
        // never get the keyboard offset while still working with devices that
        // hide the navigation bar.
        bottom,
        insets.getSystemWindowInsetLeft());
    } else {
      Rect visibleRect = new Rect();
      rootView.getWindowVisibleDisplayFrame(visibleRect);
      float top = visibleRect.top;
      if (top <= 0) {
        top = getNotchHeight(rootView);
      }
      float bottom = rootView.getHeight() - visibleRect.bottom;
      if (bottom <= 0 && NavigationUtils.isAllScreenDevice(rootView.getContext())) {
        // 适当的加入一点bottom, 避免太沉底不好看
        bottom = (float) ((top + 0.0) / 2);
        if (isSoftNavigationBarShow(context, top)) {
          int softNavigationBarBottom = getNavigationHeight(context);
          bottom = softNavigationBarBottom > 0 ? softNavigationBarBottom : bottom;
        }
      }
      return new EdgeInsets(
        top,
        rootView.getWidth() - visibleRect.right,
        bottom,
        visibleRect.left);
    }
  }

  static @Nullable
  EdgeInsets getSafeAreaInsets(View view) {
    // The view has not been layout yet.
    if (view.getHeight() == 0) {
      return null;
    }
    View rootView = view.getRootView();
    EdgeInsets windowInsets = getRootWindowInsetsCompat(rootView);
    if (windowInsets == null) {
      return null;
    }

    // Calculate the part of the view that overlaps with window insets.
    float windowWidth = rootView.getWidth();
    float windowHeight = rootView.getHeight();
    Rect visibleRect = new Rect();
    view.getGlobalVisibleRect(visibleRect);

    windowInsets.top = Math.max(windowInsets.top - visibleRect.top, 0);
    windowInsets.left = Math.max(windowInsets.left - visibleRect.left, 0);
    windowInsets.bottom = Math.max(Math.min(visibleRect.top + view.getHeight() - windowHeight, 0) + windowInsets.bottom, 0);
    windowInsets.right = Math.max(Math.min(visibleRect.left + view.getWidth() - windowWidth, 0) + windowInsets.right, 0);
    return windowInsets;
  }

  static @Nullable
  com.th3rdwave.safeareacontext.Rect getFrame(ViewGroup rootView, View view) {
    // This can happen while the view gets unmounted.
    if (view.getParent() == null) {
      return null;
    }
    Rect offset = new Rect();
    view.getDrawingRect(offset);
    try {
      rootView.offsetDescendantRectToMyCoords(view, offset);
    } catch (IllegalArgumentException ex) {
      // This can throw if the view is not a descendant of rootView. This should not
      // happen but avoid potential crashes.
      ex.printStackTrace();
      return null;
    }

    return new com.th3rdwave.safeareacontext.Rect(offset.left, offset.top, view.getWidth(), view.getHeight());
  }
}
