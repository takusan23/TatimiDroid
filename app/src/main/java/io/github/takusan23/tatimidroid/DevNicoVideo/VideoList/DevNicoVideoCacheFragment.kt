package io.github.takusan23.tatimidroid.DevNicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoCacheFilterBottomFragment
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRSS
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_cache_filter.*
import kotlinx.android.synthetic.main.fragment_comment_cache.*
import kotlinx.coroutines.*

class DevNicoVideoCacheFragment : Fragment() {
    // 必要なやつ
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val recyclerViewList = arrayListOf<NicoVideoData>()
    lateinit var nicoVideoCache: NicoVideoCache

    // 重いから非同期処理
    lateinit var launch: Job

    lateinit var cacheFilterBottomFragment: DevNicoVideoCacheFilterBottomFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comment_cache, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nicoVideoCache = NicoVideoCache(context)
        initRecyclerView()
        initFabClick()
        load()
    }

    // FAB押したとき
    private fun initFabClick() {
        fragment_cache_fab.setOnClickListener {
            if (fragmentManager != null) {
                if (!::cacheFilterBottomFragment.isInitialized) {
                    cacheFilterBottomFragment = DevNicoVideoCacheFilterBottomFragment()
                }
                cacheFilterBottomFragment.apply {
                    cacheFragment = this@DevNicoVideoCacheFragment
                    show(this@DevNicoVideoCacheFragment.fragmentManager!!, "filter")
                }
            }
        }
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
                // 中身0だった場合
                if (recyclerViewList.isEmpty()) {
                    fragment_cache_empty_message.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::launch.isInitialized) {
            launch.cancel()
        }
    }

    /**
     * RecyclerView初期化
     * @param list NicoVideoDataの配列。RecyclerViewに表示させたい配列が別にある時だけ指定すればいいと思うよ
     * */
    fun initRecyclerView(list: ArrayList<NicoVideoData> = recyclerViewList) {
        fragment_cache_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            nicoVideoListAdapter = DevNicoVideoListAdapter(list)
            adapter = nicoVideoListAdapter
        }
    }

}