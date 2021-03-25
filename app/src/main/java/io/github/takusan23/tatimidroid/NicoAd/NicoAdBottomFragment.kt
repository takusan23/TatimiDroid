package io.github.takusan23.tatimidroid.NicoAd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoAd.JetpackCompose.NicoAdScreen
import io.github.takusan23.tatimidroid.NicoAd.ViewModel.NicoAdViewModel
import io.github.takusan23.tatimidroid.NicoAd.ViewModel.NicoAdViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.DarkColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.LightColors
import io.github.takusan23.tatimidroid.Tool.isDarkMode

/**
 * ニコニ広告の履歴とか貢献度を表示するBottomFragment
 *
 * 入れてほしいもの
 * content_id   | String | 動画か生放送ID
 *
 * */
class NicoAdBottomFragment : BottomSheetDialogFragment() {

    /** 動画、生放送ID */
    private val contentId by lazy { requireArguments().getString("content_id")!! }

    /** ViewModel。APIを叩くコードなどはこっち */
    private val viewModel by lazy {
        ViewModelProvider(this, NicoAdViewModelFactory(requireActivity().application, contentId)).get(NicoAdViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    NicoAdScreen(viewModel)
                }
            }
        }
    }

}