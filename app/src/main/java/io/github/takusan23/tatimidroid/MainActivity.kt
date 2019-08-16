package io.github.takusan23.tatimidroid

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.takusan23.tatimidroid.Fragment.*
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_main)

        //ダークモード対応
        main_activity_bottom_navigationview.backgroundTintList = ColorStateList.valueOf(darkModeSupport.getThemeColor())
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(darkModeSupport.getThemeColor()))
        }

        //生放送ID入力
        val fragmentTransitionSupport = supportFragmentManager.beginTransaction()
        fragmentTransitionSupport.replace(R.id.main_activity_linearlayout, LiveIDFragment())
        fragmentTransitionSupport.commit()
        //画面切り替え
        main_activity_bottom_navigationview.setSelectedItemId(R.id.menu_liveid);
        main_activity_bottom_navigationview.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_login -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, LoginFragment())
                    fragmentTransaction.commit()
                }
                R.id.menu_liveid -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, LiveIDFragment())
                    fragmentTransaction.commit()
                }
                R.id.menu_community -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, CommunityListFragment())
                    fragmentTransaction.commit()
                }
                R.id.menu_setting -> {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.main_activity_linearlayout, SettingsFragment())
                    fragmentTransaction.commit()
                }
            }
            true
        }
    }
}
