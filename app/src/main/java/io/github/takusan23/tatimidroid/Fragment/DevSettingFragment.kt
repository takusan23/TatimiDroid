package io.github.takusan23.tatimidroid.Fragment

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.CommentGetService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

class DevSettingFragment : PreferenceFragmentCompat() {

    // 履歴DBのSAF。512810
    val CREATE_BACKUP_FILE_RESULT_CODE = 512
    val SELECT_RESTORE_FILE_REQUEST_CODE = 810

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.dev_preference, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // コメントベンチマーク
        initCommentBenchmark()
    }

    private fun initCommentBenchmark() {
        val commentGetButton =
            preferenceManager.findPreference<Preference>("dev_setting_get_comment")
        val commentValue =
            preferenceManager.sharedPreferences.getString("dev_setting_get_comment_limit", "0")
                ?.toInt()
        val commentServiceFinishButton =
            preferenceManager.findPreference<Preference>("dev_setting_get_comment_service_finish")

        val intent = Intent(context, CommentGetService::class.java)

        // キャッシュの中から選ぶ
        val nicoVideoCache = NicoVideoCache(context)
        var idList = arrayListOf<String>()
        var titleList = arrayListOf<String>()
        runBlocking {
            // 読み込んでIDとタイトルの文字が入った配列に変換
            val cacheList = nicoVideoCache.loadCache()
            titleList = ArrayList(cacheList.map { nicoVideoData -> nicoVideoData.title })
            idList = ArrayList(cacheList.map { nicoVideoData -> nicoVideoData.videoId })
        }
        // だいあろぐ
        val buttons = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>()
        titleList.forEach {
            val items = DialogBottomSheet.DialogBottomSheetItem(it)
            buttons.add(items)
        }
        // サービス起動
        commentGetButton?.setOnPreferenceClickListener {
            // 選択画面出す
            DialogBottomSheet("キャッシュ取得の中から表示してます", buttons) { i, bottomSheetDialogFragment ->
                intent.putExtra("videoId", idList[i])
                context?.stopService(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(intent)
                } else {
                    context?.startService(intent)
                }
            }.show(parentFragmentManager, "select")
            false
        }

        // サービス終了
        commentServiceFinishButton?.setOnPreferenceClickListener {
            context?.stopService(intent)
            false
        }

        // 履歴DBバックアップと復元
        initHistoryDB()

    }

    private fun initHistoryDB() {
        // 履歴DB
        val historyDBBackup = findPreference<Preference>("dev_setting_history_db_backup")
        val historyDBRestore = findPreference<Preference>("dev_setting_history_db_restore")
        historyDBBackup?.setOnPreferenceClickListener {
            // コピーするファイル作成
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_TITLE, "NicoHistory.db")
            }
            startActivityForResult(intent, CREATE_BACKUP_FILE_RESULT_CODE)
            false
        }
        historyDBRestore?.setOnPreferenceClickListener {
            // 復元するDBのファイルもらう
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
            }
            startActivityForResult(intent, SELECT_RESTORE_FILE_REQUEST_CODE)
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            // データベースのファイルの場所
            val dbFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                File("${context?.dataDir?.path}/databases/NicoHistory.db") // getDataDir()がヌガー以降じゃないと使えない
            } else {
                File("/data/user/0/io.github.takusan23.tatimidroid/databases/NicoHistory.db") // ハードコート大丈夫か・・？
            }
            when (requestCode) {
                CREATE_BACKUP_FILE_RESULT_CODE -> {
                    // コピー作成
                    data?.data?.let { uri ->
                        if (!dbFile.exists()) {
                            showToast("ないよ")
                            return // なければ終了
                        }
                        // 書き込む
                        val dbFileByteArray = dbFile.readBytes()
                        context?.contentResolver?.openOutputStream(uri)?.write(dbFileByteArray)
                        showToast("バックアップが生成されました。")
                    }
                }
                SELECT_RESTORE_FILE_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        // データベースなければ作る
                        if (!dbFile.exists()) {
                            dbFile.createNewFile()
                        }
                        // 取り出す
                        context?.contentResolver?.openInputStream(uri)?.readBytes()?.let { byteArray ->
                            dbFile.writeBytes(byteArray)
                            showToast("復元しました。")
                        }
                    }
                }
            }
        }
    }

    /** トースト表示 */
    fun showToast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

}