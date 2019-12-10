package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_menu.*
import java.lang.Appendable

class NicoVideoMenuFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = arguments?.getString("id")

        //nicovideoFragment取る。
        val nicoVideoFragment =
            (activity as AppCompatActivity).supportFragmentManager.findFragmentByTag(id) as NicoVideoFragment

        //3DSのコメントを非表示にする
        nicoVideoFragment.apply {
            fragment_nicovideo_menu_3ds_switch.isChecked = isHide3DSComment
            fragment_nicovideo_menu_3ds_switch.setOnCheckedChangeListener { buttonView, isChecked ->
                isHide3DSComment = isChecked
            }
        }


    }


}