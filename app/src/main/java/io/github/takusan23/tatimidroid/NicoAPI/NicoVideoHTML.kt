package io.github.takusan23.tatimidroid.NicoAPI

import android.os.Handler
import android.provider.Settings
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.util.*
import kotlin.concurrent.timerTask

/**
 * ニコ動の情報取得
 * コルーチンで使ってね
 * */
class NicoVideoHTML {

    // 定期実行（今回はハートビート処理）に必要なやつ
    val handler = Handler()
    lateinit var runnable: Runnable
    var isDestory = false

    /**
     * HTML取得
     * */
    fun getHTML(videoId: String, userSession: String): Deferred<Response?> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://www.nicovideo.jp/watch/$videoId")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }


    /**
     * レスポンスヘッダーのSet-Cookieの中からnicohistoryを取得する関数
     * @param response getHTML()の返り値
     * @return nicohistoryの値。見つからないときはnull
     * */
    fun getNicoHistory(response: Response?): String? {
        val setCookie = response?.headers("Set-Cookie")
        setCookie?.forEach {
            if (it.contains("nicohistory")) {
                return it
            }
        }
        return null
    }

    /**
     * js-initial-watch-dataからdata-api-dataのJSONを取る関数
     * */
    fun parseJSON(html: String?): JSONObject {
        val document = Jsoup.parse(html)
        val json = document.getElementById("js-initial-watch-data").attr("data-api-data")
        return JSONObject(json)
    }

    /**
     * 動画URL取得APIを叩く。DMCとSmileサーバーと別の処理をしないといけないけどこの関数が隠した
     * あとDMCサーバーの動画はハートビート処理（サーバーに「動画見てますよ～」って送るやつ）もやっときますね。
     *
     * 注意
     * DMCサーバーの動画はハートビート処理が必要。なおこの関数呼べば勝手にハートビート処理やります。
     * Smileサーバーの動画 例(sm7518470)：ヘッダーにnicohistory?つけないと取得できない。
     *
     * @param jsonObject parseJSON()の返り値
     * @return 動画URL。取得できないときはnullです。
     * */
    fun getContentURI(jsonObject: JSONObject): Deferred<String?> = GlobalScope.async {
        if (!jsonObject.getJSONObject("video").isNull("dmcInfo")) {
            val contentUrl = callSessionAPI(jsonObject).await()
            return@async contentUrl
        } else {
            val url =
                jsonObject.getJSONObject("video").getJSONObject("smileInfo").getString("url")
            return@async url
        }
    }

    /**
     * DMCサーバーの動画はAPIをもう一度叩くことで動画URLが取得できる。この関数である
     * Android ニコニコ動画スレでも言われたたけどJSON複雑すぎんかこれ。
     * なおSmileサーバーの動画はHTMLの中のJSONにアドレスがある。でもHeaderにnicoHistoryをつけないとエラー出るので注意。
     * あと勝手にハートビート処理しますね。
     *
     *  @param jsonObject parseJSON()の返り値
     *  @return 動画URL。取れないときはnullなので注意ですよ
     * */
    private fun callSessionAPI(jsonObject: JSONObject): Deferred<String?> = GlobalScope.async {
        val dmcInfo = jsonObject.getJSONObject("video").getJSONObject("dmcInfo")
        val sessionAPI = dmcInfo.getJSONObject("session_api")
        //JSONつくる
        val sessionPOSTJSON = JSONObject().apply {
            put("session", JSONObject().apply {
                put("recipe_id", sessionAPI.getString("recipe_id"))
                put("content_id", sessionAPI.getString("content_id"))
                put("content_type", "movie")
                put("content_src_id_sets", JSONArray().apply {
                    this.put(JSONObject().apply {
                        this.put("content_src_ids", JSONArray().apply {
                            this.put(JSONObject().apply {
                                this.put("src_id_to_mux", JSONObject().apply {
                                    this.put("video_src_ids", sessionAPI.getJSONArray("videos"))
                                    this.put("audio_src_ids", sessionAPI.getJSONArray("audios"))
                                })
                            })
                        })
                    })
                })
                put("timing_constraint", "unlimited")
                put("keep_method", JSONObject().apply {
                    put("heartbeat", JSONObject().apply {
                        put("lifetime", 120000)
                    })
                })
                put("protocol", JSONObject().apply {
                    put("name", "http")
                    put("parameters", JSONObject().apply {
                        put("http_parameters", JSONObject().apply {
                            put("parameters", JSONObject().apply {
                                put("http_output_download_parameters", JSONObject().apply {
                                    put("use_well_known_port", "yes")
                                    put("use_ssl", "yes")
                                    put("transfer_preset", "standard2")
                                })
                            })
                        })
                    })
                })
                put("content_uri", "")
                put("session_operation_auth", JSONObject().apply {
                    put("session_operation_auth_by_signature", JSONObject().apply {
                        put("token", sessionAPI.getString("token"))
                        put("signature", sessionAPI.getString("signature"))
                    })
                })
                put("content_auth", JSONObject().apply {
                    put("auth_type", "ht2")
                    put("content_key_timeout", sessionAPI.getInt("content_key_timeout"))
                    put("service_id", "nicovideo")
                    put("service_user_id", sessionAPI.getString("service_user_id"))
                })
                put("client_info", JSONObject().apply {
                    put("player_id", sessionAPI.getString("player_id"))
                })
                put("priority", sessionAPI.getDouble("priority"))
            })
        }
        //POSTする
        val requestBody =
            sessionPOSTJSON.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://api.dmc.nico/api/sessions?_format=json")
            .post(requestBody)
            .addHeader("User-Agent", "TatimiDroid;@takusan_23")
            .addHeader("Content-Type", "application/json")
            .build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val responseString = response.body?.string()
            val jsonObject = JSONObject(responseString)
            val data = jsonObject.getJSONObject("data")
            val id = data.getJSONObject("session").getString("id")
            //サーバーから切られないようにハートビートを送信する
            val url = "https://api.dmc.nico/api/sessions/${id}?_format=json&_method=PUT"
            heatBeat(url, data.toString())
            //動画のリンク
            val content_uri = data.getJSONObject("session").getString("content_uri")
            return@async content_uri
        } else {
            return@async null
        }
    }

    /**
     * ハートビート処理を行う。
     * 40秒ごとに送信するらしい。POSTする内容はsession_apiでAPI叩いた後のJSONのdataの中身。
     * jsonの中身全てではない。
     * @param url ハートビート用URL
     * @param json
     * */
    private fun heatBeat(url: String, json: String) {
        runnable = Runnable {
            println("ハートビート : $isDestory")
            if (!isDestory) {
                val request = Request.Builder()
                    .url(url)
                    .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
                    .addHeader("User-Agent", "NicoHome;@takusan_23")
                    .build()
                val okHttpClient = OkHttpClient()
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {

                    }

                    override fun onResponse(call: Call, response: Response) {
                        println("ハートビート ${response.code}")
                    }
                })
                handler.postDelayed(runnable, 40 * 1000)
            }
        }
        handler.postDelayed(runnable, 40 * 1000)

    }

    /**
     * 終了時に呼んで
     * */
    fun destory() {
        isDestory = true
        if (::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }

}