package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.dialog_watchmode_layout.*

class BottomSheetDialogWatchMode : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_watchmode_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        dialog_watchmode_parent_linearlayout.background = ColorDrawable(darkModeSupport.getThemeColor())

        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref_setting.edit()
        //LiveID
        val liveId = arguments?.getString("liveId")

        //コメントビューワーモード
        //コメント投稿機能、視聴継続メッセージ送信機能なし
        dialog_watchmode_comment_viewer_mode_button.setOnClickListener {
            //設定変更
            editor.putBoolean("setting_watching_mode", false)
            editor.putBoolean("setting_nicocas_mode", false)
            editor.apply()
            //画面移動
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("liveId", liveId)
            startActivity(intent)
            this@BottomSheetDialogWatchMode.dismiss()
        }

        //コメント投稿モード
        //書き込める
        dialog_watchmode_comment_post_mode_button.setOnClickListener {
            //設定変更
            editor.putBoolean("setting_watching_mode", true)
            editor.putBoolean("setting_nicocas_mode", false)
            editor.apply()
            //画面移動
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("liveId", liveId)
            startActivity(intent)
            this@BottomSheetDialogWatchMode.dismiss()
        }

        //nicocas式コメント投稿モード
        //nicocasのAPIでコメント投稿を行う
        dialog_watchmode_nicocas_comment_mode_button.setOnClickListener {
            //設定変更
            editor.putBoolean("setting_watching_mode", false)
            editor.putBoolean("setting_nicocas_mode", true)
            editor.apply()
            //画面移動
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("liveId", liveId)
            startActivity(intent)
            this@BottomSheetDialogWatchMode.dismiss()
        }

    }

}