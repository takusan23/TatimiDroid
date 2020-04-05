package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoPOST
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoRSS
import io.github.takusan23.tatimidroid.NicoAPI.User
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_post.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 投稿動画取得
 * argumentsにputBoolean("my",true)で入れると自分の投稿動画を取りに行きます。（非公開は無理）
 * argumentsにputString("userId","ユーザーID")で入れると入れたユーザーIDの投稿動画を取りに行きます。
 * */
class DevNicoVideoPOSTFragment : Fragment() {

    val post = NicoVideoPOST()

    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter

    // 今のページ
    var isNowPageNum = 1

    // キャンセルできるように
    lateinit var coroutine: Job

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // RecyclerView初期化
        initRecyclerView()

        // 1ページ目取得
        getPostList(isNowPageNum)

        fragment_nicovideo_post_prev_page.setOnClickListener {
            isNowPageNum--
            getPostList(isNowPageNum)
        }
        fragment_nicovideo_post_next_page.setOnClickListener {
            isNowPageNum++
            getPostList(isNowPageNum)
        }

        fragment_nicovideo_post_swipe_to_refresh.setOnRefreshListener {
            getPostList(isNowPageNum)
        }

    }

    /**
     * 投稿動画取得。
     * @param page ページ数。
     * */
    private fun getPostList(page: Int) {
        fragment_nicovideo_post_swipe_to_refresh.isRefreshing = true
        // 通信してるならキャンセル
        if (::coroutine.isInitialized) {
            coroutine.cancel()
        }
        coroutine = GlobalScope.launch {
            val url = if (arguments?.getBoolean("my", false) == true) {
                "https://nvapi.nicovideo.jp/v1/users/me"
            } else {
                "https://nvapi.nicovideo.jp/v1/users/${arguments?.getString("userId")}"
            }
            // ユーザーID取得
            val user =
                User(context, "").getUserCoroutine(url).await()
            if (userSession.isNotEmpty() && user?.userId != null) {
                recyclerViewList.clear()
                val response = post.getList(page, user.userId.toString(), userSession).await()
                if (response.isSuccessful) {
                    post.parseHTML(response).forEach {
                        recyclerViewList.add(it)
                    }
                    activity?.runOnUiThread {
                        nicoVideoListAdapter.notifyDataSetChanged()
                        fragment_nicovideo_post_swipe_to_refresh.isRefreshing = false
                        fragment_nicovideo_post_now_page.text = "$page ${getString(R.string.page)}"
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                    activity?.runOnUiThread {
                        fragment_nicovideo_post_now_page.text = "$page ${getString(R.string.page)}"
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_nicovideo_post_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

}