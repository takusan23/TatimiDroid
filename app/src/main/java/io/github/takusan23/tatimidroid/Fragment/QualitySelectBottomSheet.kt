package io.github.takusan23.tatimidroid.Fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_qulity_fragment_layout.*
import org.json.JSONArray

class QualitySelectBottomSheet : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_qulity_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val quality_list = arguments?.getString("quality_list")
            ?: "[\"abr\",\"high\",\"normal\",\"low\",\"super_low\",\"audio_high\"]"
        val select_quality = arguments?.getString("select_quality") ?: "high"

        //TextView回す
        val jsonArray = JSONArray(quality_list)
        for (i in 0 until jsonArray.length()) {
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            //押したときのやつ
            val typedValue = TypedValue()
            context?.theme?.resolveAttribute(
                android.R.attr.selectableItemBackground,
                typedValue,
                true
            )
            val textView = TextView(context)
            val text = jsonArray.getString(i)
            //いろいろ
            textView.layoutParams = layoutParams
            textView.text = getQualityText(text, context)
            textView.textSize = 24F
            textView.setPadding(10, 10, 10, 10)
            textView.setBackgroundResource(typedValue.resourceId)
            //今の画質
            if (text == select_quality) {
                textView.setTextColor(Color.parseColor("#0d46a0"))
            }
            //押したとき
            textView.setOnClickListener {
                if (activity is CommentActivity) {
                    //送信
                    (activity as CommentActivity).sendQualityMessage(text)
                    println(text)
                    dismiss()
                }
            }

            quality_parent_linearlayout.addView(textView)
        }
    }

    //シングルトンとは
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
                    return context?.getString(R.string.quality_audio) + "(利用できない模様)"
                }
            }
            return ""
        }
    }


}