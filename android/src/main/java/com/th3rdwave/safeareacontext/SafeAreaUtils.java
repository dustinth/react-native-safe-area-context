package com.th3rdwave.safeareacontext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;

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

    private static boolean isXiaoMiNavigationBarShow(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) != 0) {
                //开启手势，不显示虚拟键
                return false;
            }
        }
        return true;
    }

    private static @Nullable
    EdgeInsets getRootWindowInsetsCompat(View rootView) {
        Context context = rootView.getContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WindowInsets insets = rootView.getRootWindowInsets();
            if (insets == null) {
                return null;
            }
            int top = insets.getSystemWindowInsetTop();
            if (top <= 0) {
                top = getNotchHeight(rootView);
            }
            int bottom = Math.min(insets.getSystemWindowInsetBottom(), insets.getStableInsetBottom());
            if (bottom <= 0 && NavigationUtils.isAllScreenDevice(rootView.getContext())) {
                // 适当的加入一点bottom, 避免太沉底不好看
                bottom = top / 2;
                if (isXiaoMiNavigationBarShow(context)) {
                    int xiaomiBottom = getNavigationHeight(context);
                    bottom = xiaomiBottom > 0 ? xiaomiBottom : bottom;
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
            int top = visibleRect.top;
            if (top <= 0) {
                top = getNotchHeight(rootView);
            }
            int bottom = rootView.getHeight() - visibleRect.bottom;
            if (bottom <= 0 && NavigationUtils.isAllScreenDevice(rootView.getContext())) {
                // 适当的加入一点bottom, 避免太沉底不好看
                bottom = top / 2;
                if (isXiaoMiNavigationBarShow(context)) {
                    int xiaomiBottom = getNavigationHeight(context);
                    bottom = xiaomiBottom > 0 ? xiaomiBottom : bottom;
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
