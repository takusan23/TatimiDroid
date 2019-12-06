package io.github.takusan23.tatimidroid.NicoVideo

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_nico_video_comment.*

class NicoVideoCommentActivity : AppCompatActivity() {

    lateinit var darkModeSupport: DarkModeSupport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_nico_video_comment)

        //動画ID取得
        val id = intent.getStringExtra("id")

        //FragmentにID詰める
        val bundle = Bundle()
        bundle.putString("id", id)
        val fragment = NicoVideoCommentFragment()
        fragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.activity_comment_linearlayout, fragment).commit()

        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            nicovieo_activity_comment_tab_layout.setBackgroundColor(darkModeSupport.getThemeColor())
            supportActionBar?.setBackgroundDrawable(ColorDrawable(darkModeSupport.getThemeColor()))
        }


        /*
        * タブ切り替え
        * */
        nicovieo_activity_comment_tab_layout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.comment) -> {
                        //コメント
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.activity_comment_linearlayout, fragment).commit()
                    }
                    getString(R.string.nicovideo_info) -> {
                        //動画情報
                        val fragment = NicoVideoInfoFragment()
                        fragment.arguments = bundle
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.activity_comment_linearlayout, fragment).commit()
                    }
                }
            }
        })

    }
}
