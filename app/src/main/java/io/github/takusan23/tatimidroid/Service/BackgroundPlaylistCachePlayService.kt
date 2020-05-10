package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject


/**
 * キャッシュ連続再生用Service。
 * 入れてほしいもの↓
 * start_id     | String | 動画の再生開始のIDを指定するときに入れてね。（任意。未指定の場合最初から再生します）
 * */
class BackgroundPlaylistCachePlayService : Service() {

    lateinit var notificationManager: NotificationManager
    lateinit var prefSessing: SharedPreferences
    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var mediaSession: MediaSessionCompat
    lateinit var mediaSessionConnector: MediaSessionConnector
    lateinit var nicoVideoCache: NicoVideoCache

    // 通知ID
    val NOTIFICAION_ID = 1919

    // 開始位置
    var startVideoId = ""

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        prefSessing = PreferenceManager.getDefaultSharedPreferences(this)

        // Broadcast初期化
        initBroadcast()
        // 通知出す
        showNotification()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        nicoVideoCache = NicoVideoCache(this)

        // 開始位置を取得
        startVideoId = intent?.getStringExtra("start_id") ?: ""

        // ExoPlayerとMediaSession初期化
        initPlayer()

        // 再生？
        loadPlaylist()

        return START_NOT_STICKY
    }

    private fun loadPlaylist() {
        val dataSourceFactory = DefaultDataSourceFactory(this, "TatimiDroid;@takusan_23")
        // プレイリスト
        val playList = ConcatenatingMediaSource()
        GlobalScope.launch {
            // 取得
            var videoList = nicoVideoCache.loadCache().await()
            val filter = CacheJSON().readJSON(this@BackgroundPlaylistCachePlayService)
            // フィルター
            videoList = if (filter != null) {
                nicoVideoCache.getCacheFilterList(videoList, filter)
            } else {
                videoList
            }
            // プレイリストに追加
            videoList.forEach {
                // 動画のパス
                val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(it.videoId)
                if (videoFileName != null) {
                    // なぜかnullの時がある
                    val contentUrl =
                        "${nicoVideoCache.getCacheFolderPath()}/${it.videoId}/$videoFileName"
                    // MediaSource
                    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .setTag(it.videoId)
                        .createMediaSource(contentUrl.toUri())
                    playList.addMediaSource(mediaSource)
                }
            }
            // 開始位置特定。
            val startPos = if (startVideoId.isNotEmpty()) {
                videoList.filter { nicoVideoData -> nicoVideoCache.getCacheFolderVideoFileName(nicoVideoData.videoId) != null }
                    .indexOfFirst { nicoVideoData -> nicoVideoData.videoId == startVideoId }
            } else {
                0
            }
            println(startPos)
            Handler(Looper.getMainLooper()).post {
                // 再生
                exoPlayer.apply {
                    prepare(playList)
                    seekTo(startPos, 0L) // 指定した位置から開始
                    playWhenReady = true
                }
            }
        }
    }

    // ExoPlayerとMediaSession初期化
    private fun initPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        // MediaSession
        mediaSession = MediaSessionCompat(this, "background_playlist_play").apply {
            isActive = true
            setPlaybackState(
                PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0L, 1F)
                    .build()
            )
        }
        // MediaSessionの操作をExoPlayerに適用してくれる神ライブラリ。無知でも使える
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlayer(exoPlayer)
        // メタデータの中身をExoPlayerではなく自前で用意する
        // 注意：この内容はAlways On Displayやロック画面やGoogle Assistantにいま再生している曲何？と聞いたときに帰ってくる情報
        // 通知の内容はこれとは別に通知にセットする
        mediaSessionConnector.setMediaMetadataProvider {
            if (exoPlayer.currentTag != null) {
                val videoId = exoPlayer.currentTag as String
                // 動画情報JSONが存在するか
                if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                    // 動画情報JSON取得
                    val videoJSON = nicoVideoCache.getCacheFolderVideoInfoText(videoId)
                    val jsonObject = JSONObject(videoJSON)
                    val currentTitle = jsonObject.getJSONObject("video").getString("title")
                    // 投稿者
                    val uploaderName = if (jsonObject.isNull("owner")) {
                        jsonObject.getJSONObject("channel").getString("name")
                    } else {
                        jsonObject.getJSONObject("owner").getString("nickname")
                    }
                    val currentThumbBitmap =
                        BitmapFactory.decodeFile(nicoVideoCache.getCacheFolderVideoThumFilePath(videoId))
                    // メタデータ
                    val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
                        putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                        putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)
                        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentTitle)
                        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, currentTitle)
                        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, uploaderName)
                        putBitmap(MediaMetadataCompat.METADATA_KEY_ART, currentThumbBitmap)
                        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it.duration) // これあるとAndroid 10でシーク使えます
                    }.build()
                    mediaMetadataCompat
                } else {
                    // 動画情報JSONなかった
                    // メタデータ
                    val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
                        putString(MediaMetadataCompat.METADATA_KEY_TITLE, nicoVideoCache.getCacheFolderVideoFileName(videoId)) // ファイル名
                        putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)
                        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, videoId)
                        putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, nicoVideoCache.getCacheFolderVideoFileName(videoId))
                        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, videoId)
                        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, nicoVideoCache.getVideoDurationSec(videoId) * 1000) // これあるとAndroid 10でシーク使えます
                    }.build()
                    mediaMetadataCompat
                }
            } else {
                null
            }
        }

        // ExoPlayerのイベント
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                // 再生状態変わったら対応
                updateNotificationPlayer()
            }

            override fun onPositionDiscontinuity(reason: Int) {
                super.onPositionDiscontinuity(reason)
                // 次の曲いったら
                updateNotificationPlayer()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                // リピート条件変わったら
                updateNotificationPlayer()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                // シャッフル有効・無効切り替わったら
                updateNotificationPlayer()
            }

        })
    }

    // 通知を更新する
    private fun updateNotificationPlayer() {
        if (exoPlayer.currentTag == null) {
            return
        }
        // 次の曲に行ったときなど
        val videoId = exoPlayer.currentTag as String
        // 動画情報があるか
        if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
            val videoJSON = nicoVideoCache.getCacheFolderVideoInfoText(videoId)
            val jsonObject = JSONObject(videoJSON)
            val currentTitle = jsonObject.getJSONObject("video").getString("title")
            // 投稿者
            val uploaderName = if (jsonObject.isNull("owner")) {
                jsonObject.getJSONObject("channel").getString("name")
            } else {
                jsonObject.getJSONObject("owner").getString("nickname")
            }
            // サムネ
            val currentThumbBitmap =
                BitmapFactory.decodeFile(nicoVideoCache.getCacheFolderVideoThumFilePath(videoId))
            // 通知更新
            showNotification(currentTitle, uploaderName, currentThumbBitmap)
        } else {
            val title = nicoVideoCache.getCacheFolderVideoFileName(videoId) ?: "取得に失敗しました。"
            showNotification(title, videoId)
        }
    }

    private fun showNotification(title: String = "", uploaderName: String = "", thumb: Bitmap? = null) {
        // Service終了ブロードキャスト
        val stopService = Intent("service_stop")
        val playIntent = Intent("play")
        val pauseIntent = Intent("pause")
        val nextIntent = Intent("next")
        val repeatOneIntent = Intent("repeat_one")
        val repeatAllIntent = Intent("repeat_all")
        val repeatOffIntent = Intent("repeat_off")
        val shuffleOn = Intent("shuffle_on")
        val shuffleOff = Intent("shuffle_off")
        // 通知作成
        val notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 通知チャンネル
            val channelId = "playlist_play"
            val notificationChannel =
                NotificationChannel(channelId, getString(R.string.background_playlist_play_channel), NotificationManager.IMPORTANCE_LOW)
            if (notificationManager.getNotificationChannel(channelId) == null) {
                // 登録
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, channelId).apply {
                setContentTitle(title)
                setContentText(uploaderName)
                setSmallIcon(R.drawable.ic_tatimidroid_playlist_play_black)
            }
        } else {
            NotificationCompat.Builder(this).apply {
                setContentTitle(title)
                setContentText(uploaderName)
                setSmallIcon(R.drawable.ic_tatimidroid_playlist_play_black)
            }
        }
        val notification = notificationBuilder.apply {
            setLargeIcon(thumb)
            if (::mediaSession.isInitialized) {
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(1)
                )
            }
            // 停止ボタン
            addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 20, stopService, PendingIntent.FLAG_UPDATE_CURRENT)))
            if (::exoPlayer.isInitialized && exoPlayer.playWhenReady) {
                // 一時停止
                addAction(NotificationCompat.Action(R.drawable.ic_pause_black_24dp, "play", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 21, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            } else {
                // 再生
                addAction(NotificationCompat.Action(R.drawable.ic_play_arrow_24px, "pause", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 22, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }
            // 次の曲
            addAction(NotificationCompat.Action(R.drawable.ic_fast_forward_black_24dp, "next", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 24, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            if (::exoPlayer.isInitialized) {
                // 初期化済みなら
                when (exoPlayer.repeatMode) {
                    Player.REPEAT_MODE_OFF -> {
                        // Off -> All Repeat
                        addAction(NotificationCompat.Action(R.drawable.ic_arrow_downward_black_24dp, "repeat", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 25, repeatAllIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                    }
                    Player.REPEAT_MODE_ALL -> {
                        // All Repeat -> Repeat One
                        addAction(NotificationCompat.Action(R.drawable.ic_repeat_black_24dp, "repeat", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 26, repeatOneIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                    }
                    Player.REPEAT_MODE_ONE -> {
                        // Repeat One -> Off
                        addAction(NotificationCompat.Action(R.drawable.ic_repeat_one_24px, "repeat", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 27, repeatOffIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                    }
                }
            } else {
                // 今再生中の曲を無限大なああああああ->プレイリストループ
                addAction(NotificationCompat.Action(R.drawable.ic_repeat_black_24dp, "repeat", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 25, repeatOneIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }
            // シャッフル
            if (::exoPlayer.isInitialized && exoPlayer.shuffleModeEnabled) {
                addAction(NotificationCompat.Action(R.drawable.ic_shuffle_black_24dp, "shuffle", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 26, shuffleOff, PendingIntent.FLAG_UPDATE_CURRENT)))
            } else {
                addAction(NotificationCompat.Action(R.drawable.ic_trending_flat_black_24dp, "shuffle", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 27, shuffleOn, PendingIntent.FLAG_UPDATE_CURRENT)))
            }
        }.build()
        startForeground(NOTIFICAION_ID, notification)
    }

    // 終了などを受け取るブロードキャスト
    private fun initBroadcast() {
        val intentFilter = IntentFilter().apply {
            addAction("service_stop")
            addAction("play")
            addAction("pause")
            addAction("next")
            addAction("repeat_one")
            addAction("repeat_all")
            addAction("repeat_off")
            addAction("shuffle_on")
            addAction("shuffle_off")
        }
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    // 終了
                    "service_stop" -> stopSelf()
                    "play" -> mediaSession.controller.transportControls.play()
                    "pause" -> mediaSession.controller.transportControls.pause()
                    "next" -> {
                        // 次の曲
                        mediaSession.controller.transportControls.skipToNext()
                        exoPlayer.next()
                    }
                    "repeat_one" -> mediaSession.controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                    "repeat_all" -> mediaSession.controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                    "repeat_off" -> mediaSession.controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                    "shuffle_on" -> mediaSession.controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                    "shuffle_off" -> mediaSession.controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        exoPlayer.release()
        mediaSession.isActive = false
        mediaSession.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

/**
 * バッググラウンド連続再生サービス（名前どうにかしたい）
 * @param context Context
 * @param startVideoId 再生開始位置を動画IDで指定するときは入れてね。入れない場合は最初から再生します。
 * */
internal fun startBackgroundPlaylistPlayService(context: Context?, startVideoId: String = "") {
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