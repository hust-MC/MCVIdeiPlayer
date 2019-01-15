package com.baidu.swan.videoplayer.callback;

import com.baidu.cloud.media.player.IMediaPlayer;

/**
 * 视频播放器状态监听回调接口。当前接口为视频播放器支持的所有回调，不是全部都要实现。
 * <p>
 * 示例中实现了{@link SimpleVideoCallback}，
 * 如果不需要这么多回调，可用上述简化接口。
 *
 * @author machao10
 * @since 2019-01-12
 */
public interface IVideoPlayerCallback extends IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnInfoListener,
        IMediaPlayer.OnSeekCompleteListener,
        IMediaPlayer.OnBufferingUpdateListener,
        IMediaPlayer.OnVideoSizeChangedListener {

    /**
     * 横竖屏切换事件回调
     *
     * @param landscape true：切到横屏；false：切到竖屏
     */
    void onScreenOrientationChanged(boolean landscape);

}
