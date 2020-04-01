package io.github.takusan23.tatimidroid.DevNicoVideo

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_menu.*

/**
 * めにゅー
 * 3DSコメント非表示オプションなど
 * */
class DevNicoVideoMenuFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        getValue()
        setValue()
    }

    /**
     * 値セット
     * */
    fun getValue() {
        fragment_nicovideo_menu_3ds_switch.isChecked =
            prefSetting.getBoolean("nicovideo_comment_3ds_hidden", false)
    }

    /**
     * 値保存
     * */
    fun setValue() {
        switchListener(fragment_nicovideo_menu_3ds_switch, "nicovideo_comment_3ds_hidden")
    }

    private fun switchListener(switch: Switch, key: String) {
        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit { putBoolean(key, isChecked) }
        }
    }

}