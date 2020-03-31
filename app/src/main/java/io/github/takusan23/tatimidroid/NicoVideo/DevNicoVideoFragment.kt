package io.github.takusan23.tatimidroid.NicoVideo

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.CommentCanvas
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoHTML
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.collections.ArrayList

class DevNicoVideoFragment : Fragment() {
    lateinit var prefSetting: SharedPreferences
    lateinit var exoPlayer: SimpleExoPlayer

    // 必要なやつ
    var userSession = ""
    var videoId = ""

    // データ取得
    val nicoVideoHTML = NicoVideoHTML()

    // コメント配列
    var commentList = arrayListOf<ArrayList<String>>()

    var isDestory = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context!!)
        // 動画ID
        videoId = arguments?.getString("id") ?: ""
        userSession = prefSetting.getString("user_session", "") ?: ""

        // データ取得
        coroutine()

        // Fragmentセットする
        initFragment()

    }

    /**
     * データ取得
     * */
    fun coroutine() {
        // HTML取得
        val nicoVideoHTML = NicoVideoHTML()
        GlobalScope.launch {
            val response = nicoVideoHTML.getHTML(videoId, userSession).await()
            val nicoHistory = nicoVideoHTML.getNicoHistory(response)
            val jsonObject = nicoVideoHTML.parseJSON(response?.body?.string())
            // 動画URL
            val contentUrl = nicoVideoHTML.getContentURI(jsonObject).await()
            println(contentUrl)
            // ExoPlayer
            activity?.runOnUiThread {
                initVideoPlayer(contentUrl, nicoHistory)
            }
        }
    }

    /**
     * ExoPlayer初期化
     * */
    private fun initVideoPlayer(videoUrl: String?, nicohistory: String?) {
        exoPlayer = SimpleExoPlayer.Builder(context!!).build()
        exoPlayer.setVideoSurfaceView(fragment_nicovideo_surfaceview)
        // SmileサーバーはCookieつけないと見れないため
        val dataSourceFactory =
            DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
        dataSourceFactory.defaultRequestProperties.set("Cookie", nicohistory)
        val videoSource =
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(videoUrl?.toUri())
        exoPlayer.prepare(videoSource)
        // 自動再生
        exoPlayer.playWhenReady = true
        // 再生ボタン押したとき
        fragment_nicovideo_play_button.setOnClickListener {
            // コメント流し制御
            exoPlayer.playWhenReady = !exoPlayer.playWhenReady
            fragment_nicovideo_comment_canvas.isPause = !exoPlayer.playWhenReady
            // アイコン入れ替え
            val drawable = if (exoPlayer.playWhenReady) {
                context?.getDrawable(R.drawable.ic_pause_black_24dp)
            } else {
                context?.getDrawable(R.drawable.ic_play_arrow_24px)
            }
            fragment_nicovideo_play_button.setImageDrawable(drawable)
        }

        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)
                initSeekBar()
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
                activity?.runOnUiThread {
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

        // 進捗
        val handler = Handler()
        val runnable = object : Runnable {
            override fun run() {
                if (!isDestory) {
                    if (exoPlayer.playWhenReady) {
                        setProgress()
                        drawComment()
                    }
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.postDelayed(runnable, 0)

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
     * Fragmentをセットする
     * */
    private fun initFragment() {
        //FragmentにID詰める
        val bundle = Bundle()
        bundle.putString("id", videoId)
        val fragment = NicoVideoCommentFragment()
        fragment.arguments = bundle
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
                        val fragment = NicoVideoInfoFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
                            .commit()
                    }
                    getString(R.string.menu) -> {
                        val fragment = NicoVideoMenuFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
                            .commit()
                    }
                    getString(R.string.parent_contents) -> {
                        val fragment = NicoVideoContentTreeFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_nicovideo_fragment_linearlayout, fragment)
                            .commit()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Fragment終了のち止める
        exoPlayer.release()
        nicoVideoHTML.destory()
        isDestory = true
    }

}