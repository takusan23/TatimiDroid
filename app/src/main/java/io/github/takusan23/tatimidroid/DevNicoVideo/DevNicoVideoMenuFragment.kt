package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoAddMylistBottomFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoQualityBottomFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.ProgramShare
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.NicoLivePlayService
import io.github.takusan23.tatimidroid.Service.NicoVideoPlayService
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.isConnectionInternet
import io.github.takusan23.tatimidroid.isNotLoginMode
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo_menu.*
import org.json.JSONObject

/**
 * めにゅー
 * 3DSコメント非表示オプションなど
 * */
class DevNicoVideoMenuFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    var userSession = ""
    var videoId = ""

    // キャッシュ再生ならtrue
    var isCache = false

    // 共有
    lateinit var share: ProgramShare

    // JSON
    lateinit var jsonObject: JSONObject

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // キャッシュ
        isCache = arguments?.getBoolean("cache") ?: false

        // 設定保存、取得
        getValue()
        setValue()

        // 動画ID
        videoId = arguments?.getString("id") ?: ""

        // そもそもキャッシュ取得できない（アニメ公式はhls形式でAES-128で暗号化されてるので取れない）動画はキャッシュボタン非表示
        if (::jsonObject.isInitialized) {
            if (NicoVideoHTML().isEncryption(jsonObject.toString())) {
                fragment_nicovideo_menu_get_cache.visibility = View.GONE
                fragment_nicovideo_menu_get_cache_eco.visibility = View.GONE
            }
        }

        // ログインしないモード用
        if (isNotLoginMode(context)) {
            fragment_nicovideo_menu_add_mylist.visibility = View.GONE
        }

        // マイリスト追加ボタン
        initMylistButton()

        // キャッシュ取得ボタン
        initCacheButton()

        // 再取得ボタン
        initReGetButton()

        // 画質変更
        initQualityButton()

        // 共有できるようにする
        initShare()

        // 動画再生
        initPlayButton()

        // コピーボタン
        initCopyButton()

        // 強制画面回転ボタン
        initRotationButton()

        // ポップアップ再生、バッググラウンド再生ボタン
        initVideoPlayServiceButton()

        // 音量コントロール
        initVolumeControl()

    }


    // ポップアップ再生、バッググラウンド再生ボタン
    private fun initVideoPlayServiceButton() {
        fragment_nicovideo_menu_popup.setOnClickListener {
            startVideoPlayService(context, "popup", videoId, isCache)
            // Activity落とす
            activity?.finish()
        }
        fragment_nicovideo_menu_background.setOnClickListener {
            startVideoPlayService(context, "background", videoId, isCache)
            // Activity落とす
            activity?.finish()
        }
    }

    private fun initRotationButton() {
        fragment_nicovideo_menu_rotation.setOnClickListener {
            val conf = resources.configuration
            //live_video_view.stopPlayback()
            when (conf.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> {
                    //縦画面
                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                Configuration.ORIENTATION_LANDSCAPE -> {
                    //横画面
                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    private fun initCopyButton() {
        fragment_nicovideo_menu_copy.setOnClickListener {
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("videoId", videoId))
            Toast.makeText(context, "${getString(R.string.video_id_copy_ok)}：${videoId}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // 動画再生ボタン
    private fun initPlayButton() {
        val devNicoVideoFragment =
            fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
        fragment_nicovideo_menu_video_play.setOnClickListener {
            devNicoVideoFragment.apply {
                if (fragment_nicovideo_framelayout.visibility == View.GONE) {
                    commentOnlyModeDisable()
                } else {
                    commentOnlyModeEnable()
                }
            }
        }
    }

    // マイリスト追加ボタン初期化
    private fun initMylistButton() {
        // マイリスト追加ボタン。インターネット未接続時は非表示にする
        if (!isConnectionInternet(context)) {
            fragment_nicovideo_menu_add_mylist.visibility = View.GONE
        }
        fragment_nicovideo_menu_add_mylist.setOnClickListener {
            val addMylistBottomFragment = DevNicoVideoAddMylistBottomFragment()
            val bundle = Bundle()
            bundle.putString("id", videoId)
            addMylistBottomFragment.arguments = bundle
            addMylistBottomFragment.show(fragmentManager!!, "mylist")
        }
    }

    // キャッシュボタン初期化
    private fun initCacheButton() {
        // キャッシュ
        if (isCache) {
            // キャッシュ取得ボタン塞ぐ
            fragment_nicovideo_menu_get_cache.visibility = View.GONE
            fragment_nicovideo_menu_get_cache_eco.visibility = View.GONE
            // キャッシュ（動画情報、コメント）再取得ボタン表示
            fragment_nicovideo_menu_re_get_cache.visibility = View.VISIBLE
        } else {
            fragment_nicovideo_menu_re_get_cache.visibility = View.GONE
        }
        // 取得
        fragment_nicovideo_menu_get_cache.setOnClickListener {
            if (!isCache) {
                // DevNicoVideoFragment取得
                val devNicoVideoFragment =
                    fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
                // キャッシュ取得サービス起動
                startCacheService(context, devNicoVideoFragment.videoId)
            }
        }
        // ログインするかはService側に書いてあるので。。。
        fragment_nicovideo_menu_get_cache_eco.setOnClickListener {
            if (!isCache) {
                // DevNicoVideoFragment取得
                val devNicoVideoFragment =
                    fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
                // キャッシュ取得サービス起動
                startCacheService(context, devNicoVideoFragment.videoId)
            }
        }
    }

    // 再取得ボタン初期化
    private fun initReGetButton() {
        val nicoVideoCache =
            NicoVideoCache(context)
        // インターネットに繋がってなければ非表示
        if (!isConnectionInternet(context)) {
            fragment_nicovideo_menu_re_get_cache.visibility = View.GONE
        }
        // 動画IDじゃない場合も非表示
        if (!nicoVideoCache.checkVideoId(videoId)) {
            fragment_nicovideo_menu_re_get_cache.visibility = View.GONE
        }
        fragment_nicovideo_menu_re_get_cache.setOnClickListener {
            nicoVideoCache.getReGetVideoInfoComment(videoId, userSession, context)
        }
    }


    // 画質変更ボタン初期化
    private fun initQualityButton() {
        val devNicoVideoFragment =
            fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
        // キャッシュ再生時またはキャッシュ優先再生時は非表示
        if (isCache || devNicoVideoFragment.canUsePriorityCachePlay) {
            fragment_nicovideo_menu_quality.visibility = View.GONE
        } else {
            fragment_nicovideo_menu_quality.setOnClickListener {
                // DevNicoVideoFragmentから持ってくる
                val json = devNicoVideoFragment.jsonObject
                // DmcInfoかSmileサーバーか
                val isDmcInfo = devNicoVideoFragment.nicoVideoHTML.isDMCServer(json)
                // 画質一覧取得
                val qualityList = if (isDmcInfo) {
                    json.getJSONObject("video").getJSONObject("dmcInfo").getJSONObject("quality")
                        .toString()
                } else {
                    json.getJSONObject("video").getJSONObject("smileInfo")
                        .getJSONArray("qualityIds")
                        .toString()
                }
                // 画質変更BottomSheet
                val qualityBottomFragment = DevNicoVideoQualityBottomFragment()
                val bundle = Bundle().apply {
                    putString("video_id", videoId)
                    putBoolean("is_dmc", isDmcInfo)
                    putString("quality", qualityList)
                    putString("select", devNicoVideoFragment.selectQuality)
                }
                qualityBottomFragment.arguments = bundle
                qualityBottomFragment.show(fragmentManager!!, "quality")
            }
        }
    }


    // 共有
    fun initShare() {
        val nicoVideoFragment = fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
        // 写真付き共有
        fragment_nicovideo_menu_share_media_attach.setOnClickListener {
            nicoVideoFragment.share.shareAttacgImage()
        }
        // 共有
        fragment_nicovideo_menu_share.setOnClickListener {
            nicoVideoFragment.share.showShareScreen()
        }

    }

    // 音量コントロール
    fun initVolumeControl() {
        val nicoVideoFragment = fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
        // 音量
        fragment_nicovideo_menu_volume_seek.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (nicoVideoFragment.isInitExoPlayer()) {
                    nicoVideoFragment.exoPlayer.volume = (progress.toFloat() / 10)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        if (nicoVideoFragment.isInitExoPlayer()) {
            fragment_nicovideo_menu_volume_seek.progress =
                (nicoVideoFragment.exoPlayer.volume * 10).toInt()
        }
    }

    /**
     * 値セット
     * */
    fun getValue() {
        fragment_nicovideo_menu_3ds_switch.isChecked =
            prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        fragment_nicovideo_menu_scroll.isChecked =
            prefSetting.getBoolean("nicovideo_comment_scroll", false)
        fragment_nicovideo_menu_hide_comment_search.isChecked =
            prefSetting.getBoolean("nicovideo_hide_search_button", true)
    }

    /**
     * 値保存
     * */
    fun setValue() {
        switchListener(fragment_nicovideo_menu_3ds_switch, "nicovideo_comment_3ds_hidden")
        switchListener(fragment_nicovideo_menu_scroll, "nicovideo_comment_scroll")
        switchListener(fragment_nicovideo_menu_hide_comment_search, "nicovideo_hide_search_button")
    }

    private fun switchListener(switch: Switch, key: String) {
        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit { putBoolean(key, isChecked) }
        }
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}