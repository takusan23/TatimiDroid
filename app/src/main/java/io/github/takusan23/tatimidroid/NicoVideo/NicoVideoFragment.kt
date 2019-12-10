package io.github.takusan23.tatimidroid.NicoVideo

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nico_video_comment.*

class NicoVideoFragment : Fragment() {

    lateinit var darkModeSupport: DarkModeSupport

    var isHide3DSComment = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nico_video_comment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        darkModeSupport = DarkModeSupport(context!!)

        //動画ID取得
        val id = arguments?.getString("id")

        //FragmentにID詰める
        val bundle = Bundle()
        bundle.putString("id", id)
        val fragment = NicoVideoCommentFragment()
        fragment.arguments = bundle

        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
            .replace(R.id.activity_comment_linearlayout, fragment)
            .commit()

        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            nicovieo_fragment_comment_tab_layout.setBackgroundColor(darkModeSupport.getThemeColor())
            (activity as AppCompatActivity).supportActionBar?.setBackgroundDrawable(
                ColorDrawable(
                    darkModeSupport.getThemeColor()
                )
            )
        }

        //コメントを選んでおく
        nicovieo_fragment_comment_tab_layout.selectTab(
            nicovieo_fragment_comment_tab_layout.getTabAt(
                1
            )
        )


        /*
        * タブ切り替え
        * */
        nicovieo_fragment_comment_tab_layout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.comment) -> {
                        //コメント
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.activity_comment_linearlayout, fragment).commit()
                    }
                    getString(R.string.nicovideo_info) -> {
                        //動画情報
                        val fragment = NicoVideoInfoFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.activity_comment_linearlayout, fragment).commit()
                    }
                    getString(R.string.menu) -> {
                        val fragment = NicoVideoMenuFragment()
                        fragment.arguments = bundle
                        (activity as AppCompatActivity).supportFragmentManager.beginTransaction()
                            .replace(R.id.activity_comment_linearlayout, fragment).commit()
                    }
                }
            }
        })


    }

}