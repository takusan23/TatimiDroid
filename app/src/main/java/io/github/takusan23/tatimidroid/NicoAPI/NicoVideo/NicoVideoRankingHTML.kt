package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import android.os.Build
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
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
 * ニコ動のランキング取得など
 * API無いっぽいしスマホ版スクレイピング（スマホ版じゃないとコメント数取れない？）
 * */
class NicoVideoRankingHTML {

    /**
     * ランキングのジャンルを選んだ時に出てくるタグを取得する
     * 例：その他を選んだ時は {オークション男,BB先輩劇場} など
     * @param genre ジャンル。[NicoVideoRSS.rankingGenreUrlList]から選んで
     * @param time 集計時間。[NicoVideoRSS.rankingTimeList]から選んで
     * @param tag タグ。音楽だとVOCALOIDなど。無くてもいい
     * */
    suspend fun getRankingGenreHTML(genre: String, time: String,tag:String?=null) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            if(tag!=null){
                url("https://sp.nicovideo.jp/ranking/$genre?term=$time&tag=$tag")
            }else{
                url("https://sp.nicovideo.jp/ranking/$genre?term=$time")
            }
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        return@withContext okHttpClient.newCall(request).execute()
    }

    /**
     * [getRankingGenreHTML]から動画一覧をスクレイピングする関数
     * @param responseString [getRankingGenreHTML]のレスポンス。Response#body#string()は一度しか呼べないのでいったん変数に入れないと
     * */
    suspend fun parseRankingVideo(responseString: String?) = withContext(Dispatchers.Default) {
        val videoList = arrayListOf<NicoVideoData>()
        val html = Jsoup.parse(responseString)
        // JSON
        val jsonObject = JSONObject(html.getElementsByTag("script")[11].html())
        val jsonVideoList = jsonObject.getJSONArray("itemListElement")
        for (i in 0 until jsonVideoList.length()) {
            val videoObject = jsonVideoList.getJSONObject(i)
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
            videoList.add(data)
        }
        videoList
    }

    /**
     * [getRankingGenreHTML]をパースする関数
     * @param responseString [getRankingGenreHTML]のレスポンス
     * */
    suspend fun parseRankingGenreTag(responseString: String?) = withContext(Dispatchers.Default) {
        // スクレイピング
        val html = Jsoup.parse(responseString)
        return@withContext html.getElementsByClass("ranking-SubHeader_ListItem")
            .map { element -> element.text() }
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