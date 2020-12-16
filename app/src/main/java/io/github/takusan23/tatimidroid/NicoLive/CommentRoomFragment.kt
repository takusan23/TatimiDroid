package io.github.takusan23.tatimidroid.NicoLive

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoLive.Adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_comment_room_layout.*

/**
 * 部屋別表示
 * */
class CommentRoomFragment : Fragment() {

    // CommentFragmentとそれのViewModel
    val commentFragment by lazy { requireParentFragment() as CommentFragment }
    val viewModel by viewModels<NicoLiveViewModel>({ commentFragment })

    lateinit var pref_setting: SharedPreferences

    var recyclerViewList = arrayListOf<CommentJSONParse>()
    lateinit var commentRecyclerViewAdapter: CommentRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_comment_room_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //ここから下三行必須
        initRecyclerView()

        // LiveDataで新規コメント監視
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { comment ->
            if (comment.roomName == comment_room_tablayout.getTabAt(comment_room_tablayout.selectedTabPosition)?.text) {
                recyclerViewList.add(0, comment)
                commentRecyclerViewAdapter.notifyItemInserted(0)
                recyclerViewScrollPos()
            }
        }

    }

    // Fragmentに来たら
    override fun onResume() {
        super.onResume()
        // 今つながってる部屋分TabItem生成する
        comment_room_tablayout.removeAllTabs()
        // 部屋統合+Store
        comment_room_tablayout.addTab(comment_room_tablayout.newTab().setText(getString(R.string.room_integration)))
        comment_room_tablayout.addTab(comment_room_tablayout.newTab().setText(getString(R.string.room_limit)))
        //TabLayout選択
        comment_room_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                // 接続
                if (tab?.text == getString(R.string.room_integration)) {
                    setCommentList(true)
                } else {
                    setCommentList(false)
                }
            }
        })
        setCommentList(true)
    }

    /**
     * コメント一覧を作成する。コメント内容はViewModelとLiveDataからもらうので、WebSocket接続はしない
     * @param isAllRoom 部屋統合のコメントを表示する場合はtrue
     * */
    private fun setCommentList(isAllRoom: Boolean = true) {
        val roomName = if (isAllRoom) getString(R.string.room_integration) else getString(R.string.room_limit)
        val list = viewModel.commentList.filter { commentJSONParse: CommentJSONParse? -> commentJSONParse?.roomName == roomName }
        recyclerViewList.clear()
        recyclerViewList.addAll(list)
        commentRecyclerViewAdapter.notifyDataSetChanged()
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
        commentRecyclerViewAdapter = CommentRecyclerViewAdapter(recyclerViewList, commentFragment)
        comment_room_recycler_view.adapter = commentRecyclerViewAdapter
        recyclerViewLayoutManager = comment_room_recycler_view.layoutManager!!
        comment_room_recycler_view.itemAnimator = null
        //区切り線いれる
        val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        comment_room_recycler_view.addItemDecoration(itemDecoration)
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}