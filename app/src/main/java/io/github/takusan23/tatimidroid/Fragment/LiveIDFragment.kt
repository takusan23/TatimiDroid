package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SnackbarProgress
import kotlinx.android.synthetic.main.fragment_liveid.*
import java.util.regex.Pattern

class LiveIDFragment : Fragment() {
    lateinit var pref_setting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_liveid, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        //タイトル
        (activity as AppCompatActivity).supportActionBar?.subtitle = ""

        main_activity_button.setOnClickListener {
            //liveIdを取る。「https://live2.nicovideo.jp/watch/」をからの文字に置き換えてもいいんだけど後ろにパラメーターあるかもだし
            //スマホ用ページもあるからなあ

            val nicoID_Matcher = Pattern.compile("(lv)([0-9]+)")
                .matcher(SpannableString(main_activity_liveid_inputedittext.text.toString()))
            if (nicoID_Matcher.find()) {
                val liveId = nicoID_Matcher.group()
                //メアド、パスワードがあるか
                if (pref_setting.getString("password", "")?.isNotEmpty() ?: false) {
                    // SnackbarProgress(context!!, main_activity_button, getString(R.string.loading)).show()
                    //ダイアログ
                    val bundle = Bundle()
                    bundle.putString("liveId", liveId)
                    val dialog = BottomSheetDialogWatchMode()
                    dialog.arguments = bundle
                    dialog.show((activity as AppCompatActivity).supportFragmentManager, "watchmode")
                    //val intent = Intent(context, CommentActivity::class.java)
                    //intent.putExtra("liveId", liveId)
                    //startActivity(intent)
                } else {
                    Toast.makeText(context, getString(R.string.mail_pass_error), Toast.LENGTH_SHORT).show()
                }
            } else {
                //正規表現失敗
                Toast.makeText(context, getString(R.string.regix_error), Toast.LENGTH_SHORT).show()
            }

        }
    }
}