package io.github.takusan23.tatimidroid.NicoVideo

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_nicovideo.*

class NicoVideoActivity : AppCompatActivity() {

    lateinit var darkModeSupport: DarkModeSupport
    lateinit var prefSetting: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_nicovideo)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        val id = intent.getStringExtra("id")
        val isCache = intent?.getBooleanExtra("cache", false) ?: false
        if (prefSetting.getBoolean("fragment_dev_niconico_video", false)) {
            // Fragment再生成するかどうか
            val checkCommentViewFragment =
                supportFragmentManager.findFragmentByTag(id)
            val fragment = if (checkCommentViewFragment != null) {
                checkCommentViewFragment as DevNicoVideoFragment
            } else {
                val nicoVideoFragment =
                    DevNicoVideoFragment()
                val bundle = Bundle()
                bundle.putString("id", id)
                bundle.putBoolean("cache", isCache)
                nicoVideoFragment.arguments = bundle
                nicoVideoFragment
            }
            //あとから探せるように第三引数にID入れる
            supportFragmentManager.beginTransaction()
                .replace(activity_nicovideo_parent_linearlayout.id, fragment, id)
                .commit()

        } else {
            val nicoVideoFragment = NicoVideoFragment()
            val bundle = Bundle()
            bundle.putString("id", id)
            nicoVideoFragment.arguments = bundle
            //あとから探せるように第三引数にID入れる
            supportFragmentManager.beginTransaction()
                .replace(activity_nicovideo_parent_linearlayout.id, nicoVideoFragment, id).commit()
        }

    }
}
