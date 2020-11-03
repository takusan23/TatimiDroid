package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_recommend.*

/**
 * 関連動画を表示するFragment
 * */
class NicoVideoRecommendFragment : Fragment() {

    private lateinit var nicoVideoListAdapter: NicoVideoListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_recommend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: NicoVideoViewModel by viewModels({ requireParentFragment() })

        // 関連動画監視
        viewModel.recommendList.observe(viewLifecycleOwner) { list ->
            initRecyclerView(list)
        }
    }

    fun initRecyclerView(list: ArrayList<NicoVideoData>) {
        fragment_nicovideo_recommend_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(list)
            adapter = nicoVideoListAdapter
        }
    }

}
