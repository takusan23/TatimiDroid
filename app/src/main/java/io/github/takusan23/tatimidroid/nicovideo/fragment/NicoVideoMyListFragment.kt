package io.github.takusan23.tatimidroid.nicovideo.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoMylistParentScreen
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.factory.NicoVideoMyListViewModelFactory

/**
 * マイリスト一覧Fragment。
 *
 * 動画一覧ではない。
 * */
class NicoVideoMyListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                val userId = arguments?.getString("userId")
                NicoVideoMylistParentScreen(
                    viewModel = viewModel(factory = NicoVideoMyListViewModelFactory(requireActivity().application, userId)),
                    onClickMylist = { myListItem ->
                        // Fragment遷移
                        val myListFragment = NicoVideoMyListListFragment().apply {
                            arguments = Bundle().apply {
                                putString("mylist_id", myListItem.id)
                                putBoolean("mylist_is_me", myListItem.isMe)
                            }
                        }
                        parentFragmentManager.beginTransaction().replace(this@NicoVideoMyListFragment.id, myListFragment).addToBackStack("mylist_video").commit()
                    }
                )
            }
        }
    }

}