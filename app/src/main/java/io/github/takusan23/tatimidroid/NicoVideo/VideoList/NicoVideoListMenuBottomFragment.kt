package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoAddMylistBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.Fragment.DialogBottomSheet
import io.github.takusan23.tatimidroid.NicoAPI.*
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startBackgroundPlaylistPlayService
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.isNotLoginMode
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_list_menu.*
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist.*
import kotlinx.coroutines.*

/**
 * マイリスト、キャッシュ取得ボタンがあるBottomSheet。動画IDとキャッシュかどうかを入れてください。
 * 入れてほしいもの
 * video_id | String  | 動画ID。画面回転時に詰むのでこっちがいい？
 * is_cache | Boolean | キャッシュの場合はtrue
 * --- できれば（インターネット接続無いと詰む） ---
 * data     | NicoVideoData | 入ってる場合はインターネットでの取得はせず、こちらを使います。
 * */
class NicoVideoListMenuBottomFragment : BottomSheetDialogFragment() {

    // データもらう
    lateinit var nicoVideoData: NicoVideoData
    lateinit var nicoVideoHTML: NicoVideoHTML
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // by lazy 使うか～（使うときまで lazy {} の中身は実行されない）
    val videoId by lazy { arguments?.getString("video_id")!! }
    val isCache by lazy { arguments?.getBoolean("is_cache") ?: false }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_list_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""
        nicoVideoHTML = NicoVideoHTML()

        // データ取得するかどうか。
        lifecycleScope.launch(Dispatchers.Main) {
            // NicoVideoDataある時
            val serializeData = arguments?.getSerializable("data")
            if (serializeData != null) {
                nicoVideoData = serializeData as NicoVideoData
            } else {
                // 無い時はインターネットから取得
                withContext(Dispatchers.IO) {
                    // データ取得
                    nicoVideoData = nicoVideoHTML.getNicoVideoData(videoId, userSession) ?: return@withContext
                }
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

            // ブラウザで開くボタン
            initOpenBrowser()
        }

    }

    private fun initOpenBrowser() {
        // キャッシュ一覧では表示させない
        if (isCache) {
            bottom_fragment_nicovideo_list_menu_browser.visibility = View.GONE
        }
        // ブラウザで開く。公式アニメが暗号化で見れん時に使って。
        bottom_fragment_nicovideo_list_menu_browser.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://nico.ms/${nicoVideoData.videoId}".toUri())
            startActivity(intent)
        }
    }

    private fun initCachePlaylistPlay() {
        if (isCache) {
            bottom_fragment_nicovideo_list_menu_playlist_background.visibility = View.VISIBLE
        }
        bottom_fragment_nicovideo_list_menu_playlist_background.setOnClickListener {
            // 第二引数で再生開始動画指定
            startBackgroundPlaylistPlayService(context, nicoVideoData.videoId)
            dismiss()
        }
    }

    // ポップアップ再生、バッググラウンド再生ボタン初期化
    private fun playServiceButton() {
        bottom_fragment_nicovideo_list_menu_popup.setOnClickListener {
            startVideoPlayService(context = context, mode = "popup", videoId = nicoVideoData.videoId, isCache = isCache)
        }
        bottom_fragment_nicovideo_list_menu_background.setOnClickListener {
            startVideoPlayService(context = context, mode = "background", videoId = nicoVideoData.videoId, isCache = isCache)
        }
        bottom_fragment_nicovideo_list_menu_play.setOnClickListener {
            // 通常再生
            val nicoVideoActivity = Intent(context, NicoVideoActivity::class.java).apply {
                putExtra("id", nicoVideoData.videoId)
                putExtra("cache", isCache)
            }
            startActivity(nicoVideoActivity)
        }
        // 強制エコノミーはキャッシュでは塞ぐ
        if (isCache) {
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
            bottom_fragment_nicovideo_list_menu_mylist.isVisible = true
            bottom_fragment_nicovideo_list_menu_atodemiru.isVisible = true
        } else {
            bottom_fragment_nicovideo_list_menu_mylist.isVisible = false
            bottom_fragment_nicovideo_list_menu_atodemiru.isVisible = false
        }
        // マイリスト画面の場合は消すに切り替える
        if (nicoVideoData.isMylist) {
            bottom_fragment_nicovideo_list_menu_mylist.text = getString(R.string.mylist_delete)
            bottom_fragment_nicovideo_list_menu_atodemiru.isVisible = false
        }
        // 非ログインモード時も消す
        if (isNotLoginMode(context)) {
            bottom_fragment_nicovideo_list_menu_mylist.isVisible = false
            bottom_fragment_nicovideo_list_menu_atodemiru.isVisible = false
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
                        lifecycleScope.launch(Dispatchers.Main) {
                            // マイリストFragment
                            val myListFragment = fragmentManager?.findFragmentById(R.id.fragment_video_list_linearlayout) as NicoVideoMyListFragment
                            // マイリスト削除API叩く。スマホ版のAPI
                            val nicoVideoSPMyListAPI = NicoVideoSPMyListAPI()
                            val deleteResponse = withContext(Dispatchers.IO) {
                                if (!nicoVideoData.isToriaezuMylist) {
                                    // マイリストから動画を削除
                                    nicoVideoSPMyListAPI.deleteMyListVideo(nicoVideoData.mylistId!!, nicoVideoData.mylistItemId, userSession)
                                } else {
                                    // とりあえずマイリストから動画を削除
                                    nicoVideoSPMyListAPI.deleteToriaezuMyListVideo(nicoVideoData.mylistItemId, userSession)
                                }
                            }
                            if (deleteResponse.isSuccessful) {
                                showToast(getString(R.string.mylist_delete_ok))
                                this@NicoVideoListMenuBottomFragment.dismiss()
                                // 再読み込み
                                // 位置特定
                                val currentPos = myListFragment.fragment_nicovideo_mylist_tablayout.selectedTabPosition
                                val fragment = myListFragment.adapter.fragmentList[currentPos]
                                (fragment as NicoVideoMyListListFragment).getMyListItems()
                            } else {
                                showToast("${getString(R.string.error)}\n${deleteResponse.code}")
                            }
                        }
                    }
                }.show(this@NicoVideoListMenuBottomFragment.parentFragmentManager, "delete")
            } else {
                // マイリスト一覧以外。追加ボタン
                val mylistBottomFragment = NicoVideoAddMylistBottomFragment()
                val bundle = Bundle().apply {
                    putString("id", nicoVideoData.videoId)
                }
                mylistBottomFragment.arguments = bundle
                mylistBottomFragment.show((activity as AppCompatActivity).supportFragmentManager, "mylist")
            }
        }
        // あとで見る（とりあえずマイリスト）に追加する
        bottom_fragment_nicovideo_list_menu_atodemiru.setOnClickListener {
            // あとで見るに追加する
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            lifecycleScope.launch(errorHandler) {
                withContext(Dispatchers.Main) {
                    // 消せないように
                    isCancelable = false
                }
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
                withContext(Dispatchers.Main) {
                    dismiss()
                }
            }
        }
    }

    // キャッシュ再取得とか削除とか（削除以外未実装）
    private fun cacheButton() {
        // キャッシュ関係
        val nicoVideoCache = NicoVideoCache(context)

        if (isCache) {
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
            startCacheService(false)
        }

        // キャッシュ取得（エコノミーモード）
        bottom_fragment_nicovideo_list_menu_get_cache_economy.setOnClickListener {
            // キャッシュ取得Service起動
            startCacheService(true)
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
                            fragmentManager?.findFragmentById(R.id.main_activity_linearlayout) as NicoVideoCacheFragment
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
                    this@NicoVideoListMenuBottomFragment.dismiss()
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
            this@NicoVideoListMenuBottomFragment.isCancelable = false
            showToast(getString(R.string.wait))
            lifecycleScope.launch {
                val status = xmlCommentJSON.xmlToJSON(nicoVideoData.videoId).await()
                showToast(getString(R.string.xml_to_json_complete))
                // 消す
                activity?.runOnUiThread {
                    this@NicoVideoListMenuBottomFragment.dismiss()
                }
            }
        }

    }

    /**
     * キャッシュ取得関数（？）
     * まあService起動させてるだけなんですけどね。
     * @param isEconomy エコノミーモードで取得する場合はtrue。
     * */
    private fun startCacheService(isEconomy: Boolean = false) {
        // キャッシュ取得Service起動
        val result = startCacheService(context, nicoVideoData.videoId, isEconomy, false)
        // 閉じる
        dismiss()
        // 取得済みならToast出す
        if (!result) {
            Toast.makeText(context, getString(R.string.cache_has_video_file), Toast.LENGTH_SHORT).show()
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