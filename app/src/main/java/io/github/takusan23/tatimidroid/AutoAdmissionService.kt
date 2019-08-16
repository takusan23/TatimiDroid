package io.github.takusan23.tatimidroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import java.util.*
import kotlin.concurrent.timerTask

class AutoAdmissionService : Service() {

    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase


    override fun onCreate() {
        super.onCreate()

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //Oreo以降は通知チャンネルいる
        //Oreo以降はサービス実行中です通知を出す必要がある。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "auto_admission_notification",
                getString(R.string.auto_admission_notification),
                NotificationManager.IMPORTANCE_LOW
            )
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel("auto_admission_notification") == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            //通知作成
            val notification = NotificationCompat.Builder(applicationContext, "auto_admission_notification")
                .setContentTitle(getString(R.string.auto_admission_notification))
                .setSmallIcon(R.drawable.ic_icon_large)
                .setContentTitle(getString(R.string.auto_admission_notification_message))
                .build()
            //表示
            startForeground(1, notification)
        }

        //データベース読み込み
        //初期化したか
        if (!this@AutoAdmissionService::autoAdmissionSQLiteSQLite.isInitialized) {
            autoAdmissionSQLiteSQLite = AutoAdmissionSQLiteSQLite(applicationContext)
            sqLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase
            autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
        }

        //SQLite読み出し
        val cursor = sqLiteDatabase.query(
            "auto_admission",
            arrayOf("name", "liveid", "start", "app"),
            null, null, null, null, null
        )
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {

            val programName = cursor.getString(0)
            val liveId = cursor.getString(1)
            val start = cursor.getString(2)
            val app = cursor.getString(3)

            //未来の番組だけ読み込む（終わってるのは読み込まない）
            if (Calendar.getInstance().timeInMillis < start.toLong()) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = start.toLong()
                //登録
                registerAutoAdmission(liveId, programName, app, calendar)
            }
            cursor.moveToNext()
        }
        cursor.close()
    }

    fun registerAutoAdmission(liveid: String, programName: String, app: String, calendar: Calendar) {

        Timer().schedule(timerTask {
            if (app.contains("tatimidroid_app")) {
                //たちみどろいど
                //設定変更。予約枠自動入場はコメント投稿モードのみ
                val pref_setting = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                val editor = pref_setting.edit()
                //設定変更
                editor.putBoolean("setting_watching_mode", true)
                editor.putBoolean("setting_nicocas_mode", false)
                editor.apply()
                //ここにIntent起動書く。
                val intent = Intent(applicationContext, CommentActivity::class.java)
                //これいる
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                //番組ID
                intent.putExtra("liveId", liveid)
                //Activity起動
                startActivity(intent)
            } else {
                //ニコニコ生放送アプリ
                //Intentとばす
                val openNicoLiveApp =
                    "https://sp.live.nicovideo.jp/watch/${liveid}"
                val intent = Intent(Intent.ACTION_VIEW, openNicoLiveApp.toUri())
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }

            //該当の番組をデータベースから消す
            sqLiteDatabase.delete("auto_admission", "liveid=?", arrayOf(liveid))

        }, calendar.time)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
