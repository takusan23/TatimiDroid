package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_login.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class LoginFragment : Fragment() {
    lateinit var pref_setting: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //タイトル
        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.login)


        //保存していたら取得
        fragment_login_mail_inputedittext.setText(pref_setting.getString("mail", ""))
        fragment_login_password_inputedittext.setText(pref_setting.getString("password", ""))

        //おしたとき
        fragment_login_button.setOnClickListener {
            //ログイン
            Thread {
                login()
            }.start()
        }

    }

    /*Cookie取得 数時間戦ったけどOkHttpじゃできなかった；；*/
    fun login() {
        val mail = fragment_login_mail_inputedittext.text.toString()
        val pass = fragment_login_password_inputedittext.text.toString()

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

            var user_session = ""
            for (cookie in httpConn.headerFields.get("Set-Cookie")!!) {
                //user_sessionだけほしい！！！
                if (cookie.contains("user_session") && !cookie.contains("deleted") && !cookie.contains("secure")) {
                    //邪魔なのを取る
                    user_session = cookie.replace("user_session=", "")
                    //uset_settionは文字数86なので切り取る
                    user_session = user_session.substring(0, 86)
                    //保存する
                    val editor = pref_setting.edit()
                    editor.putString("user_session", user_session)
                    editor.apply()
                }
            }


            if (status == HttpURLConnection.HTTP_OK) {
                // レスポンスを受け取る処理等
            } else {
                //３０２を返すのが正常なので
                //ユーザーセッション取れた？
                if (user_session.isNotEmpty()) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            activity,
                            getString(R.string.successful) + "\n" + httpConn.responseCode,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    val editor = pref_setting.edit()
                    //めあど、ぱすわーども保存する
                    editor.putString("mail", mail)
                    editor.putString("password", pass)
                    editor.apply()
                } else {
                    //取れなかった。見直してみて。
                    activity?.runOnUiThread {
                        Toast.makeText(
                            activity,
                            getString(R.string.not_get_user_session) + "\n" + httpConn.responseCode,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            httpConn?.disconnect()
        }
    }

}