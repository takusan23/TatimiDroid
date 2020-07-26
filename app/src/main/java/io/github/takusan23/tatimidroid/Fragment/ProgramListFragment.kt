package io.github.takusan23.tatimidroid.Fragment

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.fragment_program_list.*

/**
 * 番組一覧Fragmentを乗せるFragment
 * */
class ProgramListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_program_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.nicolive)

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
                R.id.nicolive_program_list_menu_nicorepo -> setFragment(CommunityListFragment.NICOREPO)
                R.id.nicolive_program_list_menu_osusume -> setFragment(CommunityListFragment.RECOMMEND)
                R.id.nicolive_program_list_menu_ranking -> setFragment(CommunityListFragment.RANKING)
                R.id.nicolive_program_list_menu_top -> setFragment(CommunityListFragment.CHUMOKU)
                R.id.nicolive_program_list_menu_korekara -> setFragment(CommunityListFragment.KOREKARA)
                R.id.nicolive_program_list_menu_yoyaku -> setFragment(CommunityListFragment.YOYAKU)
                R.id.nicolive_program_list_menu_auto_admission -> setFragment(CommunityListFragment.ADMISSION)
                R.id.nicolive_program_list_menu_rookie -> setFragment(CommunityListFragment.ROOKIE)
                R.id.nicolive_program_list_menu_jk -> {
                    // ニコニコ実況。ｊｋ
                    val nicoJKChannelFragment = NicoJKChannelFragment()
                    parentFragmentManager.beginTransaction().replace(fragment_program_list_linearlayout.id, nicoJKChannelFragment).commit()
                    fragment_program_motionlayout?.transitionToStart()
                }
            }
            true
        }

    }

    /**
     * Fragment設置。
     * @param page [CommunityListFragment.FOLLOW] など
     * */
    private fun setFragment(page: Int) {
        // Fragmentが設置されてなければ落とす
        if (!isAdded) return
        Handler(Looper.getMainLooper()).post {
            val communityListFragment = CommunityListFragment()
            val bundle = Bundle()
            bundle.putInt("page", page)
            communityListFragment.arguments = bundle
            parentFragmentManager.beginTransaction().replace(fragment_program_list_linearlayout.id, communityListFragment).commit()
            fragment_program_motionlayout?.transitionToStart()
        }
    }

}