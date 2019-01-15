package com.baidu.swan.videoplayer.widget;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import com.baidu.cloud.videoplayer.demo.BuildConfig;
import com.baidu.cloud.videoplayer.demo.R;
import com.baidu.swan.videoplayer.SwanVideoView;
import com.baidu.swan.videoplayer.callback.IVideoPlayerCallback;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;


/**
 * 播放器控件。包含基本的控制：播放、暂停、进度条等
 * 使用的时候通过{@link MediaController#bindMediaControl(SwanVideoView)}
 * 绑定响应的播放器，即可实现播放器控制
 *
 * @author machao10
 * @since 2019-01-09
 */
public class MediaController extends RelativeLayout {

    /** DEBUG TAG */
    private static final String TAG = "SimpleMediaController";
    /** debug 开关 */
    private static final boolean DEBUG = BuildConfig.DEBUG;

    /** 播放按钮 */
    private ImageButton mPlayButton;
    /** 横/竖屏切换按钮 */
    private View mToggleScreenButton;
    /** 播放进度显示 */
    private TextView positionView;
    /** 进度条 */
    private SeekBar mSeekBar;
    /** 视频总时长显示 */
    private TextView durationView;
    /** 当前播放进度记录，单位：ms */
    private long currentPositionInMSec;
    /** 进度条更新触发器 */
    private Timer positionTimer;
    /** 主进程handler */
    private Handler mMainThreadHandler;
    /** 播放控件自动消失定时器 */
    private Timer mShowTimer;
    /** 播放器view，用于同步控制状态到播放器 */
    private SwanVideoView mVideoView;
    /** 是否处于拖拽状态 */
    boolean mbIsDragging;
    /** 标记是否设置过seekBar的最大值 */
    private boolean isMaxSet = false;
    /** 视频播放器回调，这里用于回调横/竖屏点击事件 */
    private IVideoPlayerCallback mVideoPlayerCallback;

    /** 进度条更新触发周期 */
    private static final int POSITION_REFRESH_TIME = 500;
    /** 进度条自动消失事件 */
    private static final long HIDE_AFTER_SECONDS = 3 * DateUtils.SECOND_IN_MILLIS;

    /**
     * 视频播放器控件构造器。将此控件绑定到播放器上，即可实现对播放器的基本控制。
     *
     * @param context 上下文，仅用于构建view
     */
    public MediaController(Context context) {
        super(context);
        initVIew();
    }

    /**
     * 视频播放器控件构造器。将此控件绑定到播放器上，即可实现对播放器的基本控制。
     *
     * @param context 上下文，仅用于构建view
     * @param attrs   View布局参数
     */
    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVIew();
    }

    /**
     * 初始化UI
     */
    private void initVIew() {

        View layout = LayoutInflater.from(getContext()).inflate(R.layout.media_controller, this);

        mPlayButton = layout.findViewById(R.id.btn_play);
        mPlayButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mVideoView == null) {
                    if (DEBUG) {
                        Log.d(TAG, "mPlayButton clicked : videoView is null");
                    }
                } else {
                    if (mVideoView.isPlaying()) {
                        if (DEBUG) {
                            Log.d(TAG, "mPlayButton clicked : to pause");
                        }
                        mPlayButton.setBackgroundResource(R.drawable.btn_play);
                        mVideoView.pause();
                    } else {
                        Log.d(TAG, "mPlayButton clicked : to resume");
                        mPlayButton.setBackgroundResource(R.drawable.btn_pause);
                        mVideoView.start();
                    }
                }
            }

        });

        positionView = layout.findViewById(R.id.tv_position);
        mSeekBar = layout.findViewById(R.id.seekbar);
        durationView = layout.findViewById(R.id.tv_duration);

        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePosition(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                mbIsDragging = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mVideoView.getDuration() > 0) {
                    currentPositionInMSec = seekBar.getProgress();
                    if (mVideoView != null) {
                        mVideoView.seekTo(seekBar.getProgress());
                    }
                }
                mbIsDragging = false;
            }
        });

        mToggleScreenButton = layout.findViewById(R.id.btn_toggle_screen);
        mToggleScreenButton.setOnClickListener(new OnClickListener() {

            private boolean mIsLandScape;

            @Override
            public void onClick(View v) {
                mIsLandScape = !mIsLandScape;
                if (mVideoPlayerCallback != null) {
                    mVideoPlayerCallback.onScreenOrientationChanged(mIsLandScape);
                }
            }
        });

        mSeekBar.setEnabled(false);
        mPlayButton.setEnabled(false);
    }


    /**
     * 获取主进程handler
     *
     * @return 主进程的handler
     */
    public Handler getMainThreadHandler() {
        if (mMainThreadHandler == null) {
            mMainThreadHandler = new Handler(Looper.getMainLooper());
        }
        return mMainThreadHandler;
    }

    /**
     * 更新控件状态
     */
    public void updateState() {
        int status = mVideoView.getCurrentPlayerState();
        if (DEBUG) {
            Log.d(TAG, "mediaController: changeStatus=" + status);
        }
        isMaxSet = false;

        switch (status) {
            case SwanVideoView.STATE_IDLE:
            case SwanVideoView.STATE_ERROR:
                stopPositionTimer();
                mPlayButton.setEnabled(true);
                mPlayButton.setBackgroundResource(R.drawable.btn_play);
                mSeekBar.setEnabled(false);
                updatePosition(mVideoView == null ? 0 : mVideoView.getCurrentPosition());
                updateDuration(mVideoView == null ? 0 : mVideoView.getDuration());
                break;
            case SwanVideoView.STATE_PREPARING:
                mPlayButton.setEnabled(false);
                mSeekBar.setEnabled(false);
                break;
            case SwanVideoView.STATE_PREPARED:
                mPlayButton.setEnabled(true);
                mPlayButton.setBackgroundResource(R.drawable.btn_play);
                mSeekBar.setEnabled(true);
                updateDuration(mVideoView == null ? 0 : mVideoView.getDuration());
                mSeekBar.setMax(mVideoView.getDuration());
                break;
            case SwanVideoView.STATE_PLAYBACK_COMPLETED:
                stopPositionTimer();
                mSeekBar.setProgress(mSeekBar.getMax());
                mSeekBar.setEnabled(false);
                mPlayButton.setEnabled(true);
                mPlayButton.setBackgroundResource(R.drawable.btn_play);
                break;
            case SwanVideoView.STATE_PLAYING:
                startPositionTimer();
                mSeekBar.setEnabled(true);
                mPlayButton.setEnabled(true);
                mPlayButton.setBackgroundResource(R.drawable.btn_pause);
                break;
            case SwanVideoView.STATE_PAUSED:
                mPlayButton.setEnabled(true);
                mPlayButton.setBackgroundResource(R.drawable.btn_play);
                break;
            default:
                if (DEBUG) {
                    throw new IllegalStateException("Player state is out of control");
                }
                break;

        }

    }

    /**
     * 启动进度条更新
     */
    private void startPositionTimer() {
        if (positionTimer != null) {
            positionTimer.cancel();
            positionTimer = null;
        }
        positionTimer = new Timer();
        positionTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        onPositionUpdate();
                    }
                });
            }

        }, 0, POSITION_REFRESH_TIME);
    }

    /**
     * 停止进度条更新
     */
    private void stopPositionTimer() {
        if (positionTimer != null) {
            positionTimer.cancel();
            positionTimer = null;
        }
    }

    /**
     * 将播放器和控件做绑定
     *
     * @param player 要绑定的播放器
     */
    public void bindMediaControl(SwanVideoView player) {
        mVideoView = player;
    }

    /**
     * 设置横竖屏切换监听器
     *
     * @param videoPlayerCallback 视频播放器回调
     */
    public void setToggleScreenListener(IVideoPlayerCallback videoPlayerCallback) {
        mVideoPlayerCallback = videoPlayerCallback;
    }

    /**
     * 展示控件
     */
    private void show() {
        if (mVideoView == null) {
            return;
        }

        setProgress((int) currentPositionInMSec);

        setVisibility(View.VISIBLE);
    }

    /**
     * 在{@link MediaController#HIDE_AFTER_SECONDS}秒后消失
     */
    public void hideOuterAfterSeconds() {
        show();
        if (mShowTimer != null) {
            mShowTimer.cancel();
            mShowTimer = null;
        }
        mShowTimer = new Timer();
        mShowTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                getMainThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        hide();
                    }

                });
            }
        }, HIDE_AFTER_SECONDS);

    }

    /**
     * 隐藏控件
     */
    public void hide() {
        setVisibility(View.GONE);
    }

    /**
     * 更新视频时长显示
     *
     * @param milliSecond 当前视频时长，单位：ms
     */
    private void updateDuration(int milliSecond) {
        if (durationView != null) {
            durationView.setText(formatTimeText(milliSecond));
        }
    }

    /**
     * 更新当前播放进度
     *
     * @param milliSecond 当前视频播放进度，单位：ms
     */
    private void updatePosition(int milliSecond) {
        if (positionView != null) {
            positionView.setText(formatTimeText(milliSecond));
        }
    }

    /**
     * 格式化时间显示，将播放器当前的播放进度（ms）格式化成时间显示文本
     *
     * @param mSec 当前播放进度，单位：ms
     * @return 格式化后的文本，可直接用于显示
     */
    public static String formatTimeText(int mSec) {
        if (mSec < 0) {
            return "";
        }

        int second = mSec / 1000;

        int hh = second / 3600;
        int mm = second % 3600 / 60;
        int ss = second % 60;
        String strTemp;
        if (0 != hh) {
            strTemp = String.format(Locale.US, "%02d:%02d:%02d", hh, mm, ss);
        } else {
            strTemp = String.format(Locale.US, "%02d:%02d", mm, ss);
        }
        return strTemp;
    }

    /**
     * 设置seekBar的最大值，即视频总时长
     *
     * @param max 最大进度
     */
    private void setMax(int max) {
        if (isMaxSet) {
            return;
        }
        if (mSeekBar != null) {
            mSeekBar.setMax(max);
        }
        updateDuration(max);
        if (max > 0) {
            isMaxSet = true;
        }
    }

    /**
     * 设置seekBar当前进度，即视频播放进度
     *
     * @param progress 视频当前进度
     */
    public void setProgress(int progress) {
        if (mSeekBar != null) {
            mSeekBar.setProgress(progress);
        }
    }

    /**
     * 更新播放进度，每500ms更新一次
     */
    public void onPositionUpdate() {
        if (mVideoView == null || mbIsDragging) {
            return;
        }
        long position = mVideoView.getCurrentPosition();

        if (position > 0) {
            currentPositionInMSec = position;
        }

        if (getVisibility() != View.VISIBLE) {
            // 如果控制条不可见，则不设置进度
            return;
        }

        int durationInMilliSeconds = mVideoView.getDuration();
        if (durationInMilliSeconds > 0) {
            setMax(durationInMilliSeconds);
            setProgress((int) position);
        }
    }

    /**
     * 更新缓冲进度
     *
     * @param mSec 当前视频缓冲时间，单位：ms
     */
    public void onTotalCacheUpdate(int mSec) {
        if (mSeekBar != null && mSec != mSeekBar.getSecondaryProgress()) {
            mSeekBar.setSecondaryProgress((mSec));
        }
    }
}
