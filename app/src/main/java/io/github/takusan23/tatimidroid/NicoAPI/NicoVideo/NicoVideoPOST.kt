package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

/**
 * 投稿動画取得
 * */
class NicoVideoPOST {

    /**
     * 投稿動画スクレイピング。
     * @param page ページ数。。何もなければ空でいいよ。
     * @param userSession ユーザーセッション
     * */
    suspend fun getList(page: Int, userId: String, userSession: String) = withContext(Dispatchers.IO) {
        val url = "https://www.nicovideo.jp/user/$userId/video?page=$page"
        val request = Request.Builder().apply {
            url(url)
            header("Cookie", "user_session=${userSession}")
            header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    suspend fun parseHTML(responseString: String?) = withContext(Dispatchers.Default) {
        val videoList = arrayListOf<NicoVideoData>()
        val document = Jsoup.parse(responseString)
        //動画のDiv要素を取り出す
        val divList = document.getElementsByClass("outer VideoItem")
        divList.forEach {
            // 動画ID
            val videoId = it.getElementsByTag("a")[0].attr("href").replace("watch/", "")
            //一つずつ見ていく
            val title =
                it.getElementsByTag("h5").first().getElementsByTag("a").text()
            val postDate =
                it.getElementsByClass("posttime").first().text().replace(" 投稿", "")
            val thumbnailUrl = it.getElementsByTag("img")[0].attr("data-original")
            val commentCount = it.getElementsByClass("comment").first().text()
            val playCount = it.getElementsByClass("play").first().text()
            val mylistCount = it.getElementsByClass("mylist").first().text()
            val data = NicoVideoData(isCache = false, isMylist = false, title = title, videoId = videoId, thum = thumbnailUrl, date = toUnixTime(postDate), viewCount = playCount, commentCount = commentCount, mylistCount = mylistCount, mylistItemId = "", mylistAddedDate = null, duration = null, cacheAddedDate = null)
            videoList.add(data)
        }
        videoList
    }

    // UnixTime（ミリ秒）に変換する関数
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yy年MM月dd日 HH:mm")
        return simpleDateFormat.parse(time).time
    }

}