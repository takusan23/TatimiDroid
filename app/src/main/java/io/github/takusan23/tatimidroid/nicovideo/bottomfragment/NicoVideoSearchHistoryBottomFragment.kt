package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicovideo.compose.SearchHistoryScreen
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SearchHistoryScreen(
                    viewModel = viewModel,
                    onSearch = { searchText, isTag, sort ->
                        // 検索実行時
                        parentViewModel.search(searchText = searchText, isTagSearch = isTag, sortName = sort)
                        dismiss()
                    }
                )
            }
        }
    }

}