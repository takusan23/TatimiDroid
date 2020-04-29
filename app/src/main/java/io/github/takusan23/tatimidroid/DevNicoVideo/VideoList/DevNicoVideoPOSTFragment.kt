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
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoPOST
import io.github.takusan23.tatimidroid.NicoAPI.User
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.isNotLoginMode
import kotlinx.android.synthetic.main.fragment_nicovideo_post.*
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

    // 追加読み込み制御
    var isLoading = false

    // もう取れないときはtrue
    var isMaxCount = false

    // RecyclerView位置
    var position = 0
    var yPos = 0

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

        fragment_nicovideo_post_swipe_to_refresh.setOnRefreshListener {
            getPostList(isNowPageNum)
        }
    }

    /**
     * 投稿動画取得。
     * @param page ページ数。
     * */
    private fun getPostList(page: Int) {
        // fragment_nicovideo_post_recyclerview.layoutManager?.scrollToPosition(0)
        fragment_nicovideo_post_swipe_to_refresh.isRefreshing = true
        // recyclerViewList.clear()
        // 非ログインならユーザーセッションを空で上書き
        val userSession = if (isNotLoginMode(context)) {
            ""
        } else {
            this@DevNicoVideoPOSTFragment.userSession
        }
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
            val userId = arguments?.getString("userId") ?: ""
            val user =
                User().getUserCoroutine(userId, userSession, url).await()
            val response = post.getList(page, user?.userId.toString(), userSession).await()
            if (response.isSuccessful) {
                val postVideoList = post.parseHTML(response.body?.string())
                // 最後判定
                isMaxCount = postVideoList.size == 0
                postVideoList.forEach {
                    recyclerViewList.add(it)
                }
                activity?.runOnUiThread {
                    nicoVideoListAdapter.notifyDataSetChanged()
                    // スクロール
                    fragment_nicovideo_post_recyclerview.apply {
                        (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, yPos)
                    }
                    if (isAdded) {
                        fragment_nicovideo_post_swipe_to_refresh.isRefreshing = false
                        isLoading = false
                    }
                    // これで最後です。；；
                    if (isMaxCount) {
                        showToast(getString(R.string.end_scroll))
                    }
                }
            } else {
                showToast("${getString(R.string.error)}\n${response.code}")
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
            val linearLayoutManager = LinearLayoutManager(context)
            layoutManager = linearLayoutManager
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val visibleItemCount = recyclerView.childCount
                    val totalItemCount = linearLayoutManager.itemCount
                    val firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition()
                    //最後までスクロールしたときの処理
                    if (firstVisibleItem + visibleItemCount == totalItemCount && !isLoading && !isMaxCount) {
                        isLoading = true
                        isNowPageNum++
                        getPostList(isNowPageNum)
                        position =
                            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        yPos = getChildAt(0).top
                    }
                }
            })
        }
    }

}