package io.github.takusan23.tatimidroid.Adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoSelectFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoCacheFragment
import io.github.takusan23.tatimidroid.Fragment.LoginFragment
import io.github.takusan23.tatimidroid.Fragment.ProgramListFragment
import io.github.takusan23.tatimidroid.Fragment.SettingsFragment
import io.github.takusan23.tatimidroid.MainActivity

/**
 * [MainActivity]（最初に表示されるActivity）で設置するFragmentの状態を保持するためだけに作ったViewPager2のAdapter。
 * */
class MainActivityFragmentStateViewAdapter(val activity: AppCompatActivity) : FragmentStateAdapter(activity) {

    /**
     * 動的に生成したFragment
     * */
    private val createFragmentList = arrayListOf<Fragment>()


    init {
        // 内部でf+位置でFragmentにタグを付けているらしい
        repeat(5) { count ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("f$count")
            if (fragment != null) {
                createFragmentList.add(fragment)
            }
        }
    }

    // Fragmentは5個なので
    override fun getItemCount() = createFragmentList.size

    /** Fragmentを返す */
    override fun createFragment(position: Int): Fragment {
        return createFragmentList[position]
    }

    /**
     * Fragment動的生成追加関数。
     * なんでこんなことしてるかって言うと通信量を節約したいから
     * （ViewPagerだと他Fragmentの初期化処理走って無駄に通信しちゃう。キャッシュだけ表示してほしいのにランキング取りにいったりするから。）
     * @param viewPager2 ViewPager2を切り替えるのに使う。
     * @param fragmentNo 作成するFragment。
     * [MAIN_ACTIVITY_VIEWPAGER2_NICOVIDEO] [MAIN_ACTIVITY_VIEWPAGER2_NICOLIVE] など
     * */
    fun setFragment(fragmentNo: Int, viewPager2: ViewPager2) {
        // すでに同じFragmentがあれば追加しない
        val fragment = when (fragmentNo) {
            MAIN_ACTIVITY_VIEWPAGER2_NICOLIVE -> ProgramListFragment()
            MAIN_ACTIVITY_VIEWPAGER2_NICOVIDEO -> DevNicoVideoSelectFragment()
            MAIN_ACTIVITY_VIEWPAGER2_CACHE -> DevNicoVideoCacheFragment()
            MAIN_ACTIVITY_VIEWPAGER2_LOGIN -> LoginFragment()
            MAIN_ACTIVITY_VIEWPAGER2_SETTING -> SettingsFragment()
            else -> LoginFragment()
        }
        // javaClassつければ == で比較可能。
        val createdPos = createFragmentList.indexOfFirst { createdFragment -> createdFragment.javaClass == fragment.javaClass }
        if (createdPos != -1) {
            // すでに存在する場合は
            viewPager2.currentItem = createdPos
        } else {
            // 新規
            createFragmentList.add(fragment)
            notifyDataSetChanged() // ViewPager2で使えるようになったぞ
            viewPager2.currentItem = createFragmentList.size
        }
    }

    // MainActivity#setPageの引数たち。
    companion object {
        const val MAIN_ACTIVITY_VIEWPAGER2_NICOLIVE = 0
        const val MAIN_ACTIVITY_VIEWPAGER2_NICOVIDEO = 1
        const val MAIN_ACTIVITY_VIEWPAGER2_CACHE = 2
        const val MAIN_ACTIVITY_VIEWPAGER2_LOGIN = 3
        const val MAIN_ACTIVITY_VIEWPAGER2_SETTING = 4
    }

}