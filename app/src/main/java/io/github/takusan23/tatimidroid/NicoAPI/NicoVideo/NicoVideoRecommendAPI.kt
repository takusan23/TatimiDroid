package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * 関連動画のAPIを叩く・パースする関数
 * */
class NicoVideoRecommendAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 関連動画取得APIを叩く。
     * @param watchRecommendationRecipe parseJSON()#watchRecommendationRecipeの値。
     * */
    suspend fun getVideoRecommend(watchRecommendationRecipe: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nvapi.nicovideo.jp/v1/recommend?recipe=$watchRecommendationRecipe&site=nicovideo&_frontendId=6")
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * getVideoRecommend()のレスポンスをパースする。
     * @param responseString getVideoRecommend()の返り値
     * @return NicoVideoDataの配列
     * */
    suspend fun parseVideoRecommend(responseString: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(responseString)
        val items = jsonObject.getJSONObject("data").getJSONArray("items")
        for (i in 0 until items.length()) {
            val videoObject = items.getJSONObject(i)
            val contentType = videoObject.getString("contentType")
            val recommendType = videoObject.getString("recommendType")
            // 動画のみ
            if (contentType == "video") {
                val contentObject = videoObject.getJSONObject("content")
                val videoId = contentObject.getString("id")
                val videoTitle = contentObject.getString("title")
                val registeredAt = toUnixTime(contentObject.getString("registeredAt"))
                val thumb = if (contentObject.getJSONObject("thumbnail").isNull("largeUrl")) {
                    contentObject.getJSONObject("thumbnail").getString("url")
                } else {
                    contentObject.getJSONObject("thumbnail").getString("largeUrl")
                }
                val countObject = contentObject.getJSONObject("count")
                val viewCount = countObject.getString("view")
                val commentCount = countObject.getString("comment")
                val mylistCount = countObject.getString("mylist")
                val duration = contentObject.getLong("duration")
                val data = NicoVideoData(isCache = false, isMylist = false, title = videoTitle, videoId = videoId, thum = thumb, date = registeredAt, viewCount = viewCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = "", mylistAddedDate = null, duration = duration, cacheAddedDate = null)
                list.add(data)
            }
        }
        list
    }

    // UnixTimeへ変換
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

}