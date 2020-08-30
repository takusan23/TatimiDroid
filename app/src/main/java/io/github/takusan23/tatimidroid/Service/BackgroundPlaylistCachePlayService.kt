package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Job


/**
 * キャッシュ連続再生用Service。MediaBrowserServiceだけど普通にサービス起動でもどうぞ
 * 入れてほしいもの↓
 * start_id     | String | 動画の再生開始のIDを指定するときに入れる。任意です。
 * */
class BackgroundPlaylistCachePlayService : MediaBrowserServiceCompat() {

    /** [onLoadChildren]でparentIdに入ってくる。Android 11のメディアの再開の場合はこの値 */
    private val ROOT_RECENT = "root_recent"

    /** [onLoadChildren]でparentIdに入ってくる。[ROOT_RECENT]以外の場合 */
    private val ROOT = "root"

    /** 音楽再生のExoPlayer */
    lateinit var exoPlayer: SimpleExoPlayer

    /** MediaSession */
    lateinit var mediaSessionCompat: MediaSessionCompat

    /** 通知出すのに使う */
    lateinit var notificationManager: NotificationManager

    /** コルーチンキャンセル用 */
    private val cachePlayServiceCoroutineJob = Job()

    /** 設定保存 */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    /** キャッシュ関連 */
    private val nicoVideoCache by lazy { NicoVideoCache(this) }

    /** MediaSession初期化など */
    override fun onCreate() {
        super.onCreate()

        // 通知出すのに使う
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ExoPlayer用意
        exoPlayer = SimpleExoPlayer.Builder(this).build()

        // MediaSession用意
        mediaSessionCompat = MediaSessionCompat(this, "media_session").apply {

            // MediaButtons と TransportControls の操作を受け付ける
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

            // MediaSessionの操作のコールバック
            setCallback(object : MediaSessionCompat.Callback() {

                /** 再生準備 */
                override fun onPrepare() {
                    super.onPrepare()
                    val dataSourceFactory = DefaultDataSourceFactory(this@BackgroundPlaylistCachePlayService, "TatimiDroid;@takusan_23")
                    val videoId = prefSetting.getString("cache_last_play_video_id", "") ?: return
                    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(nicoVideoCache.getCacheFolderVideoFilePath(videoId).toUri()) // 動画の場所
                    exoPlayer.prepare(mediaSource)
                }

                /** 再生 */
                override fun onPlay() {
                    super.onPlay()
                    startThisService()
                    exoPlayer.playWhenReady = true
                }

                /** 一時停止 */
                override fun onPause() {
                    super.onPause()
                    exoPlayer.playWhenReady = false
                }

                /** 通知のシーク動かした時 */
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    exoPlayer.seekTo(pos)
                }

                /** 止めた時 */
                override fun onStop() {
                    super.onStop()
                    isActive = false
                    stopSelf()
                }

            })

            // 忘れずに
            setSessionToken(sessionToken)
        }

        // ExoPlayerの再生状態が更新されたときも通知を更新する
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                updateState()
                showNotification()
            }
        })

    }

    /**
     * 再生状態とメタデータを設定する。今回はメタデータはハードコートする
     *
     * MediaSessionのsetCallBackで扱う操作([MediaSessionCompat.Callback.onPlay]など)も[PlaybackStateCompat.Builder.setState]に書かないと何も起きない
     * */
    private fun updateState() {
        val stateBuilder = PlaybackStateCompat.Builder().apply {
            // 取り扱う操作。とりあえず 再生準備 再生 一時停止 シーク を扱うようにする。書き忘れると何も起きない
            setActions(PlaybackStateCompat.ACTION_PREPARE or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_STOP)
            // 再生してるか。ExoPlayerを参照
            val state = if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            // 位置
            val position = exoPlayer.currentPosition
            // 再生状態を更新
            setState(state, position, 1.0f) // 最後は再生速度
        }.build()
        mediaSessionCompat.setPlaybackState(stateBuilder)
        // メタデータの設定
        val duration = 288L // 再生時間
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            // Android 11 の MediaSession で使われるやつ
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "音楽のタイトル")
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "音楽のアーティスト")
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration * 1000) // これあるとAndroid 10でシーク使えます
        }.build()
        mediaSessionCompat.setMetadata(mediaMetadataCompat)
    }

    /** 通知を表示する */
    private fun showNotification() {
        // 通知を作成。通知チャンネルのせいで長い
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知チャンネル
            val channelId = "playlist_play"
            val notificationChannel = NotificationChannel(channelId, getString(R.string.background_playlist_play_channel), NotificationManager.IMPORTANCE_LOW)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                // 登録
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, channelId)
        } else {
            NotificationCompat.Builder(this)
        }
        notification.apply {
            setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken).setShowActionsInCompactView(0))
            setSmallIcon(R.drawable.ic_background_icon)
            // 通知領域に置くボタン
            if (exoPlayer.isPlaying) {
                addAction(NotificationCompat.Action(R.drawable.ic_pause_black_24dp, "一時停止", MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_PAUSE)))
            } else {
                addAction(NotificationCompat.Action(R.drawable.ic_play_arrow_24px, "再生", MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_PLAY)))
            }
            addAction(NotificationCompat.Action(R.drawable.ic_clear_black, "停止", MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_STOP)))
        }
        // 通知表示
        startForeground(84, notification.build())
    }

    /** フォアグラウンドサービスを起動する */
    private fun startThisService() {
        val playlistPlayServiceIntent = Intent(this, BackgroundPlaylistCachePlayService::class.java)
        // 起動
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(playlistPlayServiceIntent)
        } else {
            startService(playlistPlayServiceIntent)
        }
    }

    /**
     * [MediaBrowserServiceCompat]へ接続しようとした時に呼ばれる
     * Android 11 のメディアの再開では重要になっている
     * */
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // 最後の曲をリクエストしている場合はtrue
        val isRequestRecentMusic = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
        // BrowserRootに入れる値を変える
        val rootPath = if (isRequestRecentMusic) ROOT_RECENT else ROOT
        return BrowserRoot(rootPath, null)
    }

    /**
     * Activityとかのクライアントへ曲一覧を返す
     * */
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // 保険。遅くなると怒られるぽい？
        result.detach()
        if (parentId == ROOT_RECENT) {
            // 動画情報いれる
            result.sendResult(arrayListOf(createMediaItem("sm157", "てすとです", "さぶたいとる")))
        }
    }

    /**
     * [onLoadChildren]で返すアイテムを作成する
     * */
    private fun createMediaItem(videoId: String, title: String, subTitle: String): MediaBrowserCompat.MediaItem {
        val mediaDescriptionCompat = MediaDescriptionCompat.Builder().apply {
            setTitle(title)
            setSubtitle(subTitle)
            setMediaId(videoId)
        }.build()
        return MediaBrowserCompat.MediaItem(mediaDescriptionCompat, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.release()
        exoPlayer.release()
    }

}

/**
 * バッググラウンド連続再生サービス（名前どうにかしたい）
 * @param context Context
 * @param startVideoId 再生開始位置を動画IDで指定するときは入れてね。入れない場合は最初から再生します。
 * */
fun startBackgroundPlaylistPlayService(context: Context?, startVideoId: String = "") {
    // 連続再生！？
    val playlistPlayServiceIntent = Intent(context, BackgroundPlaylistCachePlayService::class.java)
    playlistPlayServiceIntent.putExtra("start_id", startVideoId)
    // 多重起動対策
    context?.stopService(playlistPlayServiceIntent)
    // 起動
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context?.startForegroundService(playlistPlayServiceIntent)
    } else {
        context?.startService(playlistPlayServiceIntent)
    }
}