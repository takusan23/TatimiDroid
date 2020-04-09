package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONObject
import java.lang.Exception
import java.net.URI
import java.util.*

/**
 * 全部屋コメントサーバーに接続する関数
 * 公式番組では今の部屋のみ接続している。
 *
 * @param liveId 生放送ID
 * @param userSession ユーザーセッション
 *
 * */
class NicoLiveComment() {

    // 接続済みWebSocketアドレスが入る
    val connectedWebSocketAddressList = arrayListOf<String>()

    // 接続済みWebSocketClientが入る
    val connectionWebSocketClientList = arrayListOf<WebSocketClient>()


    /**
     * 公式番組は視聴セッションWebSocketから流れてきたmessageServerUriとかを使ってこれ「connectionWebSocket()」使って
     * */

    /**
     * 公式以外の番組。
     * 全部屋WebSocketアドレスがあるAPIがあるので使わせてもらう
     * @return OkhttpのResponse
     * */
    fun getProgramInfo(liveId: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("https://live2.nicovideo.jp/watch/$liveId/programinfo")
                header("Cookie", "user_session=$userSession")
                header("User-Agent", "TatimiDroid;@takusan_23")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * WebSocketに接続する関数
     * @param webSocketUri WebSocketアドレス
     * @param threadId スレッドID
     * @param roomName 部屋の名前
     * @param onMessageFunc コメントが来たら呼ばれる高階関数。引数は第一がコメントの内容、第二は部屋の名前、第三が過去のコメントならtrue
     * */
    fun connectionWebSocket(webSocketUri: String, threadId: String, roomName: String, onMessageFunc: (String, String, Boolean) -> Unit) {
        // 過去コメントか流れてきたコメントか
        var historyComment = -100
        // 過去コメントだとtrue
        var isHistoryComment = true
        val uri = URI(webSocketUri)
        val protocol = Draft_6455(
            Collections.emptyList(),
            Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?
        )
        val webSocketClient = object : WebSocketClient(uri, protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", threadId)
                jsonObject.put("service", "LIVE")
                jsonObject.put("score", 1)
                jsonObject.put("res_from", historyComment)
                sendJSONObject.put("thread", jsonObject)
                this.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                if (message != null) {
                    //過去コメントかな？
                    if (historyComment < 0) {
                        historyComment++
                    } else {
                        isHistoryComment = false
                    }
                    // 高階関数呼ぶ
                    onMessageFunc(message, roomName, isHistoryComment)
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        webSocketClient.connect()
        connectionWebSocketClientList.add(webSocketClient)
    }

    /**
     * 終了時に呼んでね
     * */
    fun destroy() {
        connectionWebSocketClientList.forEach {
            it.close()
        }
    }

}