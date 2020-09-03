package io.github.takusan23.tatimidroid.NicoLive

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoLive.Adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_commentview.*
import org.java_websocket.client.WebSocketClient

/**
 * ニコ生コメント表示Fragment
 * */
class CommentViewFragment : Fragment() {
    // 接続中の部屋名
    var recyclerViewList: ArrayList<ArrayList<String>> = arrayListOf()
    lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter

    var websocketList: ArrayList<WebSocketClient> = arrayListOf()
    lateinit var prefSetting: SharedPreferences

    // getString(R.string.arena)
    lateinit var stringArena: String

    var liveId = ""

    lateinit var commentFragment: CommentFragment

    lateinit var recyclerView: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commentview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        //LiveIDとる
        liveId = arguments?.getString("liveId") ?: ""

        recyclerView = view.findViewById(R.id.fragment_comment_recyclerview)

        stringArena = getString(R.string.arena)

        //CommentFragment取得
        commentFragment = requireParentFragment() as CommentFragment

        commentFragment.apply {
            // RecyclerView初期化
            recyclerView.setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            recyclerView.layoutManager = mLayoutManager
            commentRecyclerViewAdapter = CommentRecyclerViewAdapter(commentFragment.commentJSONList)
            recyclerView.adapter = commentRecyclerViewAdapter
            recyclerView.itemAnimator = null
            //区切り線いれる
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            recyclerView.addItemDecoration(itemDecoration)
        }

        // スクロールボタン。追従するぞい
        fragment_comment_following_button.setOnClickListener {
            // Fragmentはクソ！
            (recyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            // Visibilityゴーン。誰もカルロス・ゴーンの話しなくなったな
            setFollowingButtonVisibility(false)
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
    }

    // Adapter初期化済みかどうか
    fun isInitAdapter(): Boolean {
        return ::commentRecyclerViewAdapter.isInitialized
    }

    /**
     * スクロール位置をゴニョゴニョする関数。追加時に呼んでね
     * もし一番上なら->新しいコメント来ても一番上に追従する
     * 一番上にいない->この位置に留まる
     * */
    fun recyclerViewScrollPos() {
        // れいあうとまねーじゃー
        val linearLayoutManager = (recyclerView.layoutManager as LinearLayoutManager)
        // RecyclerViewで表示されてる中で一番上に表示されてるコメントの位置
        val visibleListFirstItemPos = linearLayoutManager.findFirstVisibleItemPosition()
        // 追いかけるボタンを利用するか
        if (prefSetting.getBoolean("setting_oikakeru_hide", false)) {
            // 利用しない
            //一番上なら追いかける
            if (visibleListFirstItemPos == 0 || visibleListFirstItemPos == 1) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0)
            }
            // 使わないので非表示
            setFollowingButtonVisibility(false)
        } else {
            // 利用する
            if (visibleListFirstItemPos == 0 || commentFragment.commentJSONList.isEmpty()) {
                recyclerView.scrollToPosition(0)
                // 追従ボタン非表示
                setFollowingButtonVisibility(false)
            } else {
                // 一番上じゃないので追従ボタン表示
                setFollowingButtonVisibility(true)
            }
        }
    }

    /**
     * コメント追いかけるボタンを表示、非表示する関数
     * @param visible 表示する場合はtrue。非表示にする場合はfalse
     * */
    fun setFollowingButtonVisibility(visible: Boolean) {
        fragment_comment_following_button?.apply {
            visibility = if (visible) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

}