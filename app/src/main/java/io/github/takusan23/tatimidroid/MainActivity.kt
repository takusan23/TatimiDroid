package io.github.takusan23.tatimidroid

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Fragment.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_liveid.*
import java.util.regex.Pattern


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

        //共有から起動した
        lunchShareIntent()

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

    //共有から起動した場合
    private fun lunchShareIntent() {
        if (Intent.ACTION_SEND.equals(intent.action)) {
            val extras = intent.extras
            //URL
            val url = extras?.getCharSequence(Intent.EXTRA_TEXT)
            //生放送ID取得
            //正規表現で取り出す
            val nicoID_Matcher = Pattern.compile("(lv)([0-9]+)")
                .matcher(SpannableString(url ?: ""))
            if (nicoID_Matcher.find()) {
                //ダイアログ
                val liveId = nicoID_Matcher.group()
                val bundle = Bundle()
                bundle.putString("liveId", liveId)
                val dialog = BottomSheetDialogWatchMode()
                dialog.arguments = bundle
                dialog.show(supportFragmentManager, "watchmode")
            } else {
                //なかった。
                Toast.makeText(this, getString(R.string.regix_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
