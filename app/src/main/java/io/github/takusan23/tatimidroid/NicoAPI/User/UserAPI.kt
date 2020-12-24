package io.github.takusan23.tatimidroid.NicoAPI.User

import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * ニコニコのユーザー情報を取得するAPI
 * コルーチン版のみ
 * */
class UserAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ユーザー情報を取得する。コルーチン版。自分の情報を取得する[getMyAccountUserData]もあります。
     * @param userId ユーザーID。作者は「40210583」
     * @param userSession ユーザーセッション
     * */
    suspend fun getUserData(userId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/$userId")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3") // これ必要。
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * 自分のアカウントの情報を取得する。
     * @param userSession ユーザーセッション
     * */
    suspend fun getMyAccountUserData(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("x-frontend-id", "3") // これ必要。
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getUserData]のレスポンスぼでーをパースする
     * @param responseString [okhttp3.ResponseBody.string]
     * @return データクラス
     * */
    suspend fun parseUserData(responseString: String?) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
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
        val largeIcon = user.getJSONObject("icons").getString("large")
        UserData(description, isPremium, niconicoVersion, followeeCount, followerCount, userId, nickName, isFollowing, currentLevel, largeIcon)
    }

}