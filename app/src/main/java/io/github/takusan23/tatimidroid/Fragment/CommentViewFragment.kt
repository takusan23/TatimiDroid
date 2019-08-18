package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_commentview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.*
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
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

class CommentViewFragment : Fragment() {
    //接続中の部屋名
    var connectingRoomName = ""
    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    var websocketList: ArrayList<WebSocketClient> = arrayListOf()
    lateinit var pref_setting: SharedPreferences
    lateinit var snackbarProgress: SnackbarProgress

    //現在接続中のWebSocketのアドレス
    val connectionWebSocketAddressList = arrayListOf<String>()

    //定期的に立ち見席があるか
    var timer = Timer()

    //getString(R.string.arena)
    lateinit var stringArena: String

    //TTS
    lateinit var tts: TextToSpeech

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commentview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        val liveId = activity?.intent?.getStringExtra("liveId") ?: ""

        stringArena = getString(R.string.arena)

        //RecyclerView
        //recyclerViewList = ArrayList()
        //ここから下三行必須
        recyclerViewList = ArrayList()
        fragment_comment_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        fragment_comment_recyclerview.layoutManager = mLayoutManager
        commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList)
        fragment_comment_recyclerview.adapter = commentRecyclerViewAdapter
        recyclerViewLayoutManager = fragment_comment_recyclerview.layoutManager!!
        fragment_comment_recyclerview.setItemAnimator(null);
        //val viewPool = fragment_comment_recyclerview.recycledViewPool
        //viewPool.setMaxRecycledViews(1, 128)

        //ログイン情報がなければ戻す
        if (pref_setting.getString("mail", "")?.contains("") != false) {
            //UserSession取得
            //コルーチン全くわからん！
            runBlocking {
                //コメント（立ち見含めて）接続
                getLiveInfo()
                //コメント投稿に必須な情報（ユーザーID、プレミアム会員かどうか）の取得と現在の部屋と座席番号取得のために
                //視聴モードの場合は取得する
                if (pref_setting.getBoolean("setting_watching_mode", false)) {
                    getplayerstatus()
                }

                //ニコ生の視聴に必要な情報を流してくれるWebSocketへ接続するために
                //getNicoLiveWebPage()

            }
        } else {
            showToast("メールアドレス、パスワードが保存されてません。")
        }

        //定期的に立ち見席が出てないか確認する
        timer.schedule(60000, 60000) {
            getLiveInfo()
        }


    }


    //getplayerstatus
    fun getplayerstatus() {

        val liveId = activity?.intent?.getStringExtra("liveId") ?: ""
        val usersession = pref_setting.getString("user_session", "") ?: ""

        val request = Request.Builder()
            .url("https://live.nicovideo.jp/api/getplayerstatus/$liveId")   //getplayerstatus、httpsでつながる？
            .header("Cookie", "user_session=$usersession")
            .get()
            .build()
        val okHttpClient = OkHttpClient()


        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                val response_string = response.body?.string()
                if (response.isSuccessful) {
                    //必要なものを取り出していく
                    //add,port,thread
                    val document = Jsoup.parse(response_string)
                    //ひつようなやつ
                    val ms = document.select("ms")
                    //今の部屋とか座席番号とか
                    val user = document.select("user")
                    val room = user.select("room_label").text()
                    val seet = user.select("room_seetno").text()
                    //番組名
                    val stream = document.select("stream")
                    val title = stream.select("title").text()
                    activity?.runOnUiThread {
                        (activity as AppCompatActivity).supportActionBar?.subtitle = "$room - $seet"
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }


    /**コメントサーバーに接続する
     * @param address アドレス
     * @param room 部屋の名前
     * */
    fun connectCommentServer(address: String, thread: String, room: String) {
        val uri = URI(address)

        val protocol = Draft_6455(
            Collections.emptyList(),
            Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?
        )
        val webSocketClient = object : WebSocketClient(uri, protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {

                //System.out.println("接続しました $room")
                connectingRoomName += "$room "
                activity?.runOnUiThread {
                    Snackbar.make(
                        fragment_comment_recyclerview,
                        getString(R.string.connected) + ": $connectingRoomName",
                        Snackbar.LENGTH_LONG
                    ).setAnchorView(activity?.fab)
                        .show()
                }

                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", thread)
                jsonObject.put("service", "LIVE")
                jsonObject.put("score", 1)
                jsonObject.put("res_from", -100)
                sendJSONObject.put("thread", jsonObject)
                this.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                //System.out.println("接続を終了しました。$reason")
            }

            override fun onMessage(message: String?) {
                //Adaprer
                if (message != null) {

                    //運営コメントはアリーナだけ表示する
                    val commentJSONParse = CommentJSONParse(message, room)

                    //コメント流す
                    niconicoComment(commentJSONParse.comment, commentJSONParse.userId)

                    //コテハン登録
                    registerKotehan(commentJSONParse)

                    //追い出しコメントを非表示
                    if (pref_setting.getBoolean("setting_hidden_oidashi_comment", true)) {
                        //追い出しコメントを非表示
                        if (!commentJSONParse.comment.contains("/hb ifseetno")) {
                            if (commentJSONParse.premium.contains("運営")) {
                                //運営コメントはアリーナだけ
                                if (!room.contains(getString(R.string.arena))) {
                                    addItemRecyclerView(message, room)
                                }
                            } else {
                                addItemRecyclerView(message, room)
                            }
                        }
                    } else {
                        if (commentJSONParse.premium.contains("運営")) {
                            //運営コメントはアリーナだけ
                            if (!room.contains(getString(R.string.arena))) {
                                addItemRecyclerView(message, room)
                            }
                        } else {
                            addItemRecyclerView(message, room)
                        }
                    }

                    //Toast / TTS
                    if (activity is CommentActivity) {
                        //Toast
                        if ((activity as CommentActivity).isToast) {
                            activity?.runOnUiThread {
                                Toast.makeText(context, commentJSONParse.comment, Toast.LENGTH_SHORT).show()
                            }
                        }
                        //TTS
                        if ((activity as CommentActivity).isTTS) {
                            //初期化済みか
                            if (!this@CommentViewFragment::tts.isInitialized) {
                                tts = TextToSpeech(context, TextToSpeech.OnInitListener { p0 ->
                                    if (p0 == TextToSpeech.SUCCESS) {
                                        showToast(getString(R.string.tts_init))
                                    } else {
                                        showToast(getString(R.string.error))
                                    }
                                })
                            }
                            tts.speak(commentJSONParse.comment, TextToSpeech.QUEUE_ADD, null, null)
                        } else {
                            if (this@CommentViewFragment::tts.isInitialized) {
                                tts.shutdown()
                            }
                        }
                    }
                }
            }

            override fun onError(ex: Exception?) {
                //System.out.println("$room えらー")
            }
        }
        //接続
        webSocketClient.connect()
        websocketList.add(webSocketClient)
    }

    //コテハン登録
    private fun registerKotehan(commentJSONParse: CommentJSONParse) {
        //@マーク検索
        var pos = 0
        val comment = commentJSONParse.comment
        if (comment.contains("@") || comment.contains("＠")) {
            if (comment.contains("@")) {
                pos = comment.indexOf("@")
            }
            if (comment.contains("＠")) {
                pos = comment.indexOf("＠")
            }
            //切り取る
            val kotehan = comment.subSequence(pos + 1, comment.length)
            //追加
            if (activity is CommentActivity) {
                (activity as CommentActivity).kotehanMap.put(commentJSONParse.userId, kotehan.toString())
            }
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //WebSocket切断
        websocketList.forEach {
            it.close()
        }
        timer.cancel()
        if (this@CommentViewFragment::tts.isInitialized) {
            tts.shutdown()
        }
    }

    //番組情報取得APIを叩く
    fun getLiveInfo() {
        //番組ID
        val usersession = pref_setting.getString("user_session", "") ?: ""
        val id = activity?.intent?.getStringExtra("liveId") ?: ""
        val request = Request.Builder()
            .url("https://live2.nicovideo.jp/watch/$id/programinfo")
            .header("Cookie", "user_session=$usersession")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    showToast("${getString(R.string.error)}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response_string = response.body?.string()
                    //JSONパース
                    val jsonObject = JSONObject(response_string)
                    //必要なものを取り出す
                    val data = jsonObject.getJSONObject("data")
                    val title = data.getString("title")
                    val description = data.getString("description")
                    val room = data.getJSONArray("rooms")
                    //アリーナ、立ち見のコメントサーバーへ接続
                    for (index in 0..(room.length() - 1)) {
                        val roomObject = room.getJSONObject(index)
                        val webSocketUri = roomObject.getString("webSocketUri")
                        var roomName = roomObject.getString("name")
                        val threadId = roomObject.getString("threadId")
                        //現在接続中のアドレスから同じのがないかちぇっく
                        //定期的に新しい部屋が無いか確認しに行くため
                        if (connectionWebSocketAddressList.indexOf(webSocketUri) == -1) {
                            connectionWebSocketAddressList.add(webSocketUri)
                            //アリーナの場合は部屋名がコミュニティ番号なので直す
                            if (roomName.contains("co")) {
                                roomName = stringArena
                            }
                            //ActionBarに番組名を書く
                            activity?.runOnUiThread {
                                (activity as AppCompatActivity).supportActionBar?.title = "$title - $id"
                            }
                            //WebSocket接続
                            connectCommentServer(webSocketUri, threadId, roomName)
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                }
            }
        })
    }


    /*RecyclerViewについかする*/
    fun addItemRecyclerView(json: String, roomName: String) {
        //同じのが追加されないように
        val size = recyclerViewList.size
        val item = arrayListOf<String>()
        item.add("")
        item.add(json)
        item.add(roomName)
        recyclerViewList.add(0, item)
        //RecyclerView更新
        activity?.runOnUiThread {
            if (fragment_comment_recyclerview != null) {
                if (recyclerViewList.size <= size + 1) {
                    commentRecyclerViewAdapter.notifyItemInserted(0)
                }
                // 画面上で最上部に表示されているビューのポジションとTopを記録しておく
                val pos = (recyclerViewLayoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                var top = 0
                if ((recyclerViewLayoutManager as LinearLayoutManager).childCount > 0) {
                    top = (recyclerViewLayoutManager as LinearLayoutManager).getChildAt(0)!!.top
                }
                //一番上なら追いかける
                //System.out.println(pos)
                if (pos == 0 || pos == 1) {
                    fragment_comment_recyclerview.scrollToPosition(0)
                } else {
                    fragment_comment_recyclerview.post {
                        (recyclerViewLayoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos + 1, top)
                    }
                }
            }
        }
    }

    //コメント流す
    fun niconicoComment(message: String, userId: String) {
        //NGコメントは流さない
        val userNGList = (activity as CommentActivity).userNGList
        val commentNGList = (activity as CommentActivity).commentNGList
        //-1で存在しない
        if (userNGList.indexOf(userId) == -1 && commentNGList.indexOf(message) == -1) {
            //追い出しコメントは流さない
            if (!message.contains("/hb ifseetno")) {
                if (activity is CommentActivity) {
                    //UIスレッドで呼んだら遅延せずに表示されました！
                    activity?.runOnUiThread {
                        activity?.comment_canvas?.postComment(message)
                        //ポップアップ再生
                        if ((activity as CommentActivity).overlay_commentcamvas != null) {
                            (activity as CommentActivity).overlay_commentcamvas!!.postComment(message)
                        }
                    }
                }
            }
        }
    }
}