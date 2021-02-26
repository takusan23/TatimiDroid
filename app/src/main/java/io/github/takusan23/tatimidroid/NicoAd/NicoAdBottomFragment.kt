package io.github.takusan23.tatimidroid.NicoAd

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.platform.compositionContext
import androidx.lifecycle.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoAd.JetpackCompose.NicoAdScreen
import io.github.takusan23.tatimidroid.NicoAd.ViewModel.NicoAdViewModel
import io.github.takusan23.tatimidroid.NicoAd.ViewModel.NicoAdViewModelFactory
import io.github.takusan23.tatimidroid.R
import java.util.concurrent.atomic.AtomicReference

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

    @InternalComposeUiApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            ViewTreeLifecycleOwner.set(this, viewLifecycleOwner)
            val newRecomposer = AtomicReference(WindowRecomposerFactory.LifecycleAware).get().createRecomposer(this)
            compositionContext = newRecomposer

            setContent {
                NicoAdScreen(viewModel)
            }
        }
    }

}