package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSeriesAPI
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_series.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * シリーズFragment
 *
 * 入れてほしいもの
 * series_id    | String    | シリーズID。https://sp.nicovideo.jp/series/{ここの数字}
 * series_title | String    | シリーズタイトル
 * */
class NicoVideoSeriesFragment : Fragment() {

    val prefSetting by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    // Adapter
    val nicoVideoList = arrayListOf<NicoVideoData>()
    val nicoVideoListAdapter = NicoVideoListAdapter(nicoVideoList)

    val seriesTitle by lazy { arguments?.getString("series_title") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_series, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()

        // 画面回転復帰時
        if (savedInstanceState != null) {
            // ViewModelにしたい
            (savedInstanceState.getSerializable("list") as ArrayList<NicoVideoData>).toList().forEach {
                nicoVideoList.add(it)
            }
            nicoVideoListAdapter.notifyDataSetChanged()
        } else {
            // シリーズ取得
            val seriesId = arguments?.getString("series_id") ?: return
            val userSession = prefSetting.getString("user_session", "") ?: return
            lifecycleScope.launch(Dispatchers.Default) {
                val seriesAPI = NicoVideoSeriesAPI()
                val response = seriesAPI.getSeriesVideoList(userSession, seriesId)
                seriesAPI.parseSeriesVideoList(response.body?.string()).forEach {
                    nicoVideoList.add(it)
                }
                withContext(Dispatchers.Main) {
                    // 反映
                    nicoVideoListAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        fragment_nicovideo_series_recyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = nicoVideoListAdapter
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", nicoVideoList)
    }

}