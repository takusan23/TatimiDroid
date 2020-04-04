package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.content.SharedPreferences
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
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoHTML
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_list_menu.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import kotlin.concurrent.timerTask

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

        // タイトル、ID設定
        bottom_fragment_nicovideo_list_menu_title.text = nicoVideoData.title
        bottom_fragment_nicovideo_list_menu_id.text = nicoVideoData.videoId

        // マイリスト登録ボタン
        mylistButton()

        // キャッシュ関係
        cacheButton()

    }

    // マイリスト。そのうち作る
    private fun mylistButton() {
        bottom_fragment_nicovideo_list_menu_mylist.setOnClickListener {
            val mylistBottomFragment = DevNicoVideoAddMylistBottomFragment()
            val bundle = Bundle().apply {
                putString("id", nicoVideoData.videoId)
            }
            mylistBottomFragment.arguments = bundle
            mylistBottomFragment.show((activity as AppCompatActivity).supportFragmentManager, "mylist")
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
        } else {
            // キャッシュ無いときは取得ボタンを置く
            bottom_fragment_nicovideo_list_menu_cache_menu.visibility = View.GONE
            bottom_fragment_nicovideo_list_menu_get_cache.visibility = View.VISIBLE
        }

        // キャッシュ取得
        bottom_fragment_nicovideo_list_menu_get_cache.setOnClickListener {
            // キャッシュ取得中はBottomFragmentを消させないようにする
            this.isCancelable = false
            bottom_fragment_nicovideo_list_menu_get_cache.text = getString(R.string.get_cache_video)
            // 定期実行。ハートビート処理
            val timer = Timer()
            // 取得
            GlobalScope.launch {
                val response = nicoVideoHTML.getHTML(nicoVideoData.videoId, userSession).await()
                if (response?.isSuccessful == true) {
                    val nicoHistory = nicoVideoHTML.getNicoHistory(response) ?: ""
                    val jsonObject = nicoVideoHTML.parseJSON(response?.body?.string())
                    if (!nicoVideoCache.isEncryption(jsonObject.toString())) {
                        // DMCサーバーならハートビート（視聴継続メッセージ送信）をしないといけないので
                        var contentUrl = ""
                        if (nicoVideoHTML.isDMCServer(jsonObject)) {
                            // https://api.dmc.nico/api/sessions のレスポンス
                            val sessionAPIJSONObject =
                                nicoVideoHTML.callSessionAPI(jsonObject).await()
                            if (sessionAPIJSONObject != null) {
                                // 動画URL
                                contentUrl =
                                    nicoVideoHTML.getContentURI(jsonObject, sessionAPIJSONObject)
                                val heartBeatURL =
                                    nicoVideoHTML.getHeartBeatURL(jsonObject, sessionAPIJSONObject)
                                val postData =
                                    nicoVideoHTML.getSessionAPIDataObject(jsonObject, sessionAPIJSONObject)
                                // ハートビート処理
                                timer.schedule(timerTask {
                                    nicoVideoHTML.postHeartBeat(heartBeatURL, postData) {}
                                }, 40 * 1000, 40 * 1000)
                            }
                        } else {
                            // Smileサーバー。動画URL取得
                            contentUrl = nicoVideoHTML.getContentURI(jsonObject, null)
                        }
                        // キャッシュ取得
                        nicoVideoCache.getCache(nicoVideoData.videoId, jsonObject.toString(), contentUrl, userSession, nicoHistory)
                        // キャッシュ取得成功ブロードキャストを受け取る
                        nicoVideoCache.initBroadcastReceiver {
                            // 取得完了したら呼ばれる。
                            timer.cancel()
                            // 取得できたら消せるようにする
                            this@DevNicoVideoListMenuBottomFragment.dismiss()
                        }
                    } else {
                        showToast(context?.getString(R.string.encryption_not_download) ?: "暗号化むり")
                        activity?.runOnUiThread {
                            // 取得できたら消せるようにする
                            this@DevNicoVideoListMenuBottomFragment.dismiss()
                        }
                    }
                }
            }
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

        // キャッシュの動画情報、コメント更新（未実装）
        bottom_fragment_nicovideo_list_menu_re_get_cache.setOnClickListener {
            // キャッシュ取得中はBottomFragmentを消させないようにする
            this.isCancelable = false
            bottom_fragment_nicovideo_list_menu_re_get_cache.text =
                getString(R.string.cache_updateing)
            GlobalScope.launch {
                // 動画HTML取得
                val response = nicoVideoHTML.getHTML(nicoVideoData.videoId, userSession).await()
                if (response?.isSuccessful == true) {
                    // 動画情報更新
                    val jsonObject = nicoVideoHTML.parseJSON(response.body?.string())
                    val videoIdFolder =
                        File("${nicoVideoCache.getCacheFolderPath()}/${nicoVideoData.videoId}")
                    nicoVideoCache.saveVideoInfo(videoIdFolder, nicoVideoData.videoId, jsonObject.toString())

                    // コメント取得
                    val commentResponse =
                        nicoVideoHTML.getComment(nicoVideoData.videoId, userSession, jsonObject)
                            .await()
                    val commentString = commentResponse?.body?.string()
                    if (commentResponse?.isSuccessful == true && commentString != null) {
                        // コメント更新
                        nicoVideoCache.getCacheComment(videoIdFolder, nicoVideoData.videoId, jsonObject.toString(), userSession)
                        showToast(getString(R.string.cache_update_ok))
                        // 取得できたら閉じる
                        Handler(Looper.getMainLooper()).post {
                            this@DevNicoVideoListMenuBottomFragment.dismiss()
                        }
                    } else {
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response?.code}")
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