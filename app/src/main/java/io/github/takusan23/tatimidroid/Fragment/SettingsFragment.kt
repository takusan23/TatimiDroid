package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import io.github.takusan23.tatimidroid.Activity.KonoApp
import io.github.takusan23.tatimidroid.Activity.LicenceActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import io.github.takusan23.tatimidroid.Tool.AppDataZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat


class SettingsFragment : PreferenceFragmentCompat() {

    /** プライバシーポリシー */
    private val PRIVACY_POLICY_URL = "https://github.com/takusan23/TatimiDroid/blob/master/privacy_policy.md"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val editText = EditText(context).apply {
            hint = "検索"
            setCompoundDrawablesRelativeWithIntrinsicBounds(context.getDrawable(R.drawable.ic_24px), null, null, null)
        }

        // 設定項目を配列にしまう
        val preferenceList = arrayListOf<Preference>().apply {
            // 再帰的に呼び出す
            fun getChildPreference(group: PreferenceGroup) {
                val preferenceCount = group.preferenceCount
                repeat(preferenceCount) { index ->
                    val preference = group.getPreference(index)
                    add(preference)
                    if (preference is PreferenceGroup) {
                        getChildPreference(preference)
                    }
                }
            }
            getChildPreference(preferenceScreen)
        }

        // テキスト変更を監視。
        editText.addTextChangedListener {
            val inputText = it?.toString() ?: return@addTextChangedListener
            // PreferenceCategory以外を非表示
            preferenceList.forEach { preference -> if (preference !is PreferenceGroup) preference.isVisible = false }
            // 部分一致した設定のみを表示する。なお、所属してるPreferenceCategoryが非表示だと出ない
            preferenceList.filter { preference -> preference.title?.contains(inputText) ?: false }.forEach { preference -> preference.isVisible = true }
        }
        (view as LinearLayout).addView(editText, 0) // 無理矢理感
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val licence_preference = findPreference<Preference>("licence_preference")
        val konoapp_preference = findPreference<Preference>("konoapp_preference")
        val auto_admission_stop_preference = findPreference<Preference>("auto_admission_stop_preference")
        val konoapp_privacy = findPreference<Preference>("konoapp_privacy")
        // フォント設定へ行くPreference
        val fontPreference = findPreference<Preference>("font_preference")
        // 開発者用項目
        val devSetting = findPreference<Preference>("dev_setting")
        // キャッシュ再生について
        val aboutCache = findPreference<Preference>("about_cache")
        // 一番最初に使った日
        val firstTimeDay = findPreference<Preference>("first_time_preference")
        // 端末内履歴消す
        val nicoHistoryDBClear = findPreference<Preference>("delete_history")
        // 端末内履歴書出し
        val backupHistoryDB = findPreference<Preference>("history_db_backup")

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
            context?.stopService(intent)
            true
        }
        konoapp_privacy?.setOnPreferenceClickListener {
            //プライバシーポリシー
            startBrowser(PRIVACY_POLICY_URL)
            true
        }
        nicoHistoryDBClear?.setOnPreferenceClickListener {
            // 端末内履歴を消すか
            val buttons = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.delete), R.drawable.ic_outline_delete_24px))
                add(DialogBottomSheet.DialogBottomSheetItem(getString(R.string.cancel), R.drawable.ic_arrow_back_black_24dp, Color.RED))
            }
            DialogBottomSheet(getString(R.string.delete_message), buttons) { i, bottomSheetDialogFragment ->
                lifecycleScope.launch(Dispatchers.Main) {
                    // 吹っ飛ばす（全削除）
                    withContext(Dispatchers.IO) {
                        NicoHistoryDBInit.getInstance(requireContext()).nicoHistoryDBDAO().deleteAll()
                    }
                    bottomSheetDialogFragment.dismiss()
                }
            }.show(getParentFragmentManager(), "delete")
            true
        }

        aboutCache?.setOnPreferenceClickListener {
            startBrowser("https://takusan23.github.io/Bibouroku/2020/04/08/たちみどろいどのキャッシュ機能について/")
            true
        }

        firstTimeDay?.setOnPreferenceClickListener { preference ->
            lifecycleScope.launch(Dispatchers.Main) {
                // 最初に使った日特定
                val list = withContext(Dispatchers.IO) {
                    NicoHistoryDBInit.getInstance(requireContext()).nicoHistoryDBDAO().getAll()
                }
                if (list.isEmpty()) return@launch // なければ落とす
                // 取り出す
                val history = list.first()
                val title = history.title
                val time = history.unixTime
                val id = history.serviceId
                val service = history.type
                // 今日からの何日前か。詳しくは NicoVideoInfoFragment#getDayCount() みて。ほぼ同じことしてる。
                val dayCalc = ((System.currentTimeMillis() / 1000) - time) / 60 / 60 / 24
                preference.summary = """
                最初に見たもの：$title ($id/ $service)
                一番最初に使った日：${toFormatTime(time * 1000)}
                今日から引くと：$dayCalc 日前
             """.trimIndent()
            }
            false
        }

        // Zipファイルへ
        backupHistoryDB?.setOnPreferenceClickListener {
            // データベースのファイルの場所
            val dbFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                File("${context?.dataDir?.path}/databases/NicoHistory.db") // getDataDir()がヌガー以降じゃないと使えない
            } else {
                File("/data/user/0/io.github.takusan23.tatimidroid/databases/NicoHistory.db") // ハードコート大丈夫か・・？
            }
            AppDataZip.createZipFile(requireActivity() as AppCompatActivity, dbFile)
            false
        }

    }

    /** UnixTime -> わかりやすい形式に */
    private fun toFormatTime(time: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日HH時mm分ss秒")
        return simpleDateFormat.format(time)
    }


    private fun startBrowser(link: String) {
        val i = Intent(Intent.ACTION_VIEW, link.toUri());
        startActivity(i);
    }

}