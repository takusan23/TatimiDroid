package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoMyListViewPagerAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist.*
import kotlinx.coroutines.*

/**
 * マイリストFragment。RecyclerViewが乗ってるFragmentはDevNicoVideoMyListListFragmentです。
 * このFragmentはTabLayout+ViewPager2が乗ってるだけ。
 * */
class DevNicoVideoMyListFragment : Fragment() {

    lateinit var adapter: DevNicoVideoMyListViewPagerAdapter
    private lateinit var prefSetting: SharedPreferences
    private var userSession = ""

    // マイリスト一覧の配列
    private var myListItems = arrayListOf<NicoVideoSPMyListAPI.MyListData>()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_mylist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        if (savedInstanceState == null) {
            // マイリスト一覧取得
            getMyListItems()
        } else {
            // 画面回転復帰時
            myListItems = (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoSPMyListAPI.MyListData>)
            initViewPager()
        }

    }

    // ViewPager初期化
    private fun initViewPager() {
        adapter = DevNicoVideoMyListViewPagerAdapter(activity as AppCompatActivity, myListItems)
        fragment_nicovideo_mylist_tablayout.setBackgroundColor(getThemeColor(context))
        fragment_nicovideo_mylist_viewpager.adapter = adapter
        // TabLayout
        TabLayoutMediator(fragment_nicovideo_mylist_tablayout, fragment_nicovideo_mylist_viewpager, TabLayoutMediator.TabConfigurationStrategy { tab, position ->
            tab.text = myListItems[position].title
        }).attach()
    }

    // マイリスト一覧取得
    private fun getMyListItems() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable.message}")
        }
        GlobalScope.launch(errorHandler) {
            val nicoVideoSPMyListAPI = NicoVideoSPMyListAPI()
            // マイリスト一覧取得
            val response = nicoVideoSPMyListAPI.getMyListList(userSession)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
            }
            myListItems = nicoVideoSPMyListAPI.parseMyListList(response.body?.string())
            if (!isAdded) return@launch
            // とりあえずマイリスト追加
            myListItems.add(0, NicoVideoSPMyListAPI.MyListData(getString(R.string.toriaezu_mylist), "", 500))
            // 動画の登録の多い順に並び替える？
            if (prefSetting.getBoolean("setting_nicovideo_mylist_sort_itemcount", false)) {
                myListItems.sortByDescending { myListData -> myListData.itemsCount }
            }
            // ViewPager初期化
            initViewPager()
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", myListItems)
    }

}