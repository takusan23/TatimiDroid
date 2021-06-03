package io.github.takusan23.tatimidroid.nicovideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoUserProfileScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoAccountViewModel
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoAccountViewModelFactory

/**
 * アカウント情報Fragment
 *
 * 投稿動画とか公開マイリストとか。データ取得は[NicoAccountViewModel]に書いてあります
 *
 * 入れてほしいもの
 *
 * userId   | String    | ユーザーID。ない場合(nullのとき)は自分のアカウントの情報を取りに行きます
 * */
class NicoAccountFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val userId = arguments?.getString("userId")

                NicoUserProfileScreen(viewModel = viewModel(factory = NicoAccountViewModelFactory(requireActivity().application, userId)))
            }
        }
    }

    /**
     * Fragmentを置く。第２引数はバックキーで戻るよう
     * */
    private fun setFragment(fragment: Fragment, backstack: String) {
        parentFragmentManager.beginTransaction().replace(id, fragment).addToBackStack(backstack).commit()
    }

}