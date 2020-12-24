package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoMyListViewPagerAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoVideoMyListViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoMyListViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist.*

/**
 * マイリストFragment。RecyclerViewが乗ってるFragmentはDevNicoVideoMyListListFragmentです。
 * このFragmentはTabLayout+ViewPager2が乗ってるだけ。
 * */
class NicoVideoMyListFragment : Fragment() {

    /** ViewModel */
    private lateinit var myListViewModel: NicoVideoMyListViewModel

    /** ViewPagerのAdapter */
    lateinit var adapter: NicoVideoMyListViewPagerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_mylist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        myListViewModel = ViewModelProvider(this, NicoVideoMyListViewModelFactory(requireActivity().application, userId)).get(NicoVideoMyListViewModel::class.java)

        // マイリスト一覧受け取り
        myListViewModel.myListDataLiveData.observe(viewLifecycleOwner) { myListItems ->
            // 自分の場合は先頭にとりあえずマイリスト追加する
            if (userId == null) {
                // とりあえずマイリスト追加
                myListItems.add(0, NicoVideoSPMyListAPI.MyListData(getString(R.string.atodemiru), "", 500, true))
            }
            // ViewPager初期化
            initViewPager(myListItems)
        }

    }

    // ViewPager初期化
    private fun initViewPager(myListItems: ArrayList<NicoVideoSPMyListAPI.MyListData>) {
        adapter = NicoVideoMyListViewPagerAdapter(activity as AppCompatActivity, myListItems)
        fragment_nicovideo_mylist_tablayout.setBackgroundColor(getThemeColor(context))
        fragment_nicovideo_mylist_viewpager.adapter = adapter
        // TabLayout
        TabLayoutMediator(fragment_nicovideo_mylist_tablayout, fragment_nicovideo_mylist_viewpager) { tab, position ->
            // マイリストに登録してる動画数。あとで見るは何件かわからんので（API叩くのもめんどい）
            val itemCount = if (myListItems[position].title != getString(R.string.atodemiru)) ":${myListItems[position].itemsCount}" else ""
            tab.text = "${myListItems[position].title}$itemCount"
        }.attach()
    }

}