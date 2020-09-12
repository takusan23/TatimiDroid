package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.widget.addTextChangedListener
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_skip_customize.*

/**
 * 押したときのスキップ秒数を変更できるやつ
 * | video_id    |String | 動画ID。Fragmentを探すために使います。
 * */
class NicoVideoSkipCustomizeBottomFragment : BottomSheetDialogFragment() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_skip_customize, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        bottom_fragment_skip_input.setText(prefSetting.getString("nicovideo_skip_sec", "5"))

        // 保存
        bottom_fragment_skip_input.addTextChangedListener {
            if (it?.isNotEmpty() == true) {
                // 空文字だと toLong() で落ちるので対策（toLongOrNull()使えば変換できない時にnullにしてくれる）
                prefSetting.edit { putString("nicovideo_skip_sec", bottom_fragment_skip_input.text.toString()) }
                applyUI()
            }
        }

    }

    // ボタンのスキップ秒数設定反映
    private fun applyUI() {
        (requireParentFragment() as? NicoVideoFragment)?.apply {
            initController()
        }
    }

}