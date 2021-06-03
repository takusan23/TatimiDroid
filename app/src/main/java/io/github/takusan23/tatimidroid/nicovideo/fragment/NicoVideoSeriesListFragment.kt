package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoSeriesParentScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoSeriesListViewModelFactory

/**
 * シリーズ一覧表示Fragment
 *
 * いれるもの
 * userId   |String | ユーザーID。なければ自分のを取ってくる
 * */
class NicoVideoSeriesListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val userId = arguments?.getString("userId")

                NicoVideoSeriesParentScreen(
                    viewModel = viewModel(factory = NicoVideoSeriesListViewModelFactory(requireActivity().application, userId)),
                    onSeriesClick = { nicoVideoSeriesData ->
                        // シリーズ動画一覧へ遷移
                        val nicoVideoSeriesFragment = NicoVideoSeriesFragment().apply {
                            arguments = Bundle().apply {
                                putString("series_id", nicoVideoSeriesData.seriesId)
                            }
                        }
                        parentFragmentManager.beginTransaction().replace(this@NicoVideoSeriesListFragment.id, nicoVideoSeriesFragment).addToBackStack("series_video").commit()
                    }
                )
            }
        }
    }

}