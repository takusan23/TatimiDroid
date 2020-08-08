package io.github.takusan23.tatimidroid.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import io.github.takusan23.tatimidroid.NicoLive.CommunityListFragment
import io.github.takusan23.tatimidroid.R

class ProgramListViewPager(val activity: AppCompatActivity) :
    FragmentPagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putInt("page", position)
        val communityListFragment = CommunityListFragment()
        communityListFragment.arguments = bundle
        return communityListFragment
    }

    override fun getCount(): Int {
        return 7
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when (position) {
            0 -> return activity.getString(R.string.follow_program)
            1 -> return activity.getString(R.string.nicorepo)
            2 -> return activity.getString(R.string.osusume)
            3 -> return activity.getString(R.string.ranking)
            4 -> return activity.getString(R.string.nico_nama_game_recruiting_program)
            5 -> return activity.getString(R.string.nico_nama_game_playing_program)
            6 -> return activity.getString(R.string.auto_admission)
        }
        return activity.getString(R.string.follow_program)
    }

}