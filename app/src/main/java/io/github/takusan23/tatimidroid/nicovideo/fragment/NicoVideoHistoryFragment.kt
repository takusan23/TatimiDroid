package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoHistoryScreen

/** ニコ動履歴Fragment */
class NicoVideoHistoryFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                NicoVideoHistoryScreen(
                    viewModel = viewModel(),
                    onVideoClick = { (requireActivity() as? MainActivity)?.setNicovideoFragment(it.videoId, it.isCache) },
                    onMenuClick = { }
                )
            }
        }
    }

}