package io.github.takusan23.tatimidroid.nicolive.bottomfragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.BottomFragmentNicovideoQualityBinding

/**
 * ニコ生画質変更BottomFragment。
 * Bundleには何も入れなくていいから、[NicoLiveViewModel.qualityListJSONArray]と[NicoLiveViewModel.currentQuality]の値を入れておいてね
 * */
class NicoLiveQualitySelectBottomSheet : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoQualityBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // CommentFragmentのViewModel取得する
        val commentFragmentViewModel by viewModels<NicoLiveViewModel>({ requireParentFragment() })

        //TextView回す
        val jsonArray = commentFragmentViewModel.qualityListJSONArray
        for (i in 0 until jsonArray.length()) {
            val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            //押したときのやつ
            val typedValue = TypedValue()
            context?.theme?.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
            val textView = TextView(context)
            val text = jsonArray.getString(i)
            //いろいろ
            textView.layoutParams = layoutParams
            textView.text = getQualityText(text, context)
            textView.textSize = 24F
            textView.setPadding(10, 10, 10, 10)
            textView.setBackgroundResource(typedValue.resourceId)
            //今の画質
            if (text == commentFragmentViewModel.currentQuality) {
                textView.setTextColor(Color.parseColor("#0d46a0"))
            }
            //押したとき
            textView.setOnClickListener {
                //送信
                commentFragmentViewModel.nicoLiveHTML.sendQualityMessage(text)
                dismiss()
            }
            viewBinding.bottomFragmentNicovideoQualityLinearLayout.addView(textView)
        }
    }

    companion object {
        fun getQualityText(text: String, context: Context?): String {
            when (text) {
                "abr" -> {
                    return context?.getString(R.string.quality_auto) ?: ""
                }
                "super_high" -> {
                    return "3Mbps"
                }
                "high" -> {
                    return "2Mbps"
                }
                "normal" -> {
                    return "1Mbps"
                }
                "low" -> {
                    return "384kbps"
                }
                "super_low" -> {
                    return "192kbps"
                }
                "audio_high" -> {
                    return context?.getString(R.string.quality_audio) ?: "音声のみ"
                }
            }
            return ""
        }
    }


}