package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.Tool.OkHttpClientSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコレポ取得
 * こっちな生放送バージョン
 * */
class NicoLiveNicoRepoAPI {

    /** シングルトンなOkHttpClient */
    private val okHttpClient = OkHttpClientSingleton.okHttpClient

    /**
     * ニコレポ取得APIを叩く。
     * 多分タイムアウトで例外出るからtry/catchで囲えばいいと思う
     * ログイン切れたかどうかは ステータスコートが500 または レスポンスヘッダーにx-niconico-idがない とき。
     * @param userSession ユーザーセッション
     * @return OkHttpのレスポンス
     * */
    suspend fun getNicorepo(userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://www.nicovideo.jp/api/nicorepo/timeline/my/all?client_app=pc_myrepo")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        okHttpClient.newCall(request).execute()
    }

    /**
     * ニコレポ取得APIのレスポンスをパースする。
     * @param jsonString [getNicorepo]のレスポンス。response#body#string()
     * @return ProgramDataの配列
     * */
    suspend fun parseJSON(jsonString: String?) = withContext(Dispatchers.Default) {
        val dataList = arrayListOf<NicoLiveProgramData>()
        val jsonObject = JSONObject(jsonString)
        val data = jsonObject.getJSONArray("data")
        for (i in 0 until data.length()) {
            val nicorepoObject = data.getJSONObject(i)
            //番組開始だけ取得
            if (nicorepoObject.has("program")) {
                if (nicorepoObject.has("community")) {
                    val program = nicorepoObject.getJSONObject("program")
                    val community = nicorepoObject.getJSONObject("community")
                    val title = program.getString("title")
                    val name = community.getString("name")
                    val live = parseTime(program.getString("beginAt"))
                    val timeshift = parseTime(program.getString("beginAt"))
                    val liveId = program.getString("id")
                    val thumb = community.getJSONObject("thumbnailUrl").getString("small")
                    //変換
                    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    val date_calender = simpleDateFormat.parse(program.getString("beginAt"))
                    val calender = Calendar.getInstance(TimeZone.getDefault())
                    calender.time = date_calender
                    val beginAt = calender.time.time.toString()
                    val data = NicoLiveProgramData(title, name, beginAt, beginAt, liveId, name, "Begun", thumb)
                    dataList.add(data)
                }
            }
        }
        dataList
    }

    // 時間フォーマット直すやつ
    private fun parseTime(startTime: String): String {
        //SimpleDataFormat
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val date_calender = simpleDateFormat.parse(startTime)
        val calender = Calendar.getInstance(TimeZone.getDefault())
        calender.time = date_calender
        val afterSimpleDateFormat = SimpleDateFormat("MM/dd (EEE) HH:mm", Locale.JAPAN)
        val time = afterSimpleDateFormat.format(date_calender)
        return time
    }

}