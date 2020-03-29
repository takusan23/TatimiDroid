package io.github.takusan23.tatimidroid.Fragment

import android.app.*
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Activity.FloatingCommentViewer
import io.github.takusan23.tatimidroid.Adapter.CommentViewPager
import io.github.takusan23.tatimidroid.Background.BackgroundPlay
import io.github.takusan23.tatimidroid.GoogleCast.GoogleCast
import io.github.takusan23.tatimidroid.NicoLiveAPI.NicoLogin
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentCollectionSQLiteHelper
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import io.github.takusan23.tatimidroid.SQLiteHelper.NicoHistorySQLiteHelper
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.bottom_fragment_enquate_layout.view.*
import kotlinx.android.synthetic.main.comment_card_layout.*
import kotlinx.android.synthetic.main.fragment_comment_room_layout.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask

class CommentFragment : Fragment() {

    lateinit var commentActivity: AppCompatActivity

    lateinit var pref_setting: SharedPreferences

    lateinit var darkModeSupport: DarkModeSupport

    //ユーザーセッション
    var usersession = ""

    //視聴に必要なデータ受信用WebSocket
    lateinit var connectionNicoLiveWebSocket: WebSocketClient

    //放送開始時間？こっちは放送開始前まである。
    var programStartTime: Long = 0

    //放送開始時間？こっちが正しい
    var programLiveTime: Long = 0

    //コメント送信用WebSocket
    lateinit var commentPOSTWebSocketClient: WebSocketClient

    //コメント送信時に必要なpostKeyを払い出す時に必要なthreadId
    var getPostKeyThreadId = ""

    //コメント投稿時につかうticketを入れておく
    //これはコメント送信用WebSocketにthreadメッセージ（過去コメントなど指定して）送信すると帰ってくるJSONObjectに入ってる
    var commentTicket = ""

    //コメント投稿に必須なユーザーID
    var userId = ""

    //コメント投稿に必須なプレミアム会員かどうか
    var premium = 0

    //コメントの内容
    var commentValue = ""

    //コマンド（匿名とか）
    var commentCommand = ""

    //視聴モード（コメント投稿機能付き）かどうか
    var isWatchingMode = false

    //視聴モードがnicocasの場合
    var isNicocasMode = false

    //hls
    var hls_address = ""

    //こてはん（固定ハンドルネーム　配列
    val kotehanMap = mutableMapOf<String, String>()

    //生放送を見る場合はtrue
    var watchLive = false

    //TTS使うか
    var isTTS = false

    //Toast表示
    var isToast = false

    //定期的に投稿するやつ
    //視聴続けてますよ送信用
    var timer = Timer()

    //経過時間
    var programTimer = Timer()

    //アクティブ計算
    val activeTimer = Timer()
    val activeList = arrayListOf<String>()

    //番組ID
    var liveId = ""

    //番組名
    var programTitle = ""

    //コミュニティID
    var communityID = ""

    //サムネイル
    var thumbnailURL = ""

    //NGデータベース
    lateinit var ngListSQLiteHelper: NGListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    //コメントNG配列
    val commentNGList = arrayListOf<String>()

    //ユーザーNG配列
    val userNGList = arrayListOf<String>()

/*
    //ポップアップ再生（オーバーレイ）
    var overlay_commentcamvas: CommentCanvas? = null
    lateinit var popupView: View
    lateinit var overlay_commentTextView: TextView
*/

    //オーバーレイ再生中かどうか。
    // var isPopupPlay = false
    //オーバーレイ再生の通知ID
    val overlayNotificationID = 5678

    // //バックグラウンド再生MediaPlayer
    // lateinit var mediaPlayer: MediaPlayer
    lateinit var broadcastReceiver: BroadcastReceiver

    // //バックグラウンド再生できてるか
    // var isBackgroundPlay = false
    //バックグラウンド再生の通知ID
    val backgroundNotificationID = 1234

    lateinit var backgroundPlay: BackgroundPlay

    //NotificationManager
    lateinit var notificationManager: NotificationManager

    //コメント非表示？
    var isCommentHidden = false

    //アンケート内容いれとく
    var enquateJSONArray = ""

    //コメントコレクション
    val commentCollectionList = arrayListOf<String>()
    val commentCollectionYomiList = arrayListOf<String>()

    //アンケートView
    lateinit var enquateView: View

    //運営コメント
    lateinit var uncomeTextView: TextView

    //下のコメント（広告貢献、ランクイン等）
    lateinit var infoTextView: TextView

    //自動次枠移動
    var isAutoNextProgram = false
    var autoNextProgramTimer = Timer()

    //ミュート用
    lateinit var audioManager: AudioManager
    var volume = 0

    //運コメ・infoコメント非表示
    var hideInfoUnnkome = false

    //いやよ
    var isTokumeiComment = true

    //共有
    lateinit var programShare: ProgramShare

    //画質変更BottomSheetFragment
    lateinit var qualitySelectBottomSheet: QualitySelectBottomSheet

    //最初の画質
    var start_quality = ""

    //低遅延なのか。でふぉは低遅延有効
    var isLowLatency = true

    //モバイルデータなら最低画質の設定で一度だけ動かすように
    var mobileDataQualityCheck = false

    //二窓モードになっている場合
    var isNimadoMode = false

    /*
    * 画面回転でこの子たちnullになるのでfindViewByIdを絶対使わないということはできなかった。
    * */
    //lateinit var live_video_view: VideoView
    lateinit var commentCanvas: CommentCanvas
    lateinit var liveFrameLayout: FrameLayout
    lateinit var fab: FloatingActionButton

    lateinit var rotationSensor: RotationSensor

    //延長検知。視聴セッション接続後すぐに送られてくるので一回目はパス。
    var isEntyouKenti = false

    //匿名コメント非表示機能。基本off
    var isTokumeiHide = false

    //ExoPlayer
    lateinit var exoPlayer: SimpleExoPlayer

    //ポップアップ再生ようExoPlayer
    lateinit var popupExoPlayer: SimpleExoPlayer

    //番組終了時刻（UnixTime
    var programEndUnixTime: Long = 0

    // GoogleCast使うか？
    lateinit var googleCast: GoogleCast

    var isOfficial = false

    //コメントWebSocket
    var commentMessageServerUri = ""
    var commentThreadId = ""
    var commentRoomName = ""

    //履歴機能
    lateinit var nicoHistorySQLiteHelper: NicoHistorySQLiteHelper
    lateinit var nicoHistorySQLiteDatabase: SQLiteDatabase

    // フォント変更機能
    lateinit var customFont: CustomFont

    // ニコ生ゲームようWebView
    lateinit var nicoNamaGameWebView: NicoNamaGameWebView

    // ニコ生ゲームが有効になっているか
    var isAddedNicoNamaGame = false

    // 全部屋接続
    lateinit var allRoomComment: AllRoomComment

    // SurfaceView(ExoPlayer) + CommentCanvasのLayoutParams
    lateinit var surfaceViewLayoutParams: FrameLayout.LayoutParams

    // ポップアップ再生をクラスに切り分けた
    lateinit var popUpPlayer: PopUpPlayer

    lateinit var commentViewPager: CommentViewPager

    var isOpen = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        commentCanvas = view.findViewById(R.id.comment_canvas)
        liveFrameLayout = view.findViewById(R.id.live_framelayout)
        fab = view.findViewById(R.id.fab)

        isNimadoMode = activity is NimadoActivity

        commentActivity = activity as AppCompatActivity

        darkModeSupport = DarkModeSupport(context!!)
        darkModeSupport.setActivityTheme(activity as AppCompatActivity)

        // ActionBarが邪魔という意見があった（私も思う）ので消す
        if (activity !is NimadoActivity) {
            commentActivity.supportActionBar?.hide()
        }

        backgroundPlay = BackgroundPlay(context!!)
        popUpPlayer = PopUpPlayer(context, this)

        //起動時の音量を保存しておく
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        //ダークモード対応
//        activity_comment_bottom_navigation_bar.backgroundTintList =
//            ColorStateList.valueOf(darkModeSupport.getThemeColor())
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            commentActivity.supportActionBar?.setBackgroundDrawable(
                ColorDrawable(
                    darkModeSupport.getThemeColor()
                )
            )
            activity_comment_tab_layout.background = ColorDrawable(darkModeSupport.getThemeColor())
        }

        // GoogleCast？
        googleCast = GoogleCast(context!!)
        // GooglePlay開発者サービスがない可能性あり、Gapps焼いてない、ガラホ　など
        if (googleCast.isGooglePlayServicesAvailable()) {
            googleCast.init()
        }

        // 公式番組の場合はAPIが使えないため部屋別表示を無効にする。
        isOfficial = arguments?.getBoolean("isOfficial") ?: false
        // if (isOfficial) {
        //     activity_comment_tab_layout.getTabAt(2)?.tabLabelVisibility =
        //         TabLayout.TAB_LABEL_VISIBILITY_UNLABELED
        // }

        notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        initDB()

        //liveId = intent?.getStringExtra("liveId") ?: ""
        liveId = arguments?.getString("liveId") ?: ""

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context!!)

        isLowLatency = !pref_setting.getBoolean("setting_low_latency_off", false)

        // println("低遅延$isLowLatency")

        //センサーによる画面回転
        if (pref_setting.getBoolean("setting_rotation_sensor", false)) {
            rotationSensor = RotationSensor(commentActivity)
        }

        // ユーザーの設定したフォント読み込み
        customFont = CustomFont(context)

        /*
        * ID同じだと２窓のときなぜか隣のFragmentが置き換わるなどするので
        * IDを作り直す
        * */
        // fragment_comment_fragment_linearlayout.id = View.generateViewId()
        // 縦画面のときのみやる作業
        if (fragment_comment_fragment_linearlayout != null && comment_activity_fragment_layout_motionlayout != null) {
            fragment_comment_fragment_linearlayout.background =
                ColorDrawable(darkModeSupport.getThemeColor())
            setAlwaysShowProgramInfo()
            fragment_comment_fragment_linearlayout.setOnClickListener {
                // 表示、非表示
                comment_fragment_program_info.visibility =
                    if (comment_fragment_program_info.visibility == View.GONE) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
        }

        //とりあえずコメントViewFragmentへ
        val checkCommentViewFragment =
            childFragmentManager.findFragmentByTag("${liveId}_comment_view_fragment")
        //Fragmentは画面回転しても存在するのでremoveして終了させる。
        if (checkCommentViewFragment != null) {
            val fragmentTransaction =
                childFragmentManager.beginTransaction()
            fragmentTransaction.remove(checkCommentViewFragment)
            fragmentTransaction.commit()
        }

        //NGデータベース読み込み
        loadNGDataBase()

        //生放送を視聴する場合はtrue
        watchLive = pref_setting.getBoolean("setting_watch_live", true)

        //二窓モードではPreferenceの値を利用しない
        //いつかSharedPreferenceで視聴モードを管理するのやめようと思う。
        var watchingmode = false
        var nicocasmode = false
        if (arguments?.getString("watch_mode")?.isNotEmpty() == true) {
            isWatchingMode = false
            when (arguments?.getString("watch_mode")) {
                "comment_post" -> {
                    watchingmode = true
                    isWatchingMode = true
                }
                "nicocas" -> {
                    nicocasmode = true
                    isWatchingMode = false
                    isNicocasMode = true
                }
            }
        }

        if (!watchingmode && !nicocasmode) {
            fab.hide()
        }

        //運営コメント、InfoコメントのTextView初期化
        uncomeTextView = TextView(context!!)
        live_framelayout.addView(uncomeTextView)
        uncomeTextView.visibility = View.GONE
        //infoコメント
        infoTextView = TextView(context!!)
        live_framelayout.addView(infoTextView)
        infoTextView.visibility = View.GONE

        //視聴しない場合は非表示
        if (!watchLive) {
            live_framelayout.visibility = View.GONE
        }

        //コメント投稿画面開く
        fab.setOnClickListener {
            //表示アニメーションに挑戦した。
            val showAnimation =
                AnimationUtils.loadAnimation(context!!, R.anim.comment_cardview_show_animation)
            //表示
            comment_activity_comment_cardview.startAnimation(showAnimation)
            comment_activity_comment_cardview.visibility = View.VISIBLE
            fab.hide()
            //コメント投稿など
            commentCardView()
            //旧式はサポート切ります！
        }

        // ViewPager
        comment_viewpager.id = View.generateViewId()
        commentViewPager =
            CommentViewPager(activity as AppCompatActivity, liveId, isOfficial)
        comment_viewpager.adapter = commentViewPager
        activity_comment_tab_layout.setupWithViewPager(comment_viewpager)
        // コメントを指定しておく
        comment_viewpager.currentItem = 1
        // 初期化してないとき
        allRoomComment = AllRoomComment(context, liveId, this)

        // ステータスバー透明化＋タイトルバー非表示＋ノッチ領域にも侵略。関数名にAndがつくことはあんまりない
        hideStatusBarAndSetFullScreen()

        //ログイン情報がなければ戻す
        if (pref_setting.getString("mail", "")?.contains("") != false) {
            usersession = pref_setting.getString("user_session", "") ?: ""

            // htmlを視聴モード切り替えBottomFragmentから持ってくる。これで二回取得する必要がなくなるのでネットが遅いときはすごく助かる
            val html = arguments?.getString("html")
            if (html != null) {
                // HTMLパースする（HTMLの中にあるJSONをパース）
                parseNicoLiveHTML(html)
                // 部屋の名前と部屋番号を取得（これにユーザーセッションが必要）
                getplayerstatus()
                // 全部屋表示Fragment表示
                if (!isOfficial) {
                    // setAllRoomCommentFragment()
                }
            } else {
                // ニコ生の視聴ページ（HTML）を取得する。
                GlobalScope.launch {
                    // 取得できるまで待機
                    val response = getNicoLiveWebPage().await()
                    if (response.isSuccessful) {
                        // 成功した
                        // ログインできているかチェック（user_session切れなど）
                        // コメント投稿モード以外でもユーザーセッションは必要なのでここで判断しておく
                        var niconicoId = response.headers["x-niconico-id"]
                        // 視聴モード以外のときは適当に入れておく（将来的に視聴モードのみにするため）
                        if (!isWatchingMode) {
                            niconicoId = "てきとうに"
                        }
                        // ログイン無い
                        if (niconicoId == null) {
                            // ログイン済みの場合はレスポンスヘッダーにユーザーIDが入ってる。なければ(null)ログイン出来てない。
                            NicoLogin.login(context) {
                                // ログイン成功時でコメント投稿モードの場合は再度HTML（ニコ生視聴ページ）をリクエスト
                                if (isWatchingMode) {
                                    GlobalScope.launch {
                                        val againResponse = getNicoLiveWebPage().await()
                                        if (againResponse.isSuccessful && againResponse.headers["x-niconico-id"] != null) {
                                            // HTMLパースする（HTMLの中にあるJSONをパース）
                                            parseNicoLiveHTML(againResponse.body?.string())
                                        } else {
                                            showToast("ログインが切れたため再度ログインしました。しかし視聴ページの取得に失敗しました。\n${againResponse.code}")
                                        }
                                    }
                                }
                                // 部屋の名前と部屋番号を取得（これにユーザーセッションが必要）
                                getplayerstatus()
                                // 全部屋表示Fragment表示
                                if (!isOfficial) {
                                    // setAllRoomCommentFragment()
                                }
                            }
                        } else {
                            // HTMLパースする（HTMLの中にあるJSONをパース）
                            parseNicoLiveHTML(response.body?.string())
                            // 部屋の名前と部屋番号を取得（これにユーザーセッションが必要）
                            getplayerstatus()
                            // 全部屋表示Fragment表示
                            if (!isOfficial) {
                                // setAllRoomCommentFragment()
                            }
                        }
                    } else {
                        // 失敗。
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                }
            }
        } else {
            showToast(getString(R.string.mail_pass_error))
            commentActivity.finish()
        }

        //アクティブ人数クリアなど、追加部分はCommentViewFragmentです
        activeUserClear()

        /*
        * ブロードキャスト
        * */
        val intentFilter = IntentFilter()
        intentFilter.addAction("background_program_stop")
        intentFilter.addAction("background_program_pause")
        intentFilter.addAction("program_popup_close")
        intentFilter.addAction("direct_reply_comment")
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                when (p1?.action) {
                    "program_popup_close" -> {
                        popUpPlayer.apply {
                            if (isPopupPlay) {
                                if (isInitializedExoPlayer()) {
                                    //ポップアップ再生終了
                                    destroy()
                                }
                            }
                        }
                    }
                    "background_program_stop" -> {
                        //停止
                        backgroundPlay.pause()
                        notificationManager.cancel(backgroundNotificationID)//削除
                    }
                    "background_program_pause" -> {
                        if (backgroundPlay.exoPlayer.playWhenReady) {
                            //一時停止
                            backgroundPlay.pause()
                            //通知作成
                            backgroundPlayNotification(
                                getString(R.string.background_play_play),
                                NotificationCompat.FLAG_ONGOING_EVENT
                            )
                        } else {
                            //Liveで再生
                            backgroundPlay.play(hls_address.toUri())
                            //通知作成
                            backgroundPlayNotification(
                                getString(R.string.background_play_pause),
                                NotificationCompat.FLAG_ONGOING_EVENT
                            )
                        }
                    }
                    "direct_reply_comment" -> {
                        // Direct Reply でポップアップ画面でもコメント投稿できるようにする。ぬがあー以降で使える
                        val remoteInput = RemoteInput.getResultsFromIntent(p1)
                        val comment = remoteInput.getCharSequence("direct_reply_comment")
                        sendComment(comment as String, "") // コメント投稿
                        popUpPlayer.showPopUpPlayerNotification() // 通知再設置
                    }
                }
            }
        }
        commentActivity.registerReceiver(broadcastReceiver, intentFilter)

    }

    /**
     * 全画面UI
     * */
    fun hideStatusBarAndSetFullScreen() {
        if (pref_setting.getBoolean("setting_display_cutout", false)) {
            activity?.window?.decorView?.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val attrib = activity?.window?.attributes
                attrib?.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        } else {
            activity?.window?.decorView?.systemUiVisibility = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val attrib = activity?.window?.attributes
                attrib?.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
        }
    }

    fun setAlwaysShowProgramInfo() {
        // MotionLayout固定
        if (comment_activity_fragment_layout_motionlayout != null) {
            val isAlwaysShowProgramInfo =
                pref_setting.getBoolean("setting_always_program_info", false)
            if (isAlwaysShowProgramInfo) {
                // Start->End
                comment_fragment_program_info.visibility = View.VISIBLE
                // バー消す
                fragment_comment_bar.visibility = View.GONE
            } else {
                // End->Start
                comment_fragment_program_info.visibility = View.GONE
                // バー表示
                fragment_comment_bar.visibility = View.VISIBLE
            }
        }
    }

    // ニコ生ゲーム有効
    fun setNicoNamaGame() {
        // WebViewのためにFrameLayout広げるけど動画とコメントCanvasはサイズそのまま
        surfaceViewLayoutParams.apply {
            live_surface_view.layoutParams = this
            commentCanvas.layoutParams = this
        }
        // WebViewように少し広げる
        val frameLayoutParams = liveFrameLayout.layoutParams
        frameLayoutParams.height += 140
        liveFrameLayout.layoutParams = frameLayoutParams
        // ニコ生WebView
        nicoNamaGameWebView = NicoNamaGameWebView(context, liveId, live_framelayout)
        live_framelayout.addView(nicoNamaGameWebView.webView)
        isAddedNicoNamaGame = true
    }

    // ニコ生ゲーム削除
    fun removeNicoNamaGame() {
        live_framelayout.removeView(nicoNamaGameWebView.webView)
        // FrameLayout戻す
        live_framelayout.layoutParams.apply {
            height = surfaceViewLayoutParams.height
            width = surfaceViewLayoutParams.width
        }
        isAddedNicoNamaGame = false
    }

    private fun initDB() {
        nicoHistorySQLiteHelper = NicoHistorySQLiteHelper(context!!)
        nicoHistorySQLiteDatabase = nicoHistorySQLiteHelper.writableDatabase
        nicoHistorySQLiteHelper.setWriteAheadLoggingEnabled(false)
    }


    private fun testEnquate() {
        setEnquetePOSTLayout(
            "/vote start コロナ患者近くにいる？ はい いいえ 僕がコロナです",
            "start"
        )
        Timer().schedule(timerTask {
            commentActivity.runOnUiThread {
                setEnquetePOSTLayout(
                    "/vote showresult per 176 353 471",
                    "result"
                )
            }
        }, 5000)
    }

    /*
    * アクティブ人数を1分ごとにクリア
    * */
    private fun activeUserClear() {
        //1分でリセット
        activeTimer.schedule(10000, 60000) {
            commentActivity.runOnUiThread {
                //println(activeList)
                // 指定した時間の配列の要素を取得する
                if (::allRoomComment.isInitialized) {
                    // 一分前のUnixTime
                    val calender = Calendar.getInstance()
                    calender.add(Calendar.MINUTE, -1)
                    val unixTime = calender.timeInMillis / 1000L
                    // 今のUnixTime
                    val nowUnixTime = System.currentTimeMillis() / 1000L
                    // 範囲内のコメントを取得する
                    val timeList = allRoomComment.recyclerViewList.toList().filter { arrayList ->
                        if (arrayList != null) {
                            val commentJSONParse = CommentJSONParse(arrayList[1], "")
                            if (commentJSONParse.date.toFloatOrNull() != null) {
                                commentJSONParse.date.toLong() in unixTime..nowUnixTime
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    // 同じIDを取り除く
                    val idList =
                        timeList.distinctBy { arrayList -> CommentJSONParse(arrayList[1], "").userId }
                    activity_comment_comment_active_text.text =
                        "${idList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}"
                }
            }
        }
    }

    fun loadNGDataBase() {
        //NGデータベース
        if (!this@CommentFragment::ngListSQLiteHelper.isInitialized) {
            //データベース
            ngListSQLiteHelper = NGListSQLiteHelper(context!!)
            sqLiteDatabase = ngListSQLiteHelper.writableDatabase
            ngListSQLiteHelper.setWriteAheadLoggingEnabled(false)
        }
        setNGList("user", userNGList)
        setNGList("comment", commentNGList)
    }

    fun setNGList(name: String, list: ArrayList<String>) {
        //コメントNG読み込み
        val cursor = sqLiteDatabase.query(
            "ng_list",
            arrayOf("type", "value"),
            "type=?", arrayOf(name), null, null, null
        )
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {
            list.add(cursor.getString(1))
            cursor.moveToNext()
        }
        cursor.close()
    }

    //getplayerstatus
    fun getplayerstatus() {

        val liveId = arguments?.getString("liveId") ?: ""

        val request = Request.Builder()
            .url("https://live.nicovideo.jp/api/getplayerstatus/$liveId")   //getplayerstatus、httpsでつながる？
            .header("Cookie", "user_session=$usersession")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                val response_string = response.body?.string()
                if (response.isSuccessful) {
                    //必要なものを取り出していく
                    //add,port,thread
                    val document = Jsoup.parse(response_string)
                    // コミュ限など、視聴ができない場合はgetplayerstatusタグの属性「status」にOK以外が入るのでちぇっく。
                    if (document.getElementsByTag("getplayerstatus")[0].attr("status") == "ok") {
                        //ひつようなやつ
                        val ms = document.select("ms")
                        val user = document.select("user")
                        val room = user.select("room_label").text()
                        val seet = user.select("room_seetno").text()
                        //番組名もほしい
                        val stream = document.select("stream")
                        programTitle = stream.select("title").text()
                        //コミュニティID
                        communityID = stream.select("default_community").text()
                        //コメント投稿に必須なゆーざーID、プレミアム会員かどうか
                        userId = user.select("user_id").text()
                        premium = user.select("is_premium").text().toInt()
                        //vpos
                        programStartTime =
                            document.select("stream").select("open_time").text().toLong()
                        programLiveTime =
                            document.select("stream").select("start_time").text().toLong()
                        //経過時間計算
                        setLiveTime()
                        //サムネもほしい
                        thumbnailURL = document.getElementsByTag("thumb_url")[0].text()
                        //履歴追加
                        insertDB()
                        // 番組情報
                        commentActivity.runOnUiThread {
                            comment_fragment_program_title.text = "$programTitle - $liveId"
                            comment_fragment_program_id.text = "$room - $seet"
                        }
                    } else {
                        // エラーの原因取る。
                        val code = document.getElementsByTag("code")[0].text()
                        commentActivity.runOnUiThread {
                            Toast.makeText(
                                context,
                                "${getString(R.string.error)}\n$code",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }


    fun insertDB() {
        var type = "live"
        val unixTime = System.currentTimeMillis() / 1000
        val contentValues = ContentValues()
        contentValues.apply {
            put("service_id", liveId)
            put("user_id", communityID)
            put("title", programTitle)
            put("type", type)
            put("date", unixTime)
            put("description", "")
        }
        nicoHistorySQLiteDatabase.insert(NicoHistorySQLiteHelper.TABLE_NAME, null, contentValues)
    }

    //経過時間計算
    private fun setLiveTime() {
        //1秒ごとに
        programTimer = Timer()
        programTimer.schedule(0, 1000) {

            val unixtime = System.currentTimeMillis() / 1000L

            val calc = unixtime - programLiveTime

            //分、秒だけCalendar使う
            //val cale = Calendar.getInstance()
            //cale.timeInMillis = calc * 1000L

            val date = Date(calc * 1000L)

            //時間はUNIX時間から計算する
            val hour = (calc / 60 / 60)

            val simpleDateFormat = SimpleDateFormat("mm:ss")


            commentActivity.runOnUiThread {
                if (activity_comment_comment_time != null) {
                    activity_comment_comment_time.text =
                        "$hour:" + simpleDateFormat.format(date.time)
                }
            }
        }

    }

    //ニコ生の視聴用の情報を流してくれるWebSocketに接続する
//コメントに投稿したり、HLSのアドレスはWebSocketから取得する必要がある。
//で、WebSocketのアドレスはHTMLを解析する必要がある！？！？！？
    fun getNicoLiveWebPage(): Deferred<Response> = GlobalScope.async {
        //番組ID
        val id = arguments?.getString("liveId") ?: ""

        //コメビュモードの場合はユーザーセッション無いので
        val request = if (isWatchingMode) {
            //視聴モード（ユーザーセッション付き）
            Request.Builder()
                .url("https://live2.nicovideo.jp/watch/${id}")
                .header("User-Agent", "TatimiDroid;@takusan_23")
                .header("Cookie", "user_session=${usersession}")
                .get()
                .build()
        } else {
            //コメビュモード（ユーザーセッションなし）
            Request.Builder()
                .url("https://live2.nicovideo.jp/watch/${id}")
                .header("User-Agent", "TatimiDroid;@takusan_23")
                .get()
                .build()
        }

        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    // HTMLから必要な情報を取得する
    fun parseNicoLiveHTML(responseHTML: String?) {
        //HTML解析
        val html = Jsoup.parse(responseHTML)
        //謎のJSON取得
        //この部分長すぎてChromeだとうまくコピーできないんだけど、Edgeだと完璧にコピーできたぞ！
        //F12押してConsole選んで以下のJavaScriptを実行すれば取れます。
        //console.log(document.getElementById('embedded-data').getAttribute('data-props'))
        if (html.getElementById("embedded-data") != null) {
            val json = html.getElementById("embedded-data").attr("data-props")
            val jsonObject = JSONObject(json)
            val site = jsonObject.getJSONObject("site")
            val relive = site.getJSONObject("relive")
            //WebSocketリンク
            val websocketUrl = relive.getString("webSocketUrl")
            //番組情報
            val program = jsonObject.getJSONObject("program")
            //公式番組かどうか
            val providerType = program.getString("providerType")
            isOfficial = providerType == "official"
            //broadcastId
            val broadcastId = program.getString("broadcastId")
            connectionNicoLiveWebSocket(websocketUrl, broadcastId)
        }
    }

    //ニコ生の視聴に必要なデータを流してくれるWebSocket
    //視聴セッションWebSocket
    fun connectionNicoLiveWebSocket(url: String, broadcastId: String) {
        val uri = URI(url)
        connectionNicoLiveWebSocket = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // System.out.println("ニコ生視聴セッションWebSocket接続開始 $liveId")
                //最初にクライアント側からサーバーにメッセージを送る必要がある
                val jsonObject = JSONObject()
                jsonObject.put("type", "watch")
                //body
                val bodyObject = JSONObject()
                bodyObject.put("command", "getpermit")
                //requirement
                val requirementObject = JSONObject()
                requirementObject.put("broadcastId", broadcastId)
                requirementObject.put("route", "")
                //stream
                val streamObject = JSONObject()
                streamObject.put("protocol", "hls")
                streamObject.put("requireNewStream", true)
                streamObject.put("priorStreamQuality", "high")
                streamObject.put("isLowLatency", isLowLatency)
                streamObject.put("isChasePlay", false)
                //room
                val roomObject = JSONObject()
                roomObject.put("isCommentable", true)
                roomObject.put("protocol", "webSocket")

                requirementObject.put("stream", streamObject)
                requirementObject.put("room", roomObject)

                bodyObject.put("requirement", requirementObject)
                jsonObject.put("body", bodyObject)

                //もう一個
                val secondObject = JSONObject()
                secondObject.put("type", "watch")
                val secondbodyObject = JSONObject()
                secondbodyObject.put("command", "playerversion")
                val paramsArray = JSONArray()
                paramsArray.put("leo")
                secondbodyObject.put("params", paramsArray)
                secondObject.put("body", secondbodyObject)

                /*    println(
                        """

                        JSON情報
                        $liveId------
                        ${jsonObject.toString()}
                        ------
                        $secondObject
                        ----

                    """.trimIndent()
                    )*/

                //送信
                this.send(secondObject.toString())
                this.send(jsonObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                // System.out.println("ニコ生視聴セッション終了：$liveId")
            }

            override fun onMessage(message: String?) {
                // println("$message $liveId $url")
                //HLSのアドレス　と　変更可能な画質一覧取る
                if (message?.contains("currentStream") == true) {
                    //System.out.println("HLSアドレス：$liveId")
                    val jsonObject = JSONObject(message)
                    val currentObject =
                        jsonObject.getJSONObject("body").getJSONObject("currentStream")
                    hls_address = currentObject.getString("uri")
                    //生放送再生
                    if (watchLive) {
                        //モバイルデータは最低画質で読み込む設定　
                        sendMobileDataQuality()
                        setPlayVideoView()
                    } else {
                        //レイアウト消す
                        live_framelayout.visibility = View.GONE
                    }
                    //画質変更一覧
                    val qualityTypesJSONArray = currentObject.getJSONArray("qualityTypes")
                    val selectQuality = currentObject.getString("quality")
                    // 画質変更BottomFragmentに詰める
                    val bundle = Bundle()
                    bundle.putString("select_quality", selectQuality)
                    bundle.putString("quality_list", qualityTypesJSONArray.toString())
                    bundle.putString("liveId", liveId)
                    qualitySelectBottomSheet = QualitySelectBottomSheet()
                    qualitySelectBottomSheet.arguments = bundle
                    //画質変更成功？
                    if (start_quality.isEmpty()) {
                        //最初の読み込みは入れるだけ
                        start_quality = selectQuality
                    } else {
                        //画質変更した。Snackbarでユーザーに教える
                        val oldQuality = QualitySelectBottomSheet.getQualityText(
                            start_quality,
                            context!!
                        )
                        val newQuality = QualitySelectBottomSheet.getQualityText(
                            selectQuality,
                            context!!
                        )
                        Snackbar.make(
                            live_surface_view,
                            "${getString(R.string.successful_quality)}\n${oldQuality}→${newQuality}",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        start_quality = selectQuality
                    }

                    //GoogleCast
                    commentActivity.runOnUiThread {
                        googleCast.apply {
                            programTitle = this@CommentFragment.programTitle
                            programSubTitle = this@CommentFragment.liveId
                            programThumbnail = this@CommentFragment.thumbnailURL
                            hlsAddress = this@CommentFragment.hls_address
                            resume()
                        }
                    }

                }

                //threadId、WebSocketURL受信
                //コメント投稿時に使う。
                if (message?.contains("messageServerUri") == true) {
                    val jsonObject = JSONObject(message)
                    // もし放送者の場合はWebSocketに部屋一覧が流れてくるので阻止。
                    if (jsonObject.getJSONObject("body").has("room")) {
                        val room = jsonObject.getJSONObject("body").getJSONObject("room")
                        val threadId = room.getString("threadId")
                        val messageServerUri = room.getString("messageServerUri")
                        val roomName = room.getString("roomName")

                        //コメント投稿時に必要なpostKeyを取得するために使う
                        getPostKeyThreadId = threadId

                        // System.out.println("コメントWebSocket情報 ${threadId} ${messageServerUri}")

                        //コメント投稿時に使うWebSocketに接続する
                        connectionCommentPOSTWebSocket(messageServerUri, threadId)

                        commentMessageServerUri = messageServerUri
                        commentThreadId = threadId
                        commentRoomName = roomName

                        // 公式番組のとき
                        if (isOfficial) {
                            // ViewPager
                            commentActivity.runOnUiThread {
                                // WebSocketで流れてきたアドレスへ接続する
                                allRoomComment.connectCommentServer(commentMessageServerUri, commentThreadId, commentRoomName)
                            }
                        }


                    }
                }


                //postKey受信
                //今回は受信してpostKeyが取得できたらコメントを送信する仕様にします。
                //postKeyをもらう イコール　コメントを送信する
                if (message?.contains("postkey") == true) {
                    val jsonObject = JSONObject(message)
                    val command = jsonObject.getJSONObject("body").getString("command")
                    //コメント送信なので２重チェック
                    if (command.contains("postkey")) {

                        //JSON配列の０番目にpostkeyが入ってる。
                        val paramsArray =
                            jsonObject.getJSONObject("body").getJSONArray("params")
                        val postkey = paramsArray.getString(0)
                        //コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
                        // 100=1秒らしい。 例：300->3秒
                        val unixTime = System.currentTimeMillis() / 1000L
                        val vpos = (unixTime - programStartTime) * 100
                        // println(vpos)
                        val jsonObject = JSONObject()
                        val chatObject = JSONObject()
                        chatObject.put(
                            "thread",
                            getPostKeyThreadId
                        )    //視聴用セッションWebSocketからとれる
                        chatObject.put(
                            "vpos",
                            vpos
                        )     //番組情報取得で取得した値 - = System.currentTimeMillis() UNIX時間
                        // 匿名が有効の場合は184をつける
                        if (isTokumeiComment) {
                            commentCommand = "184 $commentCommand"
                        }
                        chatObject.put("mail", commentCommand)
                        chatObject.put(
                            "ticket",
                            commentTicket
                        )     //視聴用セッションWebSocketからとれるコメントが流れるWebSocketに接続して最初に必要な情報を送ったら取得できる
                        chatObject.put("content", commentValue)
                        chatObject.put("postkey", postkey)
                        //この２つ、ユーザーIDとプレミアム会員かどうか。これどうやってとる？gatPlayerStatus？　もしgetPlayerStatusなくなってもHTML解析すればとれそう？
                        chatObject.put("premium", premium)
                        chatObject.put("user_id", userId)
                        jsonObject.put("chat", chatObject)

                        //System.out.println(jsonObject.toString())

                        //送信
                        //println(jsonObject.toString())
                        commentPOSTWebSocketClient.send(jsonObject.toString())
                    }
                }

                //総来場者数、コメント数
                if (message?.contains("statistics") == true) {
                    val jsonObject = JSONObject(message)
                    val params = jsonObject.getJSONObject("body").getJSONArray("params")
                    val watchCount = params[0]
                    val commentCount = params[1]
                    commentActivity.runOnUiThread {
                        if (activity_comment_watch_count != null && activity_comment_comment_count != null) {
                            activity_comment_watch_count.text = watchCount.toString()
                            activity_comment_comment_count.text = commentCount.toString()
                        }
                    }
                }

                //延長を検知
                if (message?.contains("schedule") == true) {
                    //時間出す場所確保したので終了時刻書く。
                    val jsonObject = JSONObject(message)
                    val endTime =
                        jsonObject.getJSONObject("body").getJSONObject("update")
                            .getLong("endtime")
                    val beginTime = jsonObject.getJSONObject("body").getJSONObject("update")
                        .getLong("begintime")
                    if (isEntyouKenti) {
                        //終了時刻計算
                        val simpleDateFormat = SimpleDateFormat("MM/dd HH:mm:ss")
                        val date = Date(endTime)
                        val time = simpleDateFormat.format(date)
                        val message =
                            "${getString(R.string.entyou_message)}\n${getString(R.string.end_time)} $time"
                        commentActivity.runOnUiThread {
                            Snackbar.make(live_surface_view, message, Snackbar.LENGTH_LONG)
                                .apply {
                                    anchorView = getSnackbarAnchorView()
                                    //複数行へ
                                    val textview = view.findViewById<TextView>(R.id.snackbar_text)
                                    textview.isSingleLine = false
                                    show()
                                }
                        }

                    } else {
                        isEntyouKenti = true
                    }

                    //延長したら残り時間再計算する
                    //割り算！
                    val calc = (endTime - beginTime) / 1000
                    //時間/分
                    val hour = calc / 3600
                    var hourString = hour.toString()
                    if (hourString.length == 1) {
                        hourString = "0$hourString"
                    }
                    val minute = calc % 3600 / 60
                    var minuteString = minute.toString()
                    if (minuteString.length == 1) {
                        minuteString = "0$minuteString"
                    }
                    commentActivity.runOnUiThread {
                        activity_comment_comment_end_time?.text =
                            "${hourString}:${minuteString}:00"
                    }

                    //番組終了時刻を入れる
                    programEndUnixTime = endTime / 1000

                }

                // 自動終了
                if (message?.contains("disconnect") == true) {
                    val jsonObject = JSONObject(message)
                    val command = jsonObject.getJSONObject("body").getString("command")
                    if (command == "disconnect") {
                        // 終了メッセージ
                        if (pref_setting.getBoolean("setting_disconnect_activity_finish", false)) {
                            if (activity is CommentActivity) {
                                // Activity が CommentActivity なら消す。二窓Activityは動かないように
                                activity?.finish()
                            }
                        }
                    }
                }

            }

            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
                System.out.println("ニコ生視聴セッションWebSocket　えらー")
            }
        }

        //それと別に30秒間隔で視聴を続けてますよメッセージを送信する必要がある模様
        timer = Timer()
        timer.schedule(30000, 30000) {
            if (!connectionNicoLiveWebSocket.isClosed) {
                //ひとつめ
                val jsonObject = JSONObject()
                jsonObject.put("type", "watch")

                val bodyJSONObject = JSONObject()
                bodyJSONObject.put("command", "watching")

                val paramsArray = JSONArray()
                paramsArray.put(broadcastId)
                paramsArray.put("-1")
                paramsArray.put("0")

                bodyJSONObject.put("params", paramsArray)
                jsonObject.put("body", bodyJSONObject)

                //ふたつめ
                val secondJSONObject = JSONObject()
                secondJSONObject.put("type", "pong")
                secondJSONObject.put("body", JSONObject())

                //それぞれを３０秒ごとに送る
                //なお他の環境と同時に視聴すると片方切断される（片方の画面に同じ番組を開くとだめ的なメッセージ出る）
                connectionNicoLiveWebSocket.send(jsonObject.toString())
                connectionNicoLiveWebSocket.send(secondJSONObject.toString())
                //System.out.println(jsonObject.toString())
            }
        }
        //接続
        connectionNicoLiveWebSocket.connect()
    }

    //コメント送信用WebSocket。今の部屋に繋がってる（アリーナならアリーナ）
    fun connectionCommentPOSTWebSocket(url: String, threadId: String) {
        //過去コメントか流れてきたコメントか
        var historyComment = 0
        // 過去コメント0にしたので常に
        //
        val uri = URI(url)
        //これはプロトコルの設定が必要
        val protocol = Draft_6455(
            Collections.emptyList(),
            Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?
        )
        commentPOSTWebSocketClient = object : WebSocketClient(uri, protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                //System.out.println("コメント送信用WebSocket接続開始")

                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", threadId)
                //jsonObject.put("service", "LIVE")
                jsonObject.put("score", 1)
                jsonObject.put("nicoru", 0)
                jsonObject.put("with_global", 1)
                jsonObject.put("fork", 0)
                jsonObject.put("user_id", userId)
                jsonObject.put("res_from", historyComment)
                sendJSONObject.put("thread", jsonObject)
                commentPOSTWebSocketClient.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                //コメント送信に使うticketを取得する
                if (message != null) {
                    // JSONぱーす
                    val commentJSONParse = CommentJSONParse(message, getString(R.string.arena))

                    //thread
                    if (message.contains("ticket")) {
                        val jsonObject = JSONObject(message)
                        val thread = jsonObject.getJSONObject("thread")
                        commentTicket = thread.getString("ticket")
                    }
                    //chat_result
                    //コメント送信できたか
                    if (message.contains("chat_result")) {
                        val jsonObject = JSONObject(message)
                        val status =
                            jsonObject.getJSONObject("chat_result").getString("status")
                        commentActivity.runOnUiThread {
                            if (status.toInt() == 0) {
                                Snackbar.make(
                                        fab,
                                        getString(R.string.comment_post_success),
                                        Snackbar.LENGTH_SHORT
                                    )
                                    .setAnchorView(getSnackbarAnchorView())
                                    .show()
                            } else {
                                Snackbar.make(
                                        fab,
                                        "${getString(R.string.comment_post_error)}：${status}",
                                        Snackbar.LENGTH_SHORT
                                    )
                                    .setAnchorView(getSnackbarAnchorView()).show()
                            }
                        }
                    }

                    // /voteが流れてきたとき（アンケート）
                    // だたし過去コメントには反応しないようにする
                    if (message.contains("/vote")) {
                        //コメント取得
                        val jsonObject = JSONObject(message)
                        val chatObject = jsonObject.getJSONObject("chat")
                        val content = chatObject.getString("content")
                        val premium = chatObject.getInt("premium")
                        if (premium == 3) {
                            println(content)
                            //運営コメント
                            //アンケ開始
                            if (content.contains("/vote start")) {
                                commentActivity.runOnUiThread {
                                    setEnquetePOSTLayout(content, "start")
                                }
                            }
                            //アンケ結果
                            if (content.contains("/vote showresult")) {
                                commentActivity.runOnUiThread {
                                    setEnquetePOSTLayout(content, "showresult")
                                }
                            }
                            //アンケ終了
                            if (content.contains("/vote stop")) {
                                commentActivity.runOnUiThread {
                                    if (this@CommentFragment::enquateView.isInitialized) {
                                        comment_fragment_enquate_framelayout.removeView(enquateView)
                                    }
                                }
                            }
                        }
                    }

                    //disconnectを検知
                    if (commentJSONParse.comment.contains("/disconnect")) {
                        if (commentJSONParse.premium.contains("運営")) {
                            //自動次枠移動が有効なら使う
                            if (isAutoNextProgram) {
                                checkNextProgram()
                                Snackbar.make(
                                    live_video_view,
                                    context?.getString(R.string.next_program_message) ?: "",
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            } else {
                                //終了メッセージ
                                Snackbar.make(
                                    live_video_view,
                                    context?.getString(R.string.program_disconnect) ?: "",
                                    Snackbar.LENGTH_SHORT
                                ).setAction(context?.getString(R.string.end)) {
                                    //終了
                                    if (activity !is NimadoActivity) {
                                        //二窓Activity以外では終了できるようにする。
                                        activity?.finish()
                                    }
                                }.setAnchorView(getSnackbarAnchorView()).show()
                            }
                        }
                    }

                    //運営コメント、Infoコメント　表示・非表示
                    if (!hideInfoUnnkome) {
                        //運営コメント
                        if (commentJSONParse.premium == "生主" || commentJSONParse.premium == "運営") {
                            val isNicoad = commentJSONParse.comment.contains("/nicoad")
                            val isInfo = commentJSONParse.comment.contains("/info")
                            val isUadPoint = commentJSONParse.comment.contains("/uadpoint")
                            val isSpi = commentJSONParse.comment.contains("/spi")
                            val isGift = commentJSONParse.comment.contains("/gift")
                            // 上に表示されるやつ
                            if (!isNicoad && !isInfo && !isUadPoint && !isSpi && !isGift) {
                                // 生主コメント
                                setUnneiComment(commentJSONParse.comment)
                            } else {
                                // UIスレッド
                                activity?.runOnUiThread {
                                    // 下に表示するやつ
                                    if (isInfo) {
                                        // /info {数字}　を消す
                                        val regex = "/info \\d+ ".toRegex()
                                        showInfoComment(commentJSONParse.comment.replace(regex, ""))
                                    }
                                    // ニコニ広告
                                    if (isNicoad) {
                                        val json =
                                            JSONObject(commentJSONParse.comment.replace("/nicoad ", ""))
                                        val comment = json.getString("message")
                                        showInfoComment(comment)
                                    }
                                    // spi (ニコニコ新市場に商品が貼られたとき)
                                    if (isSpi) {
                                        showInfoComment(commentJSONParse.comment.replace("/spi ", ""))
                                    }
                                    // 投げ銭
                                    if (isGift) {
                                        // スペース区切り配列
                                        val list = commentJSONParse.comment.replace("/gift ", "")
                                            .split(" ")
                                        val userName = list[2]
                                        val giftPoint = list[3]
                                        val giftName = list[5]
                                        val message =
                                            "${userName} さんが ${giftName} （${giftPoint} pt）をプレゼントしました。"
                                        showInfoComment(message)
                                    }
                                }
                            }
                        }
                        //運営コメントけす
                        if (commentJSONParse.comment.contains("/clear")) {
                            removeUnneiComment()
                        }

/*
                        //infoコメントを表示
                        if (commentJSONParse.comment.contains("/nicoad")) {
                            activity?.runOnUiThread {
                                val json =
                                    JSONObject(
                                        commentJSONParse.comment.replace(
                                            "/nicoad ",
                                            ""
                                        )
                                    )
                                val comment = json.getString("message")
                                showInfoComment(comment)
                            }
                        }
                        if (commentJSONParse.comment.contains("/info")) {
                            activity?.runOnUiThread {
                                showInfoComment(
                                    commentJSONParse.comment.replace(
                                        "/info \\d+ ".toRegex(), // /info {数字}　を消す
                                        ""
                                    )
                                )
                            }
                        }
*/
                    }
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        //忘れがちな接続
        commentPOSTWebSocketClient.connect()
    }

    //16:9で横の大きさがわかるときに縦の大きさを返す
    fun getAspectHeightFromWidth(width: Int): Int {
        val heightCalc = width / 16
        return heightCalc * 9
    }

    //視聴モード
    fun setPlayVideoView() {
        if (context == null) {
            return
        }
        //設定で読み込むかどうか
        commentActivity.runOnUiThread {
            live_surface_view.visibility = View.VISIBLE
            // println("生放送再生：HLSアドレス : $hls_address")

            //ウィンドウの半分ぐらいの大きさに設定
            val display = commentActivity.windowManager.defaultDisplay
            val point = Point()
            display.getSize(point)

            val frameLayoutParams = liveFrameLayout.layoutParams

            //横画面のときの対応
            if (commentActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                //二窓モードのときはとりあえず更に小さくしておく
                if (isNimadoMode) {
                    frameLayoutParams.width = point.x / 2
                    frameLayoutParams.height = getAspectHeightFromWidth(point.x / 4)
                } else {
                    //16:9の9を計算
                    frameLayoutParams.height = getAspectHeightFromWidth(point.x / 2)
                }
                liveFrameLayout.layoutParams = frameLayoutParams
            } else {
                //縦画面
                frameLayoutParams.width = point.x
                //二窓モードのときは小さくしておく
                if (isNimadoMode) {
                    frameLayoutParams.width = point.x / 2
                }
                //16:9の9を計算
                frameLayoutParams.height = getAspectHeightFromWidth(frameLayoutParams.width)
                liveFrameLayout.layoutParams = frameLayoutParams
            }

            exoPlayer = SimpleExoPlayer.Builder(context!!).build()
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
                .createMediaSource(hls_address.toUri());

            //再生準備
            exoPlayer.prepare(hlsMediaSource)
            //SurfaceViewセット
            exoPlayer.setVideoSurfaceView(live_surface_view)
            //再生
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.EventListener {
                override fun onPlayerError(error: ExoPlaybackException) {
                    super.onPlayerError(error)
                    error.printStackTrace()
                    println("生放送の再生が止まりました。")
                    //再接続する？
                    //それからニコ生視聴セッションWebSocketが切断されてなければ
                    if (!connectionNicoLiveWebSocket.isClosed) {
                        println("再度再生準備を行います")
                        activity?.runOnUiThread {
                            //再生準備
                            exoPlayer.prepare(hlsMediaSource)
                            //SurfaceViewセット
                            exoPlayer.setVideoSurfaceView(live_surface_view)
                            //再生
                            exoPlayer.playWhenReady = true
                            Snackbar.make(
                                fab,
                                getString(R.string.error_player),
                                Snackbar.LENGTH_SHORT
                            ).setAnchorView(getSnackbarAnchorView()).show()
                        }
                    }
                }
            })

            //新しいバックグラウンド再生。バッググラウンドで常にExoPlayerを動かして離れた瞬間に再生をする。
            if (pref_setting.getBoolean("setting_leave_background", false)) {
                if (pref_setting.getBoolean("setting_leave_background_v2", false)) {
                    backgroundPlay.play(hls_address.toUri())
                }
            }

            // 16:9のLayoutParams
            val height = liveFrameLayout.layoutParams.height
            val width = liveFrameLayout.layoutParams.width
            surfaceViewLayoutParams = FrameLayout.LayoutParams(width, height)

        }
    }

    fun isExoPlayerInitialized(): Boolean {
        return this@CommentFragment::exoPlayer.isInitialized
    }

    override fun onStop() {
        super.onStop()
        if (this@CommentFragment::exoPlayer.isInitialized) {
            exoPlayer.release()
            // println("ExoPlayerリリース")
        }
    }

    override fun onResume() {
        super.onResume()
        //// WebView（ニコ生ゲーム用）リロード
        //activity?.runOnUiThread {
        //    if (::nicoNamaGameWebView.isInitialized) {
        //        nicoNamaGameWebView.reload()
        //    }
        //}

        if (this@CommentFragment::exoPlayer.isInitialized) {
            //exoPlayer.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()
        googleCast.pause()
    }

    //コメント投稿用
    fun sendComment(comment: String, command: String) {
        if (comment != "\n") {
            commentValue = comment
            commentCommand = command
            //コメント投稿モード（コメントWebSocketに送信）
            // val watchmode = pref_setting.getBoolean("setting_watching_mode", false)
            //nicocas式コメント投稿モード
            // val nicocasmode = pref_setting.getBoolean("setting_nicocas_mode", false)
            if (isWatchingMode) {
                //postKeyを視聴用セッションWebSocketに払い出してもらう
                //PC版ニコ生だとコメントを投稿のたびに取得してるので
                val postKeyObject = JSONObject()
                postKeyObject.put("type", "watch")
                val bodyObject = JSONObject()
                bodyObject.put("command", "getpostkey")
                val paramsArray = JSONArray()
                paramsArray.put(getPostKeyThreadId)
                bodyObject.put("params", paramsArray)
                postKeyObject.put("body", bodyObject)
                //送信する
                //この後の処理は視聴用セッションWebSocketでpostKeyを受け取る処理に行きます。
                connectionNicoLiveWebSocket.send(postKeyObject.toString())
            } else if (isNicocasMode) {
                //コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
                // 100=1秒らしい。 例：300->3秒
                val unixTime = System.currentTimeMillis() / 1000L
                val vpos = (unixTime - programStartTime) * 100
                val jsonObject = JSONObject()
                jsonObject.put("message", commentValue)
                if (isTokumeiComment) {
                    commentCommand = "184 $commentCommand"
                }
                jsonObject.put("command", commentCommand)
                jsonObject.put("vpos", vpos.toString())

                val requestBodyJSON = jsonObject.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                //println(jsonObject.toString())

                val request = Request.Builder()
                    .url("https://api.cas.nicovideo.jp/v1/services/live/programs/${liveId}/comments")
                    .header("Cookie", "user_session=${usersession}")
                    .post(requestBodyJSON)
                    .build()
                val okHttpClient = OkHttpClient()
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        showToast("${getString(R.string.error)}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        //System.out.println(response.body?.string())
                        if (response.isSuccessful) {
                            //成功
                            val snackbar =
                                Snackbar.make(
                                    fab,
                                    getString(R.string.comment_post_success),
                                    Snackbar.LENGTH_SHORT
                                )
                            snackbar.anchorView = getSnackbarAnchorView()
                            snackbar.show()
                        } else {
                            showToast("${getString(R.string.error)}\n${response.code}")
                        }
                    }
                })
            }
        }
    }

    fun showToast(message: String) {
        commentActivity.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    fun showBubbles() {
        //Android Q以降で利用可能
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(context, FloatingCommentViewer::class.java)
            intent.putExtra("liveId", liveId)
            val bubbleIntent =
                PendingIntent.getActivity(
                    context,
                    25,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            //通知作成？
            val bubbleData = Notification.BubbleMetadata.Builder()
                .setDesiredHeight(600)
                .setIcon(Icon.createWithResource(context, R.drawable.ic_library_books_24px))
                .setIntent(bubbleIntent)
                .build()
            val timelineBot = Person.Builder()
                .setBot(true)
                .setName(getString(R.string.floating_comment_viewer))
                .setImportant(true)
                .build()

            //通知送信
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            //通知チャンネル作成
            val notificationId = "floating_comment_viewer"
            if (notificationManager.getNotificationChannel(notificationId) == null) {
                //作成
                val notificationChannel = NotificationChannel(
                    notificationId, getString(R.string.floating_comment_viewer),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }
            //通知作成
            val notification = Notification.Builder(context, notificationId)
                .setContentText(getString(R.string.floating_comment_viewer_description))
                .setContentTitle(getString(R.string.floating_comment_viewer))
                .setSmallIcon(R.drawable.ic_library_books_24px)
                .setBubbleMetadata(bubbleData)
                .addPerson(timelineBot)
                .build()
            //送信
            notificationManager.notify(5, notification)
        } else {
            //Android Pieなので..
            Toast.makeText(
                context,
                getString(R.string.floating_comment_viewer_version),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    fun setLandscapePortrait() {
        val conf = resources.configuration
        //live_video_view.stopPlayback()
        when (conf.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                commentActivity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                commentActivity.requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    fun copyProgramId() {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
        //コピーしました！
        Toast.makeText(
                context,
                "${getString(R.string.copy_program_id)} : $liveId",
                Toast.LENGTH_SHORT
            )
            .show()
    }

    fun copyCommunityId() {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", communityID))
        //コピーしました！
        Toast.makeText(
                context,
                "${getString(R.string.copy_communityid)} : $communityID",
                Toast.LENGTH_SHORT
            )
            .show()
    }

    fun setSound(volume: Int) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        //println(requestCode)
        when (requestCode) {
            114 -> {
                if (resultCode == PackageManager.PERMISSION_GRANTED) {
                    //権限ゲット！YATTA!
                    //ポップアップ再生
                    startOverlayPlayer()
                    Toast.makeText(context, "権限を取得しました。", Toast.LENGTH_SHORT).show()
                } else {
                    //何もできない。
                    Toast.makeText(context, "権限取得に失敗しました。", Toast.LENGTH_SHORT).show()
                }
            }
            ProgramShare.requestCode -> {
                //画像共有
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        val uri: Uri = data.data!!
                        //保存＆共有画面表示
                        programShare.saveActivityResult(uri)
                    }
                }
            }
        }
    }

    fun destroyCode() {
        if (this@CommentFragment::commentPOSTWebSocketClient.isInitialized) {
            connectionNicoLiveWebSocket.close()
            commentPOSTWebSocketClient.close()
        }
        timer.cancel()
        programTimer.cancel()
        activeTimer.cancel()
        //バックグラウンド再生止める
        backgroundPlay.release()
        //ポップアップ再生とめる
        if (popUpPlayer.isPopupPlay) {
            popUpPlayer.destroy()
        }
        if (this@CommentFragment::broadcastReceiver.isInitialized) {
            //中野ブロードキャスト終了
            commentActivity.unregisterReceiver(broadcastReceiver)
        }
        autoNextProgramTimer.cancel()
        //センサーによる画面回転が有効になってる場合は最後に
        if (this@CommentFragment::rotationSensor.isInitialized) {
            rotationSensor.destroy()
        }
        //止める
        if (this@CommentFragment::exoPlayer.isInitialized) {
            exoPlayer.apply {
                playWhenReady = false
                stop()
                seekTo(0)
                release()
            }
        }
        if (::allRoomComment.isInitialized) {
            allRoomComment.destory()
        }
        // println("とじます")
    }

    //Activity終了時に閉じる
    override fun onDestroy() {
        super.onDestroy()
        destroyCode()
    }

    /*オーバーレイ*/
    fun startOverlayPlayer() {
        popUpPlayer.showPopUpView(hls_address)
    }

    /*バックグラウンド再生*/
    fun setBackgroundProgramPlay() {
        //再生。
        backgroundPlay.start(hls_address.toUri())
        //Nougatと分岐
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "program_background"
            val notificationChannel = NotificationChannel(
                notificationChannelId, getString(R.string.notification_background_play),
                NotificationManager.IMPORTANCE_HIGH
            )

            //通知チャンネル登録
            if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            //通知作成
            backgroundPlayNotification(
                getString(R.string.background_play_pause),
                NotificationCompat.FLAG_ONGOING_EVENT
            )
        } else {
            //通知作成
            backgroundPlayNotification(
                getString(R.string.background_play_pause),
                NotificationCompat.FLAG_ONGOING_EVENT
            )
        }
    }


    fun backgroundPlayNotification(pausePlayString: String, flag: Int) {
        //音楽コントロールブロードキャスト
        val stopIntent = Intent("background_program_stop")
        val pauseIntent = Intent("background_program_pause")


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannelId = "program_background"
            val programNotification =
                NotificationCompat.Builder(context!!, notificationChannelId)
                    .setContentTitle(programTitle)
                    .setContentText(liveId)
                    .setSmallIcon(R.drawable.ic_background_icon)
                    .addAction(
                        NotificationCompat.Action(
                            R.drawable.ic_outline_stop_24px,
                            pausePlayString,
                            PendingIntent.getBroadcast(
                                context,
                                12,
                                pauseIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                    )
                    .addAction(
                        NotificationCompat.Action(
                            R.drawable.ic_clear_black,
                            getString(R.string.background_play_finish),
                            PendingIntent.getBroadcast(
                                context,
                                12,
                                stopIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                    )
                    .build()
            //消せないようにする
            programNotification.flags = flag

            notificationManager.notify(backgroundNotificationID, programNotification)
        } else {
            val programNotification = NotificationCompat.Builder(context!!)
                .setContentTitle(programTitle)
                .setContentText(liveId)
                .setSmallIcon(R.drawable.ic_background_icon)
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        pausePlayString,
                        PendingIntent.getBroadcast(
                            context,
                            12,
                            pauseIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.ic_outline_stop_24px,
                        getString(R.string.background_play_finish),
                        PendingIntent.getBroadcast(
                            context,
                            12,
                            stopIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                ).build()

            //消せないようにする
            programNotification.flags = flag

            notificationManager.notify(backgroundNotificationID, programNotification)
        }
    }

    //Activity復帰した時に呼ばれる
    override fun onStart() {
        super.onStart()
        //アプリ戻ってきたらバックグラウンド再生、ポップアップ再生を止める。
        val windowManager =
            context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        //バックグラウンド再生から戻ってきた時
        if (backgroundPlay.isPlaying) {
            notificationManager.cancel(backgroundNotificationID) //通知削除
            Toast.makeText(
                context!!,
                getString(R.string.lunch_app_close_background),
                Toast.LENGTH_SHORT
            ).show()
            if (pref_setting.getBoolean("setting_leave_background_v2", false)) {
                //新しいバッググラウンド再生。
                backgroundPlay.pause()
            } else {
                backgroundPlay.release()
            }
        }

        //ポップアップ再生止める
        //ポップアップ再生のExoPlayerも止める
        if (popUpPlayer.isPopupPlay) {
            popUpPlayer.destroy()
            Toast.makeText(
                context,
                getString(R.string.lunch_app_close_popup),
                Toast.LENGTH_SHORT
            ).show()
        }
        //再生部分を作り直す
        if (hls_address.isNotEmpty()) {
            live_framelayout.visibility = View.VISIBLE
            setPlayVideoView()
        }
    }


/*
    //ホームボタンおした
    override fun onUserLeaveHint() {
        //別アプリを開いた時の処理
        if (pref_setting.getBoolean("setting_leave_background", false)) {
            //バックグラウンド再生
            setBackgroundProgramPlay()
        }
        if (pref_setting.getBoolean("setting_leave_popup", false)) {
            //ポップアップ再生
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(context)) {
                    //RuntimePermissionに対応させる
                    // 権限取得
                    val intent =
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context?.packageName}")
                        )
                    this@CommentFragment.startActivityForResult(intent, 114)
                } else {
                    startOverlayPlayer()
                }
            } else {
                //ろりぽっぷ
                startOverlayPlayer()
            }
        }
    }
*/

    fun setEnquetePOSTLayout(message: String, type: String) {
        enquateView =
            layoutInflater.inflate(R.layout.bottom_fragment_enquate_layout, null, false)
        if (type.contains("start")) {
            //アンケ開始
            comment_fragment_enquate_framelayout.removeAllViews()
            comment_fragment_enquate_framelayout.addView(enquateView)
            // /vote start ～なんとか　を配列にする
            val voteString = message.replace("/vote start ", "")
            val voteList = voteString.split(" ") // 空白の部分で分けて配列にする
            val jsonArray = JSONArray(voteList)
            //println(enquateStartMessageToJSONArray(message))
            //アンケ内容保存
            enquateJSONArray = jsonArray.toString()

            //０個目はタイトル
            val title = jsonArray[0]
            enquateView.enquate_title.text = title.toString()

            //１個めから質問
            for (i in 0 until jsonArray.length()) {
                //println(i)
                val button = MaterialButton(context!!)
                button.text = jsonArray.getString(i)
                button.setOnClickListener {
                    //投票
                    //enquatePOST(i - 1)
                    //アンケ画面消す
                    comment_fragment_enquate_framelayout.removeAllViews()
                    //Snackbar
                    Snackbar.make(
                        liveFrameLayout,
                        getString(R.string.enquate) + " : " + jsonArray[i].toString(),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.weight = 1F
                layoutParams.setMargins(10, 10, 10, 10)
                button.layoutParams = layoutParams
                //1～3は一段目
                if (i in 1..3) {
                    enquateView.enquate_linearlayout_1.addView(button)
                }
                //4～6は一段目
                if (i in 4..6) {
                    enquateView.enquate_linearlayout_2.addView(button)
                }
                //7～9は一段目
                if (i in 7..9) {
                    enquateView.enquate_linearlayout_3.addView(button)
                }
            }
        } else if(enquateJSONArray.isNotEmpty()) {
            //println(enquateJSONArray)
            //アンケ結果
            comment_fragment_enquate_framelayout.removeAllViews()
            comment_fragment_enquate_framelayout.addView(enquateView)
            // /vote showresult ~なんとか を　配列にする
            val voteString = message.replace("/vote showresult per ", "")
            val voteList = voteString.split(" ")
            val jsonArray = JSONArray(voteList)
            val questionJsonArray = JSONArray(enquateJSONArray)
            //０個目はタイトル
            val title = questionJsonArray.getString(0)
            enquateView.enquate_title.text = title
            //共有で使う文字
            var shareText = ""
            //結果は０個めから
            for (i in 0 until jsonArray.length()) {
                val result = jsonArray.getString(i)
                val question = questionJsonArray.getString(i + 1)
                val text = question + "\n" + enquatePerText(result)
                val button = MaterialButton(context!!)
                button.text = text
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams.weight = 1F
                layoutParams.setMargins(10, 10, 10, 10)
                button.layoutParams = layoutParams
                //1～3は一段目
                if (i in 0..2) {
                    enquateView.enquate_linearlayout_1.addView(button)
                }
                //4～6は一段目
                if (i in 3..5) {
                    enquateView.enquate_linearlayout_2.addView(button)
                }
                //7～9は一段目
                if (i in 6..8) {
                    enquateView.enquate_linearlayout_3.addView(button)
                }
                //共有の文字
                shareText += "$question : ${enquatePerText(result)}\n"
            }
            //アンケ結果を共有
            Snackbar.make(
                liveFrameLayout,
                getString(R.string.enquate_result),
                Snackbar.LENGTH_LONG
            ).setAction(getString(R.string.share)) {
                //共有する
                share(shareText, "$title($programTitle-$liveId)")
            }.show()
        }
    }

    //アンケートの結果を％表示
    fun enquatePerText(per: String): StringBuilder {
        val result = StringBuilder(per).insert(per.length - 1, ".").append("%")
        return result
    }

    //アンケートへ応答。0が１番目？
    fun enquatePOST(pos: Int) {
        val jsonArray = JSONArray()
        jsonArray.put(pos)
        val bodyObject = JSONObject()
        bodyObject.put("command", "answerenquete")
        bodyObject.put("params", jsonArray)
        val jsonObject = JSONObject()
        jsonObject.put("type", "watch")
        jsonObject.put("body", bodyObject)
        connectionNicoLiveWebSocket.send(jsonObject.toString())
    }

    fun share(shareText: String, shareTitle: String) {
        val builder = ShareCompat.IntentBuilder.from(commentActivity)
        builder.setChooserTitle(shareTitle)
        builder.setSubject(shareTitle)
        builder.setText(shareText)
        builder.setType("text/plain")
        builder.startChooser()
    }

    //新しいコメント投稿画面
    fun commentCardView() {
        //投稿ボタンを押したら投稿
        comment_cardview_comment_send_button.setOnClickListener {
            val comment = comment_cardview_comment_textinput_edittext.text.toString()
            val command = comment_cardview_command_textinputlayout.text.toString()
            sendComment(comment, command)
            comment_cardview_comment_textinput_edittext.setText("")
        }
        //Enterキーを押したら投稿する
        if (pref_setting.getBoolean("setting_enter_post", true)) {
            comment_cardview_comment_textinput_edittext.setOnKeyListener { view: View, i: Int, keyEvent: KeyEvent ->
                if (i == KeyEvent.KEYCODE_ENTER) {
                    val text = comment_cardview_comment_textinput_edittext.text.toString()
                    val command = comment_cardview_command_textinputlayout.text.toString()
                    if (text.isNotEmpty()) {
                        //コメント投稿
                        sendComment(text, command)
                        comment_cardview_comment_textinput_edittext.setText("")
                    }
                }
                false
            }
        } else {
            //複数行？
            comment_cardview_comment_textinput_edittext.maxLines = Int.MAX_VALUE
        }
        //閉じるボタン
        comment_cardview_close_button.setOnClickListener {
            //非表示アニメーションに挑戦した。
            val hideAnimation =
                AnimationUtils.loadAnimation(
                    context!!,
                    R.anim.comment_cardview_hide_animation
                )
            //表示
            comment_activity_comment_cardview.startAnimation(hideAnimation)
            comment_activity_comment_cardview.visibility = View.GONE
            fab.show()
        }

        comment_cardview_comment_command_edit_button.setOnClickListener {
            // コマンド入力画面展開
            val visibility = comment_cardview_command_edit_linearlayout.visibility
            if (visibility == View.GONE) {
                // 展開
                comment_cardview_command_edit_linearlayout.visibility = View.VISIBLE
                // アイコンを閉じるアイコンへ
                comment_cardview_comment_command_edit_button.setImageDrawable(context?.getDrawable(R.drawable.ic_expand_more_24px))
            } else {
                comment_cardview_command_edit_linearlayout.visibility = View.GONE
                comment_cardview_comment_command_edit_button.setImageDrawable(context?.getDrawable(R.drawable.ic_outline_format_color_fill_24px))
            }
        }


        // 184が有効になっているときはコメントInputEditTextのHintに追記する
        if (isTokumeiComment) {
            comment_cardview_comment_textinput_layout.hint = getString(R.string.comment)
        } else {
            comment_cardview_comment_textinput_layout.hint =
                "${getString(R.string.comment)}（${getString(R.string.disabled_tokumei_comment)}）"
        }

        var commentSize = ""
        var commentColor = ""
        var commentPos = ""

        // コマンドリセットボタン
        comment_cardview_comment_command_edit_reset_button.setOnClickListener {
            comment_cardview_command_textinputlayout.setText("")
            clearColorCommandSizeButton()
            clearColorCommandPosButton()
            commentSize = ""
            commentColor = ""
            commentPos = ""
        }
        // 大きさ
        comment_cardview_comment_command_big_button.setOnClickListener {
            commentSize = "big"
            comment_cardview_command_textinputlayout.setText("$commentSize $commentPos $commentColor")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_medium_button.setOnClickListener {
            commentSize = "medium"
            comment_cardview_command_textinputlayout.setText("$commentSize $commentPos $commentColor")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_small_button.setOnClickListener {
            commentSize = "small"
            comment_cardview_command_textinputlayout.setText("$commentSize $commentPos $commentColor")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }

        // コメントの位置
        comment_cardview_comment_command_ue_button.setOnClickListener {
            commentPos = "ue"
            comment_cardview_command_textinputlayout.setText("$commentSize $commentPos $commentColor")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_naka_button.setOnClickListener {
            commentPos = "naka"
            comment_cardview_command_textinputlayout.setText("$commentSize $commentPos $commentColor")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_shita_button.setOnClickListener {
            commentPos = "shita"
            comment_cardview_command_textinputlayout.setText("$commentSize $commentPos $commentColor")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }

        // コメントの色。流石にすべてのボタンにクリックリスナー書くと長くなるので、タグに色（文字列）を入れる方法で対処
        comment_cardview_command_edit_color_linearlayout.children.forEach {
            it.setOnClickListener {
                commentColor = it.tag as String
                comment_cardview_command_textinputlayout.setText("$commentSize $commentPos $commentColor")
            }
        }

        if (pref_setting.getBoolean("setting_comment_collection_useage", false)) {
            //コメント投稿リスト読み込み
            loadCommentPOSTList()
            //コメントコレクション補充機能
            if (pref_setting.getBoolean("setting_comment_collection_assist", false)) {
                comment_cardview_comment_textinput_edittext.addTextChangedListener(object :
                    TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                    }

                    override fun beforeTextChanged(
                        p0: CharSequence?,
                        p1: Int,
                        p2: Int,
                        p3: Int
                    ) {
                    }

                    override fun onTextChanged(
                        p0: CharSequence?,
                        p1: Int,
                        p2: Int,
                        p3: Int
                    ) {
                        comment_cardview_chipgroup.removeAllViews()
                        //コメントコレクション読み込み
                        if (p0?.length ?: 0 >= 1) {
                            commentCollectionYomiList.forEach {
                                //文字列完全一致
                                if (it.equals(p0.toString())) {
                                    val yomi = it
                                    val pos = commentCollectionYomiList.indexOf(it)
                                    val comment = commentCollectionList[pos]
                                    //Chip
                                    val chip = Chip(context)
                                    chip.text = comment
                                    //押したとき
                                    chip.setOnClickListener {
                                        //置き換える
                                        var text = p0.toString()
                                        text = text.replace(yomi, comment)
                                        comment_cardview_comment_textinput_edittext.setText(
                                            text
                                        )
                                        //カーソル移動
                                        comment_cardview_comment_textinput_edittext.setSelection(
                                            text.length
                                        )
                                        //消す
                                        comment_cardview_chipgroup.removeAllViews()
                                    }
                                    comment_cardview_chipgroup.addView(chip)
                                }
                            }
                        }
                    }
                })
            }
        } else {
            comment_cardview_comment_list_button.visibility = View.GONE
        }
    }

    // ボタンの色を戻す サイズボタン
    fun clearColorCommandSizeButton() {
        comment_cardview_comment_command_size_layout.children.forEach {
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        }
    }

    // ボタンの色を戻す 位置ボタン
    fun clearColorCommandPosButton() {
        comment_cardview_comment_command_pos_layout.children.forEach {
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.parseColor("#757575"))
        }
    }


    fun loadCommentPOSTList() {
        //データベース
        val commentCollection = CommentCollectionSQLiteHelper(context!!)
        val sqLiteDatabase = commentCollection.writableDatabase
        commentCollection.setWriteAheadLoggingEnabled(false)
        val cursor = sqLiteDatabase.query(
            "comment_collection_db",
            arrayOf("comment", "yomi", "description"),
            null, null, null, null, null
        )
        cursor.moveToFirst()
        //ポップアップメニュー
        val popup = PopupMenu(context!!, comment_cardview_comment_list_button)
        for (i in 0 until cursor.count) {
            //コメント
            val comment = cursor.getString(0)
            val yomi = cursor.getString(1)
            //メニュー追加
            popup.menu.add(comment)
            //追加
            commentCollectionList.add(comment)
            commentCollectionYomiList.add(yomi)
            cursor.moveToNext()
        }
        //閉じる
        cursor.close()
        //ポップアップメニュー押したとき
        popup.setOnMenuItemClickListener {
            comment_cardview_comment_textinput_edittext.text?.append(it.title)
            true
        }
        //表示
        comment_cardview_comment_list_button.setOnClickListener {
            popup.show()
        }
    }

/*
    override fun onBackPressed() {
        AlertDialog.Builder(context!!)
            .setTitle(getString(R.string.back_dialog))
            .setMessage(getString(R.string.back_dialog_description))
            .setPositiveButton(getString(R.string.end)) { dialogInterface: DialogInterface, i: Int ->
                finish()
                super.onBackPressed()
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss()
            }
            .show()
    }
*/

    fun getSnackbarAnchorView(): View? {
        if (fab.isShown) {
            return fab
        } else {
            return comment_activity_comment_cardview
        }
    }

    //運営コメント
    fun setUnneiComment(comment: String) {
        //UIスレッドで呼ばないと動画止まる？
        commentActivity.runOnUiThread {
            //テキスト、背景色
            uncomeTextView.visibility = View.VISIBLE
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uncomeTextView.text = Html.fromHtml(comment, Html.FROM_HTML_MODE_COMPACT)
        } else {
            uncomeTextView.text = Html.fromHtml(comment)
        }
*/
            uncomeTextView.text =
                HtmlCompat.fromHtml(comment, HtmlCompat.FROM_HTML_MODE_COMPACT)
            uncomeTextView.textSize = 20F
            uncomeTextView.setTextColor(Color.WHITE)
            uncomeTextView.background = ColorDrawable(Color.parseColor("#80000000"))
            //追加
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.gravity = Gravity.TOP
            uncomeTextView.layoutParams = layoutParams
            uncomeTextView.gravity = Gravity.CENTER
            //表示アニメーション
            val showAnimation =
                AnimationUtils.loadAnimation(context!!, R.anim.unnei_comment_show_animation)
            //表示
            uncomeTextView.startAnimation(showAnimation)
            Timer().schedule(timerTask {
                removeUnneiComment()
            }, 5000)
        }
    }

    //運営コメント消す
    fun removeUnneiComment() {
        commentActivity.runOnUiThread {
            if (this@CommentFragment::uncomeTextView.isInitialized) {
                //表示アニメーション
                val hideAnimation =
                    AnimationUtils.loadAnimation(
                        context!!,
                        R.anim.unnei_comment_close_animation
                    )
                //表示
                uncomeTextView.startAnimation(hideAnimation)
                //初期化済みなら
                uncomeTextView.visibility = View.GONE
            }
        }
    }

    //infoコメント
    fun showInfoComment(comment: String) {
        //テキスト、背景色
        infoTextView.text = comment
        infoTextView.textSize = 15F
        infoTextView.setTextColor(Color.WHITE)
        infoTextView.background = ColorDrawable(Color.parseColor("#80000000"))
        //追加
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.BOTTOM
        infoTextView.layoutParams = layoutParams
        infoTextView.gravity = Gravity.CENTER
        //表示アニメーション
        val showAnimation =
            AnimationUtils.loadAnimation(context!!, R.anim.comment_cardview_show_animation)
        //表示
        infoTextView.startAnimation(showAnimation)
        infoTextView.visibility = View.VISIBLE
        //５秒後ぐらいで消す？
        Timer().schedule(timerTask {
            removeInfoComment()
        }, 5000)
    }

    //Infoコメント消す
    fun removeInfoComment() {
        commentActivity.runOnUiThread {
            if (context != null) {
                //非表示アニメーション
                val hideAnimation =
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.comment_cardview_hide_animation
                    )
                infoTextView.startAnimation(hideAnimation)
                infoTextView.visibility = View.GONE
            }
        }
    }


    //次枠移動機能
    fun checkNextProgram() {
        //getplayerstatusは番組ID以外にも放送開始していればコミュIDを入力すれば利用可能
        //１分ぐらいで取りに行く
        autoNextProgramTimer.schedule(0, 60000) {
            val request = Request.Builder()
                .url("https://live.nicovideo.jp/api/getplayerstatus/$communityID")   //getplayerstatus、httpsでつながる？
                .header("Cookie", "user_session=$usersession")
                .get()
                .build()
            val okHttpClient = OkHttpClient()


            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showToast("${getString(R.string.error)}")
                }

                override fun onResponse(call: Call, response: Response) {
                    val response_string = response.body?.string()
                    if (response.isSuccessful) {
                        //HTMLパース
                        val document = Jsoup.parse(response_string)
                        //ひつようなやつ
                        val stream = document.select("stream")
                        val liveId = stream.select("id")
                        //成功
                        //Activity再起動
                        commentActivity.finish()
                        val intent = Intent(context, CommentActivity::class.java)
                        intent.putExtra("liveId", liveId)
                        startActivity(intent)
                    } else {
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                }
            })
        }
        //１時間経ったら止める
        Timer().schedule(timerTask {
            this.cancel()
            autoNextProgramTimer.cancel()
        }, 3600000)
    }

    /**
     * 画質変更メッセージ送信
     * */
    fun sendQualityMessage(quality: String) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "watch")
        //body
        val bodyObject = JSONObject()
        bodyObject.put("command", "getstream")
        //requirement
        val requirementObjects = JSONObject()
        requirementObjects.put("protocol", "hls")
        requirementObjects.put("quality", quality)
        requirementObjects.put("isLowLatency", isLowLatency)
        requirementObjects.put("isChasePlay", false)
        bodyObject.put("requirement", requirementObjects)
        jsonObject.put("body", bodyObject)
        //送信
        connectionNicoLiveWebSocket.send(jsonObject.toString())
    }

    /**
     * 低遅延モード。trueで有効
     * */
    fun sendLowLatency() {
        val jsonObject = JSONObject()
        jsonObject.put("type", "watch")
        //body
        val bodyObject = JSONObject()
        bodyObject.put("command", "getstream")
        //requirement
        val requirementObjects = JSONObject()
        requirementObjects.put("protocol", "hls")
        requirementObjects.put("quality", start_quality)
        requirementObjects.put("isLowLatency", !isLowLatency)
        requirementObjects.put("isChasePlay", false)
        bodyObject.put("requirement", requirementObjects)
        jsonObject.put("body", bodyObject)
        //送信
        connectionNicoLiveWebSocket.send(jsonObject.toString())
        //反転
        isLowLatency = !isLowLatency
    }


    fun sendMobileDataQuality() {
        if (!mobileDataQualityCheck) {
            //モバイルデータのときは最低画質で再生する設定
            if (pref_setting.getBoolean("setting_mobiledata_quality_low", false)) {
                //今の接続状態を取得
                val connectivityManager =
                    context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                //ろりぽっぷとましゅまろ以上で分岐
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                            ?.hasTransport(
                                NetworkCapabilities.TRANSPORT_CELLULAR
                            ) == true
                    ) {
                        //モバイルデータ通信なら画質変更メッセージ送信
                        sendQualityMessage("super_low")
                    }
                } else {
                    if (connectivityManager.activeNetworkInfo.type == ConnectivityManager.TYPE_MOBILE) {
                        //モバイルデータ通信なら画質変更メッセージ送信
                        sendQualityMessage("super_low")
                    }
                }
            }
        }
        mobileDataQualityCheck = true
    }

    /**
     * フラグメントが入るLinearLayoutのIDを返す。
     * */
    fun getFragmentLinearLayoutId(): Int {
        return activity_comment_linearlayout.id
    }

    //ExoPlayerを破棄するときに
    //初期化済みか確認してから
    fun destroyExoPlayer(exoPlayer: SimpleExoPlayer) {
        //止める
        exoPlayer.apply {
            playWhenReady = false
            stop()
            seekTo(0)
            release()
        }
    }

    fun isPopupViewInit(): Boolean {
        return popUpPlayer.isInitializedPopUpView()
    }

    fun isAllRoomCommentInit(): Boolean {
        return ::allRoomComment.isInitialized
    }

}