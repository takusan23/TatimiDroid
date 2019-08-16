package io.github.takusan23.tatimidroid

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity

class DarkModeSupport(context: Context) {

    var nightMode: Int = Configuration.UI_MODE_NIGHT_NO

    init {
        //ダークモードかどうか
        val conf = context.resources.configuration
        nightMode = conf.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    /**
     * ダークモードテーマ。setContentView()の前に書いてね
     * */
    fun setActivityTheme(activity: AppCompatActivity) {
        when (nightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                activity.setTheme(R.style.AppTheme)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                activity.setTheme(R.style.OLEDTheme)
            }
        }
    }

    /**
     * 色を返す。白か黒か
     * ダークモード->黒
     * それ以外->白
     * */
    fun getThemeColor(): Int {
        when (nightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                return Color.parseColor("#ffffff")
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                return Color.parseColor("#000000")
            }
        }
        return Color.parseColor("#ffffff")
    }

}