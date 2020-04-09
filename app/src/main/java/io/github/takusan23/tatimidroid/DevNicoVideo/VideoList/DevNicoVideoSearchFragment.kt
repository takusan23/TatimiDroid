package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSearchHTML
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_search.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * ニコ動検索Fragment
 * argumentにputString("search","検索したい内容")を入れるとその値を検索します。なおタグ検索、人気の高い順です。
 * */
class DevNicoVideoSearchFragment : Fragment() {

    // RecyclerView
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()

    // Preference
    lateinit var prefSetting: SharedPreferences
    var userSession = ""

    // 検索結果スクレイピング
    val nicoVideoSearchHTML =
        NicoVideoSearchHTML()

    // ページ数
    var page = 1

    // Coroutine
    lateinit var coroutine: Job

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

        fragment_nicovideo_search.setOnClickListener {
            page = 1
            search()
        }

        fragment_nicovideo_search_prev_page.setOnClickListener {
            // 前のページ
            if (page - 1 > 0) {
                page--
                search()
            }
        }
        fragment_nicovideo_search_next_page.setOnClickListener {
            // 次のページ
            page++
            search()
        }

        // 引っ張って更新
        fragment_nicovideo_search_swipe_refresh.setOnRefreshListener {
            search()
        }

        // Spinner選択でも検索できるように
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                search()
            }
        }
        fragment_nicovideo_search_tag_key_spinner.onItemSelectedListener = spinnerListener
        fragment_nicovideo_search_sort_spinner.onItemSelectedListener = spinnerListener
    }

    /**
     * 検索関数。
     * @param searchText 検索内容。省略すると「fragment_nicovideo_search_input」の値を使います。
     * */
    fun search(searchText: String = fragment_nicovideo_search_input.text.toString()) {
        if (searchText.isNotEmpty()) {
            // クリア
            fragment_nicovideo_search_swipe_refresh.isRefreshing = true
            recyclerViewList.clear()
            nicoVideoListAdapter.notifyDataSetChanged()
            if (::coroutine.isInitialized) {
                coroutine.cancel()
            }
            // ソート条件生成
            val sort =
                nicoVideoSearchHTML.makeSortOrder(fragment_nicovideo_search_sort_spinner.selectedItem as String)
            // タグかキーワードか
            val tagOrKeyword =
                if (fragment_nicovideo_search_tag_key_spinner.selectedItemPosition == 0) {
                    "tag"
                } else {
                    "search"
                }
            coroutine = GlobalScope.launch {
                val response = nicoVideoSearchHTML.getHTML(
                    userSession,
                    searchText,
                    tagOrKeyword,
                    sort.first,
                    sort.second,
                    page.toString()
                ).await()
                if (response.isSuccessful) {
                    nicoVideoSearchHTML.parseHTML(response.body?.string()).forEach {
                        recyclerViewList.add(it)
                    }
                    activity?.runOnUiThread {
                        nicoVideoListAdapter.notifyDataSetChanged()
                        fragment_nicovideo_search_swipe_refresh.isRefreshing = false
                        fragment_nicovideo_search_now_page.text =
                            "$page ${getString(R.string.page)}"
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_nicovideo_search_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    // Spinner初期化
    private fun initDropDownMenu() {
        // タグかキーワードか
        val spinnerList =
            arrayListOf("タグ", "キーワード")
        val spinnerAdapter =
            ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, spinnerList)
        fragment_nicovideo_search_tag_key_spinner.adapter = spinnerAdapter
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
        val sortAdapter =
            ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, sortList)
        fragment_nicovideo_search_sort_spinner.adapter = sortAdapter
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}