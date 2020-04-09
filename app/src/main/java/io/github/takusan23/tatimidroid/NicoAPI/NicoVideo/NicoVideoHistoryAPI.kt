package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * 履歴取得API
 * */
class NicoVideoHistoryAPI {

    /**
     * 履歴を取得する。
     * @param userSession ユーザーセッション
     * @return Response
     * */
    fun getHistory(userSession: String): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/users/me/watch/history?page=1&pageSize=200") // 最大200件？
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * 履歴JSONをパースする
     * @param json getHistory()で取得した値
     * @return NicoVideoDataの配列
     * */
    fun parseHistoryJSONParse(json: String?): ArrayList<NicoVideoData> {
        val list = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(json)
        val items = jsonObject.getJSONObject("data").getJSONArray("items")
        for (i in 0 until items.length()) {
            val video = items.getJSONObject(i).getJSONObject("video")
            val title = video.getString("title")
            val videoId = video.getString("id")
            val thum = if (video.getJSONObject("thumbnail").isNull("largeUrl")) {
                video.getJSONObject("thumbnail").getString("url")
            } else {
                video.getJSONObject("thumbnail").getString("largeUrl")
            }
            val date = toUnixTime(video.getString("registeredAt"))
            val count = video.getJSONObject("count")
            val viewCount = count.getInt("view").toString()
            val commentCount = count.getInt("comment").toString()
            val mylistCount = count.getInt("mylist").toString()
            val data =
                NicoVideoData(false, false, title, videoId, thum, date, viewCount, commentCount, mylistCount, "")
            list.add(data)
        }
        return list
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

}