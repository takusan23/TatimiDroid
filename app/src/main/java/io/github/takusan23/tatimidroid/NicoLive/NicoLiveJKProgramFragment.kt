package io.github.takusan23.tatimidroid.NicoLive

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.NicoLive.Adapter.NicoLiveJKProgramViewPagerAdapter
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveJkProgramBinding

/**
 * ニコニコ実況番組一覧Fragment（[NicoLiveJKProgramListFragment]）を乗せるためのFragment
 *
 * なのでこのFragmentにはViewPager2とTabLayoutしかないねん
 * */
class NicoLiveJKProgramFragment : Fragment() {

    /** ViewBinding */
    private val viewBinding by lazy { FragmentNicoliveJkProgramBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモード
        viewBinding.fragmentNicoliveJkProgramTabLayout.background = ColorDrawable(getThemeColor(context))

        // ViewPager2設定
        val adapter = NicoLiveJKProgramViewPagerAdapter(this)
        viewBinding.fragmentNicoliveJkProgramViewPager.adapter = adapter

        // タブ
        TabLayoutMediator(viewBinding.fragmentNicoliveJkProgramTabLayout, viewBinding.fragmentNicoliveJkProgramViewPager) { tab, position ->
            tab.text = adapter.getTabName(position)
        }.attach()  // 書き忘れ注意

    }
}