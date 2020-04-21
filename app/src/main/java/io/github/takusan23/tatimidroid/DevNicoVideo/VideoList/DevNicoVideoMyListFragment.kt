package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * マイリスト表示Fragment
 * 他の人のマイリストを表示する場合は以下の情報をargumentに詰めてね。
 * mylist_id    |String |マイリストのID。例：mylist/50133549
 * */
class DevNicoVideoMyListFragment : Fragment() {

    val mylistAPI = NicoVideoMyListAPI()

    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()

    lateinit var prefSetting: SharedPreferences
    var userSession = ""
    var token = ""

    // 今開いているMylistのID。とりあえずマイリストは空文字。
    var myListId = ""

    lateinit var coroutine: Job

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_mylist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        // RecyclerView初期化
        initRecyclerView()

        // 並び替えSpinner初期化
        initSortSpinner()

        // マイリストIDがある場合は
        myListId = arguments?.getString("mylist_id", "") ?: ""
        // マイリストが読み取り専用（他の人のマイリストを表示する際はtrue）
        val isMylistReadOnly = myListId.isNotEmpty()
        if (!isMylistReadOnly) {
            // 自分のマイリスト取得
            // Token取得
            fragment_nicovideo_mylist_swipe_to_refresh.isRefreshing = true
            GlobalScope.launch {
                val response = mylistAPI.getMyListHTML(userSession).await()
                val responseString = response.body?.string()
                if (response.isSuccessful && mylistAPI.getToken(responseString) != null) {
                    // Token取得
                    token = mylistAPI.getToken(responseString)!!
                    // マイリスト一覧取得
                    getMyListList()
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        } else {
            // 他の人のマイリスト取得
            fragment_nicovideo_mylist_tablayout.visibility = View.GONE
            getOtherUserMylistItems(myListId.replace("mylist/", ""))
            fragment_nicovideo_mylist_swipe_to_refresh.isEnabled = false
        }


    }

    private fun initSortSpinner() {
        val sortList =
            arrayListOf(
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
        val adapter =
            ArrayAdapter(context!!, android.R.layout.simple_spinner_dropdown_item, sortList)
        fragment_nicovideo_mylist_sort_spinner.adapter = adapter
        fragment_nicovideo_mylist_sort_spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {

                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    sort(position)
                }
            }
    }

    // ソートする
    fun sort(position: Int) {
        // 選択
        when (position) {
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

    // マイリスト一覧取得
    fun getMyListList() {
        GlobalScope.launch {
            val response = mylistAPI.getMyListList(token, userSession).await()
            if (response.isSuccessful) {
                val jsonObject = JSONObject(response.body?.string())
                activity?.runOnUiThread {
                    // JSONパースしてTabLayoutに追加
                    val jsonArray = jsonObject.getJSONArray("mylistgroup")
                    for (i in 0 until jsonArray.length()) {
                        val mylistObj = jsonArray.getJSONObject(i)
                        val name = mylistObj.getString("name")
                        val id = mylistObj.getString("id")
                        val tabItem = fragment_nicovideo_mylist_tablayout.newTab()
                        tabItem.apply {
                            text = name
                            tag = id
                        }
                        fragment_nicovideo_mylist_tablayout.addTab(tabItem)
                        // TabLayout初期化
                        initTabLayout()
                        // 引っ張って更新できるようにする
                        initSwipeToRefresh()
                        // とりあえずマイリスト取得
                        getMylistItems("")
                    }
                }
            } else {
                showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    private fun initTabLayout() {
        fragment_nicovideo_mylist_tablayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                fragment_nicovideo_mylist_swipe_to_refresh.isRefreshing = true
                recyclerViewList.clear()
                myListId = if (tab?.text == getString(R.string.toriaezu_mylist)) {
                    // とりあえずマイリスト
                    ""
                } else {
                    // それ以外
                    tab?.tag as String
                }
                // マイリストの中身取得
                getMylistItems(myListId)
            }
        })
    }

    // SwipeToRefresh初期化
    private fun initSwipeToRefresh() {
        fragment_nicovideo_mylist_swipe_to_refresh.setOnRefreshListener {
            // 選択中のタブ
            val tab =
                fragment_nicovideo_mylist_tablayout.getTabAt(fragment_nicovideo_mylist_tablayout.selectedTabPosition)
            val myListId = if (tab?.text == getString(R.string.toriaezu_mylist)) {
                // とりあえずマイリスト
                ""
            } else {
                // それ以外
                tab?.tag as String
            }
            getMylistItems(myListId)
        }
    }

    // マイリストの中身取得
    fun getMylistItems(myListId: String) {
        // 初期化
        recyclerViewList.clear()
        nicoVideoListAdapter.notifyDataSetChanged()
        // 通信中ならキャンセル
        if (::coroutine.isInitialized) {
            coroutine.cancel()
        }
        coroutine = GlobalScope.launch {
            val response = mylistAPI.getMyListItems(token, myListId, userSession).await()
            if (response.isSuccessful) {
                mylistAPI.parseMyListJSON(response.body?.string())
                    // 並び替え
                    .sortedByDescending { nicoVideoData -> nicoVideoData.mylistAddedDate }.forEach {
                        recyclerViewList.add(it)
                    }
                activity?.runOnUiThread {
                    nicoVideoListAdapter.notifyDataSetChanged()
                    fragment_nicovideo_mylist_swipe_to_refresh.isRefreshing = false
                    // ソートさせる（ソート順を選んでる場合）
                    val position = fragment_nicovideo_mylist_sort_spinner.selectedItemPosition
                    sort(position)
                }
            } else {
                showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    // 他の人のマイリストを取得する
    fun getOtherUserMylistItems(myListId: String) {
        // 初期化
        recyclerViewList.clear()
        nicoVideoListAdapter.notifyDataSetChanged()
        // 通信中ならキャンセル
        if (::coroutine.isInitialized) {
            coroutine.cancel()
        }
        coroutine = GlobalScope.launch {
            val response = mylistAPI.getOtherUserMylistItems(myListId, userSession).await()
            if (response.isSuccessful) {
                mylistAPI.parseOtherUserMyListJSON(response.body?.string())
                    .sortedByDescending { nicoVideoData -> nicoVideoData.mylistAddedDate }.forEach {
                        recyclerViewList.add(it)
                    }
                activity?.runOnUiThread {
                    nicoVideoListAdapter.notifyDataSetChanged()
                    fragment_nicovideo_mylist_swipe_to_refresh.isRefreshing = false
                }
            } else {
                showToast("${getString(R.string.error)}\n${response.code}")
            }
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_nicovideo_mylist_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}