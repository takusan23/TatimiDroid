package io.github.takusan23.tatimidroid.Activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.*
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
import android.widget.ListPopupWindow
import io.github.takusan23.tatimidroid.Fragment.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull


class CommentActivity : AppCompatActivity() {

    lateinit var pref_setting: SharedPreferences

    //ユーザーセッション
    var usersession = ""
    //視聴に必要なデータ受信用WebSocket
    lateinit var connectionNicoLiveWebSocket: WebSocketClient
    //放送開始時間
    var programStartTime: Long = 0
    //コメント送信用WebSocket
    lateinit var commentPOSTWebSocketClient: WebSocketClient
    //コメント送信時に必要なpostKeyを払い出す時に必要なthreadId
    var getPostKeyThreadId = ""
    //コメント投稿時につかうticketを入れておく
    //これはコメント送信用WebSocketにthreadメッセージ（過去コメントなど指定して）送信すると帰ってくるJSONObjectに入ってる
    var commentTicket = ""
    //コメント投稿に必須なユーザーID
    var userId = ""
    //コメント投稿に必須なプレミアム会員かどうか
    var premium = 0
    //コメントの内容
    var commentValue = ""
    //コマンド（匿名とか）
    var commentCommand = "184"
    //視聴モード（コメント投稿機能付き）かどうか
    var isWatchingMode = false

    //TTS使うか
    var isTTS = false
    //Toast表示
    var isToast = false

    //定期的に投稿するやつ
    //視聴続けてますよ送信用
    val timer = Timer()

    //番組ID
    var liveId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)

        setContentView(R.layout.activity_comment)

        //ダークモード対応
        activity_comment_bottom_navigation_bar.backgroundTintList = ColorStateList.valueOf(darkModeSupport.getThemeColor())
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(darkModeSupport.getThemeColor()))
        }

        //スリープにしない
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //横画面はLinearLayoutの向きを変える
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //横画面
            activity_comment_main_linearlayout.orientation = LinearLayout.HORIZONTAL
        } else {
            //縦画面
            activity_comment_main_linearlayout.orientation = LinearLayout.VERTICAL
        }

        liveId = intent?.getStringExtra("liveId") ?: ""

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)


        //とりあえずコメントViewFragmentへ
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.activity_comment_linearlayout, CommentViewFragment())
        fragmentTransaction.commit()


        //コメント投稿モード、nicocas式コメント投稿モード以外でFAB非表示
        val watchingmode = pref_setting.getBoolean("setting_watching_mode", false)
        val nicocasmode = pref_setting.getBoolean("setting_nicocas_mode", false)
        if (!watchingmode && !nicocasmode) {
            fab.hide()
        }


        //コメント投稿画面開く
        fab.setOnClickListener {
            val commentPOSTBottomFragment = CommentPOSTBottomFragment()
            commentPOSTBottomFragment.show(supportFragmentManager, "comment")
        }


        //視聴モードならtrue
        isWatchingMode = pref_setting.getBoolean("setting_watching_mode", false)

        //ログイン情報がなければ戻す
        if (pref_setting.getString("mail", "")?.contains("") != false) {
            //UserSession取得
            //コルーチン全くわからん！
            runBlocking {

                //番組情報取得API叩く
                //視聴モード（コメント投稿機能付き）の場合とそうでない場合分ける
                if (isWatchingMode) {
                    //視聴モード
                    //PCなどで同時視聴してると怒られる。
                    GlobalScope.async {
                        NicoLogin(this@CommentActivity, liveId)
                    }.await()
                    usersession = pref_setting.getString("user_session", "") ?: ""
                    //ユーザーID、プレミアム会員かどうか取得
                    getplayerstatus()
                    //ニコ生の視聴に必要な情報を流してくれるWebSocketへ接続するために
                    getNicoLiveWebPage()
                } else {
                    //User_Session取得
                    GlobalScope.async {
                        NicoLogin(this@CommentActivity, liveId)
                    }.await()
                    usersession = pref_setting.getString("user_session", "") ?: ""
                    //座席番号、部屋番号取得
                    getplayerstatus()
                    //ニコ生の視聴に必要な情報を流してくれるWebSocketへ接続するために
                    getNicoLiveWebPage()
                }

            }
            activity_comment_bottom_navigation_bar.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.comment_view_menu_comment_view -> {
                        //コメント
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.activity_comment_linearlayout, CommentViewFragment())
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_room -> {
                        //ギフト
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.activity_comment_linearlayout, CommentRoomFragment())
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_gift -> {
                        //ギフト
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.activity_comment_linearlayout, GiftFragment())
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_nicoad -> {
                        //広告
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.activity_comment_linearlayout, NicoAdFragment())
                        fragmentTransaction.commit()
                    }
                    R.id.comment_view_menu_info -> {
                        //番組情報
                        val fragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.replace(R.id.activity_comment_linearlayout, ProgramInfoFragment())
                        fragmentTransaction.commit()
                    }
                }
                true
            }
        } else {
            showToast(getString(R.string.mail_pass_error))
            finish()
            //startActivity(Intent(this@CommentActivity, MainActivity::class.java))
        }
    }

    //getplayerstatus
    fun getplayerstatus() {

        val liveId = intent?.getStringExtra("liveId") ?: ""

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
                    val user = document.select("user")
                    val room = user.select("room_label").text()
                    val seet = user.select("room_seetno").text()
                    //コメント投稿に必須なゆーざーID、プレミアム会員かどうか
                    userId = user.select("user_id").text()
                    premium = user.select("is_premium").text().toInt()
                    //vpos
                    programStartTime = document.select("stream").select("base_time").text().toLong()
                    runOnUiThread {
                        supportActionBar?.subtitle = "$room - $seet"
                    }
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //ニコ生の視聴用の情報を流してくれるWebSocketに接続する
    //コメントに投稿したり、HLSのアドレスはWebSocketから取得する必要がある。
    //で、WebSocketのアドレスはHTMLを解析する必要がある！？！？！？
    fun getNicoLiveWebPage() {
        //番組ID
        val id = intent?.getStringExtra("liveId") ?: ""
        var request: Request

        //コメビュモードの場合はユーザーセッション無いので
        if (isWatchingMode) {
            //視聴モード（ユーザーセッション付き）
            request = Request.Builder()
                .url("https://live2.nicovideo.jp/watch/${id}")
                .header("Cookie", "user_session=${usersession}")
                .get()
                .build()
        } else {
            //コメビュモード（ユーザーセッションなし）
            request = Request.Builder()
                .url("https://live2.nicovideo.jp/watch/${id}")
                .get()
                .build()
        }

        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("${getString(R.string.error)}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val response_string = response.body?.string()
                    //HTML解析
                    val html = Jsoup.parse(response_string)
                    //謎のJSON取得
                    //この部分長すぎてChromeだとうまくコピーできないんだけど、Edgeだと完璧にコピーできたぞ！
                    val json = html.getElementById("embedded-data").attr("data-props")
                    val jsonObject = JSONObject(json)
                    val site = jsonObject.getJSONObject("site")
                    val relive = site.getJSONObject("relive")
                    //WebSocketリンク
                    val websocketUrl = relive.getString("webSocketUrl")
                    //broadcastId取得
                    val program = jsonObject.getJSONObject("program")
                    //broadcastId
                    val broadcastId = program.getString("broadcastId")
                    connectionNicoLiveWebSocket(websocketUrl, broadcastId)
                } else {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    //ニコ生の視聴に必要なデータを流してくれるWebSocket
    //視聴セッションWebSocket
    fun connectionNicoLiveWebSocket(url: String, broadcastId: String) {
        val uri = URI(url)
        connectionNicoLiveWebSocket = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                System.out.println("ニコ生視聴セッションWebSocket接続開始")
                //最初にクライアント側からサーバーにメッセージを送る必要がある
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
                streamObject.put("isLowLatency", true)
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

                //送信
                connectionNicoLiveWebSocket.send(secondObject.toString())
                connectionNicoLiveWebSocket.send(jsonObject.toString())

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                System.out.println(reason)
            }

            override fun onMessage(message: String?) {
                //HLSのアドレスとか
                if (message?.contains("currentStream") ?: false) {
                    val jsonObject = JSONObject(message)
                    val hls_address = jsonObject.getJSONObject("body").getJSONObject("currentStream").getString("uri")
                    System.out.println("HLSアドレス ${hls_address}")
                    //生放送再生
                    setPlayVideoView(hls_address)
                }

                //threadId、WebSocketURL受信
                //コメント投稿時に使う。
                if (message?.contains("threadId") ?: false) {
                    val jsonObject = JSONObject(message)
                    val room = jsonObject.getJSONObject("body").getJSONObject("room")
                    val threadId = room.getString("threadId")
                    val messageServerUri = room.getString("messageServerUri")

                    //コメント投稿時に必要なpostKeyを取得するために使う
                    getPostKeyThreadId = threadId

                    System.out.println("コメントWebSocket情報 ${threadId} ${messageServerUri}")

                    //コメント投稿時に使うWebSocketに接続する
                    connectionCommentPOSTWebSocket(messageServerUri, threadId)
                }

                //postKey受信
                //今回は受信してpostKeyが取得できたらコメントを送信する仕様にします。
                //postKeyをもらう イコール　コメントを送信する
                if (message?.contains("postkey") ?: false) {
                    val jsonObject = JSONObject(message)
                    val command = jsonObject.getJSONObject("body").getString("command")
                    //コメント送信なので２重チェック
                    if (command.contains("postkey")) {

                        //JSON配列の０番目にpostkeyが入ってる。
                        val paramsArray = jsonObject.getJSONObject("body").getJSONArray("params")
                        val postkey = paramsArray.getString(0)
                        //コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
                        val vpos = System.currentTimeMillis() - programStartTime
                        val jsonObject = JSONObject()
                        val chatObject = JSONObject()
                        chatObject.put("thread", getPostKeyThreadId)    //視聴用セッションWebSocketからとれる
                        chatObject.put("vpos", vpos)     //番組情報取得で取得した値 - = System.currentTimeMillis() UNIX時間
                        chatObject.put("mail", "184")
                        chatObject.put(
                            "ticket",
                            commentTicket
                        )     //視聴用セッションWebSocketからとれるコメントが流れるWebSocketに接続して最初に必要な情報を送ったら取得できる
                        chatObject.put("content", commentValue)
                        chatObject.put("postkey", postkey)
                        //この２つ、ユーザーIDとプレミアム会員かどうか。これどうやってとる？gatPlayerStatus？　もしgetPlayerStatusなくなってもHTML解析すればとれそう？
                        chatObject.put("premium", premium)
                        chatObject.put("user_id", userId)
                        jsonObject.put("chat", chatObject)

                        System.out.println(jsonObject.toString())

                        //送信
                        commentPOSTWebSocketClient.send(jsonObject.toString())
                    }
                }

                //総来場者数、コメント数
                if (message?.contains("statistics") ?: false) {
                    val jsonObject = JSONObject(message)
                    val params = jsonObject.getJSONObject("body").getJSONArray("params")
                    val watchCount = params[0]
                    val commentCount = params[1]
                    runOnUiThread {
                        activity_comment_watch_count.text = watchCount.toString()
                        activity_comment_comment_count.text = commentCount.toString()
                    }
                }

            }

            override fun onError(ex: Exception?) {
                ex?.printStackTrace()
                System.out.println("ニコ生視聴セッションWebSocket　えらー")
            }
        }

        //それと別に30秒間隔で視聴を続けてますよメッセージを送信する必要がある模様
        timer.schedule(30000, 30000) {
            if (!connectionNicoLiveWebSocket.isClosed) {
                //ひとつめ
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

                //ふたつめ
                val secondJSONObject = JSONObject()
                secondJSONObject.put("type", "pong")
                secondJSONObject.put("body", JSONObject())

                //それぞれを３０秒ごとに送る
                //なお他の環境と同時に視聴すると片方切断される（片方の画面に同じ番組を開くとだめ的なメッセージ出る）
                connectionNicoLiveWebSocket.send(jsonObject.toString())
                connectionNicoLiveWebSocket.send(secondJSONObject.toString())
                System.out.println(jsonObject.toString())
            }
        }
        //接続
        connectionNicoLiveWebSocket.connect()
    }

    //コメント送信用WebSocket。今の部屋に繋がってる（アリーナならアリーナ）
    fun connectionCommentPOSTWebSocket(url: String, threadId: String) {
        val uri = URI(url)
        //これはプロトコルの設定が必要
        val protocol = Draft_6455(
            Collections.emptyList(),
            Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?
        )
        commentPOSTWebSocketClient = object : WebSocketClient(uri, protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                System.out.println("コメント送信用WebSocket接続開始")

                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", threadId)
                jsonObject.put("service", "LIVE")
                jsonObject.put("score", 1)
                jsonObject.put("user_id", userId)
                jsonObject.put("res_from", -10)
                sendJSONObject.put("thread", jsonObject)
                commentPOSTWebSocketClient.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                //コメント送信に使うticketを取得する
                if (message != null) {
                    //thread
                    if (message.contains("ticket")) {
                        val jsonObject = JSONObject(message)
                        val thread = jsonObject.getJSONObject("thread")
                        commentTicket = thread.getString("ticket")
                    }
                    //chat_result
                    //コメント送信できたか
                    if (message.contains("chat_result")) {
                        val jsonObject = JSONObject(message)
                        val status = jsonObject.getJSONObject("chat_result").getString("status")
                        runOnUiThread {
                            if (status.toInt() == 0) {
                                Snackbar.make(
                                    fab,
                                    getString(R.string.comment_post_success),
                                    Snackbar.LENGTH_SHORT
                                )
                                    .setAnchorView(fab)
                                    .show()
                            } else {
                                Snackbar.make(
                                    fab,
                                    "${getString(R.string.comment_post_error)}：${status}",
                                    Snackbar.LENGTH_SHORT
                                )
                                    .setAnchorView(fab).show()
                            }
                        }

                    }
                }
            }

            override fun onError(ex: Exception?) {

            }
        }
        //忘れがちな接続
        commentPOSTWebSocketClient.connect()
    }

    //視聴モード
    fun setPlayVideoView(hls: String) {
        //設定で読み込むかどうか
        if (pref_setting.getBoolean("setting_watch_live", false)) {
            runOnUiThread {
                //ウィンドウの半分ぐらいの大きさに設定
                val display = getWindowManager().getDefaultDisplay()
                val point = Point()
                display.getSize(point)
                val layoutParams = live_video_view.layoutParams

                //横画面のときの対応
                if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    //横画面
                    layoutParams.width = point.x / 2
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                    live_video_view.layoutParams = layoutParams
                } else {
                    //縦画面
                    layoutParams.height = point.y / 3
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                    live_video_view.layoutParams = layoutParams
                }

                val tree = live_video_view.viewTreeObserver
                tree.addOnGlobalLayoutListener {
                    //横画面のときの対応
                    val layoutParams = live_framelayout.layoutParams
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        //横画面
                        layoutParams.width = live_video_view.width
                        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        live_framelayout.layoutParams = layoutParams

                        //コメントキャンバス
                        val commentCanvasLayout = comment_canvas.layoutParams as FrameLayout.LayoutParams
                        commentCanvasLayout.width = live_video_view.width
                        commentCanvasLayout.height = live_video_view.height
                        commentCanvasLayout.gravity = Gravity.CENTER
                        comment_canvas.layoutParams = commentCanvasLayout

                    } else {
                        //縦画面
                        layoutParams.height = live_video_view.height
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                        live_framelayout.layoutParams = layoutParams
                    }
                }

                //再生
                live_video_view.setVideoURI(hls.toUri())
                live_video_view.start()
                live_video_view.setOnClickListener {
                    live_video_view.start()
                }
            }
        }
    }

    //コメント投稿用
    fun sendComment(comment: String) {
        commentValue = comment
        //コメント投稿モード（コメントWebSocketに送信）
        val watchmode = pref_setting.getBoolean("setting_watching_mode", false)
        //nicocas式コメント投稿モード
        val nicocasmode = pref_setting.getBoolean("setting_nicocas_mode", false)
        if (watchmode) {
            //postKeyを視聴用セッションWebSocketに払い出してもらう
            //PC版ニコ生だとコメントを投稿のたびに取得してるので
            val postKeyObject = JSONObject()
            postKeyObject.put("type", "watch")
            val bodyObject = JSONObject()
            bodyObject.put("command", "getpostkey")
            val paramsArray = JSONArray()
            paramsArray.put(getPostKeyThreadId)
            bodyObject.put("params", paramsArray)
            postKeyObject.put("body", bodyObject)
            //送信する
            //この後の処理は視聴用セッションWebSocketでpostKeyを受け取る処理に行きます。
            connectionNicoLiveWebSocket.send(postKeyObject.toString())
        }
        if (nicocasmode) {
            //コメント投稿時刻を計算する（なんでこれクライアント側でやらないといけないの？？？）
            val vpos = System.currentTimeMillis() - programStartTime
            val jsonObject = JSONObject()
            jsonObject.put("message", commentValue)
            jsonObject.put("command", commentCommand)
            jsonObject.put("vpos", vpos.toString())

            val requestBodyJSON = RequestBody.Companion.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonObject.toString()
            )

            val request = Request.Builder()
                .url("https://api.cas.nicovideo.jp/v1/services/live/programs/${liveId}/comments")
                .header("Cookie", "user_session=${usersession}")
                .post(requestBodyJSON)
                .build()
            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showToast("${getString(R.string.error)}")
                }

                override fun onResponse(call: Call, response: Response) {
                    System.out.println(response.body?.string())
                    if (response.isSuccessful) {
                        //成功
                        val snackbar =
                            Snackbar.make(fab, getString(R.string.comment_post_success), Snackbar.LENGTH_SHORT)
                        snackbar.anchorView = fab
                        snackbar.show()
                    } else {
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                }
            })
        }
    }

    fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.comment_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.comment_activity_menu_watch_live -> {
                if (live_framelayout.visibility == View.VISIBLE) {
                    live_framelayout.visibility = View.GONE
                    live_video_view.stopPlayback()
                } else {
                    live_framelayout.visibility = View.VISIBLE
                    live_video_view.start()
                }
            }
            R.id.comment_activity_menu_open_browser -> {
                val uri = "https://live2.nicovideo.jp/watch/$liveId".toUri()
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
            R.id.comment_activity_menu_iyayo -> {
                if (item.isChecked) {
                    item.isChecked = false
                    commentCommand = ""
                } else {
                    item.isChecked = true
                    commentCommand = "184"
                }
            }
            R.id.comment_activity_menu_tts -> {
                if (item.isChecked) {
                    item.isChecked = false
                    isTTS = false
                } else {
                    item.isChecked = true
                    isTTS = true
                }
            }
            R.id.comment_activity_menu_toast -> {
                if (item.isChecked) {
                    item.isChecked = false
                    isToast = false
                } else {
                    item.isChecked = true
                    isToast = true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    //Activity終了時に閉じる
    override fun onDestroy() {
        super.onDestroy()
        connectionNicoLiveWebSocket.close()
        commentPOSTWebSocketClient.close()
        timer.cancel()
    }

}