package io.github.takusan23.tatimidroid.Service

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import io.github.takusan23.tatimidroid.CommentCanvas
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.isLoginMode
import kotlinx.android.synthetic.main.overlay_video_player_layout.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * ニコ生をポップアップ、バックグラウンドで再生するやつ。
 * FragmentからServiceに移動させる
 *
 * Intentに詰めてほしいもの↓
 * mode           |String |"popup"（ポップアップ再生）か"background"(バッググラウンド再生)のどっちか。
 * video_id       |String |動画ID
 * is_cache       |Boolean|キャッシュ再生ならtrue
 * オプション
 * seek           |Long   |シークする場合は値（ms）を入れてね。
 *
 * */
class NicoVideoPlayService : Service() {

    // 通知ID
    private val NOTIFICAION_ID = 865

    private lateinit var prefSetting: SharedPreferences
    private lateinit var notificationManager: NotificationManager
    private lateinit var windowManager: WindowManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var mediaSessionCompat: MediaSessionCompat

    // View
    private lateinit var popupView: View
    private lateinit var exoPlayer: SimpleExoPlayer
    private lateinit var commentCanvas: CommentCanvas

    // 動画視聴からコメント取得、ハートビート処理まで
    val nicoVideoHTML = NicoVideoHTML()

    // 視聴モード
    var playMode = "popup"

    // 再生に使うやつ
    var isCache = false
    var contentUrl = ""
    var nicoHistory = ""
    lateinit var jsonObject: JSONObject
    var seekTimer = Timer()
    var isTouchingSeekBar = false // サイズ変更シークバー操作中ならtrue

    // session_apiのレスポンス
    lateinit var sessionAPIJSONObject: JSONObject

    // 動画情報とか
    var userSession = ""
    var videoId = ""
    var videoTitle = ""
    var seekMs = 0L
    var commentList = arrayListOf<CommentJSONParse>() // コメント配列

    // アスペクト比（横 / 縦）。なんか21:9並のほっそ長い動画があるっぽい？
    // 4:3 = 1.3 / 16:9 = 1.7
    var aspect = 1.7
    lateinit var popupLayoutParams: WindowManager.LayoutParams

    // 再生時間を適用したらtrue。一度だけ動くように
    var isRotationProgressSuccessful = false

    // キャッシュ取得用
    lateinit var nicoVideoCache: NicoVideoCache

    // コメント描画改善。drawComment()関数でのみ使う（0秒に投稿されたコメントが重複して表示される対策）
    private var drewedList = arrayListOf<String>() // 描画したコメントのNoが入る配列。一秒ごとにクリアされる
    private var tmpPosition = 0L // いま再生している位置から一秒引いた値が入ってる。

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)
        userSession = prefSetting.getString("user_session", "") ?: ""
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        showNotification(getString(R.string.loading))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 受け取り
        playMode = intent?.getStringExtra("mode") ?: "popup"
        videoId = intent?.getStringExtra("video_id") ?: ""
        isCache = intent?.getBooleanExtra("is_cache", false) ?: false
        seekMs = intent?.getLongExtra("seek", 0L) ?: 0L
        nicoVideoCache = NicoVideoCache(this)
        if (isCache) {
            // キャッシュ再生
            cachePlay()
        } else {
            // データ取得など
            coroutine()
        }
        initBroadcast()
        return START_NOT_STICKY
    }

    // データ取得など
    private fun coroutine() {
        GlobalScope.launch {
            // ログインしないならそもそもuserSessionの値を空にすれば！？
            val userSession = if (isLoginMode(this@NicoVideoPlayService)) {
                this@NicoVideoPlayService.userSession
            } else {
                ""
            }
            val response = nicoVideoHTML.getHTML(videoId, userSession, "").await()
            nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            val responseString = response.body?.string()
            jsonObject = nicoVideoHTML.parseJSON(responseString)
            // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
            if (nicoVideoHTML.isDMCServer(jsonObject)) {
                // 公式アニメは暗号化されてて見れないので落とす
                if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                    showToast(getString(R.string.encryption_video_not_play))
                    Handler(Looper.getMainLooper()).post {
                        this@NicoVideoPlayService.stopSelf()
                    }
                    return@launch
                }
                // https://api.dmc.nico/api/sessions のレスポンス
                val sessionAPIResponse = nicoVideoHTML.callSessionAPI(jsonObject).await()
                if (sessionAPIResponse != null) {
                    sessionAPIJSONObject = sessionAPIResponse
                    // 動画URL
                    contentUrl = nicoVideoHTML.getContentURI(jsonObject, sessionAPIJSONObject)
                    // ハートビート処理。これしないと切られる。
                    nicoVideoHTML.heartBeat(jsonObject, sessionAPIJSONObject)
                }
            } else {
                // Smileサーバー。動画URL取得。自動or低画質は最初の視聴ページHTMLのURLのうしろに「?eco=1」をつければ低画質が送られてくる
                contentUrl = nicoVideoHTML.getContentURI(jsonObject, null)
            }
            // コメント取得
            val commentJSON = nicoVideoHTML.getComment(videoId, userSession, jsonObject).await()
            if (commentJSON != null) {
                commentList =
                    ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.body?.string()!!, videoId))
            }
            // タイトル
            videoTitle = jsonObject.getJSONObject("video").getString("title")
            Handler(Looper.getMainLooper()).post {
                // ExoPlayer
                initVideoPlayer(contentUrl, nicoHistory)
            }
        }
    }

    // キャッシュ再生
    private fun cachePlay() {
        // コメントファイルがxmlならActivity終了
        val xmlCommentJSON = XMLCommentJSON(this)
        if (xmlCommentJSON.commentXmlFilePath(videoId) != null && !xmlCommentJSON.commentJSONFileExists(videoId)) {
            // xml形式はあるけどjson形式がないときは落とす
            Toast.makeText(this, R.string.xml_comment_play, Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        } else {
            // タイトル
            videoTitle = if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId)).getJSONObject("video")
                    .getString("title")
            } else {
                // 動画ファイルの名前
                nicoVideoCache.getCacheFolderVideoFileName(videoId) ?: videoId
            }
            GlobalScope.launch {
                // 動画のファイル名取得
                val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                if (videoFileName != null) {
                    contentUrl =
                        "${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName"
                    Handler(Looper.getMainLooper()).post {
                        // ExoPlayer
                        initVideoPlayer(contentUrl, "")
                    }
                    // コメント取得
                    val commentJSON = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                    commentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON, videoId))
                } else {
                    // 動画が見つからなかった
                    showToast(getString(R.string.not_found_video))
                    stopSelf()
                    return@launch
                }
            }
        }
    }

    // ExoPlayer初期化
    private fun initVideoPlayer(contentUrl: String, nicoHistory: String) {
        exoPlayer = SimpleExoPlayer.Builder(this).build()
        // キャッシュ再生と分ける
        if (isCache) {
            // キャッシュ再生
            val dataSourceFactory =
                DefaultDataSourceFactory(this, "TatimiDroid;@takusan_23")
            val videoSource =
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(contentUrl.toUri())
            exoPlayer.prepare(videoSource)
        } else {
            // SmileサーバーはCookieつけないと見れないため
            val dataSourceFactory =
                DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
            dataSourceFactory.defaultRequestProperties.set("Cookie", nicoHistory)
            val videoSource =
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(contentUrl.toUri())
            exoPlayer.prepare(videoSource)
        }
        // 自動再生
        exoPlayer.playWhenReady = true
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                if (!isRotationProgressSuccessful) {
                    // 一度だけ実行するように。画面回転時に再生時間を引き継ぐ
                    exoPlayer.seekTo(seekMs)
                    isRotationProgressSuccessful = true
                }
                if (isPopupPlay()) {
                    // 初期化してなければ落とす
                    if (!::popupView.isInitialized) {
                        return
                    }
                    // シークの最大値設定。
                    popupView.overlay_video_video_seek_bar.max =
                        (exoPlayer.duration / 1000L).toInt()
                    // 動画のシーク
                    popupView.overlay_video_video_seek_bar.setOnSeekBarChangeListener(object :
                        SeekBar.OnSeekBarChangeListener {
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
                    // 動画のアイコン入れ替え
                    val drawable = if (exoPlayer.playWhenReady) {
                        getDrawable(R.drawable.ic_pause_black_24dp)
                    } else {
                        getDrawable(R.drawable.ic_play_arrow_24px)
                    }
                    popupView.overlay_video_pause_button.setImageDrawable(drawable)
                }
            }
        })

        // Displayの大きさ
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val disp = wm.getDefaultDisplay()
        val realSize = Point()
        disp.getRealSize(realSize)

        // アスペクトひ
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                // アスペクト比が4:3か16:9か
                // 4:3 = 1.333... 16:9 = 1.777..
                val calc = width.toFloat() / height.toFloat()
                // 小数点第二位を捨てる
                aspect = BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                popupLayoutParams = getParams(realSize.x / 2)
                windowManager.updateViewLayout(popupView, popupLayoutParams)
                // 設定読み込む
                // サイズ適用
                popupView.overlay_video_size_seekbar.progress =
                    prefSetting.getInt("nicovideo_popup_size_progress", 0)
                // CommentCanvasに反映
                applyCommentCanvas()
                // 位置が保存されていれば適用
                if (prefSetting.getInt("nicovideo_popup_x_pos", 0) != 0) {
                    popupLayoutParams.x = prefSetting.getInt("nicovideo_popup_x_pos", 0)
                    popupLayoutParams.y = prefSetting.getInt("nicovideo_popup_y_pos", 0)
                    windowManager.updateViewLayout(popupView, popupLayoutParams)
                }
            }
        })

        // MediaSession。通知もう一階出せばなんか表示されるようになった。Androidむずかちい
        showNotification(videoTitle)
        initMediaSession()

        // ポップアップ再生ならView用意
        if (isPopupPlay()) {
            // 権限なければ落とす
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    !Settings.canDrawOverlays(this)
                } else {
                    false
                }
            ) {
                return
            }
            // 画面の半分を利用するように
            val width = realSize.x / 2

            // レイアウト読み込み
            val layoutInflater = LayoutInflater.from(this)
            popupLayoutParams = getParams(width)
            popupView = layoutInflater.inflate(R.layout.overlay_video_player_layout, null)
            // 表示
            windowManager.addView(popupView, popupLayoutParams)
            commentCanvas = popupView.overlay_video_commentCanvas
            commentCanvas.isPopupView = true
            // SurfaceViewセット
            exoPlayer.setVideoSurfaceView(popupView.overlay_video_surfaceview)

            // 閉じる
            popupView.overlay_video_close_button.setOnClickListener {
                stopSelf()
            }

            // リピートするか
            if (prefSetting.getBoolean("nicovideo_repeat_on", true)) {
                exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                popupView.overlay_video_repeat_button.setImageDrawable(getDrawable(R.drawable.ic_repeat_one_24px))
            }

            // ミュート・ミュート解除
            popupView.overlay_video_sound_button.setOnClickListener {
                if (::exoPlayer.isInitialized) {
                    exoPlayer.apply {
                        //音が０のとき
                        if (volume == 0f) {
                            volume = 1f
                            popupView.overlay_video_sound_button.setImageDrawable(getDrawable(R.drawable.ic_volume_up_24px))
                        } else {
                            volume = 0f
                            popupView.overlay_video_sound_button.setImageDrawable(getDrawable(R.drawable.ic_volume_off_24px))
                        }
                    }
                }
            }

            // ボタン表示
            popupView.setOnClickListener {
                if (popupView.overlay_video_button_layout.visibility == View.GONE) {
                    //表示
                    popupView.overlay_video_button_layout.visibility = View.VISIBLE
                    // コントローラー表示可能なら表示する
                    showVideoController()
                } else {
                    //非表示
                    popupView.overlay_video_button_layout.visibility = View.GONE
                    popupView.overlay_video_controller_layout.visibility = View.GONE
                }
            }

            // 画面サイズ
            var displaySize = getDisplaySize()

            // 移動
            popupView.setOnTouchListener { view, motionEvent ->
                // タップした位置を取得する
                val x = motionEvent.rawX.toInt()
                val y = motionEvent.rawY.toInt()
                // 画面回転に対応する（これで横/縦画面のときの最大値とか変えられる）
                displaySize = getDisplaySize()
                when (motionEvent.action) {
                    // Viewを移動させてるときに呼ばれる
                    MotionEvent.ACTION_MOVE -> {
                        // 中心からの座標を計算する
                        val centerX = x - (displaySize.x / 2)
                        val centerY = y - (displaySize.y / 2)

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
                    }
                }
                false
            }

            // 大きさ変更。まず変更前を入れておく
            var normalHeight = -1
            var normalWidth = -1
            popupView.overlay_video_size_seekbar.apply {
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        displaySize = getDisplaySize()
                        // 初期値。動画再生時までアスペ比が取得できないのでサイズ変更を一番最初に行うときに最小サイズを取得。
                        if (normalHeight == -1) {
                            normalHeight = popupLayoutParams.height
                            normalWidth = popupLayoutParams.width
                        }
                        // 大きさ変更シークの最大値設定。なんかこの式で期待通り動く。なんでか知らないけど動く。:thinking_face:
                        popupView.overlay_video_size_seekbar.max = when (aspect) {
                            1.3 -> (displaySize.x / 4) / 2
                            1.7 -> (displaySize.x / 16) / 2
                            else -> (displaySize.x / 16) / 2
                        }
                        // 操作中
                        when (aspect) {
                            1.3 -> {
                                popupLayoutParams.height = normalHeight + (progress + 1) * 3
                                popupLayoutParams.width = normalWidth + (progress + 1) * 4
                            }
                            1.7 -> {
                                popupLayoutParams.height = normalHeight + (progress + 1) * 9
                                popupLayoutParams.width = normalWidth + (progress + 1) * 16
                            }
                        }
                        windowManager.updateViewLayout(popupView, popupLayoutParams)
                        // CommentCanvasに反映
                        applyCommentCanvas()
                        // 位置保存。サイズ変更シーク位置を保存
                        prefSetting.edit {
                            putInt("nicovideo_popup_size_progress", popupView.overlay_video_size_seekbar.progress)
                        }
                        // コントローラー表示可能かどうか
                        showVideoController()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {

                    }
                })
            }

            //アプリ起動
            popupView.overlay_video_activity_launch.setOnClickListener {
                stopSelf()
                // アプリ起動
                val intent = Intent(this, NicoVideoActivity::class.java)
                intent.putExtra("id", videoId)
                intent.putExtra("cache", isCache)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

            // コントローラー
            popupView.overlay_video_forward_button.setOnClickListener {
                // 進める
                exoPlayer.seekTo(exoPlayer.currentPosition + 5000L)
            }
            popupView.overlay_video_replay_button.setOnClickListener {
                // 戻す
                exoPlayer.seekTo(exoPlayer.currentPosition - 5000L)
            }
            popupView.overlay_video_pause_button.setOnClickListener {
                // 一時停止
                exoPlayer.playWhenReady = !exoPlayer.playWhenReady
                // コメント止める
                commentCanvas.isPause = !exoPlayer.playWhenReady
            }

            // リピート再生
            popupView.overlay_video_repeat_button.setOnClickListener {
                when (exoPlayer.repeatMode) {
                    Player.REPEAT_MODE_OFF -> {
                        // リピート無効時
                        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                        popupView.overlay_video_repeat_button.setImageDrawable(getDrawable(R.drawable.ic_repeat_one_24px))
                        prefSetting.edit { putBoolean("nicovideo_repeat_on", true) }
                    }
                    Player.REPEAT_MODE_ONE -> {
                        // リピート有効時
                        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                        popupView.overlay_video_repeat_button.setImageDrawable(getDrawable(R.drawable.ic_repeat_black_24dp))
                        prefSetting.edit { putBoolean("nicovideo_repeat_on", false) }
                    }
                }
            }

            // シーク用に毎秒動くタイマー
            seekTimer.schedule(timerTask {
                if (exoPlayer.isPlaying) {
                    setProgress()
                    drawComment()
                }
            }, 100, 100)

        }
    }

    // コントローラーが表示できるなら表示する関数
    private fun showVideoController() {

        val buttonHeight = popupView.overlay_video_button_layout_button.height
        val sizeSeekHeight = popupView.overlay_video_button_layout_size.height
        val totalHeight = buttonHeight + sizeSeekHeight
        if (totalHeight == 0) {
            // getHeight()が正しい値を返さないときは落とす
            return
        }
        val space = popupLayoutParams.height - totalHeight

        val controllerHeight =
            popupView.overlay_video_controller_layout.height

        // 表示可能でなおボタン類が表示状態なら表示
        if (space > controllerHeight && popupView.overlay_video_button_layout.visibility == View.VISIBLE) {
            popupView.overlay_video_controller_layout.visibility =
                View.VISIBLE
        } else {
            popupView.overlay_video_controller_layout.visibility = View.GONE
        }
    }

    // サイズ変更をCommentCanvasに反映させる
    private fun applyCommentCanvas() {
        commentCanvas.viewTreeObserver.addOnGlobalLayoutListener {
            commentCanvas.apply {
                finalHeight = commentCanvas.height
                // コメントの高さの情報がある配列を消す。
                // これ消さないとサイズ変更時にコメント描画で見切れる文字が発生する。
                commentLine.clear()
                ueCommentLine.clear()
                sitaCommentLine.clear()
            }
        }
    }

    // コメント描画あああ
    private fun drawComment() {
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
                if (!drewedList.contains(it.commentNo)) {
                    drewedList.add(it.commentNo)
                    if (!it.comment.contains("\n")) {
                        // SingleLine
                        commentCanvas.post {
                            commentCanvas.postComment(it.comment, it)
                        }
                    } else {
                        // 複数行？
                        val asciiArtComment = if (it.mail.contains("shita")) {
                            it.comment.split("\n").reversed() // 下コメントだけ逆順にする
                        } else {
                            it.comment.split("\n")
                        }
                        for (line in asciiArtComment) {
                            commentCanvas.post {
                                commentCanvas.postComment(line, it, true)
                            }
                        }
                    }
                }

            }
        }
    }

    // シークを動画再生時間に合わせる
    private fun setProgress() {
        if (!isTouchingSeekBar) {
            popupView.overlay_video_video_seek_bar.progress =
                (exoPlayer.currentPosition / 1000L).toInt()
        }
    }

    private fun getParams(width: Int): WindowManager.LayoutParams {
        //アスペクト比16:9なので
        val height = when (aspect) {
            1.3 -> (width / 4) * 3 // 4:3
            1.7 -> (width / 16) * 9 // 16:9
            else -> (width / 16) * 9
        }
        // オーバーレイViewの設定をする
        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                width,
                height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                width,
                height,
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

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 画面サイズのPoint返す関数。
    private fun getDisplaySize(): Point {
        val display = windowManager.defaultDisplay
        val displaySize = Point()
        display.getSize(displaySize)
        return displaySize
    }

    /**
     * ポップアップ再生かどうかを返す
     * @return ポップアップ再生ならtrue
     * */
    private fun isPopupPlay(): Boolean {
        return playMode == "popup"
    }

    /** MediaSession。音楽アプリの再生中のあれ */
    private fun initMediaSession() {
        val mode = if (isPopupPlay()) {
            getString(R.string.popup_video_player)
        } else {
            getString(R.string.background_video_player)
        }
        mediaSessionCompat = MediaSessionCompat(this, "nicovideo")
        // メタデータ
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "$videoTitle / $videoId")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, videoId)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, videoTitle)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, videoTitle)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mode)
        }.build()
        mediaSessionCompat.apply {
            setMetadata(mediaMetadataCompat) // メタデータ入れる
            isActive = true // これつけないとAlways On Displayで表示されない
            // 常に再生状態にしておく。これでAODで表示できる
            setPlaybackState(
                PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0L, 1F)
                    .build()
            )
        }
    }

    /**
     * サービス実行中通知を送る関数。
     * まずこれを呼んでServiceを終了させないようにしないといけない。
     * */
    private fun showNotification(message: String) {
        // 停止Broadcast送信
        val stopPopupIntent = Intent("video_popup_close")
        // 通知のタイトル設定
        val title = if (isPopupPlay()) {
            getString(R.string.popup_video_player)
        } else {
            getString(R.string.background_video_player)
        }
        val icon = if (isPopupPlay()) {
            R.drawable.ic_popup_icon
        } else {
            R.drawable.ic_background_icon
        }
        // 通知作成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "video_popup"
            val notificationChannel =
                NotificationChannel(notificationChannelId, getString(R.string.video_popup_background_play_service), NotificationManager.IMPORTANCE_HIGH)
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val programNotification =
                NotificationCompat.Builder(this, notificationChannelId).apply {
                    setContentTitle(title)
                    setContentText(message)
                    setSmallIcon(icon)
                    addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@NicoVideoPlayService, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                }.build()
            startForeground(NOTIFICAION_ID, programNotification)
        } else {
            val programNotification = NotificationCompat.Builder(this).apply {
                setContentTitle(title)
                setContentText(message)
                setSmallIcon(icon)
                addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@NicoVideoPlayService, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }.build()
            startForeground(NOTIFICAION_ID, programNotification)
        }
    }

    /**
     * ブロードキャスト初期化。
     * Direct Replyの返信を受け取ったりするため
     * */
    private fun initBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("video_popup_close")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "video_popup_close" -> {
                        // 終了
                        this@NicoVideoPlayService.stopSelf()
                    }
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        if (::mediaSessionCompat.isInitialized) {
            mediaSessionCompat.apply {
                isActive = false
                setPlaybackState(
                    PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 0L, 1F)
                        .build()
                )
                release()
            }
        }
        nicoVideoHTML.destory()
        nicoVideoCache.destroy()
        exoPlayer.release()
        if (::popupView.isInitialized) {
            windowManager.removeView(popupView)
        }
        seekTimer.cancel()
    }
}

/**
 * ポップアップ再生、バッググラウンド再生サービス起動用関数。internal fun なのでどっからでも呼べると思う？
 * @param mode "popup"（ポップアップ再生）か"background"（バッググラウンド再生）
 * @param context Context
 * @param videoId 動画ID
 * @param isCache キャッシュ再生ならtrue
 * @param seek シークするなら値を入れてね。省略可能。
 * */
internal fun startVideoPlayService(context: Context?, mode: String, videoId: String, isCache: Boolean, seek: Long = 0L) {
    // ポップアップ再生の権限あるか
    if (mode == "popup") {
        if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !Settings.canDrawOverlays(context)
            } else {
                false
            }
        ) {
            // 権限取得画面出す
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context?.packageName}"))
            context?.startActivity(intent)
            return
        }
    }
    val intent = Intent(context, NicoVideoPlayService::class.java).apply {
        putExtra("mode", mode)
        putExtra("video_id", videoId)
        putExtra("is_cache", isCache)
        putExtra("seek", seek)
    }
    // サービス終了（起動させてないときは何もならないと思う）させてから起動させる。（
    // 起動してない状態でstopService呼ぶ分にはなんの問題もないっぽい？）
    context?.stopService(intent)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context?.startForegroundService(intent)
    } else {
        context?.startService(intent)
    }
}
