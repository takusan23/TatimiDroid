package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoVideoSeriesViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoSeriesViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_series.*

/**
 * シリーズの動画一覧Fragment
 *
 * レイアウトはシリーズ一覧をそのまま使いまわしてる
 *
 * 入れてほしいもの
 * series_id    | String    | シリーズID。https://sp.nicovideo.jp/series/{ここの数字}
 * */
class NicoVideoSeriesFragment : Fragment() {

    /** データ取得などViewModel */
    private lateinit var nicoVideoSeriesViewModel: NicoVideoSeriesViewModel

    /** RecyclerViewにわたす配列 */
    private val nicoVideoList = arrayListOf<NicoVideoData>()

    /** RecyclerViewにセットするAdapter */
    private val nicoVideoListAdapter = NicoVideoListAdapter(nicoVideoList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_series, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seriesId = arguments?.getString("series_id")!!
        nicoVideoSeriesViewModel = ViewModelProvider(this, NicoVideoSeriesViewModelFactory(requireActivity().application, seriesId)).get(NicoVideoSeriesViewModel::class.java)

        initRecyclerView()

        // シリーズ動画一覧取得
        nicoVideoSeriesViewModel.nicoVideoDataListLiveData.observe(viewLifecycleOwner) { list ->
            nicoVideoList.clear()
            nicoVideoList.addAll(list)
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        // 読み込み
        nicoVideoSeriesViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            fragment_nicovideo_series_refresh.isRefreshing = isLoading
        }

        // ひっぱって更新
        fragment_nicovideo_series_refresh.setOnRefreshListener {
            nicoVideoSeriesViewModel.getSeriesVideoList()
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

}