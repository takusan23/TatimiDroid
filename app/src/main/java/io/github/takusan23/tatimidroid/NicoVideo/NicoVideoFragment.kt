package io.github.takusan23.tatimidroid.NicoVideo

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.FregmentData.DevNicoVideoFragmentData
import io.github.takusan23.tatimidroid.FregmentData.TabLayoutData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRecommendAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoruAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoRecyclerPagerAdapter
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoPOSTFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NGDBEntity
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NGDBInit
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.*
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.inflate_nicovideo_player_controller.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * 開発中のニコ動クライアント（？）
 *
 * id           |   動画ID。必須
 * cache        |   キャッシュ再生ならtrue。なければfalse
 * eco          |   エコノミー再生するなら（?eco=1）true
 * */
class NicoVideoFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var darkModeSupport: DarkModeSupport

    // ハートビート
    var heartBeatTimer = Timer()
    var seekTimer = Timer()

    // 必要なやつ
    var userSession = ""
    var threadId = ""
    var isPremium = false // プレ垢ならtrue
    var isDMCServer = true // DMCサーバーならtrue。判断方法はPC版プレイヤー右クリックで視聴方法の切り替えがあればDMCで見れてる
    var userId = ""
    var videoId = ""
    var videoTitle = ""
    var is169AspectLate = true // 16:9の動画はtrue

    // 選択中の画質
    var currentVideoQuality = ""
    var currentAudioQuality = ""

    // キャッシュ取得用
    lateinit var nicoVideoCache: NicoVideoCache
    var isCache = false
    var contentUrl = ""
    var nicoHistory = ""

    /** 動画情報JSON。キャッシュ再生時でなお動画情報JSONがなければ永遠に初期化されません。 */
    lateinit var jsonObject: JSONObject

    // session_apiのレスポンス
    lateinit var sessionAPIJSONObject: JSONObject

    // データ取得からハートビートまで扱う
    val nicoVideoHTML = NicoVideoHTML()

    /** なにも操作していない、コメント取得APIの結果が入ってる配列。なま(いみしｎ)。画面回転時にSerializeで渡してるのはこっち */
    var rawCommentList = arrayListOf<CommentJSONParse>()

    /** 3DS消したりNGを適用した結果が入っている配列。RecyclerViewで表示したり流れるコメントのソースはここ */
    var commentList = arrayListOf<CommentJSONParse>()

    // 関連動画配列
    var recommendList = arrayListOf<NicoVideoData>()

    // ViewPager
    lateinit var viewPager: NicoVideoRecyclerPagerAdapter

    // 閉じたならtrue
    var isDestory = false

    // 画面回転したあとの再生時間
    var rotationProgress = 0L

    // 再生時間を適用したらtrue。一度だけ動くように
    var isRotationProgressSuccessful = false

    // 共有
    lateinit var share: ProgramShare

    // フォント
    lateinit var font: CustomFont

    // シーク操作中かどうか
    var isTouchSeekBar = false

    // コメント描画改善。drawComment()関数でのみ使う（0秒に投稿されたコメントが重複して表示される対策）
    private var drewedList = arrayListOf<String>() // 描画したコメントのNoが入る配列。一秒ごとにクリアされる
    private var tmpPosition = 0L // いま再生している位置から一秒引いた値が入ってる。

    // ニコれるように
    val nicoruAPI = NicoruAPI()

    // 画面回転復帰時
    lateinit var devNicoVideoFragmentData: DevNicoVideoFragmentData

    /** 全画面再生時はtrue */
    var isFullScreenMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(requireContext())
        nicoVideoCache = NicoVideoCache(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // ふぉんと
        font = CustomFont(context)
        // CommentCanvasにも適用するかどうか
        if (font.isApplyFontFileToCommentCanvas) {
            fragment_nicovideo_comment_canvas.typeFace = font.typeface
        }

        // スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ActionBar消す
        (activity as AppCompatActivity).supportActionBar?.hide()

        // NGベース監視。コテハンはNicoVideoAdapterの方へ
        setNGDBChangeObserve()

        // View初期化
        showSwipeToRefresh()

        // ダークモード
        initDarkmode()

        // コントローラー表示
        // initController()

        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()

        // 動画ID
        videoId = arguments?.getString("id") ?: ""

        /**
         * 画面回転復帰時かどうか
         * 画面回転復帰時はデータを取りに行かずに、ハートビート処理のみ行う
         * だたしデータが大きすぎるとBundleに入り切らないのでその時は再度取りに行く(getSerializable("data")がnull)
         * */
        if (savedInstanceState?.getSerializable("data") != null) {
            // 画面回転復帰時
            // 値もらう
            devNicoVideoFragmentData = savedInstanceState.getSerializable("data") as DevNicoVideoFragmentData
            isCache = devNicoVideoFragmentData.isCachePlay
            contentUrl = devNicoVideoFragmentData.contentUrl
            rawCommentList = ArrayList(devNicoVideoFragmentData.commentList)
            nicoHistory = devNicoVideoFragmentData.nicoHistory
            rotationProgress = devNicoVideoFragmentData.currentPos
            recommendList = devNicoVideoFragmentData.recommendList
            nicoruAPI.nicoruKey = devNicoVideoFragmentData.nicoruKey!!
            is169AspectLate = devNicoVideoFragmentData.is169AspectLate
            // 動画情報無いとき（動画情報JSON無くても再生はできる）有る
            if (devNicoVideoFragmentData.dataApiData != null) {
                jsonObject = JSONObject(devNicoVideoFragmentData.dataApiData!!)
                // プレ垢かどうか、ユーザー情報等（ニコるくんで使う）
                isPremium = nicoVideoHTML.isPremium(jsonObject)
                threadId = nicoVideoHTML.getThreadId(jsonObject)
                userId = nicoVideoHTML.getUserId(jsonObject)
            }
            // TabLayout（ViewPager）の回転前の状態取得とViewPagerの初期化
            val dynamicAddFragmentList = savedInstanceState.getParcelableArrayList<TabLayoutData>("tab") as ArrayList<TabLayoutData>
            initViewPager(dynamicAddFragmentList)
            // オンライン再生時のみやること
            if (!isCache) {
                sessionAPIJSONObject = JSONObject(devNicoVideoFragmentData.sessionAPIJSONObject!!)
                // 初期化ゾーン
                // ハートビート処理。これしないと切られる。
                nicoVideoHTML.heartBeat(jsonObject, sessionAPIJSONObject)
            }
            // UIに反映
            lifecycleScope.launch(Dispatchers.Main) {
                applyUI()
                // NG適用
                commentFilter(false)
                if (!isCache) {
                    // 関連動画
                    (viewPager.fragmentList[3] as NicoVideoRecommendFragment).apply {
                        recommendList.forEach {
                            recyclerViewList.add(it)
                        }
                        initRecyclerView()
                    }
                }
                // 全画面なら全画面にする。ただし横画面のときのみ
                if (devNicoVideoFragmentData.isFullScreenMode && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setFullScreen()
                }
            }
        } else {
            // 初めて///

            firstPlay()

        }
    }

    /**
     * NGデータベースを監視する関数
     * RoomとFlowのおかげでDBに変更が入ると通知してくれるようになった。便利
     * */
    private fun setNGDBChangeObserve() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val dao = NGDBInit.getInstance(requireContext()).ngDBDAO()
                dao.flowGetNGAll().collect { ngList ->
                    // NGコメント追加した
                    commentFilter(ngList = ngList, showToast = false)
                }
            }
        }
    }

    /**
     * 動画再生。これは画面回展示ではなく初回の再生時や、他の動画に切り替える際に使う。関数名えっっっっっち
     * 動画ID変更はとりあえずargment#putString("id","動画ID")でたのんだ。
     * @param isInitViewPager ViewPagerを初期化する際はtrue。初回実行時のみ。動画を切り替える際はfalseにしてくれ
     * */
    private fun firstPlay(isInitViewPager: Boolean = true) {
        // キャッシュを優先的に使う設定有効？
        val isPriorityCache = prefSetting.getBoolean("setting_nicovideo_cache_priority", false)

        // キャッシュ再生が有効ならtrue
        isCache = when {
            arguments?.getBoolean("cache") ?: false -> true // キャッシュ再生
            NicoVideoCache(context).existsCacheVideoInfoJSON(videoId) && isPriorityCache -> true // キャッシュ優先再生が可能
            else -> false // オンライン
        }

        // キャッシュ再生かどうかが分かったところでViewPager初期化
        if (isInitViewPager) {
            initViewPager()
        }

        // 強制エコノミーの設定有効なら
        val isPreferenceEconomyMode = prefSetting.getBoolean("setting_nicovideo_economy", false)
        // エコノミー再生するなら
        val isEconomy = arguments?.getBoolean("eco") ?: false

        when {
            // キャッシュを優先的に使う&&キャッシュ取得済みの場合 もしくは　キャッシュ再生時
            isCache -> cachePlay()
            // エコノミー再生？
            isEconomy || isPreferenceEconomyMode -> coroutine(true, "", "", true)
            // それ以外：インターネットで取得
            else -> coroutine()
        }
    }

    /**
     * フルスクリーンへ移行。
     * 先日のことなんですけども、ついに（待望）（念願の）（満を持して）、わたくしの動画が、無断転載されてました（ﾄﾞｩﾋﾟﾝ）
     * ↑これ sm29392869 全画面で見れるようになる
     * 現状横画面のみ
     * */
    private fun setFullScreen() {
        isFullScreenMode = true
        player_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp))
        // コメント一覧非表示
        fragment_nicovideo_viewpager_linarlayout.visibility = View.GONE
        // 下のタイトル非表示
        fragment_nicovideo_video_title_linearlayout.visibility = View.GONE
        // システムバー消す
        setSystemBarVisibility(false)
        // 黒色へ。
        (fragment_nicovideo_video_title_linearlayout.parent as View).setBackgroundColor(Color.BLACK)
        // アスペクト比調整
        fragment_nicovideo_framelayout.updateLayoutParams {
            val displayWidth = DisplaySizeTool.getDisplayWidth(context)
            val displayHeight = DisplaySizeTool.getDisplayHeight(context)
            if (isLandscape()) {
                // 横画面。横の大きさを計算する
                width = if (is169AspectLate) getAspectWidthFromHeight(displayHeight) else getOldAspectWidthFromHeight(displayHeight)
                height = displayHeight
            } else {
                // 縦画面。縦の大きさを計算する
                height = if (is169AspectLate) getAspectHeightFromWidth(displayWidth) else getOldAspectHeightFromWidth(displayWidth)
                width = displayWidth
            }
        }
    }

    private fun setCloseFullScreen() {
        isFullScreenMode = false
        player_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_black_24dp))
        // コメント一覧表示
        fragment_nicovideo_viewpager_linarlayout?.visibility = View.VISIBLE
        // システムバー表示
        setSystemBarVisibility(true)
        // 色戻す
        (fragment_nicovideo_video_title_linearlayout.parent as View).setBackgroundColor(getThemeColor(context))
        // 横画面のみ。下のタイトル表示
        fragment_nicovideo_video_title_linearlayout?.isVisible = true
        // アスペ（クト比）直す
        fragment_nicovideo_framelayout.updateLayoutParams {
            // 画面の幅取得。令和最新版（ビリビリワイヤレスイヤホン並感）
            val displayWidth = DisplaySizeTool.getDisplayWidth(context)
            val displayHeight = DisplaySizeTool.getDisplayHeight(context)
            if (isLandscape()) {
                // 横画面。縦の大きさを求める
                width = displayWidth / 2
                height = if (is169AspectLate) getAspectHeightFromWidth(width) else getOldAspectHeightFromWidth(width)
            } else {
                // 縦画面。縦の大きさを求める
                width = if (is169AspectLate) displayWidth else (displayWidth / 1.2).toInt()
                height = if (is169AspectLate) getAspectHeightFromWidth(width) else getOldAspectHeightFromWidth(width)
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

    /** コントローラー初期化。[exoPlayer]の再生ができる状態になったら呼んでください */
    fun initController() {
        // コントローラーを消すためのコルーチン
        val job = Job()
        // 戻るボタン
        player_control_back_button.isVisible = true
        player_control_back_button.setOnClickListener {
            requireActivity().onBackPressed()
        }
        // スキップ秒数
        val skipTime = (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5)
        val longSkipTime = (prefSetting.getString("nicovideo_long_skip_sec", "10")?.toLongOrNull() ?: 10)
        // ダブルタップ版setOnClickListener。拡張関数です。DoubleClickListener
        player_control_prev.setOnDoubleClickListener { motionEvent, isDoubleClick ->
            val skip = if (isDoubleClick) {
                // ダブルタップ時
                (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000 // 秒→ミリ秒
            } else {
                // シングル
                (prefSetting.getString("nicovideo_long_skip_sec", "10")?.toLongOrNull() ?: 10) * 1000 // 秒→ミリ秒
            }
            exoPlayer.seekTo(exoPlayer.currentPosition - skip)
            fragment_nicovideo_comment_canvas.seekComment()
            updateHideController(job)
        }
        player_control_next.setOnDoubleClickListener { motionEvent, isDoubleClick ->
            val skip = if (isDoubleClick) {
                // ダブルタップ時
                (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000 // 秒→ミリ秒
            } else {
                // シングル
                (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000 // 秒→ミリ秒
            }
            exoPlayer.seekTo(exoPlayer.currentPosition + skip)
            // コメントシークに対応させる
            fragment_nicovideo_comment_canvas.seekComment()
            updateHideController(job)
        }
        // 全画面ボタン
        player_control_fullscreen.setOnClickListener {
            if (isFullScreenMode) {
                // 全画面終了ボタン
                setCloseFullScreen()
            } else {
                // なお全画面は横のみサポート
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                setFullScreen()
            }
        }
        // コントローラー消せるように
        player_control_parent.setOnClickListener {
            // 非表示切り替え
            player_control_main.isVisible = !player_control_main.isVisible
            // ３秒待ってもViewが表示されてる場合は消せるように。
            updateHideController(job)
        }
        // ポップアップ/バッググラウンドなど
        player_control_popup.setOnClickListener {
            // ポップアップ再生
            startVideoPlayService(context = context, mode = "popup", videoId = videoId, isCache = isCache, videoQuality = currentVideoQuality, audioQuality = currentAudioQuality)
            // Activity落とす
            activity?.finish()
        }
        player_control_background.setOnClickListener {
            // バッググラウンド再生
            startVideoPlayService(context = context, mode = "background", videoId = videoId, isCache = isCache, videoQuality = currentVideoQuality, audioQuality = currentAudioQuality)
            // Activity落とす
            activity?.finish()
        }
        // シークバー用意
        player_control_seek.max = (exoPlayer.duration / 1000L).toInt()
        player_control_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // シークいじったら時間反映されるように
                val formattedTime = DateUtils.formatElapsedTime((seekBar?.progress ?: 0).toLong())
                val videoLengthFormattedTime = DateUtils.formatElapsedTime(exoPlayer.duration / 1000L)
                player_control_current.text = formattedTime
                player_control_duration.text = videoLengthFormattedTime
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // コントローラー非表示カウントダウン終了
                job.cancelChildren()
                isTouchSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTouchSeekBar = false
                // コメントシークに対応させる
                fragment_nicovideo_comment_canvas.seekComment()
                // ExoPlayer再開
                exoPlayer.seekTo((seekBar?.progress ?: 0) * 1000L)
                // コントローラー非表示カウントダウン再開
                updateHideController(job)
            }
        })
        // Viewを数秒後に非表示するとか
        updateHideController(job)
    }

    /**
     * コントローラーを消すためのコルーチン。
     * */
    private fun updateHideController(job: Job) {
        // Viewを数秒後に非表示するとか
        job.cancelChildren()
        lifecycleScope.launch(job) {
            // Viewを数秒後に消す
            delay(3000)
            if (player_control_main?.isVisible == true) {
                player_control_main?.isVisible = false
            }
        }
    }

    // Progress表示
    private fun showSwipeToRefresh() {
        fragment_nicovideo_swipe.apply {
            isRefreshing = true
            isEnabled = true
        }
    }

    // Progress非表示
    private fun hideSwipeToRefresh() {
        fragment_nicovideo_swipe.apply {
            isRefreshing = false
            isEnabled = false
        }
    }

    // ダークモード
    private fun initDarkmode() {
        darkModeSupport = DarkModeSupport(requireContext())
        fragment_nicovideo_tablayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(darkModeSupport.context))
        fragment_nicovideo_framelayout_elevation_cardview.setCardBackgroundColor(getThemeColor(darkModeSupport.context))
    }

    /**
     * 縦画面のみ。動画のタイトルなど表示・非表示やタイトル設定など
     * @param jsonObject NicoVideoHTML#parseJSON()の戻り値
     * */
    private fun initTitleArea() {
        // Fragmentがもう無い可能性が
        if (!isAdded) return
        if (fragment_nicovideo_bar != null && fragment_nicovideo_video_title_linearlayout != null) {
            fragment_nicovideo_bar?.setOnClickListener {
                // バー押したら動画のタイトルなど表示・非表示
                fragment_nicovideo_video_title_linearlayout.apply {
                    visibility = if (visibility == View.GONE) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
        fragment_nicovideo_comment_title.text = videoTitle
        fragment_nicovideo_comment_videoid.text = videoId
    }

    /**
     * データ取得から動画再生/コメント取得まで。
     * 注意：モバイルデータ接続で強制的に低画質にする設定が有効になっている場合は引数に関係なく低画質がリクエストされます。
     *      だたし、第２引数、第３引数に空文字以外を入れた場合は「モバイルデータ接続で最低画質」の設定は無視します。
     * @param isGetComment 「コ　メ　ン　ト　を　取　得　し　な　い　」場合はfalse。省略時はtrueです。
     * @param videoQualityId 画質変更する場合はIDを入れてね。省略（空文字）しても大丈夫です。でもsmileServerLowRequestがtrueならこの値に関係なく低画質になります。
     * @param audioQualityId 音質変更する場合はIDを入れてね。省略（空文字）しても大丈夫です。でもsmileServerLowRequestがtrueならこの値に関係なく低画質になります。
     * @param smileServerLowRequest DMCサーバーじゃなくてSmileサーバーの動画で低画質をリクエストする場合はtrue。DMCサーバーおよびSmileサーバーでも低画質をリクエストしない場合はfalseでいいよ
     * */
    fun coroutine(isGetComment: Boolean = true, videoQualityId: String = "", audioQualityId: String = "", smileServerLowRequest: Boolean = false) {
        // HTML取得
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            if (isAdded) {
                showToast("${getString(R.string.error)}\n${throwable}")
            }
        }
        lifecycleScope.launch(errorHandler) {
            // smileサーバーの動画は多分最初の視聴ページHTML取得のときに?eco=1をつけないと低画質リクエストできない
            val eco = if (smileServerLowRequest) {
                "1"
            } else {
                ""
            }
            // ログインしないならそもそもuserSessionの値を空にすれば！？
            val userSession = if (isLoginMode(context)) {
                this@NicoVideoFragment.userSession
            } else {
                ""
            }
            val response = nicoVideoHTML.getHTML(videoId, userSession, eco)
            // 失敗したら落とす
            if (!response.isSuccessful) {
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            jsonObject = withContext(Dispatchers.Default) {
                nicoVideoHTML.parseJSON(response.body?.string())
            }
            isDMCServer = nicoVideoHTML.isDMCServer(jsonObject)
            // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
            if (isDMCServer) {
                // 公式アニメは暗号化されてて見れないので落とす
                if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                    showToast(getString(R.string.encryption_video_not_play))
                    activity?.finish()
                    return@launch
                } else {

                    // モバイルデータで最低画質をリクエスト！
                    var videoQuality = videoQualityId
                    var audioQuality = audioQualityId

                    // 画質が指定している場合はモバイルデータ接続で最低画質の設定は無視
                    if (videoQuality.isEmpty() && audioQuality.isEmpty()) {
                        // モバイルデータ接続のときは強制的に低画質にする
                        if (prefSetting.getBoolean("setting_nicovideo_low_quality", false)) {
                            if (isConnectionMobileDataInternet(context)) {
                                // モバイルデータ
                                val videoQualityList =
                                    nicoVideoHTML.parseVideoQualityDMC(jsonObject)
                                val audioQualityList =
                                    nicoVideoHTML.parseAudioQualityDMC(jsonObject)
                                videoQuality = videoQualityList.getJSONObject(videoQualityList.length() - 1).getString("id")
                                audioQuality = audioQualityList.getJSONObject(audioQualityList.length() - 1).getString("id")
                            }
                            Snackbar.make(fragment_nicovideo_surfaceview, "${getString(R.string.quality)}：$videoQuality", Snackbar.LENGTH_SHORT).show()
                        }
                    }

                    // https://api.dmc.nico/api/sessions のレスポンス
                    val sessionAPIResponse = nicoVideoHTML.callSessionAPI(jsonObject, videoQuality, audioQuality)
                    if (sessionAPIResponse != null) {
                        sessionAPIJSONObject = sessionAPIResponse
                        // 動画URL
                        contentUrl = nicoVideoHTML.getContentURI(jsonObject, sessionAPIJSONObject)
                        // ハートビート処理。これしないと切られる。
                        nicoVideoHTML.heartBeat(jsonObject, sessionAPIJSONObject)
                        // 選択中の画質、音質控える
                        currentVideoQuality = nicoVideoHTML.getCurrentVideoQuality(sessionAPIJSONObject) ?: ""
                        currentAudioQuality = nicoVideoHTML.getCurrentAudioQuality(sessionAPIJSONObject) ?: ""
                    }
                }
            } else {
                // Smileサーバー。動画URL取得。自動or低画質は最初の視聴ページHTMLのURLのうしろに「?eco=1」をつければ低画質が送られてくる
                contentUrl = nicoVideoHTML.getContentURI(jsonObject, null)
            }
            withContext(Dispatchers.Main) {
                // UI表示
                applyUI()
                // 端末内DB履歴追記
                insertDB(videoTitle)
            }

            // コメント取得
            if (isGetComment) {
                val commentJSON = async {
                    nicoVideoHTML.getComment(videoId, userSession, jsonObject)
                }
                rawCommentList = withContext(Dispatchers.Default) {
                    ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.await()?.body?.string()!!, videoId))
                }
                // フィルターで3ds消したりする
                commentFilter()
            }

            // ニコるくん
            isPremium = nicoVideoHTML.isPremium(jsonObject)
            threadId = nicoVideoHTML.getThreadId(jsonObject)
            userId = nicoVideoHTML.getUserId(jsonObject)
            if (isPremium) {
                val nicoruResponse = nicoruAPI.getNicoruKey(userSession, threadId)
                if (!nicoruResponse.isSuccessful) {
                    showToast("${getString(R.string.error)}\n${nicoruResponse.code}")
                    return@launch
                }
                // nicoruKey!!!
                withContext(Dispatchers.Default) {
                    nicoruAPI.parseNicoruKey(nicoruResponse.body?.string())
                }
            }
            // 関連動画取得。
            val watchRecommendationRecipe = jsonObject.getString("watchRecommendationRecipe")
            val nicoVideoRecommendAPI = NicoVideoRecommendAPI()
            val recommendAPIResponse = nicoVideoRecommendAPI.getVideoRecommend(watchRecommendationRecipe)
            if (!recommendAPIResponse.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            // パース
            withContext(Dispatchers.Default) {
                nicoVideoRecommendAPI.parseVideoRecommend(recommendAPIResponse.body?.string()).forEach {
                    recommendList.add(it)
                }
            }
            // DevNicoVideoRecommendFragmentに配列渡す
            (viewPager.fragmentList[3] as NicoVideoRecommendFragment).apply {
                initRecyclerView()
            }
        }
    }

    /**
     * コメントをフィルターにかける。3DSを消すときに使ってね
     * NGコメント/ユーザー追加時は勝手に更新するので呼ばなくていいです（[setNGDBChangeObserve]参照）。
     * 重そうなのでコルーチン
     * 注意：ViewPagerが初期化済みである必要(applyUIを一回以上呼んである必要)があります。
     * 注意：NG操作が終わったときに呼ぶとうまく動く？。
     * 注意：この関数を呼ぶと勝手にコメント一覧のRecyclerViewも更新されます。（代わりにapplyUI関数からコメントRecyclerView関係を消します）
     * rawCommentListはそのままで、フィルターにかけた結果がcommentListになる
     * @param notify DevNicoVideoCommentFragmentに更新をかけたい時はtrue。コメント一覧Fragmentは別に画面回転処理を書いてるため、画面回転復帰時は使わない。
     * @param showToast トーストを表示させる場合はtrue。DB更新時とかはいらんやろ
     * @param ngList データベース監視でNGユーザー/NGコメントの配列が手に入る場合は入れてください([setNGDBChangeObserve]のときだけ使える)。なければデータベースから取り出します。
     * */
    suspend fun commentFilter(notify: Boolean = true, showToast: Boolean = true, ngList: List<NGDBEntity>? = null) = withContext(Dispatchers.Default) {
        // Fragmentが無い時があるからその時は落とす
        if (!isAdded) return@withContext
        // 3DSけす？
        val is3DSCommentHidden = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        // とりあえず
        commentList = rawCommentList
        // NGコメント。ngList引数が省略時されてるときはDBから取り出す
        val ngCommentList = (ngList ?: NGDBInit.getInstance(requireContext()).ngDBDAO().getNGCommentList())
            .map { ngdbEntity -> ngdbEntity.value }
        // NGユーザー。ngList引数が省略時されてるときはDBから取り出す
        val ngUserList = (ngList ?: NGDBInit.getInstance(requireContext()).ngDBDAO().getNGUserList())
            .map { ngdbEntity -> ngdbEntity.value }
        // NG機能
        commentList = commentList
            .filter { commentJSONParse -> !ngCommentList.contains(commentJSONParse.comment) }
            .filter { commentJSONParse -> !ngUserList.contains(commentJSONParse.userId) } as ArrayList<CommentJSONParse>
        if (is3DSCommentHidden) {
            // device:3DSが入ってるコメント削除。dropWhileでもいい気がする
            commentList = commentList.toList().filter { commentJSONParse -> !commentJSONParse.mail.contains("device:3DS") } as ArrayList<CommentJSONParse>
        }
        if (notify) {
            withContext(Dispatchers.Main) {
                (viewPager.fragmentList[1] as NicoVideoCommentFragment).initRecyclerView(commentList)
                // コメント数表示するか
                if (showToast) {
                    showToast("${getString(R.string.get_comment_count)}：${commentList.size}")
                }
                // コメントキャンバスにも入れる
                fragment_nicovideo_comment_canvas.apply {
                    rawCommentList.clear()
                    exoPlayer = this@NicoVideoFragment.exoPlayer
                    rawCommentList = commentList
                }
            }
        }
    }

    /**
     * データ取得終わった時にUIに反映させる
     * */
    suspend fun applyUI() = withContext(Dispatchers.Main) {
        // ここに来る頃にはFragmentがもう存在しない可能性があるので（速攻ブラウザバックなど）
        if (!isAdded) return@withContext
        // プレイヤー右上のアイコンにWi-Fiアイコンがあるけどあれ、どの方法で再生してるかだから。キャッシュならフォルダーになる
        val playingTypeDrawable = when {
            isCache -> requireContext().getDrawable(R.drawable.ic_folder_open_black_24dp)
            isConnectionMobileDataInternet(context) -> requireContext().getDrawable(R.drawable.ic_signal_cellular_alt_black_24dp)
            else -> requireContext().getDrawable(R.drawable.ic_wifi_black_24dp)
        }
        player_control_video_network.setImageDrawable(playingTypeDrawable)
        player_control_video_network.setOnClickListener {
            val isUnlimitedNetwork = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when {
                    isConnectionNetworkTypeUnlimited(context) -> "現在のネットワーク：定額制（食べ放題）"
                    else -> "現在のネットワーク：従量制（パケ死）"
                }
            } else {
                ""
            }
            val message = when {
                isCache -> "キャッシュで再生しています"
                isConnectionMobileDataInternet(context) -> "モバイルデータを利用して再生しています。\n$isUnlimitedNetwork"
                isConnectionWiFiInternet(context) -> "Wi-Fiを利用して再生しています。\n$isUnlimitedNetwork"
                else -> "ネットワーク接続方法がわかりませんでした。"
            }
            showToast(message)
        }
        // タイトル
        videoTitle = if (isInitJsonObject()) jsonObject.getJSONObject("video").getString("title") else ""
        initTitleArea()
        // タイトルなど
        player_control_title.text = videoTitle
        player_control_id.text = videoId
        // ViewPager初期化
        if (prefSetting.getBoolean("setting_nicovideo_comment_only", false)) {
            // 動画を再生しない場合
            commentOnlyModeEnable()
        } else {
            // ExoPlayer
            initVideoPlayer(contentUrl, nicoHistory)
        }
        // 動画情報なければ終わる
        if (!::jsonObject.isInitialized) return@withContext
        // メニューにJSON渡す
        (viewPager.fragmentList[0] as NicoVideoMenuFragment).jsonObject = jsonObject
        // 動画情報あれば更新
        if (::jsonObject.isInitialized) {
            (viewPager.fragmentList[2] as NicoVideoInfoFragment).apply {
                jsonObjectString = jsonObject.toString()
                parseJSONApplyUI(jsonObjectString)
            }
        }
        // 共有
        share = ProgramShare((activity as AppCompatActivity), fragment_nicovideo_surfaceview, videoTitle, videoId)
        if (!isCache) {
            // キャッシュ再生時以外
            // 投稿動画をViewPagerに追加
            viewPagerAddAccountFragment(jsonObject)
        }
        // リピートボタン
        player_control_repeat.setOnClickListener {
            when (exoPlayer.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    // リピート無効時
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    player_control_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
                }
                Player.REPEAT_MODE_ONE -> {
                    // リピート有効時
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    player_control_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_black_24dp))
                }
            }
        }
        // ログイン切れてるよメッセージ（プレ垢でこれ食らうと画質落ちるから；；）
        if (isLoginMode(context) && !nicoVideoHTML.verifyLogin(jsonObject)) {
            showSnackbar(getString(R.string.login_disable_message), getString(R.string.login)) {
                lifecycleScope.launch {
                    NicoLogin.reNicoLogin(context)
                    activity?.runOnUiThread {
                        activity?.finish()
                        val intent = Intent(context, NicoVideoActivity::class.java)
                        intent.putExtra("id", videoId)
                        intent.putExtra("cache", isCache)
                        context?.startActivity(intent)
                    }
                }
            }
        }
    }

    /** 端末内履歴に書き込む */
    private fun insertDB(videoTitle: String) {
        // Roomはメインスレッドでは扱えない
        lifecycleScope.launch(Dispatchers.IO) {
            val unixTime = System.currentTimeMillis() / 1000
            // 入れるデータ
            val publisherId = nicoVideoHTML.getUploaderId(jsonObject)
            val nicoHistoryDBEntity = NicoHistoryDBEntity(
                type = "video",
                serviceId = videoId,
                userId = publisherId,
                title = videoTitle,
                unixTime = unixTime,
                description = ""
            )
            // 追加
            NicoHistoryDBInit.getInstance(requireContext()).nicoHistoryDBDAO().insert(nicoHistoryDBEntity)
        }
    }

    // Snackbarを表示させる関数
// 第一引数はnull禁止。第二、三引数はnullにするとSnackBarのボタンが表示されません
    fun showSnackbar(message: String, clickMessage: String?, click: (() -> Unit)?) {
        Snackbar.make(fragment_nicovideo_surfaceview, message, Snackbar.LENGTH_SHORT).apply {
            if (clickMessage != null && click != null) {
                setAction(clickMessage) { click() }
            }
            show()
        }
    }

    /**
     * キャッシュ再生
     * */
    fun cachePlay() {
        // コメントファイルがxmlならActivity終了
        val xmlCommentJSON = XMLCommentJSON(context)
        if (xmlCommentJSON.commentXmlFilePath(videoId) != null && !xmlCommentJSON.commentJSONFileExists(videoId)) {
            // xml形式はあるけどjson形式がないときは落とす
            Toast.makeText(context, R.string.xml_comment_play, Toast.LENGTH_SHORT).show()
            activity?.finish()
            return
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                // 動画のファイル名取得
                val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                if (videoFileName != null) {
                    contentUrl = "${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName"
                    if (prefSetting.getBoolean("setting_nicovideo_comment_only", false)) {
                        // 動画を再生しない場合
                        commentOnlyModeEnable()
                    } else {
                        // withContext(Dispatchers.Main) {
                        //     // キャッシュで再生だよ！
                        //     showToast(getString(R.string.use_cache))
                        // }
                    }
                    // 動画情報
                    if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                        jsonObject = JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId))
                    }
                    // コメント取得。重いので async。この中の処理（今回はコメントJSON解析）は並列で実行される。loadCommentAsync.await()のところで合流する（止まる）
                    val loadCommentAsync = async {
                        val commentJSONFilePath = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                        nicoVideoHTML.parseCommentJSON(commentJSONFilePath, videoId)
                    }
                    // UIスレッドへ
                    withContext(Dispatchers.Main) {
                        // 再生
                        applyUI()
                        // タイトル
                        videoTitle = if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                            JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId)).getJSONObject("video").getString("title")
                        } else {
                            // 動画ファイルの名前
                            nicoVideoCache.getCacheFolderVideoFileName(videoId) ?: videoId
                        }
                        initTitleArea()
                        // フィルターで3ds消したりする。が、コメントは並列で読み込んでるので、並列で作業してるコメント取得を待つ（合流する）
                        rawCommentList = ArrayList(loadCommentAsync.await())
                        commentFilter()
                    }
                } else {
                    // 動画が見つからなかった
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.not_found_video))
                        activity?.finish()
                    }
                    return@launch
                }
            }
        }
    }

    /**
     * コメントのみの表示に切り替える
     * */
    fun commentOnlyModeEnable() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        fragment_nicovideo_framelayout.visibility = View.GONE
        hideSwipeToRefresh()
    }

    /**
     * コメントのみの表示を無効にする。動画を再生する
     * */
    fun commentOnlyModeDisable() {
        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        if (isCache) {
            initVideoPlayer(contentUrl, "")
        } else {
            initVideoPlayer(contentUrl, nicoHistory)
        }
        fragment_nicovideo_framelayout.visibility = View.VISIBLE
        showSwipeToRefresh()
    }

    /**
     * アスペクト比を合わせる
     * 注意：DMCサーバーの動画でのみ利用可能です。smileサーバー限定ってほぼ見なくなったよね。
     * @param jsonObject parseJSON()の返り値
     * @param sessionJSONObject callSessionAPI()の返り値
     * */
    private fun initAspectRate(jsonObject: JSONObject, sessionJSONObject: JSONObject) {
        // 選択中の画質
        val currentQuality = nicoVideoHTML.getCurrentVideoQuality(sessionAPIJSONObject)
        // 利用可能な画質パース
        val videoQualityList = nicoVideoHTML.parseVideoQualityDMC(jsonObject)
        // 選択中の画質を一つずつ見ていく
        for (i in 0 until videoQualityList.length()) {
            val qualityObject = videoQualityList.getJSONObject(i)
            val id = qualityObject.getString("id")
            if (id == currentQuality) {
                // あった！
                val width = qualityObject.getJSONObject("resolution").getInt("width")
                val height = qualityObject.getJSONObject("resolution").getInt("height")
                // アスペクト比が4:3か16:9か
                // 4:3 = 1.333... 16:9 = 1.777..
                val calc = width.toFloat() / height.toFloat()
                // 小数点第二位を捨てる
                val round = BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                setAspectRate(round)
            }
        }
    }

    /**
     * ExoPlayer初期化
     * */
    private fun initVideoPlayer(videoUrl: String?, nicohistory: String?) {
        isRotationProgressSuccessful = false
        exoPlayer.setVideoSurfaceView(fragment_nicovideo_surfaceview)
        // キャッシュ再生と分ける
        when {
            // キャッシュを優先的に利用する　もしくは　キャッシュ再生時
            isCache -> {
                // キャッシュ再生
                val dataSourceFactory = DefaultDataSourceFactory(context, "TatimiDroid;@takusan_23")
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(videoUrl?.toUri())
                exoPlayer.prepare(videoSource)
            }
            // それ以外：インターネットで取得
            else -> {
                // SmileサーバーはCookieつけないと見れないため
                val dataSourceFactory = DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
                dataSourceFactory.defaultRequestProperties.set("Cookie", nicohistory)
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(videoUrl?.toUri())
                exoPlayer.prepare(videoSource)
            }
        }
        // 自動再生
        exoPlayer.playWhenReady = true
        // リピートするか
        if (prefSetting.getBoolean("nicovideo_repeat_on", true)) {
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            player_control_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
        }
        // 再生ボタン押したとき
        player_control_pause.setOnClickListener {
            // コメント流し制御
            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            // アイコン入れ替え
            setPlayIcon()
        }
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                setPlayIcon()
                if (playbackState == Player.STATE_BUFFERING) {
                    // STATE_BUFFERING はシークした位置からすぐに再生できないとき。読込み中のこと。
                    showSwipeToRefresh()
                } else {
                    hideSwipeToRefresh()
                }
                if (!isRotationProgressSuccessful) {
                    // 一度だけ実行するように。画面回転前の時間を適用する
                    initController()
                    exoPlayer.seekTo(rotationProgress)
                    isRotationProgressSuccessful = true
                    // 前回見た位置から再生
                    // 画面回転時に２回目以降表示されると邪魔なので制御
                    if (rotationProgress == 0L) {
                        val progress = prefSetting.getLong("progress_$videoId", 0)
                        if (progress != 0L && isCache) {
                            Snackbar.make(fragment_nicovideo_surfaceview, "${getString(R.string.last_time_position_message)}(${DateUtils.formatElapsedTime(progress / 1000L)})", Snackbar.LENGTH_LONG).apply {
                                setAction(R.string.play) {
                                    exoPlayer.seekTo(progress)
                                }
                                show()
                            }
                        }
                    }
                }
            }
        })
        // 縦、横取得
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                // ここはsmileサーバーの動画のみで利用されるコードです。DMCサーバーではHeight/Widthが取得可能なので。
                // アスペクト比が4:3か16:9か
                // 4:3 = 1.333... 16:9 = 1.777..
                val calc = width.toFloat() / height.toFloat()
                // 小数点第二位を捨てる
                val round = BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                // 16:9なら
                is169AspectLate = round == 1.7
                // Fragmentが息してて 全画面と横画面　でなければ　アスペクト比を直す
                if (isAdded && !(::devNicoVideoFragmentData.isInitialized && (devNicoVideoFragmentData.isFullScreenMode && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE))) {
                    // アスペ比直す
                    setAspectRate(round)
                }
            }
        })

        var secInterval = 0L

        seekTimer.cancel()
        seekTimer = Timer()
        seekTimer.schedule(timerTask {
            Handler(Looper.getMainLooper()).post {
                if (exoPlayer.isPlaying) {
                    setProgress()
                    // drawComment()
                    val sec = exoPlayer.currentPosition / 1000
                    // コメント一覧スクロールする
                    if (prefSetting.getBoolean("setting_oikakeru_hide", false)) {
                        requireCommentFragment()?.apply {
                            // 追いかけるボタン利用しない
                            scroll(exoPlayer.currentPosition)
                            // 使わないので非表示
                            setFollowingButtonVisibility(false)
                        }
                    } else {
                        // 追いかけるボタン表示するかなどをやってる関数
                        // 一秒ごとに動かしたい
                        if (secInterval != sec) {
                            secInterval = sec
                            requireCommentFragment()?.setScrollFollowButton(exoPlayer.currentPosition)
                        }
                    }

                }
            }
        }, 100, 100)
    }

    // アスペクト比合わせる。引数は 横/縦 の答え
    private fun setAspectRate(round: Double) {
        // 画面の幅取得。令和最新版（ビリビリワイヤレスイヤホン並感）
        val displayWidth = DisplaySizeTool.getDisplayWidth(context)
        // 縦画面だったらtrue
        val isPortLait = context?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT
        when (round) {
            1.0 -> {
                // 縦も横も同じ
                fragment_nicovideo_framelayout.updateLayoutParams {
                    width = if (isPortLait) {
                        // 縦
                        (displayWidth / 1.5).toInt()
                    } else {
                        // 横
                        displayWidth / 2
                    }
                    height = width
                }
            }
            1.3 -> {
                // 4:3動画
                // 4:3をそのまま出すと大きすぎるので調整（代わりに黒帯出るけど仕方ない）
                fragment_nicovideo_framelayout.updateLayoutParams {
                    width = if (isPortLait) {
                        // 縦
                        (displayWidth / 1.2).toInt()
                    } else {
                        // 横
                        displayWidth / 2
                    }
                    height = getOldAspectHeightFromWidth(width)
                }
            }
            1.7 -> {
                // 16:9動画
                // 横の長さから縦の高さ計算
                fragment_nicovideo_framelayout.updateLayoutParams {
                    width = if (isPortLait) {
                        // 縦
                        displayWidth
                    } else {
                        // 横
                        displayWidth / 2
                    }
                    height = getAspectHeightFromWidth(width)
                }
            }
        }
    }

    /**
     * コメント一覧Fragmentを取得する。無い可能性も有る？
     * */
    fun requireCommentFragment() = (viewPager.fragmentList[1] as? NicoVideoCommentFragment)

    /**
     * アイコン入れ替え
     * */
    private fun setPlayIcon() {
        val drawable = if (exoPlayer.playWhenReady) {
            context?.getDrawable(R.drawable.ic_pause_black_24dp)
        } else {
            context?.getDrawable(R.drawable.ic_play_arrow_24px)
        }
        player_control_pause.setImageDrawable(drawable)
    }

    /**
     * 進捗進める
     * */
    private fun setProgress() {
        // シークバー操作中でなければ
        if (player_control_seek != null && !isTouchSeekBar) {
            player_control_seek.progress = (exoPlayer.currentPosition / 1000L).toInt()
        }
        // 再生時間TextView
        // val simpleDateFormat = SimpleDateFormat("hh:mm:ss", Locale("UTC"))
        val formattedTime = DateUtils.formatElapsedTime(exoPlayer.currentPosition / 1000L)
        player_control_current.text = formattedTime
    }

    /**
     * 16:9で横の大きさがわかるときに縦の大きさを返す
     * @param width 横
     * @return 高さ
     * */
    private fun getAspectHeightFromWidth(width: Int): Int {
        val heightCalc = width / 16
        return heightCalc * 9
    }

    /**
     * 4:3で横の長さがわかるときに縦の長さを返す
     * @param width 横
     * @return 高さ
     * */
    private fun getOldAspectHeightFromWidth(width: Int): Int {
        val heightCalc = width / 4
        return heightCalc * 3
    }

    /**
     * 16:9で縦の大きさが分かる時に横の大きさを返す。
     * @param height 高さ
     * @return 幅
     * */
    private fun getAspectWidthFromHeight(height: Int): Int {
        val widthCalc = height / 9
        return widthCalc * 16
    }

    /**
     * 4:3で縦の大きさが分かる時に横の大きさを返す。
     * @param height 高さ
     * @return はば
     * */
    private fun getOldAspectWidthFromHeight(height: Int): Int {
        val widthCalc = height / 3
        return widthCalc * 4
    }


    /**
     * ViewPager初期化
     * 注意：キャッシュ再生かどうか、動画IDがちゃんとある状態で実行しないとうまく動きません。
     * @param dynamicAddFragmentList 動的に追加したFragmentがある場合は入れてね。なければ省略していいです。
     * */
    private fun initViewPager(dynamicAddFragmentList: ArrayList<TabLayoutData> = arrayListOf()) {
        // このFragmentを置いたときに付けたTag
        viewPager = NicoVideoRecyclerPagerAdapter(activity as AppCompatActivity, videoId, isCache, dynamicAddFragmentList)
        fragment_nicovideo_viewpager.adapter = viewPager
        TabLayoutMediator(fragment_nicovideo_tablayout, fragment_nicovideo_viewpager, TabLayoutMediator.TabConfigurationStrategy { tab, position ->
            tab.text = viewPager.fragmentTabName[position]
        }).attach()
        // コメントを指定しておく
        fragment_nicovideo_viewpager.currentItem = 1
    }

    /**
     * ViewPagerにアカウントを追加する
     * */
    private fun viewPagerAddAccountFragment(jsonObject: JSONObject) {
        if (!jsonObject.isNull("owner")) {
            val ownerObject = jsonObject.getJSONObject("owner")
            val userId = ownerObject.getInt("id").toString()
            val nickname = ownerObject.getString("nickname")
            // DevNicoVideoFragment
            val postFragment = NicoVideoPOSTFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                }
            }
            // すでにあれば追加しない
            if (!viewPager.fragmentTabName.contains(nickname)) {
                viewPager.addFragment(postFragment, nickname) // Fragment追加関数
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = true
            comment_canvas?.isPause = false
        }
    }

    override fun onPause() {
        super.onPause()
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = false
            // キャッシュ再生の場合は位置を保存する
            if (isCache) {
                prefSetting.edit {
                    putLong("progress_$videoId", exoPlayer.currentPosition)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fragment終了
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        heartBeatTimer.cancel()
        nicoVideoCache.destroy()
        nicoVideoHTML.destory()
        isDestory = true
        seekTimer.cancel()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ProgramShare.requestCode -> {
                //画像共有
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        val uri: Uri = data.data!!
                        //保存＆共有画面表示
                        share.saveActivityResult(uri)
                    }
                }
            }
        }
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * onSaveInstanceState / onViewStateRestored を使って画面回転に耐えるアプリを作る。
     * */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存する値をセット。今回は再生時間
        with(outState) {
            val data = DevNicoVideoFragmentData(
                isCachePlay = isCache,
                contentUrl = contentUrl,
                nicoHistory = nicoHistory,
                commentList = rawCommentList,
                currentPos = exoPlayer.currentPosition,
                // 動画情報がないときはnull
                dataApiData = if (::jsonObject.isInitialized) {
                    jsonObject.toString()
                } else {
                    null
                },
                // キャッシュ再生時はこの値初期化されないので
                sessionAPIJSONObject = if (::sessionAPIJSONObject.isInitialized) {
                    sessionAPIJSONObject.toString()
                } else {
                    null
                },
                nicoruKey = nicoruAPI.nicoruKey,
                recommendList = recommendList,
                isFullScreenMode = isFullScreenMode,
                is169AspectLate = is169AspectLate
            )
            putSerializable("data", data)
            putParcelableArrayList("tab", viewPager.dynamicAddFragmentList)
        }
    }

    /**
     * ExoPlayerを初期化しているか
     * */
    fun isInitExoPlayer(): Boolean {
        return ::exoPlayer.isInitialized
    }

    fun isInitJsonObject(): Boolean = ::jsonObject.isInitialized

    /**
     * 画面が横かどうかを返す。横ならtrue
     * */
    fun isLandscape() = requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

}
