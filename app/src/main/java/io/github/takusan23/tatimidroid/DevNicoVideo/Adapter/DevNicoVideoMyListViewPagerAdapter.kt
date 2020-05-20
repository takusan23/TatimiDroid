package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoMyListListFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI

/**
 * マイリスト切り替えViewPagerAdapter。
 * フリックで切り替えできるようになるよ。
 * @param myListList とりあえずマイリストはMyListData#idの値を空にしてね
 * */
class DevNicoVideoMyListViewPagerAdapter(val activity: AppCompatActivity, val myListList: ArrayList<NicoVideoSPMyListAPI.MyListData>) : FragmentStateAdapter(activity) {

    /**
     * Fragment一覧。
     * Fragment#isAdded確認いるかも
     * */
    val fragmentList = arrayListOf<Fragment>().apply {
        myListList.forEach {
            val fragment = DevNicoVideoMyListListFragment()
            val bundle = Bundle().apply {
                putString("mylist_id", it.id)
            }
            fragment.arguments = bundle
            add(fragment)
        }
    }

    override fun getItemCount(): Int = myListList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }
}