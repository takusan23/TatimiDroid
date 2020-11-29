package io.github.takusan23.tatimidroid.NicoLive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.GoogleCast.GoogleCast
import io.github.takusan23.tatimidroid.NicoLive.Activity.FloatingCommentViewer
import io.github.takusan23.tatimidroid.NicoLive.Adapter.NicoLivePagerAdapter
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveQualitySelectBottomSheet
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModelFactory
import io.github.takusan23.tatimidroid.Service.startLivePlayService
import io.github.takusan23.tatimidroid.Tool.*
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.activity_comment.view.*
import kotlinx.android.synthetic.main.bottom_fragment_enquate_layout.view.*
import kotlinx.android.synthetic.main.comment_card_layout.*
import kotlinx.android.synthetic.main.include_nicolive_player_controller.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.timerTask

/**
 * 生放送再生Fragment。
 * NicoLiveFragmentじゃなくてCommentFragmentになってるのはもともと全部屋見れるコメビュを作りたかったから
 * このFragmentを探す際はTagじゃなくてIDで頼む。
 *
 * BottomFragmentとかViewPager2の中のFragmentは[requireParentFragment]が使えるかもしれないから伝えておく。
 * BottomFragmentで[requireParentFragment]を使うときは、このFragmentの[getChildFragmentManager]を使って開く必要がある
 *
 * ひつようなもの
 * liveId       | String    | 番組ID
 * watch_mode   | String    | comment_viewer comment_post nicocas のどれか
 * isOfficial   | Boolean   | 公式番組ならtrue。無くてもいいかも？
 * is_jk        | Boolean   | 実況ならtrue
 * */
class CommentFragment : Fragment(), MainActivityPlayerFragmentInterface {

    lateinit var commentActivity: AppCompatActivity
    lateinit var prefSetting: SharedPreferences
    lateinit var darkModeSupport: DarkModeSupport

    /** ユーザーセッション */
    var usersession = ""

    /** 番組ID */
    var liveId = ""

    /** 視聴モード（コメント投稿機能付き）かどうか */
    var isWatchingMode = false

    /** 視聴モードがnicocasの場合 */
    var isNicocasMode = false

    /** ニコニコ実況ならtrue */
    var isJK = false

    /** コメント表示をOFFにする場合はtrue */
    var isCommentHide = false

    // アンケート内容いれとく
    var enquateJSONArray = ""

    // コメントコレクション
    val commentCollectionList = arrayListOf<String>()
    val commentCollectionYomiList = arrayListOf<String>()

    // アンケートView
    lateinit var enquateView: View

    // 運営コメント
    lateinit var uncomeTextView: TextView

    // 下のコメント（広告貢献、ランクイン等）
    lateinit var infoTextView: TextView

    // 運コメ・infoコメント非表示
    var hideInfoUnnkome = false

    // 二窓モードになっている場合
    var isNimadoMode = false

    //ExoPlayer
    val exoPlayer by lazy { SimpleExoPlayer.Builder(requireContext()).build() }

    // GoogleCast使うか？
    lateinit var googleCast: GoogleCast

    // 公式かどうか
    var isOfficial = false

    // フォント変更機能
    lateinit var customFont: CustomFont

    // ニコ生ゲームが有効になっているか
    var isAddedNicoNamaGame = false

    // スワイプで画面切り替えるやつ
    lateinit var nicoLivePagerAdapter: NicoLivePagerAdapter

    /**
     * このFragmentからUI関係以外（インターネット接続とかデータベース追加とか）
     * はこっちに書いてある。
     * */
    lateinit var viewModel: NicoLiveViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isNimadoMode = activity is NimadoActivity

        commentActivity = activity as AppCompatActivity

        darkModeSupport = DarkModeSupport(requireContext())
        darkModeSupport.setActivityTheme(activity as AppCompatActivity)

        //ダークモード対応
        applyViewThemeColor()

        // GoogleCast？
        googleCast = GoogleCast(requireContext())
        // GooglePlay開発者サービスがない可能性あり、Gapps焼いてない、ガラホ　など
        if (googleCast.isGooglePlayServicesAvailable()) {
            googleCast.init()
        }

        // 公式番組の場合はAPIが使えないため部屋別表示を無効にする。
        isOfficial = arguments?.getBoolean("isOfficial") ?: false

        // ニコニコ実況ならtrue
        isJK = arguments?.getBoolean("is_jk") ?: false

        //スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        liveId = arguments?.getString("liveId") ?: ""

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        // ViewPager
        initViewPager()

        // センサーによる画面回転
        if (prefSetting.getBoolean("setting_rotation_sensor", false)) {
            RotationSensor(commentActivity, lifecycle)
        }

        // ユーザーの設定したフォント読み込み
        customFont = CustomFont(context)
        // CommentCanvasにも適用するかどうか
        if (customFont.isApplyFontFileToCommentCanvas) {
            comment_fragment_comment_canvas.typeface = customFont.typeface
        }

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

        if (isNimadoMode) {
            MotionLayoutTool.allTransitionEnable(comment_fragment_motionlayout, false)
        }

        //運営コメント、InfoコメントのTextView初期化
        uncomeTextView = TextView(context)
//        live_framelayout.addView(uncomeTextView)
        uncomeTextView.visibility = View.GONE
        //infoコメント
        infoTextView = TextView(context)
//        live_framelayout.addView(infoTextView)
        infoTextView.visibility = View.GONE

        fab.setOnClickListener {
            //表示アニメーションに挑戦した。
            val showAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_show_animation)
            //表示
            include?.startAnimation(showAnimation)
            include?.isVisible = true
            fab.hide()
            //コメント投稿など
            commentCardView()
        }

        // ステータスバー透明化＋タイトルバー非表示＋ノッチ領域にも侵略。関数名にAndがつくことはあんまりない
        hideStatusBarAndSetFullScreen()
        // なんかxmlが効かないので
        MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_audio_only_textview, View.GONE)

        // ログイン情報がなければ終了
        if (prefSetting.getString("mail", "")?.contains("") != false) {
            usersession = prefSetting.getString("user_session", "") ?: ""
            // ViewModel初期化
            viewModel = ViewModelProvider(this, NicoLiveViewModelFactory(requireActivity().application, liveId, isWatchingMode, isJK)).get(NicoLiveViewModel::class.java)
        } else {
            showToast(getString(R.string.mail_pass_error))
            finishFragment()
        }

        // 全画面再生時なら
        if (viewModel.isFullScreenMode) {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            setFullScreen()
        }

        // MainActivityのBottomNavを表示させるか。二窓Activityのせいでas?にしている
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav(false)

        // ミニプレイヤー時なら
        viewModel.isMiniPlayerMode.observe(viewLifecycleOwner) { isMiniPlayerMode ->
            // MainActivityのBottomNavを表示させるか
            (requireActivity() as? MainActivity)?.setVisibilityBottomNav(isMiniPlayerMode)
            setMiniPlayer(isMiniPlayerMode)
            // アイコン直す
            val icon = when (comment_fragment_motionlayout.currentState) {
                R.id.comment_fragment_transition_end -> requireContext().getDrawable(R.drawable.ic_expand_less_black_24dp)
                else -> requireContext().getDrawable(R.drawable.ic_expand_more_24px)
            }
            player_nicolive_control_back_button.setImageDrawable(icon)
            // 画面回転前がミニプレイヤーだったらミニプレイヤーにする
            if (isMiniPlayerMode) {
                comment_fragment_motionlayout.transitionToState(R.id.comment_fragment_transition_end)
            }
        }

        // 統計情報は押したときに計算するようにした
        player_nicolive_control_statistics.setOnClickListener {
            viewModel.calcToukei(true)
        }

        // SnackBar表示
        viewModel.snackbarLiveData.observe(viewLifecycleOwner) { message ->
            multiLineSnackbar(player_nicolive_control_active_text, message)
        }

        // Activity終了
        viewModel.messageLiveData.observe(viewLifecycleOwner) { message ->
            when (message) {
                "finish" -> finishFragment()
            }
        }

        // 番組情報
        viewModel.nicoLiveProgramData.observe(viewLifecycleOwner) { data ->
            initController(data.title)
            googleCast.apply {
                programTitle = data.title
                programSubTitle = data.programId
                programThumbnail = data.thum
            }
        }

        // getPlayerStatus
        viewModel.roomNameAndChairIdLiveData.observe(viewLifecycleOwner) { message ->
            player_nicolive_control_id.text = message
        }

        // ニコニコ実況の時のUI
        viewModel.nicoJKGetFlv.observe(viewLifecycleOwner) { getFlv ->
            // 番組情報入れる
            player_nicolive_control_title.text = getFlv.channelName
            player_nicolive_control_id.text = liveId
            // コマンドいらないと思うし消す
            comment_cardview_comment_command_edit_button.visibility = View.GONE
            // アクティブ計算以外使わないので消す
            (player_nicolive_control_time.parent as View).isVisible = false
            player_nicolive_control_watch_count.isVisible = false
            player_nicolive_control_comment_count.isVisible = false
            // バックグラウンド再生無いので非表示
            player_nicolive_control_background.isVisible = false
            initController(getFlv.channelName)
            aspectRatioFix()
        }

        // うんこめ
        viewModel.unneiCommentLiveData.observe(viewLifecycleOwner) { unnkome ->
            if (unnkome == "clear") {
                removeUnneiComment()
            } else {
                setUneiComment(CommentJSONParse(unnkome, getString(R.string.room_integration), liveId).comment)
            }
        }

        // あんけーと
        viewModel.enquateLiveData.observe(viewLifecycleOwner) { enquateMessage ->
            showEnquate(enquateMessage)
        }

        // 統計情報
        viewModel.statisticsLiveData.observe(viewLifecycleOwner) { statistics ->
            player_nicolive_control_watch_count?.text = statistics.viewers.toString()
            player_nicolive_control_comment_count?.text = statistics.comments.toString()
        }

        // アクティブユーザー？
        viewModel.activeCommentPostUserLiveData.observe(viewLifecycleOwner) { active ->
            player_nicolive_control_active_text.text = active
            player_nicolive_control_statistics.isVisible = true
        }

        // 経過時間
        viewModel.programTimeLiveData.observe(viewLifecycleOwner) { programTime ->
            player_nicolive_control_time.text = programTime
        }

        // 終了時刻
        viewModel.formattedProgramEndTime.observe(viewLifecycleOwner) { endTime ->
            player_nicolive_control_end_time.text = endTime
        }

        // HLSアドレス取得
        viewModel.hlsAddress.observe(viewLifecycleOwner) { address ->
            if (viewModel.isCommentOnlyMode) {
                setCommentOnlyMode(true)
            } else {
                setPlayVideoView()
                initQualityChangeBottomFragment(viewModel.currentQuality, viewModel.qualityListJSONArray)
                googleCast.apply {
                    hlsAddress = address
                    resume()
                }
            }
        }

        // 画質変更
        viewModel.changeQualityLiveData.observe(viewLifecycleOwner) { quality ->
            showQualityChangeSnackBar(viewModel.currentQuality)
        }

        // コメントうけとる
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { commentJSONParse ->
            // コメント非表示モードの場合はなさがない
            if (!isCommentHide) {
                // 豆先輩とか
                if (!commentJSONParse.comment.contains("\n")) {
                    comment_fragment_comment_canvas.postComment(commentJSONParse.comment, commentJSONParse)
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
                    comment_fragment_comment_canvas.postCommentAsciiArt(asciiArtComment, commentJSONParse)
                }
            }
        }

    }

    /**
     * コメントのみの表示に切り替える関数
     * @param isCommentOnly コメントのみの表示にする場合
     * */
    fun setCommentOnlyMode(isCommentOnly: Boolean) {
        viewModel.isCommentOnlyMode = isCommentOnly
        exoPlayer.stop()
        if (isCommentOnly) {
            MotionLayoutTool.allTransitionEnable(comment_fragment_motionlayout, false)
            MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_surface_view, View.GONE)
        } else {
            MotionLayoutTool.allTransitionEnable(comment_fragment_motionlayout, true)
            MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_surface_view, View.VISIBLE)
            setPlayVideoView()
        }
    }

    /** ダークモード等テーマに合わせた色を設定する */
    private fun applyViewThemeColor() {
        activity_comment_tab_layout.background = ColorDrawable(getThemeColor(requireContext()))
        comment_fragment_app_bar.background = ColorDrawable(getThemeColor(requireContext()))
        comment_viewpager?.background = ColorDrawable(getThemeColor(requireContext()))
        comment_fragment_background?.background = ColorDrawable(getThemeColor(requireContext()))
        player_nicolive_control_info_main?.background = ColorDrawable(getThemeColor(requireContext()))
        comment_fragment_background.background = ColorDrawable(getThemeColor(requireContext()))
    }

    /** コントローラーを初期化する。HTML取得後にやると良さそう */
    private fun initController(programTitle: String) {
        // クリックイベントを通過させない
        val job = Job()
        // 最小化するとかしないとか
        player_nicolive_control_back_button.isVisible = true
        player_nicolive_control_back_button.setOnClickListener {
            when {
                viewModel.isFullScreenMode -> {
                    setCloseFullScreen()
                }
                comment_fragment_motionlayout.currentState == R.id.comment_fragment_transition_start -> {
                    comment_fragment_motionlayout.transitionToState(R.id.comment_fragment_transition_end)
                }
                else -> {
                    comment_fragment_motionlayout.transitionToState(R.id.comment_fragment_transition_start)
                }
            }
        }
        // 番組情報
        player_nicolive_control_title.text = programTitle
        // Marqueeを有効にするにはフォーカスをあてないといけない？。<marquee>とかWeb黎明期感ある（その時代の人じゃないけど）
        player_nicolive_control_title.isSelected = true
        // 全画面/ポップアップ/バッググラウンド
        player_nicolive_control_popup.setOnClickListener { startPlayService("popup") }
        player_nicolive_control_background.setOnClickListener { startPlayService("background") }
        player_nicolive_control_fullscreen.setOnClickListener {
            if (viewModel.isFullScreenMode) {
                // 全画面終了
                setCloseFullScreen()
            } else {
                // 全画面移行
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                setFullScreen()
            }
        }
        // 統計情報表示
        comment_fragment_statistics_show?.setOnClickListener {
            player_nicolive_control_info_main.isVisible = !player_nicolive_control_info_main.isVisible
        }
        // 横画面なら常に表示
        if (!viewModel.isFullScreenMode && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            player_nicolive_control_info_main.isVisible = true
        }
        // 接続方法
        player_nicolive_control_video_network.apply {
            setImageDrawable(InternetConnectionCheck.getConnectionTypeDrawable(requireContext()))
            setOnClickListener {
                showToast(InternetConnectionCheck.createNetworkMessage(requireContext()))
            }
        }

        // MotionLayout関係
        comment_fragment_motionlayout_parent_framelayout.apply {
            allowIdList.add(R.id.comment_fragment_transition_start) // 通常状態（コメント表示など）は無条件でタッチを渡す。それ以外はプレイヤー部分のみタッチ可能
            allowIdList.add(R.id.comment_fragment_transition_fullscreen) // フルスクリーン時もクリックが行かないように
            swipeTargetView = comment_fragment_control
            motionLayout = comment_fragment_motionlayout
            // プレイヤーを押した時。普通にsetOnClickListenerとか使うと競合して動かなくなる
            onSwipeTargetViewClickFunc = { event ->
                player_nicolive_control_main?.isVisible = !player_nicolive_control_main.isVisible
                // フルスクリーン時はFabも消す
                if (viewModel.isFullScreenMode) {
                    if (player_nicolive_control_main.isVisible) fab.show() else fab.hide()
                }
                updateHideController(job)
            }
            // swipeTargetViewの上にあるViewをここに書く。ここに書いたViewを押した際はonSwipeTargetViewClickFuncが呼ばれなくなる(View#setOnClickListenerは呼ばれる)
            addAllIsClickableViewFromParentView(player_nicolive_control_main)
            // blockViewListに追加したViewが押さてたときに共通で行いたい処理などを書く
            onBlockViewClickFunc = { view, event ->
                // UI非表示なら表示
                if (!player_nicolive_control_main.isVisible) {
                    onSwipeTargetViewClickFunc?.invoke(null)
                } else {
                    //
                }
            }
        }
        comment_fragment_motionlayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
            }

            override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {

            }

            override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
                // 終了アニメーション
                if (p1 == R.id.comment_fragment_transition_finish) {
                    parentFragmentManager.beginTransaction().remove(this@CommentFragment).commit()
                } else {
                    // ここどうする？
                    val isMiniPlayerMode = isMiniPlayerMode()
                    viewModel.isMiniPlayerMode.value = isMiniPlayerMode
                }
            }

            override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

            }
        })
        updateHideController(job)
    }

    /**
     * ミニプレイヤー用UIを有効/無効にする関数
     * @param isMiniPlayerMode 有効にする場合はtrue。通常に戻す場合はfalse
     * */
    private fun setMiniPlayer(isMiniPlayerMode: Boolean) {
        listOf(
            player_nicolive_control_video_network,
            player_nicolive_control_popup,
            player_nicolive_control_background,
            player_nicolive_control_fullscreen,
        ).forEach { view ->
            // 三種の神器。これするとMotionLayoutがうまく動く？
            view.isEnabled = !isMiniPlayerMode
            view.isClickable = !isMiniPlayerMode
            view.isVisible = !isMiniPlayerMode
        }
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
                if (viewModel.isFullScreenMode) {
                    if (fab.isShown) fab.hide() else fab.show()
                }
            }
        }
    }

    /**
     * フルスクリーン再生。
     * 現状横画面のみ
     * */
    private fun setFullScreen() {
        // 全画面だよ
        viewModel.isFullScreenMode = true
        // コメビュ非表示
        comment_fragment_motionlayout.transitionToState(R.id.comment_fragment_transition_fullscreen)
        // 経過時間消す
        player_nicolive_control_info_main.isVisible = false
        // システムバー非表示
        setSystemBarVisibility(false)
        // 背景色
        comment_fragment_background.background = ColorDrawable(Color.BLACK)
        // アイコン変更
        player_nicolive_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_exit_black_24dp))
        // 高さ更新
        comment_fragment_comment_canvas.finalHeight = comment_fragment_comment_canvas.height
    }

    /**
     * 全画面解除
     * */
    private fun setCloseFullScreen() {
        // 全画面ではない
        viewModel.isFullScreenMode = false
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        // コメビュ表示
        comment_fragment_motionlayout.transitionToState(R.id.comment_fragment_transition_start)
        // 経過時間出す
        player_nicolive_control_info_main.isVisible = true
        // システムバー表示
        setSystemBarVisibility(true)
        // アイコン変更
        player_nicolive_control_fullscreen.setImageDrawable(requireContext().getDrawable(R.drawable.ic_fullscreen_black_24dp))
        // 背景色もどす
        comment_fragment_background.background = ColorDrawable(getThemeColor(context))
        // 高さ更新
        comment_fragment_comment_canvas.finalHeight = comment_fragment_comment_canvas.height
    }

    /** 上と下に出るコメントをセットする */
    private fun setUneiComment(comment: String) {
        val isNicoad = comment.contains("/nicoad")
        val isInfo = comment.contains("/info")
        val isUadPoint = comment.contains("/uadpoint")
        val isSpi = comment.contains("/spi")
        val isGift = comment.contains("/gift")
        // 上に表示されるやつ
        if (!isNicoad && !isInfo && !isUadPoint && !isSpi && !isGift) {
            // 生主コメント
            setUnneiComment(comment)
        } else {
            // UIスレッド
            activity?.runOnUiThread {
                // 下に表示するやつ
                if (isInfo) {
                    // /info {数字}　を消す
                    val regex = "/info \\d+ ".toRegex()
                    showInfoComment(comment.replace(regex, ""))
                }
                // ニコニ広告
                if (isNicoad) {
                    val json = JSONObject(comment.replace("/nicoad ", ""))
                    val comment = json.getString("message")
                    showInfoComment(comment)
                }
                // spi (ニコニコ新市場に商品が貼られたとき)
                if (isSpi) {
                    showInfoComment(comment.replace("/spi ", ""))
                }
                // 投げ銭
                if (isGift) {
                    // スペース区切り配列
                    val list = comment.replace("/gift ", "").split(" ")
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
            Snackbar.make(fab, context?.getString(R.string.program_disconnect) ?: "", Snackbar.LENGTH_SHORT).setAction(context?.getString(R.string.end)) {
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
                    comment_fragment_enquate_framelayout?.removeAllViews()
                }
            }
        }
    }

    /**
     * 画質変更SnackBar表示
     * @param selectQuality 選択した画質
     * */
    private fun showQualityChangeSnackBar(selectQuality: String?) {
        // 画質変更した。SnackBarでユーザーに教える
        multiLineSnackbar(comment_fragment_surface_view, "${getString(R.string.successful_quality)}\n→${selectQuality}")
    }

    // 画質変更BottomFragment初期化
    private fun initQualityChangeBottomFragment(selectQuality: String?, qualityTypesJSONArray: JSONArray) {
        // 画質変更BottomFragmentに詰める。なんかUIスレッドにしないとだめっぽい？
        activity?.runOnUiThread {
            val bundle = Bundle()
            bundle.putString("select_quality", selectQuality)
            bundle.putString("quality_list", qualityTypesJSONArray.toString())
            bundle.putString("liveId", liveId)
            val nicoLiveQualitySelectBottomSheet = NicoLiveQualitySelectBottomSheet()
            nicoLiveQualitySelectBottomSheet.arguments = bundle
        }
    }

    /** ViewPager2初期化 */
    private fun initViewPager() {
        comment_viewpager ?: return
        comment_viewpager.id = View.generateViewId()
        nicoLivePagerAdapter = NicoLivePagerAdapter(this, liveId, isOfficial, isJK)
        comment_viewpager.adapter = nicoLivePagerAdapter
        // Tabに入れる名前
        TabLayoutMediator(activity_comment_tab_layout, comment_viewpager) { tab, position ->
            tab.text = nicoLivePagerAdapter.fragmentTabNameList[position]
        }.attach()
        // コメントを指定しておく。View#post{}で確実にcurrentItemが仕事するようになった。ViewPager2頼むよ～
        comment_viewpager.post {
            comment_viewpager?.setCurrentItem(1, false)
        }
        // もしTabLayoutを常時表示する場合は
        if (prefSetting.getBoolean("setting_scroll_tab_hide", false)) {
            // 多分AppBarLayoutは一人っ子(AppBarLayoutの子のViewは一個)
            comment_fragment_app_bar.getChildAt(0).updateLayoutParams<AppBarLayout.LayoutParams> {
                // KTX有能
                scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
            }
        }
    }

    /**
     * ステータスバーとノッチに侵略するやつ
     * */
    fun hideStatusBarAndSetFullScreen() {
        if (prefSetting.getBoolean("setting_display_cutout", false)) {
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

    /**
     * 予め置いておいたWebViewでニコ生を再生してゲームを遊ぶ。
     * @param isWebViewPlayer 生放送再生もWebViewでやる場合はtrue
     * */
    fun setNicoNamaGame(isWebViewPlayer: Boolean = false) {
        MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_webview, View.VISIBLE)
        MotionLayoutTool.allTransitionEnable(comment_fragment_motionlayout, false)
        NicoNamaGameWebViewTool.init(comment_fragment_webview, liveId, isWebViewPlayer)
        isAddedNicoNamaGame = true
    }

    /**
     * ニコ生ゲームをやめる。
     * */
    fun removeNicoNamaGame() {
        MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_webview, View.GONE)
        MotionLayoutTool.allTransitionEnable(comment_fragment_motionlayout, true)
        comment_fragment_webview.loadUrl("about:blank")
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

    /**
     * MultilineなSnackbar
     * https://stackoverflow.com/questions/30705607/android-multiline-snackbar
     * */
    private fun multiLineSnackbar(view: View, message: String) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById(R.id.snackbar_text) as TextView
        textView.maxLines = 5 // show multiple line
        snackbar.anchorView = getSnackbarAnchorView() // 何のViewの上に表示するか指定
        snackbar.show()
    }

    //視聴モード
    fun setPlayVideoView() {
        val hlsAddress = viewModel.hlsAddress.value ?: return
        exoPlayer.stop()

        //設定で読み込むかどうか
        Handler(Looper.getMainLooper()).post {

            // 音声のみの再生はその旨（むね）を表示して、SurfaceViewを暗黒へ。わーわー言うとりますが、お時間でーす
            if (viewModel.currentQuality == "audio_high") {
                MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_audio_only_textview, View.VISIBLE)
                MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_surface_view, View.VISIBLE)
                comment_fragment_surface_view.background = ColorDrawable(Color.BLACK)
            } else {
                MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_audio_only_textview, View.GONE)
                MotionLayoutTool.setMotionLayoutViewVisible(comment_fragment_motionlayout, R.id.comment_fragment_audio_only_textview, View.GONE)
                comment_fragment_surface_view.background = null
            }

            aspectRatioFix()

            // 高さ更新
            comment_fragment_comment_canvas.finalHeight = comment_fragment_comment_canvas.height
            val sourceFactory = DefaultDataSourceFactory(requireContext(), "TatimiDroid;@takusan_23", object : TransferListener {
                override fun onTransferInitializing(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

                }

                override fun onTransferStart(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

                }

                override fun onBytesTransferred(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean, bytesTransferred: Int) {

                }

                override fun onTransferEnd(source: DataSource, dataSpec: DataSpec, isNetwork: Boolean) {

                }
            })
            val hlsMediaSource = HlsMediaSource.Factory(sourceFactory).createMediaSource(MediaItem.fromUri(hlsAddress.toUri()))
            //再生準備
            exoPlayer.setMediaSource(hlsMediaSource)
            exoPlayer.prepare()
            //SurfaceViewセット
            exoPlayer.setVideoSurfaceView(comment_fragment_surface_view)
            //再生
            exoPlayer.playWhenReady = true

            exoPlayer.addListener(object : Player.EventListener {

                override fun onPlayerError(error: ExoPlaybackException) {
                    super.onPlayerError(error)
                    error.printStackTrace()
                    println("生放送の再生が止まりました。")
                    //再接続する？
                    //それからニコ生視聴セッションWebSocketが切断されてなければ
                    if (!viewModel.nicoLiveHTML.nicoLiveWebSocketClient.isClosed) {
                        println("再度再生準備を行います")
                        activity?.runOnUiThread {
                            //再生準備
                            exoPlayer.setMediaSource(hlsMediaSource)
                            exoPlayer.prepare()
                            //SurfaceViewセット
                            exoPlayer.setVideoSurfaceView(comment_fragment_surface_view)
                            //再生
                            exoPlayer.playWhenReady = true
                            Snackbar.make(fab, getString(R.string.error_player), Snackbar.LENGTH_SHORT).apply {
                                anchorView = getSnackbarAnchorView()
                                // 再生が止まった時に低遅延が有効になっていればOFFにできるように。安定して見れない場合は低遅延が有効なのが原因
                                if (viewModel.nicoLiveHTML.isLowLatency) {
                                    setAction(getString(R.string.low_latency_off)) {
                                        viewModel.nicoLiveHTML.sendLowLatency(!viewModel.nicoLiveHTML.isLowLatency)
                                    }
                                }
                                show()
                            }
                        }
                    }
                }
            })
        }
    }

    /** アスペクト比を直す。ミニプレイヤーと全画面UIのアスペクト比も直す */
    private fun aspectRatioFix() {
        // 画面の幅/高さ取得。Android 11に対応した
        val displayWidth = DisplaySizeTool.getDisplayWidth(context)
        val displayHeight = DisplaySizeTool.getDisplayHeight(context)
        val isLandScape = commentActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandScape) {
            // 横画面時
            val playerWidth = if (isNimadoMode) displayWidth / 4 else displayWidth / 2
            val playerHeight = (9F / 16 * playerWidth)
            // MotionLayoutのConstraintSetの高さを変えることになるので少しめんどい
            comment_fragment_motionlayout?.apply {
                getConstraintSet(R.id.comment_fragment_transition_start)?.apply {
                    constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt())
                    constrainWidth(R.id.comment_fragment_surface_view, playerWidth)
                }
                // ミニプレイヤー時
                getConstraintSet(R.id.comment_fragment_transition_end)?.apply {
                    constrainHeight(R.id.comment_fragment_surface_view, (playerHeight / 1.5).toInt())
                    constrainWidth(R.id.comment_fragment_surface_view, (playerWidth / 1.5).toInt())
                }
                getConstraintSet(R.id.comment_fragment_transition_finish)?.apply {
                    constrainHeight(R.id.comment_fragment_surface_view, (playerHeight / 1.5).toInt())
                    constrainWidth(R.id.comment_fragment_surface_view, (playerWidth / 1.5).toInt())
                }
            }
        } else {
            // 縦画面時
            val playerWidth = if (isNimadoMode) displayWidth / 2 else displayWidth
            val playerHeight = (9F / 16 * playerWidth)
            // MotionLayoutのConstraintSetの高さを変えることになるので少しめんどい
            comment_fragment_motionlayout?.apply {
                getConstraintSet(R.id.comment_fragment_transition_start)?.apply {
                    constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt())
                    constrainWidth(R.id.comment_fragment_surface_view, playerWidth)
                }
                // ミニプレイヤー時
                getConstraintSet(R.id.comment_fragment_transition_end)?.apply {
                    constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt() / 2)
                    constrainWidth(R.id.comment_fragment_surface_view, playerWidth / 2)
                }
                getConstraintSet(R.id.comment_fragment_transition_finish)?.apply {
                    constrainHeight(R.id.comment_fragment_surface_view, playerHeight.toInt() / 2)
                    constrainWidth(R.id.comment_fragment_surface_view, playerWidth / 2)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.stop()
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav(false)
    }

    override fun onPause() {
        super.onPause()
        googleCast.pause()
        (requireActivity() as? MainActivity)?.setVisibilityBottomNav(true)
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
            title = viewModel.programTitle,
            thumbUrl = viewModel.thumbnailURL
        )
    }


    fun setLandscapePortrait() {
        val conf = resources.configuration
        //live_video_view.stopPlayback()
        when (conf.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                //縦画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                //横画面
                commentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
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
        val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("communityid", viewModel.communityId))
        //コピーしました！
        Toast.makeText(context, "${getString(R.string.copy_communityid)} : ${viewModel.communityId}", Toast.LENGTH_SHORT).show()
    }

    private fun destroyCode() {
        // 止める
        exoPlayer.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyCode()
    }

    /** ポップアップ再生 */
    fun startPopupPlay() {
        startPlayService("popup")
    }

    /** バックグラウンド再生 */
    fun startBackgroundPlay() {
        startPlayService("background")
    }

    /**
     * ポップアップ再生、バッググラウンド再生サービス起動用関数
     * @param mode "popup"（ポップアップ再生）か"background"（バッググラウンド再生）
     * */
    private fun startPlayService(mode: String) {
        // サービス起動
        startLivePlayService(
            context = context,
            mode = mode,
            liveId = liveId,
            isCommentPost = isWatchingMode,
            isNicocasMode = isNicocasMode,
            isJK = isJK,
            isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
            startQuality = viewModel.currentQuality
        )
        // Activity落とす
        finishFragment()
    }


    //Activity復帰した時に呼ばれる
    override fun onStart() {
        super.onStart()
        //再生部分を作り直す
        if (viewModel.hlsAddress.value?.isNotEmpty() == true && !viewModel.isCommentOnlyMode) {
            setPlayVideoView()
        }
    }

    private fun setEnquetePOSTLayout(message: String, type: String) {
        enquateView = layoutInflater.inflate(R.layout.bottom_fragment_enquate_layout, null, false)
        if (type.contains("start")) {
            // アンケ中はMotionLayoutのTransitionを一時的に無効化
            MotionLayoutTool.allTransitionEnable(comment_fragment_motionlayout, false)
            //アンケ開始
            comment_fragment_enquate_framelayout?.removeAllViews()
            comment_fragment_enquate_framelayout?.addView(enquateView)
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
                    viewModel.enquatePOST(i - 1)
                    // アンケ画面消す
                    comment_fragment_enquate_framelayout.removeAllViews()
                    // SnackBar
                    Snackbar.make(comment_fragment_surface_view, "${getString(R.string.enquate)}：${jsonArray[i]}", Snackbar.LENGTH_SHORT).apply {
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
            // 結果出たらMotionLayoutのTransitionを一時的に有効に戻す
            MotionLayoutTool.allTransitionEnable(comment_fragment_motionlayout, true)
            //アンケ結果
            comment_fragment_enquate_framelayout?.removeAllViews()
            comment_fragment_enquate_framelayout?.addView(enquateView)
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
            Snackbar.make(comment_fragment_surface_view, getString(R.string.enquate_result), Snackbar.LENGTH_SHORT).apply {
                anchorView = getSnackbarAnchorView()
                setAction(getString(R.string.share)) {
                    //共有する
                    share(shareText, "$title(${viewModel.programTitle}-$liveId)")
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
        if (viewModel.nicoLiveHTML.isPremium) {
            comment_cardview_command_edit_color_premium_linearlayout.visibility = View.VISIBLE
        }
        // 184が有効になっているときはコメントInputEditTextのHintに追記する
        if (viewModel.nicoLiveHTML.isPostTokumeiComment) {
            comment_cardview_comment_textinput_layout.hint = getString(R.string.comment)
        } else {
            comment_cardview_comment_textinput_layout.hint = "${getString(R.string.comment)}（${getString(R.string.disabled_tokumei_comment)}）"
        }
        //投稿ボタンを押したら投稿
        comment_cardview_comment_send_button.setOnClickListener {
            val comment = comment_cardview_comment_textinput_edittext.text.toString()
            // 7/27からコマンド（色や位置の指定）は全部つなげて送るのではなく、それぞれ指定する必要があるので
            val color = comment_cardview_command_color_textinputlayout.text.toString()
            val position = comment_cardview_command_position_textinputlayout.text.toString()
            val size = comment_cardview_command_size_textinputlayout.text.toString()
            // コメ送信
            lifecycleScope.launch(Dispatchers.IO) { viewModel.sendComment(comment, color, size, position, isNicocasMode) }
            comment_cardview_comment_textinput_edittext.setText("")
            if (!prefSetting.getBoolean("setting_command_save", false)) {
                // コマンドを保持しない設定ならクリアボタンを押す
                comment_cardview_comment_command_edit_reset_button.callOnClick()
            }
        }
        // Enterキー(紙飛行機ボタン)を押したら投稿する
        if (prefSetting.getBoolean("setting_enter_post", true)) {
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
                        lifecycleScope.launch(Dispatchers.IO) { viewModel.sendComment(text, color, size, position, isNicocasMode) }
                        // コメントリセットとコマンドリセットボタンを押す
                        comment_cardview_comment_textinput_edittext.setText("")
                        if (!prefSetting.getBoolean("setting_command_save", false)) {
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
            include?.startAnimation(hideAnimation)
            include?.isVisible = false
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

        if (prefSetting.getBoolean("setting_comment_collection_useage", false)) {
            //コメントコレクション補充機能
            if (prefSetting.getBoolean("setting_comment_collection_assist", false)) {
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
            return include
        }
    }

    /** 運営コメント（上に出るやつ）表示 */
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

    /** Fragment終了関数 */
    private fun finishFragment() = parentFragmentManager.beginTransaction().remove(this).commit()

    fun isInitGoogleCast(): Boolean = ::googleCast.isInitialized

    /** 戻るキー押しったとき */
    override fun onBackButtonPress() {
        // コメントのみの表示の際はFragment終了
        if (viewModel.isCommentOnlyMode) {
            finishFragment()
        } else {
            comment_fragment_motionlayout.apply {
                if (currentState == R.id.comment_fragment_transition_end) {
                    // 終了へ
                    transitionToState(R.id.comment_fragment_transition_finish)
                } else {
                    transitionToState(R.id.comment_fragment_transition_end)
                }
            }
        }
    }

    /** ミニプレイヤーで再生していればtrueを返す */
    override fun isMiniPlayerMode(): Boolean {
        return comment_fragment_motionlayout.currentState == R.id.comment_fragment_transition_end
    }

}