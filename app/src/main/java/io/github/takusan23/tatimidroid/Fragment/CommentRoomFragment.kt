package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_comment_room_layout.*
import kotlinx.android.synthetic.main.fragment_commentview.*
import okhttp3.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.java_websocket.protocols.IProtocol
import org.java_websocket.protocols.Protocol
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.net.URI
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class CommentRoomFragment : Fragment() {

    lateinit var pref_setting: SharedPreferences

    //接続中の部屋名
    var recyclerViewList: ArrayList<ArrayList<String>> = arrayListOf()
    lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    //部屋の配列
    val commentRoomList = arrayListOf<String>()
    //WebSocketの配列
    val commentRoomWebSocketList = arrayListOf<String>()
    //threadの配列
    val commentRoomThreadList = arrayListOf<String>()
    //定期的に立ち見席が出てないかチェック
    val timer = Timer()
    //コメントWebSocket
    lateinit var webSocketClient: WebSocketClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_comment_room_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //ここから下三行必須
        recyclerViewList = ArrayList()
        comment_room_recycler_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        comment_room_recycler_view.layoutManager = mLayoutManager
        commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList)
        comment_room_recycler_view.adapter = commentRecyclerViewAdapter
        recyclerViewLayoutManager = comment_room_recycler_view.layoutManager!!
        comment_room_recycler_view.setItemAnimator(null);

        //番組情報取得
        getProgramInfo()

        //定期的に立ち見席が出てないかチェック
        timer.schedule(60000, 60000) {
            getProgramInfo()
        }

        //TabLayout選択
        comment_room_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    //初期化済みか
                    if (this@CommentRoomFragment::webSocketClient.isInitialized) {
                        webSocketClient.close()
                    }
                    connectWebSocket(tab.position)
                }
            }
        })
    }

    //番組
    fun getProgramInfo() {
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
                        val roomName = roomObject.getString("name")
                        val threadId = roomObject.getString("threadId")
                        //配列に入れる
                        commentRoomList.add(roomName)
                        commentRoomWebSocketList.add(webSocketUri)
                        commentRoomThreadList.add(threadId)
                    }
                    //TabLayoutに入れる
                    setTabItem()
                    //とりあえずアリーナに接続
                    //connectWebSocket(0)
                } else {
                    activity?.runOnUiThread {
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                }
            }
        })
    }

    //立ち見席をTabLayoutに入れる
    fun setTabItem() {
        activity?.runOnUiThread {
            //for
            comment_room_tablayout.removeAllTabs()
            for (name in commentRoomList) {
                //Tab作成
                val tab: TabLayout.Tab = comment_room_tablayout.newTab()
                tab.text = name
                comment_room_tablayout.addTab(tab)
            }
        }
    }

    //コメントサーバー接続
    fun connectWebSocket(listPosition: Int) {
        //RecyclerViewを空にする
        recyclerViewList.clear()
        //配列から取得
        val url = commentRoomWebSocketList.get(listPosition)
        val thread = commentRoomThreadList.get(listPosition)
        val room = commentRoomList.get(listPosition)
        //System.out.println(room)
        val uri = URI(url)
        val protocol = Draft_6455(
            Collections.emptyList(),
            Collections.singletonList(Protocol("msg.nicovideo.jp#json")) as List<IProtocol>?
        )
        webSocketClient = object : WebSocketClient(uri, protocol) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                //System.out.println("コメントサーバー接続")
                //スレッド番号、過去コメントなど必要なものを最初に送る
                val sendJSONObject = JSONObject()
                val jsonObject = JSONObject()
                jsonObject.put("version", "20061206")
                jsonObject.put("thread", thread)
                jsonObject.put("service", "LIVE")
                jsonObject.put("score", 1)
                jsonObject.put("res_from", -100)
                sendJSONObject.put("thread", jsonObject)
                webSocketClient.send(sendJSONObject.toString())
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {

            }

            override fun onMessage(message: String?) {
                //Adaprer
                if (message != null) {
                    addItemRecyclerView(message, room)
                }
            }

            override fun onError(ex: Exception?) {

            }

        }
        //接続
        webSocketClient.connect()
    }

    /*RecyclerViewについかする*/
    fun addItemRecyclerView(json: String, roomName: String) {
        val item = arrayListOf<String>()
        item.add("")
        item.add(json)
        item.add(roomName)
        recyclerViewList.add(0, item)
        //RecyclerView更新
        activity?.runOnUiThread {
            if (comment_room_recycler_view != null) {
                commentRecyclerViewAdapter.notifyItemInserted(0)
                // 画面上で最上部に表示されているビューのポジションとTopを記録しておく
                val pos = (recyclerViewLayoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                var top = 0
                if ((recyclerViewLayoutManager as LinearLayoutManager).childCount > 0) {
                    top = (recyclerViewLayoutManager as LinearLayoutManager).getChildAt(0)!!.top
                }
                //一番上なら追いかける
                //System.out.println(pos)
                if (pos == 0 || pos == 1) {
                    comment_room_recycler_view.scrollToPosition(0)
                } else {
                    comment_room_recycler_view.post {
                        (recyclerViewLayoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos + 1, top)
                    }
                }
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
        webSocketClient.close()
    }

}