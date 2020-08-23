package io.github.takusan23.tatimidroid.NicoLive

import android.app.Activity
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.util.Linkify
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Adapter.CommentViewPager
import io.github.takusan23.tatimidroid.CommentCanvas
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.FregmentData.NicoLiveFragmentData
import io.github.takusan23.tatimidroid.GoogleCast.GoogleCast
import io.github.takusan23.tatimidroid.NicoAPI.JK.NicoJKHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.NicoLive.Activity.CommentActivity
import io.github.takusan23.tatimidroid.NicoLive.Activity.FloatingCommentViewer
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.QualitySelectBottomSheet
import io.github.takusan23.tatimidroid.NimadoActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.KotehanDBInit
import io.github.takusan23.tatimidroid.Room.Init.NGDBInit
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.Service.startLivePlayService
import io.github.takusan23.tatimidroid.Tool.*
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.bottom_fragment_enquate_layout.view.*
import kotlinx.android.synthetic.main.comment_card_layout.*
import kotlinx.android.synthetic.main.include_nicolive_player_controller.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import okhttp3.internal.toLongOrDefault
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timerTask
import kotlin.math.roundToInt

/**
 * 生放送再生Fragment。
 * NicoLiveFragmentじゃなくてCommentFragmentになってるのはもともと全部屋見れるコメビュを作りたかったから
 * */
class CommentFragment : Fragment() {

    lateinit var commentActivity: AppCompatActivity
    lateinit var pref_setting: SharedPreferences
    lateinit var darkModeSupport: DarkModeSupport

    //ユーザーセッション
    var usersession = ""

    //視聴モード（コメント投稿機能付き）かどうか
    var isWatchingMode = false

    //視聴モードがnicocasの場合
    var isNicocasMode = false

    // ニコニコ実況ならtrue
    var isJK = false

    //hls
    var hlsAddress = ""

    //生放送を見る場合はtrue
    var watchLive = false

    //経過時間
    var programTimer = Timer()

    //アクティブ計算
    val activeTimer = Timer()

    // 番組情報
    var liveId = ""          // 番組ID
    var programTitle = ""    // 番組名
    var communityID = ""     // コミュニティID
    var thumbnailURL = ""    // サムネイル
    var roomName = ""         // 部屋の名前
    var chairNo = ""         // 席の場所
    lateinit var nicoLiveJSON: JSONObject

    /** コメント表示をOFFにする場合はtrue */
    var isCommentHide = false

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

    //運コメ・infoコメント非表示
    var hideInfoUnnkome = false

    //共有
    lateinit var programShare: ProgramShare

    //画質変更BottomSheetFragment
    lateinit var qualitySelectBottomSheet: QualitySelectBottomSheet

    // 現在の画質
    var currentQuality = ""

    //低遅延なのか。でふぉは低遅延有効
    var isLowLatency = true

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

    /** 延長検知。視聴セッション接続後すぐに送られてくるので一回目はパス */
    var isEntyouKenti = false

    // 匿名コメント非表示機能。基本off
    var isTokumeiHide = false

    //ExoPlayer
    lateinit var exoPlayer: SimpleExoPlayer

    //番組終了時刻（UnixTime
    var programEndUnixTime: Long = 0

    // GoogleCast使うか？
    lateinit var googleCast: GoogleCast

    // 公式かどうか
    var isOfficial = false

    // フォント変更機能
    lateinit var customFont: CustomFont

    // ニコ生ゲームようWebView
    lateinit var nicoNamaGameWebView: NicoNamaGameWebView

    // ニコ生ゲームが有効になっているか
    var isAddedNicoNamaGame = false

    // SurfaceView(ExoPlayer) + CommentCanvasのLayoutParams
    lateinit var surfaceViewLayoutParams: FrameLayout.LayoutParams

    // スワイプで画面切り替えるやつ
    lateinit var commentViewPager: CommentViewPager

    // ニコ生視聴セッション接続とかコメント投稿まで
    val nicoLiveHTML = NicoLiveHTML()

    // ニコ生前部屋接続など
    val nicoLiveComment = NicoLiveComment()

    /** 流量制限コメント鯖の情報 */
    var storeCommentServerData: NicoLiveComment.CommentServerData? = null

    /** アリーナ（部屋統合）のコメントサーバーの情報 */
    var commentServerData: NicoLiveComment.CommentServerData? = null

    /** コメント配列。これをRecyclerViewにいれるんじゃ */
    val commentJSONList = arrayListOf<CommentJSONParse>()

    // ニコニコ実況
    val nicoJK = NicoJKHTML()
    lateinit var getFlvData: NicoJKHTML.getFlvData

    /** 全画面再生時はtrue */
    var isFullScreenMode = false

    // NG関係
    private var ngList = listOf<String>()

    /**
     * コルーチンのChannelって機能でコルーチン間で値のやり取りができます。
     * このように書くとコメント内容が流れてきます。
     * ```
     * lifecycleScope.launch {
     *  commentBroadCastChannel.asFlow().collect { println(it.comment) }
     * }
     * ```
     * これで[io.github.takusan23.tatimidroid.BottomFragment.CommentLockonBottomFragment]とリアルタイムでコメントやり取りに成功した。
     * */
    val commentBroadCastChannel = BroadcastChannel<CommentJSONParse>(Channel.BUFFERED)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        commentCanvas = view.findViewById(R.id.comment_canvas)
        liveFrameLayout = view.findViewById(R.id.live_framelayout)
        fab = view.findViewById(R.id.fab)

        isNimadoMode = activity is NimadoActivity

        commentActivity = activity as AppCompatActivity

        darkModeSupport = DarkModeSupport(requireContext())
        darkModeSupport.setActivityTheme(activity as AppCompatActivity)

        // ActionBarが邪魔という意見があった（私も思う）ので消す
        if (activity !is NimadoActivity) {
            commentActivity.supportActionBar?.hide()
        }

        //ダークモード対応
        if (isDarkMode(context)) {
            commentActivity.supportActionBar?.setBackgroundDrawable(ColorDrawable(getThemeColor(darkModeSupport.context)))
            activity_comment_tab_layout.background = ColorDrawable(getThemeColor(darkModeSupport.context))
            comment_activity_fragment_layout_elevation_cardview.setCardBackgroundColor(getThemeColor(darkModeSupport.context))
        }

        // GoogleCast？
        googleCast = GoogleCast(requireContext())
        // GooglePlay開発者サービスがない可能性あり、Gapps焼いてない、ガラホ　など
        if (googleCast.isGooglePlayServicesAvailable()) {
            googleCast.init()
        }

        // データベースを監視する
        setNGDBChangeObserve()

        // 公式番組の場合はAPIが使えないため部屋別表示を無効にする。
        isOfficial = arguments?.getBoolean("isOfficial") ?: false

        // ニコニコ実況ならtrue
        isJK = arguments?.getBoolean("is_jk") ?: false

        //スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        liveId = arguments?.getString("liveId") ?: ""

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        // 低遅延モードon/off
        nicoLiveHTML.isLowLatency = pref_setting.getBoolean("nicolive_low_latency", false)
        // 初回の画質を低画質にする設定（モバイル回線とか強制低画質モードとか）
        val isMobileDataLowQuality = pref_setting.getBoolean("setting_mobiledata_quality_low", false) == isConnectionMobileDataInternet(context) // 有効時 でなお モバイルデータ接続時
        val isPreferenceLowQuality = pref_setting.getBoolean("setting_nicolive_quality_low", false)
        if (isMobileDataLowQuality || isPreferenceLowQuality) {
            nicoLiveHTML.startQuality = "super_low"
        }

        // ViewPager
        initViewPager()

        //センサーによる画面回転
        if (pref_setting.getBoolean("setting_rotation_sensor", false)) {
            rotationSensor = RotationSensor(commentActivity)
        }

        // ユーザーの設定したフォント読み込み
        customFont = CustomFont(context)
        // CommentCanvasにも適用するかどうか
        if (customFont.isApplyFontFileToCommentCanvas) {
            commentCanvas.typeface = customFont.typeface
        }

        //とりあえずコメントViewFragmentへ
        val checkCommentViewFragment = childFragmentManager.findFragmentByTag("${liveId}_comment_view_fragment")
        //Fragmentは画面回転しても存在するのでremoveして終了させる。
        if (checkCommentViewFragment != null) {
            val fragmentTransaction = childFragmentManager.beginTransaction()
            fragmentTransaction.remove(checkCommentViewFragment)
            fragmentTransaction.commit()
        }

        //生放送を視聴する場合はtrue
        watchLive = pref_setting.getBoolean("setting_watch_live", true)

        // argumentsから値もらう
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
        uncomeTextView = TextView(context)
        live_framelayout.addView(uncomeTextView)
        uncomeTextView.visibility = View.GONE
        //infoコメント
        infoTextView = TextView(context)
        live_framelayout.addView(infoTextView)
        infoTextView.visibility = View.GONE

        //視聴しない場合は非表示
        if (!watchLive) {
            live_framelayout.visibility = View.GONE
        }

        //コメント投稿画面開く
        fab.setOnClickListener {
            //表示アニメーションに挑戦した。
            val showAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_show_animation)
            //表示
            comment_activity_comment_cardview.startAnimation(showAnimation)
            comment_activity_comment_cardview.visibility = View.VISIBLE
            fab.hide()
            //コメント投稿など
            commentCardView()
        }

        // ステータスバー透明化＋タイトルバー非表示＋ノッチ領域にも侵略。関数名にAndがつくことはあんまりない
        hideStatusBarAndSetFullScreen()

        //ログイン情報がなければ戻す
        if (pref_setting.getString("mail", "")?.contains("") != false) {
            usersession = pref_setting.getString("user_session", "") ?: ""
            // データ取得からコメント取得など
            if (!isJK) {
                // エラーのとき（タイムアウトなど）はここでToastを出すなど
                val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                    showToast("${getString(R.string.error)}\n${throwable}")
                }
                // ニコ生
                lifecycleScope.launch(errorHandler) {
                    if (savedInstanceState != null) {
                        // 画面回転復帰時
                        val data = savedInstanceState.getSerializable("data") as NicoLiveFragmentData
                        nicoLiveJSON = JSONObject(data.nicoLiveJSON)
                        // 番組名取得など
                        nicoLiveHTML.initNicoLiveData(nicoLiveJSON)
                        programTitle = nicoLiveHTML.programTitle
                        communityID = nicoLiveHTML.communityId
                        thumbnailURL = nicoLiveHTML.thumb
                        storeCommentServerData = data.storeCommentServerData
                        // 全画面再生だったら全画面にする。ただし横画面のときのみ
                        if (data.isFullScreenMode && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            setFullScreen()
                        }
                    } else {
                        // はじめて ///
                        val html = getNicoLiveHTML()
                        nicoLiveJSON = nicoLiveHTML.nicoLiveHTMLtoJSONObject(html)
                        // 番組名取得など
                        nicoLiveHTML.initNicoLiveData(nicoLiveJSON)
                        programTitle = nicoLiveHTML.programTitle
                        communityID = nicoLiveHTML.communityId
                        thumbnailURL = nicoLiveHTML.thumb
                        // 履歴に追加
                        insertDB()
                    }
                    // IO -> Main Thread
                    withContext(Dispatchers.Main) {
                        // 経過時間
                        setLiveTime()
                        // UI反映
                        initController()
                        // 番組情報FragmentにHTMLのJSON渡す
                        if (isOfficial) {
                            (commentViewPager.instantiateItem(comment_viewpager, 4) as ProgramInfoFragment).apply {
                                jsonObject = nicoLiveJSON
                                jsonApplyUI(nicoLiveJSON)
                            }
                        } else {
                            (commentViewPager.instantiateItem(comment_viewpager, 5) as ProgramInfoFragment).apply {
                                jsonObject = nicoLiveJSON
                                jsonApplyUI(nicoLiveJSON)
                            }
                        }
                    }
                    // Main Thread -> IO
                    // 視聴セッションWebSocket接続
                    connectionNicoLiveWebSocket()
                    // getPlayerStatus叩いて座席番号と今いる部屋の名前（アリーナ最前列など）取得（座席番号HTML5で消えたけど）
                    getPlayerStatus()
                }
            } else {
                // ニコニコ実況
                nicoJKCoroutine()
            }
        } else {
            showToast(getString(R.string.mail_pass_error))
            commentActivity.finish()
        }

        /** コメント人数を定期的に数える */
        activeUserClear()

        /** 統計情報は押したときに計算するようにした。 */
        player_nicolive_control_statistics.setOnClickListener {
            calcToukei(true)
        }

    }

    /** コントローラーを初期化する。HTML取得後にやると良さそう */
    fun initController() {
        val job = Job()
        // 戻るボタン
        player_nicolive_control_back_button.isVisible = true
        player_nicolive_control_back_button.setOnClickListener {
            requireActivity().onBackPressed()
        }
        // 番組情報
        player_nicolive_control_title.text = nicoLiveHTML.programTitle
        player_nicolive_control_id.text = "${nicoLiveHTML.liveId} - $roomName - $chairNo"
        // 全画面/ポップアップ/バッググラウンド
        player_nicolive_control_popup.setOnClickListener { startPlayService("popup") }
        player_nicolive_control_background.setOnClickListener { startPlayService("background") }
        player_nicolive_control_fullscreen.setOnClickListener {
            if (isFullScreenMode) {
                // 全画面終了
                setCloseFullScreen()
            } else {
                // 全画面移行
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                setFullScreen()
            }
        }
        // 統計情報表示
        comment_fragment_statistics_show?.setOnClickListener {
            player_nicolive_control_info_main.isVisible = !player_nicolive_control_info_main.isVisible
        }
        // 横画面なら常に表示
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            player_nicolive_control_info_main.isVisible = true
        }
        // 押したら消せるように
        player_nicolive_control_parent.setOnClickListener {
            player_nicolive_control_main.isVisible = !player_nicolive_control_main.isVisible
            // フルスクリーン時はFabも消す
            if (isFullScreenMode) {
                if (fab.isShown) fab.hide() else fab.show()
            }
            updateHideController(job)
        }
        // 接続方法
        player_nicolive_control_video_network.apply {
            setImageDrawable(InternetConnectionCheck.getConnectionTypeDrawable(requireContext()))
            setOnClickListener {
                showToast(InternetConnectionCheck.createNetworkMessage(requireContext()))
            }
        }
        updateHideController(job)
    }

    /** コントローラーを消すためのコルーチン。 */
    private fun updateHideController(job: Job) {
        job.cancelChildren()
        // Viewを数秒後に非表示するとか
        lifecycleScope.launch(job) {
            // Viewを数秒後に消す
            delay(3000)
            if (player_nicolive_control_main?.isVisible == true) {
                player_nicolive_control_main?.isVisible = false
                // フルスクリーン時はFabも消す
                if (isFullScreenMode) {
                    if (fab.isShown) fab.hide() else fab.show()
                }
            }
        }
    }


    /**
     * NGデータベースを監視する。これで変更をすぐに検知できる
     * */
    private fun setNGDBChangeObserve() {
        lifecycleScope.launch {
            val dao = NGDBInit.getInstance(requireContext()).ngDBDAO()
            dao.flowGetNGAll().collect { ngList ->
                // NGユーザー追加/削除を検知
                this@CommentFragment.ngList = ngList.map { ngdbEntity -> ngdbEntity.value }
                // 取得済みコメントからも排除
                commentJSONList.toList().forEach { commentJSONParse ->
                    if (this@CommentFragment.ngList.contains(commentJSONParse.comment) || this@CommentFragment.ngList.contains(commentJSONParse.userId)) {
                        commentJSONList.remove(commentJSONParse)
                    }
                }
                // 一覧更新
                (commentViewPager.instantiateItem(comment_viewpager, 1) as CommentViewFragment).apply {
                    if (isInitAdapter()) {
                        commentRecyclerViewAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    /**
     * 流量制限コメントサーバーに接続する関数。コルーチンで
     * 流量制限コメントサーバーってのはコメントが多すぎてコメントが溢れてしまう際、溢れてしまったコメントが流れてくるサーバーのことだと思います。
     * ただまぁ超がつくほどの大手じゃないとここのWebSocketに接続しても特に流れてこないと思う。
     * 公式番組では利用できない。
     * @param userId ユーザーIDが取れれば入れてね。無くてもなんか動く
     * @param yourPostKey 視聴セッションから流れてくる。けど無くても動く（yourpostが無くなるけど）
     * */
    private fun connectionStoreCommentServer(userId: String? = null, yourPostKey: String? = null) {
        lifecycleScope.launch(Dispatchers.Default) {
            // コメントサーバー取得API叩く
            val allRoomResponse = nicoLiveComment.getProgramInfo(liveId, usersession)
            if (!allRoomResponse.isSuccessful) {
                showToast("${getString(R.string.error)}\n${allRoomResponse.code}")
                return@launch
            }
            // Store鯖へつなぐ
            storeCommentServerData = nicoLiveComment.parseStoreRoomServerData(allRoomResponse.body?.string(), getString(R.string.room_limit))
            if (storeCommentServerData != null) {
                // Store鯖へ接続する。（超）大手でなければ別に接続する必要はない
                nicoLiveComment.connectionWebSocket(storeCommentServerData!!.webSocketUri, storeCommentServerData!!.threadId, storeCommentServerData!!.roomName, userId, yourPostKey, ::commentFun)
            }
        }
    }

    /**
     * フルスクリーン再生。
     * 現状横画面のみ
     * */
    private fun setFullScreen() {
        // 全画面だよ
        isFullScreenMode = true
        // コメビュ非表示
        comment_activity_fragment_layout?.visibility = View.GONE
        // 背景黒にする
        comment_activity_fragment_layout_elevation_cardview.setCardBackgroundColor(ColorStateList.valueOf(Color.BLACK))
        // システムバー非表示
        setSystemBarVisibility(false)
        // 画面の大きさ取得
        val displayHeight = DisplaySizeTool.getDisplayHeight(context)
        // アイコン変更
        player_nicolive_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp))
        // ExoPlayerのアスペクト比設定
        liveFrameLayout.updateLayoutParams {
            height = displayHeight
            width = getAspectWidthFromHeight(displayHeight)
        }
        // Fab等消せるように
        live_framelayout.setOnClickListener {
            // コントローラー表示中は無視する
            if (comment_activity_comment_cardview.visibility == View.VISIBLE) return@setOnClickListener
            // FABの表示、非表示
            if (fab.isShown) {
                fab.hide()
            } else {
                fab.show()
            }
        }
        // 高さ更新
        commentCanvas.finalHeight = commentCanvas.height
    }

    /**
     * 全画面解除
     * */
    private fun setCloseFullScreen() {
        // 全画面ではない
        isFullScreenMode = false
        // コメビュ表示
        comment_activity_fragment_layout?.visibility = View.VISIBLE
        // 背景黒戻す
        comment_activity_fragment_layout_elevation_cardview.setCardBackgroundColor(ColorStateList.valueOf(getThemeColor(context)))
        // システムバー表示
        setSystemBarVisibility(true)
        // アイコン変更
        player_nicolive_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_black_24dp))
        // 画面の幅取得
        val displayWidth = DisplaySizeTool.getDisplayWidth(context)
        // ExoPlayerのアスペクト比設定
        liveFrameLayout.updateLayoutParams {
            width = displayWidth / 2
            height = getAspectHeightFromWidth(displayWidth / 2)
        }
        // 高さ更新
        commentCanvas.finalHeight = commentCanvas.height
    }

    /**
     * ニコニコ実況のデータ取得
     * */
    private fun nicoJKCoroutine() {
        lifecycleScope.launch {
            // getflv叩く。
            val getFlvResponse = nicoJK.getFlv(liveId, usersession).await()
            if (!getFlvResponse.isSuccessful) {
                // 失敗のときは落とす
                activity?.finish()
                showToast("${getString(R.string.error)}\n${getFlvResponse.code}")
                return@launch
            }
            // getflvパースする
            getFlvData = withContext(Dispatchers.Default) {
                nicoJK.parseGetFlv(getFlvResponse.body?.string())!!
            }
            // 番組情報入れる
            player_nicolive_control_title.text = getFlvData.channelName
            player_nicolive_control_id.text = liveId
            // 接続
            withContext(Dispatchers.Default) {
                nicoJK.connectionCommentServer(getFlvData, ::commentFun)
            }
        }
        // コマンドいらないと思うし消す
        comment_cardview_comment_command_edit_button.visibility = View.GONE

        // アクティブ計算以外使わないので消す
        (player_nicolive_control_time.parent as View).isVisible = false
        player_nicolive_control_watch_count.isVisible = false
        player_nicolive_control_comment_count.isVisible = false

        initController()

    }

    /**
     * ニコ生放送ページのHTML取得。コルーチンです
     * */
    private suspend fun getNicoLiveHTML(): String? = withContext(Dispatchers.Default) {
        // ニコ生視聴ページリクエスト
        val livePageResponse = nicoLiveHTML.getNicoLiveHTML(liveId, usersession, isWatchingMode)
        if (!livePageResponse.isSuccessful) {
            // 失敗のときは落とす
            activity?.finish()
            showToast("${getString(R.string.error)}\n${livePageResponse.code}")
            null
        }
        if (!nicoLiveHTML.hasNiconicoID(livePageResponse)) {
            // niconicoIDがない場合（ログインが切れている場合）はログインする（この後の処理でユーザーセッションが必要）
            usersession = NicoLogin.reNicoLogin(context)
            // 視聴モードなら再度視聴ページリクエスト
            if (isWatchingMode) {
                getNicoLiveHTML()
            }
        }
        livePageResponse.body?.string()
    }

    /**
     * 視聴セッションWebSocketに接続する関数
     * */
    private fun connectionNicoLiveWebSocket() {
        // データ流してくれるWebSocketに接続する
        nicoLiveHTML.apply {
            connectionWebSocket(nicoLiveJSON) { command, message ->
                // メッセージ受け取り
                when {
                    command == "stream" -> {
                        // HLSアドレス取得
                        hlsAddress = getHlsAddress(message) ?: ""
                        // 画質一覧と今の画質
                        val currentQuality = getCurrentQuality(message)
                        // 生放送再生
                        if (watchLive) {
                            setPlayVideoView()
                        } else {
                            //レイアウト消す
                            live_framelayout.visibility = View.GONE
                        }
                        // 画質変更BottomSheet初期化
                        initQualityChangeBottomFragment(getCurrentQuality(message), getQualityListJSONArray(message))
                        // 最初の画質を控える
                        if (this@CommentFragment.currentQuality.isEmpty()) {
                            this@CommentFragment.currentQuality = currentQuality
                        } else {
                            // 画質変更SnackBar表示
                            showQualityChangeSnackBar(currentQuality)
                        }
                        // GoogleCast関係。Gappsなくてもやる
                        commentActivity.runOnUiThread {
                            googleCast.apply {
                                programTitle = this@CommentFragment.programTitle
                                programSubTitle = this@CommentFragment.liveId
                                programThumbnail = this@CommentFragment.thumbnailURL
                                hlsAddress = this@CommentFragment.hlsAddress
                                resume()
                            }
                        }
                    }
                    command == "room" -> {
                        // threadId、WebSocketURL受信。コメント送信時に使うWebSocketに接続する
                        if (isWatchingMode) {
                            val commentMessageServerUri = getCommentServerWebSocketAddress(message)
                            val commentThreadId = getCommentServerThreadId(message)
                            val yourPostKey = getCommentYourPostKey(message)
                            val commentRoomName = getString(R.string.room_integration) // ユーザーならコミュIDだけどもう立ちみないので部屋統合で統一
                            // コメントサーバーへ接続する
                            commentServerData = NicoLiveComment.CommentServerData(commentMessageServerUri, commentThreadId, commentRoomName, yourPostKey)
                            nicoLiveComment.connectionWebSocket(commentMessageServerUri, commentThreadId, commentRoomName, nicoLiveHTML.userId, yourPostKey, ::commentFun)
                            // 流量制限コメント鯖へ接続する
                            if (!isOfficial) {
                                connectionStoreCommentServer(nicoLiveHTML.userId, yourPostKey)
                            }
                        } else {
                            // 視聴モード以外は非ログインなのでyourPostKeyが取れない
                            val commentMessageServerUri = getCommentServerWebSocketAddress(message)
                            val commentThreadId = getCommentServerThreadId(message)
                            val commentRoomName = getString(R.string.room_integration) // ユーザーならコミュIDだけどもう立ちみないので部屋統合で統一
                            // コメントサーバーへ接続する
                            commentServerData = NicoLiveComment.CommentServerData(commentMessageServerUri, commentThreadId, commentRoomName, null)
                            nicoLiveComment.connectionWebSocket(commentMessageServerUri, commentThreadId, commentRoomName, null, null, ::commentFun)
                            // 流量制限コメント鯖へ接続する
                            if (!isOfficial) {
                                connectionStoreCommentServer(nicoLiveHTML.userId)
                            }
                        }
                    }
                    command == "postCommentResult" -> {
                        // コメント送信結果。
                        showCommentPOSTResultSnackBar(message)
                    }
                    command == "statistics" -> {
                        // 総来場者数、コメント数を表示させる
                        initStatisticsInfo(message)
                    }
                    command == "schedule" -> {
                        // 延長を検知
                        showSchedule(message)
                    }
                }
                // containsで部分一致にしてみた。なんで部分一致なのかは私も知らん
                if (command.contains("disconnect")) {
                    //番組終了
                    programEnd(message)
                }
            }
        }
    }

    /**
     * 座席番号と部屋の名前取得
     * */
    private suspend fun getPlayerStatus() = withContext(Dispatchers.IO) {
        // getPlayerStatus叩いて座席番号取得
        val getPlayerStatusResponse = nicoLiveHTML.getPlayerStatus(liveId, usersession)
        if (getPlayerStatusResponse.isSuccessful) {
            // なおステータスコード200でも中身がgetPlayerStatusのものかどうかはまだわからないので、、、
            val document =
                Jsoup.parse(getPlayerStatusResponse.body?.string())
            // 番組開始直後（開始数秒でアクセス）すると何故か視聴ページにリダイレクト（302）されるのでチェック
            val hasGetPlayerStatusTag = document.getElementsByTag("getplayerstatus ").isNotEmpty()
            // 番組が終わっててもレスポンスは200を返すのでチェック
            if (hasGetPlayerStatusTag && document.getElementsByTag("getplayerstatus ")[0].attr("status") == "ok") {
                roomName = document.getElementsByTag("room_label")[0].text() // 部屋名
                chairNo = document.getElementsByTag("room_seetno")[0].text() // 座席番号
                withContext(Dispatchers.Main) {
                    player_nicolive_control_id.text = "${nicoLiveHTML.liveId} - $roomName - $chairNo"
                }
            } else {
                // getPlayerStatus取得失敗時
                withContext(Dispatchers.Main) {
                    Snackbar.make(player_nicolive_control_id, R.string.error_getplayserstatus, Snackbar.LENGTH_SHORT).apply {
                        anchorView = getSnackbarAnchorView()
                        show()
                    }
                }
            }
        }
    }

    // コメントが来たらこの関数が呼ばれる
    private fun commentFun(comment: String, roomName: String, isHistoryComment: Boolean) {
        val room = if (isJK) {
            getFlvData.channelName
        } else {
            roomName
        }
        // JSONぱーす
        val commentJSONParse = CommentJSONParse(comment, room, liveId)
        // アンケートや運コメを表示させる。
        if (roomName != getString(R.string.room_limit)) {
            when {
                comment.contains("/vote") -> {
                    // アンケート
                    showEnquate(comment)
                }
                comment.contains("/disconnect") -> {
                    // disconnect受け取ったらSnackBar表示
                    showProgramEndMessageSnackBar(comment, commentJSONParse)
                }
            }
            if (!hideInfoUnnkome) {
                //運営コメント
                if (commentJSONParse.premium == "生主" || commentJSONParse.premium == "運営") {
                    setUneiComment(commentJSONParse)
                }
                //運営コメントけす
                if (commentJSONParse.comment.contains("/clear")) {
                    removeUnneiComment()
                }
            }
        }
        // 匿名コメント落とすモード
        if (isTokumeiHide && commentJSONParse.mail.contains("184")) {
            return
        }
        // NGユーザー/コメントの場合は「NGコメントです表記」からそもそも非表示に(配列に追加しない)するように。
        when {
            ngList.contains(commentJSONParse.userId) -> return
            ngList.contains(commentJSONParse.comment) -> return
        }
        /*
         * 重複しないように。
         * 令和元年8月中旬からアリーナに一般のコメントが流れ込むように（じゃあ枠の仕様なくせ栗田さん）
         * 令和元年12月中旬から立ち見部屋にアリーナのコメントが流れ込むように（だから枠の仕様無くせよ）
         * というわけで同じコメントが追加されてしまうので対策する。
         * 12月中旬のメンテで立ち見部屋にアリーナコメントが出るように（とさり気なく枠の時間復活）なったとき、JSONにoriginが追加されて、
         * 多分originの値がC以外のときに元の部屋のコメントだと
         * */
        Handler(Looper.getMainLooper()).post {
            // UI Thread

            // コルーチンのChannelに送信する。ロックオンBottomFragmentで利用する。なんかUIスレッドじゃないとだめっぽ？
            lifecycleScope.launch {
                if (!commentBroadCastChannel.isClosedForSend) {
                    commentBroadCastChannel.send(commentJSONParse)
                }
            }

            // コテハン追加など
            registerKotehan(commentJSONParse)
            // RecyclerViewに追加
            commentJSONList.add(0, commentJSONParse)
            // CommentFragment更新かける
            (commentViewPager.instantiateItem(comment_viewpager, 1) as CommentViewFragment).apply {
                // Adapter初期化済みなら（ViewPager多分Fragment切り替えると破棄する？ので多分必要）
                if (isInitAdapter()) {
                    commentRecyclerViewAdapter.notifyItemInserted(0)
                    recyclerViewScrollPos()
                }
            }
            // コメント非表示モードの場合はなさがない
            if (!isCommentHide) {
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
                    // 複数行対応Var
                    commentCanvas.postCommentAsciiArt(asciiArtComment, commentJSONParse)
                }
            }
        }

    }

    /**
     * コテハンがコメントに含まれている場合はコテハンDBに追加する関数
     * コテハンmap反映もしている。
     * */
    private fun registerKotehan(commentJSONParse: CommentJSONParse) {
        val comment = commentJSONParse.comment
        if (comment.contains("@") || comment.contains("＠")) {
            // @の位置を特定
            val index = when {
                comment.contains("@") -> comment.indexOf("@") + 1 // @を含めないように
                comment.contains("＠") -> comment.indexOf("＠") + 1 // @を含めないように
                else -> -1
            }
            if (index != -1) {
                val kotehan = comment.substring(index)
                // データベースにも入れる。コテハンデータベースの変更は自動でやってくれる
                lifecycleScope.launch(Dispatchers.IO) {
                    val dao = KotehanDBInit.getInstance(requireContext()).kotehanDBDAO()
                    // すでに存在する場合・・・？
                    val kotehanData = dao.findKotehanByUserId(commentJSONParse.userId)
                    if (kotehanData != null) {
                        // 存在した
                        val kotehanDBEntity = kotehanData.copy(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000))
                        dao.update(kotehanDBEntity)
                    } else {
                        // 存在してない
                        val kotehanDBEntity = KotehanDBEntity(kotehan = kotehan, addTime = (System.currentTimeMillis() / 1000), userId = commentJSONParse.userId)
                        dao.insert(kotehanDBEntity)
                    }
                }
            }
        }
    }

    /**
     * 運営コメント表示
     * */
    fun setUneiComment(commentJSONParse: CommentJSONParse) {
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
                    val list = commentJSONParse.comment.replace("/gift ", "").split(" ")
                    val userName = list[2]
                    val giftPoint = list[3]
                    val giftName = list[5]
                    val message = "${userName} さんが ${giftName} （${giftPoint} pt）をプレゼントしました。"
                    showInfoComment(message)
                }
            }
        }
    }

    /**
     * disconnectのSnackBarを表示
     * */
    fun showProgramEndMessageSnackBar(message: String, commentJSONParse: CommentJSONParse) {
        if (commentJSONParse.premium.contains("運営")) {
            //終了メッセージ
            Snackbar.make(live_video_view, context?.getString(R.string.program_disconnect) ?: "", Snackbar.LENGTH_SHORT).setAction(context?.getString(R.string.end)) {
                //終了
                if (activity !is NimadoActivity) {
                    //二窓Activity以外では終了できるようにする。
                    activity?.finish()
                }
            }.setAnchorView(getSnackbarAnchorView()).show()
        }
    }

    /**
     * アンケート表示
     * @param message /voteが文字列に含まれていることが必須
     * */
    fun showEnquate(message: String) {
        // コメント取得
        val jsonObject = JSONObject(message)
        val chatObject = jsonObject.getJSONObject("chat")
        val content = chatObject.getString("content")
        val premium = chatObject.getInt("premium")
        if (premium == 3) {
            // 運営コメントに表示
            // アンケ開始
            if (content.contains("/vote start")) {
                commentActivity.runOnUiThread {
                    setEnquetePOSTLayout(content, "start")
                }
            }
            // アンケ結果
            if (content.contains("/vote showresult")) {
                commentActivity.runOnUiThread {
                    setEnquetePOSTLayout(content, "showresult")
                }
            }
            // アンケ終了
            if (content.contains("/vote stop")) {
                commentActivity.runOnUiThread {
                    comment_fragment_enquate_framelayout.removeAllViews()
                }
            }
        }
    }

    /**
     * コメント投稿成功SnackBar
     * @param message chat_resultが文字列に含まれている必要がある
     * */
    private fun showCommentPOSTResultSnackBar(message: String) {
        lifecycleScope.launch {
            val jsonObject = JSONObject(message)

            /**
             * 本当に送信できたかどうか。
             * 実は流量制限にかかってしまったのではないか（公式番組以外では流量制限コメント鯖（store鯖）に接続できるけど公式は無理）
             * 流量制限にかかると他のユーザーには見えない。ので本当に成功したか確かめる
             * */
            // この機能は公式番組でのみ使う
            if(isOfficial){
                val comment = jsonObject.getJSONObject("data").getJSONObject("chat").getString("content")
                delay(500)
                // 受信済みコメント配列から自分が投稿したコメント(yourpostが1)でかつ5秒前まで遡った配列を作る
                val nowTime = System.currentTimeMillis() / 1000
                val prevComment = commentJSONList.filter { commentJSONParse ->
                    val time = nowTime - (commentJSONParse.date.toLongOrDefault(0))
                    time <= 5 && commentJSONParse.yourPost // 5秒前でなお自分が投稿したものを
                }.map { commentJSONParse -> commentJSONParse.comment }
                if (prevComment.contains(comment)) {
                    // コメント一覧に自分のコメントが有る
                    Snackbar.make(fab, getString(R.string.comment_post_success), Snackbar.LENGTH_SHORT).setAnchorView(getSnackbarAnchorView()).show()
                } else {
                    // 無いので流量制限にかかった（他には見えない）
                    Snackbar.make(fab, "${getString(R.string.comment_post_error)}\n${getString(R.string.comment_post_limit)}", Snackbar.LENGTH_SHORT).setAnchorView(getSnackbarAnchorView()).show()
                }
            }else{
                // ユーザー番組では多分取れる
                Snackbar.make(fab, getString(R.string.comment_post_success), Snackbar.LENGTH_SHORT).setAnchorView(getSnackbarAnchorView()).show()
            }
        }
    }

    /**
     * 番組終了。Activityを閉じる関数
     * */
    fun programEnd(message: String) {

        // 理由？
        val because = JSONObject(message).getJSONObject("data").getString("reason")
        // 原因が追い出しの場合はToast出す
        if (because == "CROWDED") {
            showToast("${getString(R.string.oidashi)}\uD83C\uDD7F")
        }

        // Activity終了
        if (pref_setting.getBoolean("setting_disconnect_activity_finish", false)) {
            if (activity is CommentActivity) {
                // Activity が CommentActivity なら消す。二窓Activityは動かないように
                activity?.finish()
            }
        }
    }

    /**
     * 延長を検知したら表示させる
     * @param message onMessageの内容。scheduleが含まれていることが必要。
     * */
    fun showSchedule(message: String?) {
        val scheduleData = nicoLiveHTML.getSchedule(message)
        //時間出す場所確保したので終了時刻書く。
        if (isEntyouKenti) {
            // 終了時刻出す
            val time = nicoLiveHTML.getScheduleEndTime(message)
            val message = "${getString(R.string.entyou_message)}\n${getString(R.string.end_time)} $time"
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
        // 延長したら残り時間再計算する
        // 割り算！
        val calc = (scheduleData.endTime - scheduleData.beginTime) / 1000
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
            player_nicolive_control_end_time?.text = "${hourString}:${minuteString}:00"
        }
        // 番組終了時刻を入れる
        programEndUnixTime = scheduleData.endTime / 1000
    }

    /**
     * 総来場者数、コメント数を表示させる
     * @param message onMessageの内容。statisticsが含まれていることが必要。
     * */
    private fun initStatisticsInfo(message: String?) {
        val data = nicoLiveHTML.getStatistics(message)
        commentActivity.runOnUiThread {
            player_nicolive_control_watch_count?.text = data.viewers.toString()
            player_nicolive_control_comment_count?.text = data.comments.toString()
        }
    }

    /**
     * 画質変更SnackBar表示
     * @param selectQuality 選択した画質
     * */
    private fun showQualityChangeSnackBar(selectQuality: String?) {
        if (selectQuality == null) {
            return
        }
        //画質変更した。Snackbarでユーザーに教える
        val oldQuality = QualitySelectBottomSheet.getQualityText(currentQuality, context)
        val newQuality = QualitySelectBottomSheet.getQualityText(selectQuality, context)
        Snackbar.make(live_surface_view, "${getString(R.string.successful_quality)}\n${oldQuality}→${newQuality}", Snackbar.LENGTH_SHORT).show()
        currentQuality = selectQuality
    }

    // 画質変更BottomFragment初期化
    private fun initQualityChangeBottomFragment(selectQuality: String?, qualityTypesJSONArray: JSONArray) {
        // 画質変更BottomFragmentに詰める。なんかUIスレッドにしないとだめっぽい？
        activity?.runOnUiThread {
            val bundle = Bundle()
            bundle.putString("select_quality", selectQuality)
            bundle.putString("quality_list", qualityTypesJSONArray.toString())
            bundle.putString("liveId", liveId)
            qualitySelectBottomSheet = QualitySelectBottomSheet()
            qualitySelectBottomSheet.arguments = bundle
        }
    }

    private fun initViewPager() {
        comment_viewpager.id = View.generateViewId()
        commentViewPager = CommentViewPager(activity as AppCompatActivity, liveId, isOfficial, isJK)
        comment_viewpager.adapter = commentViewPager
        activity_comment_tab_layout.setupWithViewPager(comment_viewpager)
        // コメントを指定しておく
        comment_viewpager.currentItem = 1
    }

    /**
     * ステータスバーとノッチに侵略するやつ
     * */
    fun hideStatusBarAndSetFullScreen() {
        if (pref_setting.getBoolean("setting_display_cutout", false)) {
            // 非表示にする
            setSystemBarVisibility(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setNotchVisibility(false)
            }
        } else {
            // 表示する
            setSystemBarVisibility(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setNotchVisibility(true)
            }
        }
    }

    /**
     * システムバーを非表示にする関数
     * システムバーはステータスバーとナビゲーションバーのこと。多分
     * @param isShow 表示する際はtrue。非表示の際はfalse
     * */
    fun setSystemBarVisibility(isShow: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 systemUiVisibilityが非推奨になり、WindowInsetsControllerを使うように
            activity?.window?.insetsController?.apply {
                if (isShow) {
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_BARS_BY_TOUCH
                    show(WindowInsets.Type.systemBars())
                } else {
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE // View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY の WindowInset版。ステータスバー表示等でスワイプしても、操作しない場合はすぐに戻るやつです。
                    hide(WindowInsets.Type.systemBars()) // Type#systemBars を使うと Type#statusBars() Type#captionBar() Type#navigationBars() 一斉に消せる
                }
            }
        } else {
            // Android 10 以前
            if (isShow) {
                activity?.window?.decorView?.systemUiVisibility = 0
            } else {
                activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            }
        }
    }

    /**
     * ノッチ領域に侵略する関数。
     * この関数はAndroid 9以降で利用可能なので各自条件分岐してね。
     * @param isShow 侵略する際はtrue。そうじゃないならfalse
     * */
    @RequiresApi(Build.VERSION_CODES.P)
    fun setNotchVisibility(isShow: Boolean) {
        val attribute = activity?.window?.attributes
        attribute?.layoutInDisplayCutoutMode = if (isShow) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    // ニコ生ゲーム有効
    fun setNicoNamaGame(isWebViewPlayer: Boolean = false) {
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
        nicoNamaGameWebView = NicoNamaGameWebView(requireContext(), liveId, isWebViewPlayer)
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

    /** アクティブ人数を計算する。一分間間隔 */
    private fun activeUserClear() {
        //1分でリセット
        activeTimer.schedule(10000, 60000) {
            calcToukei()
        }
    }

    /**
     * 統計情報を表示する。立ち見部屋の数が出なくなったの少し残念。人気番組の指数だったのに
     * @param showSnackBar SnackBarを表示する場合はtrue
     * */
    private fun calcToukei(showSnackBar: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            val calender = Calendar.getInstance()
            calender.add(Calendar.MINUTE, -1)
            val unixTime = calender.timeInMillis / 1000L
            // 今のUnixTime
            val nowUnixTime = System.currentTimeMillis() / 1000L
            // 範囲内のコメントを取得する
            val timeList = commentJSONList.toList().filter { comment ->
                if (comment.date.toFloatOrNull() != null) {
                    comment.date.toLong() in unixTime..nowUnixTime
                } else {
                    false
                }
            }
            // 同じIDを取り除く
            val idList = timeList.distinctBy { comment -> comment.userId }

            // 数えた結果
            withContext(Dispatchers.Main) {
                // 数えた結果
                player_nicolive_control_active_text.text = "${idList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}"
                // 統計ボタン押せるように
                player_nicolive_control_statistics.visibility = View.VISIBLE
            }

            // SnackBarで統計を表示する場合
            if (showSnackBar) {
                // プレ垢人数
                val premiumCount = idList.count { commentJSONParse -> commentJSONParse.premium == "\uD83C\uDD7F" }
                // 生ID人数
                val userIdCount = idList.count { commentJSONParse -> !commentJSONParse.mail.contains("184") }
                // 平均コメント数
                val commentLengthAverageDouble = timeList.map { commentJSONParse -> commentJSONParse.comment.length }.average()
                val commentLengthAverage = if (!commentLengthAverageDouble.isNaN()) {
                    commentLengthAverageDouble.roundToInt()
                } else {
                    -1
                }
                // UnixTime(ms)をmm:ssのssだけを取り出すためのSimpleDataFormat。
                val simpleDateFormat = SimpleDateFormat("ss")
                // 秒間コメントを取得する。なお最大値
                val commentPerSecondMap = timeList.groupBy({ comment ->
                    // 一分間のコメント配列から秒、コメント配列のMapに変換するためのコード
                    // 例。51秒に投稿されたコメントは以下のように：51=[いいよ, がっつコラボ, ガッツ, 歓迎]
                    val programStartTime = nicoLiveHTML.programStartTime
                    val calc = comment.date.toLong() - programStartTime
                    simpleDateFormat.format(calc * 1000).toInt()
                }, { comment ->
                    comment
                }).maxBy { map ->
                    // 秒Mapから一番多いのを取る。
                    map.value.size
                }
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    // 数えた結果
                    player_nicolive_control_active_text.text = "${idList.size}${getString(R.string.person)} / ${getString(R.string.one_minute)}"
                    // 統計情報表示
                    val toukei = """${getString(R.string.one_minute_statistics)}
${getString(R.string.comment_per_second)}(${getString(R.string.max_value)}/${calcLiveTime(commentPerSecondMap?.value?.first()?.date?.toLong() ?: 0L)})：${commentPerSecondMap?.value?.size}
${getString(R.string.one_minute_statistics_premium)}：$premiumCount
${getString(R.string.one_minute_statistics_user_id)}：$userIdCount
${getString(R.string.one_minute_statistics_comment_length)}：$commentLengthAverage"""
                    multiLineSnackbar(player_nicolive_control_active_text, toukei)
                }
            }

        }
    }


    /**
     * MultilineなSnackbar
     * https://stackoverflow.com/questions/30705607/android-multiline-snackbar
     * */
    private fun multiLineSnackbar(view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
        textView.maxLines = 5 // show multiple line
        snackbar.anchorView = getSnackbarAnchorView() // 何のViewの上に表示するか指定
        snackbar.show()
    }

    // データベースに履歴追加
    private fun insertDB() {
        // Roomはメインスレッドでは使えない
        lifecycleScope.launch(Dispatchers.IO) {
            val unixTime = System.currentTimeMillis() / 1000
            // 入れるデータ
            val nicoHistoryDBEntity = NicoHistoryDBEntity(
                type = "live",
                serviceId = liveId,
                userId = communityID,
                title = programTitle,
                unixTime = unixTime,
                description = ""
            )
            NicoHistoryDBInit.getInstance(requireContext()).nicoHistoryDBDAO().insert(nicoHistoryDBEntity)
        }
    }

    //経過時間計算
    private fun setLiveTime() {
        //1秒ごとに
        programTimer = Timer()
        programTimer.schedule(0, 1000) {
            // 現在時刻
            val unixtime = System.currentTimeMillis() / 1000L
            // フォーマット！
            commentActivity.runOnUiThread {
                player_nicolive_control_time?.text = calcLiveTime(unixtime)
            }
        }
    }

    /**
     * 相対時間を計算する。25:25みたいなこと。
     * @param unixTime 基準時間。
     * */
    private fun calcLiveTime(unixTime: Long): String {
        // 経過時間 - 番組開始時間
        val calc = unixTime - nicoLiveHTML.programStartTime
        val date = Date(calc * 1000L)
        //時間はUNIX時間から計算する
        val hour = (calc / 60 / 60)
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "$hour:${simpleDateFormat.format(date.time)}"
    }

    /**
     * 16:9で横の大きさがわかるときに縦の大きさを返す
     * @param width 幅
     * @return 幅にあった高さ。16:9になる
     * */
    private fun getAspectHeightFromWidth(width: Int): Int {
        val heightCalc = width / 16
        return heightCalc * 9
    }

    /**
     * 16:9で縦の大きさが分かる時に横の大きさを返す関数
     * @param height 高さ
     * @return 高さにあった幅。16:9になる
     * */
    private fun getAspectWidthFromHeight(height: Int): Int {
        val widthCalc = height / 9
        return widthCalc * 16
    }

    //視聴モード
    fun setPlayVideoView() {
        if (context == null) {
            return
        }
        //設定で読み込むかどうか
        Handler(Looper.getMainLooper()).post {
            live_surface_view.visibility = View.VISIBLE
            // println("生放送再生：HLSアドレス : $hls_address")

            // 画面の幅取得。Android 11に対応した
            val displayWidth = DisplaySizeTool.getDisplayWidth(context)

            //ウィンドウの半分ぐらいの大きさに設定
            val frameLayoutParams = liveFrameLayout.layoutParams

            // 全画面だった場合はアスペクト比調整しない
            if (!isFullScreenMode) {
                //横画面のときの対応
                if (commentActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    //二窓モードのときはとりあえず更に小さくしておく
                    if (isNimadoMode) {
                        frameLayoutParams.width = displayWidth / 4
                        frameLayoutParams.height = getAspectHeightFromWidth(displayWidth / 4)
                    } else {
                        //16:9の9を計算
                        frameLayoutParams.height = getAspectHeightFromWidth(displayWidth / 2)
                    }
                    liveFrameLayout.layoutParams = frameLayoutParams
                } else {
                    //縦画面
                    frameLayoutParams.width = displayWidth
                    //二窓モードのときは小さくしておく
                    if (isNimadoMode) {
                        frameLayoutParams.width = displayWidth / 2
                    }
                    //16:9の9を計算
                    frameLayoutParams.height = getAspectHeightFromWidth(frameLayoutParams.width)
                    liveFrameLayout.layoutParams = frameLayoutParams
                }
            }

            // 高さ更新
            commentCanvas.finalHeight = commentCanvas.height

            exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
            val sourceFactory = DefaultDataSourceFactory(
                context,
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

            val hlsMediaSource = HlsMediaSource.Factory(sourceFactory).createMediaSource(hlsAddress.toUri())

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
                    if (!nicoLiveHTML.nicoLiveWebSocketClient.isClosed) {
                        println("再度再生準備を行います")
                        activity?.runOnUiThread {
                            //再生準備
                            exoPlayer.prepare(hlsMediaSource)
                            //SurfaceViewセット
                            exoPlayer.setVideoSurfaceView(live_surface_view)
                            //再生
                            exoPlayer.playWhenReady = true
                            Snackbar.make(fab, getString(R.string.error_player), Snackbar.LENGTH_SHORT).apply {
                                anchorView = getSnackbarAnchorView()
                                // 再生が止まった時に低遅延が有効になっていればOFFにできるように。安定して見れない場合は低遅延が有効なのが原因
                                if (nicoLiveHTML.isLowLatency) {
                                    setAction(getString(R.string.low_latency_off)) {
                                        nicoLiveHTML.sendLowLatency(!nicoLiveHTML.isLowLatency)
                                    }
                                }
                                show()
                            }
                        }
                    }
                }
            })

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
        }
    }

    override fun onPause() {
        super.onPause()
        googleCast.pause()
    }

    /**
     * コメント投稿用
     * コメント送信処理は[NicoLiveHTML.sendPOSTWebSocketComment]でやってる
     * @param comment コメント内容。「お大事に」とか
     * @param size コメントの大きさ。これらは省略が可能
     * @param position コメントの位置。これらは省略が可能
     * @param color コメントの色。これらは省略が可能
     * */
    private fun sendComment(comment: String, color: String = "white", size: String = "medium", position: String = "naka") {
        if (isJK) {
            // ニコニコ実況
            nicoJK.postCommnet(comment, getFlvData.userId, getFlvData.baseTime.toLong(), getFlvData.threadId, usersession)
        } else {
            if (comment != "\n") {
                if (isWatchingMode) {
                    // 視聴セッションWebSocketにコメントを送信する
                    nicoLiveHTML.sendPOSTWebSocketComment(comment, color, size, position)
                } else if (isNicocasMode) {
                    // コマンドをくっつける
                    val command = "$color $size $position"
                    // ニコキャスのAPIを叩いてコメントを投稿する
                    nicoLiveHTML.sendCommentNicocasAPI(comment, command, liveId, usersession, { showToast(getString(R.string.error)) }, { response ->
                        // 成功時
                        if (response.isSuccessful) {
                            //成功
                            Snackbar.make(fab, getString(R.string.comment_post_success), Snackbar.LENGTH_SHORT).apply {
                                anchorView = getSnackbarAnchorView()
                                show()
                            }
                        } else {
                            showToast("${getString(R.string.error)}\n${response.code}")
                        }
                    })
                }
            }
        }
    }

    fun showToast(message: String) {
        commentActivity.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** フローティングコメントビューワー起動関数 */
    fun showBubbles() {
        // FloatingCommentViewer#showBubble()に移動させました
        FloatingCommentViewer.showBubbles(
            context = requireContext(),
            liveId = liveId,
            watchMode = arguments?.getString("watch_mode"),
            title = nicoLiveHTML.programTitle,
            thumbUrl = nicoLiveHTML.thumb
        )
    }


    fun setLandscapePortrait() {
        val conf = resources.configuration
        //live_video_view.stopPlayback()
        when (conf.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }

    fun copyProgramId() {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
        //コピーしました！
        Toast.makeText(context, "${getString(R.string.copy_program_id)} : $liveId", Toast.LENGTH_SHORT).show()
    }

    fun copyCommunityId() {
        val clipboardManager =
            context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", communityID))
        //コピーしました！
        Toast.makeText(context, "${getString(R.string.copy_communityid)} : $communityID", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
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
        programTimer.cancel()
        activeTimer.cancel()
        //センサーによる画面回転が有効になってる場合は最後に
        if (this@CommentFragment::rotationSensor.isInitialized) {
            rotationSensor.destroy()
        }
        //止める
        if (this@CommentFragment::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        nicoLiveHTML.destroy()
        nicoLiveComment.destroy()
        nicoJK.destroy()
        commentBroadCastChannel.cancel()
        // println("とじます")
    }

    //Activity終了時に閉じる
    override fun onDestroy() {
        super.onDestroy()
        destroyCode()
    }

    /*オーバーレイ*/
    fun startOverlayPlayer() {
        startPlayService("popup")
    }

    /*バックグラウンド再生*/
    fun setBackgroundProgramPlay() {
        startPlayService("background")
    }

    /**
     * ポップアップ再生、バッググラウンド再生サービス起動用関数
     * @param mode "popup"（ポップアップ再生）か"background"（バッググラウンド再生）
     * */
    private fun startPlayService(mode: String) {
        // サービス起動
        startLivePlayService(context = context, mode = mode, liveId = liveId, isCommentPost = isWatchingMode, isNicocasMode = isNicocasMode, isJK = isJK, isTokumei = nicoLiveHTML.isTokumeiComment, startQuality = currentQuality)
        // Activity落とす
        activity?.finish()
    }


    //Activity復帰した時に呼ばれる
    override fun onStart() {
        super.onStart()
        //再生部分を作り直す
        if (hlsAddress.isNotEmpty()) {
            live_framelayout.visibility = View.VISIBLE
            setPlayVideoView()
        }
    }

    fun setEnquetePOSTLayout(message: String, type: String) {
        enquateView = layoutInflater.inflate(R.layout.bottom_fragment_enquate_layout, null, false)
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
                val button = MaterialButton(requireContext())
                button.text = jsonArray.getString(i)
                button.setOnClickListener {
                    // 投票
                    enquatePOST(i - 1)
                    // アンケ画面消す
                    comment_fragment_enquate_framelayout.removeAllViews()
                    // SnackBar
                    Snackbar.make(liveFrameLayout, "${getString(R.string.enquate)}：${jsonArray[i]}", Snackbar.LENGTH_SHORT).apply {
                        anchorView = getSnackbarAnchorView()
                        show()
                    }
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
        } else if (enquateJSONArray.isNotEmpty()) {
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
                val button = MaterialButton(requireContext())
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
            Snackbar.make(liveFrameLayout, getString(R.string.enquate_result), Snackbar.LENGTH_SHORT).apply {
                anchorView = getSnackbarAnchorView()
                setAction(getString(R.string.share)) {
                    //共有する
                    share(shareText, "$title($programTitle-$liveId)")
                }
                show()
            }
        }
    }

    //アンケートの結果を％表示
    private fun enquatePerText(per: String): String {
        // 176 を 17.6% って表記するためのコード。１桁増やして（9%以下とき対応できないため）２桁消す
        val percentToFloat = per.toFloat() * 10
        val result = "${(percentToFloat / 100)}%"
        return result
    }

    /**
     * アンケを押す
     * @param pos アンケの位置。多分一番目が0（配列みたいに）
     * ■■■■■■■■■■■
     * ■ 1 /             ■
     * ■ /               ■
     * ■  とても良かった  ■
     * ■                 ■
     * ■■ [ 98.6 ％ ] ■■   宇宙よりも遠い場所 2020/08/09 一挙アンケ
     * */
    fun enquatePOST(pos: Int) {
        val jsonObject = JSONObject().apply {
            put("type", "answerEnquete")
            put("data", JSONObject().apply {
                put("answer", pos)
            })
        }
        nicoLiveHTML.nicoLiveWebSocketClient.send(jsonObject.toString())
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
        // プレ垢ならプレ垢用コメント色パレットを表示させる
        if (nicoLiveHTML.isPremium) {
            comment_cardview_command_edit_color_premium_linearlayout.visibility = View.VISIBLE
        }
        // 184が有効になっているときはコメントInputEditTextのHintに追記する
        if (nicoLiveHTML.isTokumeiComment) {
            comment_cardview_comment_textinput_layout.hint = getString(R.string.comment)
        } else {
            comment_cardview_comment_textinput_layout.hint =
                "${getString(R.string.comment)}（${getString(R.string.disabled_tokumei_comment)}）"
        }
        //投稿ボタンを押したら投稿
        comment_cardview_comment_send_button.setOnClickListener {
            val comment = comment_cardview_comment_textinput_edittext.text.toString()
            // 7/27からコマンド（色や位置の指定）は全部つなげて送るのではなく、それぞれ指定する必要があるので
            val color = comment_cardview_command_color_textinputlayout.text.toString()
            val position = comment_cardview_command_position_textinputlayout.text.toString()
            val size = comment_cardview_command_size_textinputlayout.text.toString()
            // コメ送信
            sendComment(comment, color, size, position)
            comment_cardview_comment_textinput_edittext.setText("")
            if (!pref_setting.getBoolean("setting_command_save", false)) {
                // コマンドを保持しない設定ならクリアボタンを押す
                comment_cardview_comment_command_edit_reset_button.callOnClick()
            }
        }
        // Enterキー(紙飛行機ボタン)を押したら投稿する
        if (pref_setting.getBoolean("setting_enter_post", true)) {
            comment_cardview_comment_textinput_edittext.imeOptions = EditorInfo.IME_ACTION_SEND
            comment_cardview_comment_textinput_edittext.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    val text = comment_cardview_comment_textinput_edittext.text.toString()
                    if (text.isNotEmpty()) {
                        // 空じゃなければ、コメント投稿
                        // 7/27からコマンド（色や位置の指定）は全部つなげて送るのではなく、それぞれ指定する必要があるので
                        val color = comment_cardview_command_color_textinputlayout.text.toString()
                        val position = comment_cardview_command_position_textinputlayout.text.toString()
                        val size = comment_cardview_command_size_textinputlayout.text.toString()
                        // コメ送信
                        sendComment(text, color, size, position)
                        // コメントリセットとコマンドリセットボタンを押す
                        comment_cardview_comment_textinput_edittext.setText("")
                        if (!pref_setting.getBoolean("setting_command_save", false)) {
                            // コマンドを保持しない設定ならクリアボタンを押す
                            comment_cardview_comment_command_edit_reset_button.callOnClick()
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } else {
            // 複数行？一筋縄では行かない
            // https://stackoverflow.com/questions/51391747/multiline-does-not-work-inside-textinputlayout
            comment_cardview_comment_textinput_edittext.inputType = InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_CLASS_TEXT
            comment_cardview_comment_textinput_edittext.maxLines = Int.MAX_VALUE
        }
        //閉じるボタン
        comment_cardview_close_button.setOnClickListener {
            // 非表示アニメーションに挑戦した。
            val hideAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_hide_animation)
            // 表示
            comment_activity_comment_cardview.startAnimation(hideAnimation)
            comment_activity_comment_cardview.visibility = View.GONE
            fab.show()
            // IMEも消す（Android 11 以降）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity?.window?.insetsController?.hide(WindowInsets.Type.ime())
            }
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

        // コマンドリセットボタン
        comment_cardview_comment_command_edit_reset_button.setOnClickListener {
            // リセット
            comment_cardview_command_color_textinputlayout.setText("")
            comment_cardview_command_position_textinputlayout.setText("")
            comment_cardview_command_size_textinputlayout.setText("")
            clearColorCommandSizeButton()
            clearColorCommandPosButton()
        }
        // 大きさ
        comment_cardview_comment_command_big_button.setOnClickListener {
            comment_cardview_command_size_textinputlayout.setText("big")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_medium_button.setOnClickListener {
            comment_cardview_command_size_textinputlayout.setText("medium")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_small_button.setOnClickListener {
            comment_cardview_command_size_textinputlayout.setText("small")
            clearColorCommandSizeButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }

        // コメントの位置
        comment_cardview_comment_command_ue_button.setOnClickListener {
            comment_cardview_command_position_textinputlayout.setText("ue")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_naka_button.setOnClickListener {
            comment_cardview_command_position_textinputlayout.setText("naka")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }
        comment_cardview_comment_command_shita_button.setOnClickListener {
            comment_cardview_command_position_textinputlayout.setText("shita")
            clearColorCommandPosButton()
            (it as Button).backgroundTintList = ColorStateList.valueOf(Color.CYAN)
        }

        // コメントの色。流石にすべてのボタンにクリックリスナー書くと長くなるので、タグに色（文字列）を入れる方法で対処
        comment_cardview_command_edit_color_linearlayout.children.forEach {
            it.setOnClickListener {
                comment_cardview_command_color_textinputlayout.setText(it.tag as String)
            }
        }
        // ↑のプレ垢版
        comment_cardview_command_edit_color_premium_linearlayout.children.forEach {
            it.setOnClickListener {
                comment_cardview_command_color_textinputlayout.setText(it.tag as String)
            }
        }

        if (pref_setting.getBoolean("setting_comment_collection_useage", false)) {
            //コメントコレクション補充機能
            if (pref_setting.getBoolean("setting_comment_collection_assist", false)) {
                comment_cardview_comment_textinput_edittext.addTextChangedListener(object :
                    TextWatcher {
                    override fun afterTextChanged(p0: Editable?) {
                    }

                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
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
            uncomeTextView.text = HtmlCompat.fromHtml(comment, HtmlCompat.FROM_HTML_MODE_COMPACT)
            uncomeTextView.textSize = 20F
            uncomeTextView.setTextColor(Color.WHITE)
            uncomeTextView.background = ColorDrawable(Color.parseColor("#80000000"))
            uncomeTextView.autoLinkMask = Linkify.WEB_URLS
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
                AnimationUtils.loadAnimation(requireContext(), R.anim.unnei_comment_show_animation)
            //表示
            uncomeTextView.startAnimation(showAnimation)
            lifecycleScope.launch {
                delay(5000)
                removeUnneiComment()
            }
        }
    }

    //運営コメント消す
    fun removeUnneiComment() {
        if (!isAdded) return
        commentActivity.runOnUiThread {
            if (this@CommentFragment::uncomeTextView.isInitialized) {
                //表示アニメーション
                val hideAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.unnei_comment_close_animation)
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
            AnimationUtils.loadAnimation(requireContext(), R.anim.comment_cardview_show_animation)
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
                val hideAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_hide_animation)
                infoTextView.startAnimation(hideAnimation)
                infoTextView.visibility = View.GONE
            }
        }
    }

    // 画面回転時に値引き継ぐ
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            if (::nicoLiveJSON.isInitialized) {
                val data = NicoLiveFragmentData(
                    isOfficial = isOfficial,
                    nicoLiveJSON = nicoLiveJSON.toString(),
                    storeCommentServerData = storeCommentServerData,
                    isFullScreenMode = isFullScreenMode
                )
                putSerializable("data", data)
            }
        }
    }

    fun isInitGoogleCast(): Boolean = ::googleCast.isInitialized

    fun isInitNicoLiveJSONObject(): Boolean = ::nicoLiveJSON.isInitialized

    fun isInitQualityChangeBottomSheet(): Boolean = ::qualitySelectBottomSheet.isInitialized

}