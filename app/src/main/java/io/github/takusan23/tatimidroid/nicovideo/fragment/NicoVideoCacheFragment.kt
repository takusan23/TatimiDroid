package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoCacheListScreen


/**
 * キャッシュ一覧を表示するFragment
 * */
class NicoVideoCacheFragment : Fragment() {

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                NicoVideoCacheListScreen(
                    viewModel = viewModel(),
                    onVideoClick = {  },
                    onMenuClick = { }
                )
            }
        }
    }

}