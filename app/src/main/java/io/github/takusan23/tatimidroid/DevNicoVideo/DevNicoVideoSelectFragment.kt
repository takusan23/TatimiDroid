package io.github.takusan23.tatimidroid.DevNicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.DevNicoVideo.VideoList.DevNicoVideoRankingFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_dev_nicovideo_select.*

class DevNicoVideoSelectFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_nicovideo_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragment_nicovideo_ranking_button.setOnClickListener {
            setFragment(DevNicoVideoRankingFragment())
        }

    }

    fun setFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(fragment_video_list_linearlayout.id, fragment)?.commit()
    }

}