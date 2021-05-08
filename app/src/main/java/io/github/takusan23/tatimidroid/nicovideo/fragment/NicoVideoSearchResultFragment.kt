package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import io.github.takusan23.tatimidroid.nicovideo.compose.SearchResultScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchResultViewModel

/**
 * 検索結果表示FragmentのViewModel
 *
 * UIをJetpackComposeで作る
 * */
class NicoVideoSearchResultFragment : Fragment() {

    /** ViewModel */
    private val viewModel by viewModels<NicoVideoSearchResultViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                // 検索結果
                SearchResultScreen(
                    searchResultViewModel = viewModel,
                    onBackScreen = {
                        parentFragmentManager.beginTransaction().remove(this@NicoVideoSearchResultFragment).commit()
                    }
                )
            }
        }
    }

}