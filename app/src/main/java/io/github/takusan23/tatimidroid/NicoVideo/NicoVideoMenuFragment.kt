package io.github.takusan23.tatimidroid.NicoVideo

import android.content.*
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Activity.KotehanListActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoAddMylistBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoQualityBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoSkipCustomizeBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.ProgramShare
import io.github.takusan23.tatimidroid.Tool.isConnectionInternet
import io.github.takusan23.tatimidroid.Tool.isNotLoginMode
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo_menu.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * めにゅー
 * 3DSコメント非表示オプションなど
 * */
class NicoVideoMenuFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences
    var userSession = ""
    var videoId = ""

    /** キャッシュ再生ならtrue */
    var isCache = false

    // 共有
    lateinit var share: ProgramShare

    // JSON
    lateinit var jsonObject: JSONObject

    /** ニコ動Fragment取得*/
    private fun requireNicoVideoFragment() = requireParentFragment() as NicoVideoFragment

    // NicoVideoFragmentのViewModelを取得する
    val viewModel: NicoVideoViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // キャッシュ
        isCache = arguments?.getBoolean("cache") ?: false

        // 動画ID
        videoId = arguments?.getString("id", "") ?: ""

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

        // 他のアプリで開くボタン
        initBrowserLaunchButton()

        // NG一覧Activity呼び出す
        initNGActivityButton()

        // スキップ秒数
        initSkipSetting()

        // 3DS消す
        init3DSHide()

        // コテハン一覧Activityボタン初期化
        initKotehanButton()

        // コメント流さないモード
        initCommentCanvasVisibleSwitch()

    }

    private fun initCommentCanvasVisibleSwitch() {
        fragment_nicovideo_menu_hide_comment_canvas.setOnCheckedChangeListener { compoundButton, b ->
            // 設定保存
            prefSetting.edit { putBoolean("nicovideo_comment_canvas_hide", b) }
            // 消す
            requireNicoVideoFragment()?.fragment_nicovideo_comment_canvas?.isVisible = !b
        }
        // 設定読み出し
        fragment_nicovideo_menu_hide_comment_canvas.isChecked = prefSetting.getBoolean("nicovideo_comment_canvas_hide", false)
    }

    private fun initKotehanButton() {
        fragment_nicovideo_menu_kotehan_activity.setOnClickListener {
            // コテハン一覧
            val intent = Intent(context, KotehanListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun init3DSHide() {
        fragment_nicovideo_menu_3ds_switch.isChecked = prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        fragment_nicovideo_menu_3ds_switch.setOnCheckedChangeListener { buttonView, isChecked ->
            // 変更
            prefSetting.edit { putBoolean("nicovideo_comment_3ds_hidden", isChecked) }
            // コメント再適用
            lifecycleScope.launch {
                viewModel.commentFilter()
            }
        }
    }

    private fun initSkipSetting() {
        // スキップ秒数変更画面表示
        fragment_nicovideo_menu_skip_setting.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("video_id", videoId)
            val skipCustomizeBottomFragment = NicoVideoSkipCustomizeBottomFragment()
            skipCustomizeBottomFragment.arguments = bundle
            skipCustomizeBottomFragment.show(parentFragmentManager, "skip")
        }
    }

    private fun initNGActivityButton() {
        fragment_nicovideo_menu_ng_activity.setOnClickListener {
            val intent = Intent(context, NGListActivity::class.java)
            startActivity(intent)
        }
    }

    private fun initBrowserLaunchButton() {
        //ブラウザで再生。
        fragment_nicovideo_menu_browser_launch.setOnClickListener {
            openBrowser("https://nico.ms/$videoId")
        }
    }

    private fun openBrowser(addr: String) {
        val intent = Intent(Intent.ACTION_VIEW, addr.toUri())
        startActivity(intent)
    }


    // ポップアップ再生、バッググラウンド再生ボタン。startVideoPlayService()はNicoVideoPlayServiceに書いてあります。（internal funなのでどこでも呼べる）
    private fun initVideoPlayServiceButton() {
        fragment_nicovideo_menu_popup.setOnClickListener {
            // ポップアップ再生
            startVideoPlayService(context = context, mode = "popup", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
            // Activity落とす
            activity?.finish()
        }
        fragment_nicovideo_menu_background.setOnClickListener {
            // バッググラウンド再生
            startVideoPlayService(context = context, mode = "background", videoId = videoId, isCache = isCache, videoQuality = viewModel.currentVideoQuality, audioQuality = viewModel.currentAudioQuality)
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
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                Configuration.ORIENTATION_LANDSCAPE -> {
                    //横画面
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
        }
    }

    private fun initCopyButton() {
        fragment_nicovideo_menu_copy.setOnClickListener {
            val clipboardManager = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("videoId", videoId))
            Toast.makeText(context, "${getString(R.string.video_id_copy_ok)}：${videoId}", Toast.LENGTH_SHORT).show()
        }
    }

    // 動画再生ボタン
    private fun initPlayButton() {
        fragment_nicovideo_menu_video_play.setOnClickListener {
            requireNicoVideoFragment()?.apply {
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
        // マイリスト追加ボタン。インターネット接続時で動画IDでなければ消す
        if (!isConnectionInternet(context) && (videoId.contains("sm") || videoId.contains("so"))) {
            fragment_nicovideo_menu_add_mylist.isVisible = false
            fragment_nicovideo_menu_atodemiru.isVisible = false
        }
        fragment_nicovideo_menu_add_mylist.setOnClickListener {
            val addMylistBottomFragment = NicoVideoAddMylistBottomFragment()
            val bundle = Bundle()
            bundle.putString("id", videoId)
            addMylistBottomFragment.arguments = bundle
            addMylistBottomFragment.show(parentFragmentManager, "mylist")
        }
        fragment_nicovideo_menu_atodemiru.setOnClickListener {
            // あとで見るに追加する
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            lifecycleScope.launch(errorHandler) {
                // あとで見る追加APIを叩く
                val spMyListAPI = NicoVideoSPMyListAPI()
                val atodemiruResponse = spMyListAPI.addAtodemiruListVideo(userSession, videoId)
                if (!atodemiruResponse.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${atodemiruResponse.code}")
                    return@launch
                }
                // 成功したか
                when (atodemiruResponse.code) {
                    201 -> {
                        // 成功時
                        showToast(getString(R.string.atodemiru_ok))
                    }
                    200 -> {
                        // すでに追加済み
                        showToast(getString(R.string.already_added))
                    }
                    else -> {
                        // えらー
                        showToast(getString(R.string.error))
                    }
                }
            }
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
                // キャッシュ取得サービス起動
                startCacheService(context, videoId)
            }
        }
        // ログインするかはService側に書いてあるので。。。
        fragment_nicovideo_menu_get_cache_eco.setOnClickListener {
            if (!isCache) {
                // キャッシュ取得サービス起動
                startCacheService(context, videoId)
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
        // キャッシュ再生時またはキャッシュ優先再生時は非表示
        if (isCache) {
            fragment_nicovideo_menu_quality.visibility = View.GONE
        } else {
            fragment_nicovideo_menu_quality.setOnClickListener {
                // DevNicoVideoFragmentから持ってくる
                val json = viewModel.nicoVideoJSON.value ?: return@setOnClickListener
                // DmcInfoかSmileサーバーか
                val isDmcInfo = viewModel.isDMCServer
                // 画質一覧取得
                val qualityList = if (isDmcInfo) {
                    json.getJSONObject("video").getJSONObject("dmcInfo").getJSONObject("quality").toString()
                } else {
                    json.getJSONObject("video").getJSONObject("smileInfo").getJSONArray("qualityIds").toString()
                }
                // 画質変更BottomSheet
                val qualityBottomFragment = NicoVideoQualityBottomFragment()
                val bundle = Bundle().apply {
                    putString("video_id", videoId)
                    putBoolean("is_dmc", isDmcInfo)
                    putString("quality", qualityList)
                    putString("select", viewModel.currentVideoQuality)
                }
                qualityBottomFragment.arguments = bundle
                qualityBottomFragment.show(parentFragmentManager, "quality")
            }
        }
    }


    // 共有
    fun initShare() {
        // 写真付き共有
        fragment_nicovideo_menu_share_media_attach.setOnClickListener {
            requireNicoVideoFragment()?.apply {
                // 再生時間も載せる
                val currentPos = requireNicoVideoFragment()?.exoPlayer?.currentPosition
                val currentTime = DateUtils.formatElapsedTime(currentPos ?: 0 / 1000)
                share.shareAttachImage(currentTime)
            }
        }
        // 共有
        fragment_nicovideo_menu_share.setOnClickListener {
            requireNicoVideoFragment()?.apply {
                // 再生時間も載せる
                val currentPos = requireNicoVideoFragment()?.exoPlayer?.currentPosition
                val currentTime = DateUtils.formatElapsedTime(currentPos ?: 0 / 1000)
                share.showShareScreen(currentTime)
            }
        }
    }

    // 音量コントロール
    fun initVolumeControl() {
        // 音量
        fragment_nicovideo_menu_volume_seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                requireNicoVideoFragment()?.exoPlayer?.volume = (progress.toFloat() / 10)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }
        })
        if (requireNicoVideoFragment()?.initExoPlayer() == true) {
            fragment_nicovideo_menu_volume_seek.progress = ((requireNicoVideoFragment()?.exoPlayer?.volume ?: 1F) * 10).toInt()
        }
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}