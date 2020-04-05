package io.github.takusan23.tatimidroid.Fragment

import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CpuUsageInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.Adapter.ProgramListViewPager
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_program_list.*
import java.lang.IllegalArgumentException

/**
 * 番組検索
 * */
class ProgramListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_program_list, container, false)
    }

    /**
     * 画面回転対策
     * */
    override fun onStart() {
        super.onStart()
        setFragment(CommunityListFragment.FOLLOW)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*
        val adapter = ProgramListViewPager(activity as AppCompatActivity)
        fragment_program_list_view_pager.adapter = adapter
        fragment_program_list_tab_layout.setupWithViewPager(fragment_program_list_view_pager)
*/

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.nicolive)

        // ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        // fragment_program_list_view_pager.backgroundTintList =
        //     ColorStateList.valueOf(darkModeSupport.getThemeColor())

        // 背景
        fragment_program_backdrop.background = ColorDrawable(darkModeSupport.getThemeColor())
        fragment_program_bar?.background = ColorDrawable(darkModeSupport.getThemeColor())
        fragment_program_list_linearlayout.background =
            ColorDrawable(darkModeSupport.getThemeColor())

        //setFragment(CommunityListFragment.FOLLOW)

        fragment_program_follow.setOnClickListener {
            setFragment(CommunityListFragment.FOLLOW)
        }
        fragment_program_nicorepo.setOnClickListener {
            setFragment(CommunityListFragment.NICOREPO)
        }
        fragment_program_osusume.setOnClickListener {
            setFragment(CommunityListFragment.RECOMMEND)
        }
        fragment_program_ranking.setOnClickListener {
            setFragment(CommunityListFragment.RANKING)
        }
        fragment_program_nicolive_top.setOnClickListener {
            setFragment(CommunityListFragment.CHUMOKU)
        }
        fragment_program_ninki_yoyaku.setOnClickListener {
            setFragment(CommunityListFragment.YOYAKU)
        }
        fragment_program_korekara.setOnClickListener {
            setFragment(CommunityListFragment.KOREKARA)
        }
        fragment_program_auto_admission.setOnClickListener {
            setFragment(CommunityListFragment.ADMISSION)
        }

    }

    /**
     * Fragment設置。
     * @param page CommunityListFragment#FOLLOW など
     * */
    fun setFragment(page: Int) {
        val communityListFragment = CommunityListFragment()
        val bundle = Bundle()
        bundle.putInt("page", page)
        communityListFragment.arguments = bundle
        fragmentManager?.beginTransaction()
            ?.replace(fragment_program_list_linearlayout.id, communityListFragment)?.commit()
        fragment_program_motionlayout?.transitionToStart()

    }

}