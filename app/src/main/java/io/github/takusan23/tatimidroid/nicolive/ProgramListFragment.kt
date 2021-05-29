package io.github.takusan23.tatimidroid.nicolive

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentProgramListBinding
import io.github.takusan23.tatimidroid.nicolive.compose.NicoLiveProgramListScreen
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.getThemeColor
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
                        NicoLiveProgramListScreen { nicoLiveProgramData ->
                            (requireActivity() as MainActivity).setNicoliveFragment(nicoLiveProgramData.programId, nicoLiveProgramData.isOfficial, false)
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        return

        // 背景色
        viewBinding.apply {
            fragmentProgramBackdropLinearLayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
            fragmentProgramBarLinearLayout?.background?.setTint(getThemeColor(context))
            fragmentProgramListLinearLayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
            fragmentProgramNavigationView.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
        }

        if (savedInstanceState == null) {
            setCommunityListFragment(CommunityListFragment.FOLLOW)
        }

        // メニュー押したとき
        viewBinding.fragmentProgramNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nicolive_program_list_menu_follow -> setCommunityListFragment(CommunityListFragment.FOLLOW)
                R.id.nicolive_program_list_menu_osusume -> setCommunityListFragment(CommunityListFragment.RECOMMEND)
                R.id.nicolive_program_list_menu_ranking -> setCommunityListFragment(CommunityListFragment.RANKING)
                R.id.nicolive_program_list_menu_top -> setCommunityListFragment(CommunityListFragment.CHUMOKU)
                R.id.nicolive_program_list_menu_korekara -> setCommunityListFragment(CommunityListFragment.KOREKARA)
                R.id.nicolive_program_list_menu_yoyaku -> setCommunityListFragment(CommunityListFragment.YOYAKU)
                R.id.nicolive_program_list_menu_rookie -> setCommunityListFragment(CommunityListFragment.ROOKIE)
                R.id.nicolive_program_list_menu_nicorepo -> setCommunityListFragment(CommunityListFragment.NICOREPO)
                R.id.nicolive_program_list_menu_nicolive_jk -> setFragment(NicoLiveJKProgramFragment())
                R.id.nicolive_program_list_menu_konomi_tag -> setFragment(NicoLiveKonomiTagProgramListFragment())
            }
            true
        }

        // ページを指定して開く
        val menuId = arguments?.getInt("fragment")
        if (menuId != null) {
            viewBinding.fragmentProgramNavigationView.setCheckedItem(menuId)
            viewBinding.fragmentProgramNavigationView.menu.performIdentifierAction(menuId, 0)
        }
    }

    /**
     * Fragmentを置く関数
     * */
    private fun setFragment(fragment: Fragment) {
        if (isAdded) {
            childFragmentManager.beginTransaction().replace(R.id.fragment_program_list_linear_layout, fragment).commit()
            // 縦画面時、親はMotionLayoutになるんだけど、横画面時はLinearLayoutなのでキャストが必要
            (viewBinding.fragmentProgramListParent as? MotionLayout)?.transitionToStart()
        }
    }

    /**
     * [CommunityListFragment]を置く関数
     *
     * @param page [CommunityListFragment.FOLLOW] など
     * */
    private fun setCommunityListFragment(page: Int) {
        // Fragmentが設置されてなければ落とす
        if (isAdded) {
            val communityListFragment = CommunityListFragment()
            val bundle = Bundle()
            bundle.putInt("page", page)
            communityListFragment.arguments = bundle
            setFragment(communityListFragment)
        }
    }

}