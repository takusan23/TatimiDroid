package io.github.takusan23.tatimidroid.NicoVideo.VideoList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoMyListAdapter
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.Factory.NicoVideoMyListViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoMyListViewModel
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_mylist.*

/**
 * マイリスト一覧Fragment。
 *
 * 動画一覧ではない。
 * */
class NicoVideoMyListFragment : Fragment() {

    /** ViewModel */
    private lateinit var myListViewModel: NicoVideoMyListViewModel

    /** RecyclerViewにわたす配列 */
    private val recyclerViewList = arrayListOf<NicoVideoSPMyListAPI.MyListData>()

    /** RecyclerViewのAdapter */
    private val myListAdapter by lazy { NicoVideoMyListAdapter(recyclerViewList, this.id, parentFragmentManager) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_mylist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId")
        myListViewModel = ViewModelProvider(this, NicoVideoMyListViewModelFactory(requireActivity().application, userId)).get(NicoVideoMyListViewModel::class.java)

        initRecyclerView()

        // マイリスト一覧受け取り
        myListViewModel.myListDataLiveData.observe(viewLifecycleOwner) { myListItems ->
            recyclerViewList.clear()
            recyclerViewList.addAll(myListItems)
            myListAdapter.notifyDataSetChanged()
        }

        // 読み込み
        myListViewModel.loadingLiveData.observe(viewLifecycleOwner) { isLoading ->
            fragment_nicovideo_mylist_refresh.isRefreshing = isLoading
        }

        // ひっぱって更新
        fragment_nicovideo_mylist_refresh.setOnRefreshListener {
            myListViewModel.getMyListList()
        }

    }

    /** RecyclerView初期化 */
    private fun initRecyclerView() {
        fragment_nicovideo_mylist_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = myListAdapter
            // 区切り線
            val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            addItemDecoration(itemDecoration)
        }
    }


}