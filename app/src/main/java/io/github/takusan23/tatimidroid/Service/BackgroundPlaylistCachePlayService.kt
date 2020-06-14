package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*


/**
 * キャッシュ連続再生用Service。
 * 入れてほしいもの↓
 * start_id     | String | 動画の再生開始のIDを指定するときに入れる。任意です。
 * */
class BackgroundPlaylistCachePlayService : MediaBrowserServiceCompat() {

    lateinit var notificationManager: NotificationManager
    lateinit var prefSessing: SharedPreferences
    lateinit var broadcastReceiver: BroadcastReceiver
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var mediaSessionCompat: MediaSessionCompat
    lateinit var nicoVideoCache: NicoVideoCache

    // 通知ID
    val NOTIFICAION_ID = 1919

    // 開始位置
    var startVideoId = ""

    // さあ？
    private val ROOT_ID = "background_playlist_service"

    // 動画一覧
    var videoList = arrayListOf<NicoVideoData>()

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nicoVideoCache = NicoVideoCache(this)
        prefSessing = PreferenceManager.getDefaultSharedPreferences(this)

        // リピート/シャッフル 切り替え受け取り用ブロードキャスト
        initBroadcast()

        // MediaSessionCompat生成
        initMediaSessionCompat()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // 開始位置を取得
        startVideoId = intent?.getStringExtra("start_id") ?: ""

        // ExoPlayer用意
        initExoPlayer()

        // 再生
        mediaSessionCompat.controller.transportControls.apply {
            // 再生
            playFromMediaId(startVideoId, null)
            // 前回リピートモード有効にしてたか
            val repeatMode = prefSessing.getInt("cache_repeat_mode", 0)
            setRepeatMode(repeatMode)
            // 前回シャッフルモードを有効にしていたか
            val isShuffleMode = prefSessing.getBoolean("cache_shuffle_mode", false)
            val shuffleMode = if (isShuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_ALL
            } else {
                PlaybackStateCompat.REPEAT_MODE_NONE
            }
            setShuffleMode(shuffleMode)
        }

        return START_NOT_STICKY
    }

    /**
     * ExoPlayer初期化
     * */
    private fun initExoPlayer() {
        exoPlayer = SimpleExoPlayer.Builder(this).build()

        // ExoPlayerのイベント
        exoPlayer.addListener(object : Player.EventListener {
            // 再生状態変わったら
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                // 通知/状態 更新
                updateState()
                createNotification()
            }

            // 次の曲いったら
            override fun onPositionDiscontinuity(reason: Int) {
                super.onPositionDiscontinuity(reason)
                // 通知/状態 更新
                updateState()
                createNotification()
            }

            // リピート条件変わったら
            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                // 通知/状態 更新
                updateState()
                createNotification()
                // リピート再生かどうかを保持するように。保存する値はBooleanじゃなくて数値です（Player.REPEAT_MODE_OFFなど）
                prefSessing.edit { putInt("cache_repeat_mode", repeatMode) }
            }

            // シャッフル有効・無効切り替わったら
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                // 通知/状態 更新
                updateState()
                createNotification()
                // シャッフル再生かどうかを保持するように。こっちはtrue/falseです。
                prefSessing.edit { putBoolean("cache_shuffle_mode", shuffleModeEnabled) }
            }
        })

    }

    /**
     * MediaSessionCompat用意。
     * */
    private fun initMediaSessionCompat() {
        // MediaSession用意
        mediaSessionCompat = MediaSessionCompat(this, "background_playlist_play_session").apply {
            /**
             * コールバックの設定
             * もしなんか動かない時は、updateState()関数の setActions() に追加済みかどうかを確認してね。
             * */
            setCallback(object : MediaSessionCompat.Callback() {
                /** 再生前 */
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    super.onPlayFromMediaId(mediaId, extras)
                    // nullなら落とす
                    mediaId ?: return
                    // 動画一覧読み込む
                    GlobalScope.launch {
                        videoList = getVideoList().await()
                        // MediaSource作る
                        val playList = ConcatenatingMediaSource()
                        videoList.forEach { cacheData ->
                            // 動画IDからUri生成からExoPlayerで再生
                            val videoFilePathUri = nicoVideoCache.getCacheFolderVideoFilePath(cacheData.videoId)
                            // MediaSource
                            val dataSourceFactory = DefaultDataSourceFactory(this@BackgroundPlaylistCachePlayService, "TatimiDroid;@takusan_23")
                            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                                .setTag(cacheData.videoId)
                                .createMediaSource(videoFilePathUri.toUri())
                            playList.addMediaSource(mediaSource)
                        }
                        // 開始位置から再生
                        val index = videoList.indexOfFirst { cacheData -> cacheData.videoId == startVideoId }
                        withContext(Dispatchers.Main) {
                            exoPlayer.apply {
                                // プレイリストセットする
                                prepare(playList)
                                // seekToで移動
                                seekTo(index, 0L)
                                // 通知/状態 更新
                                updateState()
                                createNotification()
                                // 再生
                                onPlay()
                            }
                        }
                    }
                }

                /** 再生 */
                override fun onPlay() {
                    super.onPlay()
                    exoPlayer.playWhenReady = true
                    mediaSessionCompat.isActive = true
                    // 通知/状態 更新
                    updateState()
                    createNotification()
                }

                /** 一時停止 */
                override fun onPause() {
                    super.onPause()
                    exoPlayer.playWhenReady = false
                    mediaSessionCompat.isActive = false
                    // 通知/状態 更新
                    updateState()
                    createNotification()
                }

                /** シーク */
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    exoPlayer.seekTo(pos)
                    // 通知/状態 更新
                    updateState()
                    createNotification()
                }

                /** 次の曲 */
                override fun onSkipToNext() {
                    super.onSkipToNext()
                    // いま再生中の場所取る
                    exoPlayer.next()
                }

                /** 前の曲 */
                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    exoPlayer.previous()
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

                /** 再生終了 */
                override fun onStop() {
                    super.onStop()
                    exoPlayer.release()
                    mediaSessionCompat.isActive = false
                    release()
                    stopSelf()
                }
            })

            // ？
            setSessionToken(sessionToken)

        }

        updateState()

    }


    /**
     * 曲リストを返す。
     * でもなにも書かない状態でもなんか動くので謎。
     * */
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // 許可制らしい？
        if (parentId == ROOT_ID) {
            // 曲を読み込む
            GlobalScope.launch {
                // クライアントへどぞー
                //  result.sendResult(getVideoList().await())
            }
        }
    }

    /**
     * 再生する動画一覧を返す。コルーチンで使ってね。
     * @return NicoVideoDataの配列
     * */
    private fun getVideoList(): Deferred<ArrayList<NicoVideoData>> = GlobalScope.async {
        // 取得
        var videoList = nicoVideoCache.loadCache().await()
        val filter = CacheJSON().readJSON(this@BackgroundPlaylistCachePlayService)
        // フィルター
        videoList = if (filter != null) {
            nicoVideoCache.getCacheFilterList(videoList, filter)
        } else {
            videoList
        }
        return@async videoList
    }

    /**
     * クライアント接続の制御（さあ？）
     * */
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // わからんからすべて許可（自動ドアとかいうな）
        return BrowserRoot(ROOT_ID, null)
    }

    /**
     * 動画IDからメタデータを生成する関数。
     * @param videoId 動画ID
     * @param duration 動画の長さ。ミリ秒
     * @return メタデータ。
     * */
    private fun createMetaData(videoId: String): MediaMetadataCompat {
        // 動画パス
        val videoFilePath = nicoVideoCache.getCacheFolderVideoFilePath(videoId)
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
            val thumbPath = nicoVideoCache.getCacheFolderVideoThumFilePath(videoId)
            val currentThumbBitmap = BitmapFactory.decodeFile(thumbPath)
            // 再生時間
            val duration = jsonObject.getJSONObject("video").getLong("duration")
            // メタデータ
            val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
                // Android 11 の MediaSession で使われるやつ
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, thumbPath)
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentTitle)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, uploaderName)
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration * 1000) // これあるとAndroid 10でシーク使えます
                // ？
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, videoFilePath)
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, currentTitle)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, uploaderName)
                putBitmap(MediaMetadataCompat.METADATA_KEY_ART, currentThumbBitmap)
            }.build()
            return mediaMetadataCompat
        } else {
            // 動画情報JSONなかった
            // メタデータ
            val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, videoFilePath)
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, nicoVideoCache.getCacheFolderVideoFileName(videoId)) // ファイル名
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, videoId)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, nicoVideoCache.getCacheFolderVideoFileName(videoId))
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, videoId)
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, nicoVideoCache.getVideoDurationSec(videoId) * 1000) // これあるとAndroid 10でシーク使えます
            }.build()
            return mediaMetadataCompat
        }
    }

    /**
     * 通知と再生状態（MediaSessionの再生状態）とメタデータを更新する
     * 受け付ける操作（例：一時停止「PlaybackStateCompat.ACTION_PLAY」など）もここで定義します。
     * */
    private fun updateState() {
        // ExoPlayer初期化済みか
        val isInitExoPlayer = ::exoPlayer.isInitialized
        // MediaSession再生状態変更
        val state = if (isInitExoPlayer && exoPlayer.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        // 現在の位置
        val currentPos = if (isInitExoPlayer) {
            exoPlayer.currentPosition
        } else {
            1000
        }
        // 再生中の状態変更に対応する。
        val stateBuilder = PlaybackStateCompat.Builder().apply {
            // 重要：受け付ける操作。CallBack書いたのに動かない時は見てみて（とゆうか最初から全部いれとけよぼけ）
            setActions(
                PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH or PlaybackStateCompat.ACTION_PLAY_FROM_URI or
                        PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or PlaybackStateCompat.ACTION_PREPARE_FROM_URI or PlaybackStateCompat.ACTION_PREPARE
            )
            // 再生状態
            setState(state, currentPos, 1f)
        }
        mediaSessionCompat.setPlaybackState(stateBuilder.build())

        // メタデータ更新など
        if (isInitExoPlayer && exoPlayer.currentTag is String) {
            // 再生中の動画IDはタグに入ってるので
            val videoId = exoPlayer.currentTag as String
            // メタデータセット
            mediaSessionCompat.setMetadata(createMetaData(videoId))
        }

    }

    /**
     * 通知を作成する。
     * */
    private fun createNotification() {
        this@BackgroundPlaylistCachePlayService.mediaSessionCompat.apply {
            // 通知更新
            if (controller.metadata != null) {
                // MediaMetadataCompat#METADATA_KEY_MEDIA_ID に 動画IDを詰めている。
                val videoId = controller.metadata.description.mediaId ?: return
                val title = controller.metadata.description.title
                val artist = controller.metadata.description.subtitle
                val bitmap = controller.metadata.description.iconBitmap
                showNotification(title as String, artist as String, bitmap)
            } else {
                showNotification("まだ再生していません", "準備中です")
            }
        }
    }

    /**
     * 通知作成。基本はcreateNotification()を使うのでこれを直接呼ぶことはない。
     * */
    private fun showNotification(title: String = "", uploaderName: String = "", thumb: Bitmap? = null) {
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
            // 停止ボタン
            addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_STOP)))
            if (::exoPlayer.isInitialized && exoPlayer.playWhenReady) {
                // 一時停止
                addAction(NotificationCompat.Action(R.drawable.ic_pause_black_24dp, "play", MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_PAUSE)))
            } else {
                // 再生
                addAction(NotificationCompat.Action(R.drawable.ic_play_arrow_24px, "pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_PLAY)))
            }
            // 次の曲（設定で前の曲にできる）
            if (prefSessing.getBoolean("setting_cache_background_play_prev_button", false)) {
                // 前の曲ボタン
                addAction(NotificationCompat.Action(R.drawable.ic_skip_previous_black_24dp, "prev", MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)))
            } else {
                // 次の曲ボタン
                addAction(NotificationCompat.Action(R.drawable.ic_skip_next_black_24dp, "next", MediaButtonReceiver.buildMediaButtonPendingIntent(this@BackgroundPlaylistCachePlayService, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)))
            }


            setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(sessionToken).run {
                setShowActionsInCompactView(0, 1, 3) // 複数個アイコンを表示（３個までだが）する際は区切りで指定
            })

            /**
             * リピート・シャッフル 用ボタン。こいつら多分 MediaButtonReceiver.buildMediaButtonPendingIntent に用意されてないので、自前でブロードキャストもらう。
             * */
            val repeatOneIntent = Intent("repeat_one")
            val repeatAllIntent = Intent("repeat_all")
            val repeatOffIntent = Intent("repeat_off")
            val shuffleOn = Intent("shuffle_on")
            val shuffleOff = Intent("shuffle_off")
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

    /**
     * リピートモード切り替え、シャッフルON/OFFは多分無理。しゃーないのでブロードキャストをもらう。
     * */
    private fun initBroadcast() {
        val intentFilter = IntentFilter().apply {
            addAction("repeat_one")
            addAction("repeat_all")
            addAction("repeat_off")
            addAction("shuffle_on")
            addAction("shuffle_off")
        }
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        exoPlayer.release()
        mediaSessionCompat.isActive = false
        mediaSessionCompat.release()
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