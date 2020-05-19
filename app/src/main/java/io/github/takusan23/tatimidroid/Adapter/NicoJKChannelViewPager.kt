package io.github.takusan23.tatimidroid.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.Fragment.NicoJKProgramListFragment
import io.github.takusan23.tatimidroid.R

/**
 * ニコニコ実況チャンネル一覧切り替えViewPager
 * テレビ以外にラジオとかも見れるようになるよ
 * */
class NicoJKChannelViewPager(val activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    // テレビ / ラジオ / BS の3つ
    override fun getItemCount(): Int = 3

    // Fragmentを返す
    override fun createFragment(position: Int): Fragment {
        val channel = listOf("tv", "radio", "bs")
        val bundle = Bundle().apply {
            putString("type", channel[position])
        }
        val nicoJKProgramListFragment = NicoJKProgramListFragment()
        nicoJKProgramListFragment.arguments = bundle
        return nicoJKProgramListFragment
    }

    // TabLayoutに返す名前
    val tabNameList = listOf(activity.getString(R.string.tv), activity.getString(R.string.radio), activity.getString(R.string.bs))

}