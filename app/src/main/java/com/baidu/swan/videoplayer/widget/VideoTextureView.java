package com.baidu.swan.videoplayer.widget;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.baidu.cloud.videoplayer.demo.BuildConfig;

/**
 * 用于绘制视频帧的TextureView
 *
 * @author machao10
 * @since 2019-01-09
 */
public class VideoTextureView extends TextureView {

    /** debug开关 */
    private static final boolean DEBUG = BuildConfig.DEBUG;
    /** DEBUG TAG */
    private static final String TAG = "VideoTextureView";

    /**
     * 视频TextureView构造器
     *
     * @param context 上下文
     */
    public VideoTextureView(Context context) {
        super(context);
    }

    /**
     * 视频TextureView构造器
     *
     * @param context 上下文
     * @param attrs   布局参数
     */
    public VideoTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * 视频TextureView构造器
     *
     * @param context      上下文
     * @param attrs        布局参数
     * @param defStyleAttr 布局style
     */
    public VideoTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(TAG, "onDetachedFromWindow");
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(TAG, "onAttachedToWindow");
        }
    }

    /**
     * 释放TextView的资源
     */
    public void release() {
        if (DEBUG) {
            Log.d(TAG, "release TextureView");
        }
        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if (surfaceTexture != null) {
            getSurfaceTexture().release();
        }
    }

}
