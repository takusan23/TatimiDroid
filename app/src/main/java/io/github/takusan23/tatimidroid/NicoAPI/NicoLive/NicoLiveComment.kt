package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONObject
import java.io.Serializable
import java.net.URI
import java.util.*

/**
 * 全部屋コメントサーバーに接続する関数。
 * 全部屋の中にstoreってのが混じる様になったけどこれ部屋の統合（全部アリーナ）で入り切らなかったコメントが流れてくる場所らしい。
 * 公式番組では今の部屋のみ接続している。
 * */
class NicoLiveComment {

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
    suspend fun getProgramInfo(liveId: String, userSession: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().apply {
            url("https://live2.nicovideo.jp/watch/$liveId/programinfo")
            header("Cookie", "user_session=$userSession")
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        response
    }

    /**
     * データクラスの配列にパースする
     * @param responseString getProgramInfo()のレスポンス
     * @param arena アリーナの文字列をローカライズする場合は
     * @return CommentServerDataの配列
     * */
    suspend fun parseCommentServerDataList(responseString: String?, allRoomName: String, storeRoomName: String) = withContext(Dispatchers.Default) {
        val list = arrayListOf<CommentServerData>()
        val jsonObject = JSONObject(responseString)
        val data = jsonObject.getJSONObject("data")
        val room = data.getJSONArray("rooms")
        // アリーナ、立ち見のコメントサーバーへ接続
        for (index in 0 until room.length()) {
            val roomObject = room.getJSONObject(index)
            val webSocketUri = roomObject.getString("webSocketUri")
            val name = roomObject.getString("name")
            val roomName = name
            val threadId = roomObject.getString("threadId")
            val data = CommentServerData(webSocketUri, threadId, roomName)
            list.add(data)
        }
        list
    }

    /**
     * 流量制限にかかったコメントが流れてくるコメント鯖に接続するのに必要な値をJSONから取り出す関数。
     * 流量制限のコメントサーバーは他のコメントビューアーでは「store」って表記されていると思いますが、わからんので流量制限って書いてます。覚えとくと良いかも
     * 注意：公式番組（アニメ一挙放送など。ハナヤマタよかった）では利用できません。
     * @param responseString [getProgramInfo]のレスポンスボディー
     * @param storeRoomName 流量制限って文字列を入れて
     * @return [connectionWebSocket]で使う値の入ったデータクラス
     * */
    suspend fun parseStoreRoomServerData(responseString: String?, storeRoomName: String) = withContext(Dispatchers.Default) {
        val jsonObject = JSONObject(responseString)
        val data = jsonObject.getJSONObject("data")
        val room = data.getJSONArray("rooms")
        // 部屋の中から部屋名がstoreなものを探す
        for (index in 0 until room.length()) {
            val roomObject = room.getJSONObject(index)
            val webSocketUri = roomObject.getString("webSocketUri")
            val name = roomObject.getString("name")
            if (name == "store") {
                // ここが流量制限で入り切らないコメントが流れてくるコメントサーバー
                val threadId = roomObject.getString("threadId")
                return@withContext CommentServerData(webSocketUri, threadId, storeRoomName)
            }
        }
        return@withContext null
    }

    /**
     * コメント鯖（WebSocket）に接続する関数
     * 注意：すでに接続済みの場合は接続しません
     * @param webSocketUri WebSocketアドレス
     * @param threadId スレッドID
     * @param roomName 部屋の名前
     * @param onMessageFunc コメントが来たら呼ばれる高階関数。引数は第一がコメントの内容、第二は部屋の名前、第三が過去のコメントならtrue
     * @param userId ユーザーID。なんで必要なのかは知らん。nullでも多分いい
     * @param threadKey 視聴セッションのWebSocketのときは「yourPostKey」ってのがJSONで流れてくるので指定して欲しい。nullても動く。ただ自分が投稿できたかの結果「yourpost」が取れないのでアリーナには繋ぎたい
     * */
    fun connectionWebSocket(webSocketUri: String, threadId: String, roomName: String, userId: String? = null, threadKey: String? = null, onMessageFunc: (commentText: String, roomMane: String, isHistory: Boolean) -> Unit) {
        // 接続済みの場合は繋がない
        if (connectedWebSocketAddressList.contains(webSocketUri)) {
            return
        }
        connectedWebSocketAddressList.add(webSocketUri)
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
                val jsonObject = JSONObject().apply {
                    put("version", "20061206")
                    put("thread", threadId)
                    put("service", "LIVE")
                    put("scores", 1)
                    put("res_from", historyComment)
                    put("nicoru", 0)
                    put("with_global", 1)
                    put("user_id", userId)
                    put("threadkey", threadKey)
                }
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
        connectedWebSocketAddressList.clear()
    }

    /**
     * コメントサーバーのデータクラス
     * */
    data class CommentServerData(val webSocketUri: String, val threadId: String, val roomName: String, val threadKey: String? = null) : Serializable

}