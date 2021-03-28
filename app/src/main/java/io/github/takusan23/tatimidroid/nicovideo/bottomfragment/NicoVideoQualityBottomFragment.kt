package io.github.takusan23.tatimidroid.nicovideo.bottomfragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.databinding.BottomFragmentNicovideoQualityBinding

/**
 * ニコ動の画質変更BottomFragment
 *
 * ViewModelを使ってやり取りしているのでargumentには何も入れなくておk
 * */
class NicoVideoQualityBottomFragment : BottomSheetDialogFragment() {

    /** findViewById駆逐 */
    private val viewBinding by lazy { BottomFragmentNicovideoQualityBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModelでデータ受け取る
        val viewModel by viewModels<NicoVideoViewModel>({ requireParentFragment() })
        // 選択中の画質
        val selectQuality = viewModel.currentVideoQuality
        // 画質JSONパース
        val videoQualityJSONArray = viewModel.nicoVideoHTML.parseVideoQualityDMC(viewModel.nicoVideoJSON.value!!)
        val audioQualityJSONArray = viewModel.nicoVideoHTML.parseAudioQualityDMC(viewModel.nicoVideoJSON.value!!)
        // 音声は一番いいやつ？
        val audioId = audioQualityJSONArray.getJSONObject(0).getString("id")
        for (i in 0 until videoQualityJSONArray.length()) {
            // 必要なやつ
            val quality = videoQualityJSONArray.getJSONObject(i)
            val id = quality.getString("id")
            val label = quality.getJSONObject("metadata").getString("label")
            // 一般会員は選べない画質がある（480pからプレ垢限定とか嘘でしょ？金払ってないやつクソ画質で見てんのか・・・）
            val available = quality.getBoolean("isAvailable")
            // TextView
            val textView = TextView(context).apply {
                text = label
                textSize = 24F
                maxLines = 1
                setPadding(10, 10, 10, 10)
                setOnClickListener {
                    // 画質変更して再リクエスト
                    viewModel.coroutine(false, id, audioId)
                    this@NicoVideoQualityBottomFragment.dismiss()
                }
                /**
                 * プレ垢限定 か 強制エコノミー ?eco=1 のときは availableがtrueにはならない
                 * */
                if (!available) {
                    isEnabled = false
                    text = "$label (プレ垢限定画質だから入って；；)"
                }
                // 選択中の画質など
                if (id == selectQuality) {
                    setTextColor(Color.parseColor("#0d46a0"))
                }
            }
            viewBinding.bottomFragmentNicovideoQualityLinearLayout.addView(textView)
        }
    }

}