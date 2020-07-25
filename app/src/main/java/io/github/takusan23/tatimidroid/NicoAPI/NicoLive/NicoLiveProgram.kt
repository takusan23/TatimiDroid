package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.ProgramData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup

/**
 * ニコ生のトップページから
 * フォロー中
 * 一般
 * ルーキー
 * 等を取得するクラス。
 * コルーチンですねえ！
 * ProgramAPIクラスは非推奨です
 *
 * **んなことよりニコ生TOPページの一番上に朝鮮中央テレビミラーとか馬の放送とかいらんやろ。ランキングとか置けよ**
 * */
class NicoLiveProgram {

    /**
     * NicoLiveProgram#parseJSON()の二番目の引数に入れる値。const val と val って何が違うんだ？
     * */
    companion object {
        /** フォロー中番組。 */
        const val FAVOURITE_PROGRAM = "favoriteProgramListState"

        /** あなたへのおすすめ。～歳 無職 みたいなタイトルばっか出てくるのニコ生っぽくて好き */
        const val RECOMMEND_PROGRAM = "recommendedProgramListState"

        /** 放送中の注目番組取得。公式放送。トップページの一番上に並んでる奴ら */
        const val FORCUS_PROGRAM = "focusProgramListState"

        /** これからの注目番組。*/
        const val RECENT_JUST_BEFORE_BROADCAST_STATUS_PROGRAM = "recentJustBeforeBroadcastStatusProgramListState"

        /** 人気の予約されている番組取得。アニメ一挙とか */
        const val POPULAR_BEFORE_OPEN_BROADCAST_STATUS_PROGRAM = "popularBeforeOpenBroadcastStatusProgramListState"

        /** ルーキー番組 */
        const val ROOKIE_PROGRAM = "rookieProgramListState"
    }

    /**
     * ニコ生TOPページを取得する関数。
     * コルーチンできるマン助けてこれ例外のときどうすればええん？
     * 注意：というわけで例外処理しないとたまによくタイムアウトします。CoroutineExceptionHandler等使ってね
     * */
    suspend fun getNicoLiveTopPageHTML(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/?header")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * HTMLからJSONを取り出して、番組情報の配列を取得する関数。
     * @param html [getNicoLiveTopPageHTML]のレスポンス。response#body#string()
     * @param jsonObjectName JSONObjectの名前。フォロー中番組なら[FAVOURITE_PROGRAM]です。
     * */
    suspend fun parseJSON(html: String?, jsonObjectName: String) = withContext(Dispatchers.Default) {
        // JSONっぽいのがあるので取り出す。すくれいぴんぐ
        val document = Jsoup.parse(html)
        val json = document.getElementById("embedded-data").getElementsByAttribute("data-props")
        val jsonString = json.attr("data-props")
        val jsonObject = JSONObject(jsonString)
        // JSON解析
        val programs = jsonObject.getJSONObject("view").getJSONObject(jsonObjectName).getJSONArray("programList")
        // 番組取得
        val dataList = arrayListOf<ProgramData>()
        for (i in 0 until programs.length()) {
            val programJSONObject = programs.getJSONObject(i)
            val programId = programJSONObject.getString("id")
            val title = programJSONObject.getString("title")
            val beginAt = programJSONObject.getString("beginAt")
            val endAt = programJSONObject.getString("endAt")
            val communityName = programJSONObject.getJSONObject("socialGroup").getString("name")
            val liveNow = programJSONObject.getString("liveCycle") //放送中か？
            val official = programJSONObject.getString("providerType") == "official" // community / channel は false
            val liveScreenShot = programJSONObject.getJSONObject("screenshotThumbnail").getString("liveScreenshotThumbnailUrl")
            // サムネ。放送中はスクショを取得するけどそれ以外はアイコン取得？
            val thumb = when {
                liveScreenShot == "null" -> programJSONObject.getString("thumbnailUrl")
                liveNow == "ON_AIR" -> programJSONObject.getJSONObject("screenshotThumbnail").getString("liveScreenshotThumbnailUrl")
                else -> programJSONObject.getString("thumbnailUrl")
            }
            // データクラス
            val data = ProgramData(title, communityName, beginAt, endAt, programId, "", liveNow, thumb, official)
            dataList.add(data)
        }
        dataList
    }

}