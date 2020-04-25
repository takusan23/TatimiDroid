package io.github.takusan23.tatimidroid.DevNicoVideo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRecommendAPI
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_recommend.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 関連動画を表示するFragment
 * */
class DevNicoVideoRecommendFragment : Fragment() {

    lateinit var devNicoVideoListAdapter: DevNicoVideoListAdapter
    var recyclerViewList = arrayListOf<NicoVideoData>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_recommend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
    }

    override fun onStart() {
        super.onStart()
        Handler(Looper.getMainLooper()).post {
            devNicoVideoListAdapter.notifyDataSetChanged()
        }
    }

    private fun initRecyclerView() {
        fragment_nicovideo_recommend_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            devNicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = devNicoVideoListAdapter
        }
    }

}
