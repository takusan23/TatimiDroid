package io.github.takusan23.tatimidroid.NicoLive

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.FragmentProgramListBinding

/**
 * 番組一覧Fragmentを乗せるFragment
 *
 * BundleにIntを"fragment"の名前で[R.id.nicolive_program_list_menu_nicolive_jk]等を入れておくことで指定したページを開くことができます。
 * */
class ProgramListFragment : Fragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentProgramListBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 背景色
        viewBinding.apply {
            fragmentProgramBackdropLinearLayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
            fragmentProgramBarLinearLayout?.background?.setTint(getThemeColor(context))
            fragmentProgramListLinearLayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
            fragmentProgramNavigationView.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
        }

        if (savedInstanceState == null) {
            setFragment(CommunityListFragment.FOLLOW)
        }

        // メニュー押したとき
        viewBinding.fragmentProgramNavigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nicolive_program_list_menu_follow -> setFragment(CommunityListFragment.FOLLOW)
                R.id.nicolive_program_list_menu_osusume -> setFragment(CommunityListFragment.RECOMMEND)
                R.id.nicolive_program_list_menu_ranking -> setFragment(CommunityListFragment.RANKING)
                R.id.nicolive_program_list_menu_top -> setFragment(CommunityListFragment.CHUMOKU)
                R.id.nicolive_program_list_menu_korekara -> setFragment(CommunityListFragment.KOREKARA)
                R.id.nicolive_program_list_menu_yoyaku -> setFragment(CommunityListFragment.YOYAKU)
                R.id.nicolive_program_list_menu_rookie -> setFragment(CommunityListFragment.ROOKIE)
                R.id.nicolive_program_list_menu_nicorepo -> setFragment(CommunityListFragment.NICOREPO)
                R.id.nicolive_program_list_menu_nicolive_jk -> {
                    parentFragmentManager
                        .beginTransaction()
                        .replace(viewBinding.fragmentProgramListLinearLayout.id, NicoLiveJKProgramFragment())
                        .commit()
                    (viewBinding.fragmentProgramListParent as? MotionLayout)?.transitionToStart()
                }
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
     * Fragment設置。
     * @param page [CommunityListFragment.FOLLOW] など
     * */
    private fun setFragment(page: Int) {
        // Fragmentが設置されてなければ落とす
        if (!isAdded) return
        activity?.runOnUiThread {
            val communityListFragment = CommunityListFragment()
            val bundle = Bundle()
            bundle.putInt("page", page)
            communityListFragment.arguments = bundle
            parentFragmentManager.beginTransaction().replace(viewBinding.fragmentProgramListLinearLayout.id, communityListFragment).commit()
            // 縦画面時、親はMotionLayoutになるんだけど、横画面時はLinearLayoutなのでキャストが必要
            (viewBinding.fragmentProgramListParent as? MotionLayout)?.transitionToStart()
        }
    }

}