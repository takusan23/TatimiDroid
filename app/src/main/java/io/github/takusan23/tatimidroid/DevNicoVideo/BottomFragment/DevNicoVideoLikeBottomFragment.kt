package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoInfoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_like.*
import kotlinx.coroutines.*

/**
 * いいね♡するBottomFragment。初見わからんから説明文付き。
 * 普及したら消します。
 * 欲しい物
 * video_id  | String | 動画ID。DevNicoVideoFragmentを探すのにも使う。
 * */
class DevNicoVideoLikeBottomFragment : BottomSheetDialogFragment() {

    // Fragment
    private val videoId by lazy { arguments?.getString("video_id")!! }
    private val devNicoVideoFragment by lazy { parentFragmentManager.findFragmentByTag(videoId) as DevNicoVideoFragment }
    private val nicoVideoInfoFragment by lazy { (devNicoVideoFragment.viewPager.fragmentList[2] as NicoVideoInfoFragment) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_like, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // メッセージ
        bottom_fragment_nicovideo_like_description.text = """
            いいね機能 #とは
            動画を応援できる機能。
            ・いいねしたユーザーは投稿者のみ見ることができます。
            ・いいね数はランキングに影響します。
            ・一般会員でも使えるそうです。
        """.trimIndent()

        // Like押した
        bottom_fragment_nicovideo_like_button.setOnClickListener {
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                showToast("${getString(R.string.error)}\n${throwable.message}") // エラーのときはToast出すなど
            }
            // API叩く
            GlobalScope.launch(errorHandler) {
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