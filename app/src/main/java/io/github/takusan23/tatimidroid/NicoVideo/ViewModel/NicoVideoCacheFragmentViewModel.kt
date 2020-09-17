package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.github.takusan23.tatimidroid.NicoAPI.Cache.CacheJSON
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import kotlinx.coroutines.launch
import okhttp3.internal.format

/**
 * キャッシュFragment（[io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoCacheFragment]）で使うViewModel
 *
 * 画面回転時に再読み込みをしないためのViewModel
 *
 * */
class NicoVideoCacheFragmentViewModel(application: Application) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** キャッシュ関連 */
    val nicoVideoCache = NicoVideoCache(context)

    /** キャッシュ一覧。これはフィルターする前 */
    val cacheVideoList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** RecyclerViewにわたす配列。フィルターした後 */
    val recyclerViewList = MutableLiveData<ArrayList<NicoVideoData>>()

    /** キャッシュ利用合計容量 */
    val totalUsedStorageGB = MutableLiveData<String>()

    init {
        init()
    }

    /** 読み込みしたい時に使って */
    fun init() {
        viewModelScope.launch {
            val list = arrayListOf<NicoVideoData>()
            nicoVideoCache.loadCache().forEach {
                list.add(it)
            }
            cacheVideoList.value = list
            recyclerViewList.value = list
            // フィルター適用する
            applyFilter()
            // 合計サイズ
            initStorageSpace()
        }
    }

    /** フィルターを適用する */
    fun applyFilter() {
        val filter = CacheJSON().readJSON(context)
        if (filter != null && cacheVideoList.value != null) {
            val list = nicoVideoCache.getCacheFilterList(cacheVideoList.value!!, filter)
            // LiveData更新
            recyclerViewList.postValue(list)
        }
    }

    /** 合計容量を計算する */
    private fun initStorageSpace() {
        val byte = nicoVideoCache.cacheTotalSize.toFloat()
        val gbyte = byte / 1024 / 1024 / 1024 // Byte -> KB -> MB -> GB
        totalUsedStorageGB.postValue(format("%.1f", gbyte))
    }


}
