package io.github.takusan23.tatimidroid.Service

import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.view.*
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.CommentCanvas
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.overlay_player_layout.view.*
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

    // session_apiのレスポンス
    lateinit var sessionAPIJSONObject: JSONObject

    // 動画情報とか
    var userSession = ""
    var videoId = ""
    var videoTitle = ""
    var seekMs = 0L
    var commentList = arrayListOf<ArrayList<String>>() // コメント配列

    // アスペクト比（横 / 縦）。なんか21:9並のほっそ長い動画があるっぽい？
    // 4:3 = 1.3 / 16:9 = 1.7
    var aspect = 1.7
    lateinit var popupLayoutParams: WindowManager.LayoutParams

    // 再生時間を適用したらtrue。一度だけ動くように
    var isRotationProgressSuccessful = false

    // キャッシュ取得用
    lateinit var nicoVideoCache: NicoVideoCache

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
                    ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.body?.string()!!))
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
            // 動画のファイル名取得
            val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
            if (videoFileName != null) {
                contentUrl =
                    "${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName"
                // ExoPlayer
                initVideoPlayer(contentUrl, "")
                // コメント取得
                val commentJSON = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                commentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON))
            } else {
                // 動画が見つからなかった
                Toast.makeText(this, R.string.not_found_video, Toast.LENGTH_SHORT).show()
                stopSelf()
                return
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

                        }

                        override fun onStopTrackingTouch(seekBar: SeekBar?) {
                            // 動画シークする
                            exoPlayer.seekTo((seekBar?.progress ?: 0) * 1000L)
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

        // アスペクトひ
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                // アスペクト比が4:3か16:9か
                // 4:3 = 1.333... 16:9 = 1.777..
                val calc = width.toFloat() / height.toFloat()
                // 小数点第二位を捨てる
                aspect = BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                // PopupViewのLayoutParams再計算
                val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val disp = wm.getDefaultDisplay()
                val realSize = Point()
                disp.getRealSize(realSize)
                popupLayoutParams = getParams(realSize.x / 2)
                windowManager.updateViewLayout(popupView, popupLayoutParams)
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
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val disp = wm.getDefaultDisplay()
            val realSize = Point();
            disp.getRealSize(realSize);
            val width = realSize.x / 2

            //レイアウト読み込み
            val layoutInflater = LayoutInflater.from(this)
            popupLayoutParams = getParams(width)
            popupView = layoutInflater.inflate(R.layout.overlay_video_player_layout, null)
            //表示
            windowManager.addView(popupView, popupLayoutParams)
            commentCanvas = popupView.overlay_video_commentCanvas
            commentCanvas.isFloatingView = true
            //SurfaceViewセット
            exoPlayer.setVideoSurfaceView(popupView.overlay_video_surfaceview)

            //閉じる
            popupView.overlay_video_close_button.setOnClickListener {
                stopSelf()
            }

            //ミュート・ミュート解除
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

            //ボタン表示
            popupView.setOnClickListener {
                if (popupView.overlay_video_button_layout.visibility == View.GONE) {
                    //表示
                    popupView.overlay_video_button_layout.visibility = View.VISIBLE
                    popupView.overlay_video_controller_layout.visibility = View.VISIBLE
                } else {
                    //非表示
                    popupView.overlay_video_button_layout.visibility = View.GONE
                    popupView.overlay_video_controller_layout.visibility = View.GONE
                }
            }

            // 大きさ変更。まず変更前を入れておく
            var normalHeight = -1
            var normalWidth = -1
            popupView.overlay_video_size_seekbar.apply {
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        // 初期値。動画再生時までアスペ比が取得できないのでサイズ変更を一番最初に行うときに最小サイズを取得。
                        if (normalHeight == -1) {
                            normalHeight = popupLayoutParams.height
                            normalWidth = popupLayoutParams.width
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
                        // サイズ変更をCommentCanvasに反映させる
                        commentCanvas.viewTreeObserver.addOnGlobalLayoutListener {
                            commentCanvas.apply {
                                finalHeight = commentCanvas.height
                                fontsize = (finalHeight / 10).toFloat()
                                blackPaint.textSize = fontsize
                            }
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {

                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {

                    }
                })
            }

            //画面サイズ
            val displaySize: Point by lazy {
                val display = windowManager.defaultDisplay
                val size = Point()
                display.getSize(size)
                size
            }

            //移動
            popupView.setOnTouchListener { view, motionEvent ->
                // タップした位置を取得する
                val x = motionEvent.rawX.toInt()
                val y = motionEvent.rawY.toInt()

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
                    }
                }
                false
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

            // シーク用に毎秒動くタイマー
            seekTimer.schedule(timerTask {
                if (exoPlayer.isPlaying) {
                    setProgress()
                    drawComment()
                }
            }, 1000, 1000)

        }
    }

    // コメント描画あああ
    private fun drawComment() {
        val drawList = commentList.filter { arrayList ->
            (arrayList[4].toInt() / 100) == (exoPlayer.contentPosition / 1000L).toInt()
        }
        drawList.forEach {
            val commentJSONParse = CommentJSONParse(it[8], "アリーナ", videoId)
            commentCanvas.postComment(it[2], commentJSONParse)
        }
    }

    // シークを動画再生時間に合わせる
    private fun setProgress() {
        popupView.overlay_video_video_seek_bar.progress =
            (exoPlayer.currentPosition / 1000L).toInt()
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

    /**
     * ポップアップ再生かどうかを返す
     * @return ポップアップ再生ならtrue
     * */
    fun isPopupPlay(): Boolean {
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
        mediaSessionCompat.release()
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
