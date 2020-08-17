package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerViewセット
        val nicoVideoPlayListFragment = parentFragmentManager.findFragmentByTag(NicoVideoPlayListActivity.FRAGMENT_TAG) as NicoVideoPlayListFragment
        playlistAdapter = NicoVideoPlayListAdapter(nicoVideoPlayListFragment.videoList)
        playlistAdapter.nicoVideoPlayListFragment = nicoVideoPlayListFragment
        initRecyclerView()

    }

    private fun initRecyclerView() {
        nicovideo_playlist_bottom_fragment_recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = playlistAdapter
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }
    }

}