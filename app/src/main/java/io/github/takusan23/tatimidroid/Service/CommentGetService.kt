package io.github.takusan23.tatimidroid.Service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoCommentXML
import io.github.takusan23.tatimidroid.NicoAPI.XMLCommentJSON
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

class CommentGetService : Service() {

    val NOTIFICATION_CHANNEL = "comment_get_notification"

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        showNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 動画ID
        val videoId = intent?.getStringExtra("videoId") ?: "sm157"

        // 最大取得数
        val prefSetting = PreferenceManager.getDefaultSharedPreferences(this)
        val commentGetLimit =
            prefSetting.getString("dev_setting_get_comment_limit", "0")?.toInt() ?: 1000
        val userSession = prefSetting.getString("user_session", "") ?: ""

        // 何回リクエストするか
        val requestCount = commentGetLimit / 1000

        // XML形式でコメントをリクエストするクラス
        val xmlComment = NicoCommentXML()
        // XML形式のコメントをパースするクラス
        val xmlCommentJSON = XMLCommentJSON(this)
        // コメントつなげる
        var totalComment = ""
        GlobalScope.launch {

            // getflv（ふるい）
            val getFlv = xmlComment.getFlv(userSession, videoId).await()
            val getFlvResponseString = getFlv.body?.string() ?: return@launch

            // コメント取得に必要な値
            val threadId = xmlComment.getThreadId(getFlvResponseString)
            val userId = xmlComment.getUserId(getFlvResponseString)

            // 一回目のコメント取得
            val comment =
                xmlComment.getXMLComment(threadId, userId, null, null).await() ?: return@launch
            totalComment += comment

            // XML -> ArrayList
            val commentList = xmlCommentJSON.xmlToArrayList(comment).await()

            // 二回目以降（ここから過去コメント取得）
            val wayBackKey =
                xmlComment.getWayBackKey(threadId, userSession).await() ?: return@launch
            // 件数に達するまでリクエスト
            while (commentList.size < commentGetLimit) {

                // 一応遅延させる
                delay(1000)
                // 一番古い時間でコメント取得
                val time = commentList.minBy { commentJSONParse -> commentJSONParse.date.toLong() }
                val responseComment =
                    xmlComment.getXMLComment(threadId, userId, wayBackKey, time?.date).await()
                        ?: return@launch
                // パース
                xmlCommentJSON.xmlToArrayList(responseComment).await().forEach {
                    commentList.add(it)
                }
                // 通知出す
                showNotification("取得済み：${commentList.size} / 残り：$commentGetLimit", commentList.size, commentGetLimit)

            }

            // ほぞんする
            val jsonArray = xmlCommentJSON.CommentJSONParseArrayToJSONString(commentList).await()
            // 保存。
            val jsonFile =
                File("${getExternalFilesDir(null)}/cache/$videoId/${videoId}_comment.json")
            jsonFile.writeText(jsonArray.toString())
            // おわり！閉廷！
            stopSelf()
        }
        return START_NOT_STICKY
    }

    // 通知出す
    private fun showNotification(contentText: String = "", progress: Int = -1, max: Int = -1) {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL) == null) {
                val notificationChannel =
                    NotificationChannel(NOTIFICATION_CHANNEL, "コメント取得サービス", NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(notificationChannel)
            }
            Notification.Builder(this, NOTIFICATION_CHANNEL)
        } else {
            Notification.Builder(this)
        }
        notification.apply {
            setContentTitle("コメント取得サービス実行中")
            setContentText(contentText)
            // 進捗
            if (progress != -1 && max != -1) {
                setProgress(max, progress, false)
            }
            setSmallIcon(R.drawable.ic_outline_comment_24px)
        }
        startForeground(321, notification.build())
    }

}