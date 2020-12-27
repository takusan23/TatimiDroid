package io.github.takusan23.tatimidroid.NicoLive.BottomFragment

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.Login.NicoLogin
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoLive.CommentFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.databinding.BottomFragmentWatchmodeBinding
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 視聴モード選択（コメント投稿モードなど）
 * */
class WatchModeBottomFragment : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentWatchmodeBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //ダークモード
        val darkModeSupport = DarkModeSupport(requireContext())
        viewBinding.dialogWatchmodeParentLinearLayout.background = ColorDrawable(getThemeColor(darkModeSupport.context))
        getProgram()
    }

    /**
     * 番組情報取得
     * */
    fun getProgram() {
        val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        //LiveID
        val liveId = arguments?.getString("liveId") ?: ""
        val user_session = prefSetting.getString("user_session", "")
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            activity?.runOnUiThread {
                Toast.makeText(context, "${getString(R.string.error)}\n${throwable}", Toast.LENGTH_SHORT).show()
            }
        }
        // 番組情報取得
        val nicoLiveHTML = NicoLiveHTML()
        lifecycleScope.launch(errorHandler) {
            // 視聴ページHTML取得
            val nicoHTMLResponse = nicoLiveHTML.getNicoLiveHTML(liveId, user_session)
            if (!nicoHTMLResponse.isSuccessful) {
                // 失敗時は落とす
                Toast.makeText(context, "${getString(R.string.error)}\n${nicoHTMLResponse.code}", Toast.LENGTH_SHORT).show()
                this@WatchModeBottomFragment.dismiss()
                return@launch
            }
            // JSON取り出し
            val jsonObject = withContext(Dispatchers.Default) {
                nicoLiveHTML.nicoLiveHTMLtoJSONObject(nicoHTMLResponse.body?.string())
            }
            // ログイン済みか
            val niconicoId = nicoHTMLResponse.headers["x-niconico-id"]
            // ログインが切れていれば再ログイン
            if (niconicoId == null) {
                Toast.makeText(context, R.string.re_login, Toast.LENGTH_SHORT).show()
                // 再ログインする
                NicoLogin.secureNicoLogin(context)
                Toast.makeText(context, R.string.re_login_successful, Toast.LENGTH_SHORT).show()
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
                viewBinding.bottomFragmentWatchModeCommentViewerModeButton.setOnClickListener {
                    // Fragment設置
                    val commentFragment = CommentFragment()
                    val bundle = Bundle()
                    bundle.putString("liveId", liveId)
                    bundle.putString("watch_mode", "comment_viewer")
                    bundle.putBoolean("isOfficial", isOfficial)
                    commentFragment.arguments = bundle
                    (requireActivity() as MainActivity).setPlayer(commentFragment, liveId)
                    this@WatchModeBottomFragment.dismiss()
                }

                //コメント投稿モード
                //書き込める
                viewBinding.bottomFragmentWatchModeCommentPostModeButton.setOnClickListener {
                    //画面移動
                    val commentFragment = CommentFragment()
                    val bundle = Bundle()
                    bundle.putString("liveId", liveId)
                    bundle.putString("watch_mode", "comment_post")
                    bundle.putBoolean("isOfficial", isOfficial)
                    commentFragment.arguments = bundle
                    (requireActivity() as MainActivity).setPlayer(commentFragment, liveId)
                    this@WatchModeBottomFragment.dismiss()
                }

                //nicocas式コメント投稿モード
                //nicocasのAPIでコメント投稿を行う
                viewBinding.bottomFragmentWatchModeNicocasCommentModeButton.setOnClickListener {
                    //画面移動
                    val commentFragment = CommentFragment()
                    val bundle = Bundle()
                    bundle.putString("liveId", liveId)
                    bundle.putString("watch_mode", "nicocas")
                    bundle.putBoolean("isOfficial", isOfficial)
                    commentFragment.arguments = bundle
                    (requireActivity() as MainActivity).setPlayer(commentFragment, liveId)
                    this@WatchModeBottomFragment.dismiss()
                }
            } else if (!canWatchLive) {
                //フォロワー限定番組だった
                activity?.runOnUiThread {
                    dismiss()
                    Toast.makeText(context, getString(R.string.error_follower_only), Toast.LENGTH_SHORT).show()
                }
            } else if (status == "RELEASED" && canWatchLive) {
                // 予約枠で視聴が可能な場合（コミュ限とかではない場合）
                // 予約枠BottomSheet展開。予約枠自動入場やタイムシフト予約ができますよー
                val programMenuBottomSheet = ProgramMenuBottomSheet()
                val bundle = Bundle().apply {
                    putString("liveId", liveId)
                }
                programMenuBottomSheet.arguments = bundle
                programMenuBottomSheet.show(childFragmentManager, "reservation")
                // けす
                dismiss()
            } else {
                dismiss()
                Toast.makeText(context, getString(R.string.program_end), Toast.LENGTH_SHORT).show()
            }
        }
    }
}