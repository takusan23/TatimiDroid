package io.github.takusan23.tatimidroid.NicoLive.Activity

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.NicoLive.CommentFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/*
* わざわざFloatingなコメントビューあーを作るためにクラスを作ったのかって？
* AndroidManifestに属性を設定しないといけないんですけどそれをCommentActivityで適用して
* BubblesAPIを使おうとするとアプリが履歴で大増殖します。
* 多分こいつ（属性）のせい。複数インスタンスを許可するには必要らしい。→android:documentLaunchMode
* というわけでわざわざ作ったわけです。
* */
class FloatingCommentViewer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ダークモード対応
        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_floating_comment_viewer)

        val liveId = intent.getStringExtra("liveId")
        val watchMode = intent.getStringExtra("watch_mode")

        //Fragment設置
        val trans = supportFragmentManager.beginTransaction()
        val commentFragment = CommentFragment()
        //LiveID詰める
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        bundle.putString("watch_mode", watchMode)
        commentFragment.arguments = bundle
        trans.replace(R.id.activity_floating_comment_viewer_linearlayout, commentFragment, liveId)
        trans.commit()
    }

    companion object {

        /**
         * フローティングコメントビューワー（アーかもしれない）を起動する関数。Android 10以降で利用可能です。
         * あんまり使わなそう
         * @param context こんてきすと
         * @param liveId 生放送ID
         * @param thumbUrl サムネイルのURL
         * @param title 番組たいとる
         * @param watchMode 視聴モード。以下のどれか。
         *        comment_viewer | コメントビューアー
         *        comment_post   | コメント投稿モード
         *        nicocas        | ニコキャス式コメント投稿モード
         * */
        fun showBubbles(context: Context?, liveId: String, watchMode: String?, title: String, thumbUrl: String) {
            // Android Q以降で利用可能
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                GlobalScope.launch(Dispatchers.Main) {

                    val intent = Intent(context, FloatingCommentViewer::class.java)
                    intent.putExtra("liveId", liveId)
                    intent.putExtra("watch_mode", watchMode)

                    // アイコン取得など
                    val filePath = getThumb(context, thumbUrl, liveId)
                    // 一旦Bitmapに変換したあと、Iconに変換するとうまくいく。
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    val icon = Icon.createWithAdaptiveBitmap(bitmap)

                    val bubbleIntent = PendingIntent.getActivity(context, 25, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                    // 通知作成？
                    val bubbleData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Notification.BubbleMetadata.Builder(bubbleIntent, icon)
                            .setDesiredHeight(1200)
                            .setIntent(bubbleIntent)
                            .build()
                    } else {
                        Notification.BubbleMetadata.Builder()
                            .setDesiredHeight(1200)
                            .setIcon(icon)
                            .setIntent(bubbleIntent)
                            .build()
                    }
                    val supplierPerson = Person.Builder().setName(context?.getString(R.string.floating_comment_viewer)).setIcon(icon).build()

                    // 通知送信
                    val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    // 通知チャンネル作成
                    val notificationId = "floating_comment_viewer"
                    if (notificationManager.getNotificationChannel(notificationId) == null) {
                        // 作成
                        val notificationChannel = NotificationChannel(notificationId, context?.getString(R.string.floating_comment_viewer), NotificationManager.IMPORTANCE_DEFAULT)
                        notificationManager.createNotificationChannel(notificationChannel)
                    }
                    // 通知作成
                    val notification = Notification.Builder(context, notificationId)
                        .setContentText(context?.getString(R.string.floating_comment_viewer_description))
                        .setContentTitle(context?.getString(R.string.floating_comment_viewer))
                        .setSmallIcon(R.drawable.ic_library_books_24px)
                        .setBubbleMetadata(bubbleData)
                        .addPerson(supplierPerson)
                        .setStyle(Notification.MessagingStyle(supplierPerson).apply {
                            conversationTitle = title
                        })
                        .build()
                    // 送信
                    notificationManager.notify(5, notification)
                }
            } else {
                // Android Pieなので..
                Toast.makeText(context, context?.getString(R.string.floating_comment_viewer_version), Toast.LENGTH_SHORT).show()
            }
        }

        /** サムネイル取得してキャッシュ領域へ保存する。suspend関数なので取得終わるまで一時停止する。 */
        private suspend fun getThumb(context: Context?, thumbUrl: String, liveId: String) = GlobalScope.async {
            val request = Request.Builder().apply {
                url(thumbUrl)
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = try {
                okHttpClient.newCall(request).execute()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
            val file = File("${context?.externalCacheDir?.path}/$liveId.png")
            file.createNewFile()
            try {
                file.writeBytes(response?.body?.bytes() ?: return@async null)
                return@async file.path
            } catch (e: IOException) {
                e.printStackTrace()
                return@async null
            }
        }.await()

    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

}
