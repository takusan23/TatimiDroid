package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import android.os.Build
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * ニコ動のシリーズ取得。PC版をスクレイピング
 *
 * スマホ版は規制がかかった動画を非表示にするのでPC版をスクレイピングすることに。
 * */
class NicoVideoSeriesAPI {

    /**
     * シリーズの動画一覧へアクセスしてHTMLを取りに行く。スマホ版は申し訳ないが規制が入ってるのでNG。
     * @param seriesId シリーズのID。https://nicovideo.jp/series/{ここの文字}
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    suspend fun getSeriesVideoList(userSession: String, seriesId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://nicovideo.jp/series/$seriesId")
            addHeader("Cookie", "user_session=$userSession")
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getSeriesVideoList]のHTMLをスクレイピングして配列に変換する
     * @param responseHTML [getSeriesVideoList]
     * @return [NicoVideoData]の配列
     * */
    suspend fun parseSeriesVideoList(responseHTML: String?) = withContext(Dispatchers.Default) {
        val list = arrayListOf<NicoVideoData>()
        // HTMLスクレイピング
        val document = Jsoup.parse(responseHTML)
        // mapって便利やな
        val titleList = document.getElementsByClass("VideoMediaObject-title").map {
            it.getElementsByTag("a")[0].text()
        }
        val videoIdList = document.getElementsByClass("VideoMediaObject-title").map {
            it.getElementsByTag("a")[0].attr("href").replace("/watch/", "")
        }
        val thumbUrlList = document.getElementsByClass("Thumbnail-image").map {
            it.getElementsByClass("Thumbnail-image").attr("data-background-image")
        }
        val dateList = document.getElementsByClass("SeriesVideoListContainer-videoRegisteredAt").map {
            val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm 投稿")
            simpleDateFormat.parse(it.text()).time
        }
        val viewCountList = document.getElementsByClass("VideoMetaCount VideoMetaCount-view").map { it.text() }
        val mylistCountList = document.getElementsByClass("VideoMetaCount VideoMetaCount-mylist").map { it.text() }
        val commentCountList = document.getElementsByClass("VideoMetaCount VideoMetaCount-comment").map { it.text() }
        val durationList = document.getElementsByClass("VideoLength").map {
            // SimpleDataFormatで(mm:ss)をパースしたい場合はタイムゾーンをUTCにすればいけます。これで動画時間を秒に変換できる
            val simpleDateFormat = SimpleDateFormat("mm:ss").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            simpleDateFormat.parse(it.text()).time / 1000 // 1:00 なら　60 へ
        }

        for (i in titleList.indices) {
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = titleList[i],
                videoId = videoIdList[i],
                thum = thumbUrlList[i + 1], // 余分なのが先頭に入ってる
                date = dateList[i],
                viewCount = viewCountList[i],
                commentCount = commentCountList[i],
                mylistCount = mylistCountList[i],
                duration = durationList[i]
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