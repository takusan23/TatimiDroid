package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.os.Build
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
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoCacheFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoMyListFragment
import io.github.takusan23.tatimidroid.Fragment.DialogBottomSheet
import io.github.takusan23.tatimidroid.NicoAPI.*
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoMyListAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.*
import io.github.takusan23.tatimidroid.Service.startBackgroundPlaylistPlayService
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.isNotLoginMode
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
        nicoVideoHTML = NicoVideoHTML()

        // 画面回転するとNicoVideoData失うので無いときはもう落とす
        if (!::nicoVideoData.isInitialized) {
            dismiss()
            return
        }

        // タイトル、ID設定
        bottom_fragment_nicovideo_list_menu_title.text = nicoVideoData.title
        bottom_fragment_nicovideo_list_menu_id.text = nicoVideoData.videoId

        // コピーボタン
        initCopyButton()

        // マイリスト登録ボタン
        mylistButton()

        // キャッシュ関係
        cacheButton()

        // 再生、ポップアップ再生、バッググラウンド再生ボタン初期化
        playServiceButton()

        // キャッシュ用連続再生
        initCachePlaylistPlay()

    }

    private fun initCachePlaylistPlay() {
        if (!nicoVideoData.isCache) {
            bottom_fragment_nicovideo_list_menu_playlist_background.visibility = View.GONE
        }
        bottom_fragment_nicovideo_list_menu_playlist_background.setOnClickListener {
            // 第二引数で再生開始動画指定
            startBackgroundPlaylistPlayService(context, nicoVideoData.videoId)
            dismiss()
        }
    }

    // ポップアップ再生、バッググラウンド再生ボタン初期化
    private fun playServiceButton() {
        bottom_fragment_nicovideo_list_menu_popup.setOnClickListener { startVideoPlayService(context, "popup", nicoVideoData.videoId, nicoVideoData.isCache) }
        bottom_fragment_nicovideo_list_menu_background.setOnClickListener { startVideoPlayService(context, "background", nicoVideoData.videoId, nicoVideoData.isCache) }
        bottom_fragment_nicovideo_list_menu_play.setOnClickListener {
            // 通常再生
            val nicoVideoActivity = Intent(context, NicoVideoActivity::class.java).apply {
                putExtra("id", nicoVideoData.videoId)
                putExtra("cache", nicoVideoData.isCache)
            }
            startActivity(nicoVideoActivity)
        }
        // 強制エコノミーはキャッシュでは塞ぐ
        if (nicoVideoData.isCache) {
            bottom_fragment_nicovideo_list_menu_economy_play.visibility = View.GONE
        }
        bottom_fragment_nicovideo_list_menu_economy_play.setOnClickListener {
            // エコノミーで再生
            val nicoVideoActivity = Intent(context, NicoVideoActivity::class.java).apply {
                putExtra("id", nicoVideoData.videoId)
                putExtra("eco", true)
            }
            startActivity(nicoVideoActivity)
        }
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
        // 非ログインモード時も消す
        if (isNotLoginMode(context)) {
            bottom_fragment_nicovideo_list_menu_mylist.visibility = View.GONE
        }
        bottom_fragment_nicovideo_list_menu_mylist.setOnClickListener {
            if (nicoVideoData.isMylist) {
                // 本当に消していい？
                val buttonItems = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.delete), R.drawable.ic_outline_delete_24px))
                    add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel), R.drawable.ic_arrow_back_black_24dp, Color.parseColor("#ff0000")))
                }
                DialogBottomSheet(getString(R.string.mylist_video_delete), buttonItems) { i, bottomSheetDialogFragment ->
                    if (i == 0) {
                        // 消す
                        GlobalScope.launch {
                            // マイリストFragment
                            val myListFragment =
                                fragmentManager?.findFragmentById(R.id.fragment_video_list_linearlayout) as DevNicoVideoMyListFragment
                            // マイリスト削除API叩く。スマホ版のAPI
                            val nicoVideoSPMyListAPI = NicoVideoSPMyListAPI()
                            // 削除API叩く
                            val deleteResponse =
                                nicoVideoSPMyListAPI.deleteMyListVideo(myListFragment.myListId, nicoVideoData.mylistItemId, userSession)
                                    .await()
                            if (deleteResponse.isSuccessful) {
                                showToast(getString(R.string.mylist_delete_ok))
                                activity?.runOnUiThread {
                                    this@DevNicoVideoListMenuBottomFragment.dismiss()
                                    // 再読み込み
                                    myListFragment.getMyListVideoItems(myListFragment.myListId)
                                }
                            } else {
                                showToast("${getString(R.string.error)}\n${deleteResponse.code}")
                            }
                        }
                    }
                }.apply {
                    show(this@DevNicoVideoListMenuBottomFragment.fragmentManager!!, "delete")
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
            // 本当に消していいか聞くダイアログ作成
            val buttonItems = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cache_delete), R.drawable.ic_outline_delete_24px))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel), R.drawable.ic_arrow_back_black_24dp, Color.parseColor("#ff0000")))
            }
            val okCancelBottomSheetFragment =
                DialogBottomSheet(getString(R.string.cache_delete_message), buttonItems) { i, bottomSheetDialogFragment ->
                    if (i == 0) {
                        nicoVideoCache.deleteCache(nicoVideoData.videoId)
                        val fragment =
                            fragmentManager?.findFragmentById(R.id.main_activity_linearlayout) as DevNicoVideoCacheFragment
                        // 再読み込み
                        fragment.load()
                        dismiss()
                    }
                }
            okCancelBottomSheetFragment.show(childFragmentManager, "delete_dialog")
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