package com.baidu.swan.videoplayer.callback;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.baidu.swan.videoplayer.BuildConfig;
import com.baidu.swan.videoplayer.SwanVideoView;

/**
 * SurfaceTexture状态回调
 *
 * @author machao10
 * @since 2019-01-09
 */
public final class SurfaceTextureCallback implements TextureView.SurfaceTextureListener {

    /** debug开关 */
    private static final boolean DEBUG = BuildConfig.DEBUG;
    /** DEBUG TAG */
    private static final String TAG = "SurfaceTextureCallback";
    /** 播放器view */
    private final SwanVideoView mVideoView;
    /** 用于绘制视频的TextureView */
    private TextureView mTextureView;
    /** 绘制视频的SurfaceTexture */
    private SurfaceTexture mSurfaceTexture;
    /** 标记是否需要释放surface */
    private boolean mNeedReleaseSurface = false;

    /**
     * SurfaceTexture状态回调构造器
     *
     * @param videoView 需要绘制的视频view
     * @param textureView 用于绘制的TextureView
     */
    public SurfaceTextureCallback(SwanVideoView videoView, TextureView textureView) {
        mVideoView = videoView;
        mTextureView = textureView;
    }

    /**
     * 设置是否需要释放surface
     *
     * @param needReleaseSurface 是否需要释放
     */
    public void setNeedReleaseSurface(boolean needReleaseSurface) {
        mNeedReleaseSurface = needReleaseSurface;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceTextureAvailable : width = " + width + " height = " + height);
        }
        // 第一次available的时候保存SurfaceTexture对象
        if (mSurfaceTexture == null) {
            mSurfaceTexture = surface;
            mVideoView.setSurface(new Surface(surface));
        } else {
            // 已经存在TextureView，则更新surfaceTexture
            mTextureView.setSurfaceTexture(mSurfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceTextureSizeChanged : width = " + width + " height = " + height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceTextureDestroyed : need release = " + mNeedReleaseSurface);
        }
        return mNeedReleaseSurface;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (DEBUG) {
            Log.d(TAG, "onSurfaceTextureUpdated");
        }
    }

}
