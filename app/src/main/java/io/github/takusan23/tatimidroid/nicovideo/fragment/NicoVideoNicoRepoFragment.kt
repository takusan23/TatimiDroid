package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoRepoScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoRepoViewModelFactory

/**
 * ニコレポFragment
 *　
 * userId |String | ユーザーIDを入れるとそのユーザーのニコレポを取りに行きます。ない場合は自分のニコレポを取りに行きます
 * --- にんい ---
 * show_video   | Boolean   | 初期状態から動画のチェックを入れたい場合は使ってください
 * show_live    | Boolean   | 初期状態から生放送のチェックを入れたい場合は使ってください
 * */
class NicoVideoNicoRepoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val userId = arguments?.getString("userId")
                NicoRepoScreen(
                    viewModel = viewModel(factory = NicoRepoViewModelFactory(requireActivity().application, userId)),
                    onNicoRepoClick = { }
                )
            }
        }
    }

}