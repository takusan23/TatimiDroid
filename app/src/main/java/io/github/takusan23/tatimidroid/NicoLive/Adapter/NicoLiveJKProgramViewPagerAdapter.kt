package io.github.takusan23.tatimidroid.NicoLive.Adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.NicoLive.NicoLiveJKProgramListFragment
import io.github.takusan23.tatimidroid.R

/**
 * ニコニコ実況の公式、実況タグの切り替えViewPager
 * @param parentFragment [FragmentStateAdapter]の引数に必要
 * */
class NicoLiveJKProgramViewPagerAdapter(private val parentFragment: Fragment) : FragmentStateAdapter(parentFragment) {

    /** Fragmentの数を返す */
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        val bundle = Bundle()
        // Fragmentにわたす値
        when (position) {
            0 -> bundle.putString("type", NicoLiveJKProgramListFragment.NICOLIVE_JK_PROGRAMLIST_OFFICIAL)
            1 -> bundle.putString("type", NicoLiveJKProgramListFragment.NICOLIVE_JK_PROGRAMLIST_TAG)
        }
        // Fragmentを返す
        return NicoLiveJKProgramListFragment().apply {
            arguments = bundle
        }
    }

    /**
     * TabLayoutとViewPager2を連携する際はこの関数を利用することでタブの名前を取得できます
     *
     * @param position タブの位置
     * */
    fun getTabName(position: Int): String {
        return when (position) {
            0 -> parentFragment.context?.getString(R.string.nicolive_jk_program_official) ?: "公式"
            1 -> parentFragment.context?.getString(R.string.nicolive_jk_program_tag) ?: "ニコニコ実況タグ（有志）"
            else -> ""
        }
    }

}