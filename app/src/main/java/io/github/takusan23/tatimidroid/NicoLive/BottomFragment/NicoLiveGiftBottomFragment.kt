package io.github.takusan23.tatimidroid.NicoLive.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoLive.JetpackCompose.NicoLiveGiftScreen
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveGiftViewModel
import io.github.takusan23.tatimidroid.NicoLive.ViewModel.NicoLiveGiftViewModelFactory
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.DarkColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.LightColors
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import java.util.concurrent.atomic.AtomicReference

/**
 * 投げ銭の履歴、ランキング表示BottomFragment
 *
 * いれてほしいもの
 * live_id  | String    | 番組ID
 * */
class NicoLiveGiftBottomFragment : BottomSheetDialogFragment() {

    /** 番組ID */
    private val liveId by lazy { requireArguments().getString("live_id")!! }

    /** ViewModel */
    private val viewModel by lazy {
        ViewModelProvider(this, NicoLiveGiftViewModelFactory(requireActivity().application, liveId)).get(NicoLiveGiftViewModel::class.java)
    }

    @InternalComposeUiApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            ViewTreeLifecycleOwner.set(this, viewLifecycleOwner)
            val newRecomposer = AtomicReference(WindowRecomposerFactory.LifecycleAware).get().createRecomposer(this)
            compositionContext = newRecomposer

            setContent {
                MaterialTheme(
                    colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors,
                ) {
                    NicoLiveGiftScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}