package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

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
     * */
    suspend fun getRankingGenreTag(genre: String, time: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://sp.nicovideo.jp/ranking/$genre?term=$time")
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        return@withContext okHttpClient.newCall(request).execute()
    }

    /**
     * [getRankingGenreTag]をパースする関数
     * @param responseString [getRankingGenreTag]のレスポンス
     * */
    suspend fun parseRankingGenreTag(responseString: String?) = withContext(Dispatchers.Default) {
        // スクレイピング
        val html = Jsoup.parse(responseString)
        return@withContext html.getElementsByClass("ranking-SubHeader_ListItem")
            .filter { element -> !element.hasAttr("aria-selected") } // aria-selected なければ
            .map { element -> element.text() }
    }

}