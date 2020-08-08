package io.github.takusan23.tatimidroid.NicoAPI

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ニコニコにログインする関数。
 * コルーチンで使ってください
 *
 * ```kotlin
 * //ログイン
 * lifecycleScope.launch(Dispatchers.Main) {
 *     // ログインAPIを叩く
 *     val userSession = withContext(Dispatchers.Default) {
 *         NicoLogin.nicoLoginCoroutine(mail, pass)
 *     }
 * }
 * ```
 * */
class NicoLogin {
    companion object {

        /**
         * ニコニコにログインする関数。こちらはPreferenceの値を使うので二回目以降から利用できます（だからContextが引数で必要だったのですね！）
         * ただし初回利用時は利用できません。（SharedPreferenceに値が保存されている必要があるため。）
         * 取得に成功したらSharedPreferenceに保存します。ので自分で保存処理を書かなくていいよ
         * @param context SharedPreferenceを使うため
         * @return ユーザーセッションを返します。失敗時は空文字（どうにかしたい）
         * */
        suspend fun reNicoLogin(context: Context?): String = withContext(Dispatchers.Default) {
            // メアドを取り出す
            val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
            val mail = prefSetting.getString("mail", "")
            val pass = prefSetting.getString("password", "")
            if (mail == null && pass == null) {
                withContext(Dispatchers.Main) {
                    // メアド設定してね
                    Toast.makeText(context, R.string.mail_pass_error, Toast.LENGTH_SHORT).show()
                }
                return@withContext ""
            }
            // ログインしてログイン情報保存
            val userSession = nicoLoginCoroutine(mail!!, pass!!) ?: return@withContext ""
            prefSetting.edit {
                putString("user_session", userSession)
            }
            return@withContext userSession
        }

        /**
         * ニコニコにログインする関数。
         * OkHttpで書き直した。リダイレクト禁止すればできます。Set-Cookieが複数あるので注意
         * @param mail メアド
         * @param pass パスワード
         * @return ユーザーセッション。ログイン情報
         * */
        suspend fun nicoLoginCoroutine(mail: String, pass: String): String? = withContext(Dispatchers.Default) {
            val url = "https://secure.nicovideo.jp/secure/login?site=niconico"
            val postData = "mail_tel=$mail&password=$pass"
            val request = Request.Builder().apply {
                url(url)
                addHeader("User-Agent", "TatimiDroid;@takusan_23")
                post(postData.toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())) // 送信するデータ。
            }.build()
            // リダイレクト禁止（そうしないとステータスコードが302にならない）
            val okHttpClient = OkHttpClient().newBuilder().apply {
                followRedirects(false)
                followSslRedirects(false)
            }.build()
            val response = okHttpClient.newCall(request).execute()
            // 成功時
            if (response.code == 302) {
                // Set-Cookieを探す。
                // なんか複雑なことしてるけどおそらくヘッダーSet-Cookieが複数あるせいで最後のSet-Cookieの値しか取れないのでめんどい
                response.headers.filter { pair ->
                    pair.second.contains("user_session") && !pair.second.contains("secure") && !pair.second.contains("deleted")
                }.forEach { header ->
                    // user_session
                    val user_session = header.second.split(";")[0].replace("user_session=", "")
                    return@withContext user_session
                }
                // なかった
                return@withContext null
            } else {
                // そもそも失敗
                return@withContext null
            }
        }

    }
}