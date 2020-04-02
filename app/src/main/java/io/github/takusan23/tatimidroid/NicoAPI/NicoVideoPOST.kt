package io.github.takusan23.tatimidroid.NicoAPI

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
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
    fun getList(page: Int, userSession: String): Deferred<Response> =
        GlobalScope.async {
            // 200件最大まで取得する
            val url = "https://www.nicovideo.jp/my/video?page=$page"
            val request = Request.Builder().apply {
                url(url)
                header("Cookie", "user_session=${userSession}")
                header("x-frontend-id", "6") //3でスマホ、6でPC　なんとなくPCを指定しておく。 指定しないと成功しない
                header("User-Agent", "TatimiDroid;@takusan_23")
                get()
            }.build()

            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    fun parseHTML(response: Response): ArrayList<NicoVideoData> {
        val videoList = arrayListOf<NicoVideoData>()
        val document = Jsoup.parse(response.body?.string())
        //動画のDiv要素を取り出す
        val divList = document.getElementsByClass("outer VideoItem")
        divList.forEach {
            if (it.getElementsByClass("ct").isNotEmpty()) {
                //一つずつ見ていく
                var videoId = it.getElementsByClass("ct").first().attr("href")
                videoId = videoId.replace("http://commons.nicovideo.jp/tree/", "")
                val title =
                    it.getElementsByTag("h5").first().getElementsByTag("a").text()
                val postDate =
                    it.getElementsByClass("posttime").first().text().replace(" 投稿", "")
                val thumbnailUrl = it.getElementsByTag("img").first().attr("src")
                val commentCount = it.getElementsByClass("comment").first().text()
                val playCount = it.getElementsByClass("play").first().text()
                val mylistCount = it.getElementsByClass("mylist").first().text()
                val data =
                    NicoVideoData(title, videoId, thumbnailUrl, toUnixTime(postDate), playCount, commentCount, mylistCount)
                videoList.add(data)
            }

            //次のページへ移動
            if (document.getElementsByClass("outer VideoListHeadMenuContainer")
                    .isNotEmpty() &&
                document.getElementsByClass("outer VideoListHeadMenuContainer")[0].getElementsByClass("pager")
                    .isNotEmpty()
            ) {
                val videoListButtonDiv =
                    document.getElementsByClass("outer VideoListHeadMenuContainer")[0].getElementsByClass(
                        "pager"
                    )[0]
                val nextButton = videoListButtonDiv.children().last()
                //href取得。次のページへ
                //最後かどうか判断しないと無限に取得しに行くので。最後は要素が「span」になる。最後の要素が「span」なら取得しない。
                if (nextButton.tagName() != "span") {
                    val link = nextButton.attr("href")
                }
            }
        }
        return videoList
    }

    // UnixTime（ミリ秒）に変換する関数
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yy年MM月dd日 HH:mm")
        return simpleDateFormat.parse(time).time
    }

}