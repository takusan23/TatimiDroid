package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.DevNicoVideo.*
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import org.json.JSONObject

/**
 * ニコ動の方だけViewPager2になった。データ取得後に初期化してください。
 * Fragmentをフリックで切り替えられるやつ。２にしたおかげか動的追加できるようになった
 * @param parentDevNicoVideoFragment コメント配列とかをこっからもらう。生成後にわたすとなんかうまく行かなくて；；
 * */
class DevNicoVideoRecyclerPagerAdapter(val activity: AppCompatActivity, val videoId: String, val isCache: Boolean, val parentDevNicoVideoFragment: DevNicoVideoFragment) :
    FragmentStateAdapter(activity) {

    val fragmentList = arrayListOf<Fragment>()
    val fragmentTabName = arrayListOf<String>()

    val bundle = Bundle()

    // 動的にFragment追加して見る
    init {
        bundle.putString("id", videoId)
        bundle.putBoolean("cache", isCache)
        // 動画情報JSONがあるかどうか。なければ動画情報Fragmentを非表示にするため
        val nicoVideoCache = NicoVideoCache(activity)
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
                val nicoVideoInfoFragment = NicoVideoInfoFragment().apply {
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
            val nicoVideoInfoFragment = NicoVideoInfoFragment().apply {
                arguments = bundle
            }
            val nicoVideoRecommendFragment = DevNicoVideoRecommendFragment().apply {
                arguments = bundle
            }
            fragmentList.apply {
                add(commentMenuFragment)
                add(devNicoVideoCommentFragment)
                add(nicoVideoInfoFragment)
                add(nicoVideoRecommendFragment)
                // add(nicoContentTree)
            }
            fragmentTabName.apply {
                add(activity.getString(R.string.menu))
                add(activity.getString(R.string.comment))
                add(activity.getString(R.string.nicovideo_info))
                add(activity.getString(R.string.recommend_video))
                // add(activity.getString(R.string.parent_contents))
            }
        }
    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    fun destory() {

    }

}