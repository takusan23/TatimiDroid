package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.DarkColors
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.LightColors
import io.github.takusan23.tatimidroid.Tool.isDarkMode

/**
 * 番組詳細Fragment
 * */
class JCNicoLiveInfoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(
                    // ダークモード。動的にテーマ変更できるようになるんか？
                    colors = if (isDarkMode(AmbientContext.current)) DarkColors else LightColors,
                ) {

                    val commentText = remember { mutableStateOf("") }

                    Surface {
                        Scaffold {

                        }
                    }
                }
            }
        }
    }
}