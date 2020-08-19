package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.Activity.NicoVideoPlayListActivity
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoPlayListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoPlayListFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_playlist.*


/**
 * ニコ動連続再生で動画一覧を表示する
 * */
class NicoVideoPlayListBottomFragment : BottomSheetDialogFragment() {

    lateinit var playlistAdapter: NicoVideoPlayListAdapter
    val nicoVideoPlayListFragment by lazy { parentFragmentManager.findFragmentByTag(NicoVideoPlayListActivity.FRAGMENT_TAG) as NicoVideoPlayListFragment }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerViewセット
        playlistAdapter = NicoVideoPlayListAdapter(nicoVideoPlayListFragment.videoList)
        playlistAdapter.nicoVideoPlayListFragment = nicoVideoPlayListFragment
        playlistAdapter.nicoVideoPlayListBottomFragment = this
        initRecyclerView()

        // なまえ
        nicovideo_playlist_bottom_fragment_name.text = nicoVideoPlayListFragment.arguments?.getString("name")

        // 閉じるボタン
        nicovideo_playlist_bottom_fragment_close.setOnClickListener {
            dismiss()
        }

        // トータル何分
        val totalDuration = nicoVideoPlayListFragment.videoList.sumBy { nicoVideoData -> nicoVideoData.duration?.toInt() ?: 0 }
        nicovideo_playlist_bottom_fragment_duration.text = "${getString(R.string.playlist_total_time)}：${DateUtils.formatElapsedTime(totalDuration.toLong())}"

        // 件数
        nicovideo_playlist_bottom_fragment_count.text = "${getString(R.string.video_count)}：${nicoVideoPlayListFragment.videoList.size}"

        // 逆にする
        nicovideo_playlist_bottom_fragment_reverse.setOnClickListener {
            nicoVideoPlayListFragment.videoList.reverse()
            playlistAdapter.notifyDataSetChanged()
        }

        // しゃっふる
        nicovideo_playlist_bottom_fragment_shuffle.setOnClickListener {
            if (nicovideo_playlist_bottom_fragment_shuffle.isChecked) {
                // シャッフル
                nicoVideoPlayListFragment.videoList.shuffle()
            } else {
                // シャッフル戻す。このために video_id_list が必要だったんですね
                val idList = nicoVideoPlayListFragment.videoIdList ?: return@setOnClickListener
                /** [List.sortedWith]と[Comparator]を使うことで、JavaScriptの` list.sort(function(a,b){ return a - b } `みたいな２つ比べてソートができる。 */
                nicoVideoPlayListFragment.videoList.sortWith(Comparator { a, b -> idList.indexOf(a.videoId) - idList.indexOf(b.videoId) })
            }
            playlistAdapter.notifyDataSetChanged()
        }

        scrollPlayingItem()

    }

    /**
     * 再生中の動画までスクロールする
     * */
    fun scrollPlayingItem() {
        val layoutManager = nicovideo_playlist_bottom_fragment_recyclerview.layoutManager as LinearLayoutManager
        // 位置を特定
        val pos = nicoVideoPlayListFragment.videoList.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == nicoVideoPlayListFragment.currentVideoId }
        layoutManager.scrollToPosition(pos)
    }

    /**
     * BottomFragmentをどこまで広げるか。
     * @param state [BottomSheetBehavior.STATE_HALF_EXPANDED] など
     * */
    fun setBottomFragmentState(state: Int) {
        (dialog as BottomSheetDialog).behavior.state = state
    }


    fun initRecyclerView() {
        nicovideo_playlist_bottom_fragment_recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = playlistAdapter
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            itemDecoration.setDrawable(ColorDrawable(Color.WHITE)) // 区切りの色変更
            addItemDecoration(itemDecoration)
        }
    }

}