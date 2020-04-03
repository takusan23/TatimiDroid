package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoSearchHTML
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_search.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DevNicoVideoSearchFragment : Fragment() {

    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    lateinit var prefSetting: SharedPreferences
    var userSession = ""
    val recyclerViewList = arrayListOf<NicoVideoData>()
    val nicoVideoSearchHTML = NicoVideoSearchHTML()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ドロップダウンメニュー初期化
        initDropDownMenu()

        // RecyclerView初期化
        initRecyclerView()

        fragment_nicovideo_search.setOnClickListener {
            GlobalScope.launch {
                recyclerViewList.clear()
                val response = nicoVideoSearchHTML.getHTML(
                    userSession,
                    fragment_nicovideo_search_input.text.toString(),
                    "tag",
                    "h",
                    "d",
                    "1"
                ).await()
                if (response.isSuccessful) {
                    nicoVideoSearchHTML.parseHTML(response.body?.string()).forEach {
                        recyclerViewList.add(it)
                    }
                    activity?.runOnUiThread {
                        nicoVideoListAdapter.notifyDataSetChanged()
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