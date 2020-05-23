package io.github.takusan23.tatimidroid.Fragment

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_auto_admission.*
import java.util.*

class AutoAdmissionBottomFragment : BottomSheetDialogFragment() {

    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_auto_admission, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初期化済みか
        if (!this@AutoAdmissionBottomFragment::autoAdmissionSQLiteSQLite.isInitialized) {
            //初期化
            autoAdmissionSQLiteSQLite =
                AutoAdmissionSQLiteSQLite(context!!)
            sqLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase
            //読み込む速度が上がる機能？データベースファイル以外の謎ファイルが生成されるので無効化。
            autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
        }

        //取得
        val programName = arguments?.getString("program")
        val liveId = arguments?.getString("liveId")
        val start = arguments?.getString("start")
        val description = arguments?.getString("description")

        //ニコニコ生放送アプリで起動する
        auto_admission_nicocas.setOnClickListener {
            //書き込む
            val contentValues = ContentValues()
            contentValues.put("name", programName)
            contentValues.put("liveid", liveId)
            contentValues.put("start", start)
            contentValues.put("app", "nicolive_app")
            contentValues.put("description", description)
            sqLiteDatabase.insert("auto_admission", null, contentValues)
            Toast.makeText(
                context,
                "${getString(R.string.added)}\n${programName} ${parseTime(start.toString())} (${getString(R.string.nicolive_app)})",
                Toast.LENGTH_SHORT
            ).show()
            //Service再起動
            val intent = Intent(context, AutoAdmissionService::class.java)
            context?.stopService(intent)
            context?.startService(intent)
            this@AutoAdmissionBottomFragment.dismiss()
        }

        //たちみどろいどで起動する
        auto_admission_tatimi_droid.setOnClickListener {
            //書き込む
            val contentValues = ContentValues()
            contentValues.put("name", programName)
            contentValues.put("liveid", liveId)
            contentValues.put("start", start)
            contentValues.put("app", "tatimidroid_app")
            contentValues.put("description", description)
            sqLiteDatabase.insert("auto_admission", null, contentValues)
            Toast.makeText(
                context,
                "${getString(R.string.added)}\n${programName} ${parseTime(start.toString())} (${getString(R.string.app_name)})",
                Toast.LENGTH_SHORT
            ).show()
            //Service再起動
            val intent = Intent(context, AutoAdmissionService::class.java)
            context?.stopService(intent)
            context?.startService(intent)
            this@AutoAdmissionBottomFragment.dismiss()
        }

        auto_admission_cancel.setOnClickListener {
            this@AutoAdmissionBottomFragment.dismiss()
        }
    }

    fun parseTime(string: String): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = string.toLong()
        val month = calendar.get(Calendar.MONTH)
        val date = calendar.get(Calendar.DATE)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return "${month + 1}/$date $hour:$minute"
    }

}