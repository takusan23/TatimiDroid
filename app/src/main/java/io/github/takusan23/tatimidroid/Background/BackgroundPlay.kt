package io.github.takusan23.tatimidroid.Background

import android.content.Context
import android.net.Uri
import android.support.v4.media.session.MediaSessionCompat
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener

class BackgroundPlay(val context: Context) {

    lateinit var exoPlayer: SimpleExoPlayer
    val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * 再生可能かどうか
     * */
    var playable = false

    /**
     * 再生中かどうか
     * */
    var isPlaying = false

    /**
     * 再生準備まで行う。
     * 第2引数は入れるとすぐに再生されます。
     * */
    fun play(uri: Uri, playWhenReady: Boolean = false) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        val sourceFactory = DefaultDataSourceFactory(
            context,
            "TatimiDroid;@takusan_23",
            object : TransferListener {
                override fun onTransferInitializing(
                    source: DataSource?,
                    dataSpec: DataSpec?,
                    isNetwork: Boolean
                ) {

                }

                override fun onTransferStart(
                    source: DataSource?,
                    dataSpec: DataSpec?,
                    isNetwork: Boolean
                ) {

                }

                override fun onTransferEnd(
                    source: DataSource?,
                    dataSpec: DataSpec?,
                    isNetwork: Boolean
                ) {

                }

                override fun onBytesTransferred(
                    source: DataSource?,
                    dataSpec: DataSpec?,
                    isNetwork: Boolean,
                    bytesTransferred: Int
                ) {

                }
            })
        val hlsMediaSource = HlsMediaSource.Factory(sourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(uri);
        //再生準備
        exoPlayer.prepare(hlsMediaSource)

        //再生可能か？
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                playable = playbackState == Player.STATE_READY
            }
        })
        exoPlayer.playWhenReady = true
        exoPlayer.volume = 0f

        if (playWhenReady) {
            exoPlayer.volume = 1f
            isPlaying = true
        }
    }

    fun release() {
        if (this@BackgroundPlay::exoPlayer.isInitialized) {
            exoPlayer.volume = 0f
            isPlaying = false
            exoPlayer.release()
        }
    }

    fun pause() {
        if (this@BackgroundPlay::exoPlayer.isInitialized && playable) {
            exoPlayer.volume = 0f
            isPlaying = false
        }
    }


    fun start(uri: Uri) {
        if (this@BackgroundPlay::exoPlayer.isInitialized) {
            if (pref_setting.getBoolean("setting_leave_background_v2", false)) {
                if (playable) {
                    //新しい再生方法
                    exoPlayer.volume = 1f
                    isPlaying = true
                }
            } else {
                play(uri, true)
            }
        } else {
            play(uri, true)
        }
    }

}