package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoInfoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_like.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * いいね♡するBottomFragment。初見わからんから説明文付き。
 * 普及したら消します。
 * 欲しい物
 * video_id  | String | 動画ID。DevNicoVideoFragmentを探すのにも使う。
 * */
class NicoVideoLikeBottomFragment : BottomSheetDialogFragment() {

    // requireParentFragment() が普通に動いたわ。parentFragmentManagerのときはNicoVideoFragment。childFragmentManagerのときはNicoVideoInfoFragmentになる
    private val nicoVideoFragment by lazy { requireParentFragment() as NicoVideoFragment }
    private val nicoVideoInfoFragment by lazy { (nicoVideoFragment.viewPager.fragmentList[2] as NicoVideoInfoFragment) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_like, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // メッセージ
        bottom_fragment_nicovideo_like_description.text = HtmlCompat.fromHtml(
            """
            いいね機能 #とは<br>
            動画を応援できる機能。<br>
            ・いいねしたユーザーは投稿者のみ見ることができます。<br>
            ・いいね数はランキングに影響します。<br>
            ・一般会員でも使えるそうです。<br>
            <span style="color:#FFA500">いいねするとうｐ主からお礼のメッセージが見れます</span><br>
            <small>（設定してある場合のみ）</small>
        """.trimIndent(), HtmlCompat.FROM_HTML_MODE_COMPACT
        )

        // Like押した
        bottom_fragment_nicovideo_like_button.setOnClickListener {
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable}") // エラーのときはToast出すなど
            }
            // API叩く
            lifecycleScope.launch(errorHandler) {
                nicoVideoInfoFragment.sendLike(true)
                withContext(Dispatchers.Main) {
                    dismiss() // 閉じる
                }
            }
        }
    }

    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}