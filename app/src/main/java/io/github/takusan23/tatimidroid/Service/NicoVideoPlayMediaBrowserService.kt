package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.DisplaySizeTool
import io.github.takusan23.tatimidroid.databinding.OverlayVideoPlayerLayoutBinding
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.timerTask

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
    private val nicoVideoCache by lazy { NicoVideoCache(this) }

    /** 現在再生中の動画のデータクラス */
    private var playingNicoVideoData: NicoVideoData? = null

    /** 再生中の動画のコメント配列 */
    private val commentList = arrayListOf<CommentJSONParse>()

    /** ポップアップ再生のレイアウト */
    private var popupViewBinding: OverlayVideoPlayerLayoutBinding? = null

    /** WindowManager。ポップアップ再生時に利用する */
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    /** ポップアップ再生かどうか */
    private var isPopup = false

    /** ポップアップ再生で使う。シークを動かすためのタイマー */
    private var seekTimer = Timer()

    /** MediaSessionのCustomActionを受け取るためのBroadCast。多分CustomActionをPendingIntentに変換してくれるやつはない？ */
    private lateinit var broadcastReceiver: BroadcastReceiver

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

                /** 曲一覧を　クライアント　からもらう（本当はService側で書きたかったんだけどマイリスト、シリーズ、ランキング、検索結果 etc 無理だわ） */
                override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
                    super.onPrepareFromMediaId(mediaId, extras)
                    // 動画配列を受け取る
                    val list = extras?.getSerializable("video_list") as? ArrayList<NicoVideoData>
                    videoList.clear()
                    if (list != null) {
                        videoList.addAll(list)
                        // 再生準備
                        exoPlayer.playWhenReady = false
                        cachePlay(list[0].videoId)
                    }
                }

                override fun onPrepare() {
                    super.onPrepare()
                    
                }

                /** ｲｸｿﾞｵｵｵｵｵ */
                override fun onPlay() {
                    super.onPlay()
                    exoPlayer.playWhenReady = true
                    isActive = true
                    // サービス起動
                    startThisService()
                }

                /** 一時停止 */
                override fun onPause() {
                    super.onPause()
                    exoPlayer.playWhenReady = false
                }

                /** たーりないものはー つーぎのえきでー さがーそうー */
                override fun onSkipToNext() {
                    super.onSkipToNext()
                    nextVideo()
                }

                /** 前の動画 */
                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    prevVideo()
                }

                /** 指定した位置から再生 */
                override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
                    super.onPlayFromMediaId(mediaId, extras)
                    val videoData = videoList.find { nicoVideoData -> nicoVideoData.videoId == mediaId }
                    if (videoData != null) {
                        if (videoData.isCache) {
                            cachePlay(videoData.videoId)
                            // 再生
                            onPlay()
                        }
                    }
                }

                /** シーク */
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    exoPlayer.seekTo(pos)
                }

                override fun onStop() {
                    super.onStop()
                    isActive = false
                    // サービス終了
                    stopSelf()
                }

                override fun onCustomAction(action: String?, extras: Bundle?) {
                    super.onCustomAction(action, extras)
                }

            })
        }

        // TokenをBrowserServiceへ渡す
        sessionToken = mediaSessionCompat.sessionToken

        // 前回リピートモード有効にしてたか
        val repeatMode = prefSetting.getInt("cache_repeat_mode", 0)
        exoPlayer.repeatMode = repeatMode

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
                // 次の動画へ
                if (state == Player.STATE_ENDED && exoPlayer.playWhenReady) {
                    // 動画おわった。連続再生時なら次の曲へ
                    nextVideo()
                }
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

        // BroadCastReceiver初期化
        initBroadCast()
    }

    /**
     * BroadCastReceiverを初期化する
     * */
    private fun initBroadCast() {
        val intentFilter = IntentFilter().apply {
            addAction("io.github.takusan23.tatimidroid.broadcast.popup")
            addAction("io.github.takusan23.tatimidroid.broadcast.background")
        }
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "io.github.takusan23.tatimidroid.broadcast.popup" -> {
                        setPopup()
                    }
                    "io.github.takusan23.tatimidroid.broadcast.background" -> {
                        closePopup()
                    }
                }
                // 通知更新
                showNotification()
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    /** ポップアップ再生を終了させる */
    private fun closePopup() {
        isPopup = false
        exoPlayer.clearVideoSurfaceView(popupViewBinding?.overlayVideoSurfaceview)
        if (popupViewBinding != null) {
            windowManager.removeView(popupViewBinding?.root)
            popupViewBinding = null
        }
        seekTimer.cancel()
        seekTimer = Timer()
        // 通知更新
        showNotification()
    }

    /** ポップアップ再生を利用する */
    private fun setPopup() {
        // 動画の高さ等が取得できていない場合は落とす
        exoPlayer.videoFormat?.width ?: return

        // 権限なければ落とす
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(this)
            } else {
                false
            }
        ) {
            showToast("権限がありませんでした。「他のアプリの上に重ねて表示」権限をください。")
            return
        }

        // 画面の半分を利用するように
        val width = DisplaySizeTool.getDisplayWidth(this) / 2
        val height = nicoVideoHTML.calcVideoHeightDisplaySize(exoPlayer.videoFormat!!.width, exoPlayer.videoFormat!!.height, width)
        // レイアウト読み込み
        popupViewBinding = OverlayVideoPlayerLayoutBinding.inflate(LayoutInflater.from(this))
        val popupLayoutParams = getParams(width, height.toInt())
        popupViewBinding?.apply {
            // オーバーレイ表示
            windowManager.addView(root, popupLayoutParams)
            overlayVideoCommentCanvas.isPopupView = true

            isPopup = true
            // 通知更新
            showNotification()

            // SurfaceViewセット
            exoPlayer.setVideoSurfaceView(overlayVideoSurfaceview)

            // 使わないボタンを消す
            overlayVideoControlInclude.apply {
                playerControlPopup.isVisible = false
                playerControlBackground.isVisible = false
            }
            // 番組名、ID設定
            overlayVideoControlInclude.apply {
                playerControlTitle.text = playingNicoVideoData?.title
                playerControlId.text = playingNicoVideoData?.videoId
            }

            // 閉じる
            overlayVideoControlInclude.playerControlClose.isVisible = true
            overlayVideoControlInclude.playerControlClose.setOnClickListener {
                closePopup()
            }

            // リピートするか
            if (prefSetting.getBoolean("nicovideo_repeat_on", true)) {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                overlayVideoControlInclude.playerControlRepeat.setImageDrawable(getDrawable(R.drawable.ic_repeat_one_24px))
            }

            // ミュート・ミュート解除
            overlayVideoControlInclude.playerControlMute.isVisible = true
            overlayVideoControlInclude.playerControlMute.setOnClickListener {
                exoPlayer.apply {
                    //音が０のとき
                    if (volume == 0f) {
                        volume = 1f
                        overlayVideoControlInclude.playerControlMute.setImageDrawable(getDrawable(R.drawable.ic_volume_up_24px))
                    } else {
                        volume = 0f
                        overlayVideoControlInclude.playerControlMute.setImageDrawable(getDrawable(R.drawable.ic_volume_off_24px))
                    }
                }
            }

            // UI表示
            var job: Job? = null
            root.setOnClickListener {
                overlayVideoControlInclude.playerControlMain.isVisible = !overlayVideoControlInclude.playerControlMain.isVisible
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.Main) {
                    delay(3000)
                    overlayVideoControlInclude.playerControlMain.isVisible = false
                }
            }

            // ピンチイン、ピンチアウトでズームできるようにする
            val scaleGestureDetector = ScaleGestureDetector(this@NicoVideoPlayMediaBrowserService, object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean {
                    return true
                }

                override fun onScaleEnd(p0: ScaleGestureDetector?) {

                }

                override fun onScale(p0: ScaleGestureDetector?): Boolean {
                    // ピンチイン/アウト中。
                    if (p0 == null) return true
                    // なんかうまくいくコード
                    popupLayoutParams.width = (popupLayoutParams.width * p0.scaleFactor).toInt()
                    // 縦の大きさは計算で出す（widthの時と同じようにやるとアスペクト比が崩れる。）
                    if (exoPlayer.videoFormat != null) {
                        val height = nicoVideoHTML.calcVideoHeightDisplaySize(exoPlayer.videoFormat!!.width, exoPlayer.videoFormat!!.height, popupLayoutParams.width)
                        popupLayoutParams.height = height.toInt()
                        // 更新
                        windowManager.updateViewLayout(root, popupLayoutParams)
                        // 大きさを保持しておく
                        prefSetting.edit {
                            putInt("nicovideo_popup_height", popupLayoutParams.height)
                            putInt("nicovideo_popup_width", popupLayoutParams.width)
                        }
                    }
                    return true
                }
            })

            // 移動
            root.setOnTouchListener { view, motionEvent ->
                // タップした位置を取得する
                val x = motionEvent.rawX.toInt()
                val y = motionEvent.rawY.toInt()
                // ついに直感的なズームが！？
                scaleGestureDetector.onTouchEvent(motionEvent)
                // 移動できるように
                when (motionEvent.action) {
                    // Viewを移動させてるときに呼ばれる
                    MotionEvent.ACTION_MOVE -> {
                        // 中心からの座標を計算する
                        val centerX = x - (DisplaySizeTool.getDisplayWidth(this@NicoVideoPlayMediaBrowserService) / 2)
                        val centerY = y - (DisplaySizeTool.getDisplayHeight(this@NicoVideoPlayMediaBrowserService) / 2)

                        // オーバーレイ表示領域の座標を移動させる
                        popupLayoutParams.x = centerX
                        popupLayoutParams.y = centerY

                        // 移動した分を更新する
                        windowManager.updateViewLayout(view, popupLayoutParams)

                        // サイズを保存しておく
                        prefSetting.edit {
                            putInt("nicovideo_popup_x_pos", popupLayoutParams.x)
                            putInt("nicovideo_popup_y_pos", popupLayoutParams.y)
                        }
                        return@setOnTouchListener true // setOnclickListenerが呼ばれてしまうため true 入れる
                    }
                }
                return@setOnTouchListener false
            }

            //アプリ起動
            overlayVideoControlInclude.playerControlFullscreen.setOnClickListener {
                stopSelf()
                // アプリ起動
                val intent = Intent(this@NicoVideoPlayMediaBrowserService, MainActivity::class.java)
                intent.putExtra("id", playingNicoVideoData?.videoId)
                intent.putExtra("cache", playingNicoVideoData?.isCache)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

            // 連続再生とアイコン変える
            val nextIcon = if (isPlaylistMode()) getDrawable(R.drawable.ic_skip_next_black_24dp) else getDrawable(R.drawable.ic_redo_black_24dp)
            val prevIcon = if (isPlaylistMode()) getDrawable(R.drawable.ic_skip_previous_black_24dp) else getDrawable(R.drawable.ic_undo_black_24dp)
            overlayVideoControlInclude.playerControlNext.setImageDrawable(nextIcon)
            overlayVideoControlInclude.playerControlPrev.setImageDrawable(prevIcon)

            // スキップ秒数
            val skipValueMs = (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000
            // コントローラー
            overlayVideoControlInclude.playerControlNext.setOnClickListener {
                if (isPlaylistMode()) {
                    // 次の動画
                    nextVideo()
                } else {
                    // 進める
                    exoPlayer.seekTo(exoPlayer.currentPosition + skipValueMs)
                }
            }
            overlayVideoControlInclude.playerControlPrev.setOnClickListener {
                if (isPlaylistMode()) {
                    // 前の動画
                    prevVideo()
                } else {
                    // 戻す
                    exoPlayer.seekTo(exoPlayer.currentPosition - skipValueMs)
                }
            }

            overlayVideoControlInclude.playerControlPause.setOnClickListener {
                // 一時停止
                exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                // コメント止める
                overlayVideoCommentCanvas.isPause = !exoPlayer.playWhenReady
                // アイコン変更
                overlayVideoControlInclude.playerControlPause.setImageDrawable(if (exoPlayer.playWhenReady) getDrawable(R.drawable.ic_pause_black_24dp) else getDrawable(R.drawable.ic_play_arrow_24px))
            }

            // リピート再生
            overlayVideoControlInclude.playerControlRepeat.setOnClickListener {
                when (exoPlayer.repeatMode) {
                    Player.REPEAT_MODE_OFF -> {
                        // リピート無効時
                        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                        overlayVideoControlInclude.playerControlRepeat.setImageDrawable(getDrawable(R.drawable.ic_repeat_one_24px))
                        prefSetting.edit { putBoolean("nicovideo_repeat_on", true) }
                    }
                    Player.REPEAT_MODE_ONE -> {
                        // リピート有効時
                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                        overlayVideoControlInclude.playerControlRepeat.setImageDrawable(getDrawable(R.drawable.ic_repeat_black_24dp))
                        prefSetting.edit { putBoolean("nicovideo_repeat_on", false) }
                    }
                }
            }

            // シークバー操作中かどうか
            var isTouchingSeekBar = false
            overlayVideoControlInclude.playerControlSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isTouchingSeekBar = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    // 動画シークする
                    exoPlayer.seekTo((seekBar?.progress ?: 0) * 1000L)
                    isTouchingSeekBar = false
                }
            })

            // コメント描画改善。drawComment()関数でのみ使う（0秒に投稿されたコメントが重複して表示される対策）
            val drewedList = arrayListOf<String>() // 描画したコメントのNoが入る配列。一秒ごとにクリアされる
            var tmpPosition = 0L // いま再生している位置から一秒引いた値が入ってる。

            // シーク用に毎秒動くタイマー
            seekTimer.cancel()
            seekTimer = Timer()
            seekTimer.schedule(timerTask {
                if (exoPlayer.isPlaying) {
                    Handler(Looper.getMainLooper()).post {
                        if (!isTouchingSeekBar) {
                            overlayVideoControlInclude.playerControlSeek.progress = (exoPlayer.currentPosition / 1000L).toInt()
                            overlayVideoControlInclude.playerControlSeek.max = (exoPlayer.duration / 1000L).toInt()
                            val formattedTime = DateUtils.formatElapsedTime(exoPlayer.currentPosition / 1000L)
                            val durationText = DateUtils.formatElapsedTime(exoPlayer.duration / 1000L)
                            overlayVideoControlInclude.playerControlCurrent.text = formattedTime
                            overlayVideoControlInclude.playerControlDuration.text = durationText
                        }
                    }

                    val commentCanvas = popupViewBinding?.overlayVideoCommentCanvas ?: return@timerTask
                    val currentPosition = exoPlayer.contentPosition / 100L
                    if (tmpPosition != exoPlayer.contentPosition / 1000) {
                        drewedList.clear()
                        tmpPosition = currentPosition
                    }
                    GlobalScope.launch {
                        val drawList = commentList.filter { commentJSONParse ->
                            (commentJSONParse.vpos.toLong() / 10L) == (currentPosition)
                        }
                        drawList.forEach {
                            // 追加可能か（livedl等TSのコメントはコメントIDが無い？のでvposで代替する）
                            val isAddable = drewedList.none { id -> it.commentNo == id || it.vpos == id } // 条件に合わなければtrue
                            if (isAddable) {
                                val commentNo = if (it.commentNo == "-1" || it.commentNo.isEmpty()) {
                                    // vposで代替
                                    it.vpos
                                } else {
                                    it.commentNo
                                }
                                drewedList.add(commentNo)
                                if (!it.comment.contains("\n")) {
                                    // SingleLine
                                    commentCanvas!!.post {
                                        commentCanvas!!.postComment(it.comment, it)
                                    }
                                } else {
                                    // 複数行？
                                    val asciiArtComment = if (it.mail.contains("shita")) {
                                        it.comment.split("\n").reversed() // 下コメントだけ逆順にする
                                    } else {
                                        it.comment.split("\n")
                                    }
                                    for (line in asciiArtComment) {
                                        commentCanvas!!.post {
                                            commentCanvas!!.postComment(line, it, true)
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }, 100, 100)
        }
    }

    private fun getParams(videoWidth: Int, videoHeight: Int): WindowManager.LayoutParams {
        // オーバーレイViewの設定をする
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                videoWidth,
                videoHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                videoWidth,
                videoHeight,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        }
        return params
    }


    /** 単発モードか連続再生かどうか */
    private fun isPlaylistMode() = videoList.size != 1

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
                        if (isPopup) {
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
        // mediaSessionCompat.controller.transportControls?.play()
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
            val notificationChannel = NotificationChannel(notificationChannelId, getString(R.string.video_popup_background_play_service), NotificationManager.IMPORTANCE_LOW)
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            } else {
                // 優先度を下げる
                val notificationChannel = notificationManager.getNotificationChannel(notificationChannelId).apply {
                    importance = NotificationManager.IMPORTANCE_LOW
                }
                // 再登録
                notificationManager.createNotificationChannel(notificationChannel)
            }
            NotificationCompat.Builder(this, notificationChannelId)
        } else {
            NotificationCompat.Builder(this)
        }
        notification.apply {
            // 音楽Style+アイコン指定
            setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken).setShowActionsInCompactView(1, 2, 3))
            setSmallIcon(if (isPopup) R.drawable.ic_popup_icon_black else R.drawable.ic_tatimidroid_playlist_play_black)
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
            if (isPopup) {
                val backgroundPlayIntent = Intent("io.github.takusan23.tatimidroid.broadcast.background")
                val backgroundPendingIntent = PendingIntent.getBroadcast(this@NicoVideoPlayMediaBrowserService, 114, backgroundPlayIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                addAction(R.drawable.ic_tatimidroid_playlist_play_black, "切り替え", backgroundPendingIntent)
            } else {
                val popupPlayIntent = Intent("io.github.takusan23.tatimidroid.broadcast.popup")
                val popupPendingIntent = PendingIntent.getBroadcast(this@NicoVideoPlayMediaBrowserService, 514, popupPlayIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                addAction(R.drawable.ic_popup_icon_black, "切り替え", popupPendingIntent)
            }
        }
        startForeground(4649, notification.build())
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
                videoId = data.uploaderName ?: data.videoId,
                durationSec = (data.duration ?: 0),
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        exoPlayer.release()
        closePopup()
    }

}