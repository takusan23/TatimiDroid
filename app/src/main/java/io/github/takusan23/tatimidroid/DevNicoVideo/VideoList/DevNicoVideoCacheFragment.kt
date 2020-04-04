package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoRSS
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_comment_cache.*
import kotlinx.coroutines.*
import java.io.File

class DevNicoVideoCacheFragment : Fragment() {

    val nicoRSS = NicoVideoRSS()
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoCache: NicoVideoCache

    lateinit var launch: Job

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comment_cache, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nicoVideoCache = NicoVideoCache(context)
        initRecyclerView()

        load()

    }

    // 読み込む
    fun load() {
        launch = GlobalScope.launch {
            recyclerViewList.clear()
            nicoVideoCache.loadCache().await().forEach {
                recyclerViewList.add(it)
            }
            activity?.runOnUiThread {
                nicoVideoListAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::launch.isInitialized) {
            launch.cancel()
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        fragment_cache_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(recyclerViewList)
            adapter = nicoVideoListAdapter
        }
    }

}