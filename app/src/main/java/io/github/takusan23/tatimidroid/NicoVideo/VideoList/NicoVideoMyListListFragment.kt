package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.AllShowDropDownMenuAdapter
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoVideoMyListListViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoMyListListViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist_list.*

/**
 * マイリストの動画一覧表示Fragment。
 * ViewPagerで表示するFragmentです。
 * 入れてほしいもの↓
 * mylist_id   |String |マイリストのID。空の場合はとりあえずマイリストをリクエストします
 * mylist_is_me|Boolean|マイリストが自分のものかどうか。自分のマイリストの場合はtrue
 * */
class NicoVideoMyListListFragment : Fragment() {

    /** ViewModel */
    private lateinit var myListListViewModel: NicoVideoMyListListViewModel

    /** RecyclerViewへ渡す配列 */
    private val recyclerViewList = arrayListOf<NicoVideoData>()

    /** RecyclerViewへ入れるAdapter */
    val nicoVideoListAdapter = NicoVideoListAdapter(recyclerViewList)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_mylist_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val myListId = arguments?.getString("mylist_id")!!
        val isMe = arguments?.getBoolean("mylist_is_me")!!
        myListListViewModel = ViewModelProvider(this, NicoVideoMyListListViewModelFactory(requireActivity().application, myListId, isMe)).get(NicoVideoMyListListViewModel::class.java)

        // ダークモード
        fragment_nicovideo_mylist_list_app_bar.background = ColorDrawable(getThemeColor(requireContext()))

        // RecyclerView初期化
        initRecyclerView()

        // 並び替えメニュー初期化
        initSortMenu()

        // データ取得を待つ
        myListListViewModel.nicoVideoDataListLiveData.observe(viewLifecycleOwner) { videoList ->
            recyclerViewList.clear()
            recyclerViewList.addAll(videoList)
            nicoVideoListAdapter.notifyDataSetChanged()
        }

        // くるくる
        myListListViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            fragment_nicovideo_mylist_list_swipe.isRefreshing = isLoading
        }

        // ひっぱって更新
        fragment_nicovideo_mylist_list_swipe.setOnRefreshListener {
            myListListViewModel.getMyListVideoList()
        }

    }

    /** 並び替えメニュー初期化 */
    private fun initSortMenu() {
        val sortList = arrayListOf(
            "登録が新しい順",
            "登録が古い順",
            "再生の多い順",
            "再生の少ない順",
            "投稿日時が新しい順",
            "投稿日時が古い順",
            "再生時間の長い順",
            "再生時間の短い順",
            "コメントの多い順",
            "コメントの少ない順",
            "マイリスト数の多い順",
            "マイリスト数の少ない順"
        )
        val adapter = AllShowDropDownMenuAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortList)
        fragment_nicovideo_mylist_list_sort.apply {
            setAdapter(adapter)
            setOnItemClickListener { parent, view, position, id ->
                myListListViewModel.sort(position)
            }
            setText(sortList[0], false)
        }
    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        fragment_nicovideo_mylist_list_recyclerview.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            // Adapterセット
            adapter = nicoVideoListAdapter
        }
    }

}