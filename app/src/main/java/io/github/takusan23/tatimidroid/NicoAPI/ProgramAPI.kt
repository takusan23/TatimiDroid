package io.github.takusan23.tatimidroid.NicoAPI

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import okhttp3.*
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 番組検索系
 * */
class ProgramAPI(val context: Context?) {

    /**
     * フォロー中
     * */
    fun getFollowProgram(error: (() -> Unit)?, responseFun: (ArrayList<ProgramData>) -> Unit) {
        // TOPページ取得
        getNicoLiveTopPage(error) {
            responseFun(parseJSON(it, "favoriteProgramListState"))
        }
    }

    /**
     * ニコレポ
     * */
    fun getNicorepo(error: (() -> Unit)?, responseFun: (Response, ArrayList<ProgramData>) -> Unit) {
        val request = Request.Builder()
            .url("https://www.nicovideo.jp/api/nicorepo/timeline/my/all?client_app=pc_myrepo")
            .header("Cookie", "user_session=${getUserSession()}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (error != null) {
                    error()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataList = arrayListOf<ProgramData>()
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(response.body?.string())
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
                                val time = context?.getString(R.string.nicorepo)
                                val timeshift = parseTime(program.getString("beginAt"))
                                val liveId = program.getString("id")
                                val thum =
                                    community.getJSONObject("thumbnailUrl").getString("small")
                                //変換
                                val simpleDateFormat =
                                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                                val date_calender =
                                    simpleDateFormat.parse(program.getString("beginAt"))
                                val calender = Calendar.getInstance(TimeZone.getDefault())
                                calender.time = date_calender
                                val beginAt = calender.time.time.toString()
                                val data =
                                    ProgramData(title, name, beginAt, beginAt, liveId, name, "Begun", thum)
                                dataList.add(data)
                            }
                        }
                    }
                    responseFun(response, dataList)
                } else if (response.code == 500) {
                    // 再ログイン
                    val niconicoId = response.headers["x-niconico-id"]
                    if (niconicoId == null) {
                        NicoLogin.login(context) {
                            // もういっかい
                            getNicorepo(error, responseFun)
                        }
                    }
                }
            }
        })
    }

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


    /**
     * おすすめ番組
     * */
    fun getRecommend(error: (() -> Unit)?, responseFun: (ArrayList<ProgramData>) -> Unit) {
        // TOPページ取得
        getNicoLiveTopPage(error) {
            // JSONパース
            responseFun(parseJSON(it, "recommendedProgramListState"))
        }
    }

    /**
     * ランキング
     * */
    fun getRanking(error: (() -> Unit)?, responseFun: (Response, ArrayList<ProgramData>) -> Unit) {
        val request = Request.Builder().apply {
            url("https://sp.live.nicovideo.jp/ranking")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=${getUserSession()}")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (error != null) {
                    error()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataList = arrayListOf<ProgramData>()
                if (response.isSuccessful) {
                    val document = Jsoup.parse(response.body?.string())
                    //JSONっぽいのがあるので取り出す
                    val json = document.getElementsByTag("script").get(5)
                    var json_string = URLDecoder.decode(json.html(), "utf-8")
                    // いらない部分消す
                    json_string = json_string.replace("window.__initial_state__ = \"", "")
                    json_string =
                        json_string.replace("window.__public_path__ = \"https://nicolive.cdn.nimg.jp/relive/sp/\";", "")
                    json_string =
                        json_string.replace("\";", "")
                    val jsonObject = JSONObject(json_string)
                    //JSON解析
                    val programs =
                        jsonObject.getJSONObject("pageContents").getJSONObject("ranking")
                            .getJSONObject("rankingPrograms")
                            .getJSONArray("rankingPrograms")
                    for (i in 0 until programs.length()) {
                        val jsonObject = programs.getJSONObject(i)
                        val programId = jsonObject.getString("id")
                        val title = jsonObject.getString("title")
                        val beginAt = jsonObject.getString("beginAt")
                        // val endAt = jsonObject.getString("endAt")
                        val communityName = jsonObject.getString("socialGroupName")
                        val liveNow = jsonObject.getString("liveCycle") //放送中か？
                        val rank = jsonObject.getString("rank")
                        val thum = jsonObject.getString("thumbnailUrl")
                        // データクラス
                        val data =
                            ProgramData(title, communityName, beginAt, beginAt, programId, "", liveNow, thum)
                        dataList.add(data)
                    }
                    responseFun(response, dataList)
                } else {
                    showToast(context?.getString(R.string.error) + "\n" + response.code)
                }
            }
        })
    }


    val NICONAMA_GAME_PLAYING =
        "https://api.spi.nicovideo.jp/v1/matching/profiles/targets/frontend/statuses/playing?limit=30"
    val NICONAMA_GAME_MATCHING =
        "https://api.spi.nicovideo.jp/v1/matching/profiles/targets/frontend/statuses/matching?limit=20"

    /**
     * ニコ生ゲームプレイ中
     * @param url URL。NICONAMA_GAME_PLAYING か NICONAMA_GAME_MATCHING
     * */
    fun getNicoNamaGame(url: String, error: (() -> Unit)?, responseFun: (Response, ArrayList<ProgramData>) -> Unit) {
        val request = Request.Builder().apply {
            url(url) // なんと！APIがある！
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=${getUserSession()}")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (error != null) {
                    error()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val dataList = arrayListOf<ProgramData>()
                if (response.isSuccessful) {
                    // JSONパース
                    val jsonObject = JSONObject(response.body?.string())
                    val data = jsonObject.getJSONArray("data")
                    for (i in 0 until data.length()) {
                        val program = data.getJSONObject(i)
                        val contentId = program.getString("contentId") // 番組ID
                        val contentTitle = program.getString("contentTitle") // 番組名
                        val startedAt = program.getString("startedAt") // 番組開始時間
                        val providerName = program.getString("providerName") // 放送者
                        val productName = program.getString("productName") // ゲーム名
                        val contentThumbnailUrl = program.getString("contentThumbnailUrl")
                        // ISO 8601の形式からUnixTimeへ変換（Adapterの方で必要）
                        val simpleDateFormat =
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                        val date_calender = simpleDateFormat.parse(startedAt)
                        val calender = Calendar.getInstance(TimeZone.getDefault())
                        calender.time = date_calender
                        // データクラス
                        val data =
                            ProgramData("$contentTitle\n\uD83C\uDFAE：$productName", providerName, calender.time.time.toString(), calender.time.time.toString(), contentId, providerName, "ON_AIR", contentThumbnailUrl)
                        dataList.add(data)
                    }
                    responseFun(response, dataList)
                } else {
                    showToast(context?.getString(R.string.error) + "\n" + response.code)
                }
            }
        })
    }

    /**
     * 放送中の注目番組取得。公式放送
     * */
    fun getFocusProgramListState(error: (() -> Unit)?, responseFun: (ArrayList<ProgramData>) -> Unit) {
        // TOPページ取得
        getNicoLiveTopPage(error) {
            // JSONパース
            responseFun(parseJSON(it, "focusProgramListState"))
        }
    }

    /**
     * これからの注目番組取得。
     * */
    fun getRecentJustBeforeBroadcastStatusProgramListState(error: (() -> Unit)?, responseFun: (ArrayList<ProgramData>) -> Unit) {
        // TOPページ取得
        getNicoLiveTopPage(error) {
            // JSONパース
            responseFun(parseJSON(it, "recentJustBeforeBroadcastStatusProgramListState"))
        }
    }

    /**
     * 人気の予約されている番組取得
     * */
    fun getPopularBeforeOpenBroadcastStatusProgramListState(error: (() -> Unit)?, responseFun: (ArrayList<ProgramData>) -> Unit) {
        // TOPページ取得
        getNicoLiveTopPage(error) {
            // JSONパース
            responseFun(parseJSON(it, "popularBeforeOpenBroadcastStatusProgramListState"))
        }
    }

    /**
     * ニコ生のTOPページ取得
     * */
    private fun getNicoLiveTopPage(error: (() -> Unit)?, responseFun: (JSONObject) -> Unit) {
        val request = Request.Builder().apply {
            url("https://live.nicovideo.jp/?header") // なんと！APIがある！
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=${getUserSession()}")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (error != null) {
                    error()
                }
                showToast(context?.getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // ログイン状態がなければ再ログイン
                    val niconicoId = response.headers["x-niconico-id"]
                    if (niconicoId != null) {
                        val document = Jsoup.parse(response.body?.string())
                        // JSONっぽいのがあるので取り出す
                        val json = document.getElementById("embedded-data")
                            .getElementsByAttribute("data-props")
                        val json_string = json.attr("data-props")
                        val jsonObject = JSONObject(json_string)
                        responseFun(jsonObject)
                    } else {
                        // 再ログイン
                        NicoLogin.login(context) {
                            showToast(context?.getString(R.string.relogin))
                            getNicoLiveTopPage(error, responseFun)
                        }
                    }
                } else {
                    showToast(context?.getString(R.string.error) + "\n" + response.code)
                }
            }
        })
    }

    /**
     * ニコ生TOPのJSONは共通なのでまとめる
     * @param ニコ生TOPJSON
     * @param parseObjectName 項目。 favoriteProgramListState など
     * */
    private fun parseJSON(jsonObject: JSONObject, parseObjectName: String): ArrayList<ProgramData> {
        val dataList = arrayListOf<ProgramData>()
        // JSON解析
        val programs =
            jsonObject.getJSONObject("view").getJSONObject(parseObjectName)
                .getJSONArray("programList")
        for (i in 0 until programs.length()) {
            val jsonObject = programs.getJSONObject(i)
            val programId = jsonObject.getString("id")
            val title = jsonObject.getString("title")
            val beginAt = jsonObject.getString("beginAt")
            val endAt = jsonObject.getString("endAt")
            val communityName =
                jsonObject.getJSONObject("socialGroup").getString("name")
            val liveNow = jsonObject.getString("liveCycle") //放送中か？
            val official =
                jsonObject.getString("providerType") == "official" // community / channel は false

            val liveScreenShot = jsonObject.getJSONObject("screenshotThumbnail")
                .getString("liveScreenshotThumbnailUrl")
            // サムネ。放送中はスクショを取得するけどそれ以外はアイコン取得？
            val thum = when {
                liveScreenShot == "null" -> {
                    jsonObject.getString("thumbnailUrl")
                }
                liveNow == "ON_AIR" -> {
                    jsonObject.getJSONObject("screenshotThumbnail")
                        .getString("liveScreenshotThumbnailUrl")
                }
                else -> {
                    jsonObject.getString("thumbnailUrl")
                }
            }
            // データクラス
            val data =
                ProgramData(title, communityName, beginAt, endAt, programId, "", liveNow, thum, official)
            dataList.add(data)
        }
        return dataList
    }

    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserSession(): String? {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences?.getString("user_session", "")
    }
}