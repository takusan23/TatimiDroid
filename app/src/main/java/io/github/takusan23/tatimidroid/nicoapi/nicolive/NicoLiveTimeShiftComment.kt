package io.github.takusan23.tatimidroid.nicoapi.nicolive

import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.CommentServerData
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONObject
import java.net.URI
import java.util.*

/**
 * タイムシフト特化コメント取得クラス
 * */
class NicoLiveTimeShiftComment {

    private val nicoLiveComment = NicoLiveComment()

    private var webSocketClient: WebSocketClient? = null

    /** コメントを一時的に保持しておく */
    private val commentJSONList = arrayListOf<CommentJSONParse>()

    /** 再生時間を定期的に入れてください。 */
    var currentPositionSec = 0L

    /** 定期実行用 */
    private var commentTimer: Job? = null

    fun connect(
        commentServerData: CommentServerData,
        startTime: Long,
        requestHistoryCommentCount: Int = -100,
        whenValue: Long? = null,
        onMessageFunc: (commentText: String, roomMane: String, isHistory: Boolean) -> Unit
    ) {
        // 追加リクエスト済み？。同じリクエストを飛ばさないように
        var isRequested = false

        // ユーザーエージェントとプロトコル
        val protocol = Draft_6455(Collections.emptyList(), Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?)
        val headerMap = mutableMapOf<String, String>()
        headerMap["User-Agent"] = "TatimiDroid;@takusan_23"
        webSocketClient = object : WebSocketClient(URI(commentServerData.webSocketUri), protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // JSON作成して送信
                val jsonString = nicoLiveComment.createSendJson(commentServerData, requestHistoryCommentCount, whenValue)
                send(jsonString)
            }

            override fun onMessage(message: String?) {
                // 受け取る
                if (message != null) {
                    // 配列に一旦収納
                    commentJSONList.add(CommentJSONParse(message, commentServerData.roomName, ""))
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onError(ex: Exception?) {

            }

        }
        // 接続
        webSocketClient?.connect()

        // コメントを流す
        commentTimer = GlobalScope.launch {
            while (isActive) {
                // 秒単位で流す
                val drawCommentList = commentJSONList.filter { comment -> comment.vpos.toLong() / 100 == currentPositionSec }
                if (drawCommentList.isNotEmpty()) {
                    // 公開関数を呼ぶ
                    drawCommentList.forEach { comment ->
                        onMessageFunc(comment.commentJson, commentServerData.roomName, false)
                    }
                } else {
                    // コメントが空っぽだったら、次のコメントをリクエストする
                    val jsonString = nicoLiveComment.createSendJson(commentServerData, requestHistoryCommentCount, drawCommentList.last().date.toLong())
                    println(JSONObject(jsonString).toString(4))
                    // webSocketClient?.send(jsonString)
                }
            }
        }

    }

    /** 終了時に呼んでください */
    fun destroy() {
        webSocketClient?.close()
        commentTimer?.cancel()
    }

}