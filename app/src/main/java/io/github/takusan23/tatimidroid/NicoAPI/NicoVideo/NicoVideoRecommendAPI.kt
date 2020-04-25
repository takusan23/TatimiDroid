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
 * 関連動画のAPIを叩く・パースする関数
 * */
class NicoVideoRecommendAPI {
    /**
     * 関連動画取得APIを叩く。
     * @param watchRecommendationRecipe parseJSON()#watchRecommendationRecipeの値。
     * */
    fun getVideoRecommend(watchRecommendationRecipe: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://nvapi.nicovideo.jp/v1/recommend?recipe=$watchRecommendationRecipe&site=nicovideo&_frontendId=6")
                addHeader("User-Agent", "TatimiDroid;@takusan_23")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * getVideoRecommend()のレスポンスをパースする。
     * @param responseString getVideoRecommend()の返り値
     * @return NicoVideoDataの配列
     * */
    fun parseVideoRecommend(responseString: String?): ArrayList<NicoVideoData> {
        val list = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(responseString)
        val items = jsonObject.getJSONObject("data").getJSONArray("items")
        for (i in 0 until items.length()) {
            val videoObject = items.getJSONObject(i)
            val recommendType = videoObject.getString("recommendType")
            // 関連動画のみ
            if (recommendType == "recommend") {
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
                val data =
                    NicoVideoData(false, false, videoTitle, videoId, thumb, registeredAt, viewCount, commentCount, mylistCount, "", null, duration,null)
                list.add(data)
            }
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