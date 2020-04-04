package io.github.takusan23.tatimidroid.NicoAPI

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject

/**
 * ニコニコのユーザー情報を取得するAPI
 * 高階関数/コルーチン版どっちもあるよ
 * @param context Context
 * @param userId ユーザーID。自分のを取得する場合は空にして、getUserCoroutine()の引数に「https://nvapi.nicovideo.jp/v1/users/me」を指定
 * */
class User(val context: Context?, val userId: String) {

    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    val userSession = prefSetting.getString("user_session", "")

    /**
     * ユーザー情報を取得する。 高階関数版
     * 中身でコルーチン動かしてるだけなんですけどね
     * */
    fun getUser(responseFun: (UserData?) -> Unit) {
        GlobalScope.launch {
            val userData = getUserCoroutine().await()
            responseFun(userData)
        }
    }

    /**
     * ユーザー情報を取得する。コルーチン版。
     * @param url 自分のページを表示させる場合はこれ→https://nvapi.nicovideo.jp/v1/users/me
     * */
    fun getUserCoroutine(url: String = "https://nvapi.nicovideo.jp/v1/users/$userId"): Deferred<UserData?> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url(url)
                header("Cookie", "user_session=$userSession")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("x-frontend-id", "3") // これ必要。
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonObject = JSONObject(response.body?.string())
                val user = jsonObject.getJSONObject("data").getJSONObject("user")
                val description = user.getString("description")
                val isPremium = user.getBoolean("isPremium")
                val niconicoVersion = user.getString("registeredVersion") // GINZA とか く とか
                val followeeCount = user.getInt("followeeCount")
                val followerCount = user.getInt("followerCount")
                val userLevel = user.getJSONObject("userLevel")
                val currentLevel = userLevel.getInt("currentLevel") // ユーザーレベル。大人数ゲームとかはレベル条件ある
                val userId = user.getInt("id")
                val nickName = user.getString("nickname")
                val isFollowing = if (jsonObject.getJSONObject("data").has("relationships")) {
                    jsonObject.getJSONObject("data").getJSONObject("relationships")
                        .getJSONObject("sessionUser").getBoolean("isFollowing") // フォロー中かどうか
                } else {
                    false
                }
                val userData =
                    UserData(description, isPremium, niconicoVersion, followeeCount, followerCount, userId, nickName, isFollowing, currentLevel)
                return@async userData
            } else {
                showToast(context?.getString(R.string.error) + "\n" + response.code)
                return@async null
            }
        }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}

data class UserData(
    val description: String,
    val isPremium: Boolean,
    val niconicoVersion: String,
    val followeeCount: Int,
    val followerCount: Int,
    val userId: Int,
    val nickName: String,
    val isFollowing: Boolean,
    val currentLevel: Int
)