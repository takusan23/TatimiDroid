package io.github.takusan23.tatimidroid.nicolive

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
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentProgramListBinding
import io.github.takusan23.tatimidroid.nicolive.bottomfragment.ProgramMenuBottomSheet
import io.github.takusan23.tatimidroid.nicolive.compose.NicoLiveProgramListScreen
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * 番組一覧Fragmentを乗せるFragment
 *
 * BundleにIntを"fragment"の名前で[R.id.nicolive_program_list_menu_nicolive_jk]等を入れておくことで指定したページを開くことができます。
 * */
class ProgramListFragment : Fragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentProgramListBinding.inflate(layoutInflater) }

    @ExperimentalMaterialApi
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                    Surface {
                        NicoLiveProgramListScreen(
                            onProgramClick = { nicoLiveProgramData ->
                                (requireActivity() as MainActivity).setNicoliveFragment(nicoLiveProgramData.programId, nicoLiveProgramData.isOfficial, false)
                            },
                            onMenuClick = { nicoLiveProgramData ->
                                // とりあえず
                                ProgramMenuBottomSheet().apply {
                                    arguments = Bundle().apply { putString("liveId", nicoLiveProgramData.programId) }
                                }.show(childFragmentManager, "menu")
                            }
                        )
                    }
                }
            }
        }
    }


}