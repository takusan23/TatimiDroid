package io.github.takusan23.tatimidroid.NicoVideo.BottomFragment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_quality.*
import org.json.JSONArray
import org.json.JSONObject

class NicoVideoQualityBottomFragment : BottomSheetDialogFragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_quality, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val devNicoVideoFragment = requireParentFragment() as NicoVideoFragment

        // データ受け取り
        // dmcInfo(DMCサーバー)かsmileInfo（Smileサーバー）か
        val isDmcInfo = arguments?.getBoolean("is_dmc") ?: true
        // video.dmcInfo.qualityかvideo.smileInfo.qualityIdsの値。
        val qualityList = arguments?.getString("quality")
        // 選択中の画質
        val selectQuality = arguments?.getString("select")

        if (isDmcInfo) {
            // video.dmcInfo.qualityのJSONパース
            val jsonObject = JSONObject(qualityList)
            val videoQualityJSONArray = jsonObject.getJSONArray("videos")
            val audioQualityJSONArray = jsonObject.getJSONArray("audios")
            // 音声は一番いいやつ？
            val audioId = audioQualityJSONArray.getJSONObject(0).getString("id")
            for (i in 0 until videoQualityJSONArray.length()) {
                // 必要なやつ
                val quality = videoQualityJSONArray.getJSONObject(i)
                val id = quality.getString("id")
                val label = quality.getString("label")
                // 一般会員は選べない画質がある（480pからプレ垢限定とか嘘でしょ？金払ってないやつクソ画質で見てんのか・・・）
                val available = quality.getBoolean("available")
                // TextView
                val textView = TextView(context).apply {
                    text = label
                    textSize = 24F
                    maxLines = 1
                    setPadding(10, 10, 10, 10)
                    setOnClickListener {
                        // 画質変更して再リクエスト
                        // 今の再生時間控える
                        devNicoVideoFragment.apply {
                            viewModel.coroutine(false, id, audioId)
                        }
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
                bottom_fragment_nicovideo_quality_linearlayout.addView(textView)
            }
        } else {
            // video.smileInfo.qualityIdsパース
            // smileサーバーの動画は自動か低画質しかない？
            val jsonObject = JSONArray(qualityList)
            for (i in 0 until jsonObject.length()) {
                val name = jsonObject.getString(i)
                // TextView
                val textView = TextView(context).apply {
                    text = name
                    textSize = 24F
                    setPadding(10, 10, 10, 10)
                    setOnClickListener {
                        // 画質変更して再リクエスト
                        // 今の再生時間控える
                        devNicoVideoFragment.apply {
                            viewModel.coroutine(false, "", "", name == "low")
                        }
                        this@NicoVideoQualityBottomFragment.dismiss()
                    }
                }
                bottom_fragment_nicovideo_quality_linearlayout.addView(textView)
            }
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}