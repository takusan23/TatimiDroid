package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.Adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveComment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTagAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_comment_room_layout.*
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

/**
 * 部屋別表示
 * */
class CommentRoomFragment : Fragment() {

    lateinit var commentFragment: CommentFragment

    lateinit var pref_setting: SharedPreferences

    var liveId = ""

    //接続中の部屋名
    var recyclerViewList = arrayListOf<CommentJSONParse>()
    lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    //定期的に立ち見席が出てないかチェック
    val timer = Timer()

    //コメントWebSocket
    lateinit var webSocketClient: WebSocketClient

    // ニコ生コメントサーバー接続など
    val nicoLiveComment = NicoLiveComment()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_comment_room_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        liveId = arguments?.getString("liveId") ?: ""

        //ここから下三行必須
        initRecyclerView()

        //CommentFragment取得
        val fragment = fragmentManager?.findFragmentByTag(liveId)
        commentFragment = fragment as CommentFragment

    }

    // Fragmentに来たら
    override fun onResume() {
        super.onResume()
        // 今つながってる部屋分TabItem生成する
        comment_room_tablayout.removeAllTabs()
        // 部屋統合+Store
        comment_room_tablayout.addTab(comment_room_tablayout.newTab().setText(getString(R.string.room_integration)).setTag(commentFragment.commentServerData))
        comment_room_tablayout.addTab(comment_room_tablayout.newTab().setText(getString(R.string.room_limit)).setTag(commentFragment.storeCommentServerData))
        //TabLayout選択
        comment_room_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                // 接続
                val tag = tab?.tag
                if (tag is NicoLiveComment.CommentServerData) {
                    connectionWebSocket(tag)
                }
            }
        })

        if (commentFragment.commentServerData != null) {
            connectionWebSocket(commentFragment.commentServerData!!)
        }
    }

    // Fragmentから離れたら
    override fun onPause() {
        super.onPause()
        nicoLiveComment.destroy()
    }

    private fun connectionWebSocket(data: NicoLiveComment.CommentServerData) {
        // List消す+WebSocket切断
        recyclerViewList.clear()
        commentRecyclerViewAdapter.notifyDataSetChanged()
        nicoLiveComment.destroy()
        // 接続
        nicoLiveComment.connectionWebSocket(data.webSocketUri, data.threadId, data.roomName) { commentText, roomMane, isHistory ->
            val commentJSONParse = CommentJSONParse(commentText, roomMane, liveId)
            Handler(Looper.getMainLooper()).post {
                // UI Thread
                if (commentJSONParse.origin != "C" || commentFragment.nicoLiveHTML.isOfficial) {
                    recyclerViewList.add(0, commentJSONParse)
                    commentRecyclerViewAdapter.notifyItemInserted(0)
                    recyclerViewScrollPos()
                }
            }
        }
    }

    /**
     * スクロール位置をゴニョゴニョする関数。追加時に呼んでね
     * もし一番上なら->新しいコメント来ても一番上に追従する
     * 一番上にいない->この位置に留まる
     * */
    private fun recyclerViewScrollPos() {
        if (!isAdded) return
        // 画面上で最上部に表示されているビューのポジションとTopを記録しておく
        val pos =
            (comment_room_recycler_view.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        var top = 0
        if ((comment_room_recycler_view.layoutManager as LinearLayoutManager).childCount > 0) {
            top = (comment_room_recycler_view.layoutManager as LinearLayoutManager).getChildAt(0)!!.top
        }
        //一番上なら追いかける
        if (pos == 0 || pos == 1) {
            comment_room_recycler_view.scrollToPosition(0)
        } else {
            comment_room_recycler_view.post {
                (comment_room_recycler_view.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(
                    pos + 1,
                    top
                )
            }
        }
    }

    private fun initRecyclerView() {
        comment_room_recycler_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        comment_room_recycler_view.layoutManager = mLayoutManager
        commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList)
        comment_room_recycler_view.adapter = commentRecyclerViewAdapter
        recyclerViewLayoutManager = comment_room_recycler_view.layoutManager!!
        comment_room_recycler_view.itemAnimator = null
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::webSocketClient.isInitialized) {
            webSocketClient.close()
        }
    }

}