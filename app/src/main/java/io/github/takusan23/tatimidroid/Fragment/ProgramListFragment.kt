package io.github.takusan23.tatimidroid.Fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.Adapter.ProgramListViewPager
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_program_list.*

/**
 * 番組検索にViewPager乗せるためのFragment
 * */
class ProgramListFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_program_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = ProgramListViewPager(activity as AppCompatActivity)
        fragment_program_list_view_pager.adapter = adapter
        fragment_program_list_tab_layout.setupWithViewPager(fragment_program_list_view_pager)

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.follow_program)

        // ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        fragment_program_list_view_pager.backgroundTintList =
            ColorStateList.valueOf(darkModeSupport.getThemeColor())
    }

}