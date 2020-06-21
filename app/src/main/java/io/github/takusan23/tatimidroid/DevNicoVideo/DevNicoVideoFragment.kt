package io.github.takusan23.tatimidroid.DevNicoVideo

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoRecyclerPagerAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoPOSTFragment
import io.github.takusan23.tatimidroid.FregmentData.DevNicoVideoFragmentData
import io.github.takusan23.tatimidroid.FregmentData.TabLayoutData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRecommendAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoruAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.SQLiteHelper.NicoHistorySQLiteHelper
import io.github.takusan23.tatimidroid.Tool.*
import io.github.takusan23.tatimidroid.Tool.isConnectionMobileDataInternet
import io.github.takusan23.tatimidroid.Tool.isLoginMode
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo_comment.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.net.ssl.SSLProtocolException
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * 開発中のニコ動クライアント（？）
 * */
class DevNicoVideoFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var darkModeSupport: DarkModeSupport

    // 端末内履歴DB
    lateinit var nicoHistorySQLiteHelper: NicoHistorySQLiteHelper
    lateinit var nicoHistorySQLiteDB: SQLiteDatabase

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
    lateinit var viewPager: DevNicoVideoRecyclerPagerAdapter

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

    // NG機能とコテハン
    lateinit var ngDataBaseTool: NGDataBaseTool
    val kotehanMap = mutableMapOf<String, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(requireContext())
        nicoVideoCache = NicoVideoCache(context)
        userSession = prefSetting.getString("user_session", "") ?: ""
        ngDataBaseTool = NGDataBaseTool(context)

        // 端末内履歴DB初期化
        nicoHistorySQLiteHelper = NicoHistorySQLiteHelper(requireContext())
        nicoHistorySQLiteDB = nicoHistorySQLiteHelper.writableDatabase
        nicoHistorySQLiteHelper.setWriteAheadLoggingEnabled(false)

        // ふぉんと
        font = CustomFont(context)
        // CommentCanvasにも適用するかどうか
        if (font.isApplyFontFileToCommentCanvas) {
            fragment_nicovideo_comment_canvas.typeface = font.typeface
        }

        // 動画ID
        videoId = arguments?.getString("id") ?: ""

        // スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ActionBar消す
        (activity as AppCompatActivity).supportActionBar?.hide()

        // View初期化
        showSwipeToRefresh()

        // ダークモード
        initDarkmode()

        // コントローラー表示
        initController()

        exoPlayer = SimpleExoPlayer.Builder(requireContext()).build()

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
            // 動画情報無いとき（動画情報JSON無くても再生はできる）有る
            if (devNicoVideoFragmentData.dataApiData != null) {
                jsonObject = JSONObject(devNicoVideoFragmentData.dataApiData!!)
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
            GlobalScope.launch(Dispatchers.Main) {
                applyUI().await()
                commentFilter(false).await()
                // NG適用
                if (!isCache) {
                    // 関連動画
                    (viewPager.fragmentList[3] as DevNicoVideoRecommendFragment).apply {
                        recommendList.forEach {
                            recyclerViewList.add(it)
                        }
                        initRecyclerView()
                    }
                }
            }
        } else {
            // 初めて///

            // キャッシュを優先的に使う設定有効？
            val isPriorityCache = prefSetting.getBoolean("setting_nicovideo_cache_priority", false)

            // キャッシュ再生が有効ならtrue
            isCache = when {
                arguments?.getBoolean("cache") ?: false -> true // キャッシュ再生
                NicoVideoCache(context).existsCacheVideoInfoJSON(videoId) && isPriorityCache -> true // キャッシュ優先再生が可能
                else -> false // オンライン
            }

            // キャッシュ再生かどうかが分かったところでViewPager初期化
            initViewPager()

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

    }

    // コントローラーのUI変更
    private fun initController() {
        // コメント可視化View（DanmakuView）非表示
        if (prefSetting.getBoolean("setting_nicovideo_danmakuview_hide", false)) {
            danmakuView.visibility = View.GONE
        }
        fragment_nicovideo_fab.setOnClickListener {
            fragment_nicovideo_controller.apply {
                // 表示。
                val showAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_show_animation)
                //表示
                fragment_nicovideo_controller.startAnimation(showAnimation)
                fragment_nicovideo_controller.visibility = View.VISIBLE
                fragment_nicovideo_fab.hide()
            }
        }
        fragment_nicovideo_controller_close.setOnClickListener {
            fragment_nicovideo_controller.apply {
                // 非表示
                val showAnimation = AnimationUtils.loadAnimation(context, R.anim.comment_cardview_hide_animation)
                //表示
                fragment_nicovideo_controller.startAnimation(showAnimation)
                fragment_nicovideo_controller.visibility = View.GONE
                fragment_nicovideo_fab.show()
            }
        }
        // スキップ秒数
        val skipTime = (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5)
        val longSkipTime = (prefSetting.getString("nicovideo_long_skip_sec", "10")?.toLongOrNull() ?: 10)
        fragment_nicovideo_controller_replay.text = "${skipTime} | ${longSkipTime}"
        fragment_nicovideo_controller_forward.text = "${skipTime} | ${longSkipTime}"
        // 押したとき
        fragment_nicovideo_controller_replay.setOnClickListener {
            // 5秒戻す
            val skip = (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000 // 秒→ミリ秒
            exoPlayer.seekTo(exoPlayer.currentPosition - skip)
        }
        fragment_nicovideo_controller_replay.setOnLongClickListener {
            // 長押し時
            val skip = (prefSetting.getString("nicovideo_long_skip_sec", "10")?.toLongOrNull() ?: 10) * 1000 // 秒→ミリ秒
            exoPlayer.seekTo(exoPlayer.currentPosition - skip)
            true
        }
        fragment_nicovideo_controller_forward.setOnClickListener {
            // 5秒進める
            val skip = (prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5) * 1000 // 秒→ミリ秒
            exoPlayer.seekTo(exoPlayer.currentPosition + skip)
        }
        fragment_nicovideo_controller_forward.setOnLongClickListener {
            // 長押し時
            val skip = (prefSetting.getString("nicovideo_long_skip_sec", "10")?.toLongOrNull() ?: 10) * 1000 // 秒→ミリ秒
            exoPlayer.seekTo(exoPlayer.currentPosition + skip)
            true
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
        fragment_nicovideo_tablayout.backgroundTintList = ColorStateList.valueOf(darkModeSupport.getThemeColor())
        fragment_nicovideo_framelayout_elevation_cardview.setBackgroundColor(darkModeSupport.getThemeColor())
    }

    /**
     * 縦画面のみ。動画のタイトルなど表示・非表示やタイトル設定など
     * @param jsonObject NicoVideoHTML#parseJSON()の戻り値
     * */
    fun initTitleArea() {
        if (fragment_nicovideo_bar != null && fragment_nicovideo_video_title_linearlayout != null) {
            fragment_nicovideo_bar.setOnClickListener {
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
        // val nicoVideoHTML = NicoVideoHTML()
        GlobalScope.launch(Dispatchers.IO) {
            // smileサーバーの動画は多分最初の視聴ページHTML取得のときに?eco=1をつけないと低画質リクエストできない
            val eco = if (smileServerLowRequest) {
                "1"
            } else {
                ""
            }
            // ログインしないならそもそもuserSessionの値を空にすれば！？
            val userSession = if (isLoginMode(context)) {
                this@DevNicoVideoFragment.userSession
            } else {
                ""
            }
            val response = nicoVideoHTML.getHTML(videoId, userSession, eco).await()
            // 失敗したら落とす
            if (!response.isSuccessful) {
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
            isDMCServer = nicoVideoHTML.isDMCServer(jsonObject)
            // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
            if (isDMCServer) {
                // 公式アニメは暗号化されてて見れないので落とす
                if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                    if (isAdded) {
                        showToast(getString(R.string.encryption_video_not_play))
                        withContext(Dispatchers.Main) {
                            activity?.finish()
                        }
                    }
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
                                videoQuality =
                                    videoQualityList.getJSONObject(videoQualityList.length() - 1)
                                        .getString("id")
                                audioQuality =
                                    audioQualityList.getJSONObject(audioQualityList.length() - 1)
                                        .getString("id")
                            }
                            withContext(Dispatchers.Main) {
                                Snackbar.make(fragment_nicovideo_surfaceview, "${getString(R.string.quality)}：$videoQuality", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // https://api.dmc.nico/api/sessions のレスポンス
                    val sessionAPIResponse = nicoVideoHTML.callSessionAPI(jsonObject, videoQuality, audioQuality).await()
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
            // コメント取得
            if (isGetComment) {
                val commentJSON = nicoVideoHTML.getComment(videoId, userSession, jsonObject).await()
                if (commentJSON != null) {
                    rawCommentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.body?.string()!!, videoId))
                }
            }
            withContext(Dispatchers.Main) {
                // UI表示
                applyUI().await()
                // フィルターで3ds消したりする
                commentFilter().await()
                // 端末内DB履歴追記
                insertDB(videoTitle)
            }
            // ニコるくん
            isPremium = nicoVideoHTML.isPremium(jsonObject)
            threadId = nicoVideoHTML.getThreadId(jsonObject)
            userId = nicoVideoHTML.getUserId(jsonObject)
            if (isPremium) {
                val nicoruResponse = nicoruAPI.getNicoruKey(userSession, threadId).await()
                if (!nicoruResponse.isSuccessful) {
                    showToast("${getString(R.string.error)}\n${nicoruResponse.code}")
                    return@launch
                }
                // nicoruKey!!!
                nicoruAPI.parseNicoruKey(nicoruResponse.body?.string())
            }

            // 関連動画取得。なんかしらんけどこれもエラー出るっぽい
            try {
                val watchRecommendationRecipe = jsonObject.getString("watchRecommendationRecipe")
                val nicoVideoRecommendAPI = NicoVideoRecommendAPI()
                val recommendAPIResponse = nicoVideoRecommendAPI.getVideoRecommend(watchRecommendationRecipe).await()
                if (!recommendAPIResponse.isSuccessful) {
                    // 失敗時
                    if (isAdded) {
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                    return@launch
                }
                // パース
                nicoVideoRecommendAPI.parseVideoRecommend(recommendAPIResponse.body?.string()).forEach {
                    recommendList.add(it)
                }
                // UIスレッドへ
                withContext(Dispatchers.Main) {
                    // Fragment表示されているか（取得できた時点でもう存在しないかもしれない）
                    if (isAdded) {
                        // DevNicoVideoRecommendFragmentに配列渡す
                        (viewPager.fragmentList[3] as DevNicoVideoRecommendFragment).apply {
                            initRecyclerView()
                        }
                    }
                }
            } catch (e: SSLProtocolException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * コメントをフィルターにかける。3DS消したり（ハニワでやるとほぼ消える）とかNGデータベースを適用する時に使う。
     * 重そうなのでコルーチン
     * 注意：ViewPagerが初期化済みである必要(applyUIを一回以上呼んである必要)があります。
     * 注意：NG操作が終わったときに呼ぶとうまく動く？。
     * 注意：この関数を呼ぶと勝手にコメント一覧のRecyclerViewも更新されます。（代わりにapplyUI関数からコメントRecyclerView関係を消します）
     * rawCommentListはそのままで、フィルターにかけた結果がcommentListになる
     * @param notify DevNicoVideoCommentFragmentに更新をかけたい時はtrue。画面回転時に落ちる（ほんとこの仕様４ね）のでその時だけfalse
     * */
    fun commentFilter(notify: Boolean = true) = GlobalScope.async(Dispatchers.IO) {
        // 3DSけす？
        val is3DSCommentHidden = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        // NG機能
        commentList = rawCommentList.filter { commentJSONParse ->
            !ngDataBaseTool.ngCommentStringList.contains(commentJSONParse.comment) || !ngDataBaseTool.ngUserStringList.contains(commentJSONParse.userId)
        } as ArrayList<CommentJSONParse>
        if (is3DSCommentHidden) {
            // device:3DSが入ってるコメント削除。dropWhileでもいい気がする
            commentList = commentList.toList().filter { commentJSONParse -> !commentJSONParse.mail.contains("device:3DS") } as ArrayList<CommentJSONParse>
        }
        if (notify) {
            withContext(Dispatchers.Main) {
                (viewPager.fragmentList[1] as DevNicoVideoCommentFragment).initRecyclerView(true)
            }
        }
    }

    /**
     * データ取得終わった時にUIに反映させる
     * */
    fun applyUI() = GlobalScope.async(Dispatchers.Main) {
        // ここに来る頃にはFragmentがもう存在しない可能性があるので（速攻ブラウザバックなど）
        if (!isAdded) return@async
        // ViewPager初期化
        //   initViewPager().await()
        if (prefSetting.getBoolean("setting_nicovideo_comment_only", false)) {
            // 動画を再生しない場合
            commentOnlyModeEnable()
        } else {
            // ExoPlayer
            initVideoPlayer(contentUrl, nicoHistory)
        }
        // 動画情報なければ終わる
        if (!::jsonObject.isInitialized) {
            return@async
        }
        // メニューにJSON渡す
        (viewPager.fragmentList[0] as DevNicoVideoMenuFragment).jsonObject = jsonObject
/*
        // コメントFragmentにコメント配列を渡す
        (viewPager.fragmentList[1] as DevNicoVideoCommentFragment).apply {
            recyclerViewList = ArrayList(commentList)
            initRecyclerView(true)
        }
        Toast.makeText(context, "${getString(R.string.get_comment_count)}：${commentList.size}", Toast.LENGTH_SHORT).show()
*/
        // タイトル
        videoTitle = jsonObject.getJSONObject("video").getString("title")
        initTitleArea()
        // 共有
        share = ProgramShare((activity as AppCompatActivity), fragment_nicovideo_surfaceview, videoTitle, videoId)
        if (!isCache) {
            // キャッシュ再生時以外
            // 投稿動画をViewPagerに追加
            viewPagerAddAccountFragment(jsonObject)
        }
        // リピートボタン
        fragment_nicovideo_controller_repeat.setOnClickListener {
            when (exoPlayer.repeatMode) {
                Player.REPEAT_MODE_OFF -> {
                    // リピート無効時
                    exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
                    fragment_nicovideo_controller_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
                }
                Player.REPEAT_MODE_ONE -> {
                    // リピート有効時
                    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
                    fragment_nicovideo_controller_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_black_24dp))
                }
            }
        }
        // ログイン切れてるよメッセージ（プレ垢でこれ食らうと画質落ちるから；；）
        if (isLoginMode(context) && !nicoVideoHTML.verifyLogin(jsonObject)) {
            showSnackbar(getString(R.string.login_disable_message), getString(R.string.login)) {
                GlobalScope.launch {
                    NicoLogin.loginCoroutine(context).await()
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

    private fun insertDB(videoTitle: String) {
        val type = "video"
        val unixTime = System.currentTimeMillis() / 1000
        val contentValues = ContentValues()
        contentValues.apply {
            put("service_id", videoId)
            put("user_id", "")
            put("title", videoTitle)
            put("type", type)
            put("date", unixTime)
            put("description", "")
        }
        nicoHistorySQLiteDB.insert(NicoHistorySQLiteHelper.TABLE_NAME, null, contentValues)
    }

    // Snackbarを表示させる関数
    // 第一引数はnull禁止。第二、三引数はnullにするとSnackBarのボタンが表示されません
    fun showSnackbar(message: String, clickMessage: String?, click: (() -> Unit)?) {
        Snackbar.make(fragment_nicovideo_surfaceview, message, Snackbar.LENGTH_SHORT).apply {
            if (clickMessage != null && click != null) {
                setAction(clickMessage) { click() }
            }
            anchorView = if (fragment_nicovideo_fab.isShown) {
                fragment_nicovideo_fab
            } else {
                fragment_nicovideo_controller
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
            GlobalScope.launch(Dispatchers.IO) {
                // 動画のファイル名取得
                val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
                if (videoFileName != null) {
                    contentUrl = "${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName"
                    if (prefSetting.getBoolean("setting_nicovideo_comment_only", false)) {
                        // 動画を再生しない場合
                        commentOnlyModeEnable()
                    } else {
                        withContext(Dispatchers.Main) {
                            // キャッシュで再生だよ！
                            showToast(getString(R.string.use_cache))
                        }
                    }
                    // コメント取得
                    val commentJSON = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                    rawCommentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON, videoId))
                    // 動画情報
                    if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                        jsonObject = JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId))
                    }
                } else {
                    // 動画が見つからなかった
                    withContext(Dispatchers.Main) {
                        showToast(getString(R.string.not_found_video))
                        activity?.finish()
                    }
                    return@launch
                }
                // UIスレッドへ
                withContext(Dispatchers.Main) {
                    // 再生
                    applyUI().await()
                    // フィルターで3ds消したりする
                    commentFilter().await()
                    // タイトル
                    videoTitle = if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                        JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId)).getJSONObject("video").getString("title")
                    } else {
                        // 動画ファイルの名前
                        nicoVideoCache.getCacheFolderVideoFileName(videoId) ?: videoId
                    }
                    initTitleArea()
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
        fragment_nicovideo_fab.hide()
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
        fragment_nicovideo_fab.show()
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
                val round =
                    BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
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
                val dataSourceFactory =
                    DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
                dataSourceFactory.defaultRequestProperties.set("Cookie", nicohistory)
                val videoSource =
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(videoUrl?.toUri())
                exoPlayer.prepare(videoSource)
            }
        }
        // 自動再生
        exoPlayer.playWhenReady = true
        // リピートするか
        if (prefSetting.getBoolean("nicovideo_repeat_on", true)) {
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
            fragment_nicovideo_controller_repeat.setImageDrawable(context?.getDrawable(R.drawable.ic_repeat_one_24px))
        }
        // 再生ボタン押したとき
        fragment_nicovideo_play_button.setOnClickListener {
            // コメント流し制御
            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            fragment_nicovideo_comment_canvas.isPause = !exoPlayer.playWhenReady
            // アイコン入れ替え
            setPlayIcon()
        }
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                initSeekBar()
                setPlayIcon()
                if (playbackState == Player.STATE_BUFFERING) {
                    // STATE_BUFFERING はシークした位置からすぐに再生できないとき。読込み中のこと。
                    showSwipeToRefresh()
                } else {
                    hideSwipeToRefresh()
                }
                if (!isRotationProgressSuccessful) {

                    // 一時的に。addOnGlobalLayoutListenerで正確なViewの幅を取得してるんだけどこれ値が結構大きく変わるので対策
                    var tmpWidth = -1
                    danmakuView?.apply {
                        // 非表示なら初期化しない
                        if (danmakuView.visibility != View.GONE) {
                            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                                override fun onGlobalLayout() {
                                    if (tmpWidth != width || tmpWidth == 0) {
                                        // おなじになるまで待つ
                                        tmpWidth = width
                                    } else {
                                        // 同じならコメント盛り上がり可視化Viewを初期化
                                        init(exoPlayer.duration / 1000, commentList, width)
                                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                                    }
                                }
                            })
                        }
                    }

                    // 一度だけ実行するように。画面回転時に再生時間を引き継ぐ
                    exoPlayer.seekTo(rotationProgress)
                    isRotationProgressSuccessful = true
                    // 前回見た位置から再生
                    // 画面回転時に２回目以降表示されると邪魔なので制御
                    if (rotationProgress == 0L) {
                        val progress = prefSetting.getLong("progress_$videoId", 0)
                        if (progress != 0L && isCache) {
                            Snackbar.make(fragment_nicovideo_surfaceview, "${getString(R.string.last_time_position_message)}(${DateUtils.formatElapsedTime(progress / 1000L)})", Snackbar.LENGTH_LONG)
                                .apply {
                                    anchorView = fragment_nicovideo_fab
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
                val round =
                    BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                if (isAdded) {
                    setAspectRate(round)
                }
            }
        })

        var secInterval = 0L
        var tmp = false

        seekTimer.cancel()
        seekTimer = Timer()
        seekTimer.schedule(timerTask {
            Handler(Looper.getMainLooper()).post {
                if (!isDestory) {
                    if (exoPlayer.isPlaying) {
                        setProgress()
                        drawComment()
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
            }
        }, 100, 100)
    }

    // アスペクト比合わせる。引数は 横/縦 の答え
    private fun setAspectRate(round: Double) {
        // 画面の幅取得
        val display = activity?.windowManager?.defaultDisplay
        val point = Point()
        display?.getSize(point)
        when (round) {
            1.3 -> {
                // 4:3動画
                // 4:3をそのまま出すと大きすぎるので調整（代わりに黒帯出るけど仕方ない）
                val width =
                    if (context?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        // 縦
                        (point.x / 1.2).toInt()
                    } else {
                        // 横
                        point.x / 2
                    }
                val height = getOldAspectHeightFromWidth(width)
                val params = LinearLayout.LayoutParams(width, height)
                fragment_nicovideo_framelayout.layoutParams = params
            }
            1.7 -> {
                // 16:9動画
                // 横の長さから縦の高さ計算
                val width =
                    if (context?.resources?.configuration?.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        // 縦
                        point.x
                    } else {
                        // 横
                        point.x / 2
                    }
                val height = getAspectHeightFromWidth(width)
                val params = LinearLayout.LayoutParams(width, height)
                fragment_nicovideo_framelayout.layoutParams = params
            }
        }
        // サイズ変更に対応
        fragment_nicovideo_comment_canvas.apply {
            commentLine.clear()
            ueCommentLine.clear()
            sitaCommentLine.clear()
        }
    }

    /**
     * コメント一覧Fragmentを取得する。無い可能性も有る？
     * */
    fun requireCommentFragment() = (viewPager.fragmentList[1] as? DevNicoVideoCommentFragment)

    /**
     * アイコン入れ替え
     * */
    private fun setPlayIcon() {
        val drawable = if (exoPlayer.playWhenReady) {
            context?.getDrawable(R.drawable.ic_pause_black_24dp)
        } else {
            context?.getDrawable(R.drawable.ic_play_arrow_24px)
        }
        fragment_nicovideo_play_button.setImageDrawable(drawable)
    }

    /**
     * コメント描画システム。
     * ArrayList#filter{ }が多分重い（コメントファイル数十MBとかだと応答無くなる）ので
     * UIスレッド：再生時間取得（ExoPlayerは基本UIスレッドで操作する）
     * 別スレッド：filter{ }で再生時間のコメント取得
     * UIスレッド：コメント描画
     * という複雑な方法をとっている（代わりに普通に動くようになった）
     * */
    fun drawComment() {
        val currentPosition = exoPlayer.contentPosition / 100L
        val currentPositionSec = exoPlayer.contentPosition / 1000
        GlobalScope.launch {
            if (tmpPosition != currentPositionSec) {
                drewedList.clear()
                tmpPosition = currentPositionSec
            }
            val drawList = commentList.filter { commentJSONParse ->
                (commentJSONParse.vpos.toLong() / 10L) == (currentPosition)
            }
            drawList.forEach {
                // 追加可能か（livedl等TSのコメントはコメントIDが無い？のでvposで代替する）
                val isAddable = drewedList.none { id -> it.commentNo == id || it.vpos == id } // 条件に合わなければtrue
                if (isAddable) {
                    // コメントIDない場合はvposで代替する
                    if (it.commentNo == "-1" || it.commentNo.isEmpty()) {
                        drewedList.add(it.vpos)
                    } else {
                        drewedList.add(it.commentNo)
                    }
                    if (!it.comment.contains("\n")) {
                        // SingleLine
                        fragment_nicovideo_comment_canvas.post {
                            // 画面回転するとnullになるのでちぇちぇちぇっくわんつー
                            if (fragment_nicovideo_comment_canvas != null) {
                                fragment_nicovideo_comment_canvas.postComment(it.comment, it)
                            }
                        }
                    } else {
                        // 複数行？
                        val asciiArtComment = if (it.mail.contains("shita")) {
                            it.comment.split("\n").reversed() // 下コメントだけ逆順にする
                        } else {
                            it.comment.split("\n")
                        }
                        for (line in asciiArtComment) {
                            fragment_nicovideo_comment_canvas.post {
                                // 画面回転対策
                                if (fragment_nicovideo_comment_canvas != null) {
                                    fragment_nicovideo_comment_canvas.postComment(line, it, true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * シークバー初期化
     * */
    fun initSeekBar() {
        // シークできるようにする
        fragment_nicovideo_seek.max = (exoPlayer.duration / 1000L).toInt()
        fragment_nicovideo_seek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // シークいじったら時間反映されるように
                val formattedTime = DateUtils.formatElapsedTime((seekBar?.progress ?: 0).toLong())
                val videoLengthFormattedTime =
                    DateUtils.formatElapsedTime(exoPlayer.duration / 1000L)
                fragment_nicovideo_progress_text.text = "$formattedTime / $videoLengthFormattedTime"
                // 操作中でもExoPlayerに反映させる
                if (isTouchSeekBar) {
                    exoPlayer.seekTo((seekBar?.progress ?: 0) * 1000L)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isTouchSeekBar = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isTouchSeekBar = false
            }
        })
    }

    /**
     * 進捗進める
     * */
    private fun setProgress() {
        // シークバー操作中でなければ
        if (fragment_nicovideo_seek != null && !isTouchSeekBar) {
            fragment_nicovideo_seek.progress = (exoPlayer.currentPosition / 1000L).toInt()
        }
        // 再生時間TextView
        // val simpleDateFormat = SimpleDateFormat("hh:mm:ss", Locale("UTC"))
        val formattedTime = DateUtils.formatElapsedTime(exoPlayer.currentPosition / 1000L)
        val videoLengthFormattedTime = DateUtils.formatElapsedTime(exoPlayer.duration / 1000L)
        fragment_nicovideo_progress_text.text = "$formattedTime / $videoLengthFormattedTime"
    }

    /**
     * 16:9で横の大きさがわかるときに縦の大きさを返す
     * */
    private fun getAspectHeightFromWidth(width: Int): Int {
        val heightCalc = width / 16
        return heightCalc * 9
    }

    /**
     * 4:3で横の長さがわかるときに縦の長さを返す
     * */
    private fun getOldAspectHeightFromWidth(width: Int): Int {
        val heightCalc = width / 4
        return heightCalc * 3
    }

    /**
     * ViewPager初期化
     * 注意：キャッシュ再生かどうか、動画IDがちゃんとある状態で実行しないとうまく動きません。
     * @param dynamicAddFragmentList 動的に追加したFragmentがある場合は入れてね。なければ省略していいです。
     * */
    private fun initViewPager(dynamicAddFragmentList: ArrayList<TabLayoutData> = arrayListOf()) {
        viewPager = DevNicoVideoRecyclerPagerAdapter(activity as AppCompatActivity, videoId, isCache, this@DevNicoVideoFragment, dynamicAddFragmentList)
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
            val postFragment = DevNicoVideoPOSTFragment().apply {
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

/*
    */
    /**
     * Fragmentをセットする
     * *//*

    private fun initFragment() {
        //FragmentにID詰める
        val bundle = Bundle()
        bundle.putString("id", videoId)
        val fragment =
            NicoVideoCommentFragment()
        fragment.arguments = bundle
        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
            .commit()
        fragment_nicovideo_tablayout.selectTab(fragment_nicovideo_tablayout.getTabAt(1))
        fragment_nicovideo_tablayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.comment) -> {
                        //コメント
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
                            .commit()
                    }
                    getString(R.string.nicovideo_info) -> {
                        //動画情報
                        val fragment =
                            NicoVideoInfoFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
                            .commit()
                    }
                    getString(R.string.menu) -> {
                        val fragment =
                            NicoVideoMenuFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
                            .commit()
                    }
                    getString(R.string.parent_contents) -> {
                        val fragment =
                            NicoVideoContentTreeFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
                            .commit()
                    }
                }
            }
        })
    }
*/

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
        // Fragment終了のち止める
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        heartBeatTimer.cancel()
        nicoVideoCache.destroy()
        nicoVideoHTML.destory()
        isDestory = true
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
                recommendList = recommendList
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

}
