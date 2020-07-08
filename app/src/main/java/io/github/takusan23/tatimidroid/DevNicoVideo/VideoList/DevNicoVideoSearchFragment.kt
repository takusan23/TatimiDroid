package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.AllShowDropDownMenuAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSearchHTML
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_search.*
import kotlinx.coroutines.*
import java.lang.IndexOutOfBoundsException

/**
 * ニコ動検索Fragment
 * argumentにputString("search","検索したい内容")を入れるとその値を検索します。なおタグ検索、人気の高い順です。
 *
 * search       | String | 検索したい内容
 * search_hide  | Boolean| 検索領域を非表示にする場合はtrue
 * sort_show    | Boolean| 並び替えを初めから表示する場合はtrue。なおタグ/キーワードの変更は出ない
 * */
class DevNicoVideoSearchFragment : Fragment() {

    // RecyclerView
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()

    // Preference
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // 検索結果スクレイピング
    val nicoVideoSearchHTML = NicoVideoSearchHTML()

    // ページ数
    var page = 1

    // Coroutine
    lateinit var coroutine: Job

    // 追加読み込み制御
    var isLoading = false

    // もう取れないときはtrue
    var isMaxCount = false

    // RecyclerView位置
    var position = 0
    var yPos = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ドロップダウンメニュー初期化
        initDropDownMenu()

        // RecyclerView初期化
        initRecyclerView()

        // argumentの値を使って検索。
        val searchText = arguments?.getString("search")
        if (searchText != null && searchText.isNotEmpty()) {
            fragment_nicovideo_search_input.setText(searchText)
            search(searchText)
        }

        // 非表示オプション（再生中にタグ検索する時に使う）
        val isSearchHide = arguments?.getBoolean("search_hide") ?: false
        if (isSearchHide) {
            (fragment_nicovideo_search_input.parent as View).visibility = View.GONE
            (fragment_nicovideo_search_tag_key_menu.parent as View).visibility = View.GONE
        }

        // 検索ボタン
        fragment_nicovideo_search.setOnClickListener {
            page = 1
            search()
        }

        // 引っ張って更新
        fragment_nicovideo_search_swipe_refresh.setOnRefreshListener {
            page = 1
            search()
        }

        // エンターキー押したら検索実行
        fragment_nicovideo_search_input.setOnEditorActionListener { v, actionId, event ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    page = 1
                    search()
                    true
                }
                else -> false
            }
        }

        // タグ、並び替え常に出す必要なくない？というわけで非表示にできるようにする
        fragment_nicovideo_search_option.setOnClickListener {
            fragment_nicovideo_search_sort_parent_linarlayout.apply {
                if (visibility == View.VISIBLE) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                }
            }
        }

        // たぐ、並び替えメニュー押しても検索できるように
        fragment_nicovideo_search_sort_menu.addTextChangedListener {
            page = 1 // RecyclerView空にするので
            search()
        }
        fragment_nicovideo_search_tag_key_menu.addTextChangedListener {
            page = 1 // RecyclerView空にするので
            search()
        }

        // 動画再生中に検索した時に、ソートが消えるので表示
        if (arguments?.getBoolean("sort_show") == true) {
            fragment_nicovideo_search_sort_parent_linarlayout.visibility = View.VISIBLE
            (fragment_nicovideo_search_tag_key_menu.parent as View).visibility = View.GONE
        }

    }

    /**
     * 検索関数。
     * 注意：pageに1が入っているときはRecyclerViewを空にします。それ以外は空にしません
     * @param searchText 検索内容。省略すると「fragment_nicovideo_search_input」の値を使います。
     * */
    fun search(searchText: String = fragment_nicovideo_search_input.text.toString()) {
        if (searchText.isNotEmpty()) {
            // すでにあればキャンセル？
            if (::coroutine.isInitialized) {
                coroutine.cancel()
            }
            // 例外処理。コルーチン内で例外出るとここに来るようになるらしい。あたまいい
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}${throwable.message}")
            }
            coroutine = GlobalScope.launch(errorHandler) {
                val response = withContext(Dispatchers.Main) {
                    // 1ならクリアとRecyclerViewの位置クリア
                    if (page == 1) {
                        recyclerViewList.clear()
                        position = 0
                        yPos = 0
                        isMaxCount = false
                    }
                    fragment_nicovideo_search_swipe_refresh.isRefreshing = true
                    // ソート条件生成
                    val sort = nicoVideoSearchHTML.makeSortOrder(fragment_nicovideo_search_sort_menu.text.toString())
                    // タグかキーワードか
                    val tagOrKeyword = if (fragment_nicovideo_search_tag_key_menu.text.toString() == "タグ") {
                        "tag"
                    } else {
                        "search"
                    }
                    // 検索結果html取りに行く
                    nicoVideoSearchHTML.getHTML(
                        userSession,
                        searchText,
                        tagOrKeyword,
                        sort.first,
                        sort.second,
                        page.toString()
                    )
                }
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${response.code}")
                    // もう読み込まない
                    isMaxCount = true
                    return@launch
                }
                nicoVideoSearchHTML.parseHTML(response.body?.string()).forEach {
                    recyclerViewList.add(it)
                }
                // 追加。UIスレッドへ
                if (isAdded) {
                    withContext(Dispatchers.Main) {
                        // リスト更新
                        nicoVideoListAdapter.notifyDataSetChanged()
                        // スクロール位置復元
                        fragment_nicovideo_search_recyclerview.apply {
                            (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, yPos)
                        }
                        fragment_nicovideo_search_swipe_refresh.isRefreshing = false
                        // また読み込めるように
                        isLoading = false
                    }
                }
            }
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_nicovideo_search_recyclerview.apply {
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
                        page++
                        search()
                        position = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        yPos = getChildAt(0).top
                    }
                }
            })
        }
    }

    // Spinner初期化
    private fun initDropDownMenu() {
        // タグかキーワードか
        val spinnerList = arrayListOf("タグ", "キーワード")
        val tagOrKeywordAdapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, spinnerList)
        fragment_nicovideo_search_tag_key_menu.apply {
            setAdapter(tagOrKeywordAdapter)
            setText(spinnerList[0], false)
        }
        // 並び替え
        val sortList = arrayListOf(
            "人気が高い順",
            "あなたへのおすすめ順",
            "投稿日時が新しい順",
            "再生数が多い順",
            "マイリスト数が多い順",
            "コメントが新しい順",
            "コメントが古い順",
            "再生数が少ない順",
            "コメント数が多い順",
            "コメント数が少ない順",
            "マイリスト数が少ない順",
            "投稿日時が古い順",
            "再生時間が長い順",
            "再生時間が短い順"
        )
        val sortAdapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortList)
        fragment_nicovideo_search_sort_menu.apply {
            setAdapter(sortAdapter)
            setText(sortList[0], false)
        }
    }

    private fun showToast(message: String) {
        if (isAdded) {
            activity?.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

/*
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
        outState.putInt("page", page)
    }
*/


}