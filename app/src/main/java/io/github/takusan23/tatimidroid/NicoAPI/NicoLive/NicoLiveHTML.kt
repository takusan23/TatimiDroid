package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import android.os.Handler
import android.os.Looper
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
import java.io.IOException
import java.lang.Exception
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

    // 低遅延モードが有効ならtrue
    var isLowLatency = true

    // 視聴継続メッセージ送信用Timer
    var timer = Timer()

    // postKey取るときに使うthreadId
    var threadId = ""

    // コメント投稿で使う
    var postCommentText = ""
    var postCommentCommand = ""
    var premium = 0     // プレ垢なら1
    var userId = ""     // ユーザーID
    var isOfficial = false // 公式番組ならtrue

    // 番組情報関係。initNicoLiveData()を呼ばないとこの値は入りません！！！
    var programTitle = ""   // 番組ID
    var communityId = ""    // コミュID
    var thumb = ""          // サムネイル
    var programOpenTime = 0L    // 番組開始時間
    var programStartTime = 0L    // 番組開場時間


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
    fun getNicoLiveHTML(liveId: String, userSession: String?, isLogin: Boolean = true): Deferred<Response> =
        GlobalScope.async {
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
        premium = if (isPremium(nicoLiveJSON)) {
            1
        } else {
            0
        }
        userId = getNiconicoID(nicoLiveJSON) ?: ""
        programOpenTime = nicoLiveJSON.getJSONObject("program").getLong("openTime")
        programStartTime = nicoLiveJSON.getJSONObject("program").getLong("beginTime")
        programTitle = nicoLiveJSON.getJSONObject("program").getString("title")
        communityId = nicoLiveJSON.getJSONObject("community").getString("id")
        thumb = nicoLiveJSON.getJSONObject("program").getJSONObject("thumbnail").getString("small")
        isOfficial = isOfficial(nicoLiveJSON)
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
                // 視聴セッションWebSocketに接続したら最初に送らないといけないJSONがあるので送りつける
                val jsonObject = JSONObject()
                jsonObject.put("type", "watch")
                //body
                val bodyObject = JSONObject()
                bodyObject.put("command", "getpermit")
                //requirement
                val requirementObject = JSONObject()
                requirementObject.put("broadcastId", broadcastId)
                requirementObject.put("route", "")
                //stream
                val streamObject = JSONObject()
                streamObject.put("protocol", "hls")
                streamObject.put("requireNewStream", true)
                streamObject.put("priorStreamQuality", "high")
                streamObject.put("isLowLatency", isLowLatency)
                streamObject.put("isChasePlay", false)
                //room
                val roomObject = JSONObject()
                roomObject.put("isCommentable", true)
                roomObject.put("protocol", "webSocket")

                requirementObject.put("stream", streamObject)
                requirementObject.put("room", roomObject)

                bodyObject.put("requirement", requirementObject)
                jsonObject.put("body", bodyObject)

                //もう一個
                val secondObject = JSONObject()
                secondObject.put("type", "watch")
                val secondbodyObject = JSONObject()
                secondbodyObject.put("command", "playerversion")
                val paramsArray = JSONArray()
                paramsArray.put("leo")
                secondbodyObject.put("params", paramsArray)
                secondObject.put("body", secondbodyObject)

                // JSON送信！
                this.send(secondObject.toString())
                this.send(jsonObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    // 高階関数呼ぶ
                    val command = if (JSONObject(message).getJSONObject("body").has("command")) {
                        JSONObject(message).getJSONObject("body").getString("command")
                    } else {
                        ""
                    }
                    onMessageFun(command, message)
                    when {
                        command == "currentstream" -> {
                            currentQuality = getCurrentQuality(message)
                        }
                        command == "currentroom" -> {
                            val jsonObject = JSONObject(message)
                            // もし放送者の場合はWebSocketに部屋一覧が流れてくるので阻止。
                            if (jsonObject.getJSONObject("body").has("room")) {
                                threadId = getCommentServerThreadId(message)
                            }
                        }
                        command == "postkey" -> {
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
                // ひとつめ
                val jsonObject = JSONObject()
                jsonObject.put("type", "watch")

                val bodyJSONObject = JSONObject()
                bodyJSONObject.put("command", "watching")

                val paramsArray = JSONArray()
                paramsArray.put(broadcastId)
                paramsArray.put("-1")
                paramsArray.put("0")

                bodyJSONObject.put("params", paramsArray)
                jsonObject.put("body", bodyJSONObject)

                // ふたつめ
                val secondJSONObject = JSONObject()
                secondJSONObject.put("type", "pong")
                secondJSONObject.put("body", JSONObject())

                // それぞれを３０秒ごとに送る
                // なお他の環境と同時に視聴すると片方切断される（片方の画面に同じ番組を開くとだめ的なメッセージ出る）
                nicoLiveWebSocketClient.send(jsonObject.toString())
                nicoLiveWebSocketClient.send(secondJSONObject.toString())
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
        //postKeyを視聴用セッションWebSocketに払い出してもらう
        //PC版ニコ生だとコメントを投稿のたびに取得してるので
        val postKeyObject = JSONObject()
        postKeyObject.put("type", "watch")
        val bodyObject = JSONObject()
        bodyObject.put("command", "getpostkey")
        val paramsArray = JSONArray()
        paramsArray.put(threadId)
        bodyObject.put("params", paramsArray)
        postKeyObject.put("body", bodyObject)
        //送信する
        //この後の処理は視聴用セッションWebSocketでpostKeyを受け取る処理に行きます。
        nicoLiveWebSocketClient.send(postKeyObject.toString())
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
     * @param jsonObject nicoLiveHTMLtoJSONObject()の値
     * @return ニコニコのユーザーID
     * */
    fun getNiconicoID(jsonObject: JSONObject): String? {
        return jsonObject.getJSONObject("user").getString("id")
    }

    /**
     * プレ垢かどうか
     * @param jsonObject nicoLiveHTMLtoJSONObject()の値
     * @return プレ垢ならtrue。そうじゃなければfalse
     * */
    fun isPremium(jsonObject: JSONObject): Boolean {
        return jsonObject.getJSONObject("user").getString("accountType") == "premium"
    }

    /**
     * PostKey取得
     * @param message postkeyが文字列に含まれていること
     * @return postkeyが見つからなければnull。あればpostkey
     * */
    fun getPostKey(message: String?): String? {
        val jsonObject = JSONObject(message)
        val command = jsonObject.getJSONObject("body").getString("command")
        // コメント送信なので２重チェック
        if (command.contains("postkey")) {
            //JSON配列の０番目にpostkeyが入ってる。
            val paramsArray =
                jsonObject.getJSONObject("body").getJSONArray("params")
            return paramsArray.getString(0)
        }
        return null
    }

    /**
     * 今の部屋のコメントサーバーのWebSocketアドレスの取得関数
     * @param message messageServerUriが文字列に含まれていること
     * */
    fun getCommentServerWebSocketAddress(message: String?): String {
        val room = JSONObject(message).getJSONObject("body").getJSONObject("room")
        return room.getString("messageServerUri")
    }

    /**
     * 今の部屋のコメントサーバーのスレッドIDの取得関数
     * @param message messageServerUriが文字列に含まれていること
     * */
    fun getCommentServerThreadId(message: String?): String {
        val room = JSONObject(message).getJSONObject("body").getJSONObject("room")
        return room.getString("threadId")
    }

    /**
     * 今の部屋のコメントサーバーの部屋の名前の取得関数
     * @param message messageServerUriが文字列に含まれていること
     * */
    fun getCommentRoomName(message: String?): String {
        val room = JSONObject(message).getJSONObject("body").getJSONObject("room")
        return room.getString("roomName")
    }

    /**
     * 視聴WebSocketからHLSアドレス取得
     * @param message currentStreamが文字列内に入ってなければnull。あればHLSアドレス取れます。
     * */
    fun getHlsAddress(message: String?): String? {
        if (message?.contains("currentStream") == true) {
            val jsonObject = JSONObject(message)
            val currentObject =
                jsonObject.getJSONObject("body").getJSONObject("currentStream")
            return currentObject.getString("uri")
        }
        return null
    }

    /**
     * 画質一覧を取得する関数。
     * @param message currentStreamが文字列内に入ってなければnull。あればHLSアドレス取れます。
     * @return 利用可能な画質
     * */
    fun getQualityListJSONArray(message: String?): JSONArray {
        val currentObject =
            JSONObject(message).getJSONObject("body").getJSONObject("currentStream")
        return currentObject.getJSONArray("qualityTypes")
    }

    /**
     * 現在の画質を取得する関数。
     * @param message currentStreamが文字列内に入ってなければnull。あればHLSアドレス取れます。
     * @return 現在の画質
     * */
    fun getCurrentQuality(message: String?): String {
        val currentObject =
            JSONObject(message).getJSONObject("body").getJSONObject("currentStream")
        return currentObject.getString("quality")
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
     * 画質変更メッセージ送信
     * */
    fun sendQualityMessage(quality: String) {
        val jsonObject = JSONObject()
        jsonObject.put("type", "watch")
        // body
        val bodyObject = JSONObject()
        bodyObject.put("command", "getstream")
        // requirement
        val requirementObjects = JSONObject()
        requirementObjects.put("protocol", "hls")
        requirementObjects.put("quality", quality)
        requirementObjects.put("isLowLatency", isLowLatency)
        requirementObjects.put("isChasePlay", false)
        bodyObject.put("requirement", requirementObjects)
        jsonObject.put("body", bodyObject)
        //送信
        nicoLiveWebSocketClient.send(jsonObject.toString())
    }

    /**
     * 低遅延モード。trueで有効
     * */
    fun sendLowLatency() {
        val jsonObject = JSONObject()
        jsonObject.put("type", "watch")
        // body
        val bodyObject = JSONObject()
        bodyObject.put("command", "getstream")
        // requirement
        val requirementObjects = JSONObject()
        requirementObjects.put("protocol", "hls")
        requirementObjects.put("quality", currentQuality)
        requirementObjects.put("isLowLatency", !isLowLatency)
        requirementObjects.put("isChasePlay", false)
        bodyObject.put("requirement", requirementObjects)
        jsonObject.put("body", bodyObject)
        //送信
        nicoLiveWebSocketClient.send(jsonObject.toString())
        //反転
        isLowLatency = !isLowLatency
    }

    /**
     * 終了時によんで
     * WebSocket切るので
     * */
    fun destroy() {
        timer.cancel()
        nicoLiveWebSocketClient.close()
        commentPOSTWebSocketClient.close()
    }

}