package io.github.takusan23.tatimidroid.nicovideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.nicovideo.bottomfragment.NicoVideoListMenuBottomFragment
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoListScreen
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * ランキング、マイリスト等を表示するFragmentを乗せるFragment。
 * BottonNavBar押した時に切り替わるFragmentはこれ
 * */
class NicoVideoSelectFragment : Fragment() {

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    Surface {
                        NicoVideoListScreen(
                            application = requireActivity().application,
                            onVideoClick = { nicoVideoData ->
                                (requireActivity() as MainActivity).setNicovideoFragment(nicoVideoData.videoId, nicoVideoData.isCache)
                            },
                            onMenuClick = { nicoVideoData ->
                                val menuBottomSheet = NicoVideoListMenuBottomFragment()
                                // データ渡す
                                val bundle = Bundle()
                                bundle.putString("video_id", nicoVideoData.videoId)
                                bundle.putBoolean("is_cache", nicoVideoData.isCache)
                                bundle.putSerializable("data", nicoVideoData)
                                // bundle.putSerializable("video_list", nicoVideoDataList)
                                menuBottomSheet.arguments = bundle
                                (requireActivity() as MainActivity).currentFragment()?.apply {
                                    menuBottomSheet.show(this.childFragmentManager, "menu")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}