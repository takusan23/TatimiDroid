package io.github.takusan23.tatimidroid.NGUploader.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.WindowRecomposerFactory
import androidx.compose.ui.platform.compositionContext
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewTreeLifecycleOwner
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NGUploader.JetpackCompose.NGUploaderScreen
import io.github.takusan23.tatimidroid.NGUploader.ViewModel.NGUploaderViewModel
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.DarkColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.LightColors
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import java.util.concurrent.atomic.AtomicReference

/**
 * NG投稿者機能の編集用BottomFragment
 * */
class NGUploaderBottomFragment : BottomSheetDialogFragment() {

    /** データベースアクセスなどはViewModelに */
    private val viewModel by viewModels<NGUploaderViewModel>({ this })

    @InternalComposeUiApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            ViewTreeLifecycleOwner.set(this, viewLifecycleOwner)
            val newRecomposer = AtomicReference(WindowRecomposerFactory.LifecycleAware).get().createRecomposer(rootView)
            compositionContext = newRecomposer
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    NGUploaderScreen(viewModel = viewModel)
                }
            }
        }
    }
}