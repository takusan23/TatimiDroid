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
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

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
                mylistAPI.parseMyListJSON(response.body?.string()).forEach {
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