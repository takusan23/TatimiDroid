package io.github.takusan23.tatimidroid.nicolive.bottomfragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicolive.compose.NicoLiveProgramListMenu
import io.github.takusan23.tatimidroid.nicolive.viewmodel.factory.NicoLiveProgramListMenuViewModelFactory
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * 番組の
 * TS予約、予約枠自動入場
 * 入れてほしいもの↓
 * liveId   | String | 番組ID
 * */
class ProgramMenuBottomSheet : BottomSheetDialogFragment() {

    private val liveId by lazy { arguments?.getString("liveId", "")!! }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    Surface {
                        NicoLiveProgramListMenu(viewModel = viewModel(factory = NicoLiveProgramListMenuViewModelFactory(requireActivity().application, liveId)))
                    }
                }
            }
        }
    }

}