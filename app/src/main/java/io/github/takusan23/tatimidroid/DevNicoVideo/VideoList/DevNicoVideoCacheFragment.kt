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
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.DevNicoVideo.Adapter.DevNicoVideoListAdapter
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoCacheFilterBottomFragment
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheFilterDataClass
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoRSS
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_cache_filter.*
import kotlinx.android.synthetic.main.fragment_comment_cache.*
import kotlinx.coroutines.*
import okhttp3.internal.format

class DevNicoVideoCacheFragment : Fragment() {
    // 必要なやつ
    lateinit var nicoVideoListAdapter: DevNicoVideoListAdapter
    val cacheVideoList = arrayListOf<NicoVideoData>() // キャッシュ一覧
    val recyclerViewList = arrayListOf<NicoVideoData>() // RecyclerViewにわたす配列
    lateinit var nicoVideoCache: NicoVideoCache

    // 重いから非同期処理
    lateinit var launch: Job

    // lateinit var cacheFilterBottomFragment: DevNicoVideoCacheFilterBottomFragment

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

    // ストレージの空き確認
    private fun initStorageSpace() {
        val byte = nicoVideoCache.cacheTotalSize.toFloat()
        val gbyte = byte / 1024 / 1024 / 1024 // Byte -> KB -> MB -> GB
        fragment_cache_storage_info.text = "${getString(R.string.cache_usage)}：${format("%.1f",gbyte)} GB" // 小数点以下一桁
    }

    // フィルター読み込む
    private fun loadFilter() {
        val filter = CacheJSON().readJSON(context)
        if (filter != null) {
            applyFilter(filter)
        }
    }

    // Filterを適用する
    fun applyFilter(filterDataClass: CacheFilterDataClass) {
        val list = nicoVideoCache.getCacheFilterList(cacheVideoList, filterDataClass)
        activity?.runOnUiThread {
            initRecyclerView(list)
        }
    }

    // フィルター削除関数
    fun filterDeleteMessageShow() {
        // 本当に消していい？
        Snackbar.make(fragment_cache_empty_message, getString(R.string.filter_clear_message), Snackbar.LENGTH_SHORT)
            .apply {
                setAction(getString(R.string.delete_ok)) {
                    CacheJSON().deleteFilterJSONFile(context)
                    initRecyclerView()
                }
                anchorView = fragment_cache_fab
                show()
            }
    }

    // FAB押したとき
    private fun initFabClick() {
        fragment_cache_fab.setOnClickListener {
            if (fragmentManager != null) {
                val cacheFilterBottomFragment = DevNicoVideoCacheFilterBottomFragment().apply {
                    cacheFragment = this@DevNicoVideoCacheFragment
                }
                cacheFilterBottomFragment.show(fragmentManager!!, "filter")
            }
        }
    }

    // 読み込む
    fun load() {
        launch = GlobalScope.launch {
            recyclerViewList.clear()
            nicoVideoCache.loadCache().await().forEach {
                recyclerViewList.add(it)
                cacheVideoList.add(it)
            }
            // フィルター読み込む
            loadFilter()
            activity?.runOnUiThread {
                nicoVideoListAdapter.notifyDataSetChanged()
                // 中身0だった場合
                if (recyclerViewList.isEmpty()) {
                    fragment_cache_empty_message.visibility = View.VISIBLE
                }
                // 合計サイズ
                initStorageSpace()
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