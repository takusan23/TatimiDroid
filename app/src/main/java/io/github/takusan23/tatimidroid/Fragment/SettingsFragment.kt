package io.github.takusan23.tatimidroid.Fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import io.github.takusan23.tatimidroid.Activity.KonoApp
import io.github.takusan23.tatimidroid.Activity.LicenceActivity
import io.github.takusan23.tatimidroid.AutoAdmissionService
import io.github.takusan23.tatimidroid.R
import java.io.File


class SettingsFragment : PreferenceFragmentCompat() {

    val privacy_policy = "https://github.com/takusan23/TatimiDroid/blob/master/privacy_policy.md"

    val fontFileOpenCode = 845

    lateinit var setting_font_category: PreferenceCategory

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //タイトル
        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.setting)

        val licence_preference = findPreference<Preference>("licence_preference")
        val konoapp_preference = findPreference<Preference>("konoapp_preference")
        val auto_admission_stop_preference =
            findPreference<Preference>("auto_admission_stop_preference")
        val konoapp_privacy = findPreference<Preference>("konoapp_privacy")

        // フォントかてごりー
        setting_font_category = findPreference<PreferenceCategory>("setting_font_category")!!
        // フォント指定
        val fontSelectPreference = findPreference<Preference>("setting_font_select")
        // フォント削除
        val font_select_reset = findPreference<Preference>("setting_font_reset")


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

        fontSelectPreference?.setOnPreferenceClickListener {
            // フォント選択
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.apply {
                type = "font/*"
            }
            startActivityForResult(intent, fontFileOpenCode)
            true
        }

        font_select_reset?.setOnPreferenceClickListener {
            // 選択したフォントリセット
            resetFont()
            // 選択中フォント更新
            setting_font_category.summary = getSelectFontName()
            true
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            darkmode_switch_preference?.isVisible = true
        }

        // 選択中フォントの名前を選択の説明文として入れるなど
        setting_font_category.summary = getSelectFontName()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fontFileOpenCode && resultCode == Activity.RESULT_OK && data!!.data != null) {
            // リクエストがフォント、選択が成功している場合
            // 削除
            resetFont()
            // コピー先フォルダー。ScopedStorageです。
            val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
            fontFolder.mkdir()
            // ふぁいるこぴー。
            val fileName = getFileName(data.data)
            val scopedStorageFontFile = File("${fontFolder}/$fileName")
            scopedStorageFontFile.createNewFile()
            scopedStorageFontFile.outputStream().use {
                context?.contentResolver?.openInputStream(data.data!!)?.copyTo(it)
            }
            // 選択中フォント更新
            setting_font_category.summary = getSelectFontName()
        }
    }

    // Uriからファイル名取得。取得失敗時は空の文字が
    fun getFileName(uri: Uri?): String {
        var fileName = ""
        if (uri == null) {
            return fileName
        }
        val cursor = context?.contentResolver?.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.moveToFirst()
        // 取得
        fileName = cursor?.getString(0) ?: ""
        cursor?.close()
        return fileName
    }

    // 拡張子取得
    fun getExtension(fileName: String): String {
        // 最初の「.」の位置を取得
        val dotPos = fileName.indexOf(".")
        return fileName.substring(dotPos)
    }


    private fun resetFont() {
        // 削除
        val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
        if (fontFolder.exists()) {
            // 存在するとき削除
            fontFolder.listFiles()?.forEach {
                it.delete()
            }
        }
    }

    fun startBrowser(link: String) {
        val i = Intent(Intent.ACTION_VIEW, link.toUri());
        startActivity(i);
    }

    // 選択中のフォントの名前
    fun getSelectFontName(): CharSequence? {
        val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
        if (fontFolder.listFiles()?.size ?: 0 <= 0) {
            return ""
        }
        return fontFolder.listFiles()?.get(0)?.name ?: ""
    }

}