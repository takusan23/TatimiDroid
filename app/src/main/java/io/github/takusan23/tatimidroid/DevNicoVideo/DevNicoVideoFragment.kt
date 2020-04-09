package io.github.takusan23.tatimidroid.DevNicoVideo

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
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
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoViewPager
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoPOSTFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo_comment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * 開発中のニコ動クライアント（？）
 * */
class DevNicoVideoFragment : Fragment() {
    lateinit var prefSetting: SharedPreferences
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var darkModeSupport: DarkModeSupport

    // ハートビート
    var heartBeatTimer = Timer()
    var seekTimer = Timer()

    // 必要なやつ
    var userSession = ""
    var videoId = ""
    var videoTitle = ""


    // 一度だけ実行する
    var isInit = true

    // キャッシュ取得用
    lateinit var nicoVideoCache: NicoVideoCache
    var isCache = false
    var contentUrl = ""
    var nicoHistory = ""
    lateinit var jsonObject: JSONObject

    // session_apiのレスポンス
    lateinit var sessionAPIJSONObject: JSONObject

    // データ取得からハートビートまで扱う
    val nicoVideoHTML =
        NicoVideoHTML()

    // コメント配列
    var commentList = arrayListOf<ArrayList<String>>()

    // ViewPager
    lateinit var viewPager: DevNicoVideoViewPager

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context!!)
        nicoVideoCache =
            NicoVideoCache(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        font = CustomFont(context)

        // 動画ID
        videoId = arguments?.getString("id") ?: ""
        // キャッシュ再生が有効ならtrue
        isCache = arguments?.getBoolean("cache") ?: false

        // スリープにしない
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // ActionBar消す
        (activity as AppCompatActivity).supportActionBar?.hide()

        // View初期化
        showSwipeToRefresh()

        // ダークモード
        initDarkmode()

        // Fragmentセットする
        initViewPager()

        // ブロードキャスト初期化
        initBroadCastReceiver()

        // コントローラー表示
        initController()

        // キャッシュ再生のときとそうじゃないとき
        exoPlayer = SimpleExoPlayer.Builder(context!!).build()
        if (isCache) {
            // キャッシュ再生
            cachePlay()
        } else {
            // データ取得
            coroutine()
        }
    }

    private fun initController() {
        fragment_nicovideo_fab.setOnClickListener {
            fragment_nicovideo_controller.apply {
                // 表示。
                val showAnimation =
                    AnimationUtils.loadAnimation(context!!, R.anim.comment_cardview_show_animation)
                //表示
                fragment_nicovideo_controller.startAnimation(showAnimation)
                fragment_nicovideo_controller.visibility = View.VISIBLE
                fragment_nicovideo_fab.hide()
            }
        }
        fragment_nicovideo_controller_close.setOnClickListener {
            fragment_nicovideo_controller.apply {
                // 非表示
                val showAnimation =
                    AnimationUtils.loadAnimation(context!!, R.anim.comment_cardview_hide_animation)
                //表示
                fragment_nicovideo_controller.startAnimation(showAnimation)
                fragment_nicovideo_controller.visibility = View.GONE
                fragment_nicovideo_fab.show()
            }
        }
        fragment_nicovideo_controller_replay.setOnClickListener {
            // 5秒戻す
            exoPlayer.seekTo(exoPlayer.currentPosition - 5000)
        }
        fragment_nicovideo_controller_forward.setOnClickListener {
            // 5秒進める
            exoPlayer.seekTo(exoPlayer.currentPosition + 5000)
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
        darkModeSupport = DarkModeSupport(context!!)
        fragment_nicovideo_tablayout.backgroundTintList =
            ColorStateList.valueOf(darkModeSupport.getThemeColor())
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
     * @param videoQualityId 画質変更する場合はIDを入れてね。省略しても大丈夫です。
     * @param audioQualityId 音質変更する場合はIDを入れてね。省略しても大丈夫です。
     * @param smileServerLowRequest DMCサーバーじゃなくてSmileサーバーの動画で低画質をリクエストする場合はtrue。DMCサーバーおよびSmileサーバーでも低画質をリクエストしない場合はfalseでいいよ
     * */
    fun coroutine(isGetComment: Boolean = true, videoQualityId: String = "", audioQualityId: String = "", smileServerLowRequest: Boolean = false) {
        // HTML取得
        // val nicoVideoHTML = NicoVideoHTML()
        GlobalScope.launch {
            // smileサーバーの動画は多分最初の視聴ページHTML取得のときに?eco=1をつけないと低画質リクエストできない
            val eco = if (smileServerLowRequest) {
                "1"
            } else {
                ""
            }
            val response = nicoVideoHTML.getHTML(videoId, userSession, eco).await()
            nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            jsonObject = nicoVideoHTML.parseJSON(response?.body?.string())
            // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
            if (nicoVideoHTML.isDMCServer(jsonObject)) {
                // 公式アニメは暗号化されてて見れないので落とす
                if (nicoVideoHTML.isEncryption(jsonObject.toString())) {
                    showToast(getString(R.string.encryption_video_not_play))
                    activity?.runOnUiThread {
                        activity?.finish()
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
                            activity?.runOnUiThread {
                                Snackbar.make(fragment_nicovideo_surfaceview, "${getString(R.string.quality)}：$videoQuality", Snackbar.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }

                    // https://api.dmc.nico/api/sessions のレスポンス
                    val sessionAPIResponse =
                        nicoVideoHTML.callSessionAPI(jsonObject, videoQuality, audioQuality)
                            .await()
                    if (sessionAPIResponse != null) {
                        sessionAPIJSONObject = sessionAPIResponse
                        // 動画URL
                        contentUrl = nicoVideoHTML.getContentURI(jsonObject, sessionAPIJSONObject)
                        // ハートビート処理。これしないと切られる。
                        nicoVideoHTML.heartBeat(jsonObject, sessionAPIJSONObject)
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
                    commentList =
                        ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.body?.string()!!))
                }
            }
            activity?.runOnUiThread {
                if (prefSetting.getBoolean("setting_nicovideo_comment_only", false)) {
                    // 動画を再生しない場合
                    commentOnlyModeEnable()
                } else {
                    // ExoPlayer
                    initVideoPlayer(contentUrl, nicoHistory)
                }
                // コメントFragmentにコメント配列を渡す
                (viewPager.instantiateItem(fragment_nicovideo_viewpager, 1) as DevNicoVideoCommentFragment).apply {
                    commentList.forEach {
                        recyclerViewList.add(it)
                    }
                }
                // タイトル
                videoTitle = jsonObject.getJSONObject("video").getString("title")
                initTitleArea()
                // 共有
                share =
                    ProgramShare((activity as AppCompatActivity), fragment_nicovideo_surfaceview, videoTitle, videoId)
                // 投稿動画をViewPagerに追加
                viewPagerAddAccountFragment(jsonObject)
            }
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
            // 動画のファイル名取得
            val videoFileName = nicoVideoCache.getCacheFolderVideoFileName(videoId)
            if (videoFileName != null) {
                contentUrl =
                    "${nicoVideoCache.getCacheFolderPath()}/$videoId/$videoFileName"
                if (prefSetting.getBoolean("setting_nicovideo_comment_only", false)) {
                    // 動画を再生しない場合
                    commentOnlyModeEnable()
                } else {
                    // ExoPlayer
                    initVideoPlayer(contentUrl, "")
                }
                // コメント取得
                val commentJSON = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
                commentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON))
            } else {
                // 動画が見つからなかった
                Toast.makeText(context, R.string.not_found_video, Toast.LENGTH_SHORT).show()
                activity?.finish()
                return
            }
            // タイトル
            videoTitle = if (nicoVideoCache.existsCacheVideoInfoJSON(videoId)) {
                JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId)).getJSONObject("video")
                    .getString("title")
            } else {
                // 動画ファイルの名前
                nicoVideoCache.getCacheFolderVideoFileName(videoId) ?: videoId
            }
            initTitleArea()
            // 共有
            share =
                ProgramShare((activity as AppCompatActivity), fragment_nicovideo_surfaceview, videoTitle, videoId)
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
        exoPlayer = SimpleExoPlayer.Builder(context!!).build()
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
     * ExoPlayer初期化
     * */
    private fun initVideoPlayer(videoUrl: String?, nicohistory: String?) {
        isRotationProgressSuccessful = false
        exoPlayer.setVideoSurfaceView(fragment_nicovideo_surfaceview)
        // キャッシュ再生と分ける
        if (isCache) {
            // キャッシュ再生
            val dataSourceFactory =
                DefaultDataSourceFactory(context, "TatimiDroid;@takusan_23")
            val videoSource =
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(videoUrl?.toUri())
            exoPlayer.prepare(videoSource)
        } else {
            // SmileサーバーはCookieつけないと見れないため
            val dataSourceFactory =
                DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
            dataSourceFactory.defaultRequestProperties.set("Cookie", nicohistory)
            val videoSource =
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(videoUrl?.toUri())
            exoPlayer.prepare(videoSource)
        }
        // 自動再生
        exoPlayer.playWhenReady = true
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
                if (!isRotationProgressSuccessful) {
                    // 一度だけ実行するように。画面回転時に再生時間を引き継ぐ
                    exoPlayer.seekTo(rotationProgress)
                    isRotationProgressSuccessful = true
                    // 前回見た位置から再生
                    // 画面回転時に２回目以降表示されると邪魔なので制御
                    if (rotationProgress == 0L) {
                        val progress = prefSetting.getLong("progress_$videoId", 0)
                        if (progress != 0L) {
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
                if (playbackState == Player.STATE_BUFFERING) {
                    // STATE_BUFFERING はシークした位置からすぐに再生できないとき。読込み中のこと。
                    showSwipeToRefresh()
                } else {
                    hideSwipeToRefresh()
                }
            }
        })
        // 縦、横取得
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                // アスペクト比が4:3か16:9か
                // 4:3 = 1.333... 16:9 = 1.777..
                val calc = width.toFloat() / height.toFloat()
                // 小数点第二位を捨てる
                val round = BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                // View#post()すればLayoutParamsが動くようになるらしい
                fragment_nicovideo_framelayout.post {
                    when (round) {
                        1.3 -> {
                            // 4:3動画
                            // 横の長さから縦の高さ計算
                            fragment_nicovideo_framelayout.viewTreeObserver.addOnGlobalLayoutListener {
                                val width = fragment_nicovideo_framelayout.width
                                val height = getOldAspectHeightFromWidth(width)
                                val params = LinearLayout.LayoutParams(width, height)
                                fragment_nicovideo_framelayout.layoutParams = params
                            }
                        }
                        1.7 -> {
                            // 16:9動画
                            // 横の長さから縦の高さ計算
                            fragment_nicovideo_framelayout.viewTreeObserver.addOnGlobalLayoutListener {
                                val width = fragment_nicovideo_framelayout.width
                                val height = getAspectHeightFromWidth(width)
                                val params = LinearLayout.LayoutParams(width, height)
                                fragment_nicovideo_framelayout.layoutParams = params
                            }
                        }
                    }
                }
            }
        })
        seekTimer.cancel()
        seekTimer = Timer()
        seekTimer.schedule(timerTask {
            Handler(Looper.getMainLooper()).post {
                if (!isDestory) {
                    if (exoPlayer.isPlaying) {
                        setProgress()
                        drawComment()
                        scroll(exoPlayer.currentPosition / 1000L)
                    }
                }
            }
        }, 1000, 1000)
    }

    /**
     * RecyclerViewをスクロールする
     * @param millSeconds 再生時間（秒）。
     * */
    fun scroll(seconds: Long) {
        // スクロールしない設定？
        if (prefSetting.getBoolean("nicovideo_comment_scroll", false)) {
            return
        }
        // Nullチェック
        if ((viewPager.instantiateItem(fragment_nicovideo_viewpager, 1) as? DevNicoVideoCommentFragment)?.view?.findViewById<RecyclerView>(R.id.activity_nicovideo_recyclerview) != null) {
            val recyclerView =
                (viewPager.instantiateItem(fragment_nicovideo_viewpager, 1) as? DevNicoVideoCommentFragment)?.activity_nicovideo_recyclerview
            val list =
                (viewPager.instantiateItem(fragment_nicovideo_viewpager, 1) as DevNicoVideoCommentFragment).recyclerViewList
            // findを使って条件に合うコメントのはじめの値を取得する。この例では今の時間と同じか大きいくて最初の値。
            val find =
                list.find { arrayList -> (arrayList[4].toFloat() / 100).toInt() >= seconds.toInt() }
            // 配列から位置をとる
            val pos = list.indexOf(find)
            // スクロール
            (recyclerView?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(pos, 0)
        }
    }


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
     * */
    fun drawComment() {
        val drawList = commentList.filter { arrayList ->
            (arrayList[4].toInt() / 100) == (exoPlayer.contentPosition / 1000L).toInt()
        }
        drawList.forEach {
            val commentJSONParse = CommentJSONParse(it[8], "アリーナ",videoId)
            fragment_nicovideo_comment_canvas.postComment(it[2], commentJSONParse)
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
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                exoPlayer.seekTo((seekBar?.progress ?: 0) * 1000L)
            }
        })
    }

    /**
     * 進捗進める
     * */
    private fun setProgress() {
        if (fragment_nicovideo_seek != null) {
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
     * */
    private fun initViewPager() {
        viewPager = DevNicoVideoViewPager(activity as AppCompatActivity, videoId, isCache)
        fragment_nicovideo_viewpager.adapter = viewPager
        fragment_nicovideo_tablayout.setupWithViewPager(fragment_nicovideo_viewpager)
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
            val fragment = fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
            val postFragment = DevNicoVideoPOSTFragment().apply {
                arguments = Bundle().apply {
                    putString("userId", userId)
                }
            }
            // すでにあれば追加しない
            if (!fragment.viewPager.fragmentTabName.contains(nickname)) {
                fragment.viewPager.fragmentList.add(3, postFragment)
                fragment.viewPager.fragmentTabName.add(3, nickname)
                fragment.viewPager.notifyDataSetChanged() // 更新！
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

    /**
     * BroadCastReceiver初期化
     * */
    fun initBroadCastReceiver() {
        nicoVideoCache.initBroadcastReceiver()
    }

    override fun onResume() {
        super.onResume()
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = true
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
        outState.putLong("progress", exoPlayer.currentPosition)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // 保存した値を取得。今回は再生時間
        rotationProgress = (savedInstanceState?.getLong("progress")) ?: 0L
    }

}