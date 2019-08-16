package io.github.takusan23.tatimidroid.Fragment

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_sheet_fragment_post_layout.*
import kotlinx.android.synthetic.main.dialog_watchmode_layout.*

class CommentPOSTBottomFragment : BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_sheet_fragment_post_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        bottom_fragment_post_linearlayout.background = ColorDrawable(darkModeSupport.getThemeColor())

        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        val nicocasmode = pref_setting.getBoolean("setting_nicocas_mode", false)

        //nicocas警告
        if (nicocasmode) {
            val textView = TextView(context)
            textView.text = getString(R.string.nicocas_alert)
            textView.setTextColor(Color.RED)
            bottom_fragment_post_linearlayout.addView(textView, 1)
        }
        //投稿
        bottom_fragment_post_button.setOnClickListener {
            if (activity is CommentActivity) {
                //コメント投稿
                (activity as CommentActivity).sendComment(bottom_fragment_post_edittext.text.toString())
                //閉じる
                this@CommentPOSTBottomFragment.dismiss()
            }
        }

        //IMEを表示させる
        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(bottom_fragment_post_edittext, 0)

        //Enderキーを押したら投稿する
        bottom_fragment_post_edittext.setOnEditorActionListener { textView, i, keyEvent ->
            if (i == KeyEvent.ACTION_DOWN) {
                if (activity is CommentActivity) {
                    //コメント投稿
                    (activity as CommentActivity).sendComment(bottom_fragment_post_edittext.text.toString())
                    //閉じる
                    this@CommentPOSTBottomFragment.dismiss()
                }
            }
            false
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //IMEを閉じる
        val imm = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(bottom_fragment_post_edittext.windowToken, 0)
    }

}