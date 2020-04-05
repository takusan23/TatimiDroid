package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_dev_setting.*

class DevSettingFragment : Fragment() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dev_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context!!)

        // setSetting(fragment_dev_niconico_video, "fragment_dev_niconico_video")
        setSetting(fragment_dev_intent_mime, "fragment_dev_intent_mime")

    }

    /**
     * 設定Switch
     * */
    fun setSetting(switch: Switch, key: String) {
        switch.isChecked = prefSetting.getBoolean(key, false)
        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            prefSetting.edit {
                putBoolean(key, isChecked)
            }
        }
    }

}