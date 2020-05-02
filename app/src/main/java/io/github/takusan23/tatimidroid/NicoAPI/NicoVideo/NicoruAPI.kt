package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * ニコるくんAPI。金稼ぎに走ってしまったニコるくんなんて言ってはいけない。
 * nicoru_result.statusが2ならnicorukey切れてる。4ならすでに追加済み。
 * */
class NicoruAPI {

    // nicoruKey
    var nicoruKey = ""

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
                header("X-Frontend-Id", "6")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * NicoruKeyを取得する。
     * 一度取得したら他のにこるでもこのKeyを使い回す。この関数を呼ぶとnicoryKeyが使えるようになります。
     * @param responseString getNicoruKey()のレスポンス
     * @return nicoruKeyに値が入る
     * */
    fun parseNicoruKey(responseString: String?) {
        val jsonObject = JSONObject(responseString)
        nicoruKey = jsonObject.getJSONObject("data").getString("nicorukey")
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

    /**
     * ニコるくんの結果を取得する
     * @param responseJSONObject postNicoru()のレスポンスをJSONArrayにして2番目のJSONObject
     * @return status : 0 なら成功？ 1だとnicoruKeyがおかしい 2だとnicoruKey失効、4だとすでにニコり済み
     * */
    fun nicoruResultStatus(responseJSONObject: JSONObject): Int {
        val nicoruResult = responseJSONObject.getJSONObject("nicoru_result").getInt("status")
        return nicoruResult
    }

    /**
     * ニコるくんのニコる数を取得する関数
     * @param responseJSONObject postNicoru()のレスポンスをJSONArrayにして2番目のJSONObject
     * @return ニコる数
     * */
    fun nicoruResultNicoruCount(responseJSONObject: JSONObject): Int {
        val nicoruResult = responseJSONObject.getJSONObject("nicoru_result").getInt("nicoru_count")
        return nicoruResult
    }

}