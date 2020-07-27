package io.github.takusan23.tatimidroid.NicoAPI.NicoVideo

import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask

/**
 * ニコ動の情報取得
 * コルーチンで使ってね
 * */
class NicoVideoHTML {


    // 定期実行（今回はハートビート処理）に必要なやつ
    var heartBeatTimer = Timer()

    /**
     * HTML取得
     * @param eco 「１」を入れるとエコノミー
     * */
    suspend fun getHTML(videoId: String, userSession: String, eco: String = "") = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://www.nicovideo.jp/watch/$videoId?eco=$eco")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).execute()
    }

    /**
     * NicoVideoDataを取得する関数。
     * @param userSession ユーザーセッション
     * @param videoId 動画ID
     * @return 成功しなかった場合はnullが返ってきます。
     * */
    suspend fun getNicoVideoData(videoId: String, userSession: String) = withContext(Dispatchers.IO) {
        // 動画情報取得
        val videoResponse = getHTML(videoId, userSession)
        // 失敗時落とす
        if (!videoResponse.isSuccessful) {
            null
        } else {
            withContext(Dispatchers.Default) {
                // JSONパース
                val jsonObject = parseJSON(videoResponse.body?.string())
                val videoObject = jsonObject.getJSONObject("video")
                // データクラス化
                NicoVideoData(
                    isCache = false,
                    isMylist = false,
                    title = videoObject.getString("title"),
                    videoId = videoId,
                    thum = videoObject.getString("thumbnailURL"),
                    date = postedDateTimeToUnixTime(videoObject.getString("postedDateTime")),
                    viewCount = videoObject.getString("viewCount"),
                    commentCount = jsonObject.getJSONObject("thread").getString("commentCount"),
                    mylistCount = videoObject.getString("mylistCount"),
                    isToriaezuMylist = false,
                    duration = videoObject.getLong("duration")
                )
            }
        }
    }

    /**
     * js-initial-watch-dataのdata-api-dataのJSONをデータクラス（[NicoVideoData]）へ変換する。
     * なんとなくコルーチンです。
     * @param jsonObject [parseJSON]の返り値
     * @param isCache キャッシュならtrue。省略時false
     * */
    suspend fun createNicoVideoData(jsonObject: JSONObject, isCache: Boolean = false) = withContext(Dispatchers.Default) {
        // JSON化
        val videoObject = jsonObject.getJSONObject("video")
        // データクラス化
        NicoVideoData(
            isCache = isCache,
            isMylist = false,
            title = videoObject.getString("title"),
            videoId = videoObject.getString("id"),
            thum = videoObject.getString("thumbnailURL"),
            date = postedDateTimeToUnixTime(videoObject.getString("postedDateTime")),
            viewCount = videoObject.getString("viewCount"),
            commentCount = jsonObject.getJSONObject("thread").getString("commentCount"),
            mylistCount = videoObject.getString("mylistCount"),
            isToriaezuMylist = false,
            duration = videoObject.getLong("duration")
        )
    }

    /**
     * 動画が暗号化されているか
     * dmcInfoが無いときもfalse
     * 暗号化されているときはtrue
     * されてないときはfalse
     * @param json js-initial-watch-dataのdata-api-data
     * */
    fun isEncryption(json: String): Boolean {
        return when {
            JSONObject(json).getJSONObject("video").isNull("dmcInfo") -> false
            JSONObject(json).getJSONObject("video").getJSONObject("dmcInfo")
                .has("encryption") -> true
            else -> false
        }
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
     * ハートビート処理はheartBeat()関数を一度呼んでください。後は勝手にハートビートをPOSTしてくれます。
     *
     * 注意
     * DMCサーバーの動画はハートビート処理が必要。
     * Smileサーバーの動画 例(sm7518470)：ヘッダーにnicohistory?つけないと取得できない。
     *
     * @param jsonObject parseJSON()の返り値
     * @param sessionJSONObject callSessionAPI()の戻り値。Smileサーバーならnullでいいです。
     * @return 動画URL。取得できないときはnullです。
     * */
    fun getContentURI(jsonObject: JSONObject, sessionJSONObject: JSONObject?): String {
        if (!jsonObject.getJSONObject("video").isNull("dmcInfo") || sessionJSONObject != null) {
            // DMCInfoの動画
            val data = sessionJSONObject!!.getJSONObject("data")
            // 動画のリンク
            val contentUrl = data.getJSONObject("session").getString("content_uri")
            return contentUrl
        } else {
            // Smileサーバーの動画
            val url =
                jsonObject.getJSONObject("video").getJSONObject("smileInfo").getString("url")
            return url
        }
    }

    /**
     * DMC サーバー or Smile　サーバー
     * @param jsonObject parseJSON()の戻り値
     * @return dmcサーバーならtrue
     * */
    fun isDMCServer(jsonObject: JSONObject): Boolean {
        return !jsonObject.getJSONObject("video").isNull("dmcInfo")
    }

    /**
     * ハートビート用URL取得。DMCサーバーの動画はハートビートしないと切られてしまうので。
     * 注意　DMCサーバーの動画のみ値が帰ってきます。
     * @param jsonObject parseJSON()の戻り値
     * @param sessionJSONObject callSessionAPI()の戻り値
     * @return DMCの場合はハートビート用URLを返します。Smileサーバーの動画はnull
     * */
    private fun getHeartBeatURL(jsonObject: JSONObject, sessionJSONObject: JSONObject): String? {
        if (!jsonObject.getJSONObject("video").isNull("dmcInfo")) {
            //DMC Info
            val data = sessionJSONObject.getJSONObject("data")
            val id = data.getJSONObject("session").getString("id")
            //サーバーから切られないようにハートビートを送信する
            val url = "https://api.dmc.nico/api/sessions/${id}?_format=json&_method=PUT"
            return url
        } else {
            return null
        }
    }

    /**
     * ハートビートのときにPOSTする中身。
     * @param jsonObject parseJSON()の戻り値
     * @param sessionJSONObject callSessionAPI()の戻り値
     * @return DMCサーバーならPOSTする中身を返します。Smileサーバーならnullです。
     * */
    private fun getSessionAPIDataObject(jsonObject: JSONObject, sessionJSONObject: JSONObject): String? {
        if (!jsonObject.getJSONObject("video").isNull("dmcInfo")) {
            val data = sessionJSONObject.getJSONObject("data")
            return data.toString()
        } else {
            return null
        }
    }

    /**
     * DMCサーバーの動画はAPIをもう一度叩くことで動画URLが取得できる。この関数である
     * Android ニコニコ動画スレでも言われたたけどJSON複雑すぎんかこれ。
     * なおSmileサーバーの動画はHTMLの中のJSONにアドレスがある。でもHeaderにnicoHistoryをつけないとエラー出るので注意。
     * ハートビートは自前で用意してください。40秒間隔でpostHeartBeat()を呼べばいいです。
     *
     *  @param jsonObject parseJSON()の返り値
     *
     *  @param videoQualityId 画質変更時は入れて。例：「archive_h264_4000kbps_1080p」。ない場合はdmcInfoから持ってきます。画質変更する場合のみ利用すればいいと思います。
     *  @param audioQualityId 音質変更時は入れて。例：「archive_aac_192kbps」。ない場合はdmcInfoから持ってきます。音質変更する場合のみ利用すればいいと思います。
     *
     *  @return APIのレスポンス。JSON形式
     * */
    suspend fun callSessionAPI(jsonObject: JSONObject, videoQualityId: String = "", audioQualityId: String = "") = withContext(Dispatchers.IO) {
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
                                    if (videoQualityId.isNotEmpty() && audioQualityId.isNotEmpty()) {
                                        // 画質変更対応Ver
                                        this.remove("video_src_ids")
                                        this.remove("audio_src_ids")
                                        this.put("video_src_ids", JSONArray().put(videoQualityId))
                                        this.put("audio_src_ids", JSONArray().put(audioQualityId))
                                    } else {
                                        // 画質はdmcInfoに任せた！
                                        this.put("video_src_ids", sessionAPI.getJSONArray("videos"))
                                        this.put("audio_src_ids", sessionAPI.getJSONArray("audios"))
                                    }
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
                                    // ログインしないモード対策
                                    val transfer_preset =
                                        sessionAPI.getJSONArray("transfer_presets")
                                            .optString(0, "")
                                    put("transfer_preset", transfer_preset)
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
            jsonObject
        } else {
            null
        }
    }


    /**
     * ハートビート処理を行う。これをしないとサーバーから切られてしまう。最後にdestroy()呼ぶ必要があるのはこれを終了させるため
     * 40秒ごとに送信するらしい。
     * @param jsonObject parseJSON()の戻り値
     * @param sessionAPIJSONObject callSessionAPI()の戻り値
     * */
    fun heartBeat(jsonObject: JSONObject, sessionAPIJSONObject: JSONObject) {
        heartBeatTimer.cancel()
        heartBeatTimer = Timer()
        val heartBeatURL =
            getHeartBeatURL(jsonObject, sessionAPIJSONObject)
        val postData =
            getSessionAPIDataObject(jsonObject, sessionAPIJSONObject)
        heartBeatTimer.schedule(timerTask {
            // ハートビート処理
            postHeartBeat(heartBeatURL, postData) {
                //  println("Angel Beats!")
            }
        }, 0, 40 * 1000)
    }


    /**
     * getthumbinfoを叩く。コルーチン。
     * 再生時間を取得するのに使えます。
     * @param videoId 動画ID
     * @param userSession ユーザーセッション
     * @return 取得失敗時はnull。成功時はResponse
     * */
    fun getThumbInfo(videoId: String, userSession: String): Deferred<Response?> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("https://ext.nicovideo.jp/api/getthumbinfo/$videoId")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            return@async response
        } else {
            return@async null
        }
    }


    /**
     * コメント取得に必要なJSONを作成する関数。
     * @param videoId 動画ID
     * @param userSession ユーザーセッション
     * @param jsonObject js-initial-watch-dataのdata-api-dataのJSON
     * @return 取得失敗時はnull。成功時はResponse
     * */
    private suspend fun makeCommentAPIJSON(videoId: String, userSession: String, jsonObject: JSONObject) = withContext(Dispatchers.Default) {
        /**
         * dmcInfoが存在するかで分ける。たまによくない動画に当たる。ちなみにこいつ無くてもThreadIdとかuser_id取れるけど、
         * 再生時間が取れないので無理。非公式？XML形式で返してくれるコメント取得APIを叩くことにする。
         * 再生時間、JSONの中に入れないといけないっぽい。
         * */
        // userkey
        val userkey = jsonObject.getJSONObject("context").getString("userkey")
        // user_id
        val user_id = if (verifyLogin(jsonObject)) {
            jsonObject.getJSONObject("viewer").getString("id")
        } else {
            ""
        }

        // 動画時間（分）
        // duration(再生時間
        val length = jsonObject.getJSONObject("video").getInt("duration")
        // 必要なのは「分」なので割る
        // そして分に+1している模様
        // 一時間超えでも分を使う模様？66みたいに
        val minute = (length / 60) + 1

        //contentつくる。1000が限界？
        val content = "0-${minute}:100,1000,nicoru:100"

        /**
         * JSONの構成を指示してくれるJSONArray
         * threads[]の中になんのJSONを作ればいいかが書いてある。
         * */
        val commentComposite = jsonObject.getJSONObject("commentComposite").getJSONArray("threads")
        // 投げるJSON
        val postJSONArray = JSONArray()
        for (i in 0 until commentComposite.length()) {
            val thread = commentComposite.getJSONObject(i)
            val thread_id =
                thread.getString("id")  //thread まじでなんでこの管理方法にしたんだ運営・・
            val fork = thread.getInt("fork")    //わからん。
            val isOwnerThread = thread.getBoolean("isOwnerThread")

            // 公式動画のみ？threadkeyとforce_184が必要かどうか
            val isThreadkeyRequired = thread.getBoolean("isThreadkeyRequired")

            // 公式動画のコメント取得に必須なthreadkeyとforce_184を取得する。
            var threadResponse: String? = ""
            var threadkey: String? = ""
            var force_184: String? = ""
            if (isThreadkeyRequired) {
                // 公式動画に必要なキーを取り出す。
                threadResponse = getThreadKeyForce184(thread_id, userSession)
                //なーんかUriでパースできないので仕方なく＆のいちを取り出して無理やり取り出す。
                val andPos = threadResponse?.indexOf("&")
                // threadkeyとforce_184パース
                threadkey =
                    threadResponse?.substring(0, andPos!!)
                        ?.replace("threadkey=", "")
                force_184 =
                    threadResponse?.substring(andPos!!, threadResponse.length)
                        ?.replace("&force_184=", "")
            }

            // threads[]のJSON配列の中に「isActive」がtrueなら次のJSONを作成します
            if (thread.getBoolean("isActive")) {
                val jsonObject = JSONObject().apply {
                    // 投稿者コメントも見れるように。「isOwnerThread」がtrueの場合は「version」を20061206にする？
                    if (isOwnerThread) {
                        put("version", "20061206")
                        put("res_from", -1000)
                    } else {
                        put("version", "20090904")
                    }
                    put("thread", thread_id)
                    put("fork", fork)
                    put("language", 0)
                    put("user_id", user_id)
                    put("with_global", 1)
                    put("score", 1)
                    put("nicoru", 3)
                    put("userkey", userkey)
                    // 公式動画（isThreadkeyRequiredはtrue）はthreadkeyとforce_184必須。
                    // threadkeyのときはもしかするとuserkeyいらない
                    if (isThreadkeyRequired) {
                        put("force_184", force_184)
                        put("threadkey", threadkey)
                    }
                }
                val post_thread = JSONObject().apply {
                    put("thread", jsonObject)
                }
                postJSONArray.put(post_thread)
            }
            // threads[]のJSON配列の中に「isLeafRequired」がtrueなら次のJSONを作成します
            if (thread.getBoolean("isLeafRequired")) {
                val jsonObject = JSONObject().apply {
                    put("thread", thread_id)
                    put("language", 0)
                    put("user_id", user_id)
                    put("content", content)
                    put("scores", 1)
                    put("nicoru", 3)
                    put("userkey", userkey)
                    put("fork", fork) // これ指定するとなんか仕様変更耐えた
                    // 公式動画（isThreadkeyRequiredはtrue）はthreadkeyとforce_184必須。
                    // threadkeyのときはもしかするとuserkeyいらない
                    if (isThreadkeyRequired) {
                        put("force_184", force_184)
                        put("threadkey", threadkey)
                    }
                }
                val thread_leaves = JSONObject().apply {
                    put("thread_leaves", jsonObject)
                }
                postJSONArray.put(thread_leaves)
            }
        }
        postJSONArray
    }

    /**
     * コメント取得API。コルーチン。JSON形式の方です。xmlではない（ニコるくん取れないしCommentJSONParse使い回せない）。
     * コメント取得くっっっっそめんどくせえ
     * @param userSession ユーザーセッション
     * @param jsonObject js-initial-watch-dataのdata-api-dataのJSON
     * @return 取得失敗時はnull。成功時はResponse
     * */
    suspend fun getComment(videoId: String, userSession: String, jsonObject: JSONObject) = withContext(Dispatchers.IO) {
        val postData = makeCommentAPIJSON(videoId, userSession, jsonObject).toString()
            .toRequestBody()
        // リクエスト
        val request = Request.Builder().apply {
            url("https://nmsg.nicovideo.jp/api.json/")
            header("Cookie", "user_session=${userSession}")
            header("User-Agent", "TatimiDroid;@takusan_23")
            post(postData)
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            response
        } else {
            null
        }
    }

    /**
     * コメントJSONをパースする
     * @param responseString JSON
     * @return CommentJSONParseの配列
     * */
    fun parseCommentJSON(json: String, videoId: String): ArrayList<CommentJSONParse> {
        val commentList = arrayListOf<CommentJSONParse>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            if (jsonObject.has("chat") && !jsonObject.getJSONObject("chat").has("deleted")) {
                val commentJSONParse = CommentJSONParse(jsonObject.toString(), "ニコ動", videoId)
                commentList.add(commentJSONParse)
            }
        }
        // 新しい順に並び替え
        commentList.sortBy { commentJSONParse -> commentJSONParse.vpos.toInt() }
        return commentList
    }

    /**
     * 公式動画を取得するには別にAPIを叩く必要がある。この関数ね。コルーチンです。
     * @param threadId video.dmcInfo.thread.thread_id の値
     * @param userSession ユーザーセッション
     * @return 成功時threadKey。失敗時nullです
     * */
    private suspend fun getThreadKeyForce184(thread: String, userSession: String) = withContext(Dispatchers.IO) {
        val url = "https://flapi.nicovideo.jp/api/getthreadkey?thread=$thread"
        val request = Request.Builder()
            .url(url).get()
            .header("Cookie", "user_session=${userSession}")
            .header("User-Agent", "TatimiDroid;@takusan_23")
            .build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            response.body?.string()
        } else {
            null
        }
    }

    /**
     * ハートビートをPOSTする関数。非同期処理です。Smileサーバーならこの処理はいらない？
     * @param url ハートビート用APIのURL
     * @param json getSessionAPIDataObject()の戻り値
     * @param responseFun 成功時に呼ばれます。
     * */
    fun postHeartBeat(url: String?, json: String?, responseFun: () -> Unit) {
        val request = Request.Builder()
            .url(url!!)
            .post(json!!.toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("User-Agent", "TatimiDroid;@takusan_23")
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {

            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    responseFun()
                }
            }
        })
    }

    /**
     * ログイン済みか。ログイン済みでもユーザーセッションは意外に早く無効化されてしまう。（多重ログイン等で）
     * 再ログインするときとかに使って
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return ログイン済みならtrue
     * */
    fun verifyLogin(jsonObject: JSONObject): Boolean {
        return jsonObject.getJSONObject("viewer").getInt("id") != 0
    }

    /**
     * プレミアム会員かどうか返す
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return プレミアム会員ならtrue
     * */
    fun isPremium(jsonObject: JSONObject): Boolean {
        return jsonObject.getJSONObject("viewer").getBoolean("isPremium")
    }

    /**
     * 公式動画かどうかを返す
     * 注意：なお公式動画だからといって全ての公式動画が暗号化とは限らないので、暗号化されてるかどうかは「isEncryption()」を使ってください。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return 公式動画ならtrue。
     * */
    fun isOfficial(jsonObject: JSONObject): Boolean {
        return jsonObject.getJSONObject("video").getBoolean("isOfficial")
    }

    /**
     * ThreadIdを返す。ニコるKey取得とかコメント取得に使って
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @param isCommunity コミュニティの方のidを取得するときはtrue。公式動画のコメント（PC版でコメントリストの下の「チャンネルコメント」のドロップダウンメニューがある）をニコる場合はtrue。省略時は自動で判断します。
     * @return threadId
     * */
    fun getThreadId(jsonObject: JSONObject, isCommunityOrOfficial: Boolean = isOfficial(jsonObject)): String {
        val idsObject = jsonObject.getJSONObject("thread").getJSONObject("ids")
        return if (!isCommunityOrOfficial) {
            idsObject.getString("default") // 普通の動画
        } else {
            idsObject.getString("community") // 公式動画
        }
    }

    /**
     * ユーザーIDを取得する
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return userId
     * */
    fun getUserId(jsonObject: JSONObject): String {
        return jsonObject.getJSONObject("viewer").getString("id")
    }

    /**
     * 選択中の画質を取得する
     * @param sessionAPIJSONObject callSessionAPI()叩いたときのレスポンス
     * @return 画質。例：archive_h264_360p_low
     * */
    fun getCurrentVideoQuality(sessionAPIJSONObject: JSONObject): String? {
        val videoSrcId = sessionAPIJSONObject.getJSONObject("data").getJSONObject("session")
            .getJSONArray("content_src_id_sets").getJSONObject(0).getJSONArray("content_src_ids")
            .getJSONObject(0).getJSONObject("src_id_to_mux").getJSONArray("video_src_ids")
            .getString(0)
        return videoSrcId
    }

    /**
     * 選択中の音質を取得する
     * @param sessionAPIJSONObject callSessionAPI()叩いたときのレスポンス
     * @return 音質。例：archive_aac_192kbps
     * */
    fun getCurrentAudioQuality(sessionAPIJSONObject: JSONObject): String? {
        val audioQualityId = sessionAPIJSONObject.getJSONObject("data").getJSONObject("session")
            .getJSONArray("content_src_id_sets").getJSONObject(0).getJSONArray("content_src_ids")
            .getJSONObject(0).getJSONObject("src_id_to_mux").getJSONArray("audio_src_ids")
            .getString(0)
        return audioQualityId
    }

    /**
     * アスペクト比が 4:3 かどうか
     * DMCサーバーの動画である必要があります。なんかうまく行かない場合はfalseになります
     * @param jsonObject parseJSON()の返り値
     * @param sessionJSONObject callSessionAPI()の返り値
     * @return アスペクト比が4:3ならtrue
     * */
    fun isOldAspectRate(jsonObject: JSONObject, sessionJSONObject: JSONObject): Boolean {
        // DMCサーバーであるか。もうsmileサーバー見かけなくなったけどどうなの？
        if (isDMCServer(jsonObject)) {
            // 選択中の画質
            val currentQuality = getCurrentVideoQuality(sessionJSONObject)
            // 利用可能な画質パース
            val videoQualityList = parseVideoQualityDMC(jsonObject)
            // 選択中の画質を一つずつ見ていく
            for (i in 0 until videoQualityList.length()) {
                val qualityObject = videoQualityList.getJSONObject(i)
                val id = qualityObject.getString("id")
                if (id == currentQuality) {
                    // あった！
                    val width = qualityObject.getJSONObject("resolution").getInt("width")
                    val height = qualityObject.getJSONObject("resolution").getInt("height")
                    // アスペクト比が4:3か16:9か
                    // 4:3 = 1.333... 16:9 = 1.777..
                    val calc = width.toFloat() / height.toFloat()
                    // 小数点第二位を捨てる
                    val round = BigDecimal(calc.toString()).setScale(1, RoundingMode.DOWN).toDouble()
                    return round == 1.3
                }
            }
            return false
        } else {
            return false
        }
    }

    /**
     * video.postedDateTimeの日付をUnixTime(ミリ秒)に変換する
     * */
    fun postedDateTimeToUnixTime(postedDateTime: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        return simpleDateFormat.parse(postedDateTime).time
    }

    /**
     * 画質一覧を返す。
     * 注意：DMCサーバーの動画で使ってね。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * @return video.dmcInfo.quality.videos の値（配列）
     * */
    fun parseVideoQualityDMC(jsonObject: JSONObject): JSONArray {
        return jsonObject.getJSONObject("video").getJSONObject("dmcInfo").getJSONObject("quality")
            .getJSONArray("videos")
    }

    /**
     * 音質一覧を返す
     * */
    fun parseAudioQualityDMC(jsonObject: JSONObject): JSONArray {
        return jsonObject.getJSONObject("video").getJSONObject("dmcInfo").getJSONObject("quality")
            .getJSONArray("audios")
    }

    /**
     * 投稿者のユーザーIDを取得する。
     * ユーザー投稿動画ならユーザーID。
     * 公式動画ならチャンネルID。
     * アカウント消した場合は空文字が返ってきます。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun getUploaderId(jsonObject: JSONObject): String {
        return when {
            !jsonObject.isNull("owner") -> {
                jsonObject.getJSONObject("owner").getString("id") // ユーザーID
            }
            !jsonObject.isNull("channel") -> {
                jsonObject.getJSONObject("channel").getString("globalId") // 公式動画の時はチャンネルIDを
            }
            else -> "" // うｐ主が動画を消さずにアカウント消した場合は owner channel ともにnullになる。（というかアカウント消しても動画は残るんか）
        }
    }

    /**
     * 投稿者の名前を取得する。
     * もしアカウント消えてる場合はからの文字が返ってくる。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun getUploaderName(jsonObject: JSONObject): String {
        return when {
            !jsonObject.isNull("owner") -> {
                jsonObject.getJSONObject("owner").getString("nickname") // ユーザー
            }
            !jsonObject.isNull("channel") -> {
                jsonObject.getJSONObject("channel").getString("name") // 公式動画
            }
            else -> "" // うｐ主が動画を消さずにアカウント消した場合は owner channel ともにnullになる。（というかアカウント消しても動画は残るんか）
        }
    }

    /**
     * いいね済みかどうかを取得する。
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun isLiked(jsonObject: JSONObject): Boolean {
        return jsonObject.getJSONObject("context").getBoolean("isLiked")
    }

    /**
     * いいねを変更をJSONに適用する関数。
     * @param isLiked いいねしてればtrue
     * @param jsonObject js-initial-watch-dataのdata-api-dataの値
     * */
    fun setLiked(jsonObject: JSONObject, isLiked: Boolean) {
        jsonObject.getJSONObject("context").remove("isLiked")
        jsonObject.getJSONObject("context").put("isLiked", isLiked)
    }

    /**
     * 終了時に呼んで
     * */
    fun destory() {
        heartBeatTimer.cancel()
    }

}