package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoAddMylistBottomFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoQualityBottomFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.ProgramShare
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.isConnectionInternet
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo_menu.*

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        if (videoId.contains("so")) {
            fragment_nicovideo_menu_get_cache.visibility = View.GONE
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
            fragment_nicovideo_menu_get_cache.isEnabled = false
            // キャッシュ（動画情報、コメント）再取得ボタン表示
            fragment_nicovideo_menu_re_get_cache.visibility = View.VISIBLE
        } else {
            fragment_nicovideo_menu_re_get_cache.visibility = View.GONE
        }
        // 取得
        val cache = NicoVideoCache(context)
        fragment_nicovideo_menu_get_cache.setOnClickListener {
            if (!isCache) {
                // DevNicoVideoFragment取得
                val devNicoVideoFragment =
                    fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
                // キャッシュ取得。動画+コメント+動画情報
                cache.getCache(
                    videoId,
                    devNicoVideoFragment.jsonObject.toString(),
                    devNicoVideoFragment.contentUrl,
                    devNicoVideoFragment.userSession,
                    devNicoVideoFragment.nicoHistory
                )
            }
        }
    }

    // 再取得ボタン初期化
    private fun initReGetButton() {
        // インターネットに繋がってなければ非表示
        if (!isConnectionInternet(context)) {
            fragment_nicovideo_menu_re_get_cache.visibility = View.GONE
        }
        fragment_nicovideo_menu_re_get_cache.setOnClickListener {
            val nicoVideoCache = NicoVideoCache(context)
            nicoVideoCache.getReGetVideoInfoComment(videoId, userSession, context)
        }
    }


    // 画質変更ボタン初期化
    private fun initQualityButton() {
        val devNicoVideoFragment =
            fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment
        // キャッシュ再生時は非表示
        if (isCache) {
            fragment_nicovideo_menu_quality.visibility = View.GONE
        }
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
                json.getJSONObject("video").getJSONObject("smileInfo").getJSONArray("qualityIds")
                    .toString()
            }
            // 画質変更BottomSheet
            val qualityBottomFragment = DevNicoVideoQualityBottomFragment()
            val bundle = Bundle().apply {
                putString("video_id", videoId)
                putBoolean("is_dmc", isDmcInfo)
                putString("quality", qualityList)
            }
            qualityBottomFragment.arguments = bundle
            qualityBottomFragment.show(fragmentManager!!, "quality")
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

    /**
     * 値セット
     * */
    fun getValue() {
        fragment_nicovideo_menu_3ds_switch.isChecked =
            prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
        fragment_nicovideo_menu_scroll.isChecked =
            prefSetting.getBoolean("nicovideo_comment_scroll", false)
    }

    /**
     * 値保存
     * */
    fun setValue() {
        switchListener(fragment_nicovideo_menu_3ds_switch, "nicovideo_comment_3ds_hidden")
        switchListener(fragment_nicovideo_menu_scroll, "nicovideo_comment_scroll")
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