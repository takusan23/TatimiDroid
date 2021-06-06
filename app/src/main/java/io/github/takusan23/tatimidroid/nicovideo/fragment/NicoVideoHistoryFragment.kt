package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoHistoryScreen
import io.github.takusan23.tatimidroid.tool.isDarkMode

/** ニコ動履歴Fragment */
class NicoVideoHistoryFragment : Fragment() {

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    Surface {
                        NicoVideoHistoryScreen(
                            viewModel = viewModel(),
                            onVideoClick = { (requireActivity() as? MainActivity)?.setNicovideoFragment(it.videoId, it.isCache) },
                            onMenuClick = { }
                        )
                    }
                }
            }
        }
    }

}