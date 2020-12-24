package io.github.takusan23.tatimidroid.NicoLive

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.fragment_program_list.*

/**
 * 番組一覧Fragmentを乗せるFragment
 *
 * BundleにIntを"fragment"の名前で[R.id.nicolive_program_list_menu_nicolive_jk]等を入れておくことで指定したページを開くことができます。
 * */
class ProgramListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_program_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 背景色
        fragment_program_backdrop.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
        fragment_program_bar?.background = ColorDrawable(getThemeColor(context))
        fragment_program_list_linearlayout.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))
        fragment_program_menu.backgroundTintList = ColorStateList.valueOf(getThemeColor(context))

        if (savedInstanceState == null) {
            setFragment(CommunityListFragment.FOLLOW)
        }

        // メニュー押したとき
        fragment_program_menu.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nicolive_program_list_menu_follow -> setFragment(CommunityListFragment.FOLLOW)
                R.id.nicolive_program_list_menu_osusume -> setFragment(CommunityListFragment.RECOMMEND)
                R.id.nicolive_program_list_menu_ranking -> setFragment(CommunityListFragment.RANKING)
                R.id.nicolive_program_list_menu_top -> setFragment(CommunityListFragment.CHUMOKU)
                R.id.nicolive_program_list_menu_korekara -> setFragment(CommunityListFragment.KOREKARA)
                R.id.nicolive_program_list_menu_yoyaku -> setFragment(CommunityListFragment.YOYAKU)
                R.id.nicolive_program_list_menu_auto_admission -> setFragment(CommunityListFragment.ADMISSION)
                R.id.nicolive_program_list_menu_rookie -> setFragment(CommunityListFragment.ROOKIE)
                R.id.nicolive_program_list_menu_nicolive_jk -> setFragment(CommunityListFragment.NICOLIVE_JK)
                R.id.nicolive_program_list_menu_nicorepo -> setFragment(CommunityListFragment.NICOREPO)
            }
            true
        }

        // ページを指定して開く
        val menuId = arguments?.getInt("fragment")
        if (menuId != null) {
            fragment_program_menu.setCheckedItem(menuId)
            fragment_program_menu.menu.performIdentifierAction(menuId, 0)
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
            parentFragmentManager.beginTransaction().replace(fragment_program_list_linearlayout.id, communityListFragment).commit()
            fragment_program_motionlayout?.transitionToStart()
        }
    }

}