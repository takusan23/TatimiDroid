package io.github.takusan23.tatimidroid.NicoAPI

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ニコニコにログインする関数。
 * 高階関数とコルーチン版どちらもあります。
 * */
class NicoLogin {
    companion object {
        /**
         * UIスレッドでは実行できません。
         * ニコ動へログインする関数。ただし初回利用時は利用できません。（SharedPreferenceに値が保存されている必要があるため。）
         * @param context SharedPreferenceに保存するときなどに必要。
         * @param loginSuccessful ログインに成功したら呼ばれます。（注意：UIスレッドではありません。）
         * */
        fun login(context: Context?, loginSuccessful: () -> Unit) {
            val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
            val mail = pref_setting.getString("mail", "")
            val pass = pref_setting.getString("password", "")
            // 使用するサーバーのURLに合わせる
            val urlSt = "https://secure.nicovideo.jp/secure/login?site=niconico"

            var httpConn: HttpURLConnection? = null

            val postData = "mail_tel=$mail&password=$pass"

            try {
                // URL設定
                val url = URL(urlSt)

                // HttpURLConnection
                httpConn = url.openConnection() as HttpURLConnection

                // request POST
                httpConn.requestMethod = "POST"

                // no Redirects
                httpConn.instanceFollowRedirects = false

                // データを書き込む
                httpConn.doOutput = true

                // 時間制限
                httpConn.readTimeout = 10000
                httpConn.connectTimeout = 20000

                //ユーザーエージェント
                httpConn.setRequestProperty("User-Agent", "TatimiDroid;@takusan_23")

                // 接続
                httpConn.connect()

                try {
                    httpConn.outputStream.use { outStream ->
                        outStream.write(postData.toByteArray(StandardCharsets.UTF_8))
                        outStream.flush()
                    }
                } catch (e: IOException) {
                    // POST送信エラー
                    e.printStackTrace()
                }
                // POSTデータ送信処理
                val status = httpConn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP) {
                    // レスポンスを受け取る処理等
                    for (cookie in httpConn.headerFields.get("Set-Cookie")!!) {
                        //user_sessionだけほしい！！！
                        if (cookie.contains("user_session") &&
                            !cookie.contains("deleted") &&
                            !cookie.contains("secure")
                        ) {
                            //邪魔なのを取る
                            var user_session = cookie.replace("user_session=", "")
                            //uset_settionは文字数86なので切り取る
                            user_session = user_session.substring(0, 86)
                            //保存する
                            val editor = pref_setting.edit()
                            editor.putString("user_session", user_session)
                            //めあど、ぱすわーども保存する
                            editor.putString("mail", mail)
                            editor.putString("password", pass)
                            editor.apply()
                            // ログイン成功なので関数を呼ぶ
                            loginSuccessful()
                        }
                    }
                } else {
                    //失敗
                    val mHandler = Handler(Looper.getMainLooper())
                    mHandler.post {
                        // 失敗メッセージ
                        Toast.makeText(context, "${context?.getString(R.string.error)}\n${status}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                httpConn?.disconnect()
            }
        }

        /**
         * ニコニコにログインする関数。コルーチン版
         * ただし初回利用時は利用できません。（SharedPreferenceに値が保存されている必要があるため。）
         * 注意：これ取得したら必ずuser_sessionをPreferenceから再取得してください。
         * @param context SharedPreferenceを使うため
         * @return ユーザーセッションを返します。
         * */
        suspend fun loginCoroutine(context: Context?) = suspendCoroutine<String> { suspendCoroutine ->
            login(context) {
                val userSession = PreferenceManager.getDefaultSharedPreferences(context).getString("user_session", "") ?: ""
                suspendCoroutine.resume(userSession)
            }
        }
    }
}