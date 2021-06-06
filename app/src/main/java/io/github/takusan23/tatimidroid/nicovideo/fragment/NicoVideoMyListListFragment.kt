package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoMylistVideoListScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoMyListListViewModelFactory

/**
 * マイリストの動画一覧表示Fragment。
 * ViewPagerで表示するFragmentです。
 * 入れてほしいもの↓
 * mylist_id   |String |マイリストのID。空の場合はとりあえずマイリストをリクエストします
 * mylist_is_me|Boolean|マイリストが自分のものかどうか。自分のマイリストの場合はtrue
 * */
class NicoVideoMyListListFragment : Fragment() {

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val myListId = arguments?.getString("mylist_id")!!
                val isMe = arguments?.getBoolean("mylist_is_me")!!
                NicoVideoMylistVideoListScreen(
                    viewModel = viewModel(factory = NicoVideoMyListListViewModelFactory(requireActivity().application, myListId, isMe)),
                    onClickVideo = { (requireActivity() as? MainActivity)?.setNicovideoFragment(it.videoId, it.isCache) },
                    onClickMenu = { }
                )
            }
        }
    }

}