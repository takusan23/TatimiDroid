package io.github.takusan23.tatimidroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import java.util.*
import kotlin.concurrent.timerTask

class AutoAdmissionService : Service() {

    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()

        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //データベース読み込み
        //初期化したか
        if (!this@AutoAdmissionService::autoAdmissionSQLiteSQLite.isInitialized) {
            autoAdmissionSQLiteSQLite =
                AutoAdmissionSQLiteSQLite(applicationContext)
            sqLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase
            autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
        }
        //通知
        showForegroundNotification()
    }

    fun registerAutoAdmission(
        liveid: String,
        programName: String,
        app: String,
        calendar: Calendar
    ) {

        Timer().schedule(timerTask {
            if (app.contains("tatimidroid_app")) {
                //たちみどろいど
                //  //設定変更。予約枠自動入場はコメント投稿モードのみ
                //  val pref_setting = PreferenceManager.getDefaultSharedPreferences(applicationContext)
                //  val editor = pref_setting.edit()
                //  //設定変更
                //  editor.putBoolean("setting_watching_mode", true)
                //  editor.putBoolean("setting_nicocas_mode", false)
                //  editor.apply()
                //ここにIntent起動書く。
                val intent = Intent(applicationContext, CommentActivity::class.java)
                //これいる
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                //番組ID
                intent.putExtra("liveId", liveid)
                //視聴モード
                intent.putExtra("watch_mode", "comment_post")
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
            //Service再起動
            stopSelf()
            startService(Intent(applicationContext, AutoAdmissionService::class.java))
            //該当の番組をデータベースから消す
            sqLiteDatabase.delete("auto_admission", "liveid=?", arrayOf(liveid))

        }, calendar.time)


        /*　予約枠自動入場1分前に通知を送信する　
        *   Android Q 以降でバックグラウンドからActivityを起動しようとすとブロックされて起動できないので、通知にアプリ起動アクションを置くことにした（通知からはおｋらしい）。
        *   ちなみにアプリが起動していればバックグラウンドからきどうできるので履歴から消さなければおっけー？
        * */

        val notificationCalender = calendar
        notificationCalender.add(Calendar.MINUTE, -1)//1分前に

        Timer().schedule(timerTask {
            //通知表示
            showAutoAdmissionNotification(liveid, programName, app, notificationCalender)
        }, notificationCalender.time)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)

    }

    fun showAutoAdmissionNotification(
        liveid: String,
        programName: String,
        app: String,
        calendar: Calendar
    ) {
        notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //OreoかNougatか
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Oreo
            val notificationChannel = NotificationChannel(
                "auto_admission_one_minute_notification",
                getString(R.string.auto_admission_one_minute_notification),
                NotificationManager.IMPORTANCE_HIGH //重要度上げとく
            )
            //通知チャンネル登録
            if (notificationManager.getNotificationChannel("auto_admission_one_minute_notification") == null) {
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val time = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
            val programInfo = "${programName} - ${liveid}\n${time}"

            //通知作成
            var notification = NotificationCompat.Builder(
                applicationContext,
                "auto_admission_one_minute_notification"
            )
                .setSmallIcon(R.drawable.ic_auto_admission_start_icon)
                .setContentTitle(getString(R.string.auto_admission_one_minute_notification_description))
                .setStyle(NotificationCompat.BigTextStyle().bigText(programInfo))
                .setVibrate(longArrayOf(100, 0, 100, 0))    //バイブ

            //Action付き通知作成
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(applicationContext, MainActivity::class.java)
                notification.setContentTitle(getString(R.string.auto_admission_one_minute_notification))
                notification.setContentText(getString(R.string.auto_admission_one_minute_notification_description_androidq))
                notification.addAction(
                    R.drawable.ic_auto_admission_start_icon,
                    getString(R.string.lunch_app),
                    PendingIntent.getActivity(
                        applicationContext, 45, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }


            //表示
            notificationManager.notify(2525, notification.build())
        } else {
            //Nougat
            val time = "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
            val programInfo = "${programName} - ${liveid}\n${time}"
            //通知作成
            val notification = NotificationCompat.Builder(applicationContext)
                .setContentTitle(getString(R.string.auto_admission_one_minute_notification))
                .setSmallIcon(R.drawable.ic_icon_large)
                .setContentTitle(getString(R.string.auto_admission_one_minute_notification_description))
                .setStyle(NotificationCompat.BigTextStyle().bigText(programInfo))
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setVibrate(longArrayOf(100, 0, 100, 0))    //バイブ
                .build()
            //表示
            notificationManager.notify(2525, notification)
        }
    }

    fun showForegroundNotification() {
        var programList = ""
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
            if ((Calendar.getInstance().timeInMillis / 1000L) < start.toLong()) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = start.toLong() * 1000L
                //登録
                registerAutoAdmission(liveId, programName, app, calendar)
                //通知に表示
                val month = calendar.get(Calendar.MONTH)
                val date = calendar.get(Calendar.DATE)
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)

                val program = "${programName}- ${liveId} (${month + 1}/$date $hour:$minute)"
                programList = programList + "\n" + program
            }
            cursor.moveToNext()
        }
        cursor.close()

        //予約無い時
        if (programList.isEmpty()) {
            programList = getString(R.string.auto_admission_empty)
        }

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
            val notification =
                NotificationCompat.Builder(applicationContext, "auto_admission_notification")
                    .setContentTitle(getString(R.string.auto_admission_notification))
                    .setSmallIcon(R.drawable.ic_icon_large)
                    .setContentTitle(getString(R.string.auto_admission_notification_message))
                    .setStyle(NotificationCompat.BigTextStyle().bigText(programList))
                    .build()
            //表示
            startForeground(1, notification)
        }
    }


    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
