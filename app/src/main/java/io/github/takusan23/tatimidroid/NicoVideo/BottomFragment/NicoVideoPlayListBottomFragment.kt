package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.Activity.NicoVideoPlayListActivity
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoPlayListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoPlayListFragment
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoPlayListViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_playlist.*


/**
 * ニコ動連続再生で動画一覧を表示する
 * */
class NicoVideoPlayListBottomFragment : BottomSheetDialogFragment() {

    private lateinit var playlistAdapter: NicoVideoPlayListAdapter
    private val nicoVideoPlayListFragment by lazy { parentFragmentManager.findFragmentByTag(NicoVideoPlayListActivity.FRAGMENT_TAG) as NicoVideoPlayListFragment }

    /** [NicoVideoPlayListFragment]のViewModel。スコープは[NicoVideoPlayListFragment]（Activityではない）  */
    private lateinit var viewModel: NicoVideoPlayListViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModel初期化
        viewModel = ViewModelProvider(nicoVideoPlayListFragment).get(NicoVideoPlayListViewModel::class.java)

        // データ受け取り
        viewModel.playListVideoList.observe(viewLifecycleOwner) { videoList ->

            // RecyclerViewセット
            playlistAdapter = NicoVideoPlayListAdapter(videoList, viewModel)
            playlistAdapter.nicoVideoPlayListFragment = nicoVideoPlayListFragment
            playlistAdapter.nicoVideoPlayListBottomFragment = this
            initRecyclerView()

            // トータル何分
            val totalDuration = videoList.sumBy { nicoVideoData -> nicoVideoData.duration?.toInt() ?: 0 }
            nicovideo_playlist_bottom_fragment_duration.text = "${getString(R.string.playlist_total_time)}：${DateUtils.formatElapsedTime(totalDuration.toLong())}"

            // 件数
            nicovideo_playlist_bottom_fragment_count.text = "${getString(R.string.video_count)}：${videoList.size}"

            // 逆にする
            nicovideo_playlist_bottom_fragment_reverse.setOnClickListener {
                // 実は、ArrayListにaddしても、LiveDataには通知が行かない。代入(postValue)が無いと通知されない
                viewModel.playListVideoList.value = ArrayList(videoList.reversed())
                playlistAdapter.notifyDataSetChanged()
                viewModel.isReverseMode.value = !(viewModel.isReverseMode.value ?: false)
            }

            // しゃっふる
            nicovideo_playlist_bottom_fragment_shuffle.setOnClickListener {
                if (nicovideo_playlist_bottom_fragment_shuffle.isChecked) {
                    // シャッフル
                    viewModel.playListVideoList.value = ArrayList(videoList.shuffled())
                } else {
                    // シャッフル戻す。このために video_id_list が必要だったんですね
                    val idList = viewModel.playListVideoIdList.value ?: return@setOnClickListener
                    /** [List.sortedWith]と[Comparator]を使うことで、JavaScriptの` list.sort(function(a,b){ return a - b } `みたいな２つ比べてソートができる。 */
                    viewModel.playListVideoList.value = ArrayList(videoList.sortedWith { a, b -> idList.indexOf(a.videoId) - idList.indexOf(b.videoId) }) // Kotlin 1.4で更に書きやすくなった
                }
                playlistAdapter.notifyDataSetChanged()
                viewModel.isEnableShuffleMode.value = nicovideo_playlist_bottom_fragment_shuffle.isChecked
            }

            scrollPlayingItem()
        }

        // なまえ
        nicovideo_playlist_bottom_fragment_name.text = viewModel.playListName.value

        // 閉じるボタン
        nicovideo_playlist_bottom_fragment_close.setOnClickListener {
            dismiss()
        }
        
        // Chipにチェックを入れる
        viewModel.isReverseMode.observe(viewLifecycleOwner) { isChecked ->
            nicovideo_playlist_bottom_fragment_reverse.isChecked = isChecked
        }
        viewModel.isEnableShuffleMode.observe(viewLifecycleOwner) { isChecked ->
            nicovideo_playlist_bottom_fragment_shuffle.isChecked = isChecked
        }


    }

    /** 再生中の動画までスクロールする */
    fun scrollPlayingItem() {
        val layoutManager = nicovideo_playlist_bottom_fragment_recyclerview.layoutManager as LinearLayoutManager
        // 位置を特定
        val pos = viewModel.playListVideoList.value?.indexOfFirst { nicoVideoData -> nicoVideoData.videoId == viewModel.playingVideoId.value } ?: 0
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