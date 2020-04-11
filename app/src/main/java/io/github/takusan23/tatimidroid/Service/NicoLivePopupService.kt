package io.github.takusan23.tatimidroid.Service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.CommentCanvas
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.isConnectionInternet
import kotlinx.android.synthetic.main.overlay_player_layout.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.timerTask


/**
 * ニコ生をポップアップで再生するやつ。
 * FragmentからServiceに移動させる
 * */
class NicoLivePopupService : Service() {

    // 通知ID
    private val NOTIFICAION_ID = 865

    private lateinit var prefSetting: SharedPreferences
    private lateinit var windowManager: WindowManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var broadcastReceiver: BroadcastReceiver
    private lateinit var mediaSessionCompat: MediaSessionCompat

    // ニコ生視聴
    private val nicoLiveHTML = NicoLiveHTML()

    // ニコ生前部屋接続など
    val nicoLiveComment = NicoLiveComment()
    val timer = Timer()

    // View
    private lateinit var popupView: View
    private lateinit var popupExoPlayer: SimpleExoPlayer
    private lateinit var commentCanvas: CommentCanvas

    // 番組情報関係
    var liveId = ""
    var userSession = ""
    var isCommentPOSTMode = false   // コメント投稿モード。ログイン状態
    var isNicoCasMode = false       // nicocasモード。非ログイン状態
    var programTitle = ""
    var communityID = ""
    var thumbnailURL = ""
    var hlsAddress = ""

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

        liveId = intent?.getStringExtra("live_id") ?: ""
        isCommentPOSTMode = intent?.getBooleanExtra("is_comment_post", true) ?: false
        isNicoCasMode = intent?.getBooleanExtra("is_nicocas", false) ?: false

        coroutine()

        initBroadcast()

        return START_NOT_STICKY
    }

    // データ取得
    private fun coroutine() {
        GlobalScope.launch {
            // ニコ生視聴ページリクエスト
            val livePageResponse =
                nicoLiveHTML.getNicoLiveHTML(liveId, userSession, true).await()
            if (!livePageResponse.isSuccessful) {
                // 失敗のときはService落とす
                this@NicoLivePopupService.stopSelf()
                showToast("${getString(R.string.error)}\n${livePageResponse.code}")
                return@launch
            }
            if (!nicoLiveHTML.hasNiconicoID(livePageResponse)) {
                // niconicoIDがない場合（ログインが切れている場合）はログインする（この後の処理でユーザーセッションが必要）
                NicoLogin.loginCoroutine(this@NicoLivePopupService).await()
                // 視聴モードなら再度視聴ページリクエスト
                if (isCommentPOSTMode) {
                    coroutine()
                    // コルーチン終了
                    return@launch
                }
            }
            // HTMLからJSON取得する
            val nicoLiveJSON =
                nicoLiveHTML.nicoLiveHTMLtoJSONObject(livePageResponse.body?.string())

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
                        command == "currentstream" -> {
                            // HLSアドレス取得
                            hlsAddress = getHlsAddress(message) ?: ""
                            // モバイルデータは最低画質で読み込む設定
                            if (prefSetting.getBoolean("setting_mobiledata_quality_low", false)) {
                                if (isConnectionInternet(this@NicoLivePopupService)) {
                                    // 最低画質指定
                                    nicoLiveHTML.sendQualityMessage("super_low")
                                }
                            }
                            // UI Thread
                            Handler(Looper.getMainLooper()).post {
                                // 生放送再生
                                initPopupPlayer()
                            }
                        }
                        command == "currentroom" -> {
                            // threadId、WebSocketURL受信。コメント送信時に使うWebSocketに接続する
                            val jsonObject = JSONObject(message)
                            // もし放送者の場合はWebSocketに部屋一覧が流れてくるので阻止。
                            if (jsonObject.getJSONObject("body").has("room")) {
                                val commentMessageServerUri =
                                    getCommentServerWebSocketAddress(message)
                                val commentThreadId = getCommentServerThreadId(message)
                                val commentRoomName = getCommentRoomName(message)
                                // 公式番組のとき
                                if (isOfficial) {
                                    Handler(Looper.getMainLooper()).post {
                                        // WebSocketで流れてきたアドレスへ接続する
                                        nicoLiveComment.connectionWebSocket(commentMessageServerUri, commentThreadId, commentRoomName, ::commentFun)
                                    }
                                }
                                // コメント投稿時に使うWebSocketに接続する
                                commentPOSTWebSocketConnection(commentMessageServerUri, commentThreadId, userId)
                            }
                        }
                        message.contains("disconnect") -> {
                            this@NicoLivePopupService.stopSelf()
                        }
                    }
                }
            }
            // 全部屋接続。定期的にAPIを叩く
            // ただし公式番組では利用できないので分岐
            if (!nicoLiveHTML.isOfficial) {
                timer.schedule(timerTask { initAllRoomConenct() }, 0, 60 * 1000)
            }
        }
    }

    /**
     * 全部屋接続！！！
     * */
    private fun initAllRoomConenct() {
        GlobalScope.launch {
            // 全部屋接続
            val allRoomResponse = nicoLiveComment.getProgramInfo(liveId, userSession).await()
            if (!allRoomResponse.isSuccessful) {
                showToast("${getString(R.string.error)}\n${allRoomResponse.code}")
                return@launch
            }
            // JSONパース
            val jsonObject = JSONObject(allRoomResponse.body?.string())
            val data = jsonObject.getJSONObject("data")
            val room = data.getJSONArray("rooms")
            // アリーナ、立ち見のコメントサーバーへ接続
            for (index in 0 until room.length()) {
                val roomObject = room.getJSONObject(index)
                val webSocketUri = roomObject.getString("webSocketUri")
                var roomName = roomObject.getString("name")
                val threadId = roomObject.getString("threadId")
                // 現在接続中のアドレスから同じのがないかちぇっく
                // 定期的に新しい部屋が無いか確認しに行くため
                nicoLiveComment.apply {
                    if (connectedWebSocketAddressList.indexOf(webSocketUri) == -1) {
                        connectedWebSocketAddressList.add(webSocketUri)
                        //アリーナの場合は部屋名がコミュニティ番号なので直す
                        if (roomName.contains("co") || roomName.contains("ch")) {
                            roomName = getString(R.string.arena) ?: "アリーナ"
                        }
                        //WebSocket接続。::関数 で高階関数を引数に入れられる
                        connectionWebSocket(webSocketUri, threadId, roomName, ::commentFun)
                    }
                }
            }
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
     * ポップアップView初期化。ぽっぴっぽーみたい（？）
     * */
    private fun initPopupPlayer() {

        if (!Settings.canDrawOverlays(this)) {
            return
        }

        // 画面の半分を利用するように
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val disp = wm.getDefaultDisplay()
        val realSize = Point();
        disp.getRealSize(realSize);
        val width = realSize.x / 2

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
        //表示
        windowManager.addView(popupView, params)
        popupView.overlay_commentCanvas.isFloatingView = true
        commentCanvas = popupView.overlay_commentCanvas

        // MediaSession
        initMediaSession()

        // ExoPlayer初期化
        popupExoPlayer = SimpleExoPlayer.Builder(this).build()
        val sourceFactory = DefaultDataSourceFactory(
            this,
            "TatimiDroid;@takusan_23",
            object : TransferListener {
                override fun onTransferInitializing(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {

                }

                override fun onTransferStart(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {

                }

                override fun onTransferEnd(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean) {

                }

                override fun onBytesTransferred(source: DataSource?, dataSpec: DataSpec?, isNetwork: Boolean, bytesTransferred: Int) {

                }
            })

        val hlsMediaSource = HlsMediaSource.Factory(sourceFactory)
            .setAllowChunklessPreparation(true)
            .createMediaSource(hlsAddress.toUri())

        //再生準備
        popupExoPlayer.prepare(hlsMediaSource)
        //SurfaceViewセット
        popupExoPlayer.setVideoSurfaceView(popupView.overlay_surfaceview)
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
                        //SurfaceViewセット
                        popupExoPlayer.setVideoSurfaceView(popupView.overlay_surfaceview)
                        //再生
                        popupExoPlayer.playWhenReady = true
                    }
                }
            }
        })

        //閉じる
        popupView.overlay_close_button.setOnClickListener {
            stopSelf()
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
                    params.x = centerX
                    params.y = centerY

                    // 移動した分を更新する
                    windowManager.updateViewLayout(view, params)
                }
            }
            false
        }

        //ボタン表示
        popupView.setOnClickListener {
            if (popupView.overlay_button_layout.visibility == View.GONE) {
                //表示
                popupView.overlay_button_layout.visibility = View.VISIBLE
            } else {
                //非表示
                popupView.overlay_button_layout.visibility = View.GONE
            }
        }

        popupView.overlay_send_comment_button.setOnClickListener {
            showNotification(programTitle)
        }

        // 大きさ変更。まず変更前を入れておく
        val normalHeight = params.height
        val normalWidth = params.width
        popupView.overlay_size_seekbar.apply {
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    // 操作中
                    params.height = normalHeight + (progress + 1) * 9
                    params.width = normalWidth + (progress + 1) * 16
                    windowManager.updateViewLayout(popupView, params)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }

    }

    /** MediaSession。音楽アプリの再生中のあれ */
    private fun initMediaSession() {
        mediaSessionCompat = MediaSessionCompat(this, "nicolive")
        // メタデータ
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "$programTitle / $liveId")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, liveId)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, programTitle)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, programTitle)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.popup_player))
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
            connectionCommentPOSTWebSocket(commentMessageServerUri, threadId, userId) { message ->
                // アンケートとかはしないから。。。
            }
        }
    }

    /**
     * サービス実行中通知を送る関数。
     * まずこれを呼んでServiceを終了させないようにしないといけない。
     * */
    private fun showNotification(title: String) {
        val stopPopupIntent = Intent("program_popup_close")
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
                    // setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken))
                    setContentTitle(getString(R.string.popup_notification_description))
                    setContentText(title)
                    setSmallIcon(R.drawable.ic_popup_icon)
                    addAction(directReply())
                    addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@NicoLivePopupService, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                }.build()
            //消せないようにする
            startForeground(NOTIFICAION_ID, programNotification)
        } else {
            val programNotification = NotificationCompat.Builder(this).apply {
                setContentTitle(getString(R.string.notification_background_play))
                setContentText(title)
                setSmallIcon(R.drawable.ic_popup_icon)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addAction(directReply()) // Android ぬがあー以降でDirect Replyが使える
                }
                addAction(NotificationCompat.Action(R.drawable.ic_clear_black, getString(R.string.finish), PendingIntent.getBroadcast(this@NicoLivePopupService, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }.build()
            //消せないようにする
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
        /*
         * ブロードキャスト
         * */
        val intentFilter = IntentFilter()
        intentFilter.addAction("program_popup_close")
        intentFilter.addAction("direct_reply_comment")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "program_popup_close" -> {
                        // 終了
                        this@NicoLivePopupService.stopSelf()
                        onDestroy()
                    }
                    "direct_reply_comment" -> {

                    }
                }
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
        windowManager.removeView(popupView)
        popupExoPlayer.release()
        nicoLiveHTML.destroy()
        nicoLiveComment.destroy()
        mediaSessionCompat.release()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

}