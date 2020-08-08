package io.github.takusan23.tatimidroid.NicoLive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import io.github.takusan23.tatimidroid.Adapter.NicoJKChannelViewPager
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_jk_channel.*

/**
 * ニコJKのViewPager2やらTabLayoutが乗ってるFragment。
 * NicoJKProgramListFragmentはViewPager2が表示する
 * */
class NicoJKChannelFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_jk_channel, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewPagerのAdapter作成
        val adapter = NicoJKChannelViewPager(activity as AppCompatActivity)
        fragment_jk_channel_viewpager2.adapter = adapter
        // TabLayout
        TabLayoutMediator(fragment_jk_channel_tablayout, fragment_jk_channel_viewpager2, TabLayoutMediator.TabConfigurationStrategy { tab, position ->
            tab.text = adapter.tabNameList[position]
        }).attach()

    }

}