package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_skip_customize.*

/**
 * 押したときのスキップ秒数を変更できるやつ
 * */
class DevNicoVideoSkipCustomizeBottomFragment : BottomSheetDialogFragment() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_skip_customize, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        bottom_fragment_skip_input.setText(prefSetting.getString("nicovideo_skip_ms", "5000"))

        bottom_fragment_skip_set.setOnClickListener {
            prefSetting.edit { putString("nicovideo_skip_ms", bottom_fragment_skip_input.text.toString()) }
        }

    }
}