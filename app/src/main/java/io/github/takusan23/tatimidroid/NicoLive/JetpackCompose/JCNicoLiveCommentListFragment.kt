package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.droppopalert.DropPopAlert
import io.github.takusan23.droppopalert.toDropPopAlert
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoLive.Adapter.CommentRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveRoomVisibleBottomFragment
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentJcNicoliveCommentListBinding
import java.util.*

/**
 * Jetpack Compose 版ニコ生のコメント一覧
 *
 * アクティブ人数表示のために[io.github.takusan23.tatimidroid.NicoLive.CommentViewFragment]が使い回せなかった
 *
 * [requireParentFragment]が[JCNicoLiveFragment]を指すようにしてください。
 *
 * 注意点としては、[io.github.takusan23.tatimidroid.NicoLive.CommentViewFragment]と若干違います。
 *
 * 旧式は[NicoLiveViewModel.commentList]をRecyclerViewのAdapterに渡してましたが、
 * JC版はこっちで配列を用意し[NicoLiveViewModel.commentReceiveLiveData]でコメントを受け取って追加していく方式を取りました。
 * */
class JCNicoLiveCommentListFragment : Fragment() {

    /** レイアウト */
    private val viewBinding by lazy { FragmentJcNicoliveCommentListBinding.inflate(layoutInflater) }

    /** ViewModel */
    private val viewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

    /** Adapter */
    private val commentRecyclerViewAdapter by lazy {
        // ViewModelにあるコメント配列をコピーする
        CommentRecyclerViewAdapter(
            commentList = viewModel.commentList.filter { isReceiveCommentCheck(it) } as ArrayList<CommentJSONParse>,
            commentFragment = requireParentFragment()
        )
    }

    /** 保存するやつ */
    private val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ダークモード
        viewBinding.fragmentJcNicoliveCommentListAppBar.background = ColorDrawable(getThemeColor(requireContext()))

        // RecyclerView初期化
        viewBinding.fragmentJcNicoliveCommentListRecyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = commentRecyclerViewAdapter
            itemAnimator = null
            //区切り線いれる
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }

        // CommentFragmentのViewModelにあるコメントLiveDataを監視する
        viewModel.commentReceiveLiveData.observe(viewLifecycleOwner) { comment ->
            // 流量制限は表示しない設定かもしれない？
            if (isReceiveCommentCheck(comment)) {
                // コメント追加
                commentRecyclerViewAdapter.commentList.add(0, comment)
                commentRecyclerViewAdapter.notifyItemInserted(0)
                recyclerViewScrollPos()
            }
        }
        viewModel.updateRecyclerViewLiveData.observe(viewLifecycleOwner) {
            commentRecyclerViewAdapter.notifyDataSetChanged()
        }

        // 追いかけるボタン
        viewBinding.fragmentJcNicoliveCommentListJumpButton.setOnClickListener {
            (viewBinding.fragmentJcNicoliveCommentListRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(0, 0)
            viewBinding.fragmentJcNicoliveCommentListJumpButton.isVisible = false
        }

        // 統計情報受け取り
        viewModel.statisticsLiveData.observe(viewLifecycleOwner) { statistics ->
            viewBinding.fragmentJcNicoliveCommentListTotalUserTextView.text = statistics.viewers.toString()
            viewBinding.fragmentJcNicoliveCommentListCommentCountTextView.text = statistics.comments.toString()
        }

        // アクティブ人数（一分間の）
        viewModel.activeCommentPostUserLiveData.observe(viewLifecycleOwner) { activeMessage ->
            viewBinding.fragmentJcNicoliveCommentListActiveCountTextView.text = activeMessage
        }

        // アクティブ人数押した時
        viewBinding.fragmentJcNicoliveCommentListActiveCountTextView.setOnClickListener {
            viewModel.calcToukei(true)
        }

        // 表示する部屋（コメントサーバー）の設定
        viewBinding.fragmentJcNicoliveCommentListRoomImageView.setOnClickListener {
            NicoLiveRoomVisibleBottomFragment().show(parentFragmentManager, "room")
        }

        // 表示する部屋（コメントサーバー）の設定 は公式番組では使えないので
        viewModel.nicoLiveProgramData.observe(viewLifecycleOwner) { data ->
            if (data.isOfficial) {
                viewBinding.fragmentJcNicoliveCommentListRoomImageView.isVisible = true
            }
        }

        /**
         * 表示するコメントサーバーの設定が切り替わった
         * [NicoLiveRoomVisibleBottomFragment]参照
         * */
        viewModel.isReceiveArenaCommentLiveData.observe(viewLifecycleOwner) { filterReceiveComment() }
        viewModel.isReceiveLimitCommentLiveData.observe(viewLifecycleOwner) { filterReceiveComment() }
    }

    /**
     * 表示するコメント鯖の設定が切り替わったら読んでください。RecyclerViewに渡してる配列を更新します。
     *
     * [NicoLiveRoomVisibleBottomFragment]参照
     * */
    private fun filterReceiveComment() {
        // 新しい配列を作ってRecyclerViewへ渡す
        val filteredList = viewModel.commentList.filter { commentJSONParse ->
            return@filter isReceiveCommentCheck(commentJSONParse)
        }
        // 配列を更新とRecyclerView更新
        commentRecyclerViewAdapter.apply {
            commentList.clear()
            commentList.addAll(filteredList)
            notifyDataSetChanged()
        }
    }

    /**
     * [isReceiveLimitComment]や[isReceiveArenaComment]の値からコメントを追加すべきかどうかを返す
     *
     * [NicoLiveRoomVisibleBottomFragment]参照
     * */
    private fun isReceiveCommentCheck(commentJSONParse: CommentJSONParse): Boolean {
        // 表示設定読み出し
        val isShowArena = viewModel.isReceiveArenaCommentLiveData.value ?: true
        val isShowLimit = viewModel.isReceiveLimitCommentLiveData.value ?: true
        // 部屋統合表示しない + 部屋統合コメント
        if (!isShowArena && commentJSONParse.roomName == getString(R.string.room_integration)) {
            return false
        }
        // 流量制限表示しない + 流量制限コメント
        if (!isShowLimit && commentJSONParse.roomName == getString(R.string.room_limit)) {
            return false
        }
        return true
    }

    /**
     * スクロール位置をゴニョゴニョする関数。追加時に呼んでね
     * もし一番上なら->新しいコメント来ても一番上に追従する
     * 一番上にいない->この位置に留まる
     * */
    fun recyclerViewScrollPos() {
        // れいあうとまねーじゃー
        val linearLayoutManager = (viewBinding.fragmentJcNicoliveCommentListRecyclerView.layoutManager as LinearLayoutManager)
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
            viewBinding.fragmentJcNicoliveCommentListJumpButton.isVisible = false
        } else {
            // 利用する
            if (visibleListFirstItemPos == 0 || viewModel.commentList.isEmpty()) {
                viewBinding.fragmentJcNicoliveCommentListRecyclerView.scrollToPosition(0)
                // 追従ボタン非表示
                viewBinding.fragmentJcNicoliveCommentListJumpButton.isVisible = false
            } else {
                // 一番上じゃないので追従ボタン表示
                if (!viewBinding.fragmentJcNicoliveCommentListJumpButton.isVisible) {
                    viewBinding.fragmentJcNicoliveCommentListJumpButton.toDropPopAlert().showAlert(DropPopAlert.ALERT_DROP)
                }
            }
        }
    }

}