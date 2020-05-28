package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.github.takusan23.tatimidroid.Activity.KonoApp
import io.github.takusan23.tatimidroid.Activity.LicenceActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NicoHistorySQLiteHelper
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import java.io.File
import java.nio.file.Files
import java.nio.file.Files.readAttributes
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat


class SettingsFragment : PreferenceFragmentCompat() {

    val privacy_policy = "https://github.com/takusan23/TatimiDroid/blob/master/privacy_policy.md"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val licence_preference = findPreference<Preference>("licence_preference")
        val konoapp_preference = findPreference<Preference>("konoapp_preference")
        val auto_admission_stop_preference =
            findPreference<Preference>("auto_admission_stop_preference")
        val konoapp_privacy = findPreference<Preference>("konoapp_privacy")
        // フォント設定へ行くPreference
        val fontPreference = findPreference<Preference>("font_preference")
        // 開発者用項目
        val devSetting = findPreference<Preference>("dev_setting")
        // キャッシュ再生について
        val aboutCache = findPreference<Preference>("about_cache")
        // 一番最初に使った日
        val firstTimeDay = findPreference<Preference>("first_time_preference")
        // DarkModeSwitch
        val darkmode_switch_preference = findPreference<SwitchPreference>("setting_darkmode")

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
        konoapp_privacy?.setOnPreferenceClickListener {
            //プライバシーポリシー
            startBrowser(privacy_policy)
            true
        }


        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            darkmode_switch_preference?.isVisible = true
        }

        fontPreference?.setOnPreferenceClickListener {
            // フォント設定へ切り替え
            fragmentManager?.beginTransaction()?.apply {
                replace(
                    R.id.main_activity_linearlayout,
                    FontSettingFragment(),
                    "font_preference"
                )
                addToBackStack(null)
            }?.commit()
            true
        }

        // 開発者用設定項目
        devSetting?.setOnPreferenceClickListener {
            // フォント設定へ切り替え
            fragmentManager?.beginTransaction()?.apply {
                replace(
                    R.id.main_activity_linearlayout,
                    DevSettingFragment(),
                    "dev_preference"
                )
                addToBackStack(null)
            }?.commit()
            true
        }

        aboutCache?.setOnPreferenceClickListener {
            startBrowser("https://takusan23.github.io/Bibouroku/2020/04/08/たちみどろいどのキャッシュ機能について/")
            true
        }

        firstTimeDay?.setOnPreferenceClickListener { preference ->
            // DB用意
            val nicoHistorySQLiteHelper = NicoHistorySQLiteHelper(requireContext())
            val sqLiteDatabase = nicoHistorySQLiteHelper.writableDatabase
            nicoHistorySQLiteHelper.setWriteAheadLoggingEnabled(false)
            // 探す
            val cursor = sqLiteDatabase.query(NicoHistorySQLiteHelper.TABLE_NAME, arrayOf("service_id", "type", "date", "title", "user_id"), null, null, null, null, null)
            cursor.moveToFirst()
            if (cursor.count > 0) {
                val title = cursor.getString(3)
                val time = cursor.getString(2)
                val id = cursor.getString(1)
                val service = cursor.getString(0)
                preference.summary = "最初に見たもの：$title ($id/ $service)\n一番最初に使った日：${toFormatTime(time.toLong() * 1000)}"
            }
            cursor.close()
            false
        }
    }

    /** UnixTime -> わかりやすい形式に */
    private fun toFormatTime(time: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日HH時mm分ss秒")
        return simpleDateFormat.format(time)
    }


    fun startBrowser(link: String) {
        val i = Intent(Intent.ACTION_VIEW, link.toUri());
        startActivity(i);
    }

}