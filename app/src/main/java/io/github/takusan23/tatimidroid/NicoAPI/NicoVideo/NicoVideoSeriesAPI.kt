package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import android.os.Build
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * ニコ動のシリーズ取得
 * https://sp.nicovideo.jp/series/　へアクセスしてスクレイピングしてJSONを取り出す
 * スマホ版じゃないとコメント数が取れない？
 * */
class NicoVideoSeriesAPI() {

    /**
     * シリーズの動画一覧へアクセスしてHTMLを取りに行く
     * @param seriesId シリーズのID。https://sp.nicovideo.jp/series/{ここの文字}
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    suspend fun getSeriesVideoList(userSession: String, seriesId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://sp.nicovideo.jp/series/$seriesId")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getSeriesVideoList]のHTMLをパースして配列に変換する
     * @param responseHTML HTML
     * @return [NicoVideoData]の配列
     * */
    suspend fun parseSeriesVideoList(responseHTML: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        // HTMLスクレイピング
        val jsonElement = Jsoup.parse(responseHTML).getElementsByTag("script")
        val seriesJSONElement = jsonElement[5]
/*
        jsonElement.forEachIndexed { index, element ->
            println(index)
            println(element.html())
        }
*/
        // JSONパース
        val jsonObject = JSONObject(seriesJSONElement.html())
        val itemListElement = jsonObject.getJSONArray("itemListElement")
        for (i in 0 until itemListElement.length()) {
            val videoObject = itemListElement.getJSONObject(i)
            val title = videoObject.getString("name")
            val videoId = videoObject.getString("@id").replace("https://sp.nicovideo.jp/watch/", "")
            val thumbUrl = videoObject.getJSONArray("thumbnail").getJSONObject(0).getString("contentUrl")
            val date = toUnixTime(videoObject.getString("uploadDate"))
            val countObject = videoObject.getJSONArray("interactionStatistic")
            val viewCount = countObject.getJSONObject(0).getString("userInteractionCount")
            val mylistCount = countObject.getJSONObject(1).getString("userInteractionCount")
            val commentCount = countObject.getJSONObject(2).getString("userInteractionCount")
            val duration = formatISO8601ToSecond(videoObject.getString("duration"))
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = title,
                videoId = videoId,
                thum = thumbUrl,
                date = date,
                viewCount = viewCount,
                commentCount = commentCount,
                mylistCount = mylistCount,
                duration = duration
            )
            list.add(data)
        }
        list
    }

    /** UnixTime変換 */
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        return simpleDateFormat.parse(time).time
    }

    /**
     * なんかしらんけど、「PT176S」みたいな文字列も時間を表してるらしく、それをパースする（ISO8601 Durationで検索）
     * なおAndroid 8以上のAPIを利用しているため8未満のデバイスは0を返します。
     * @return 時間（秒）を返します。なおAndroid 8未満は対応してないので0です
     * */
    private fun formatISO8601ToSecond(duration: String): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Duration.parse(duration).get(ChronoUnit.SECONDS)
        } else {
            0
        }
    }

}