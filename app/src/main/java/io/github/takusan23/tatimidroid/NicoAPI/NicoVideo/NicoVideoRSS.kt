package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

/**
 * ニコ動のRSS。
 * */
class NicoVideoRSS {

    // ランキングURL
    val rankingGenreUrlList = arrayListOf(
        "genre/all",
        "hot-topic",
        "genre/entertainment",
        "genre/radio",
        "genre/music_sound",
        "genre/dance",
        "genre/animal",
        "genre/nature",
        "genre/cooking",
        "genre/traveling_outdoor",
        "genre/vehicle",
        "genre/sports",
        "genre/society_politics_news",
        "genre/technology_craft",
        "genre/commentary_lecture",
        "genre/anime",
        "genre/game",
        "genre/other"
    )

    // ランキング集計期間
    val rankingTimeList = arrayListOf(
        "hour",
        "24h",
        "week",
        "month",
        "total"
    )

    /**
     * ランキング取得。コルーチン
     * @param ジャンル。rankingGenreUrlListから取ってきてね
     * @param time 集計期間。rankingTimeListから取ってきてね
     * @return 成功時NicoVideoData配列。失敗時nullね
     * */
    fun getRanking(genre: String, time: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://www.nicovideo.jp/ranking/$genre?term=$time&rss=2.0&lang=ja-jp")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * RSSパース
     * */
    fun parseHTML(response: Response): ArrayList<NicoVideoData> {
        val rankingList = arrayListOf<NicoVideoData>()

        val rss = response.body?.string()
        val document = Jsoup.parse(rss, "", Parser.xmlParser())
        // Title
        val titleTag = document.getElementsByTag("title")
        titleTag.removeAt(0)
        // URL
        val linkTag =
            document.getElementsByTag("link") // <link>取得にはJsoup#parse()の引数にParser.xmlParser()を入れる必要あり
        linkTag.removeAt(0)
        /**
         * Descriptionタグ。
         * これCDATAがRSSにあるとうまくパースできないみたい。
         * Document#text()でCDATAなしのHTMLが取得できるのでもう一度Jsoupにスクレイピングさせる。
         * */
        val description = document.getElementsByTag("description")
        description.removeAt(0)
        // ぱーす
        for (i in 0 until 100) {
            val title = titleTag[i].text()
            val videoId = linkTag[i].text().replace("https://www.nicovideo.jp/watch/", "")
            // これ特殊
            val descriptionJsoup = Jsoup.parse(description[i].text())
            val thum = descriptionJsoup.getElementsByTag("img")[0].attr("src")
            val date = descriptionJsoup.getElementsByClass("nico-info-date")[0].text()
            val viewCount =
                descriptionJsoup.getElementsByClass("nico-info-total-view")[0].text()
            val commentCount =
                descriptionJsoup.getElementsByClass("nico-info-total-res")[0].text()
            val mylistCount =
                descriptionJsoup.getElementsByClass("nico-info-total-mylist")[0].text()
            val nicoVideoData =
                NicoVideoData(false, false, title, videoId, thum, stringToUnixTime(date), viewCount, commentCount, mylistCount, "")
            rankingList.add(nicoVideoData)
        }
        return rankingList
    }

    // 日付表示をUnixTimeにするやつ。ミリ秒
    private fun stringToUnixTime(string: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH：mm：ss")
        return simpleDateFormat.parse(string).time
    }

}