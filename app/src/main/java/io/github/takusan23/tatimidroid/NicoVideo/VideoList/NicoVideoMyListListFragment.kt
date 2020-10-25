package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.AllShowDropDownMenuAdapter
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist_list.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * マイリスト一覧表示Fragment。
 * ViewPagerで表示するFragmentです。
 * 入れてほしいもの↓
 * mylist_id   |String |マイリストのID。空の場合はとりあえずマイリストをリクエストします
 * is_other    |Boolean|他の人のマイリストを読み込む時に使う。
 * */
class NicoVideoMyListListFragment : Fragment() {

    lateinit var nicoVideoListAdapter: NicoVideoListAdapter
    lateinit var prefSetting: SharedPreferences
    private val recyclerViewList = arrayListOf<NicoVideoData>()

    private var userSession = ""
    private var myListId = ""
    private var isOther = false
    private var sortMenuPos = 0
    private var myListName = ""

    private val nicoVideoSPMyListAPI = NicoVideoSPMyListAPI()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_mylist_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // ダークモード
        fragment_nicovideo_mylist_list_app_bar.background = ColorDrawable(getThemeColor(requireContext()))

        // RecyclerView初期化
        initRecyclerView()

        // 並び替えメニュー初期化
        initSortMenu()

        myListId = arguments?.getString("mylist_id") ?: return
        isOther = arguments?.getBoolean("is_other") ?: false

        // データ取得
        getMyListItems()

        fragment_nicovideo_mylist_list_swipe.setOnRefreshListener {
            getMyListItems()
        }

    }

    // マイリストの中身取得
    fun getMyListItems() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        lifecycleScope.launch(errorHandler) {
            // 初期化
            recyclerViewList.clear()
            nicoVideoListAdapter.notifyDataSetChanged()
            fragment_nicovideo_mylist_list_swipe.isRefreshing = true
            // データ取得
            // とりあえずマイリストかマイリストか？
            val myListItemsReponse = when {
                isOther -> nicoVideoSPMyListAPI.getOtherUserMyListItems(myListId, userSession)
                myListId.isEmpty() -> nicoVideoSPMyListAPI.getToriaezuMyListList(userSession)
                else -> nicoVideoSPMyListAPI.getMyListItems(myListId, userSession)
            }
            if (!myListItemsReponse.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${myListItemsReponse.code}")
                return@launch
            }
            // レスポンス
            val responseString = withContext(Dispatchers.Default) {
                myListItemsReponse.body?.string()
            }
            // パース
            val videoItems = withContext(Dispatchers.Default) {
                if (isOther) {
                    nicoVideoSPMyListAPI.parseOtherUserMyListJSON(responseString)
                } else {
                    nicoVideoSPMyListAPI.parseMyListItems(myListId, responseString)
                }.sortedByDescending { nicoVideoData -> nicoVideoData.mylistAddedDate } // ソート
            }
            // マイリス名
            myListName = if (isOther) {
                nicoVideoSPMyListAPI.parseMyListName(responseString)
            } else {
                arguments?.getString("mylist_name") ?: ""
            }
            // RecyclerViewへ追加
            videoItems.forEach {
                recyclerViewList.add(it)
            }
            nicoVideoListAdapter.notifyDataSetChanged()
            fragment_nicovideo_mylist_list_swipe.isRefreshing = false
            sort()
            // 連続再生ボタン
        }
    }

    private fun initSortMenu() {
        val sortList = arrayListOf(
            "登録が新しい順",
            "登録が古い順",
            "再生の多い順",
            "再生の少ない順",
            "投稿日時が新しい順",
            "投稿日時が古い順",
            "再生時間の長い順",
            "再生時間の短い順",
            "コメントの多い順",
            "コメントの少ない順",
            "マイリスト数の多い順",
            "マイリスト数の少ない順"
        )
        val adapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortList)
        fragment_nicovideo_mylist_list_sort.apply {
            setAdapter(adapter)
            setOnItemClickListener { parent, view, position, id ->
                sortMenuPos = position
                sort()
            }
            setText(sortList[0], false)
        }
    }


    // ソートする
    private fun sort() {
        // 選択
        when (sortMenuPos) {
            0 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.mylistAddedDate }
            1 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.mylistAddedDate }
            2 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            3 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.viewCount.toInt() }
            4 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.date }
            5 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.date }
            6 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.duration }
            7 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.duration }
            8 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            9 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.commentCount.toInt() }
            10 -> recyclerViewList.sortByDescending { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
            11 -> recyclerViewList.sortBy { nicoVideoData -> nicoVideoData.mylistCount.toInt() }
        }
        nicoVideoListAdapter.notifyDataSetChanged()
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    private fun initRecyclerView() {
        fragment_nicovideo_mylist_list_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            // Adapterセット
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

}