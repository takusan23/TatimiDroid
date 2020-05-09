package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * 実験用。
 * xmlでコメント取得する
 * */
class NicoCommentXML {

    /**
     * ニコニコ動画のコメントサーバーとかの情報があるAPIを叩く。今はHTMLに含まれているのでこの方法は使わない。
     * コルーチンです。
     * @param userSession ユーザーセッション
     * @param videoId 動画ID
     * */
    fun getFlv(userSession: String, videoId: String): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://flapi.nicovideo.jp/api/getflv/$videoId")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * threadIdを返す
     * @param responseString getFlv()のレスポンス
     * */
    fun getThreadId(responseString: String): String {
        val list = responseString.split("&")
        val threadId = list[0].split("=")[1]
        return threadId
    }

    /**
     * userIdを返す
     * @param responseString getFlv()のレスポンス
     * */
    fun getUserId(responseString: String): String {
        val list = responseString.split("&")
        val threadId = list[5].split("=")[1]
        return threadId
    }

    /**
     * 過去コメント取得に必要な値を取得する関数
     * @param userSession ユーザーセッション
     * @param threadId getThreadId()の値
     * */
    fun getWayBackKey(threadId: String, userSession: String): Deferred<String?> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://flapi.nicovideo.jp/api/getwaybackkey?thread=$threadId")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("Cookie", "user_session=$userSession")
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string()
            val wayBackKey = responseString?.replace("waybackkey=", "")
            return@async wayBackKey
        }

    /**
     * コメント取得。コルーチンです。
     * @param threadId getThreadId()の値
     * @param unixTime 過去コメントの日付指定。UnixTimeで頼んだ。
     * @param userId ユーザーID
     * @param wayBackKey getWayBackKey()で取得した値。過去コメント取得の際に使う。過去コメントじゃないならnullでいいよ
     * */
    fun getXMLComment(threadId: String, userId: String, wayBackKey: String?, unixTime: String?): Deferred<String?> =
        GlobalScope.async {
            // POSTする内容
            val postData = if (wayBackKey == null && unixTime == null) {
                // 通常リクエスト
                "<thread res_from=\"-1000\" version=\"20061206\" scores=\"1\" thread=\"${threadId}\"/>\n"
            } else {
                // 過去コメリクエスト
                "<thread res_from=\"-1000\" version=\"20061206\" scores=\"1\" thread=\"${threadId}\" waybackkey=\"${wayBackKey}\" when=\"${unixTime}\" user_id=\"${userId}\"  />\n"
            }
            // リクエスト
            val request = Request.Builder().apply {
                url("https://nmsg.nicovideo.jp/api/") // https！？
                header("User-Agent", "TatimiDroid;@takusan_23")
                post(postData.toRequestBody("application/xml".toMediaType()))
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            val comment = response.body?.string()
            return@async comment
        }

}