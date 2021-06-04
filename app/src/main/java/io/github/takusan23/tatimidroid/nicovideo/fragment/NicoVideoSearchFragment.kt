package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nicovideo.compose.screen.NicoVideoSearchScreen

/**
 * ニコ動検索Fragment
 * argumentにputString("search","検索したい内容")を入れるとその値を検索します。なおタグ検索、人気の高い順です。
 *
 * search       | String | 検索したい内容
 * search_hide  | Boolean| 検索領域を非表示にする場合はtrue
 * sort_show    | Boolean| 並び替えを初めから表示する場合はtrue。なおタグ/キーワードの変更は出ない
 * */
class NicoVideoSearchFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NicoVideoSearchScreen(
                    viewModel = viewModel(),
                    onMenuClick = {},
                    onVideoClick = { (requireActivity() as? MainActivity)?.setNicovideoFragment(it.videoId, it.isCache, false, true) },
                )
            }
        }
    }

}