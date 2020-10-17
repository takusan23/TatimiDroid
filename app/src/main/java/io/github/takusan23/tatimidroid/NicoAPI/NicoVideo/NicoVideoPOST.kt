package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat

/**
 * 投稿動画取得
 * */
class NicoVideoPOST {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * 投稿動画取得APIを叩く。
     * マイページが変わって（というか私GINZA時代だしずっと変わってなかったんやな）投稿動画APIが出現した
     * というわけでスクレイピング回避することに成功しました。
     * @param userId ユーザーID。
     * @param userSession ユーザーセッション
     * @param page ページ。最近のサイトみたいに必要な部分だけAPIを叩いて取得するようになった。
     * */
    suspend fun getPOSTVideo(userId: String, userSession: String, page: Int = 0) = withContext(Dispatchers.IO) {
        // うらる
        val url = "https://nvapi.nicovideo.jp/v1/users/$userId/videos?sortKey=registeredAt&sortOrder=desc&pageSize=25&page=$page"
        val request = Request.Builder().apply {
            url(url)
            addHeader("Cookie", "user_session=${userSession}")
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            addHeader("x-frontend-id", "6")
            addHeader("X-Frontend-Version", "0")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * [getPOSTVideo]のレスポンスぼでーJSONをパースする。
     * @param responseString レスポンス。JSON
     * @return [NicoVideoData]の配列
     * */
    suspend fun parsePOSTVideo(responseString: String?) = withContext(Dispatchers.Default) {
        val videoList = arrayListOf<NicoVideoData>()
        val jsonObject = JSONObject(responseString)
        val items = jsonObject.getJSONObject("data").getJSONArray("items")
        // まわす
        for (i in 0 until items.length()) {
            val videoObject = items.getJSONObject(i)
            val title = videoObject.getString("title")
            val videoId = videoObject.getString("id")
            val thumbnailUrl = videoObject.getJSONObject("thumbnail").getString("url")
            val postDate = videoObject.getString("registeredAt")
            val countObject = videoObject.getJSONObject("count")
            val playCount = countObject.getInt("view").toString()
            val commentCount = countObject.getInt("comment").toString()
            val mylistCount = countObject.getInt("mylist").toString()
            val duration = videoObject.getInt("duration").toLong()
            val data = NicoVideoData(
                isCache = false,
                isMylist = false,
                title = title,
                videoId = videoId,
                thum = thumbnailUrl,
                date = toUnixTime(postDate),
                viewCount = playCount,
                commentCount = commentCount,
                mylistCount = mylistCount,
                mylistItemId = "",
                mylistAddedDate = null,
                duration = duration,
                cacheAddedDate = null
            )
            videoList.add(data)
        }
        videoList
    }

    // UnixTime（ミリ秒）に変換する関数
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
        return simpleDateFormat.parse(time).time
    }

}