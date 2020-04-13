package io.github.takusan23.tatimidroid.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.Fragment.*
import io.github.takusan23.tatimidroid.R

class CommentViewPager(val activity: AppCompatActivity, val liveId: String, val isOfficial: Boolean = false, val isJK: Boolean = false) :
    FragmentStatePagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    fun liveIdBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        return bundle
    }

    override fun getItem(position: Int): Fragment {
        val commentViewFragment = CommentViewFragment()
        commentViewFragment.arguments = liveIdBundle()
        when {
            isOfficial -> {
                when (position) {
                    0 -> {
                        val commentMenuFragment = CommentMenuFragment()
                        commentMenuFragment.arguments = liveIdBundle()
                        return commentMenuFragment
                    }
                    1 -> {
                        return commentViewFragment
                    }
                    2 -> {
                        val giftFragment = GiftFragment()
                        giftFragment.arguments = liveIdBundle()
                        return giftFragment
                    }
                    3 -> {
                        val nicoAdFragment = NicoAdFragment()
                        nicoAdFragment.arguments = liveIdBundle()
                        return nicoAdFragment
                    }
                    4 -> {
                        val programInfoFragment = ProgramInfoFragment()
                        programInfoFragment.arguments = liveIdBundle()
                        return programInfoFragment
                    }
                }
            }
            isJK -> {
                when (position) {
                    0 -> {
                        val commentMenuFragment = CommentMenuFragment()
                        commentMenuFragment.arguments = liveIdBundle()
                        return commentMenuFragment
                    }
                    1 -> {
                        return commentViewFragment
                    }
                }
            }
            else -> {
                when (position) {
                    0 -> {
                        val commentMenuFragment = CommentMenuFragment()
                        commentMenuFragment.arguments = liveIdBundle()
                        return commentMenuFragment
                    }
                    1 -> {
                        return commentViewFragment
                    }
                    2 -> {
                        val commentRoomFragment = CommentRoomFragment()
                        commentRoomFragment.arguments = liveIdBundle()
                        return commentRoomFragment
                    }
                    3 -> {
                        val giftFragment = GiftFragment()
                        giftFragment.arguments = liveIdBundle()
                        return giftFragment
                    }
                    4 -> {
                        val nicoAdFragment = NicoAdFragment()
                        nicoAdFragment.arguments = liveIdBundle()
                        return nicoAdFragment
                    }
                    5 -> {
                        val programInfoFragment = ProgramInfoFragment()
                        programInfoFragment.arguments = liveIdBundle()
                        return programInfoFragment
                    }
                }
            }
        }
        return commentViewFragment
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when {
            isOfficial -> {
                when (position) {
                    0 -> return activity.getString(R.string.menu)
                    1 -> return activity.getString(R.string.comment)
                    2 -> return activity.getString(R.string.gift)
                    3 -> return activity.getString(R.string.nicoads)
                    4 -> return activity.getString(R.string.program_info)
                }
            }
            isJK -> {
                when (position) {
                    0 -> return activity.getString(R.string.menu)
                    1 -> return activity.getString(R.string.comment)
                }
            }
            else -> {
                when (position) {
                    0 -> return activity.getString(R.string.menu)
                    1 -> return activity.getString(R.string.comment)
                    2 -> return activity.getString(R.string.room_comment)
                    3 -> return activity.getString(R.string.gift)
                    4 -> return activity.getString(R.string.nicoads)
                    5 -> return activity.getString(R.string.program_info)
                }
            }
        }
        return activity.getString(R.string.menu)
    }

    override fun getCount(): Int {
        return when {
            isOfficial -> 5
            isJK -> 2
            else -> 6
        }
    }
}