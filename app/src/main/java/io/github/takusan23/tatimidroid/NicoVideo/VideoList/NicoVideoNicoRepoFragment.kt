package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoRepo.NicoRepoDataClass
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoRepoAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoRepoViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoRepoViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_nicorepo.*

/**
 * ニコレポFragment
 *　
 * userId |String | ユーザーIDを入れるとそのユーザーのニコレポを取りに行きます。ない場合は自分のニコレポを取りに行きます
 * --- にんい ---
 * show_video   | Boolean   | 初期状態から動画のチェックを入れたい場合は使ってください
 * show_live    | Boolean   | 初期状態から生放送のチェックを入れたい場合は使ってください
 * */
class NicoVideoNicoRepoFragment : Fragment() {

    /** データ取得とか保持とかのViewModel */
    private lateinit var nicoRepoViewModel: NicoRepoViewModel

    /** RecyclerViewへ渡す配列 */
    private val recyclerViewList = arrayListOf<NicoRepoDataClass>()

    /** RecyclerViewにセットするAdapter */
    private val nicoRepoAdapter = NicoRepoAdapter(recyclerViewList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_nicorepo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        nicoRepoViewModel = ViewModelProvider(this, NicoRepoViewModelFactory(requireActivity().application, userId)).get(NicoRepoViewModel::class.java)

        // RecyclerView初期化
        initRecyclerView()

        // 読み込みLiveDataうけとり
        nicoRepoViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            fragment_nicovideo_nicorepo_swipe.isRefreshing = isLoading
        }

        // 配列受け取り
        nicoRepoViewModel.nicoRepoDataListLiveData.observe(viewLifecycleOwner) { parseList ->
            recyclerViewList.clear()
            recyclerViewList.addAll(parseList)
            nicoRepoAdapter.notifyDataSetChanged()
        }

        // ひっぱって更新
        fragment_nicovideo_nicorepo_swipe.setOnRefreshListener {
            // データ消す
            recyclerViewList.clear()
            nicoRepoViewModel.getNicoRepo()
        }

        // そーと
        fragment_nicovideo_nicorepo_filter_live.setOnCheckedChangeListener { buttonView, isChecked ->
            nicoRepoViewModel.apply {
                isShowLive = isChecked
                filterAndPostLiveData()
            }
        }
        fragment_nicovideo_nicorepo_filter_video.setOnCheckedChangeListener { buttonView, isChecked ->
            nicoRepoViewModel.apply {
                isShowVideo = isChecked
                filterAndPostLiveData()
            }
        }

        // argumentの値を適用する
        fragment_nicovideo_nicorepo_filter_live.isChecked = arguments?.getBoolean("show_live", true) ?: true
        fragment_nicovideo_nicorepo_filter_video.isChecked = arguments?.getBoolean("show_video", true) ?: true
    }

    // RecyclerViewを初期化
    private fun initRecyclerView() {
        fragment_nicovideo_nicorepo_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = nicoRepoAdapter
        }
    }

}