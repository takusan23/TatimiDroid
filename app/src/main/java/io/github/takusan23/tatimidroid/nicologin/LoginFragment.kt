package io.github.takusan23.tatimidroid.nicologin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.nicologin.compose.NicoLoginScreen
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.isDarkMode

/** ログイン画面Fragment */
class LoginFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    Surface {
                        // ViewModel
                        NicoLoginScreen(viewModel = viewModel()) { nicoLoginDataClass ->
                            // 二段階認証画面へ飛ばす
                            val twoFactorAuthLoginActivity = Intent(context, TwoFactorAuthLoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                putExtra("login", nicoLoginDataClass)
                            }
                            context?.startActivity(twoFactorAuthLoginActivity)
                        }
                    }
                }
            }
        }
    }

}