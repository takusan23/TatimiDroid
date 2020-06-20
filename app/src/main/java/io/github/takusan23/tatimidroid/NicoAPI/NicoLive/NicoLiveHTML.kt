package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import android.os.Handler
import android.os.Looper
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.ScheduleDataClass
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.StatisticsDataClass
import io.github.takusan23.tatimidroid.NicoAPI.ProgramData
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.Console
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

/**
 *  ニコ生のHTMLページを取得したりWebSocketに接続したりするクラス。
 *  コルーチンで使ってね
 * */
class NicoLiveHTML {

    // 視聴セッションWebSocket
    lateinit var nicoLiveWebSocketClient: WebSocketClient

    // コメント送信用WebSocket
    lateinit var commentPOSTWebSocketClient: WebSocketClient

    /** 低遅延モードが有効ならtrue */
    var isLowLatency = true

    // 画質設定。最初の接続でのみ使われる(WebSocket接続成功時に送る。それ以外では使わない)
    var startQuality = "high"

    // 視聴継続メッセージ送信用Timer
    var timer = Timer()

    // postKey取るときに使うthreadId
    var threadId = ""

    // コメント投稿で使う。isPremiumとかはinitNicoLiveData()を呼ばないとこの値は入りません！！！
    var postCommentText = ""
    var postCommentCommand = ""
    var premium = 0     // プレ垢なら1
    var isPremium = false   // 550円課金してるならtrue
    var userId = ""     // ユーザーID
    var isOfficial = false // 公式番組ならtrue
    var liveId = ""

    // 番組情報関係。initNicoLiveData()を呼ばないとこの値は入りません！！！
    var programTitle = ""   // 番組ID
    var communityId = ""    // コミュID
    var communityName = ""    // コミュ名
    var supplierName = ""    // 放送者名
    var status = ""         // ON_AIR　とか
    var thumb = ""          // サムネイル
    var programOpenTime = 0L    // 番組開始時間
    var programStartTime = 0L    // 番組開場時間
    var programEndTime = 0L     // 番組終了時間

    // 匿名コメントならtrue
    var isTokumeiComment = true

    // コメント投稿で使うチケット
    private var commentTicket = ""

    // 現在の画質
    private var currentQuality = "super_low"


    /**
     * HTMLの中からJSONを見つけてくる関数
     * @param response HTML
     * */
    fun nicoLiveHTMLtoJSONObject(response: String?): JSONObject {
        val html = Jsoup.parse(response)
        val json = html.getElementById("embedded-data").attr("data-props")
        return JSONObject(json)
    }

    /**
     * ISO 8601 -> わかりやすいやつに
     * */
    fun iso8601ToFormat(unixTime: Long): String {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN)
        return simpleDateFormat.format(unixTime * 1000)
    }

    /**
     * ニコ生視聴ページのHTMLを取得する関数。
     * @param liveId 生放送ID
     * @param userSession ユーザーセッション
     * @param isLogin ログイン状態で行うか。省略時はログイン状態で行います（true）。
     * */
    fun getNicoLiveHTML(liveId: String, userSession: String?, isLogin: Boolean = true): Deferred<Response> = GlobalScope.async {
        val url = "https://live2.nicovideo.jp/watch/$liveId"
        val request = Request.Builder().apply {
            get()
            url(url)
            addHeader("User-Agent", "TatimiDroid;@takusan_23")
            if (isLogin) {
                // ログイン時はユーザーセッションを入れる。
                addHeader("Cookie", "user_session=$userSession")
            }
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        return@async response
    }

    /**
     * getPlayerStatusを叩く
     * @param liveId 生放送ID
     * @param userSession ユーザーセッション
     * @return OkhttpのResponse
     * */
    fun getPlayerStatus(liveId: String, userSession: String?): Deferred<Response> =
        GlobalScope.async {
            val url = "https://live.nicovideo.jp/api/getplayerstatus/$liveId"
            val request = Request.Builder().apply {
                get()
                url(url)
                addHeader("User-Agent", "TatimiDroid;@takusan_23")
                addHeader("Cookie", "user_session=$userSession")

            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * 動画情報などをセットする。コメント投稿もこれを呼ばないと使えないので呼んでね。
     * 視聴セッション接続前に呼ぶと忘れないで済む
     * @param jsonObject nicoLiveHTMLtoJSONObject()の値
     * */
    fun initNicoLiveData(nicoLiveJSON: JSONObject) {
        liveId = nicoLiveJSON.getJSONObject("program").getString("nicoliveProgramId")
        isPremium = isPremium(nicoLiveJSON)
        premium = if (isPremium) {
            1
        } else {
            0
        }
        userId = getNiconicoID(nicoLiveJSON) ?: ""
        programOpenTime = nicoLiveJSON.getJSONObject("program").getLong("openTime")
        programStartTime = nicoLiveJSON.getJSONObject("program").getLong("beginTime")
        programEndTime = nicoLiveJSON.getJSONObject("program").getLong("endTime")
        programTitle = nicoLiveJSON.getJSONObject("program").getString("title")
        communityId = if (nicoLiveJSON.has("community")) {
            nicoLiveJSON.getJSONObject("community").getString("id")
        } else {
            nicoLiveJSON.getJSONObject("channel").getString("id")
        }
        status = nicoLiveJSON.getJSONObject("program").getString("status")
        supplierName =
            nicoLiveJSON.getJSONObject("program").getJSONObject("supplier").getString("name")
        communityName = nicoLiveJSON.getJSONObject("socialGroup").getString("name")
        thumb = nicoLiveJSON.getJSONObject("program").getJSONObject("thumbnail").getString("small")
        isOfficial = isOfficial(nicoLiveJSON)
    }

    /**
     * 番組情報詰めたクラスを返す。
     * 内部でinitNicoLiveData()を呼んでパースしてます。
     * @param nicoLiveJSON nicoLiveHTMLtoJSONObject()の値
     * */
    fun getProgramData(nicoLiveJSON: JSONObject): ProgramData {
        initNicoLiveData(nicoLiveJSON)
        return ProgramData(programTitle, communityName, programOpenTime.toString(), programEndTime.toString(), liveId, supplierName, status, thumb, isOfficial)
    }

    /**
     * ログインが有効かどうか
     * @param response getNicoLiveHTML()の戻り値
     * @return x-niconico-idがあればtrue。なければ（ログインが切れていれば）false
     * */
    fun hasNiconicoID(response: Response): Boolean {
        return response.headers["x-niconico-id"] != null
    }

    /**
     * ニコ生視聴セッションWebSocketへ接続する関数。
     * @param jsonObject nicoLiveHTMLtoJSONObject()の戻り値
     * @param onMessageFun 第一引数はcommand（messageServerUriとか）。第二引数はJSONそのもの
     * */
    fun connectionWebSocket(jsonObject: JSONObject, onMessageFun: (String, String) -> Unit) {
        val site = jsonObject.getJSONObject("site")
        val relive = site.getJSONObject("relive")
        // WebSocketアドレス
        val webSocketUrl = relive.getString("webSocketUrl")
        // 番組情報
        val program = jsonObject.getJSONObject("program")
        // broadcastId
        val broadcastId = program.getString("broadcastId")
        connectionNicoLiveWebSocket(webSocketUrl, broadcastId, onMessageFun)
    }

    /**
     * ニコ生の視聴に必要なデータを流してくれるWebSocketに接続する
     * @param webSocketUrl connectionWebSocket()のソース読め
     * @param broadcastId connectionWebSocket()のソース読め
     * @param onMessageFun 第一引数はcommand（messageServerUriとか）。第二引数はJSONそのもの。なにもない時がある（なんだろうね？）
     * */
    private fun connectionNicoLiveWebSocket(webSocketUrl: String, broadcastId: String, onMessageFun: (String, String) -> Unit) {
        nicoLiveWebSocketClient = object : WebSocketClient(URI(webSocketUrl)) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // 視聴セッションWebSocketに接続したら最初に送らないといけないJSONがあるので送りつける。2020/06/02から送るJSONの中身が変わっているぞおい勝手に変更するな
                val jsonObject = JSONObject().apply {
                    put("type", "startWatching")
                    put("data", JSONObject().apply {
                        put("stream", JSONObject().apply {
                            put("quality", startQuality) // 画質
                            put("protocol", "hls")
                            put("latency", toLatencyString()) // 低遅延。highかlowになった。Booleanに戻せよ
                            put("chasePlay", false)
                        })
                        put("room", JSONObject().apply {
                            put("protocol", "webSocket")
                            put("commentable", true)
                        })
                        put("reconnect", false)
                    })
                }
/*
                //もう一個
                val secondObject = JSONObject()
                secondObject.put("type", "watch")
                val secondbodyObject = JSONObject()
                secondbodyObject.put("command", "playerversion")
                val paramsArray = JSONArray()
                paramsArray.put("leo")
                secondbodyObject.put("params", paramsArray)
                secondObject.put("body", secondbodyObject)
*/

                // JSON送信！
                //  this.send(secondObject.toString())
                this.send(jsonObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    // type。streamとかcommentroomとか
                    val type = JSONObject(message).getString("type")
                    onMessageFun(type, message)
                    when {
                        type == "stream" -> {
                            currentQuality = getCurrentQuality(message)
                        }
                        type == "room" -> {
                            threadId = getCommentServerThreadId(message)
                        }
                        type == "postkey" -> {
                            // コメント投稿時に使うpostkeyを受け取る
                            val postKey = getPostKey(message)
                            if (postKey != null) {
                                // コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
                                // 100=1秒らしい。 例：300->3秒
                                val unixTime = System.currentTimeMillis() / 1000L
                                val vpos = (unixTime - programOpenTime) * 100
                                // println(vpos)
                                val jsonObject = JSONObject()
                                val chatObject = JSONObject()
                                chatObject.put("thread", threadId)
                                // 視聴用セッションWebSocketからとれる
                                chatObject.put("vpos", vpos)
                                // 番組情報取得で取得した値 - = System.currentTimeMillis() UNIX時間
                                // 匿名が有効の場合は184をつける
                                if (isTokumeiComment) {
                                    postCommentCommand = "184 $postCommentCommand"
                                }
                                chatObject.put("mail", postCommentCommand)
                                chatObject.put("ticket", commentTicket)
                                //視聴用セッションWebSocketからとれるコメントが流れるWebSocketに接続して最初に必要な情報を送ったら取得できる
                                chatObject.put("content", postCommentText)
                                chatObject.put("postkey", postKey)
                                //この２つ、ユーザーIDとプレミアム会員かどうか。これどうやってとる？gatPlayerStatus？　もしgetPlayerStatusなくなってもHTML解析すればとれそう？
                                chatObject.put("premium", premium)
                                chatObject.put("user_id", userId)
                                jsonObject.put("chat", chatObject)
                                // 投稿
                                commentPOSTWebSocketClient.send(jsonObject.toString())
                            }
                        }
                    }
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        //それと別に30秒間隔で視聴を続けてますよメッセージを送信する必要がある模様
        timer = Timer()
        timer.schedule(30000, 30000) {
            if (!nicoLiveWebSocketClient.isClosed) {
                // ハートビート処理も2020/06/02の更新で送る内容が変わってる
                val pongObject = JSONObject().apply {
                    put("type", "pong")
                }
                val keepSeatObject = JSONObject().apply {
                    put("type", "keepSeat")
                }
                // それぞれを３０秒ごとに送る
                // なお他の環境と同時に視聴すると片方切断される（片方の画面に同じ番組を開くとだめ的なメッセージ出る）
                nicoLiveWebSocketClient.send(pongObject.toString())
                nicoLiveWebSocketClient.send(keepSeatObject.toString())
            }
        }
        //接続
        nicoLiveWebSocketClient.connect()
    }

    /**
     * コメント送信用WebSocketに接続する。今見てる部屋のWebSocketに接続する
     * @param url getCommentServerWebSocketAddress()
     * @param threadId getCommentServerThreadId()
     * @param userId ニコニコのユーザーID
     * */
    fun connectionCommentPOSTWebSocket(url: String, threadId: String, userId: String, onMessageFun: (String) -> Unit) {
        //これはプロトコルの設定が必要
        val protocol = Draft_6455(
            Collections.emptyList(),
            Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?
        )
        commentPOSTWebSocketClient = object : WebSocketClient(URI(url), protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", threadId)
                jsonObject.put("score", 1)
                jsonObject.put("nicoru", 0)
                jsonObject.put("with_global", 1)
                jsonObject.put("fork", 0)
                jsonObject.put("user_id", userId)
                jsonObject.put("res_from", 0)
                sendJSONObject.put("thread", jsonObject)
                commentPOSTWebSocketClient.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    onMessageFun(message)
                    if (message.contains("ticket")) {
                        // コメント投稿に使うticketを取得する
                        val jsonObject = JSONObject(message)
                        val thread = jsonObject.getJSONObject("thread")
                        commentTicket = thread.getString("ticket")
                    }
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        //忘れがちな接続
        commentPOSTWebSocketClient.connect()
    }

    /**
     * コメント投稿用WebSocketへコメントを送信する。これは今の部屋のコメントサーバーに投げる
     * @param comment コメント内容。「よお」とか
     * @param command コマンド内容。「shita」とか
     * */
    fun sendPOSTWebSocketComment(comment: String, command: String) {
        postCommentText = comment
        postCommentCommand = command
        // postKeyを視聴用セッションWebSocketに払い出してもらう
        // PC版ニコ生だとコメントを投稿のたびに取得してるので
        // 2020/06/02の更新で{type:"getPostKey"}を送りつけるだけで動くようになる
        // 送信する
        //この後の処理は視聴用セッションWebSocketでpostKeyを受け取る処理に行きます。
        nicoLiveWebSocketClient.send("{\"type\":\"getPostkey\"}")
    }

    /**
     * コメント投稿APIを叩く。これはニコキャスのAPIを使ってコメントを投稿するので多分アリーナに投げられる。
     * 注意　これは非同期処理です。（これはコルーチンにする必要ないか。）
     * @param comment コメント内容。
     * @param command コマンド内容。
     * @param liveId 生放送ID
     * @param userSession ユーザーセッション
     * @param error 失敗時に呼ばれる高階関数。注意点　これはUIスレッドではありません！！！！
     * @param responseFun 成功時に呼ばれる高階関数。注意点　これはUIスレッドれ呼ばれるようになってます！！！！
     * */
    fun sendCommentNicocasAPI(comment: String, command: String, liveId: String, userSession: String?, error: () -> Unit, responseFun: (Response) -> Unit) {
        // コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
        //  100=1秒らしい。 例：300->3秒
        val unixTime = System.currentTimeMillis() / 1000L
        val vpos = (unixTime - programOpenTime) * 100
        val jsonObject = JSONObject()
        jsonObject.put("message", comment)
        var commandText = command
        if (isTokumeiComment) {
            commandText = "184 $command"
        }
        jsonObject.put("command", commandText)
        jsonObject.put("vpos", vpos.toString())

        val requestBodyJSON = jsonObject.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        //println(jsonObject.toString())

        val request = Request.Builder().apply {
            url("https://api.cas.nicovideo.jp/v1/services/live/programs/${liveId}/comments")
            header("Cookie", "user_session=${userSession}")
            header("User-Agent", "TatimiDroid;@takusan_23")
            post(requestBodyJSON)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                error()
            }

            override fun onResponse(call: Call, response: Response) {
                Handler(Looper.getMainLooper()).post {
                    responseFun(response)
                }
            }
        })
    }

    /**
     * ニコニコのユーザーID取得。
     * 重要　コメント投稿モード以外でこの関数を呼ぶとnull出ます。ログインしてないから仕方ないね。
     * @param jsonObject nicoLiveHTMLtoJSONObject()の値。
     * @return ニコニコのユーザーID
     * */
    fun getNiconicoID(jsonObject: JSONObject): String? {
        return if (jsonObject.getJSONObject("user").has("id")) {
            jsonObject.getJSONObject("user").getString("id")
        } else {
            null
        }
    }

    /**
     * プレ垢かどうか
     * 重要　コメント投稿モード以外でこの関数を呼ぶと正しい値が帰ってこないと思います。
     * @param jsonObject nicoLiveHTMLtoJSONObject()の値
     * @return プレ垢ならtrue。そうじゃなければfalse
     * */
    fun isPremium(jsonObject: JSONObject): Boolean {
        return jsonObject.getJSONObject("user").getString("accountType") == "premium"
    }

    /**
     * PostKey取得
     * 注意：typeがpostKeyである必要があります。
     * @param message ニコ生視聴セッションWebSocketから流れてきたJSON文字列
     * @return postKey
     * */
    fun getPostKey(message: String?): String? {
        val jsonObject = JSONObject(message)
        val postKey = jsonObject.getJSONObject("data").getString("value")
        return postKey
    }

    /**
     * 今の部屋のコメントサーバーのWebSocketアドレスの取得関数
     * 注意：typeがroomのとき。
     * @param message ニコ生視聴セッションWebSocketから流れてきたJSON文字列。
     * @return WebSocketのアドレス
     * */
    fun getCommentServerWebSocketAddress(message: String?): String {
        val webSocketUri = JSONObject(message).getJSONObject("data").getJSONObject("messageServer").getString("uri")
        return webSocketUri
    }

    /**
     * 今の部屋のコメントサーバーのスレッドIDの取得関数
     * 注意：typeがroomのとき。
     * @param message ニコ生視聴セッションWebSocketから流れてきたJSON文字列。
     * @return コメント鯖接続に使うthreadId
     * */
    fun getCommentServerThreadId(message: String?): String {
        val threadId = JSONObject(message).getJSONObject("data").getString("threadId")
        return threadId
    }

    /**
     * 今の部屋のコメントサーバーの部屋の名前の取得関数
     * 注意：typeがroomのとき。
     * @param message ニコ生視聴セッションWebSocketから流れてきたJSON文字列。
     * @return 部屋名。立ち見１とか
     * */
    fun getCommentRoomName(message: String?): String {
        val roomName = JSONObject(message).getJSONObject("data").getString("name")
        return roomName
    }

    /**
     * 視聴WebSocketからHLSアドレス取得
     * 注意：typeがstreamのとき。
     * @param message ニコ生視聴セッションWebSocketから流れてきたJSON文字列
     * @return hlsアドレス
     * */
    fun getHlsAddress(message: String?): String? {
        val hlsAddress = JSONObject(message).getJSONObject("data").getString("uri")
        return hlsAddress
    }

    /**
     * 画質一覧を取得する関数。
     * 注意：typeがstreamのとき。
     * @param message ニコ生視聴セッションWebSocketから流れてきたJSON文字列
     * @return 利用可能な画質のJSONArray
     * */
    fun getQualityListJSONArray(message: String?): JSONArray {
        val availableQualities = JSONObject(message).getJSONObject("data").getJSONArray("availableQualities")
        return availableQualities
    }

    /**
     * 現在の画質を取得する関数。
     * 注意：typeがstreamのときのみ利用可能。
     * @param message 画質名。
     * @return 現在の画質
     * */
    fun getCurrentQuality(message: String?): String {
        val quality = JSONObject(message).getJSONObject("data").getString("quality")
        return quality
    }

    /**
     * 総来場者数（運営曰く、アクティブ人数は３０分でコメント０のときが悲しいから付けないらしい）/コメント数/ニコニ広告ポイント/投げ銭ポイント
     * 注意：typeがstatisticsのときのみ
     * @param message ニコ生視聴セッションWebSocketから流れてくるJSON文字列
     * @return 総来場者数/コメント数/投げ銭ポイント/ニコニ広告ポイント数のデータクラス。
     * */
    fun getStatistics(message: String?): StatisticsDataClass {
        val jsonObject = JSONObject(message).getJSONObject("data")
        val adPoints = jsonObject.optInt("adPoints", 0)
        val comments = jsonObject.optInt("comments", 0)
        val giftPoints = jsonObject.optInt("giftPoints", 0)
        val viewers = jsonObject.optInt("viewers", 0)
        return StatisticsDataClass(adPoints, comments, giftPoints, viewers)
    }

    /**
     * 延長検知JSONをパースして終了時刻をフォーマットして返す。
     * 注意：typeがscheduleのときのみ
     * @param message ニコ生視聴セッションWebSocketから流れてくるJSON文字列
     * @param timeFormat 省略可能。時間フォーマットをしていたい場合は入れてね。Javaのフォーマットで頼んだ。
     * @return 終了時間（文字列）
     * */
    fun getScheduleEndTime(message: String?, timeFormat: String = "MM/dd HH:mm:ss"): String? {
        // JSONパース
        val data = JSONObject(message).getJSONObject("data")
        // ISO8601 -> UnixTime
        val begin = toUnixTime(data.getString("begin"))
        val end = toUnixTime(data.getString("end"))
        // 変換する
        val simpleDateFormat = SimpleDateFormat("MM/dd HH:mm:ss")
        val time = simpleDateFormat.format(end)
        return time
    }

    /**
     * 延長検知JSONをパースしてデータクラスに入れる関数。
     * 注意：typeがscheduleのときのみ
     * @param message ニコ生視聴セッションWebSocketから流れてくるJSON文字列
     * @return 番組開始時間/番組終了時間が入ってるデータクラス
     * */
    fun getSchedule(message: String?): ScheduleDataClass {
        // JSONパース
        val data = JSONObject(message).getJSONObject("data")
        // ISO8601 -> UnixTime
        val beginTime = toUnixTime(data.getString("begin"))
        val endTime = toUnixTime(data.getString("end"))
        return ScheduleDataClass(beginTime, endTime)
    }

    /**
     * 公式番組かどうか。
     * @param jsonObject nicoLiveHTMLtoJSONObject()の戻り値
     * @return 公式番組ならtrue
     * */
    fun isOfficial(jsonObject: JSONObject): Boolean {
        val program = jsonObject.getJSONObject("program")
        return program.getString("providerType") == "official"
    }

    /**
     * 低遅延のon/offがBooleanのtrue/falseから文字列のhigh/lowになっているので低遅延有効時はhighを、無効時はlowを返す関数
     * @param islowLatency 低遅延有効時はtrue。そうじゃなければfalse
     * @return true：high / false：low
     * */
    private fun toLatencyString(islowLatency: Boolean = isLowLatency): String {
        return if (islowLatency) {
            "high"
        } else {
            "low"
        }
    }

    /**
     * 画質変更メッセージ送信
     * */
    fun sendQualityMessage(quality: String) {
        val jsonObject = JSONObject().apply {
            put("type", "changeStream")
            put("data", JSONObject().apply {
                put("quality", quality)
                put("protocol", "hls")
                put("latency", toLatencyString())
                put("chasePlay", false)
            })
        }
        //送信
        nicoLiveWebSocketClient.send(jsonObject.toString())
    }

    /**
     * 低遅延モード。
     * @param isLowLatency 低遅延モードをONにする場合はtrue。じゃなければfalse
     * */
    fun sendLowLatency(isLowLatency: Boolean) {
        val jsonObject = JSONObject().apply {
            put("type", "changeStream")
            put("data", JSONObject().apply {
                put("quality", currentQuality)
                put("protocol", "hls")
                put("latency", toLatencyString(isLowLatency))
                put("chasePlay", false)
            })
        }
        // 送信
        nicoLiveWebSocketClient.send(jsonObject.toString())
        this.isLowLatency = isLowLatency
    }

    /**
     * 終了時によんで
     * WebSocket切るので
     * */
    fun destroy() {
        timer.cancel()
        if (::nicoLiveWebSocketClient.isInitialized) {
            nicoLiveWebSocketClient.close()
        }
        if (::commentPOSTWebSocketClient.isInitialized) {
            commentPOSTWebSocketClient.close()
        }
    }

    /**
     * ISO8601の時間フォーマットをUnixTimeへ変換する
     * 注意：ミリ秒です
     * @param time ISO8601で書かれた時間
     * @return UnixTime ms
     * */
    private fun toUnixTime(time: String): Long {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX")
        return simpleDateFormat.parse(time).time
    }

}