package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoCommentFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoMenuFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoContentTreeFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoInfoFragment
import io.github.takusan23.tatimidroid.R

class DevNicoVideoViewPager(val activity: AppCompatActivity, val videoId: String, val isCache: Boolean) :
    FragmentPagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    // Fragmentを返す
    override fun getItem(position: Int): Fragment {
        val devNicoVideoCommentFragment = DevNicoVideoCommentFragment()
        val bundle = Bundle()
        bundle.putString("id", videoId)
        bundle.putBoolean("cache", isCache)
        devNicoVideoCommentFragment.arguments = bundle

        // インターネット接続とキャッシュ再生で分岐
        if (isCache) {
            // キャッシュ再生
            when (position) {
                0 -> {
                    val commentMenuFragment = DevNicoVideoMenuFragment()
                    commentMenuFragment.arguments = bundle
                    return commentMenuFragment
                }
                1 -> {
                    return devNicoVideoCommentFragment
                }
                2 -> {
                    val nicoVideoInfoFragment = NicoVideoInfoFragment()
                    nicoVideoInfoFragment.arguments = bundle
                    return nicoVideoInfoFragment
                }
            }
            return devNicoVideoCommentFragment
        } else {
            // インターネット接続
            when (position) {
                0 -> {
                    val commentMenuFragment = DevNicoVideoMenuFragment()
                    commentMenuFragment.arguments = bundle
                    return commentMenuFragment
                }
                1 -> {
                    return devNicoVideoCommentFragment
                }
                2 -> {
                    val nicoVideoInfoFragment = NicoVideoInfoFragment()
                    nicoVideoInfoFragment.arguments = bundle
                    return nicoVideoInfoFragment
                }
                3 -> {
                    val nicoAdFragment = NicoVideoContentTreeFragment()
                    nicoAdFragment.arguments = bundle
                    return nicoAdFragment
                }
            }
            return devNicoVideoCommentFragment
        }

    }

    // Fragment数
    override fun getCount(): Int {
        if (isCache) {
            // キャッシュ再生
            return 3
        } else {
            // インターネット接続
            return 4
        }
    }

    // TabLayoutの名前
    override fun getPageTitle(position: Int): CharSequence? {
        if (isCache) {
            // キャッシュ再生
            when (position) {
                0 -> return activity.getString(R.string.menu)
                1 -> return activity.getString(R.string.comment)
                2 -> return activity.getString(R.string.nicovideo_info)
                else -> return activity.getString(R.string.comment)
            }
        } else {
            // インターネット接続
            when (position) {
                0 -> return activity.getString(R.string.menu)
                1 -> return activity.getString(R.string.comment)
                2 -> return activity.getString(R.string.nicovideo_info)
                3 -> return activity.getString(R.string.parent_contents)
                else -> return activity.getString(R.string.comment)
            }
        }
    }

}