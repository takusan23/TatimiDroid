package io.github.takusan23.tatimidroid.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.takusan23.searchpreferencefragment.SearchPreferenceChildFragment
import io.github.takusan23.searchpreferencefragment.SearchPreferenceFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.fragment.SettingsFragment

/**
 * 設定画面。Activityに
 * */
class SettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        // 自作ライブラリ https://github.com/takusan23/SearchPreferenceFragment
        val searchSettingsFragment = SettingsFragment()
        searchSettingsFragment.arguments = Bundle().apply {
            // 階層化されている場合
            val hashMap = hashMapOf<String, Int>()
            putSerializable(SearchPreferenceFragment.PREFERENCE_XML_FRAGMENT_NAME_HASH_MAP, hashMap)
            putInt(SearchPreferenceChildFragment.PREFERENCE_XML_RESOURCE_ID, R.xml.preferences)
        }
        supportFragmentManager.beginTransaction().replace(R.id.activity_setting_fragment_host_framelayout, searchSettingsFragment).commit()

    }
}