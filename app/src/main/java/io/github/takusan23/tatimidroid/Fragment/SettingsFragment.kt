package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.takusan23.tatimidroid.Activity.LicenceActivity
import io.github.takusan23.tatimidroid.Activity.KonoApp
import io.github.takusan23.tatimidroid.AutoAdmissionService
import io.github.takusan23.tatimidroid.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val licence_preference = findPreference<Preference>("licence_preference")
        val konoapp_preference = findPreference<Preference>("konoapp_preference")
        val auto_admission_stop_preference = findPreference<Preference>("auto_admission_stop_preference")

        licence_preference?.setOnPreferenceClickListener {
            //ライセンス画面
            startActivity(Intent(context, LicenceActivity::class.java))
            true
        }
        konoapp_preference?.setOnPreferenceClickListener {
            //このアプリについて
            startActivity(Intent(context, KonoApp::class.java))
            true
        }
        auto_admission_stop_preference?.setOnPreferenceClickListener {
            //Service再起動
            val intent = Intent(context, AutoAdmissionService::class.java)
            context!!.stopService(intent)
        }
    }
}