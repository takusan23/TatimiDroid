package io.github.takusan23.tatimidroid.Service

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.NicoLive.Activity.CommentActivity
import io.github.takusan23.tatimidroid.NicoAPI.JK.NicoJKHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.Tool.DisplaySizeTool
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import io.github.takusan23.tatimidroid.Tool.isConnectionMobileDataInternet
import kotlinx.android.synthetic.main.overlay_player_layout.view.*
import kotlinx.coroutines.*
import java.util.*


/**
 * ニコ生をポップアップ、バックグラウンドで再生するやつ。
 * FragmentからServiceに移動させる
 *
 * Intentに詰めてほしいもの↓
 * mode           |String |"popup"（ポップアップ再生）か"background"(バッググラウンド再生)のどっちか。
 * live_id        |String |生放送ID
 * is_comment_post|Boolean|コメント投稿モードならtrue
 * is_nicocas     |Boolean|ニコキャス式コメント投稿モードならtrue
 * オプション。任意で
 * is_tokumei     |Boolean|184で投稿するか（省略時はtrue。匿名で投稿する。）
 * is_jk          |Boolean|ニコニコ実況の場合はtrue
 * start_quality  |String | 画質を設定したい場合は入れてね。
 *
 * */
class NicoLivePlayService : Service() {

    // 通知ID
    private val NOTIFICAION_ID = 865

    private lateinit var prefSetting: SharedPreferences
    private lateinit var windowManager: WindowManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var mediaSessionCompat: MediaSessionCompat

    // ニコ生視聴
    private val nicoLiveHTML = NicoLiveHTML()
    private val nicoJK = NicoJKHTML()
    lateinit var getFlvData: NicoJKHTML.getFlvData

    /** コメントサーバー接続など */
    val nicoLiveComment = NicoLiveComment()

    // View
    private lateinit var popupView: View
    private lateinit var popupExoPlayer: SimpleExoPlayer
    private lateinit var commentCanvas: CommentCanvas
    var uiHideTimer = Timer() // UIを自動で非表示にするためのTimer

    // 番組情報関係
    var liveId = ""
    var userSession = ""
    var isCommentPOSTMode = false   // コメント投稿モード。ログイン状態
    var isNicoCasMode = false       // nicocasモード。非ログイン状態
    var programTitle = ""
    var communityID = ""
    var thumbnailURL = ""
    var hlsAddress = ""

    // ニコニコ実況の場合はtrue
    var isJK = false

    // 再生モード。ポップアップかバッググラウンド
    var playMode = "popup"


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)
        userSession = prefSetting.getString("user_session", "") ?: ""
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Serviceはこれ呼ばないと怒られる
        showNotification(getString(R.string.loading))

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        playMode = intent?.getStringExtra("mode") ?: "popup"
        liveId = intent?.getStringExtra("live_id") ?: ""
        isCommentPOSTMode = intent?.getBooleanExtra("is_comment_post", true) ?: false
        isNicoCasMode = intent?.getBooleanExtra("is_nicocas", false) ?: false
        // 184
        val isTokumei = intent?.getBooleanExtra("is_tokumei", true) ?: true
        nicoLiveHTML.isTokumeiComment = isTokumei
        // 低遅延モードon/off
        nicoLiveHTML.isLowLatency = prefSetting.getBoolean("nicolive_low_latency", false)
        // JK
        isJK = intent?.getBooleanExtra("is_jk", false) ?: false
        // 開始時の画質を指定するか
        nicoLiveHTML.startQuality = intent?.getStringExtra("start_quality") ?: "high"

        // 初回の画質を低画質にする設定（モバイル回線とか強制低画質モードとか）
        val isMobileDataLowQuality = prefSetting.getBoolean("setting_mobiledata_quality_low", false) == isConnectionMobileDataInternet(this) // 有効時 でなお モバイルデータ接続時
        val isPreferenceLowQuality = prefSetting.getBoolean("setting_nicolive_quality_low", false)
        // 低画質設定
        if (isMobileDataLowQuality || isPreferenceLowQuality) {
            nicoLiveHTML.startQuality = "super_low"
        }

        if (isJK) {
            jkCoroutine()
        } else {
            coroutine()
        }

        initBroadcast()

        return START_NOT_STICKY
    }

    // ニコニコ実況のデータ取得
    private fun jkCoroutine() {
        GlobalScope.launch {
            // getflv叩く。
            val getFlvResponse = nicoJK.getFlv(liveId, userSession).await()
            if (!getFlvResponse.isSuccessful) {
                // 失敗のときは落とす
                stopSelf()
                showToast("${getString(R.string.error)}\n${getFlvResponse.code}")
                return@launch
            }
            // getflvパースする
            getFlvData = nicoJK.parseGetFlv(getFlvResponse.body?.string())!!
            Handler(Looper.getMainLooper()).post {
                // 通知の内容更新
                showNotification(getFlvData.channelName)
                initPopUpView()
            }
            // 接続。最後に呼べ。
            nicoJK.connectionCommentServer(getFlvData, ::commentFun)
        }
    }

    // データ取得
    private fun coroutine() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        GlobalScope.launch(errorHandler) {
            // ニコ生視聴ページリクエスト
            val livePageResponse = nicoLiveHTML.getNicoLiveHTML(liveId, userSession, true)
            if (!livePageResponse.isSuccessful) {
                // 失敗のときはService落とす
                this@NicoLivePlayService.stopSelf()
                showToast("${getString(R.string.error)}\n${livePageResponse.code}")
                return@launch
            }
            if (!nicoLiveHTML.hasNiconicoID(livePageResponse)) {
                // niconicoIDがない場合（ログインが切れている場合）はログインする（この後の処理でユーザーセッションが必要）
                NicoLogin.reNicoLogin(this@NicoLivePlayService)
                // 視聴モードなら再度視聴ページリクエスト
                if (isCommentPOSTMode) {
                    coroutine()
                    // コルーチン終了
                    return@launch
                }
            }
            // HTMLからJSON取得する
            val nicoLiveJSON = nicoLiveHTML.nicoLiveHTMLtoJSONObject(livePageResponse.body?.string())

            // コメント投稿の際に使う値を初期化する
            // 番組名取得など
            nicoLiveHTML.initNicoLiveData(nicoLiveJSON)
            programTitle = nicoLiveHTML.programTitle
            communityID = nicoLiveHTML.communityId
            thumbnailURL = nicoLiveHTML.thumb

            // 通知の内容更新
            showNotification(programTitle)

            // データ流してくれるWebSocketへ接続！
            nicoLiveHTML.apply {
                connectionWebSocket(nicoLiveJSON) { command, message ->
                    // 使うやつだけ
                    when {
                        command == "stream" -> {
                            // HLSアドレス取得
                            hlsAddress = getHlsAddress(message) ?: ""
                            // UI Thread
                            Handler(Looper.getMainLooper()).post {
                                // 生放送再生
                                initPlayer()
                            }
                        }
                        command == "room" -> {
                            // ポップアップ再生ならコメントサーバーに接続する
                            if (isPopupPlay()) {
                                // threadId、WebSocketURL受信。コメント送信時に使うWebSocketに接続する
                                // もし放送者の場合はWebSocketに部屋一覧が流れてくるので阻止。
                                val commentMessageServerUri = getCommentServerWebSocketAddress(message)
                                val commentThreadId = getCommentServerThreadId(message)
                                val commentRoomName = getCommentRoomName(message)
                                // 部屋が統合したので
                                nicoLiveComment.connectionWebSocket(commentMessageServerUri, commentThreadId, commentRoomName, ::commentFun)
                            }
                        }
                    }
                    // containsで部分一致にしてみた。なんで部分一致なのかは私も知らん
                    if (command.contains("disconnect")) {
                        // 番組終了
                        this@NicoLivePlayService.stopSelf()
                    }
                }
            }

            // Store鯖（流量制限で溢れたコメントが流れてくる鯖）へ接続する。
            // 公式番組以外 で ポップアップ再生時
            if (!nicoLiveHTML.isOfficial && isPopupPlay()) {
                connectionStoreCommentServer()
            }

        }
    }

    /**
     * Store鯖（流量制限で溢れたコメントが流れてくるサーバー）へ接続する
     * */
    private suspend fun connectionStoreCommentServer() = withContext(Dispatchers.Default) {
        // コメントサーバー取得API叩く
        val allRoomResponse = nicoLiveComment.getProgramInfo(liveId, userSession)
        if (!allRoomResponse.isSuccessful) {
            showToast("${getString(R.string.error)}\n${allRoomResponse.code}")
            return@withContext
        }
        // Store鯖を取り出す
        val storeCommentServerData = nicoLiveComment.parseStoreRoomServerData(allRoomResponse.body?.string(), getString(R.string.room_limit))
        if (storeCommentServerData != null) {
            // Store鯖へ接続する。（超）大手でなければ別に接続する必要はない
            nicoLiveComment.connectionWebSocket(storeCommentServerData.webSocketUri, storeCommentServerData.threadId, storeCommentServerData.roomName, ::commentFun)
        }
    }

    // コメント受け取り
    private fun commentFun(comment: String, roomName: String, isHistoryComment: Boolean) {
        val commentJSONParse = CommentJSONParse(comment, roomName, liveId)
        Handler(Looper.getMainLooper()).post {
            if (commentJSONParse.origin != "C") {
                // 初期化してないときは落とす
                if (!::commentCanvas.isInitialized) {
                    return@post
                }
                // 豆先輩とか
                if (!commentJSONParse.comment.contains("\n")) {
                    commentCanvas.postComment(commentJSONParse.comment, commentJSONParse)
                } else {
                    // https://stackoverflow.com/questions/6756975/draw-multi-line-text-to-canvas
                    // 豆先輩！！！！！！！！！！！！！！！！！！
                    // 下固定コメントで複数行だとAA（アスキーアートの略 / CA(コメントアート)とも言う）がうまく動かない。配列の中身を逆にする必要がある
                    // Kotlinのこの書き方ほんと好き
                    val asciiArtComment = if (commentJSONParse.mail.contains("shita")) {
                        commentJSONParse.comment.split("\n").reversed() // 下コメントだけ逆順にする
                    } else {
                        commentJSONParse.comment.split("\n")
                    }
                    for (line in asciiArtComment) {
                        commentCanvas.postComment(line, commentJSONParse, true)
                    }
                }
            }
        }
    }

    /**
     * ポップアップ再生かどうかを返す
     * @return ポップアップ再生ならtrue
     * */
    private fun isPopupPlay(): Boolean {
        return playMode == "popup"
    }

    /**
     * ポップアップView初期化。ぽっぴっぽーみたい（？）
     * */
    private fun initPlayer() {


        // ポップアップ再生、バッググラウンド再生　共にExoPlayer、MediaSessionの初期化を行う。

        // ExoPlayer初期化
        popupExoPlayer = SimpleExoPlayer.Builder(this).build()
        val sourceFactory =
            DefaultDataSourceFactory(this, "TatimiDroid;@takusan_23", object : TransferListener {
                override fun onTransferInitializing(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {

                }

                override fun onTransferStart(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {

                }

                override fun onTransferEnd(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {

                }

                override fun onBytesTransferred(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean, bytesTransferred: Int) {

                }
            })

        val hlsMediaSource = HlsMediaSource.Factory(sourceFactory).createMediaSource(hlsAddress.toUri())

        //再生準備
        popupExoPlayer.prepare(hlsMediaSource)
        //再生
        popupExoPlayer.playWhenReady = true
        // エラーのとき
        popupExoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                error.printStackTrace()
                println("生放送の再生が止まりました。")
                // 番組終了していなければ
                if (!nicoLiveHTML.nicoLiveWebSocketClient.isClosed) {
                    println("再度再生準備を行います")
                    Handler(Looper.getMainLooper()).post {
                        // 再生準備
                        popupExoPlayer.prepare(hlsMediaSource)
                        if (::popupView.isInitialized) {
                            //SurfaceViewセット
                            popupExoPlayer.setVideoSurfaceView(popupView.overlay_surfaceview)
                        }
                        //再生
                        popupExoPlayer.playWhenReady = true
                    }
                }
            }
        })

        // MediaSession。通知もう一階出せばなんか表示されるようになった。Androidむずかちい
        showNotification(programTitle)
        initMediaSession()

        // ポップアップ再生ならポップアップViewを用意する
        if (isPopupPlay()) {
            initPopUpView()
        }

    }

    private fun initPopUpView() {
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
        val width = DisplaySizeTool().getDisplayWidth(this) / 2
        //アスペクト比16:9なので
        val height = (width / 16) * 9
        //レイアウト読み込み
        val layoutInflater = LayoutInflater.from(this)
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
        popupView = layoutInflater.inflate(R.layout.overlay_player_layout, null)
        // 表示
        windowManager.addView(popupView, params)
        commentCanvas = popupView.overlay_commentCanvas
        commentCanvas.isPopupView = true
        if (isJK) {
            // ニコニコ実況の場合はSurfaceView非表示
            popupView.overlay_surfaceview.visibility = View.GONE
            // 半透明
            (popupView.overlay_surfaceview.parent as FrameLayout).setBackgroundColor(Color.parseColor("#1A000000"))
            // ミュートボタン塞ぐ
            popupView.overlay_sound_button.visibility = View.GONE
        }

        // SurfaceViewセット
        if (::popupExoPlayer.isInitialized) {
            popupExoPlayer.setVideoSurfaceView(popupView.overlay_surfaceview)
        }

        //閉じる
        popupView.overlay_close_button.setOnClickListener {
            stopSelf()
        }

        // サイズ変更の仕様変更メッセージ
        popupView.overlay_size_change_message_button.setOnClickListener {
            Toast.makeText(this, getString(R.string.popup_size_change_message), Toast.LENGTH_SHORT).show()
        }

        //アプリ起動
        popupView.overlay_activity_launch.setOnClickListener {
            stopSelf()
            // モード選ぶ
            val mode = when {
                isCommentPOSTMode -> "comment_post"
                isNicoCasMode -> "nicocas"
                else -> "comment_viewer"
            }
            // アプリ起動
            val intent = Intent(this, CommentActivity::class.java)
            intent.putExtra("liveId", liveId)
            intent.putExtra("watch_mode", mode)
            intent.putExtra("isOfficial", nicoLiveHTML.isOfficial)
            intent.putExtra("is_jk", isJK)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        //ミュート・ミュート解除
        popupView.overlay_sound_button.setOnClickListener {
            if (::popupExoPlayer.isInitialized) {
                popupExoPlayer.apply {
                    //音が０のとき
                    if (volume == 0f) {
                        volume = 1f
                        popupView.overlay_sound_button.setImageDrawable(getDrawable(R.drawable.ic_volume_up_24px))
                    } else {
                        volume = 0f
                        popupView.overlay_sound_button.setImageDrawable(getDrawable(R.drawable.ic_volume_off_24px))
                    }
                }
            }
        }

        // ピンチイン、ピンチアウトでズームなど
        // ピンチイン、ピンチアウトって単語、スマホ黎明期みたいで懐かしいな
        val scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScaleBegin(p0: ScaleGestureDetector?): Boolean {
                return true
            }

            override fun onScaleEnd(p0: ScaleGestureDetector?) {

            }

            override fun onScale(p0: ScaleGestureDetector?): Boolean {
                // ズーム操作中
                if (p0 == null) return true
                // なんかうまくいくコード
                params.width = (params.width * p0.scaleFactor).toInt()
                // アスペクト比が(頭)おかCなるので計算で出す。(16対9なので)
                params.height = (params.width / 16) * 9
                // 更新
                windowManager.updateViewLayout(popupView, params)
                // 大きさを保持しておく
                prefSetting.edit {
                    putInt("nicolive_popup_width", params.width)
                    putInt("nicolive_popup_height", params.height)
                }
                return true
            }
        })

        // 移動
        popupView.setOnTouchListener { view, motionEvent ->
            // タップした位置を取得する
            val x = motionEvent.rawX.toInt()
            val y = motionEvent.rawY.toInt()
            // ピンチイン、ピンチアウト対応
            scaleGestureDetector.onTouchEvent(motionEvent)
            when (motionEvent.action) {
                // Viewを移動させてるときに呼ばれる
                MotionEvent.ACTION_MOVE -> {

                    // オーバーレイ表示領域の座標を移動させる
                    params.x = x - (DisplaySizeTool().getDisplayWidth(this) / 2)
                    params.y = y - (DisplaySizeTool().getDisplayHeight(this) / 2)

                    // 移動した分を更新する
                    windowManager.updateViewLayout(view, params)

                    // 位置保存
                    prefSetting.edit {
                        putInt("nicolive_popup_x_pos", params.x)
                        putInt("nicolive_popup_y_pos", params.y)
                    }
                    return@setOnTouchListener true // ここのtrue意味あるんかな
                }
            }
            return@setOnTouchListener false
        }

        //ボタン表示
        popupView.setOnClickListener {
            // ktxで表示/非表示を短く書こう！
            popupView.overlay_button_layout.isVisible = !popupView.overlay_button_layout.isVisible
        }

        popupView.overlay_send_comment_button.setOnClickListener {
            showNotification(programTitle)
        }

        // サイズが保存されていれば適用
        if (prefSetting.getInt("nicolive_popup_width", 0) != 0) {
            params.width = prefSetting.getInt("nicolive_popup_width", width)
            params.height = prefSetting.getInt("nicolive_popup_height", height)
            windowManager.updateViewLayout(popupView, params)
        }

        // 位置が保存されていれば適用
        if (prefSetting.getInt("nicolive_popup_x_pos", 0) != 0) {
            params.x = prefSetting.getInt("nicolive_popup_x_pos", 0)
            params.y = prefSetting.getInt("nicolive_popup_y_pos", 0)
            windowManager.updateViewLayout(popupView, params)
        }

    }

    /** MediaSession。音楽アプリの再生中のあれ */
    private fun initMediaSession() {
        val mode = if (isPopupPlay()) {
            getString(R.string.popup_notification_title)
        } else {
            getString(R.string.background_play)
        }
        mediaSessionCompat = MediaSessionCompat(this, "nicolive")
        // メタデータ
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "$programTitle / $liveId")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, liveId)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, programTitle)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, programTitle)
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

    // コメント投稿用のコメントサーバー接続
    private fun commentPOSTWebSocketConnection(commentMessageServerUri: String, commentThreadId: String, userId: String) {
        nicoLiveHTML.apply {
            // コメントサーバー接続
            connectionCommentPOSTWebSocket(commentMessageServerUri, commentThreadId, userId) { message ->
                // アンケートとかはしないから。。。
            }
        }
    }

    /**
     * サービス実行中通知を送る関数。
     * まずこれを呼んでServiceを終了させないようにしないといけない。
     * */
    private fun showNotification(message: String) {
        // 停止Broadcast送信
        val stopPopupIntent = Intent("program_popup_close")
        // 通知のタイトル設定
        val title = if (isPopupPlay()) {
            getString(R.string.popup_notification_title)
        } else {
            getString(R.string.background_play)
        }
        val icon = if (isPopupPlay()) {
            R.drawable.ic_popup_icon
        } else {
            R.drawable.ic_background_icon
        }
        // 通知作成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "program_popup"
            val notificationChannel =
                NotificationChannel(notificationChannelId, getString(R.string.popup_notification_title), NotificationManager.IMPORTANCE_HIGH)

            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val programNotification =
                NotificationCompat.Builder(this, notificationChannelId).apply {
                    setContentTitle(title)
                    setContentText(message)
                    setSmallIcon(icon)
                    if (isPopupPlay()) {
                        addAction(directReply())
                    }
                    addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@NicoLivePlayService, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                }.build()
            startForeground(NOTIFICAION_ID, programNotification)
        } else {
            val programNotification = NotificationCompat.Builder(this).apply {
                setContentTitle(title)
                setContentText(message)
                setSmallIcon(icon)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPopupPlay()) {
                    addAction(directReply()) // Android ぬがあー以降でDirect Replyが使える
                }
                addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@NicoLivePlayService, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }.build()
            startForeground(NOTIFICAION_ID, programNotification)
        }
    }

    /**
     * Direct Reply関係
     * https://qiita.com/syarihu/items/9e7eb50ac97148687475
     * */
    private fun directReply(): NotificationCompat.Action? {
        val intent = Intent("direct_reply_comment")
        // 入力されたテキストを受け取るPendingIntent
        val replyPendingIntent =
            PendingIntent.getBroadcast(this, 334, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        // 入力受ける
        val remoteInput = androidx.core.app.RemoteInput.Builder("direct_reply_comment").apply {
            setLabel("コメントを投稿")
        }.build()
        val action =
            NotificationCompat.Action.Builder(R.drawable.ic_send_black, "コメントを投稿", replyPendingIntent)
                .apply {
                    addRemoteInput(remoteInput)
                }.build()
        return action
    }

    /**
     * ブロードキャスト初期化。
     * Direct Replyの返信を受け取ったりするため
     * */
    private fun initBroadcast() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("program_popup_close")
        intentFilter.addAction("direct_reply_comment")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "program_popup_close" -> {
                        // 終了
                        this@NicoLivePlayService.stopSelf()
                    }
                    "direct_reply_comment" -> {
                        // コメント送信
                        // Direct Reply でポップアップ画面でもコメント投稿できるようにする。ぬがあー以降で使える
                        val remoteInput = RemoteInput.getResultsFromIntent(intent)
                        val comment = remoteInput.getCharSequence("direct_reply_comment")
                        when {
                            isJK -> {
                                nicoJK.postCommnet(comment as String, getFlvData.userId, getFlvData.baseTime.toLong(), getFlvData.threadId, userSession)
                            }
                            isCommentPOSTMode -> {
                                nicoLiveHTML.sendPOSTWebSocketComment(comment as String) // コメント投稿
                                showNotification(programTitle) // 通知再設置
                            }
                            isNicoCasMode -> {
                                // コメント投稿。nicocasのAPI叩く
                                nicoLiveHTML.sendCommentNicocasAPI(comment as String, "", liveId, userSession, { showToast(getString(R.string.error)) }, {
                                    showNotification(programTitle) // 通知再設置
                                })
                            }
                        }
                    }
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        if (::popupView.isInitialized) {
            windowManager.removeView(popupView)
        }
        if (::popupExoPlayer.isInitialized) {
            popupExoPlayer.release()
        }
        nicoLiveHTML.destroy()
        nicoLiveComment.destroy()
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
        nicoJK.destroy()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool().setLanguageContext(newBase))
    }

}

/**
 * 生放送のポップアップ再生、バッググラウンド再生サービス起動用関数。internal fun なのでどっからでも呼べると思う？
 * 注意：ポップアップ再生で権限がないときは表示せず権限取得画面を表示させます。
 * @param context Context
 * @param mode "popup"（ポップアップ再生）か"background"(バッググラウンド再生)のどっちか。
 * @param liveId 生放送ID
 * @param isCommentPost コメント投稿モードならtrue
 * @param isNicocasMode ニコキャス式湖面投稿モードならtrue
 * @param isTokumei 匿名でコメントする場合はtrue。省略時true
 * @param isJK 実況ならtrue。省略時false
 * @param startQuality 画質を指定する場合は入れてね。highとか。ない場合はそのままでいいです（省略時：high）。
 * */
internal fun startLivePlayService(context: Context?, mode: String, liveId: String, isCommentPost: Boolean, isNicocasMode: Boolean, isJK: Boolean = false, isTokumei: Boolean = true, startQuality: String = "high") {
    // ポップアップ再生の権限があるか
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
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context?.startActivity(intent)
            return
        }
    }
    val intent = Intent(context, NicoLivePlayService::class.java).apply {
        putExtra("mode", mode)
        putExtra("live_id", liveId)
        putExtra("is_comment_post", isCommentPost)
        putExtra("is_nicocas", isNicocasMode)
        putExtra("is_tokumei", isTokumei)
        putExtra("is_jk", isJK)
        putExtra("start_quality", startQuality)
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
