package com.baidu.swan.videoplayer;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.cloud.videoplayer.demo.BuildConfig;

/**
 * 视频工具类
 *
 * @author Emercy
 * @since 2019-01-14
 */
public class VideoUtils {

    /** debug tag */
    private static final String TAG = "VideoUtils";
    /** debug 开关 */
    private static final boolean DEBUG = BuildConfig.DEBUG;

    /**
     * 从父布局中移除该view
     *
     * @param view 待移除的view对象
     * @return 是否移除成功
     */
    public static boolean removeFromParent(View view) {
        if (view != null) {
            if (view.getParent() != null && view.getParent() instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view.getParent();
                if (vg.indexOfChild(view) != -1) {
                    try {
                        vg.removeView(view);
                    } catch (Exception ex) {
                        if (DEBUG) {
                            ex.printStackTrace();
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将view添加到decorView上
     *
     * @param activity 当前activity
     * @param view     待添加的view
     * @return 结果
     */
    public static boolean attachDecor(Activity activity, View view) {
        if (activity != null && view != null) {
            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            if (decorView != null) {
                removeFromParent(view);
                decorView.removeView(view);
                decorView.addView(view);
                return true;
            }
        }
        return false;
    }
}
