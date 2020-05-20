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
 * ニコレポのAPIを叩く関数。
 * コルーチンです。
 * */
class NicoRepoAPI {

    /**
     * ニコレポのAPIを叩いてレスポンスを返す関数。
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス。
     * */
    fun getNicoRepoResponse(userSession: String): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://www.nicovideo.jp/api/nicorepo/timeline/my/all?client_app=pc_myrepo")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * ニコレポのレスポンスJSONをパースする関数。
     * @param responseString getNicoRepoResponse()の返り値
     * @return NicoVideoDataの配列
     * */
    fun parseNicoRepoResponse(responseString: String?): ArrayList<NicoVideoData> {
        val list = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(responseString)
        val dataList = jsonObject.getJSONArray("data")
        for (i in 0 until dataList.length()) {
            val dataObject = dataList.getJSONObject(i)
            // 動画のみ
            val topic = dataObject.getString("topic")
            if (topic == "nicovideo.user.video.upload") {
                val videoObject = dataObject.getJSONObject("video")
                val id = videoObject.getString("id")
                val title = videoObject.getString("title")
                val videoId = videoObject.getString("videoWatchPageId")
                val createAt = toUnixTime(dataObject.getString("createdAt"))
                val thumb = videoObject.getJSONObject("thumbnailUrl").getString("normal")
                val data = NicoVideoData(isCache = false, isMylist = false, title = title, videoId = videoId, thum = thumb, date = createAt, viewCount = "-1", commentCount = "-1", mylistCount = "-1", mylistItemId = "", mylistAddedDate = null, duration = null, cacheAddedDate = null)
                list.add(data)
            }
        }
        return list
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        return simpleDateFormat.parse(time).time
    }

}