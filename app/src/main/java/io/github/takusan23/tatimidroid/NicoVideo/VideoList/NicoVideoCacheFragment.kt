package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheFilterDataClass
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Activity.NicoVideoPlayListActivity
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoCacheFilterBottomFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_comment_cache.*
import kotlinx.android.synthetic.main.include_playlist_button.*
import kotlinx.coroutines.launch
import okhttp3.internal.format

class NicoVideoCacheFragment : Fragment() {
    // 必要なやつ
    lateinit var nicoVideoListAdapter: NicoVideoListAdapter
    val cacheVideoList = arrayListOf<NicoVideoData>() // キャッシュ一覧
    val recyclerViewList = arrayListOf<NicoVideoData>() // RecyclerViewにわたす配列
    lateinit var nicoVideoCache: NicoVideoCache

    // lateinit var cacheFilterBottomFragment: DevNicoVideoCacheFilterBottomFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comment_cache, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nicoVideoCache = NicoVideoCache(context)
        initRecyclerView()
        initFabClick()
        // 画面回転復帰時か
        if (savedInstanceState == null) {
            load()
        } else {
            (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).forEach {
                cacheVideoList.add(it)
            }
            (savedInstanceState.getSerializable("recycler") as ArrayList<NicoVideoData>).forEach {
                recyclerViewList.add(it)
            }
            fragment_cache_storage_info.text = savedInstanceState.getString("storage")
            nicoVideoListAdapter.notifyDataSetChanged()
            // 連続再生ボタン表示
            include_playlist_button.isVisible = true
        }

        // 連続再生
        include_playlist_button.setOnClickListener {
            val intent = Intent(requireContext(), NicoVideoPlayListActivity::class.java)
            // 中身を入れる
            intent.putExtra("video_list", recyclerViewList)
            intent.putExtra("name", getString(R.string.cache))
            startActivity(intent)
        }

    }

    // ストレージの空き確認
    private fun initStorageSpace() {
        val byte = nicoVideoCache.cacheTotalSize.toFloat()
        val gbyte = byte / 1024 / 1024 / 1024 // Byte -> KB -> MB -> GB
        fragment_cache_storage_info.text = "${getString(R.string.cache_usage)}：${format("%.1f", gbyte)} GB" // 小数点以下一桁
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
            initRecyclerView(list, fragment_cache_recyclerview.layoutManager?.onSaveInstanceState())
        }
    }

    // フィルター削除関数
    fun filterDeleteMessageShow() {
        // 本当に消していい？
        Snackbar.make(fragment_cache_empty_message, getString(R.string.filter_clear_message), Snackbar.LENGTH_SHORT).apply {
            setAction(getString(R.string.reset)) {
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
            val cacheFilterBottomFragment = NicoVideoCacheFilterBottomFragment().apply {
                cacheFragment = this@NicoVideoCacheFragment
            }
            cacheFilterBottomFragment.show(parentFragmentManager, "filter")
        }
    }

    // 読み込む
    fun load() {
        lifecycleScope.launch {
            recyclerViewList.clear()
            cacheVideoList.clear()
            nicoVideoCache.loadCache().forEach {
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
                // 連続再生ボタン表示
                include_playlist_button.isVisible = true
            }
        }
    }

    /**
     * RecyclerView初期化
     * @param list NicoVideoDataの配列。RecyclerViewに表示させたい配列が別にある時だけ指定すればいいと思うよ
     * @param layoutManagerParcelable RecyclerViewのスクロール位置を復元できるらしい。RecyclerView#layoutManager#onSaveInstanceState()で取れる
     * https://stackoverflow.com/questions/27816217/how-to-save-recyclerviews-scroll-position-using-recyclerview-state
     * */
    fun initRecyclerView(list: ArrayList<NicoVideoData> = recyclerViewList, layoutManagerParcelable: Parcelable? = null) {
        fragment_cache_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            if (layoutManagerParcelable != null) {
                layoutManager?.onRestoreInstanceState(layoutManagerParcelable)
            }
            nicoVideoListAdapter = NicoVideoListAdapter(list)
            adapter = nicoVideoListAdapter
        }
    }

    /**
     * 値を画面回転時に引き継ぐ
     * */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.apply {
            putString("storage", fragment_cache_storage_info.text.toString())
            putSerializable("recycler", recyclerViewList)
            putSerializable("list", cacheVideoList)
        }

    }

}