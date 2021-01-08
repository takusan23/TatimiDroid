package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.mediarouter.media.MediaItemMetadata
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.isLoginMode
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.ArrayList

/**
 * [android.media.session.MediaController.TransportControls.prepareFromMediaId]りようしてね
 * */
class NicoVideoPlayMediaBrowserService : MediaBrowserServiceCompat() {

    /** 設定保存 */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    /** [onLoadChildren]でparentIdに入ってくる。Android 11のメディアの再開の場合はこの値 */
    private val ROOT_RECENT = "root_recent"

    /** [onLoadChildren]でparentIdに入ってくる。[ROOT_RECENT]以外の場合 */
    private val ROOT = "root"

    /** 動画再生するやつ */
    private val exoPlayer by lazy { SimpleExoPlayer.Builder(this).build() }

    /** 通知出すやつ */
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    /** MediaSession。音声情報を外部へ */
    private lateinit var mediaSessionCompat: MediaSessionCompat

    /** 動画一覧 */
    private val videoList = arrayListOf<NicoVideoData>()

    /** ニコ動API叩くときに使う */
    private val nicoVideoHTML = NicoVideoHTML()

    /** キャッシュ関連 */
    private val nicoVideoCache = NicoVideoCache(this)

    /** 現在再生中の動画のデータクラス */
    private var playingNicoVideoData: NicoVideoData? = null

    /** 再生中の動画のコメント配列 */
    private val commentList = arrayListOf<CommentJSONParse>()

    /**
     * クライアントからの接続を制御。とりあえずすべて許可
     * */
    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // 最後の曲をリクエストしている場合はtrue
        val isRequestRecentMusic = rootHints?.getBoolean(BrowserRoot.EXTRA_RECENT) ?: false
        // BrowserRootに入れる値を変える
        val rootPath = if (isRequestRecentMusic) ROOT_RECENT else ROOT
        return BrowserRoot(rootPath, null)
    }

    /**
     * クライアントへ曲一覧を返す。
     *
     * けど曲一覧ここでは返さないため、Android 11のMediaResumeにだけ対応させる
     * */
    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // 多分遅いので先に呼んでおく
        result.detach()
        /**
         * 最後の曲を返す。
         * */
        if (parentId == ROOT_RECENT) {
            // 最後に再生した曲を返す
            val recentVideoId = prefSetting.getString("nicovideo_player_service_last_id", null) ?: return // nullなら何もしない
            // 最後に聞いてる曲を返却
            GlobalScope.launch {
                // metadataからdescription作成
                val metadata = getMediaMetadata(recentVideoId)
                if (metadata != null) {
                    val thumbPath = nicoVideoCache.getCacheFolderVideoThumFilePath(recentVideoId)
                    val mediaDescriptionCompat = MediaDescriptionCompat.Builder().apply {
                        setTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                        setSubtitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                        setMediaId(recentVideoId)
                        setIconUri(thumbPath.toUri())
                        setIconBitmap(BitmapFactory.decodeFile(thumbPath))
                    }.build()
                    val item = MediaBrowserCompat.MediaItem(mediaDescriptionCompat, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                    // 結果送信
                    result.sendResult(arrayListOf(item))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // MediaSession初期化
        mediaSessionCompat = MediaSessionCompat(this, "tatimidroid_nicovideo_service_media_session").apply {
            setCallback(object : MediaSessionCompat.Callback() {

                override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
                    super.onPrepareFromMediaId(mediaId, extras)
                    // 動画配列を受け取る
                    val list = extras?.getSerializable("video_list") as? ArrayList<NicoVideoData>
                    videoList.clear()
                    if (list != null) {
                        videoList.addAll(list)
                        // 再生準備
                        cachePlay(list[0].videoId)
                    }
                }

                override fun onPlay() {
                    super.onPlay()
                    exoPlayer.playWhenReady = true
                    isActive = true
                    // サービス起動
                    startThisService()
                }

                override fun onPause() {
                    super.onPause()
                    exoPlayer.playWhenReady = false
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    nextVideo()
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    prevVideo()
                }

                override fun onStop() {
                    super.onStop()
                    isActive = false
                    // サービス終了
                    stopSelf()
                }
            })
        }

        // TokenをBrowserServiceへ渡す
        sessionToken = mediaSessionCompat.sessionToken

        // ExoPlayerのコールバックでMediaSessionの状態を変更
        exoPlayer.addListener(object : Player.EventListener {

            // playWhenReadyが切り替わったら呼ばれる
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                super.onPlayWhenReadyChanged(playWhenReady, reason)
                // 通知/状態 更新
                updateState()
                showNotification()
            }

            // playbackStateが変わったら呼ばれる
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
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

    /** 前の動画に移動する */
    private fun prevVideo() {
        val currentPos = videoList.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == playingNicoVideoData?.videoId }
        val prevVideoPos = if (currentPos - 1 >= 0) {
            // 次の動画がある
            currentPos - 1
        } else {
            // 最初の動画にする
            videoList.size - 1
        }
        val videoData = videoList[prevVideoPos]
        // 再生
        if (videoData.isCache) {
            cachePlay(videoData.videoId)
        }
    }

    /** 次の動画に移動する */
    private fun nextVideo() {
        val currentPos = videoList.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == playingNicoVideoData?.videoId }
        val nextVideoPos = if (currentPos + 1 < videoList.size) {
            // 次の動画がある
            currentPos + 1
        } else {
            // 最初の動画にする
            0
        }
        val videoData = videoList[nextVideoPos]
        // 再生
        if (videoData.isCache) {
            cachePlay(videoData.videoId)
        }
    }

    /**
     * サービスを起動してActivityが終了しても再生続けられるように
     * */
    private fun startThisService() {
        val intent = Intent(this, NicoVideoPlayMediaBrowserService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * キャッシュを利用して動画を再生
     * @param videoId 動画ID
     * */
    private fun cachePlay(videoId: String) {
        // 後始末
        nicoVideoHTML.destroy()

        // コメントファイルがxmlならActivity終了
        val xmlCommentJSON = XMLCommentJSON(this)
        if (xmlCommentJSON.commentXmlFilePath(videoId) != null && !xmlCommentJSON.commentJSONFileExists(videoId)) {
            // xml形式はあるけどjson形式がないときは落とす
            Toast.makeText(this, R.string.xml_comment_play, Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        } else {
            // タイトル
            playingNicoVideoData = videoList.find { nicoVideoData -> nicoVideoData.videoId == videoId }
            GlobalScope.launch {
                // 動画のファイル名取得
                val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                if (videoFileName != null) {
                    val contentUrl = "${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName"
                    // コメント取得
                    val commentJSON = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                    commentList.clear()
                    commentList.addAll(nicoVideoHTML.parseCommentJSON(commentJSON, videoId))
                    withContext(Dispatchers.Main) {
                        // ExoPlayer
                        playExoPlayer(true, contentUrl, "")
                        if (isPopupPlay()) {
                            // プレイヤーにセット
                            // キャッシュなので
                            // showNetworkType(true)
                        }
                    }
                } else {
                    // 動画が見つからなかった。
                    showToast(getString(R.string.not_found_video))
                    stopSelf()
                    return@launch
                }
            }
        }
    }

    /**
     * ポップアップ再生かどうかを返す
     * todo always false
     * */
    private fun isPopupPlay(): Boolean {
        return false
    }

    /**
     * ExoPlayerで動画を再生する
     * @param isCache キャッシュで再生する場合はtrue
     * @param contentUrl 動画URL
     * @param nicoHistory smile鯖の動画を再生する場合はCookieいれて
     * */
    private fun playExoPlayer(isCache: Boolean, contentUrl: String, nicoHistory: String) {
        // キャッシュ再生と分ける
        if (isCache) {
            // キャッシュ再生
            val dataSourceFactory = DefaultDataSourceFactory(this, "TatimiDroid;@takusan_23")
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(contentUrl.toUri()))
            exoPlayer.setMediaSource(videoSource)
        } else {
            // SmileサーバーはCookieつけないと見れないため
            val dataSourceFactory = DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
            dataSourceFactory.defaultRequestProperties.set("Cookie", nicoHistory)
            val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(contentUrl.toUri()))
            exoPlayer.setMediaSource(videoSource)
        }
        exoPlayer.prepare()
        // シーク
        // exoPlayer.seekTo(seekMs)
        // 自動再生
        mediaSessionCompat.controller.transportControls?.play()
        // MediaSession。通知もう一階出せばなんか表示されるようになった。Androidむずかちい
        // showNotification(currentVideoTitle)
        // initMediaSession()
    }

    /**
     * Toastを表示する関数
     * @param s メッセージ
     * */
    private fun showToast(s: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * MediaSessionので扱う操作、再生状態などを更新する
     */
    private fun updateState() {
        // 再生状態、受け付ける操作の定義など
        val state = PlaybackStateCompat.Builder().apply {
            // 受け付ける操作
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
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT,
            )
            // 再生してるか。ExoPlayerを参照
            val state = if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            // 位置
            val position = exoPlayer.currentPosition
            // 再生状態を更新
            setState(state, position, 1.0f) // 最後は再生速度
        }.build()
        mediaSessionCompat.setPlaybackState(state)
        // MediaSessionへ動画情報を渡す
        if (playingNicoVideoData != null) {
            // 再生中
            GlobalScope.launch {
                // 最後に再生した曲を保存しておく。Android 11 の メディアの再開 や 連続再生の開始（無指定の時） で使う
                prefSetting.edit { putString("nicovideo_player_service_last_id", playingNicoVideoData!!.videoId) }
                val metadata = getMediaMetadata(playingNicoVideoData!!.videoId)
                mediaSessionCompat.setMetadata(metadata)
            }
        }
    }

    /** 通知を表示する */
    private fun showNotification() {
        // 通知を作成。通知チャンネルのせいで長い
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "video_popup"
            val notificationChannel = NotificationChannel(notificationChannelId, getString(R.string.video_popup_background_play_service), NotificationManager.IMPORTANCE_HIGH)
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, notificationChannelId)
        } else {
            NotificationCompat.Builder(this)
        }
        notification.apply {
            // 音楽Style+アイコン指定
            setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken).setShowActionsInCompactView(0, 1, 3))
            setSmallIcon(if (isPopupPlay()) R.drawable.ic_popup_icon_black else R.drawable.ic_tatimidroid_playlist_play_black)
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
            // 終了ボタン
            addAction(R.drawable.ic_clear_black, "終了", MediaButtonReceiver.buildMediaButtonPendingIntent(this@NicoVideoPlayMediaBrowserService, PlaybackStateCompat.ACTION_STOP))
            // 前の動画
            addAction(R.drawable.ic_skip_previous_black_24dp, "前の動画", MediaButtonReceiver.buildMediaButtonPendingIntent(this@NicoVideoPlayMediaBrowserService, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
            // 一時停止
            if (exoPlayer.playWhenReady) {
                addAction(R.drawable.ic_pause_black_24dp, "一時停止", MediaButtonReceiver.buildMediaButtonPendingIntent(this@NicoVideoPlayMediaBrowserService, PlaybackStateCompat.ACTION_PAUSE))
            } else {
                addAction(R.drawable.ic_play_arrow_24px, "再生", MediaButtonReceiver.buildMediaButtonPendingIntent(this@NicoVideoPlayMediaBrowserService, PlaybackStateCompat.ACTION_PLAY))
            }
            // 次の
            addAction(R.drawable.ic_skip_next_black_24dp, "次", MediaButtonReceiver.buildMediaButtonPendingIntent(this@NicoVideoPlayMediaBrowserService, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
            // ポップアップ再生・バックグラウンド再生 切り替え
            if (isPopupPlay()) {
                addAction(R.drawable.ic_tatimidroid_playlist_play_black, "切り替え", null)
            } else {
                addAction(R.drawable.ic_popup_icon_black, "切り替え", null)
            }
        }
    }

    /**
     * 動画IDから[MediaMetadataCompat]を作成する
     * @param videoData 動画情報
     * */
    private suspend fun getMediaMetadata(videoId: String) = withContext(Dispatchers.Default) {
        val hasCache = nicoVideoCache.existsCacheVideoInfoJSON(videoId)
        return@withContext if (hasCache) {
            val data = nicoVideoHTML.createNicoVideoData(JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId)), true)
            // キャッシュ再生時
            val thumbPath = nicoVideoCache.getCacheFolderVideoThumFilePath(videoId)
            val thumbBitmap = BitmapFactory.decodeFile(thumbPath)
            createMetadata(
                title = data.title,
                videoId = data.videoId,
                durationSec = (data.duration ?: 0) * 1000,
                bitmap = thumbBitmap
            )
        } else {
            // 通常再生。あとｄe
            createMetadata(
                title = "インターネットは未実装",
                videoId = "インターネットは未実装",
                durationSec = -1,
                bitmap = null
            )
        }
    }

    /**
     * [MediaMetadataCompat]を作成する
     * @param title 動画タイトル
     * @param videoId 動画ID
     * @param bitmap サムネイル
     * @param durationSec 再生時間。秒で
     * */
    private fun createMetadata(title: String, videoId: String, durationSec: Long, bitmap: Bitmap?): MediaMetadataCompat? {
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, title) // ファイル名
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, videoId)
            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationSec * 1000) // これあるとAndroid 10でシーク使えます
        }.build()
        return mediaMetadataCompat
    }

    /**
     * 動画を取得する
     * */
    private fun getVideo() {

    }

}