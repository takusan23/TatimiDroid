package io.github.takusan23.tatimidroid.Fragment

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
import kotlinx.android.synthetic.main.fragment_program_list.*

/**
 * 番組検索
 * */
class ProgramListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_program_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.nicolive)

        // ダークモード
        val darkModeSupport = DarkModeSupport(context!!)

        // 背景
        fragment_program_backdrop.background = ColorDrawable(darkModeSupport.getThemeColor())
        fragment_program_bar?.background = ColorDrawable(darkModeSupport.getThemeColor())
        fragment_program_list_linearlayout.background = ColorDrawable(darkModeSupport.getThemeColor())

        if (savedInstanceState == null) {
            setFragment(CommunityListFragment.FOLLOW)
        }

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
        fragment_program_jk.setOnClickListener {
            // ニコニコ実況。ｊｋ
            val nicoJKChannelFragment = NicoJKChannelFragment()
            fragmentManager?.beginTransaction()
                ?.replace(fragment_program_list_linearlayout.id, nicoJKChannelFragment)
                ?.commit()
            fragment_program_motionlayout?.transitionToStart()
        }

    }

    /**
     * Fragment設置。
     * @param page CommunityListFragment#FOLLOW など
     * */
    fun setFragment(page: Int) {
        Handler(Looper.getMainLooper()).post {
            val communityListFragment = CommunityListFragment()
            val bundle = Bundle()
            bundle.putInt("page", page)
            communityListFragment.arguments = bundle
            fragmentManager?.beginTransaction()
                ?.replace(fragment_program_list_linearlayout.id, communityListFragment)?.commit()
            fragment_program_motionlayout?.transitionToStart()
        }
    }

}