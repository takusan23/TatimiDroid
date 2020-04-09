package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoCommentFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoMenuFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoInfoFragment
import io.github.takusan23.tatimidroid.R

class DevNicoVideoViewPager(val activity: AppCompatActivity, val videoId: String, val isCache: Boolean) :
    FragmentPagerAdapter(activity.supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    // Fragmentの配列
    val fragmentList = arrayListOf<Fragment>()
    val fragmentTabName = arrayListOf<String>()

    val bundle = Bundle()

    // 動的にFragment追加して見る
    init {
        bundle.putString("id", videoId)
        bundle.putBoolean("cache", isCache)
        // 動画情報JSONがあるかどうか。なければ動画情報Fragmentを非表示にするため
        val nicoVideoCache =
            NicoVideoCache(activity)
        val exists = nicoVideoCache.existsCacheVideoInfoJSON(videoId)
        // インターネット接続とキャッシュ再生で分岐
        if (isCache) {
            val commentMenuFragment = DevNicoVideoMenuFragment().apply {
                arguments = bundle
            }
            val devNicoVideoCommentFragment = DevNicoVideoCommentFragment().apply {
                arguments = bundle
            }

            fragmentList.apply {
                add(commentMenuFragment)
                add(devNicoVideoCommentFragment)
            }
            fragmentTabName.apply {
                add(activity.getString(R.string.menu))
                add(activity.getString(R.string.comment))
            }
            if (exists) {
                // 動画情報JSONがあれば動画情報Fragmentを表示させる
                val nicoVideoInfoFragment = NicoVideoInfoFragment()
                    .apply {
                    arguments = bundle
                }
                fragmentList.add(nicoVideoInfoFragment)
                fragmentTabName.add(activity.getString(R.string.nicovideo_info))
            }
        } else {
            val commentMenuFragment = DevNicoVideoMenuFragment().apply {
                arguments = bundle
            }
            val devNicoVideoCommentFragment = DevNicoVideoCommentFragment().apply {
                arguments = bundle
            }
            val nicoVideoInfoFragment = NicoVideoInfoFragment()
                .apply {
                arguments = bundle
            }
            fragmentList.apply {
                add(commentMenuFragment)
                add(devNicoVideoCommentFragment)
                add(nicoVideoInfoFragment)
                // add(nicoContentTree)
            }
            fragmentTabName.apply {
                add(activity.getString(R.string.menu))
                add(activity.getString(R.string.comment))
                add(activity.getString(R.string.nicovideo_info))
                // add(activity.getString(R.string.parent_contents))
            }
        }
    }

    // Fragmentを返す
    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    // Fragment数
    override fun getCount(): Int {
        return fragmentList.size
    }

    // TabLayoutの名前
    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentTabName[position]
    }

}