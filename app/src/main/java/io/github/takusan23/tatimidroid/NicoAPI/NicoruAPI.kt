package io.github.takusan23.tatimidroid.NicoAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Connection
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ニコるくんAPI。金稼ぎに走ってしまったニコるくんなんて言ってはいけない。
 * nicoru_result.statusが2ならnicorukey切れてる。4ならすでに追加済み。
 * */
class NicoruAPI {

    /**
     * ニコるときに使う「nicorukey」を取得する。コルーチンです。
     * @param userSession ユーザーセッション
     * @param threadId js-initial-watch-dataのdata-api-dataのthread.ids.defaultの値
     * */
    fun getNicoruKey(userSession: String, threadId: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://nvapi.nicovideo.jp/v1/nicorukey?language=0&threadId=$threadId")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("Cookie", "user_session=$userSession")
                header("Content-Type", "application/x-www-form-urlencoded")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * ニコるを送信する。コルーチンです。
     * すでにニコってても200が帰ってくる模様。
     * @param userSession ユーザーセッション
     * @param threadId js-initial-watch-dataのdata-api-dataのthread.ids.defaultの値
     * @param userId ユーザーID
     * @param id コメントID
     * @param commentText コメントの内容
     * @param postDate コメントの投稿時間（UnixTime）。決してニコった時間ではない。
     * @param nicoruKey getNicoruKey()で取得した値。
     * */
    fun postNicoru(userSession: String, threadId: String, userId: String, id: String, commentText: String, postDate: String, nicoruKey: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                // POSTするJSON
                val postData = JSONArray().apply {
                    put(JSONObject().apply {
                        this.put("nicoru", JSONObject().apply {
                            put("thread", threadId)
                            put("user_id", userId)
                            put("premium", 1)
                            put("fork", 0)
                            put("language", 0)
                            put("id", id)
                            put("content", commentText)
                            put("postdate", postDate)
                            put("nicorukey", nicoruKey)
                        })
                    })
                }
                url("https://nmsg.nicovideo.jp/api.json/")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("Cookie", "user_session=$userSession")
                header("Content-Type", "application/x-www-form-urlencoded")
                post(postData.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

}