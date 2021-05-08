package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.takusan23.tatimidroid.nicovideo.NicoVideoSelectFragment
import io.github.takusan23.tatimidroid.nicovideo.compose.SearchScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchViewModel_

/**
 * ニコ動検索Fragment。
 *
 * JetpackComposeで作り直された。
 * */
class NicoVideoSearchFragment_ : Fragment() {

    /** ViewModel */
    private val viewModel by viewModels<NicoVideoSearchViewModel_>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // 検索UI
                SearchScreen(
                    searchViewModel = viewModel,
                    onSearch = { searchText, isTagSearch, sort ->
                        // Fragment遷移
                        (requireParentFragment() as NicoVideoSelectFragment).setFragment(NicoVideoSearchResultFragment(), System.currentTimeMillis().toString())
                    }
                )
            }
        }
    }

}