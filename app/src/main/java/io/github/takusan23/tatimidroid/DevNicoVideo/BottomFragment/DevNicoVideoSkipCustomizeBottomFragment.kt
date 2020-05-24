package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_skip_customize.*
import kotlinx.android.synthetic.main.fragment_nicovideo.*
import kotlinx.android.synthetic.main.fragment_nicovideo.view.*

/**
 * 押したときのスキップ秒数を変更できるやつ
 * | video_id    |String | 動画ID。Fragmentを探すために使います。
 * */
class DevNicoVideoSkipCustomizeBottomFragment : BottomSheetDialogFragment() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_skip_customize, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        bottom_fragment_skip_input.setText(prefSetting.getString("nicovideo_skip_sec", "5"))
        bottom_fragment_skip_input_long.setText(prefSetting.getString("nicovideo_long_skip_sec", "10"))

        // 保存
        bottom_fragment_skip_input.addTextChangedListener {
            if (it?.isNotEmpty() == true) {
                // 空文字だと toLong() で落ちるので対策（toLongOrNull()使えば変換できない時にnullにしてくれる）
                prefSetting.edit { putString("nicovideo_skip_sec", bottom_fragment_skip_input.text.toString()) }
                applyUI()
            }
        }

        // 長押し時
        bottom_fragment_skip_input_long.addTextChangedListener {
            if (it?.isNotEmpty() == true) {
                // 空文字だと toLong() で落ちるので対策（toLongOrNull()使えば変換できない時にnullにしてくれる）
                prefSetting.edit { putString("nicovideo_skip_long_sec", bottom_fragment_skip_input_long.text.toString()) }
                applyUI()
            }
        }

    }

    // ボタンのスキップ秒数設定反映
    private fun applyUI() {
        val skipTime = prefSetting.getString("nicovideo_skip_sec", "5")
        val longSkipTime = prefSetting.getString("nicovideo_long_skip_sec", "10")
        val liveId = arguments?.getString("video_id")
        (parentFragmentManager.findFragmentByTag(liveId) as DevNicoVideoFragment).apply {
            fragment_nicovideo_controller_replay.text = "${skipTime} | ${longSkipTime}"
            fragment_nicovideo_controller_forward.text = "${skipTime} | ${longSkipTime}"
        }
    }

}