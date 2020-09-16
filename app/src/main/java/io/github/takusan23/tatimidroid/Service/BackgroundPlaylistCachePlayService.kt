package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.*
import org.json.JSONObject


/**
 * キャッシュ連続再生用。Androidのドキュメントに従ってメディアブラウザサービスを作った。
 *
 * 注意：再生する際は、prepare() -> play() ではなく playFromMediaId() を使ってください。
 *
 * [MediaSessionCompat.Callback.onPlayFromMediaId]の中でprepare()の処理をします（は？）
 *
 * ```
 * // 例
 * mediaControllerCompat.transportControls.playFromMediaId("sm157", null)
 * ```
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

    /** リピート有効等の操作はブロードキャストで受け取る */
    lateinit var broadcastReceiver: BroadcastReceiver

    /** MediaSession初期化など */
    override fun onCreate() {
        super.onCreate()

        // 通知出すのに使う
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ExoPlayer用意
        exoPlayer = SimpleExoPlayer.Builder(this).build()

        // ブロードキャスト用意
        initBroadcast()

        // MediaSession用意
        mediaSessionCompat = MediaSessionCompat(this, "media_session").apply {

            // MediaSessionの操作のコールバック
            setCallback(object : MediaSessionCompat.Callback() {

                /** Android 11 のメディアの再開の時使う。 */
                override fun onPrepare() {
                    super.onPrepare()
                    GlobalScope.launch(cachePlayServiceCoroutineJob) {
                        // 最後に聞いた曲
                        val videoId = prefSetting.getString("cache_last_play_video_id", "")
                        loadCacheVideo(videoId)
                    }
                }

                /**
                 * 動画IDを指定して再生。
                 * 通常再生時はこちらを呼んでください
                 * */
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    super.onPlayFromMediaId(mediaId, extras)
                    mediaId ?: return
                    // 動画読み込む
                    GlobalScope.launch(cachePlayServiceCoroutineJob) {
                        loadCacheVideo(mediaId)
                        // 再生する
                        onPlay()
                    }
                }

                /** 再生 */
                override fun onPlay() {
                    super.onPlay()
                    startThisService()
                    exoPlayer.playWhenReady = true
                    isActive = true
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

                /** 前の曲 */
                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    exoPlayer.previous()
                }

                /** 次の曲 */
                override fun onSkipToNext() {
                    super.onSkipToNext()
                    exoPlayer.next()
                }

                /** リピートモード切り替え */
                override fun onSetRepeatMode(repeatMode: Int) {
                    super.onSetRepeatMode(repeatMode)
                    when (repeatMode) {
                        PlaybackStateCompat.REPEAT_MODE_NONE -> {
                            // 一周したら終わり
                            exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                        }
                        PlaybackStateCompat.REPEAT_MODE_ALL -> {
                            // 無限ループループする
                            exoPlayer.repeatMode = Player.REPEAT_MODE_ALL
                        }
                        PlaybackStateCompat.REPEAT_MODE_ONE -> {
                            // 同じ曲を何回も聞く。
                            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                        }
                    }
                }

                /** シャッフルモード切り替え */
                override fun onSetShuffleMode(shuffleMode: Int) {
                    super.onSetShuffleMode(shuffleMode)
                    val isShuffleMode = shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL
                    exoPlayer.shuffleModeEnabled = isShuffleMode
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

            // 前回リピートモード有効にしてたか
            val repeatMode = prefSetting.getInt("cache_repeat_mode", 0)
            controller.transportControls.setRepeatMode(repeatMode)
            // 前回シャッフルモードを有効にしていたか
            val isShuffleMode = prefSetting.getBoolean("cache_shuffle_mode", false)
            val shuffleMode = if (isShuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL
            } else {
                PlaybackStateCompat.REPEAT_MODE_NONE
            }
            controller.transportControls.setShuffleMode(shuffleMode)

        }

        // ExoPlayerの再生状態が更新されたときも通知を更新する
        exoPlayer.addListener(object : Player.EventListener {
            // 再生状態変わったら
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                // 通知/状態 更新
                updateState()
                showNotification()
            }

            // 次の曲いったら
            override fun onPositionDiscontinuity(reason: Int) {
                super.onPositionDiscontinuity(reason)
                // 通知/状態 更新
                updateState()
                showNotification()
            }

            // リピート条件変わったら
            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                // 通知/状態 更新
                updateState()
                showNotification()
                // リピート再生かどうかを保持するように。保存する値はBooleanじゃなくて数値です（Player.REPEAT_MODE_OFFなど）
                prefSetting.edit { putInt("cache_repeat_mode", repeatMode) }
            }

            // シャッフル有効・無効切り替わったら
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                // 通知/状態 更新
                updateState()
                showNotification()
                // シャッフル再生かどうかを保持するように。こっちはtrue/falseです。
                prefSetting.edit { putBoolean("cache_shuffle_mode", shuffleModeEnabled) }
            }
        })

    }

    /**
     * キャッシュ一覧を読み込んでExoPlayerに詰める。なんとなくコルーチンで
     * @param seekVideoId 指定すると指定した動画IDから再生を始めます
     * */
    suspend fun loadCacheVideo(seekVideoId: String? = null) = withContext(Dispatchers.Default) {
        // フィルター条件JSONを読む
        val filterJSON = CacheJSON().readJSON(this@BackgroundPlaylistCachePlayService)
        // 動画一覧
        val videoList = if (filterJSON != null) {
            nicoVideoCache.getCacheFilterList(nicoVideoCache.loadCache(), filterJSON)
        } else {
            nicoVideoCache.loadCache()
        }
        // ExoPlayerのプレイリスト機能
        val concatenatedSource = ConcatenatingMediaSource()
        val dataSourceFactory = DefaultDataSourceFactory(this@BackgroundPlaylistCachePlayService, "TatimiDroid;@takusan_23")
        videoList.forEach { video ->
            // プレイリストに追加
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .setTag(video.videoId)
                .createMediaSource(nicoVideoCache.getCacheFolderVideoFilePath(video.videoId).toUri()) // 動画の場所
            concatenatedSource.addMediaSource(mediaSource)
        }
        // 再生位置を出す
        val index = if (seekVideoId != null) {
            videoList.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == seekVideoId }
        } else {
            0
        }
        withContext(Dispatchers.Main) {
            exoPlayer.prepare(concatenatedSource)
            exoPlayer.seekTo(index, 0) // 開始位置
        }
    }

    /**
     * 再生状態とメタデータを設定する。
     * なんかしらんけど、setMetadata()あたりが調子悪い
     *
     * MediaSessionのsetCallBackで扱う操作([MediaSessionCompat.Callback.onPlay]など)も[PlaybackStateCompat.Builder.setState]に書かないと何も起きない
     * */
    private fun updateState() {
        val stateBuilder = PlaybackStateCompat.Builder().apply {
            // 取り扱う操作。
            setActions(
                PlaybackStateCompat.ACTION_PREPARE
                        or PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_SEEK_TO
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                        or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                        or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
            // 再生してるか。ExoPlayerを参照
            val state = if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            // 位置
            val position = exoPlayer.currentPosition
            // 再生状態を更新
            setState(state, position, 1.0f) // 最後は再生速度
        }.build()
        mediaSessionCompat.setPlaybackState(stateBuilder)
        val metadata = when {
            exoPlayer.currentTag is String -> {
                // 再生中
                // 最後に再生した曲を保存しておく。Android 11 の メディアの再開 で使う
                prefSetting.edit { putString("cache_last_play_video_id", exoPlayer.currentTag as String) }
                createMetaData(exoPlayer.currentTag as String)
            }
            exoPlayer.playbackState == Player.STATE_IDLE -> {
                // 再生していないときは最後に再生してた曲を
                val recentVideoId = prefSetting.getString("cache_last_play_video_id", null) ?: return // nullなら何もしない
                createMetaData(recentVideoId)
            }
            else -> {
                // 仮のデータ。なんかここらへんがおかしいと通知が二重で発行される
                MediaMetadataCompat.Builder().apply {
                    // Android 11 の MediaSession で使われるやつ
                    putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.loading))
                    putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.loading))
                }.build()
            }
        }
        mediaSessionCompat.setMetadata(metadata)
    }

    /**
     * 動画IDからメタデータを生成する関数。
     * @param videoId 動画ID
     * @param duration 動画の長さ。ミリ秒
     * @return メタデータ。
     * */
    private fun createMetaData(videoId: String): MediaMetadataCompat {
        // 動画情報JSONが存在するか
        if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
            // 動画情報JSON取得
            val videoJSON = nicoVideoCache.getCacheFolderVideoInfoText(videoId)
            val jsonObject = JSONObject(videoJSON)
            val currentTitle = jsonObject.getJSONObject("video").getString("title")
            // 投稿者
            val uploaderName = NicoVideoHTML().getUploaderName(jsonObject)
            val thumbPath = nicoVideoCache.getCacheFolderVideoThumFilePath(videoId)
            val thumbBitmap = BitmapFactory.decodeFile(thumbPath)
            // 再生時間
            val duration = jsonObject.getJSONObject("video").getLong("duration")
            // メタデータ
            val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
                // Android 11 の MediaSession で使われるやつ
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, uploaderName)
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration * 1000) // これあるとAndroid 10でシーク使えます
                putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, thumbBitmap) // サムネ。えっっっ
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, thumbPath)
            }.build()
            return mediaMetadataCompat
        } else {
            // 動画情報JSONなかった
            // メタデータ
            val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, nicoVideoCache.getCacheFolderVideoFileName(videoId)) // ファイル名
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, videoId)
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, nicoVideoCache.getVideoDurationSec(videoId) * 1000) // これあるとAndroid 10でシーク使えます
            }.build()
            return mediaMetadataCompat
        }
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
            setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken).setShowActionsInCompactView(0, 1, 3))
            setSmallIcon(R.drawable.ic_tatimidroid_playlist_play_black)

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
                // Android 11 からは MediaSession から値をもらってsetContentTextしてくれるけど10以前はしてくれないので
                mediaSessionCompat.controller.metadata?.apply {
                    getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART).apply {
                        setLargeIcon(this)
                    }
                    setContentTitle(getText(MediaMetadataCompat.METADATA_KEY_TITLE))
                    setContentText(getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
                }
            }

            /**
             * ボタン。本来はMediaButtonReceiverを使うんだけど、なんかメディアの再開だとうまく動かないのでBroadcastで代替
             * */
            val pauseIntent = Intent("pause") // こいつ本来は MediaButtonReceiver が使えるはずなんだけど、メディアの再開だと動かない
            val playIntent = Intent("play") // のでBroadcastを経由して操作することに（えぇ）
            val stopIntent = Intent("stop") // まじでなんで使えんの？
            val prevIntent = Intent("prev")
            val nextIntent = Intent("next")
            val repeatOneIntent = Intent("repeat_one")
            val repeatAllIntent = Intent("repeat_all")
            val repeatOffIntent = Intent("repeat_off")
            val shuffleOn = Intent("shuffle_on")
            val shuffleOff = Intent("shuffle_off")
            // 停止ボタン
            addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 30, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            if (::exoPlayer.isInitialized && exoPlayer.playWhenReady) {
                // 一時停止
                addAction(NotificationCompat.Action(R.drawable.ic_pause_black_24dp, "play", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 31, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            } else {
                // 再生
                addAction(NotificationCompat.Action(R.drawable.ic_play_arrow_24px, "pause", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 32, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }
            // 次の曲（設定で前の曲にできる）
            if (prefSetting.getBoolean("setting_cache_background_play_prev_button", false)) {
                // 前の曲ボタン
                addAction(NotificationCompat.Action(R.drawable.ic_skip_previous_black_24dp, "prev", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 33, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            } else {
                // 次の曲ボタン
                addAction(NotificationCompat.Action(R.drawable.ic_skip_next_black_24dp, "next", PendingIntent.getBroadcast(this@BackgroundPlaylistCachePlayService, 33, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }
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
        /**
         * 本来はここで曲（いや動画だけど）一覧を返すんだけど、
         * 返した後にシャッフル再生有効にすると順番ぐちゃぐちゃになると思うから返しても意味が無さそうな気がする。
         * なので今はAndroid 11のメディアの再開に備えて、最後に再生した曲を返す
         * */
        if (parentId == ROOT_RECENT) {
            // 最後に再生した曲を返す
            val recentVideoId = prefSetting.getString("cache_last_play_video_id", null) ?: return // nullなら何もしない
            // 最後に聞いてる曲を返却
            result.sendResult(arrayListOf(createMediaItem(recentVideoId, createMetaData(recentVideoId))))
        }
    }

    /**
     * [onLoadChildren]で返すアイテムを作成する
     * */
    private fun createMediaItem(videoId: String, metadataCompat: MediaMetadataCompat): MediaBrowserCompat.MediaItem {
        // サ胸パス
        val thumbPath = nicoVideoCache.getCacheFolderVideoThumFilePath(videoId)
        val mediaDescriptionCompat = MediaDescriptionCompat.Builder().apply {
            setTitle(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            setSubtitle(metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            setMediaId(videoId)
            setIconUri(thumbPath.toUri())
            setIconBitmap(BitmapFactory.decodeFile(thumbPath))
        }.build()
        return MediaBrowserCompat.MediaItem(mediaDescriptionCompat, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    /**
     * リピートモード切り替え、シャッフルON/OFFは多分無理。しゃーないのでブロードキャストをもらう。
     * */
    private fun initBroadcast() {
        val intentFilter = IntentFilter().apply {
            addAction("pause")
            addAction("play")
            addAction("stop")
            addAction("prev")
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
                    "pause" -> mediaSessionCompat.controller.transportControls.pause()
                    "play" -> mediaSessionCompat.controller.transportControls.play()
                    "stop" -> mediaSessionCompat.controller.transportControls.stop()
                    "prev" -> mediaSessionCompat.controller.transportControls.skipToPrevious()
                    "next" -> mediaSessionCompat.controller.transportControls.skipToNext()
                    "repeat_one" -> mediaSessionCompat.controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                    "repeat_all" -> mediaSessionCompat.controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL)
                    "repeat_off" -> mediaSessionCompat.controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                    "shuffle_on" -> mediaSessionCompat.controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL)
                    "shuffle_off" -> mediaSessionCompat.controller.transportControls.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE)
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    /** あとしまつ。帰りの会 / HR。掃除当番だとつらい */
    override fun onDestroy() {
        super.onDestroy()
        mediaSessionCompat.release()
        exoPlayer.release()
        unregisterReceiver(broadcastReceiver)
    }

}

