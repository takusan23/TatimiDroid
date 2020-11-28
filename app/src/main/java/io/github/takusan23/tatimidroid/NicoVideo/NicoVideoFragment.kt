package io.github.takusan23.tatimidroid.NicoVideo

import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.Adapter.Parcelable.TabLayoutData
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.MainActivityPlayerFragmentInterface
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoRecyclerPagerAdapter
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoPlayListBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoPOSTFragment
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoSeriesFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModelFactory
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.*
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.include_nicovideo_player_controller.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.roundToInt

/**
 * 開発中のニコ動クライアント（？）
 *
 * id           |   動画ID。必須
 * --- 任意 ---
 * cache        |   キャッシュ再生ならtrue。なければfalse
 * eco          |   エコノミー再生するなら（?eco=1）true
 * internet     |   キャッシュ有っても強制的にインターネットを利用する場合はtrue
 * fullscreen   |   最初から全画面で再生する場合は true。
 * */
class NicoVideoFragment : Fragment(), MainActivityPlayerFragmentInterface {

    lateinit var prefSetting: SharedPreferences
    lateinit var darkModeSupport: DarkModeSupport

    // ViewPager
    lateinit var viewPager: NicoVideoRecyclerPagerAdapter

    // 再生時間を適用したらtrue。一度だけ動くように
    var isRotationProgressSuccessful = false

    // フォント
    lateinit var font: CustomFont

    // シーク操作中かどうか
    var isTouchSeekBar = false

    /**
     * MVVM ｲｸｿﾞｵｵｵｵｵ！
     * データを置くためのクラスです。これは画面回転しても値を失うことはないです。
     * */
    lateinit var viewModel: NicoVideoViewModel

    /** 動画ID。ViewModel初期化までは空っぽ */
    var videoId = ""

    /** キャッシュ再生かどうか。ViewModel初期化までは不明 */
    var isCache = false

    private var seekTimer = Timer()

    val exoPlayer by lazy { SimpleExoPlayer.Builder(requireContext()).build() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(requireContext())

        // ふぉんと
        font = CustomFont(context)
        // CommentCanvasにも適用するかどうか
        if (font.isApplyFontFileToCommentCanvas) {
            fragment_nicovideo_comment_canvas.typeFace = font.typeface
        }

        // スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // くーるくるー
        showSwipeToRefresh()

        // ダークモード
        initDarkmode()

        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(requireActivity(), lifecycle)
        }

        // 動画ID
        val videoId = arguments?.getString("id")
        // キャッシュ再生
        val isCache = arguments?.getBoolean("cache")
        // エコノミー再生
        val isEconomy = arguments?.getBoolean("eco") ?: false
        // 強制的にインターネットを利用して取得
        val useInternet = arguments?.getBoolean("internet") ?: false
        // 全画面で開始
        val isStartFullScreen = arguments?.getBoolean("fullscreen") ?: false
        // 連続再生？
        val videoList = arguments?.getSerializable("video_list") as? ArrayList<NicoVideoData>

        // ViewModel初期化
        viewModel = ViewModelProvider(this, NicoVideoViewModelFactory(requireActivity().application, videoId, isCache, isEconomy, useInternet, isStartFullScreen, videoList)).get(NicoVideoViewModel::class.java)

        // 全画面モードなら
        if (viewModel.isFullScreenMode) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            setFullScreen()
        }

        // ミニプレイヤーなら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // MainActivityのBottomNavを表示させるか
            (requireActivity() as MainActivity).setVisibilityBottomNav(isMiniPlayerMode)
            setMiniPlayer(isMiniPlayerMode)
            // アイコン直す
            val icon = when (fragment_nicovideo_motionlayout.currentState) {
                R.id.fragment_nicovideo_transition_end -> requireContext().getDrawable(R.drawable.ic_expand_less_black_24dp)
                else -> requireContext().getDrawable(R.drawable.ic_expand_more_24px)
            }
            player_control_back_button.setImageDrawable(icon)
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                fragment_nicovideo_motionlayout.transitionToState(R.id.fragment_nicovideo_transition_end)
            }
        }

        // 動画ID変更受け取り。
        viewModel.playingVideoId.observe(viewLifecycleOwner) {
            this.videoId = it
        }

        // キャッシュ再生かどうか。ついでに連続再生の動画変更イベントもここで
        viewModel.isOfflinePlay.observe(viewLifecycleOwner) {
            this.isCache = it
            // 動画変更時にやることリスト
            initViewPager(viewModel.dynamicAddFragmentList)
            fragment_nicovideo_comment_canvas.clearCommentList()
            player_control_playlist.isVisible = viewModel.isPlayListMode
        }

        // Activity終了などのメッセージ受け取り
        viewModel.messageLiveData.observe(viewLifecycleOwner) {
            when (it) {
                getString(R.string.encryption_video_not_play) -> finishFragment()
            }
        }

        // SnackBarを表示しろメッセージを受け取る
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) {
            Snackbar.make(fragment_nicovideo_surfaceview, it, Snackbar.LENGTH_SHORT).show()
        }

        if (viewModel.isCommentOnlyMode) {
            setCommentOnlyMode(true)
        } else {
            // 動画再生
            viewModel.contentUrl.observe(viewLifecycleOwner) { contentUrl ->
                val oldPosition = exoPlayer.currentPosition
                playExoPlayer(contentUrl)
                // 画質変更時は途中から再生。動画IDが一致してないとだめ
                if (oldPosition > 0 && exoPlayer.currentMediaItem?.mediaId == videoId) {
                    exoPlayer.seekTo(oldPosition)
                }
                exoPlayer.setVideoSurfaceView(fragment_nicovideo_surfaceview)
                // コメントキャンバスにも入れる
                fragment_nicovideo_comment_canvas.exoPlayer = this@NicoVideoFragment.exoPlayer
            }
        }

        // コメント
        viewModel.commentList.observe(viewLifecycleOwner) { commentList ->
            fragment_nicovideo_comment_canvas.rawCommentList = commentList
        }

        // 動画情報
        viewModel.nicoVideoData.observe(viewLifecycleOwner) { nicoVideoData ->
            // ViewPager
            initUI(nicoVideoData)
        }

        // ViewPager追加など
        viewModel.nicoVideoJSON.observe(viewLifecycleOwner) { json ->
            if (viewModel.isOfflinePlay.value == false) {
                viewPagerAddAccountFragment(json)
            }
        }

        // コントローラー用意
        initController()

    }

    /** UIに反映させる */
    private fun initUI(nicoVideoData: NicoVideoData) {
        // プレイヤー右上のアイコンにWi-Fiアイコンがあるけどあれ、どの方法で再生してるかだから。キャッシュならフォルダーになる
        val playingTypeDrawable = when {
            nicoVideoData.isCache -> requireContext().getDrawable(R.drawable.ic_folder_open_black_24dp)
            else -> InternetConnectionCheck.getConnectionTypeDrawable(requireContext())
        }
        player_control_video_network.setImageDrawable(playingTypeDrawable)
        player_control_video_network.setOnClickListener {
            // なんの方法（キャッシュ・モバイルデータ・Wi-Fi）で再生してるかを表示する
            val message = if (nicoVideoData.isCache) {
                getString(R.string.use_cache)
            } else {
                InternetConnectionCheck.createNetworkMessage(requireContext())
            }
            showToast(message)
        }
        player_control_title.text = nicoVideoData.title
        // Marqueeを有効にするにはフォーカスをあてないといけない？。<marquee>とかWeb黎明期感ある（その時代の人じゃないけど）
        player_control_title.isSelected = true
        player_control_id.text = nicoVideoData.videoId
        // リピートボタン
        player_control_repeat.setOnClickListener {
            when (exoPlayer.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    // リピート無効時
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    player_control_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
                    prefSetting.edit { putBoolean("nicovideo_repeat_on", true) }
                }
                Player.REPEAT_MODE_ONE -> {
                    // リピート有効時
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    player_control_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_black_24dp))
                    prefSetting.edit { putBoolean("nicovideo_repeat_on", false) }
                }
            }
        }
    }

    private fun playExoPlayer(contentUrl: String) {
        // キャッシュ再生と分ける
        when {
            // キャッシュを優先的に利用する　もしくは　キャッシュ再生時
            viewModel.isOfflinePlay.value ?: false -> {
                // キャッシュ再生
                val dataSourceFactory = DefaultDataSourceFactory(requireContext(), "TatimiDroid;@takusan_23")
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(videoId).build())
                exoPlayer.setMediaSource(videoSource)
            }
            // それ以外：インターネットで取得
            else -> {
                // SmileサーバーはCookieつけないと見れないため
                val dataSourceFactory = DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
                dataSourceFactory.defaultRequestProperties.set("Cookie", viewModel.nicoHistory)
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(videoId).build())
                exoPlayer.setMediaSource(videoSource)
            }
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        // リピートするか
        if (prefSetting.getBoolean("nicovideo_repeat_on", true)) {
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            player_control_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
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
                if (playbackState == Player.STATE_ENDED && playWhenReady) {
                    // 動画おわった。連続再生時なら次の曲へ
                    viewModel.nextVideo()
                }
                if (!isRotationProgressSuccessful) {
                    // 一度だけ実行するように。画面回転前の時間を適用する
                    isRotationProgressSuccessful = true
                    // 前回見た位置から再生
                    exoPlayer.seekTo(viewModel.currentPosition)
                    fragment_nicovideo_comment_canvas.seekComment()
                    if (viewModel.currentPosition == 0L) {
                        // 画面回転時に２回目以降表示されると邪魔なので制御
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
                // DMCのJSONからも幅とかは取れるけどキャッシュ再生でJSONがない場合をサポートしたいため
                if (isAdded) { // コールバックなのでこの時点でもう無いかもしれない
                    aspectRatioFix(width, height)
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

    /** アスペクト比を直す関数 */
    private fun aspectRatioFix(width: Int, height: Int) {
        val displayWidth = DisplaySizeTool.getDisplayWidth(requireContext())
        val displayHeight = DisplaySizeTool.getDisplayHeight(requireContext())
        if (isLandscape()) {
            // 横画面
            var playerWidth = displayWidth / 2
            var playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, playerWidth).roundToInt()
            // 縦動画の場合は調整する
            if (playerHeight > displayHeight) {
                playerWidth /= 2
                playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, playerWidth).roundToInt()
            }
            // レイアウト調整。プレイヤーのFrameLayoutのサイズを変える
            fragment_nicovideo_motionlayout.getConstraintSet(R.id.fragment_nicovideo_transition_start).apply {
                constrainHeight(R.id.fragment_nicovideo_surfaceview, playerHeight)
                constrainWidth(R.id.fragment_nicovideo_surfaceview, playerWidth)
            }
            // ミニプレイヤーも
            fragment_nicovideo_motionlayout.getConstraintSet(R.id.fragment_nicovideo_transition_end).apply {
                constrainHeight(R.id.fragment_nicovideo_background, (playerHeight / 1.5).roundToInt())
                constrainWidth(R.id.fragment_nicovideo_background, (playerWidth / 1.5).roundToInt())
            }
            fragment_nicovideo_motionlayout.getConstraintSet(R.id.fragment_nicovideo_transition_finish).apply {
                constrainHeight(R.id.fragment_nicovideo_background, (playerHeight / 1.5).roundToInt())
                constrainWidth(R.id.fragment_nicovideo_background, (playerWidth / 1.5).roundToInt())
            }
            // 全画面UI
            fragment_nicovideo_motionlayout.getConstraintSet(R.id.fragment_nicovideo_transition_fullscreen).apply {
                val fullScreenWidth = viewModel.nicoVideoHTML.calcVideoWidthDisplaySize(width, height, displayHeight).toInt()
                constrainHeight(R.id.fragment_nicovideo_surfaceview, displayHeight)
                constrainWidth(R.id.fragment_nicovideo_surfaceview, fullScreenWidth)
            }
        } else {
            // 縦画面
            var playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, displayWidth).roundToInt()
            var playerWidth = displayWidth
            // 縦動画の場合は調整する
            if (playerHeight > displayWidth) {
                playerWidth /= 2
                playerHeight = viewModel.nicoVideoHTML.calcVideoHeightDisplaySize(width, height, playerWidth).roundToInt()
            }
            // レイアウト調整。プレイヤーのFrameLayoutのみ。背景はプレイヤーFrameLayoutの高さに合うように制約が設定してある。幅は最大
            fragment_nicovideo_motionlayout.getConstraintSet(R.id.fragment_nicovideo_transition_start).apply {
                constrainHeight(R.id.fragment_nicovideo_surfaceview, playerHeight)
                constrainWidth(R.id.fragment_nicovideo_surfaceview, playerWidth)
            }
            // ミニプレイヤーも
            fragment_nicovideo_motionlayout.getConstraintSet(R.id.fragment_nicovideo_transition_end).apply {
                constrainHeight(R.id.fragment_nicovideo_background, playerHeight / 2)
                constrainWidth(R.id.fragment_nicovideo_background, playerWidth / 2)
            }
            fragment_nicovideo_motionlayout.getConstraintSet(R.id.fragment_nicovideo_transition_finish).apply {
                constrainHeight(R.id.fragment_nicovideo_background, playerHeight / 2)
                constrainWidth(R.id.fragment_nicovideo_background, playerWidth / 2)
            }
        }
    }

    /**
     * フルスクリーンへ移行。
     * 先日のことなんですけども、ついに（待望）（念願の）（満を持して）、わたくしの動画が、無断転載されてました（ﾄﾞｩﾋﾟﾝ）
     * ↑これ sm29392869 全画面で見れるようになる
     * 現状横画面のみ
     * */
    private fun setFullScreen() {
        viewModel.isFullScreenMode = true
        player_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp))
        // コメント一覧非表示
        fragment_nicovideo_motionlayout.transitionToState(R.id.fragment_nicovideo_transition_fullscreen)
        // システムバー消す
        setSystemBarVisibility(false)
        // 背景を黒く
        fragment_nicovideo_background.background = ColorDrawable(Color.BLACK)
    }

    /** フルスクリーン解除 */
    private fun setCloseFullScreen() {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        viewModel.isFullScreenMode = false
        player_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_black_24dp))
        // コメント一覧表示
        fragment_nicovideo_motionlayout.transitionToState(R.id.fragment_nicovideo_transition_start)
        // システムバー表示
        setSystemBarVisibility(true)
        // 背景戻す
        fragment_nicovideo_background.background = ColorDrawable(getThemeColor(requireContext()))
    }

    /**
     * システムバーを非表示にする関数
     * システムバーはステータスバーとナビゲーションバーのこと。多分
     * @param isShow 表示する際はtrue。非表示の際はfalse
     * */
    private fun setSystemBarVisibility(isShow: Boolean) {
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

    /** コントローラー初期化。ミニプレイヤーをスワイプして終了はこの関数を呼ばないと使えない */
    fun initController() {
        // コントローラーを消すためのコルーチン
        val job = Job()
        // 戻るボタン
        player_control_back_button.isVisible = true
        player_control_back_button.setOnClickListener {
            // 最小化するとかしないとか
            fragment_nicovideo_motionlayout.apply {
                when {
                    viewModel.isFullScreenMode -> {
                        setCloseFullScreen()
                    }
                    currentState == R.id.fragment_nicovideo_transition_start -> {
                        transitionToState(R.id.fragment_nicovideo_transition_end)
                    }
                    else -> {
                        transitionToState(R.id.fragment_nicovideo_transition_start)
                    }
                }
            }
        }
        // MotionLayoutでスワイプできない対策に独自FrameLayoutを作った
        fragment_nicovideo_motionlayout_parent_framelayout.apply {
            allowIdList.add(R.id.fragment_nicovideo_transition_start) // 通常状態（コメント表示など）は無条件でタッチを渡す。それ以外はプレイヤー部分のみタッチ可能
            allowIdList.add(R.id.fragment_nicovideo_transition_fullscreen) // フルスクリーン時もクリックが行かないように
            swipeTargetView = fragment_nicovideo_surfaceview
            motionLayout = fragment_nicovideo_motionlayout
            // プレイヤーを押した時。普通にsetOnClickListenerとか使うと競合して動かなくなる
            onSwipeTargetViewClickFunc = {
                player_control_main?.isVisible = !player_control_main.isVisible
                // あとなんかMotionLayoutが有効の時はシークバーとかが押せなくなるので表示中は一旦Transition無効化。
                MotionLayoutTool.allTransitionEnable(fragment_nicovideo_motionlayout, !player_control_main.isVisible)
                // ３秒待ってもViewが表示されてる場合は消せるように。
                updateHideController(job)
            }
            onSwipeTargetViewDoubleClickFunc = { ev ->
                // player_control_parentでコントローラーのUIが表示されてなくてもスキップできるように
                if (ev != null) {
                    val skip = if (ev.x > player_control_center_parent.width / 2) {
                        // 半分より右
                        (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000 // 秒→ミリ秒
                    } else {
                        // 半分より左
                        -(prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000 // 秒→ミリ秒
                    }
                    exoPlayer.seekTo(exoPlayer.currentPosition + skip)
                    fragment_nicovideo_comment_canvas.seekComment()
                    updateHideController(job)
                }
            }
            // swipeTargetViewの上にあるViewをここに書く。ここに書いたViewを押した際はonSwipeTargetViewClickFuncが呼ばれなくなる(View#setOnClickListenerは呼ばれる)
            addAllIsClickableViewFromParentView(player_control_main)
            blockViewList.add(player_control_seek)
            // blockViewListに追加したViewが押さてたときに共通で行いたい処理などを書く
            onBlockViewClickFunc = { view, event ->
                player_control_main?.apply {
                    // UI非表示なら表示
                    if (!isVisible) {
                        onSwipeTargetViewClickFunc?.invoke()
                    }
                }
            }
        }
        // 再生/一時停止
        player_control_pause.setOnClickListener {
            // 再生ボタン押したとき
            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            // アイコン入れ替え
            setPlayIcon()
        }
        // 連続再生とそれ以外でアイコン変更
        val playListModePrevIcon = if (viewModel.isPlayListMode) requireContext().getDrawable(R.drawable.ic_skip_previous_black_24dp) else requireContext().getDrawable(R.drawable.ic_undo_black_24dp)
        val playListModeNextIcon = if (viewModel.isPlayListMode) requireContext().getDrawable(R.drawable.ic_skip_next_black_24dp) else requireContext().getDrawable(R.drawable.ic_redo_black_24dp)
        player_control_prev.setImageDrawable(playListModePrevIcon)
        player_control_next.setImageDrawable(playListModeNextIcon)
        player_control_prev.setOnClickListener {
            // 動画の最初へ
            exoPlayer.seekTo(0)
        }
        player_control_next.setOnClickListener {
            // 次の動画へ
            viewModel.nextVideo()
        }
        // 全画面ボタン
        player_control_fullscreen.setOnClickListener {
            if (viewModel.isFullScreenMode) {
                // 全画面終了ボタン
                setCloseFullScreen()
            } else {
                // なお全画面は横のみサポート。SCREEN_ORIENTATION_USER_LANDSCAPEを使うと逆向き横画面を回避できる（横画面でも二通りあるけど自動で解決してくれる）
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                setFullScreen()
            }
        }
        // 連続再生ならプレイリスト
        player_control_playlist.setOnClickListener {
            NicoVideoPlayListBottomFragment().show(childFragmentManager, "list")
        }
        // ポップアップ/バッググラウンドなど
        player_control_popup.setOnClickListener {
            // ポップアップ再生
            startVideoPlayService(context = context, mode = "popup", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
            // Activity落とす
            finishFragment()
        }
        player_control_background.setOnClickListener {
            // バッググラウンド再生
            startVideoPlayService(context = context, mode = "background", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
            // Activity落とす
            finishFragment()
        }

        // MotionLayoutのコールバック
        fragment_nicovideo_motionlayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {

            }

            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                if (p1 == R.id.fragment_nicovideo_transition_finish) {
                    // 終了時。左へスワイプした時
                    parentFragmentManager.beginTransaction().remove(this@NicoVideoFragment).commit()
                } else {
                    // ここどうする？
                    val isMiniPlayerMode = isMiniPlayerMode()
                    viewModel.isMiniPlayerMode.value = isMiniPlayerMode
                }
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

            }
        })

        // シークバー用意
        player_control_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !player_control_main.isVisible) {
                    // ユーザー操作だけど、プレイヤーが非表示の時は変更しない。なんかMotionLayoutのせいなのかVisibility=GONEでも操作できるっぽい？
                    seekBar?.progress = (exoPlayer.currentPosition / 1000L).toInt()
                }
                // シークいじったら時間反映されるように
                val formattedTime = DateUtils.formatElapsedTime((seekBar?.progress ?: 0).toLong())
                val videoLengthFormattedTime = DateUtils.formatElapsedTime(exoPlayer.duration / 1000L)
                player_control_current.text = formattedTime
                player_control_duration.text = videoLengthFormattedTime
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // コントローラー非表示カウントダウン終了。
                isTouchSeekBar = true
                job.cancelChildren()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // コメントシークに対応させる
                fragment_nicovideo_comment_canvas.seekComment()
                // ExoPlayer再開
                exoPlayer.seekTo((seekBar?.progress ?: 0) * 1000L)
                // コントローラー非表示カウントダウン開始
                updateHideController(job)
                isTouchSeekBar = false
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
                // あとなんかMotionLayoutが有効の時はシークバーとかが押せなくなるので表示中は一旦Transition無効化。
                MotionLayoutTool.allTransitionEnable(fragment_nicovideo_motionlayout, !player_control_main.isVisible)
            }
        }
    }

    /**
     * ミニプレイヤー用UIを有効/無効にする関数
     * @param isMiniPlayerMode 有効にする場合はtrue。通常に戻す場合はfalse
     * */
    private fun setMiniPlayer(isMiniPlayerMode: Boolean) {
        player_control_video_network.isVisible = !isMiniPlayerMode
        player_control_popup.isVisible = !isMiniPlayerMode
        player_control_background.isVisible = !isMiniPlayerMode
        player_control_repeat.isVisible = !isMiniPlayerMode
        player_control_fullscreen.isVisible = !isMiniPlayerMode
        player_control_prev.isVisible = !isMiniPlayerMode
        player_control_next.isVisible = !isMiniPlayerMode
        player_control_current.isVisible = !isMiniPlayerMode
        player_control_seek.isVisible = !isMiniPlayerMode
        player_control_duration.isVisible = !isMiniPlayerMode
        player_control_playlist.isVisible = !isMiniPlayerMode && viewModel.isPlayListMode
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
        fragment_nicovideo_tablayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(requireContext()))
        fragment_nicovideo_viewpager_parent.background = ColorDrawable(getThemeColor(requireContext()))
        fragment_nicovideo_background.background = ColorDrawable(getThemeColor(requireContext()))
    }

    /**
     * Snackbarを表示させる関数
     * 第一引数はnull禁止。第二、三引数はnullにするとSnackBarのボタンが表示されません
     * */
    fun showSnackbar(message: String, clickMessage: String?, click: (() -> Unit)?) {
        Snackbar.make(fragment_nicovideo_surfaceview, message, Snackbar.LENGTH_SHORT).apply {
            if (clickMessage != null && click != null) {
                setAction(clickMessage) { click() }
            }
            show()
        }
    }

    /**
     * 動画再生View（SurfaceView）を非表示にしてコメントのみの表示にする関数
     * @param enable コメントのみにする場合はtrue
     * */
    fun setCommentOnlyMode(enable: Boolean) {
        viewModel.isCommentOnlyMode = enable
        exoPlayer.stop()
        if (enable) {
            // MotionLayoutを無効
            MotionLayoutTool.allTransitionEnable(fragment_nicovideo_motionlayout, false)
            MotionLayoutTool.setMotionLayoutViewVisible(fragment_nicovideo_motionlayout, R.id.fragment_nicovideo_background, View.GONE)
            hideSwipeToRefresh()
        } else {
            // MotionLayoutを有効
            MotionLayoutTool.allTransitionEnable(fragment_nicovideo_motionlayout, true)
            MotionLayoutTool.setMotionLayoutViewVisible(fragment_nicovideo_motionlayout, R.id.fragment_nicovideo_background, View.VISIBLE)
            viewModel.contentUrl.value?.let { playExoPlayer(it) }
            showSwipeToRefresh()
        }
    }

    /** コメント一覧Fragmentを取得する。無い可能性も有る？ */
    private fun requireCommentFragment() = (viewPager.fragmentList[1] as? NicoVideoCommentFragment)

    /** アイコン入れ替え */
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
            player_control_seek.max = (exoPlayer.duration / 1000L).toInt()
            player_control_seek.progress = (exoPlayer.currentPosition / 1000L).toInt()
            viewModel.currentPosition = exoPlayer.currentPosition
            // 再生時間TextView
            val formattedTime = DateUtils.formatElapsedTime(exoPlayer.currentPosition / 1000L)
            player_control_current.text = formattedTime
        }
    }


    /**
     * ViewPager初期化
     * 注意：キャッシュ再生かどうか、動画IDがちゃんとある状態で実行しないとうまく動きません。
     * @param dynamicAddFragmentList 動的に追加したFragmentがある場合は入れてね。なければ省略していいです。
     * */
    private fun initViewPager(dynamicAddFragmentList: ArrayList<TabLayoutData> = arrayListOf()) {
        // このFragmentを置いたときに付けたTag
        viewPager = NicoVideoRecyclerPagerAdapter(this, videoId, viewModel.isOfflinePlay.value ?: false, dynamicAddFragmentList)
        fragment_nicovideo_viewpager.adapter = viewPager
        TabLayoutMediator(fragment_nicovideo_tablayout, fragment_nicovideo_viewpager) { tab, position ->
            tab.text = viewPager.fragmentTabName[position]
        }.attach()
        // コメントを指定しておく。View#post{}で確実にcurrentItemが仕事するようになった。ViewPager2頼むよ～
        fragment_nicovideo_viewpager.post {
            fragment_nicovideo_viewpager?.setCurrentItem(1, false)
        }
        // もしTabLayoutを常時表示する場合は
        if (prefSetting.getBoolean("setting_scroll_tab_hide", false)) {
            fragment_nicovideo_tablayout.updateLayoutParams<AppBarLayout.LayoutParams> {
                // KTX有能
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            }
        }
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
            // シリーズ一覧Fragment追加
            val seriesId = NicoVideoHTML().getSeriesId(jsonObject)
            val seriesTitle = NicoVideoHTML().getSeriesTitle(jsonObject)
            if (seriesId != null && seriesTitle != null) {
                // シリーズ設定してある。ViewPager2にFragment追加
                val seriesFragment = NicoVideoSeriesFragment().apply {
                    arguments = Bundle().apply {
                        putString("series_id", seriesId)
                        putString("series_title", seriesTitle)
                    }
                }
                // 登録済みなら追加しない
                if (!viewPager.fragmentTabName.contains(seriesTitle)) {
                    viewPager.addFragment(seriesFragment, seriesTitle) // Fragment追加関数
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        exoPlayer.playWhenReady = true
        comment_canvas?.isPause = false
        (requireActivity() as MainActivity).setVisibilityBottomNav(false)
    }

    override fun onPause() {
        super.onPause()
        exoPlayer.playWhenReady = false
        // キャッシュ再生の場合は位置を保存する
        if (isCache) {
            prefSetting.edit {
                putLong("progress_$videoId", exoPlayer.currentPosition)
            }
        }
        (requireActivity() as MainActivity).setVisibilityBottomNav(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        seekTimer.cancel()
        exoPlayer.release()
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Fragment終了関数 */
    private fun finishFragment() = parentFragmentManager.beginTransaction().remove(this).commit()

    /** 画面が横かどうかを返す。横ならtrue */
    fun isLandscape() = requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /** 戻るキー押した時 */
    override fun onBackButtonPress() {
        // コメントのみの表示の際は何もしない
        // コメントのみの表示の際はFragment終了
        if (viewModel.isCommentOnlyMode) {
            finishFragment()
        } else {
            fragment_nicovideo_motionlayout.apply {
                if (currentState == R.id.fragment_nicovideo_transition_end) {
                    transitionToState(R.id.fragment_nicovideo_transition_finish)
                } else {
                    transitionToState(R.id.fragment_nicovideo_transition_end)
                }
            }
        }
    }

    /** ミニプレイヤーかどうか */
    override fun isMiniPlayerMode() = fragment_nicovideo_motionlayout.currentState == R.id.fragment_nicovideo_transition_end || fragment_nicovideo_motionlayout.currentState == R.id.fragment_nicovideo_transition_finish

}
