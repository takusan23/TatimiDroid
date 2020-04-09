package io.github.takusan23.tatimidroid.SQLiteHelper

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.AutoAdmissionService
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.ProgramData
import io.github.takusan23.tatimidroid.R

/**
 * 予約枠自動入場DBに書き込む
 * */
class AutoAdmissionDBUtil(context: Context) {
    var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite = AutoAdmissionSQLiteSQLite(context)
    var sqLiteDatabase: SQLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase

    init {
        autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
    }

    /**
     * 予約枠自動入場に追加
     *  @param type tatimidroid_app か nicolive_app
     * */
    fun addAutoAdmissionDB(app: String, context: Context?, programData: ProgramData, view: View) {
        val dbBeginTime = findAutoAdmissionDB(programData.programId)
        val nicoLiveHTML =
            NicoLiveHTML()
        if (dbBeginTime == -1L) {
            // DB未登録
            //書き込む
            val contentValues = ContentValues()
            contentValues.put("name", programData.title)
            contentValues.put("liveid", programData.programId)
            contentValues.put("start", (programData.beginAt.toLong() / 1000L).toString())
            contentValues.put("app", "nicolive_app")
            contentValues.put("description", "")
            sqLiteDatabase.insert("auto_admission", null, contentValues)
            // Toastに表示させるアプリ名
            val appName = if (app == "tatimidroid_app") {
                context?.getString(R.string.app_name)
            } else {
                context?.getString(R.string.nicolive_app)
            }
            Toast.makeText(
                context,
                "${context?.getString(R.string.added)}\n${programData.title} ${nicoLiveHTML.iso8601ToFormat(programData.beginAt.toLong())} (${appName})",
                Toast.LENGTH_SHORT
            ).show()
            //Service再起動
            val intent = Intent(context, AutoAdmissionService::class.java)
            context?.stopService(intent)
            context?.startService(intent)
        } else {
            // DB登録済み
            Snackbar.make(view, "${context?.getString(R.string.already_added)}（${nicoLiveHTML.iso8601ToFormat(dbBeginTime)}）\nお楽しみに！", Snackbar.LENGTH_SHORT)
                .setAction(R.string.delete) {
                    // 削除する
                    deleteAutoAdmissionDB(programData.programId, context)
                    Toast.makeText(
                        context,
                        R.string.remove_auto_admission,
                        Toast.LENGTH_SHORT
                    ).show()
                }.show()
        }
    }

    /**
     *  予約枠自動入場DBから番組IDを検索してくる
     *  @return 予約済みなら開始時間（UnixTime）、なければ -1
     * */
    fun findAutoAdmissionDB(liveId: String): Long {
        val cursor =
            sqLiteDatabase.query(AutoAdmissionSQLiteSQLite.TABLE_NAME, arrayOf("start"), "liveid=?", arrayOf(liveId), null, null, null)
        cursor.moveToFirst()
        var beginTime = -1L
        for (i in 0 until cursor.count) {
            beginTime = cursor.getString(0).toLong()
            cursor.moveToNext()
        }
        cursor.close()
        return beginTime
    }

    /**
     * 予約枠自動入場から番組を消す
     * */
    fun deleteAutoAdmissionDB(liveId: String, context: Context?) {
        sqLiteDatabase.delete(AutoAdmissionSQLiteSQLite.TABLE_NAME, "liveid=?", arrayOf(liveId))
        //Service再起動
        val intent = Intent(context, AutoAdmissionService::class.java)
        context?.stopService(intent)
        context?.startService(intent)
    }


}