package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoPOST
import io.github.takusan23.tatimidroid.NicoAPI.User.User
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.isNotLoginMode
import kotlinx.android.synthetic.main.fragment_nicovideo_post.*
import kotlinx.coroutines.*

/**
 * 投稿動画取得
 * my           |Boolean| trueのときは自分の投稿動画を取得しに行きます。（非公開は無理）
 * userId       |String | ユーザーIDを入れるとそのユーザーの投稿した動画を取得しに行きます。
 * */
class NicoVideoPOSTFragment : Fragment() {

    val post = NicoVideoPOST()

    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    // 今のページ
    var page = 1

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

        when {
            recyclerViewList.isNotEmpty() -> {
                // 動画再生アクティビティのときはViewPagerくるくるしても多分値残ってる
                nicoVideoListAdapter.notifyDataSetChanged()
            }
            savedInstanceState != null -> {
                // 画面回転復帰時
                (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).toList().forEach {
                    recyclerViewList.add(it)
                }
                recyclerViewList.distinct()
                page = savedInstanceState.getInt("page")
                nicoVideoListAdapter.notifyDataSetChanged()
            }
            else -> {
                // データ取得が必要
                getPostList(page)
            }
        }

        fragment_nicovideo_post_swipe_to_refresh.setOnRefreshListener {
            page = 1
            position = 0
            yPos = 0
            recyclerViewList.clear()
            getPostList(page)
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
            this@NicoVideoPOSTFragment.userSession
        }
        // 通信してるならキャンセル
        if (::coroutine.isInitialized) {
            coroutine.cancel()
        }
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        coroutine = lifecycleScope.launch(errorHandler) {
            // 自分の場合はまずユーザーIDを取る
            val userId = if (arguments?.getBoolean("my", false) == true) {
                // 自分の場合はAPI叩いてユーザーID取る
                val user = User()
                val response = user.getUserCoroutine("", userSession, "https://nvapi.nicovideo.jp/v1/users/me")
                if (!response.isSuccessful) {
                    null
                } else {
                    user.parseUserData(response.body?.string()).userId.toString()
                }
            } else {
                // ユーザーID指定
                arguments?.getString("userId")
            } ?: return@launch // 取れないなら終了
            // 投稿動画取得する
            val response = post.getPOSTVideo(userId, userSession, page)
            if (response.isSuccessful) {
                // 成功時。JSONパース
                val postVideoList = withContext(Dispatchers.Default) {
                    post.parsePOSTVideo(response.body?.string())
                }
                // 最後判定
                isMaxCount = postVideoList.size == 0
                postVideoList.forEach {
                    recyclerViewList.add(it)
                }
                nicoVideoListAdapter.notifyDataSetChanged()
                // スクロール
                fragment_nicovideo_post_recyclerview.apply {
                    (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, yPos)
                }
                fragment_nicovideo_post_swipe_to_refresh.isRefreshing = false
                isLoading = false
                // これで最後です。；；は配列の中身が一個以上あればな話。
                if (isMaxCount && recyclerViewList.isNotEmpty()) {
                    showToast(getString(R.string.end_scroll))
                }
                // そもそも取得できなかった場合
                if (recyclerViewList.isEmpty()) {
                    fragment_nicovideo_post_private_message.visibility = View.VISIBLE
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
            // Adapterセット
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
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
                        page++
                        getPostList(page)
                        position = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        yPos = getChildAt(0).top
                    }
                }
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
        outState.putInt("page", page)
    }

}