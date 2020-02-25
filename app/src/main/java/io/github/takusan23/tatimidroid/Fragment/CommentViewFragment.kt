package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.*
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.fragment_commentview.*
import kotlinx.android.synthetic.main.overlay_player_layout.view.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class CommentViewFragment : Fragment() {
    //接続中の部屋名
    var connectingRoomName = ""
    var recyclerViewList: ArrayList<ArrayList<String>> = arrayListOf()
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

    var liveId = ""

    lateinit var commentFragment: CommentFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commentview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //LiveIDとる
        liveId = arguments?.getString("liveId") ?: ""
        //println("なんでええええええええ$liveId")

        val recyclerView = view.findViewById<RecyclerView>(R.id.fragment_comment_recyclerview)

        stringArena = getString(R.string.arena)

        //CommentFragment取得
        val fragment =
            (activity as AppCompatActivity).supportFragmentManager.findFragmentByTag(liveId)
        commentFragment = fragment as CommentFragment

        commentFragment.apply {
            if (!isAllRoomCommentInit()) {
                // 初期化してないとき
                allRoomComment = AllRoomComment(
                    context,
                    liveId,
                    this
                )
            }
            // RecyclerView初期化
            recyclerView.setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = mLayoutManager
            commentRecyclerViewAdapter =
                CommentRecyclerViewAdapter(allRoomComment.recyclerViewList)
            recyclerView.adapter = commentRecyclerViewAdapter
            allRoomComment.recyclerView = recyclerView
            recyclerView.setItemAnimator(null);
        }

        //val viewPool = fragment_comment_recyclerview.recycledViewPool
        //viewPool.setMaxRecycledViews(1, 128)

/*
        //ログイン情報がなければ戻す
        if (pref_setting.getString("mail", "")?.contains("") != false) {
            //UserSession取得
            //コルーチン全くわからん！
            runBlocking {
                //コメント（立ち見含めて）接続
                if (!commentFragment.isOfficial) {
                    getLiveInfo()
                }
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
            if (!commentFragment.isOfficial) {
                getLiveInfo()
            }
        }

        //公式番組。部屋のAPIが取得できないので今の部屋のこめんと鯖につなぐ。
        if (commentFragment.isOfficial) {
            if (commentFragment.commentMessageServerUri.isNotEmpty()) {
                connectCommentServer(
                    commentFragment.commentMessageServerUri,
                    commentFragment.commentThreadId,
                    ""
                )
            }
        }
*/

    }


    //getplayerstatus
    fun getplayerstatus() {

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
                        //二窓モードでは表示させない
                        if (activity !is NimadoActivity) {
                            if (activity is AppCompatActivity) {
                                (activity as AppCompatActivity).supportActionBar?.subtitle =
                                    "$room - $seet"
                            }
                        }
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
        //過去コメントか流れてきたコメントか
        var historyComment = -100
        //過去コメントだとtrue
        var isHistoryComment = true

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
                    ).setAnchorView(commentFragment.fab)
                        .show()
                }

                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", thread)
                jsonObject.put("service", "LIVE")
                jsonObject.put("score", 1)
                jsonObject.put("res_from", historyComment)
                sendJSONObject.put("thread", jsonObject)
                this.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                //System.out.println("接続を終了しました。$reason")
            }

            override fun onMessage(message: String?) {
                //Adaprer
                if (message != null) {

                    //val commentActivity = activity as CommentActivity

                    //運営コメントはアリーナだけ表示する
                    val commentJSONParse = CommentJSONParse(message, room)
                    //ID
                    val userId = commentJSONParse.userId

                    //184非表示機能つける。いるこれ？
                    val commentShow = if (commentFragment.isTokumeiHide) {
                        !commentJSONParse.mail.contains("184")  //mailに184があるとき
                    } else {
                        true
                    }
                    if (commentShow) {
                        //コメント流す
                        niconicoComment(
                            commentJSONParse.comment,
                            commentJSONParse.userId,
                            commentJSONParse.mail,
                            commentJSONParse
                        )

                        //コテハン登録
                        registerKotehan(commentJSONParse)

                        //アクティブ人数計算
                        calcActiveCount(commentJSONParse)

                        //過去コメントかな？
                        if (historyComment < 0) {
                            historyComment++
                        } else {
                            isHistoryComment = false
                        }

                        //追い出しコメントを非表示
                        if (pref_setting.getBoolean("setting_hidden_oidashi_comment", true)) {
                            //追い出しコメントを非表示
                            if (!commentJSONParse.comment.contains("/hb ifseetno")) {
                                if (commentJSONParse.premium.contains("運営")) {
                                    //運営コメントはアリーナだけ
                                    if (!room.contains(getString(R.string.arena))) {
                                        addItemRecyclerView(message, room, userId, commentJSONParse)
                                    }
                                } else {
                                    addItemRecyclerView(message, room, userId, commentJSONParse)
                                }
                            }
                        } else {
                            if (commentJSONParse.premium.contains("運営")) {
                                //運営コメントはアリーナだけ
                                if (!room.contains(getString(R.string.arena))) {
                                    addItemRecyclerView(message, room, userId, commentJSONParse)
                                }
                            } else {
                                addItemRecyclerView(message, room, userId, commentJSONParse)
                            }
                        }

                        //Toast / TTS
                        //Toast
                        if (commentFragment.isToast) {
                            activity?.runOnUiThread {
                                Toast.makeText(
                                    context,
                                    commentJSONParse.comment,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        //TTS
                        if (commentFragment.isTTS) {
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

                        //disconnectを検知
                        if (commentJSONParse.comment.contains("/disconnect")) {
                            if (commentJSONParse.premium.contains("運営")) {
                                //自動次枠移動が有効なら使う
                                if (commentFragment.isAutoNextProgram) {
                                    commentFragment.checkNextProgram()
                                    Snackbar.make(
                                        fragment_comment_recyclerview,
                                        getString(R.string.next_program_message),
                                        Snackbar.LENGTH_SHORT
                                    ).show()
                                } else {
                                    //終了メッセージ
                                    Snackbar.make(
                                        fragment_comment_recyclerview,
                                        getString(R.string.program_disconnect),
                                        Snackbar.LENGTH_SHORT
                                    ).setAction(getString(R.string.end)) {
                                        //終了
                                        if (activity !is NimadoActivity) {
                                            //二窓Activity以外では終了できるようにする。
                                            activity?.finish()
                                        }
                                    }.setAnchorView(commentFragment.getSnackbarAnchorView()).show()
                                }
                            }
                        }

                        //運営コメント、Infoコメント　表示・非表示
                        if (!commentFragment.hideInfoUnnkome) {
                            //運営コメント
                            if (commentJSONParse.comment.contains("/perm")) {
                                if (!isHistoryComment) {
                                    val text = commentJSONParse.comment.replace("/perm ", "")
                                    commentFragment.setUnneiComment(text)
                                }
                            }
                            //運営コメントけす
                            if (commentJSONParse.comment.contains("/clear")) {
                                if (!isHistoryComment) {
                                    commentFragment.removeUnneiComment()
                                }
                            }

                            //infoコメントを表示
                            if (commentJSONParse.comment.contains("/nicoad")) {
                                if (!isHistoryComment) {
                                    activity?.runOnUiThread {
                                        val json =
                                            JSONObject(
                                                commentJSONParse.comment.replace(
                                                    "/nicoad ",
                                                    ""
                                                )
                                            )
                                        val comment = json.getString("message")
                                        commentFragment.showInfoComment(comment)
                                    }
                                }
                            }
                            if (commentJSONParse.comment.contains("/info")) {
                                if (!isHistoryComment) {
                                    activity?.runOnUiThread {
                                        commentFragment.showInfoComment(
                                            commentJSONParse.comment.replace(
                                                "/info ",
                                                ""
                                            )
                                        )
                                    }
                                }
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

    //アクティブ人数計算（１分間IDをカウントする）
    private fun calcActiveCount(commentJSONParse: CommentJSONParse) {
        val id = commentJSONParse.userId
        //ID入れる
        if (commentFragment.activeList.indexOf(id) == -1) {
            commentFragment.activeList.add(id)
        }
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
            commentFragment.kotehanMap.put(
                commentJSONParse.userId,
                kotehan.toString()
            )
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
        val request = Request.Builder()
            .url("https://live2.nicovideo.jp/watch/$liveId/programinfo")
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
                            if (roomName.contains("co") || roomName.contains("ch")) {
                                roomName = stringArena
                            }
                            //ActionBarに番組名を書く
                            activity?.runOnUiThread {
                                if (activity is AppCompatActivity) {
                                    //二窓モードでは表示させない
                                    if (activity !is NimadoActivity) {
                                        (activity as AppCompatActivity).supportActionBar?.title =
                                            "$title - $liveId"
                                    }
                                }
                            }
                            //WebSocket接続
                            connectCommentServer(webSocketUri, threadId, roomName)
                        }
                    }
                    activity?.runOnUiThread {
                        //部屋別表示のTabItemに部屋数バッジ表示
                        commentFragment.activity_comment_tab_layout.getTabAt(2)
                            ?.orCreateBadge?.apply {
                            number = connectionWebSocketAddressList.size
                            isVisible = true
                            backgroundColor = Color.parseColor("#757575")
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
    fun addItemRecyclerView(
        json: String,
        roomName: String,
        userId: String,
        commentJSONParse: CommentJSONParse
    ) {

        /*
        * 重複しないように。
        * 令和元年8月中旬からアリーナに一般のコメントが流れ込むように（じゃあ枠の仕様なくせ栗田さん）
        * 令和元年12月中旬から立ち見部屋にアリーナのコメントが流れ込むように（だから枠の仕様無くせよ）
        * というわけで同じコメントが追加されてしまうので対策する。
        * 12月中旬のメンテで立ち見部屋にアリーナコメントが出るように（とさり気なく枠の時間復活）なったとき、JSONにoriginが追加されて、
        * 多分originの値がC以外のときに元の部屋のコメントだと
        * */
        if (commentJSONParse.origin != "C") {
            val size = recyclerViewList.size
            val item = arrayListOf<String>()
            item.add("")
            item.add(json)
            item.add(roomName)
            item.add(userId)
            item.add(liveId)

            recyclerViewList.add(0, item)

            //ロックオンできるように
            //ロックオン中は自動更新できるようにする
            val fragment = fragmentManager?.findFragmentByTag("comment_menu")
            if (fragment != null) {
                if (fragment is CommentMenuBottomFragment) {
                    if (fragment.userId == commentJSONParse.userId) {
                        //更新する
                        fragment.setLockOnComment()
                    }
                }
            }


            //RecyclerView更新
            activity?.runOnUiThread {
                if (fragment_comment_recyclerview != null) {
                    if (recyclerViewList.size <= size + 1) {
                        commentRecyclerViewAdapter.notifyItemInserted(0)
                    }
                    // 画面上で最上部に表示されているビューのポジションとTopを記録しておく
                    val pos =
                        (recyclerViewLayoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
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
                            (recyclerViewLayoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                pos + 1,
                                top
                            )
                        }
                    }
                }
            }
        }


/*
        if (arenaDoubleComment[commentJSONParse.date] != commentJSONParse.userId) {
            arenaDoubleComment[commentJSONParse.date] = commentJSONParse.userId
            recyclerViewList.add(0, item)
            //ロックオンできるように
            if (activity is CommentActivity) {
                //ロックオン中は自動更新できるようにする
                val fragment = fragmentManager?.findFragmentByTag("comment_menu")
                if (fragment != null) {
                    if (fragment is CommentMenuBottomFragment) {
                        if (fragment.userId == commentJSONParse.userId) {
                            //更新する
                            fragment.setLockOnComment()
                        }
                    }
                }
            }

            //RecyclerView更新
            activity?.runOnUiThread {
                if (fragment_comment_recyclerview != null) {
                    if (recyclerViewList.size <= size + 1) {
                        commentRecyclerViewAdapter.notifyItemInserted(0)
                    }
                    // 画面上で最上部に表示されているビューのポジションとTopを記録しておく
                    val pos =
                        (recyclerViewLayoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
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
                            (recyclerViewLayoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                                pos + 1,
                                top
                            )
                        }
                    }
                }
            }
        }
*/


    }

    var oldComment = ""
    var oldUser = ""
    var oldVpos = ""

    //コメント流す
    fun niconicoComment(
        message: String,
        userId: String,
        command: String,
        commentJSONParse: CommentJSONParse
    ) {
        //コメントを流さない設定？
        if (!commentFragment.isCommentHidden) {
            //NGコメントは流さない
            val userNGList = commentFragment.userNGList
            val commentNGList = commentFragment.commentNGList
            //-1で存在しない
            if (userNGList.indexOf(userId) == -1 && commentNGList.indexOf(message) == -1) {
                //追い出しコメントは流さない
                if (!message.contains("/hb ifseetno")) {
                    //UIスレッドで呼んだら遅延せずに表示されました！


                    //二重に表示されない対策。originにCが入っていなければいいという本当にこれでいいのか？
                    if (commentJSONParse.origin != "C") {

                        //  println("${commentJSONParse.origin} / $message / $userId")

                        activity?.runOnUiThread {
                            commentFragment.commentCanvas.postComment(
                                message,
                                commentJSONParse
                            )
                            //ポップアップ再生
                            if (commentFragment.overlay_commentcamvas != null) {
                                commentFragment.overlay_commentcamvas!!.postComment(
                                    message, commentJSONParse
                                )
                                //コメント
                                val textView =
                                    commentFragment.popupView.overlay_comment_textview
                                textView.text =
                                    "$message\n${textView.text}"
                            }
                        }
                    }
                }
            }
        }
    }
}