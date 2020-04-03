package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
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
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoViewPager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoHTML
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo_comment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

/**
 * 開発中のニコ動クライアント（？）
 * */
class DevNicoVideoFragment : Fragment() {
    lateinit var prefSetting: SharedPreferences
    lateinit var exoPlayer: SimpleExoPlayer
    lateinit var darkModeSupport: DarkModeSupport

    // 必要なやつ
    var userSession = ""
    var videoId = ""

    // 一度だけ実行する
    var isInit = true

    // キャッシュ取得用
    lateinit var nicoVideoCache: NicoVideoCache
    var isCache = false
    var contentUrl = ""
    var nicoHistory = ""
    lateinit var jsonObject: JSONObject

    // データ取得からハートビートまで扱う
    val nicoVideoHTML = NicoVideoHTML()

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

    // 設定項目
    // 3DSコメント非表示
    var is3DSCommentHide = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context!!)
        nicoVideoCache = NicoVideoCache(context)
        userSession = prefSetting.getString("user_session", "") ?: ""
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

        exoPlayer = SimpleExoPlayer.Builder(context!!).build()
        // キャッシュ再生のときとそうじゃないとき
        if (isCache) {
            // キャッシュ再生
            cachePlay()
        } else {
            // データ取得
            coroutine()
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
    fun initTitleArea(jsonObject: JSONObject) {
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
        fragment_nicovideo_comment_title.text = jsonObject.getJSONObject("video").getString("title")
        fragment_nicovideo_comment_videoid.text = videoId
    }

    /**
     * データ取得から動画再生/コメント取得まで
     * */
    fun coroutine() {
        // HTML取得
        val nicoVideoHTML = NicoVideoHTML()
        GlobalScope.launch {
            val response = nicoVideoHTML.getHTML(videoId, userSession).await()
            nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
            jsonObject = nicoVideoHTML.parseJSON(response?.body?.string())
            // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
            if (nicoVideoHTML.isDMCServer(jsonObject)) {
                // https://api.dmc.nico/api/sessions のレスポンス
                val sessionAPIJSONObject = nicoVideoHTML.callSessionAPI(jsonObject).await()
                if (sessionAPIJSONObject != null) {
                    // 動画URL
                    contentUrl = nicoVideoHTML.getContentURI(jsonObject, sessionAPIJSONObject)
                    val heartBeatURL =
                        nicoVideoHTML.getHeartBeatURL(jsonObject, sessionAPIJSONObject)
                    val postData =
                        nicoVideoHTML.getSessionAPIDataObject(jsonObject, sessionAPIJSONObject)
                    // ハートビート処理
                    activity?.runOnUiThread {
                        heatBeat(heartBeatURL, postData)
                    }
                }
            } else {
                // Smileサーバー。動画URL取得
                contentUrl = nicoVideoHTML.getContentURI(jsonObject, null)
            }
            // コメント取得
            val commentJSON = nicoVideoHTML.getComment(videoId, userSession, jsonObject).await()
            if (commentJSON != null) {
                commentList =
                    ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON.body?.string()!!))
            }
            activity?.runOnUiThread {
                // ExoPlayer
                initVideoPlayer(contentUrl, nicoHistory)
                // コメントFragmentにコメント配列を渡す
                (viewPager.instantiateItem(fragment_nicovideo_viewpager, 1) as DevNicoVideoCommentFragment).apply {
                    commentList.forEach {
                        recyclerViewList.add(it)
                    }
                }
                // タイトル
                initTitleArea(jsonObject)
            }
        }
    }

    /**
     * キャッシュ再生
     * */
    fun cachePlay() {
        val cacheVideoPath = nicoVideoCache.getCacheFolderVideoFilePath(videoId)
        initVideoPlayer(cacheVideoPath, "")
        // コメント取得
        val commentJSON = nicoVideoCache.getCacheFolderVideoCommentText(videoId)
        commentList = ArrayList(nicoVideoHTML.parseCommentJSON(commentJSON))
        // コメントFragmentにコメント配列を渡す
        (viewPager.instantiateItem(fragment_nicovideo_viewpager, 1) as DevNicoVideoCommentFragment).apply {
            commentList.forEach {
                recyclerViewList.add(it)
            }
            /**
             * 本来ならここでRecyclerViewの更新をかけますが、
             * 残念ながらViewPagerのDevNicoVideoCommentFragment初期化より速くここまでだどりつくため初期化してないエラーでちゃう
             * ので更新はコメントアウトした
             * */
            // nicoVideoAdapter.notifyDataSetChanged()
        }
        // タイトル
        initTitleArea(JSONObject(nicoVideoCache.getCacheFolderVideoInfoText(videoId)))
    }

    /**
     * ExoPlayer初期化
     * */
    private fun initVideoPlayer(videoUrl: String?, nicohistory: String?) {
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
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (!isDestory) {
                    if (exoPlayer.isPlaying) {
                        setProgress()
                        drawComment()
                        scroll(exoPlayer.currentPosition / 1000L)
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(runnable, 0)
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
     * ハートビート処理を行う。
     * 40秒ごとに送信するらしい。POSTする内容はsession_apiでAPI叩いた後のJSONのdataの中身。
     * jsonの中身全てではない。
     * @param url ハートビート用URL
     * @param json POSTする内容
     * */
    private fun heatBeat(url: String?, json: String?) {
        if (url == null && json == null) {
            return
        }
        val runnable = object : Runnable {
            override fun run() {
                // 終了したら使わない。
                if (!isDestory) {
                    nicoVideoHTML.postHeartBeat(url, json) {
                        activity?.runOnUiThread {
                            Handler().postDelayed(this, 40 * 1000)
                        }
                    }
                }
            }
        }
        Handler().postDelayed(runnable, 40 * 1000)
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
            val commentJSONParse = CommentJSONParse(it[8], "アリーナ")
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
                //exoPlayer.seekTo(progress * 1000L)
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
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        val formattedTime = simpleDateFormat.format(exoPlayer.currentPosition)
        fragment_nicovideo_progress_text.text = formattedTime
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
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        // Fragment終了のち止める
        if (::exoPlayer.isInitialized) {
            exoPlayer.release()
        }
        nicoVideoCache.destroy()
        nicoVideoHTML.destory()
        isDestory = true
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