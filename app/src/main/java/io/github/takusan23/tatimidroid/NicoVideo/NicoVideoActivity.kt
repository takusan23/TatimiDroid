package io.github.takusan23.tatimidroid.NicoVideo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_nicovideo.*

class NicoVideoActivity : AppCompatActivity() {

    lateinit var darkModeSupport: DarkModeSupport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_nicovideo)

        val id = intent.getStringExtra("id")

        val nicoVideoFragment = NicoVideoFragment()
        val bundle = Bundle()
        bundle.putString("id", id)
        nicoVideoFragment.arguments = bundle

        //あとから探せるように第三引数にID入れる
        supportFragmentManager.beginTransaction()
            .replace(activity_nicovideo_parent_linearlayout.id, nicoVideoFragment, id).commit()

    }
}
