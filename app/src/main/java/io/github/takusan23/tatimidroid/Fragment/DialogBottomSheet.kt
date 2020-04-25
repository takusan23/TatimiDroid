package io.github.takusan23.tatimidroid.Fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_dialog.*
import kotlinx.android.synthetic.main.textview_ripple.view.*

/**
 * BottomSheet版ダイアログを自作してみた。
 * @param description ダイアログの説明
 * @param buttonItems DialogBottomSheetItemの配列。
 * @param clickEvent クリック押したときのコールバック。引数は押した位置です。
 * */
class DialogBottomSheet(val description: String, val buttonItems: ArrayList<DialogBottomSheetItem>, val clickEvent: (Int) -> Unit) :
    BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 説明文
        bottom_fragment_dialog_description.text = description
        // ボタン作成
        for (i in 0 until buttonItems.size) {
            val item = buttonItems[i]
            // 押したときのRippleつけたいがためにinflateしてる
            val layout = layoutInflater.inflate(R.layout.textview_ripple, null)
            val textView = (layout as TextView).apply {
                text = item.title
                if (item.icon != -1) {
                    setCompoundDrawablesWithIntrinsicBounds(context?.getDrawable(item.icon), null, null, null)
                }
                if (item.textColor != -1) {
                    setTextColor(item.textColor)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        compoundDrawableTintList = ColorStateList.valueOf(item.textColor)
                    }
                }
                setOnClickListener {
                    // 高階関数
                    clickEvent(i)
                    // 閉じる
                    dismiss()
                }
            }
            bottom_fragment_dialog_linearlayout.addView(textView)
        }
    }

    // ボタンのテキスト、アイコンなど。IconとtextColorは無指定では-1（設定しない）になります。
    data class DialogBottomSheetItem(val title: String, val icon: Int = -1, val textColor: Int = -1)

}