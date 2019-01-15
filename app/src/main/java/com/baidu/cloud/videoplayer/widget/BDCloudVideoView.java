package com.baidu.cloud.videoplayer.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.cloud.media.player.BDCloudMediaPlayer;
import com.baidu.cloud.media.player.IMediaPlayer;
import com.baidu.cloud.videoplayer.demo.BuildConfig;

import java.io.IOException;
import java.util.Map;

/**
 * 播放器视图VideoView，可以当做普通View使用。
 * 底层内部封装了TextureView绘制视频帧，播放内核采用百度云播放器SDK
 *
 * @author machao10
 * @since 2019-01-09
 */
public class BDCloudVideoView extends FrameLayout {

    /** 调试开关 */
    private static final boolean DEBUG = BuildConfig.DEBUG;
    /** debug TAG */
    private static final String TAG = "SwanVideoView";


    /* ================ 播放器状态 START ============== */
    /** 播放错误 */
    public static final int STATE_ERROR = -1;
    /** 播放器空闲 */
    public static final int STATE_IDLE = 0;
    /** 正在解析视频源 */
    public static final int STATE_PREPARING = 1;
    /** 视频信息解析完毕 */
    public static final int STATE_PREPARED = 2;
    /** 正在播放 */
    public static final int STATE_PLAYING = 3;
    /** 视频暂停 */
    public static final int STATE_PAUSED = 4;
    /** 播放完毕 */
    public static final int STATE_PLAYBACK_COMPLETED = 5;
    /* ================ 播放器状态 END ============== */

    /** 最大缓冲大小为500M */
    private static final int MAX_CACHE = 500 * 1000;
    /** 设置播放器建立连接和数据下载过程中的超时时长，单位：us */
    private static final int PLAYER_TIME_OUT_US = 15000000;

    /** 播放器当前的状态 */
    private int mCurrentState = STATE_IDLE;

    /** 是否需要播放 */
    private boolean mReadyToPlay;
    /** 视频源url */
    private Uri mUri;
    /** 指定headers，默认不指定 */
    private Map<String, String> mHeaders;
    /** 播放控件 */
    private SimpleMediaController mController;
    /** 百度云播放器实例 */
    private BDCloudMediaPlayer mMediaPlayer;
    /** 当前播放进度百分比 */
    private int mCurrentBufferPercentage;
    /** app context */
    private Context mAppContext;
    /** 用于绘制视频的TextureView对象 */
    private TextureRenderView mTextureView;
    /** 视频播放初始位置 */
    private long mInitPlayPositionInMSec = -1;
    /** 标记是否自动循环播放 */
    private boolean mLooping;
    /** 标记是否使用控件 */
    private boolean mControllerEnabled = true;


    /* ============ 加载中状态view START ========= */
    /** 加载中布局容器 */
    private RelativeLayout mLoadingLayout;
    /** 加载中进度条 */
    private ProgressBar mLoadingBar;
    /** 加载中提示语 */
    private TextView mLoadingHint;
    /* ============ 加载中状态view END =========== */

    /** 视频view根布局 */
    private FrameLayout mVideoRootView;

    static {
        BDCloudMediaPlayer.setAK("5989e435183e42c5a3f7da72dbac006c");
    }


    /**
     * 视频播放器view构造器
     *
     * @param context 上下文
     */
    public BDCloudVideoView(Context context) {
        super(context);
        initVideoView(context);
    }

    /**
     * x视频播放器view构造器
     *
     * @param context 上下文
     * @param attrs   布局参数
     */
    public BDCloudVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    /**
     * 视频播放器view构造器
     *
     * @param context      上下文
     * @param attrs        布局参数
     * @param defStyleAttr 布局风格参数
     */
    public BDCloudVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    /**
     * 初始化视频view
     *
     * @param context 上下文对象
     */
    private void initVideoView(Context context) {

        mAppContext = context.getApplicationContext();

        mVideoRootView = new FrameLayout(context);
        FrameLayout.LayoutParams rootViewParams = new FrameLayout.LayoutParams(-1, -1);
        addView(mVideoRootView, rootViewParams);

        mController = new MediaController(context);
        LayoutParams controllerParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        controllerParams.gravity = Gravity.BOTTOM;
        mController.setVisibility(GONE);
        addView(mController, controllerParams);
        mController.bindMediaControl(this);

        initTextureView();
        addCachingHintView();

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        setCurrentState(STATE_IDLE);

        setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mControllerEnabled) {
                    return;
                }
                if (mController.getVisibility() != VISIBLE) {
                    mController.hideOuterAfterSeconds();
                } else {
                    mController.hide();
                }
            }
        });
    }

    /**
     * 为播放器设置surface
     *
     * @param surface 用于绘制视频图像的surface对象
     */
    public void setSurface(Surface surface) {
        mMediaPlayer.setSurface(surface);
    }

    /**
     * 添加加载提示控件
     */
    private void addCachingHintView() {
        mLoadingLayout = new RelativeLayout(this.getContext());
        FrameLayout.LayoutParams loadingParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams
                .MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        mLoadingLayout.setVisibility(View.GONE);
        addView(mLoadingLayout, loadingParams);

        RelativeLayout.LayoutParams loadingBarParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        loadingBarParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        mLoadingBar = new ProgressBar(this.getContext());
        mLoadingBar.setId(android.R.id.text1);
        mLoadingBar.setMax(100);
        mLoadingBar.setProgress(10);
        mLoadingBar.setSecondaryProgress(100);
        mLoadingLayout.addView(mLoadingBar, loadingBarParams);

        loadingBarParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        loadingBarParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        loadingBarParams.addRule(RelativeLayout.BELOW, android.R.id.text1);
        mLoadingHint = new TextView(this.getContext());
        mLoadingHint.setTextColor(0xffffffff);
        mLoadingHint.setText(R.string.laoding);
        mLoadingHint.setGravity(Gravity.CENTER_HORIZONTAL);
        mLoadingLayout.addView(mLoadingHint, loadingBarParams);
    }

    /**
     * 设置播放器回调
     *
     * @param callback 播放器回调
     */
    public void setVideoPlayerCallback(IVideoPlayerCallback callback) {
        mVideoPlayerCallback = callback;
        if (mController != null) {
            mController.setToggleScreenListener(callback);
        }
    }

    /**
     * 获取播放器当前状态
     *
     * @return 播放器状态
     */
    public int getCurrentPlayerState() {
        return mCurrentState;
    }

    /**
     * 设置播放器状态
     *
     * @param newState 播放器状态
     */
    private void setCurrentState(int newState) {
        if (mCurrentState != newState) {
            mCurrentState = newState;
            if (mController != null) {
                mController.updateState();
            }
        }
    }

    /**
     * 设置缓冲提示的可见性
     *
     * @param visible true：可见；false：不可见
     */
    private void setCacheViewVisibility(boolean visible) {
        if (visible) {
            mLoadingLayout.setVisibility(View.VISIBLE);
        } else {
            mLoadingLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 初始化textView，用于渲染视频内容
     */
    private void initTextureView() {
        if (mTextureView != null) {
            if (mMediaPlayer != null) {
                mMediaPlayer.setDisplay(null);
            }
            mTextureView.release();
            mVideoRootView.removeView(mTextureView);
            mTextureView = null;
        }

        mTextureView = new VideoTextureView(getContext());

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        mTextureView.setLayoutParams(params);
        mVideoRootView.addView(mTextureView);

        mSurfaceCallback = new SurfaceTextureCallback(this, mTextureView);
        mTextureView.setSurfaceTextureListener(mSurfaceCallback);
    }

    /**
     * 设置视频源路径
     *
     * @param path 视频源路径，支持本地和网络
     */
    public void setVideoPath(String path) {
        mUri = Uri.parse(path);
        openVideo();
        requestLayout();
        invalidate();
    }

    /**
     * 设置请求header，需要在setVideoPath之前调用
     *
     * @param headers 网络请求header，默认不设置
     */
    public void setHeaders(Map<String, String> headers) {
        mHeaders = headers;
    }

    /**
     * 停止播放并释放资源。如果想再次播放，需要重新create
     */
    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            releasePlayer();
            mReadyToPlay = false;
        }
    }

    /**
     * 打开播放器
     */
    private void openVideo() {
        if (mUri == null) {
            return;
        }

        releasePlayer();

        try {
            mMediaPlayer = createPlayer();
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mCurrentBufferPercentage = 0;
            mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mMediaPlayer.setTimeoutInUs(PLAYER_TIME_OUT_US);
            mMediaPlayer.prepareAsync();
            setCacheViewVisibility(true);

            setCurrentState(STATE_PREPARING);

        } catch (IOException | IllegalArgumentException ex) {
            if (DEBUG) {
                Log.w(TAG, "Unable to open content: " + mUri, ex);
            }
            setCurrentState(STATE_ERROR);
            mReadyToPlay = false;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    /**
     * 创建播放器
     *
     * @return 百度云播放器
     */
    public BDCloudMediaPlayer createPlayer() {
        BDCloudMediaPlayer bdCloudMediaPlayer = new BDCloudMediaPlayer(this.getContext());

        bdCloudMediaPlayer.setLogEnabled(DEBUG);
        bdCloudMediaPlayer.setDecodeMode(BDCloudMediaPlayer.DECODE_AUTO);

        if (mInitPlayPositionInMSec > 0) {
            bdCloudMediaPlayer.setInitPlayPosition(mInitPlayPositionInMSec); // 设置初始播放位置
            mInitPlayPositionInMSec = -1;
        }

        bdCloudMediaPlayer.setMaxCacheSizeInBytes(MAX_CACHE);
        bdCloudMediaPlayer.setLooping(mLooping);

        return bdCloudMediaPlayer;
    }

    /**
     * 设置是否循环播放
     *
     * @param isLoop 是否循环播放
     */
    public void setLooping(boolean isLoop) {
        this.mLooping = isLoop;
        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(mLooping);
        }
    }

    /**
     * 设置音量
     *
     * @param volume 音量值
     */
    public void setVolume(float volume) {
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(volume, volume);
        }
    }

    /**
     * 设置控件是否可用
     *
     * @param enable true：可用；false：不可用
     */
    public void setMediaControllerEnabled(boolean enable) {
        mControllerEnabled = enable;
    }

    /**
     * 设置播放器初始播放位置。单位ms
     *
     * @param mSec 初始播放位置
     */
    public void setInitPlayPosition(long mSec) {
        mInitPlayPositionInMSec = mSec;
        if (mMediaPlayer != null) {
            mMediaPlayer.setInitPlayPosition(mInitPlayPositionInMSec);
            // 重置播放位置
            mInitPlayPositionInMSec = -1;
        }
    }

    /**
     * 播放器prepared事件回调，在播放器解析完视频meta信息后回调
     */
    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            if (DEBUG) {
                Log.d(TAG, "onPrepared");
            }
            setCurrentState(STATE_PREPARED);

            setCacheViewVisibility(false);

            if (mVideoPlayerCallback != null) {
                mVideoPlayerCallback.onPrepared(mMediaPlayer);
            }

            if (mReadyToPlay) {
                start();
            }
        }
    };

    /**
     * 视频播放结束回调
     */
    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    Log.d(TAG, "onCompletion");
                    setCacheViewVisibility(false);
                    setCurrentState(STATE_PLAYBACK_COMPLETED);
                    mReadyToPlay = false;
                    if (mVideoPlayerCallback != null) {
                        mVideoPlayerCallback.onCompletion(mMediaPlayer);
                    }
                }
            };

    /**
     * 播放器播放失败回调
     */
    private IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                public boolean onError(IMediaPlayer mp, int what, int extra) {
                    Log.d(TAG, "onError: " + what + "," + extra);
                    setCurrentState(STATE_ERROR);
                    mReadyToPlay = false;

                    setCacheViewVisibility(false);

                    return mVideoPlayerCallback == null
                            || mVideoPlayerCallback.onError(mMediaPlayer, what, extra);

                }
            };

    /**
     * 视频播放器缓存更新回调
     */
    private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new IMediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                    Log.d(TAG, "onBufferingUpdate: percent=" + percent);
                    mCurrentBufferPercentage = percent;
                    if (mVideoPlayerCallback != null) {
                        mVideoPlayerCallback.onBufferingUpdate(mp, percent);
                    }

                    if (mController != null) {
                        mController.onTotalCacheUpdate(percent * getDuration() / 100);
                    }
                }
            };

    /**
     * 播放器seek结束回调
     */
    private IMediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new IMediaPlayer
            .OnSeekCompleteListener() {

        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            Log.d(TAG, "onSeekComplete");
            setCacheViewVisibility(false);
            if (mVideoPlayerCallback != null) {
                mVideoPlayerCallback.onSeekComplete(mp);
            }
        }
    };

    /**
     * 释放全部资源，释放之后不可再使用播放器
     */
    public void release() {
        // 释放播放器player
        releasePlayer();
        mReadyToPlay = false;

        // 释放textView相关资源
        if (mTextureView != null) {
            if (mTextureView.isAvailable()) {
                mSurfaceCallback.setNeedReleaseSurface(true);
            } else {
                mTextureView.release();
            }
            mTextureView = null;
        }
        // 释放控件相关资源
        if (mController != null) {
            mController.setToggleScreenListener(null);
            mController.bindMediaControl(null);
            mController = null;
        }
    }

    /**
     * 重置播放器
     */
    private void releasePlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.setDisplay(null);
            mMediaPlayer.release();
            mMediaPlayer = null;
            setCurrentState(STATE_IDLE);
        }

        if (mVideoPlayerCallback != null) {
            mVideoPlayerCallback = null;
        }
    }


    /**
     * 开始播放/在暂停状态下继续播放
     */
    public void start() {
        if (mMediaPlayer == null) {
            return;
        }
        if (mCurrentState == STATE_ERROR || mCurrentState == STATE_PLAYBACK_COMPLETED) {

            if (mCurrentState == STATE_PLAYBACK_COMPLETED) {
                mMediaPlayer.stop();
            }

            mMediaPlayer.prepareAsync();
            setCacheViewVisibility(true);
            setCurrentState(STATE_PREPARING);
        } else if (isInPlaybackState()) {
            mMediaPlayer.start();
            if (DEBUG) {
                Log.d(TAG, "start video : " + mUri);
            }
            setCurrentState(STATE_PLAYING);
        }
        mReadyToPlay = true;
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                setCurrentState(STATE_PAUSED);
            }
        }
        mReadyToPlay = false;
    }

    /**
     * 获取缓冲进度百分比
     *
     * @return 当前播放进度百分比
     */
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    /**
     * 获取当前视频源url
     *
     * @return 视频源url
     */
    public String getCurrentPlayingUrl() {
        if (this.mUri != null) {
            return this.mUri.toString();
        }
        return null;
    }

    /**
     * 获得视频时长。单位：ms
     *
     * @return 视频总时长
     */
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }

        return 0;
    }

    /**
     * 获取当前播放进度。单位：ms
     *
     * @return 视频播放进度
     */
    public int getCurrentPosition() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    /**
     * 将播放器定位到到某个播放位置。单位：ms
     *
     * @param mSec 待定位的位置
     */
    public void seekTo(int mSec) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(mSec);
            setCacheViewVisibility(true);
        }
    }

    /**
     * 判断是否正在播放
     *
     * @return true：处于播放状态；false：非播放状态
     */
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }


    /**
     * 是否处于播放状态。与isPlaying不同之处在于，pause和completion也属于播放状态，但是isPlaying会返回true
     *
     * @return 是否处于播放状态
     */
    private boolean isInPlaybackState() {
        return (mMediaPlayer != null
                && mCurrentState != STATE_ERROR
                && mCurrentState != STATE_IDLE
                && mCurrentState != STATE_PREPARING);
    }

    /**
     * 获取视频宽度
     *
     * @return 视频宽度
     */
    public int getVideoWidth() {
        return mMediaPlayer.getVideoWidth();
    }

    /**
     * 获取视频高度
     *
     * @return 视频高度
     */
    public int getVideoHeight() {
        return mMediaPlayer.getVideoHeight();
    }

    /**
     * 获取当前网络内容下载速率
     *
     * @return
     */
    public long getDownloadSpeed() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getDownloadSpeed();

        }
        return 0L;
    }

    /**
     * 获取视频截图
     */
    public Bitmap getBitmap() {
        if (mTextureView != null) {
            return mTextureView.getBitmap();
        }
        return null;
    }


}
