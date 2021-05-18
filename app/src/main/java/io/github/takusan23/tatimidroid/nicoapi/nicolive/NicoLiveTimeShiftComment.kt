package io.github.takusan23.tatimidroid.nicoapi.nicolive

import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.CommentServerData
import kotlinx.coroutines.*
import okhttp3.internal.toLongOrDefault
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
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

    /** 一分感覚でJSONをWebSocketに投げて次の一分間のコメントを取得するのに使う */
    private var jsonPostTimer: Job? = null

    /** コメント取得時に指定した時間が入ってる。 */
    private var lastPostTimeSec = 0L

    /** コメント鯖情報 */
    private var commentServerData: CommentServerData? = null

    /**
     * タイムシフト特化？コメント鯖接続関数
     * */
    fun connect(
        commentServerData: CommentServerData,
        startTime: Long,
        onMessageFunc: (commentText: String, roomMane: String, isHistory: Boolean) -> Unit
    ) {
        // とりあえず一分後まで
        lastPostTimeSec = startTime + 60
        this@NicoLiveTimeShiftComment.commentServerData = commentServerData

        // ユーザーエージェントとプロトコル
        val protocol = Draft_6455(Collections.emptyList(), Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?)
        val headerMap = mutableMapOf<String, String>()
        headerMap["User-Agent"] = "TatimiDroid;@takusan_23"
        webSocketClient = object : WebSocketClient(URI(commentServerData.webSocketUri), protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                // JSON作成して送信。1コメから指定した時間までを取得
                val jsonString = nicoLiveComment.createSendJson(commentServerData, 1, lastPostTimeSec)
                send(jsonString)
            }

            override fun onMessage(message: String?) {
                // 受け取る
                if (message != null) {
                    // 配列に一旦収納。このあとのcommentTimerで高階関数をいい感じに呼ぶ
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
                // 秒単位で流す。投稿日時から開始時間を引けば何秒のときに流せばいいのか出るので
                val drawCommentList = commentJSONList.filter { comment -> comment.date.toLongOrDefault(0) - startTime == currentPositionSec }
                if (drawCommentList.isNotEmpty()) {
                    // 公開関数を呼ぶ
                    drawCommentList.forEach { comment ->
                        onMessageFunc(comment.commentJson, commentServerData.roomName, false)
                    }
                }
                delay(1000)
            }
        }

        // JSONをWebSocketに投げる
        jsonPostTimer = GlobalScope.launch {
            while (isActive) {
                // 一分間
                delay(60 * 1000)
                // 最後のコメント番号
                val lastComment = commentJSONList.maxByOrNull { commentJSONParse -> commentJSONParse.commentNo.toIntOrNull() ?: 0 }
                if (lastComment != null) {
                    // 次の一分間
                    lastPostTimeSec += 60
                    // コメント取得。最後のコメント番号+1から、指定した時間（一分後）を指定する
                    val sendJsonString = nicoLiveComment.createSendJson(commentServerData, lastComment.commentNo.toInt() + 1, lastPostTimeSec)
                    // 送信
                    webSocketClient?.send(sendJsonString)
                }
            }
        }
    }

    /** 終了時に呼んでください */
    fun destroy() {
        webSocketClient?.close()
        commentTimer?.cancel()
        jsonPostTimer?.cancel()
    }

}