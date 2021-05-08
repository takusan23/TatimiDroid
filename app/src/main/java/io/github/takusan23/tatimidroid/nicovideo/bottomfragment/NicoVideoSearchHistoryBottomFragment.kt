package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.databinding.BottomFragmentSearchHistoryBinding
import io.github.takusan23.tatimidroid.nicovideo.adapter.NicoVideoSearchHistoryAdapter
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchHistoryViewModel
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchViewModel

/**
 * 検索履歴BottomFragment
 * */
class NicoVideoSearchHistoryBottomFragment : BottomSheetDialogFragment() {

    /** ViewModel */
    private val viewModel by viewModels<NicoVideoSearchHistoryViewModel>()

    /** 多分親Fragmentが検索Fragmentになるので */
    private val parentViewModel by viewModels<NicoVideoSearchViewModel>({ requireParentFragment() })

    /** ViewBinding */
    private val viewBinding by lazy { BottomFragmentSearchHistoryBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 検索履歴一覧表示
        viewModel.searchHistoryAllListLiveData.observe(viewLifecycleOwner) { list ->
            viewBinding.bottomFragmentSearchHistoryRecyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(requireContext())
                adapter = NicoVideoSearchHistoryAdapter(list) { history ->
                    // 検索
                    parentViewModel.search(
                        searchText = history.text,
                        isTagSearch = history.isTagSearch,
                        sortName = history.sort
                    )
                    dismiss()
                }
                if (itemDecorationCount == 0) {
                    addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
                }
            }
        }

    }

}