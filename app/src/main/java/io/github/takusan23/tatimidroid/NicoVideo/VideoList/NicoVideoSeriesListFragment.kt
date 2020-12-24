package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoSeriesData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoSeriesAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoVideoSeriesListViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoSeriesListViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_series.*

/**
 * シリーズ一覧表示Fragment
 *
 * いれるもの
 * userId   |String | ユーザーID。なければ自分のを取ってくる
 * */
class NicoVideoSeriesListFragment : Fragment() {

    /** データ取得とか保持とかのViewModel */
    private lateinit var seriesListViewModel: NicoVideoSeriesListViewModel

    /** RecyclerViewへ渡す配列 */
    val seriesList = arrayListOf<NicoVideoSeriesData>()

    /** RecyclerViewのAdapter */
    val nicoVideoSeriesAdapter by lazy { NicoVideoSeriesAdapter(seriesList, this.id, parentFragmentManager) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_series, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        seriesListViewModel = ViewModelProvider(this, NicoVideoSeriesListViewModelFactory(requireActivity().application, userId)).get(NicoVideoSeriesListViewModel::class.java)

        initRecyclerView()

        // シリーズ一覧受け取り
        seriesListViewModel.nicoVideoDataListLiveData.observe(viewLifecycleOwner) { list ->
            seriesList.clear()
            seriesList.addAll(list)
            nicoVideoSeriesAdapter.notifyDataSetChanged()
        }

        // 読み込み
        seriesListViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            fragment_nicovideo_series_refresh.isRefreshing = isLoading
        }

        // ひっぱって更新
        fragment_nicovideo_series_refresh.setOnRefreshListener {
            seriesListViewModel.getSeriesList()
        }

    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        fragment_nicovideo_series_recyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = nicoVideoSeriesAdapter
            // 区切り線
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }
    }

}