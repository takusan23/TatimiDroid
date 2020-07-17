package io.github.takusan23.tatimidroid.NicoAPI.User

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject

/**
 * ニコニコのユーザー情報を取得するAPI
 * コルーチン版のみ
 * @param context Context
 * @param userId ユーザーID。自分のを取得する場合は空にして、getUserCoroutine()の引数に「https://nvapi.nicovideo.jp/v1/users/me」を指定
 * */
class User {

    /**
     * ユーザー情報を取得する。コルーチン版。
     * @param userId ユーザーID。作者は「40210583」
     * @param userSession ユーザーセッション
     * @param url 自分のページを表示させる場合はこれ→https://nvapi.nicovideo.jp/v1/users/me
     * */
    suspend fun getUserCoroutine(userId: String, userSession: String, url: String = "https://nvapi.nicovideo.jp/v1/users/$userId") = withContext(Dispatchers.IO) {
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
            withContext(Dispatchers.Default) {
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
                UserData(description, isPremium, niconicoVersion, followeeCount, followerCount, userId, nickName, isFollowing, currentLevel)
            }
        } else {
            null
        }
    }
}