package io.github.takusan23.tatimidroid.NicoLiveAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 *  ニコ生のHTMLページを取得する
 * コルーチンで使ってね
 * */
class NicoLiveHTML {

    /**
     * HTML取得
     * @param liveId 番組ID
     * @param usersession ユーザーセッション。なければ非ログイン？
     * */
    fun getNicoLiveHTML(liveId: String, usersession: String?): Deferred<String> =
        GlobalScope.async {
            val url = "https://live2.nicovideo.jp/watch/$liveId"
            val request = Request.Builder()
                .get()
                .url(url)
                .addHeader("User-Agent", "TatimiDroid;@takusan_23")
                .addHeader("Cookie", "user_session=$usersession")
                .build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                return@async response.body?.string() ?: ""
            } else {
                return@async ""
            }
        }

    /**
     * HTMLの中からJSONを見つけてくる関数
     * @param response HTML
     * */
    fun nicoLiveHTMLtoJSONObject(response: String): JSONObject {
        val html = Jsoup.parse(response)
        val json = html.getElementById("embedded-data").attr("data-props")
        return JSONObject(json)
    }

}