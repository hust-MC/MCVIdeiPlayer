package com.baidu.swan.videoplayer.callback;

import com.baidu.cloud.media.player.BDTimedText;
import com.baidu.cloud.media.player.IMediaPlayer;

/**
 * 简化版视频播放器状态监听回调接口。当前接口为上层必须实现的接口，若要使用更多接口，
 * 可参见{@link com.baidu.swan.videoplayer.callback.IVideoPlayerCallback}
 *
 * 示例中实现了该类，如果不需要这么多回调，推荐使用此接口。
 *
 * @author machao10
 * @since 2019-01-12
 */
public abstract class SimpleVideoCallback implements IVideoPlayerCallback {
    @Override
    public void onBufferingUpdate(IMediaPlayer player, int i) {

    }

    @Override
    public boolean onInfo(IMediaPlayer player, int i, int i1) {
        return false;
    }

    @Override
    public void onSeekComplete(IMediaPlayer player) {

    }

    @Override
    public void onScreenOrientationChanged(boolean landscape) {

    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer player, int i, int i1, int i2, int i3) {

    }
}
