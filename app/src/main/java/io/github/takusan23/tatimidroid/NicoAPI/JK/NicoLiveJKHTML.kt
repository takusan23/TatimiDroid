package io.github.takusan23.tatimidroid.NicoAPI.JK

import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.*

/**
 * ニコニコ実況の番組一覧を取得することぐらいしか無い
 *
 * */
class NicoLiveJKHTML {

    /** OkHttp */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ニコニコ実況の番組一覧のHTMLを取得する
     *
     * ぶっちゃけハードコートでも良くね？
     * @param userSession ログイン情報
     * @return OkHttpのレスポンス。[parseNicoLiveJKProgramList]で使う
     * */
    suspend fun getNicoLiveJKProgramList(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://jk.nicovideo.jp/")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        return@withContext okHttpClient.newCall(request).execute()
    }

    /**
     * [getNicoLiveJKProgramList]をスクレイピングしてデータを取り出す
     * */
    suspend fun parseNicoLiveJKProgramList(responseString: String?) = withContext(Dispatchers.Default) {

        // おはよう！午前４時に 何してるんだい？
        val startTime = Calendar.getInstance().apply {
            // 午前4時に枠が変わる
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time.time
        // 次の日の朝４時まで
        val endTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time.time

        val document = Jsoup.parse(responseString)
        val nicoLiveProgramDataList = document.getElementsByClass("tv__card tvcard").map { element ->
            // divの中にあるimgを取得
            val imgElement = element.getElementsByTag("img")[0]

            val title = imgElement.attr("alt")
            val channelName = "$title（ニコニコ実況）"
            // 生放送IDは取れない。チャンネルIDが取れる
            val programId = element.attr("href").replace("https://live.nicovideo.jp/watch/", "")
            val broadCaster = channelName
            val lifeCycle = "ON_AIR"
            val thumbUrl = "https://jk.nicovideo.jp/${imgElement.attr("src")}"

            /**
             * 本当に謎なんだけど、どうやらニコニコ実況だけは公式なのに、programinfo（全部屋、流量制限コメントサーバー、ハブられたコメント）
             *
             * のAPIが叩けるので例外としてfalseにする
             * */
            val isOfficial = false
            NicoLiveProgramData(title, channelName, startTime.toString(), endTime.toString(), programId, broadCaster, lifeCycle, thumbUrl, isOfficial)
        }
        if (nicoLiveProgramDataList.isNotEmpty()) {
            // 中身あるので返す
            return@withContext nicoLiveProgramDataList
        } else {
            // 空っぽだったのでハードコートした値を返す。正直こっちでよくね？
            return@withContext arrayListOf(
                NicoLiveProgramData("NHK 総合", "NHK 総合（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646436", "NHK 総合（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("Eテレ 総合", "Eテレ 総合（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646437", "Eテレ 総合（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("日本テレビ", "日本テレビ（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646438", "日本テレビ（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("テレビ朝日", "テレビ朝日（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646439", "テレビ朝日（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("TBSテレビ", "TBSテレビ（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646440", "TBSテレビ（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("テレビ東京", "テレビ東京（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646441", "テレビ東京（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("フジテレビ", "フジテレビ（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646442", "フジテレビ（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("TOKYO MX", "TOKYO MX（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646485", "TOKYO MX（ニコニコ実況）", "ON_AIR", "", true),
                NicoLiveProgramData("BS11", "BS11（ニコニコ実況）", startTime.toString(), endTime.toString(), "ch2646846", "BS11（ニコニコ実況）", "ON_AIR", "", true),
            )
        }
    }

}