package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_recommend.*

/**
 * 関連動画を表示するFragment
 * */
class NicoVideoRecommendFragment : Fragment() {

    lateinit var nicoVideoListAdapter: NicoVideoListAdapter
    var recyclerViewList = arrayListOf<NicoVideoData>()

    var id = ""

    /** [NicoVideoFragment]。このFragmentが置いてあるFragment。by lazy で使われるまで初期化しないように */
    private val devNicoVideoFragment by lazy {
        val videoId = arguments?.getString("id")
        parentFragmentManager.findFragmentByTag(videoId) as? NicoVideoFragment
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_recommend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        id = arguments?.getString("id") ?: return
        initRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        // initRecyclerView()
    }

    fun initRecyclerView() {
        if (!isAdded || devNicoVideoFragment == null) {
            return
        }
        // DevNicoVideoFragmentから受け取る
        recyclerViewList.clear()
        devNicoVideoFragment?.recommendList?.forEach {
            recyclerViewList.add(it)
        }
        fragment_nicovideo_recommend_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

}
