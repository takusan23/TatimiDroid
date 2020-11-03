package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoPlayListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_playlist.*


/**
 * ニコ動連続再生で動画一覧を表示する
 * */
class NicoVideoPlayListBottomFragment : BottomSheetDialogFragment() {

    private lateinit var playlistAdapter: NicoVideoPlayListAdapter

    /** [io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment]のViewModel取得 */
    private val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // データ受け取り
        viewModel.videoList?.also { videoList ->
            // RecyclerViewセット
            playlistAdapter = NicoVideoPlayListAdapter(videoList, viewModel)
            playlistAdapter.nicoVideoPlayListBottomFragment = this
            initRecyclerView()

            // トータル何分
            val totalDuration = videoList.sumBy { nicoVideoData -> nicoVideoData.duration?.toInt() ?: 0 }
            nicovideo_playlist_bottom_fragment_duration.text = "${getString(R.string.playlist_total_time)}：${DateUtils.formatElapsedTime(totalDuration.toLong())}"

            // 件数
            nicovideo_playlist_bottom_fragment_count.text = "${getString(R.string.video_count)}：${videoList.size}"

            // 逆にする
            nicovideo_playlist_bottom_fragment_reverse.setOnClickListener {
                val videoListTemp = ArrayList(videoList)
                videoList.clear()
                videoList.addAll(videoListTemp.reversed())
                playlistAdapter.notifyDataSetChanged()
                scrollPlayingItem()
                viewModel.isReversed.value = !(viewModel.isReversed.value ?: false)
            }

            // しゃっふる
            nicovideo_playlist_bottom_fragment_shuffle.setOnClickListener {
                if (nicovideo_playlist_bottom_fragment_shuffle.isChecked) {
                    // シャッフル
                    val videoListTemp = ArrayList(videoList)
                    videoList.clear()
                    videoList.addAll(videoListTemp.shuffled())
                } else {
                    // シャッフル戻す。このために video_id_list が必要だったんですね
                    val idList = viewModel.originVideoSortList ?: return@setOnClickListener
                    /** [List.sortedWith]と[Comparator]を使うことで、JavaScriptの` list.sort(function(a,b){ return a - b } `みたいな２つ比べてソートができる。 */
                    val videoListTemp = ArrayList(videoList)
                    videoList.clear()
                    videoList.addAll(videoListTemp.sortedWith { a, b -> idList.indexOf(a.videoId) - idList.indexOf(b.videoId) }) // Kotlin 1.4で更に書きやすくなった
                }
                playlistAdapter.notifyDataSetChanged()
                scrollPlayingItem()
                viewModel.isShuffled.value = nicovideo_playlist_bottom_fragment_shuffle.isChecked
            }

            scrollPlayingItem()
        }

        // 閉じるボタン
        nicovideo_playlist_bottom_fragment_close.setOnClickListener {
            dismiss()
        }

        // Chipにチェックを入れる
        viewModel.isReversed.observe(viewLifecycleOwner) { isChecked ->
            nicovideo_playlist_bottom_fragment_reverse.isChecked = isChecked
        }
        viewModel.isShuffled.observe(viewLifecycleOwner) { isChecked ->
            nicovideo_playlist_bottom_fragment_shuffle.isChecked = isChecked
        }


    }

    /** 再生中の動画までスクロールする */
    private fun scrollPlayingItem() {
        val layoutManager = nicovideo_playlist_bottom_fragment_recyclerview.layoutManager as LinearLayoutManager
        // 位置を特定
        val pos = viewModel.videoList?.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == viewModel.playingVideoId.value } ?: 0
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
            requireContext().getDrawable(R.drawable.recyclerview_dividers)?.apply {
                itemDecoration.setDrawable(this) // 区切りの色変更
            }
            addItemDecoration(itemDecoration)
        }
    }

}