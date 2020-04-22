package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoCacheFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoMyListFragment
import io.github.takusan23.tatimidroid.NicoAPI.*
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoMyListAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.GetCacheService
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_list_menu.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * マイリスト、キャッシュ取得ボタンがあるBottomSheet
 * */
class DevNicoVideoListMenuBottomFragment : BottomSheetDialogFragment() {

    // データもらう
    lateinit var nicoVideoData: NicoVideoData
    lateinit var nicoVideoHTML: NicoVideoHTML
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_list_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""
        nicoVideoHTML =
            NicoVideoHTML()

        // タイトル、ID設定
        bottom_fragment_nicovideo_list_menu_title.text = nicoVideoData.title
        bottom_fragment_nicovideo_list_menu_id.text = nicoVideoData.videoId

        // コピーボタン
        initCopyButton()

        // マイリスト登録ボタン
        mylistButton()

        // キャッシュ関係
        cacheButton()

        // ポップアップ再生、バッググラウンド再生ボタン初期化
        playServiceButton()

    }

    // ポップアップ再生、バッググラウンド再生ボタン初期化
    private fun playServiceButton() {
        bottom_fragment_nicovideo_list_menu_popup.setOnClickListener { startVideoPlayService(context, "popup", nicoVideoData.videoId, nicoVideoData.isCache) }
        bottom_fragment_nicovideo_list_menu_background.setOnClickListener { startVideoPlayService(context, "background", nicoVideoData.videoId, nicoVideoData.isCache) }
    }

    // IDコピーボタン
    private fun initCopyButton() {
        bottom_fragment_nicovideo_list_menu_copy.setOnClickListener {
            val clipboardManager =
                context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("videoId", nicoVideoData.videoId))
            Toast.makeText(context, "${getString(R.string.video_id_copy_ok)}：${nicoVideoData.videoId}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    // マイリスト。そのうち作る
    private fun mylistButton() {
        // 動画ID以外はマイリスト登録ボタンを消す
        if (nicoVideoData.videoId.contains("sm") || nicoVideoData.videoId.contains("so")) {
            bottom_fragment_nicovideo_list_menu_mylist.visibility = View.VISIBLE
        } else {
            bottom_fragment_nicovideo_list_menu_mylist.visibility = View.GONE
        }
        // マイリスト画面の場合は消すに切り替える
        if (nicoVideoData.isMylist) {
            bottom_fragment_nicovideo_list_menu_mylist.text = getString(R.string.mylist_delete)
        }
        bottom_fragment_nicovideo_list_menu_mylist.setOnClickListener {
            if (nicoVideoData.isMylist) {
                // マイリスト一覧。削除ボタン
                GlobalScope.launch {
                    val myListAPI =
                        NicoVideoMyListAPI()
                    val tokenHTML = myListAPI.getMyListHTML(userSession).await()
                    if (tokenHTML.isSuccessful) {
                        val token = myListAPI.getToken(tokenHTML.body?.string()) ?: ""
                        val fragment =
                            fragmentManager?.findFragmentById(R.id.fragment_video_list_linearlayout)
                        if (fragment is DevNicoVideoMyListFragment) {
                            // 削除API叩く
                            val deleteResponse =
                                myListAPI.mylistDeleteVideo(fragment.myListId, nicoVideoData.mylistItemId, token, userSession)
                                    .await()
                            if (deleteResponse.isSuccessful) {
                                showToast(getString(R.string.mylist_delete_ok))
                                activity?.runOnUiThread {
                                    // BottomSheet消して再読み込み
                                    this@DevNicoVideoListMenuBottomFragment.dismiss()
                                    fragment.getMylistItems(fragment.myListId)
                                }
                            } else {
                                showToast("${getString(R.string.error)}\n${deleteResponse.code}")
                            }
                        }
                    }
                }
            } else {
                // マイリスト一覧以外。追加ボタン
                val mylistBottomFragment = DevNicoVideoAddMylistBottomFragment()
                val bundle = Bundle().apply {
                    putString("id", nicoVideoData.videoId)
                }
                mylistBottomFragment.arguments = bundle
                mylistBottomFragment.show((activity as AppCompatActivity).supportFragmentManager, "mylist")
            }
        }
    }

    // キャッシュ再取得とか削除とか（削除以外未実装）
    private fun cacheButton() {
        // キャッシュ関係
        val nicoVideoCache = NicoVideoCache(context)

        if (nicoVideoData.isCache) {
            // キャッシュのときは再取得メニュー表示させる
            bottom_fragment_nicovideo_list_menu_cache_menu.visibility = View.VISIBLE
            bottom_fragment_nicovideo_list_menu_get_cache.visibility = View.GONE
            bottom_fragment_nicovideo_list_menu_get_cache_economy.visibility = View.GONE
        } else {
            // キャッシュ無いときは取得ボタンを置く
            bottom_fragment_nicovideo_list_menu_cache_menu.visibility = View.GONE
            bottom_fragment_nicovideo_list_menu_get_cache.visibility = View.VISIBLE
            bottom_fragment_nicovideo_list_menu_get_cache_economy.visibility = View.VISIBLE
        }

        // キャッシュ取得
        bottom_fragment_nicovideo_list_menu_get_cache.setOnClickListener {
            // キャッシュ取得Service起動
            startCacheService(context, nicoVideoData.videoId, false)
            // 閉じる
            dismiss()
        }

        // キャッシュ取得（エコノミーモード）
        bottom_fragment_nicovideo_list_menu_get_cache_economy.setOnClickListener {
            // キャッシュ取得Service起動
            startCacheService(context, nicoVideoData.videoId, true)
            // 閉じる
            dismiss()
        }

        // キャッシュ削除
        bottom_fragment_nicovideo_list_menu_delete_cache.setOnClickListener {
            nicoVideoCache.deleteCache(nicoVideoData.videoId)
            val fragment = fragmentManager?.findFragmentById(R.id.fragment_video_list_linearlayout)
            if (fragment is DevNicoVideoCacheFragment) {
                // 再読み込み
                fragment.load()
            }
            dismiss()
        }

        // 動画ID以外は非表示にする処理
        if (nicoVideoData.videoId.contains("sm") || nicoVideoData.videoId.contains("so")) {
            bottom_fragment_nicovideo_list_menu_re_get_cache.visibility = View.VISIBLE
        } else {
            bottom_fragment_nicovideo_list_menu_re_get_cache.visibility = View.GONE
        }
        // キャッシュの動画情報、コメント更新
        bottom_fragment_nicovideo_list_menu_re_get_cache.setOnClickListener {
            // キャッシュ取得中はBottomFragmentを消させないようにする
            this.isCancelable = false
            bottom_fragment_nicovideo_list_menu_re_get_cache.text =
                getString(R.string.cache_updateing)
            // 再取得
            nicoVideoCache.getReGetVideoInfoComment(nicoVideoData.videoId, userSession, context) {
                // 取得できたら閉じる
                Handler(Looper.getMainLooper()).post {
                    this@DevNicoVideoListMenuBottomFragment.dismiss()
                }
            }
        }

        // XML形式をJSON形式に変換する
        // コメントファイル（XML）があれば表示させる
        val xmlCommentJSON = XMLCommentJSON(context)
        if (xmlCommentJSON.commentXmlFilePath(nicoVideoData.videoId) != null) {
            bottom_fragment_nicovideo_list_menu_xml_to_json.visibility = View.VISIBLE
        }
        bottom_fragment_nicovideo_list_menu_xml_to_json.setOnClickListener {
            // BottomSheet消えないように。
            this@DevNicoVideoListMenuBottomFragment.isCancelable = false
            showToast(getString(R.string.wait))
            GlobalScope.launch {
                val status = xmlCommentJSON.xmlToJSON(nicoVideoData.videoId).await()
                showToast(getString(R.string.xml_to_json_complete))
                // 消す
                activity?.runOnUiThread {
                    this@DevNicoVideoListMenuBottomFragment.dismiss()
                }
            }
        }

    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nicoVideoHTML.destory()
    }

}