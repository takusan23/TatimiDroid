package io.github.takusan23.tatimidroid

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


//適当なLiveId入れてね
class NicoLogin(val context: Context, val liveId: String) {
    val pref_setting: SharedPreferences

    init {
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        //そもそもSharedPreferenceにセッション情報なければ強制ログイン
        val user_session = pref_setting.getString("user_session", "") ?: ""
        if (user_session.isNotEmpty()) {
            //適当にAPI叩いて認証情報エラーだったら再ログインする
            val request = Request.Builder()
                .url("https://live2.nicovideo.jp/watch/${liveId}/programinfo")
                .header("Cookie", "user_session=${user_session}")
                .get()
                .build()
            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    //？
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        //エラーなのでユーザーセッション取得
                        getUserSession()
                    } else {
                        //そもそも番組が終わってる可能性があるのでチェック
                        val response_string = response.body?.string()
                        val jsonObject = JSONObject(response_string)
                        val status = jsonObject.getJSONObject("data").getString("status")
                        if (!status.contains("onAir")) {
                            (context as AppCompatActivity).runOnUiThread {
                                //おわってる　か　まだ
                                context.finish()
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.program_end),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            })
        } else {
            //初回
            getUserSession()
        }
    }

    companion object {
        /**
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
                        Toast.makeText(
                            context,
                            "${context?.getString(R.string.error)}\n${status}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                httpConn?.disconnect()
            }
        }
    }

    fun getUserSession() {
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

            for (cookie in httpConn.headerFields.get("Set-Cookie")!!) {
                //user_sessionだけほしい！！！
                if (cookie.contains("user_session") && !cookie.contains("deleted") && !cookie.contains(
                        "secure"
                    )
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
                }
            }

            if (status == HttpURLConnection.HTTP_OK) {
                // レスポンスを受け取る処理等
            } else {
                //失敗
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            httpConn?.disconnect()
        }
    }

}