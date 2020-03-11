package io.github.takusan23.tatimidroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Handler
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
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import io.github.takusan23.tatimidroid.Fragment.CommentFragment
import kotlinx.android.synthetic.main.overlay_player_layout.view.*

class PopUpPlayer(var context: Context?, var commentFragment: CommentFragment) {

    // 表示されてればTrue
    var isPopupPlay = false
    // View
    lateinit var popupView: View
    lateinit var popupExoPlayer: SimpleExoPlayer
    lateinit var commentCanvas: CommentCanvas
    // MediaSession
    lateinit var mediaSessionCompat: MediaSessionCompat
    // 通知
    private val overlayNotificationID = 865
    // manager
    private val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val notificationManager =
        context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showPopUpView(hlsAddress: String) {
        //すでにある場合は消す
        if (::popupExoPlayer.isInitialized) {
            destroyExoPlayer(popupExoPlayer)
            // windowManager.removeView(popupView)
        }
        if (Settings.canDrawOverlays(context)) {
            // 画面の半分を利用するように
            val wm = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val disp = wm.getDefaultDisplay()
            val realSize = Point();
            disp.getRealSize(realSize);
            val width = realSize.x / 2

            //アスペクト比16:9なので
            val height = (width / 16) * 9
            //レイアウト読み込み
            val layoutInflater = LayoutInflater.from(context)
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
            isPopupPlay = true
            popupView.overlay_commentCanvas.isFloatingView = true
            commentCanvas = popupView.overlay_commentCanvas

            // MediaSession
            initMediaSession()
            //通知表示
            showPopUpPlayerNotification()

            //ポップアップ再生もExoPlayerにお引越し。
            popupExoPlayer = ExoPlayerFactory.newSimpleInstance(context)
            val sourceFactory = DefaultDataSourceFactory(
                context,
                "TatimiDroid;@takusan_23",
                object : TransferListener {
                    override fun onTransferInitializing(
                        source: DataSource?,
                        dataSpec: DataSpec?,
                        isNetwork: Boolean
                    ) {

                    }

                    override fun onTransferStart(
                        source: DataSource?,
                        dataSpec: DataSpec?,
                        isNetwork: Boolean
                    ) {

                    }

                    override fun onTransferEnd(
                        source: DataSource?,
                        dataSpec: DataSpec?,
                        isNetwork: Boolean
                    ) {

                    }

                    override fun onBytesTransferred(
                        source: DataSource?,
                        dataSpec: DataSpec?,
                        isNetwork: Boolean,
                        bytesTransferred: Int
                    ) {

                    }
                })

            val hlsMediaSource = HlsMediaSource.Factory(sourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(hlsAddress.toUri());

            //再生準備
            popupExoPlayer.prepare(hlsMediaSource)
            //SurfaceViewセット
            popupExoPlayer.setVideoSurfaceView(popupView.overlay_surfaceview)
            //再生
            popupExoPlayer.playWhenReady = true

            popupExoPlayer.addListener(object : Player.EventListener {

                override fun onPlayerError(error: ExoPlaybackException?) {
                    super.onPlayerError(error)
                    error?.printStackTrace()
                    println("生放送の再生が止まりました。")
                    // 番組終了していなければ
                    if ((System.currentTimeMillis() / 1000L) < commentFragment.programEndUnixTime) {
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
                isPopupPlay = false
                windowManager.removeView(popupView)
                notificationManager.cancel(overlayNotificationID)
                popupExoPlayer.apply {
                    playWhenReady = false
                    stop()
                    seekTo(0)
                    release()
                }
            }

            //アプリ起動
            popupView.overlay_activity_launch.setOnClickListener {
                destroy()
                // アプリ起動
                val intent = commentFragment.activity?.intent
                intent?.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT // すでにある場合は新しく生成しない
                context?.startActivity(intent)
            }

            //ミュート・ミュート解除
            popupView.overlay_sound_button.setOnClickListener {
                if (::popupExoPlayer.isInitialized) {
                    popupExoPlayer.apply {
                        //音が０のとき
                        if (volume == 0f) {
                            volume = 1f
                            popupView.overlay_sound_button.setImageDrawable(context?.getDrawable(R.drawable.ic_volume_up_24px))
                        } else {
                            volume = 0f
                            popupView.overlay_sound_button.setImageDrawable(context?.getDrawable(R.drawable.ic_volume_off_24px))
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

            //コメント流し
            val overlay_commentCanvas =
                popupView.findViewById<CommentCanvas>(R.id.overlay_commentCanvas)
            overlay_commentCanvas.isPopupView = true

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
                showPopUpPlayerNotification()
            }

            // 大きさ変更。まず変更前を入れておく
            val normalHeight = params.height
            val normalWidth = params.width
            popupView.overlay_size_seekbar.apply {
                setOnSeekBarChangeListener(object :
                    SeekBar.OnSeekBarChangeListener {
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
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 権限貰いに行く
            // 権限取得
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context?.packageName}"))
            context?.startActivity(intent)
        }
    }

    /** MediaSession。音楽アプリの再生中のあれ */
    private fun initMediaSession() {
        mediaSessionCompat = MediaSessionCompat(context!!, "nicolive")
        // メタデータ
        val mediaMetadataCompat = MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "${commentFragment.programTitle} / ${commentFragment.liveId}")
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, commentFragment.liveId)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, commentFragment.programTitle)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, commentFragment.programTitle)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, context?.getString(R.string.popup_player))
        }.build()
        mediaSessionCompat.apply {
            setMetadata(mediaMetadataCompat) // メタデータ入れる
            isActive = true // これつけないとAlways On Displayで表示されない
            // 常に再生状態にしておく。これでAODで表示できる
            setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0L, 1F).build())
        }
    }

    /*ポップアップ再生通知*/
    fun showPopUpPlayerNotification() {

        val stopPopupIntent = Intent("program_popup_close")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "program_popup"
            val notificationChannel = NotificationChannel(
                notificationChannelId, context?.getString(R.string.popup_notification_title),
                NotificationManager.IMPORTANCE_HIGH
            )

            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val programNotification =
                NotificationCompat.Builder(context!!, notificationChannelId).apply {
                    // setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSessionCompat.sessionToken))
                    setContentTitle(context?.getString(R.string.popup_notification_description))
                    setContentText(commentFragment.programTitle)
                    setSmallIcon(R.drawable.ic_popup_icon)
                    addAction(directReply())
                    addAction(NotificationCompat.Action(R.drawable.ic_clear_black, context?.getString(R.string.finish), PendingIntent.getBroadcast(context, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
                }.build()

            //消せないようにする
            programNotification.flags = NotificationCompat.FLAG_ONGOING_EVENT

            notificationManager.notify(overlayNotificationID, programNotification)
        } else {
            val programNotification = NotificationCompat.Builder(context).apply {
                setContentTitle(context?.getString(R.string.notification_background_play))
                setContentText(commentFragment.programTitle)
                setSmallIcon(R.drawable.ic_popup_icon)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addAction(directReply()) // Android ぬがあー以降でDirect Replyが使える
                }
                addAction(NotificationCompat.Action(R.drawable.ic_clear_black, context?.getString(R.string.finish), PendingIntent.getBroadcast(context, 24, stopPopupIntent, PendingIntent.FLAG_UPDATE_CURRENT)))
            }.build()
            //消せないようにする
            programNotification.flags = NotificationCompat.FLAG_ONGOING_EVENT
            notificationManager.notify(overlayNotificationID, programNotification)
        }
    }

    /**
     *
     * https://qiita.com/syarihu/items/9e7eb50ac97148687475
     * */
    fun directReply(): NotificationCompat.Action? {
        val intent = Intent("direct_reply_comment")
        // 入力されたテキストを受け取るPendingIntent
        val replyPendingIntent =
            PendingIntent.getBroadcast(context, 334, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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

    //ExoPlayerを破棄するときに
    //初期化済みか確認してから
    private fun destroyExoPlayer(exoPlayer: SimpleExoPlayer) {
        //止める
        exoPlayer.apply {
            playWhenReady = false
            stop()
            seekTo(0)
            release()
        }
    }

    // 終了
    fun destroy() {
        if (isInitializedExoPlayer()) {
            destroyExoPlayer(popupExoPlayer)
        }
        if (isInitializedPopUpView()) {
            windowManager.removeView(popupView)
        }
        notificationManager.cancel(overlayNotificationID) // 通知削除
        isPopupPlay = false
        if (isInitializedMediaSession()) {
            mediaSessionCompat.isActive = false
            mediaSessionCompat.release()
        }
    }

    /** ExoPlayer初期化済みか */
    fun isInitializedExoPlayer(): Boolean {
        return ::popupExoPlayer.isInitialized
    }

    /** ポップアップのViewが初期化済みか */
    fun isInitializedPopUpView(): Boolean {
        return ::popupView.isInitialized
    }

    /** MediaSession初期化済みか */
    fun isInitializedMediaSession(): Boolean {
        return ::mediaSessionCompat.isInitialized
    }

}