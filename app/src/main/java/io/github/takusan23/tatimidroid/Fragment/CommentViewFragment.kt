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
}