package io.github.takusan23.tatimidroid.NicoAPI.NicoLive

import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.CommentServerData
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

    /** 接続中の[CommentServerData]が入る配列。なお重複は消してます */
    val connectionCommentServerDataList = arrayListOf<CommentServerData>()

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
     * @return [connectCommentServerWebSocket]で使う値の入ったデータクラス
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
     * コメント鯖へ接続する関数。
     * なおこの関数ではすでに接続済みかどうかの判定はしてません。というか多分いらないはず（部屋割り時代は定期的に部屋があるか確認してたので必要だった）
     * @param commentServerData コメントサーバーの情報が入ったデータクラス。threadId,部屋の名前,threadKeyがあれば作成可能です
     * @param requestHistoryCommentCount コメントの取得する量。負の値で
     * @param whenValue 指定した時間のコメントが欲しい場合は指定してください。一番古いコメントのdateとdate_usecを取って{date}.{date_usec}すれば取れると思う
     * @param onMessageFunc コメントが来た時に呼ばれる高階関数
     * */
    fun connectCommentServerWebSocket(commentServerData: CommentServerData, requestHistoryCommentCount: Int = -100, whenValue: Double? = null, onMessageFunc: (commentText: String, roomMane: String, isHistory: Boolean) -> Unit) {
        // 過去コメントか流れてきたコメントか
        var historyComment = requestHistoryCommentCount
        // 過去コメントだとtrue
        var isHistoryComment = true
        val uri = URI(commentServerData.webSocketUri)
        // ユーザーエージェントとプロトコル
        val protocol = Draft_6455(Collections.emptyList(), Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?)
        val headerMap = mutableMapOf<String, String>()
        headerMap["User-Agent"] = "TatimiDroid;@takusan_23"
        val webSocketClient = object : WebSocketClient(uri, protocol, headerMap) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject().apply {
                    put("version", "20061206")
                    put("service", "LIVE")
                    put("thread", commentServerData.threadId)
                    put("scores", 1)
                    put("res_from", historyComment)
                    put("nicoru", 0)
                    put("with_global", 1)
                    put("user_id", commentServerData.userId)
                    put("threadkey", commentServerData.threadKey)
                    put("waybackkey", "")
                    if (whenValue != null) {
                        // 過去コメント
                        put("when", whenValue)
                    }
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
                    onMessageFunc(message, commentServerData.roomName, isHistoryComment)
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        // 忘れるな
        webSocketClient.connect()
        connectionWebSocketClientList.add(webSocketClient)
        connectionCommentServerDataList.add(commentServerData)
        // 重複は消す
        connectionCommentServerDataList.distinctBy { commentServerData -> commentServerData.webSocketUri }
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

}