package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSearchHTML
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.AllShowDropDownMenuAdapter
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentNicovideoSearchBinding
import kotlinx.coroutines.*

/**
 * ニコ動検索Fragment
 * argumentにputString("search","検索したい内容")を入れるとその値を検索します。なおタグ検索、人気の高い順です。
 *
 * search       | String | 検索したい内容
 * search_hide  | Boolean| 検索領域を非表示にする場合はtrue
 * sort_show    | Boolean| 並び替えを初めから表示する場合はtrue。なおタグ/キーワードの変更は出ない
 * */
class NicoVideoSearchFragment : Fragment() {

    // RecyclerView
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()

    // Preference
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // 検索結果スクレイピング
    private val nicoVideoSearchHTML = NicoVideoSearchHTML()

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

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicovideoSearchBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモード
        viewBinding.fragmentNicovideoSearchAppBar.background = ColorDrawable(getThemeColor(requireContext()))

        // ドロップダウンメニュー初期化
        initDropDownMenu()

        // RecyclerView初期化
        initRecyclerView()

        // argumentの値を使って検索。
        val searchText = arguments?.getString("search")
        if (searchText != null && searchText.isNotEmpty()) {
            viewBinding.fragmentNicovideoSearchInput.setText(searchText)
            search(searchText)
        }

        // 非表示オプション（再生中にタグ検索する時に使う）
        val isSearchHide = arguments?.getBoolean("search_hide") ?: false
        if (isSearchHide) {
            (viewBinding.fragmentNicovideoSearchInput.parent as View).visibility = View.GONE
            (viewBinding.fragmentNicovideoSearchTagKeyMenu.parent as View).visibility = View.GONE
        }

        // 検索ボタン
        viewBinding.fragmentNicovideoSearchImageView.setOnClickListener {
            page = 1
            search()
        }

        // 引っ張って更新
        viewBinding.fragmentNicovideoSearchSwipeRefresh.setOnRefreshListener {
            page = 1
            search()
        }

        // エンターキー押したら検索実行
        viewBinding.fragmentNicovideoSearchInput.setOnEditorActionListener { v, actionId, event ->
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
        viewBinding.fragmentNicovideoSearchOption.setOnClickListener {
            viewBinding.fragmentNicovideoSearchSortParentLinarLayout.isVisible = !viewBinding.fragmentNicovideoSearchSortParentLinarLayout.isVisible
        }

        // たぐ、並び替えメニュー押しても検索できるように
        viewBinding.fragmentNicovideoSearchSortMenu.addTextChangedListener {
            page = 1 // RecyclerView空にするので
            search()
        }
        viewBinding.fragmentNicovideoSearchTagKeyMenu.addTextChangedListener {
            page = 1 // RecyclerView空にするので
            search()
        }

        // 動画再生中に検索した時に、ソートが消えるので表示
        if (arguments?.getBoolean("sort_show") == true) {
            (viewBinding.fragmentNicovideoSearchSortParentLinarLayout as View).visibility = View.GONE
            (viewBinding.fragmentNicovideoSearchTagKeyMenu.parent as View).visibility = View.GONE
        }

    }

    /**
     * 検索関数。
     * 注意：pageに1が入っているときはRecyclerViewを空にします。それ以外は空にしません
     * @param searchText 検索内容。省略すると「fragment_nicovideo_search_input」の値を使います。
     * */
    fun search(searchText: String = viewBinding.fragmentNicovideoSearchInput.text.toString()) {
        if (searchText.isNotEmpty()) {
            // すでにあればキャンセル？
            if (::coroutine.isInitialized) {
                coroutine.cancel()
            }
            // 例外処理。コルーチン内で例外出るとここに来るようになるらしい。あたまいい
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}${throwable}")
                viewBinding.fragmentNicovideoSearchSwipeRefresh.isRefreshing = false
            }
            coroutine = lifecycleScope.launch(errorHandler) {
                val response = withContext(Dispatchers.Main) {
                    // 1ならクリアとRecyclerViewの位置クリア
                    if (page == 1) {
                        recyclerViewList.clear()
                        position = 0
                        yPos = 0
                        isMaxCount = false
                    }
                    viewBinding.fragmentNicovideoSearchSwipeRefresh.isRefreshing = true
                    // ソート条件生成
                    val sort = nicoVideoSearchHTML.makeSortOrder(viewBinding.fragmentNicovideoSearchSortMenu.text.toString())
                    // タグかキーワードか
                    val tagOrKeyword = if (viewBinding.fragmentNicovideoSearchTagKeyMenu.text.toString() == "タグ") {
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
                withContext(Dispatchers.Default) {
                    nicoVideoSearchHTML.parseHTML(response.body?.string()).forEach {
                        recyclerViewList.add(it)
                    }
                }
                // 追加。
                // リスト更新
                nicoVideoListAdapter.notifyDataSetChanged()
                // スクロール位置復元
                viewBinding.fragmentNicovideoSearchRecyclerView.apply {
                    (layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, yPos)
                }
                viewBinding.fragmentNicovideoSearchSwipeRefresh.isRefreshing = false
                // また読み込めるように
                isLoading = false
            }
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        viewBinding.fragmentNicovideoSearchRecyclerView.apply {
            setHasFixedSize(true)
            val linearLayoutManager = LinearLayoutManager(context)
            layoutManager = linearLayoutManager
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
        viewBinding.fragmentNicovideoSearchTagKeyMenu.apply {
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
        viewBinding.fragmentNicovideoSearchSortMenu.apply {
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