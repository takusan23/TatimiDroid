package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.*
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoMyListFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoPOSTFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoSearchFragment
import io.github.takusan23.tatimidroid.FregmentData.TabLayoutData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R

/**
 * ニコ動の方だけViewPager2になった。
 * Fragmentをフリックで切り替えられるやつ。２にしたおかげか動的追加できるようになった
 * @param dynamicAddFragmentList 動的に追加したFragmentがある場合は入れてね。ない場合は省略していいよ（そこにないなら無いですね）。主に画面回転復帰時に使う。
 * */
class DevNicoVideoRecyclerPagerAdapter(val activity: AppCompatActivity, val videoId: String, val isCache: Boolean, val dynamicAddFragmentList: ArrayList<TabLayoutData> = arrayListOf()) : FragmentStateAdapter(activity) {

    // 画面回転時に回転前に動的にFragmentを追加場合復元するからその時使う
    companion object {
        const val TAB_LAYOUT_DATA_SEARCH = "search"
        const val TAB_LAYOUT_DATA_MYLIST = "mylist"
        const val TAB_LAYOUT_DATA_POST = "post"
    }

    /** Fragment一覧 */
    val fragmentList = arrayListOf<Fragment>()

    /** Fragment名一覧 */
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

        // 引数に指定したFragmentがある場合
        dynamicAddFragmentList.toList().forEach { data ->
            // Fragment作る
            when (data.type) {
                TAB_LAYOUT_DATA_SEARCH -> DevNicoVideoSearchFragment()
                TAB_LAYOUT_DATA_POST -> DevNicoVideoPOSTFragment()
                TAB_LAYOUT_DATA_MYLIST -> DevNicoVideoMyListFragment()
                else -> null
            }?.let { fragment ->
                // Bundle詰める
                fragment.arguments = data.bundle
                fragmentList.add(fragment)
                fragmentTabName.add(data.text ?: "タブ")
                notifyDataSetChanged() // 更新
            }
        }

    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    /**
     * 動的にFragmentを追加する時に使う関数。
     * 注意：対応しているFragmentは以下のとおりです。未来の私へ。。。
     *  - DevNicoVideoPOSTFragment
     *  - DevNicoVideoSearchFragment
     *  - DevNicoVideoMyListListFragment
     *  - 将来的にはシリーズ機能も（一般とかはマイリスト制限きついからシリーズ機能使ってそう（しらんけど））
     * @param fragment 追加したいFragment
     * @param tabName TabLayoutで表示する名前
     * */
    fun addFragment(fragment: Fragment, tabName: String) {
        fragmentList.add(fragment)
        fragmentTabName.add(tabName)
        notifyDataSetChanged() // 更新
        dynamicAddFragmentList.add(TabLayoutData(getType(fragment), tabName, fragment.arguments))
    }

    /**
     * FragmentからTAB_LAYOUT_DATA_SEARCHとかを生成する
     * @param fragment addFragment()で追加可能なFragment
     * */
    private fun getType(fragment: Fragment): String {
        return when {
            fragment is DevNicoVideoPOSTFragment -> TAB_LAYOUT_DATA_POST
            fragment is DevNicoVideoMyListFragment -> TAB_LAYOUT_DATA_MYLIST
            fragment is DevNicoVideoSearchFragment -> TAB_LAYOUT_DATA_SEARCH
            else -> "" // ありえない
        }
    }

}