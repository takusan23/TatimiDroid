package io.github.takusan23.tatimidroid.NicoVideo

import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_nicovideo.*

class NicoVideoActivity : AppCompatActivity() {

    lateinit var darkModeSupport: DarkModeSupport
    lateinit var prefSetting:SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_nicovideo)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(this)

        val id = intent.getStringExtra("id")
        if(prefSetting.getBoolean("fragment_dev_niconico_video",false)){
            val nicoVideoFragment = DevNicoVideoFragment()
            val bundle = Bundle()
            bundle.putString("id", id)
            nicoVideoFragment.arguments = bundle
            //あとから探せるように第三引数にID入れる
            supportFragmentManager.beginTransaction()
                .replace(activity_nicovideo_parent_linearlayout.id, nicoVideoFragment, id).commit()
        }else{
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
