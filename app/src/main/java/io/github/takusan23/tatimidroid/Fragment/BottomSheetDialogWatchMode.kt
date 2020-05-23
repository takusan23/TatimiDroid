package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.dialog_watchmode_layout.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BottomSheetDialogWatchMode : BottomSheetDialogFragment() {

    /*
    * findViewById卒業できない
    * */
    lateinit var commentViewerModeButton: Button
    lateinit var commentPostModeButton: Button
    lateinit var nicocasModeButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_watchmode_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        commentViewerModeButton =
            view.findViewById(R.id.dialog_watchmode_comment_viewer_mode_button)
        commentPostModeButton =
            view.findViewById(R.id.dialog_watchmode_comment_post_mode_button)
        nicocasModeButton =
            view.findViewById(R.id.dialog_watchmode_nicocas_comment_mode_button)

        //ダークモード
        val darkModeSupport = DarkModeSupport(context!!)
        dialog_watchmode_parent_linearlayout.background =
            ColorDrawable(darkModeSupport.getThemeColor())

        getProgram()

    }

    /**
     * 番組情報取得
     * */
    fun getProgram() {
        val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = pref_setting.edit()
        //LiveID
        val liveId = arguments?.getString("liveId") ?: ""
        val user_session = pref_setting.getString("user_session", "")

        // 番組情報取得
        val nicoLiveHTML = NicoLiveHTML()
        GlobalScope.launch {
            // 視聴ページHTML取得
            val nicoHTMLResponse = nicoLiveHTML.getNicoLiveHTML(liveId, user_session).await()
            if (!nicoHTMLResponse.isSuccessful) {
                Handler(Looper.getMainLooper()).post {
                    // 失敗時は落とす
                    Toast.makeText(context, "${getString(R.string.error)}\n${nicoHTMLResponse.code}", Toast.LENGTH_SHORT)
                        .show()
                    this@BottomSheetDialogWatchMode.dismiss()
                }
                return@launch
            }
            // JSON取り出し
            val jsonObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(nicoHTMLResponse.body?.string())
            // ログイン済みか
            val niconicoId = nicoHTMLResponse.headers["x-niconico-id"]
            // ログインが切れていれば再ログイン
            if (niconicoId == null) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, R.string.re_login, Toast.LENGTH_SHORT).show()
                }
                // 再ログインする
                NicoLogin.loginCoroutine(context).await()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, R.string.re_login_successful, Toast.LENGTH_SHORT).show()
                }
            }
            //現在放送中か？
            val status = jsonObject.getJSONObject("program").getString("status")
            //公式番組かどうか
            val type = jsonObject.getJSONObject("program").getString("providerType")
            var isOfficial = false
            if (type.contains("official")) {
                isOfficial = true
            }
            //この番組を視聴可能かどうか。デフォtrue
            var canWatchLive = true
            //コミュ限定か調べる
            val isFollowerOnly =
                jsonObject.getJSONObject("program").getBoolean("isFollowerOnly")
            if (isFollowerOnly) {
                //コミュ限
                canWatchLive = false
                //コミュ限だったとき→コミュニティをフォローしているか確認する
                val isFollowed =
                    jsonObject.getJSONObject("socialGroup").getBoolean("isFollowed")
                if (isFollowed) {
                    canWatchLive = true
                }
            }
            if (status == "ON_AIR" && canWatchLive) {
                //生放送中！
                //コメントビューワーモード
                //コメント投稿機能、視聴継続メッセージ送信機能なし
                commentViewerModeButton.setOnClickListener {
                    //設定変更
                    editor.putBoolean("setting_watching_mode", false)
                    editor.putBoolean("setting_nicocas_mode", false)
                    editor.apply()
                    //画面移動
                    val intent = Intent(context, CommentActivity::class.java)
                    intent.putExtra("liveId", liveId)
                    intent.putExtra("watch_mode", "comment_viewer")
                    intent.putExtra("isOfficial", isOfficial)
                    // intent.putExtra("html", responseString)
                    startActivity(intent)
                    this@BottomSheetDialogWatchMode.dismiss()
                }

                //コメント投稿モード
                //書き込める
                commentPostModeButton.setOnClickListener {
                    //設定変更
                    editor.putBoolean("setting_watching_mode", true)
                    editor.putBoolean("setting_nicocas_mode", false)
                    editor.apply()
                    //画面移動
                    val intent = Intent(context, CommentActivity::class.java)
                    intent.putExtra("liveId", liveId)
                    intent.putExtra("watch_mode", "comment_post")
                    intent.putExtra("isOfficial", isOfficial)
                    //  intent.putExtra("html", responseString)
                    startActivity(intent)
                    this@BottomSheetDialogWatchMode.dismiss()
                }

                //nicocas式コメント投稿モード
                //nicocasのAPIでコメント投稿を行う
                nicocasModeButton.setOnClickListener {
                    //設定変更
                    editor.putBoolean("setting_watching_mode", false)
                    editor.putBoolean("setting_nicocas_mode", true)
                    editor.apply()
                    //画面移動
                    val intent = Intent(context, CommentActivity::class.java)
                    intent.putExtra("liveId", liveId)
                    intent.putExtra("watch_mode", "nicocas")
                    intent.putExtra("isOfficial", isOfficial)
                    // intent.putExtra("html", responseString)
                    startActivity(intent)
                    this@BottomSheetDialogWatchMode.dismiss()
                }
            } else if (!canWatchLive) {
                //フォロワー限定番組だった
                activity?.runOnUiThread {
                    dismiss()
                    Toast.makeText(context, getString(R.string.error_follower_only), Toast.LENGTH_SHORT)
                        .show()
                }
            } else if (status == "RELEASED" && canWatchLive) {
                // 予約枠で視聴が可能な場合（コミュ限とかではない場合）
                // 予約枠BottomSheet展開。予約枠自動入場やタイムシフト予約ができますよー
                val programMenuBottomSheet = ProgramMenuBottomSheet()
                val bundle = Bundle().apply {
                    putString("liveId", liveId)
                }
                programMenuBottomSheet.arguments = bundle
                if (fragmentManager != null) {
                    programMenuBottomSheet.show(fragmentManager!!, "reservation")
                }
                // けす
                dismiss()
            } else {
                activity?.runOnUiThread {
                    dismiss()
                    Toast.makeText(context, getString(R.string.program_end), Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }
}