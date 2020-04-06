package io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.bottom_fragment_nicovideo_quality.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class DevNicoVideoQualityBottomFragment : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_nicovideo_quality, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 動画ID
        val videoId = arguments?.getString("video_id")

        val devNicoVideoFragment =
            fragmentManager?.findFragmentByTag(videoId) as DevNicoVideoFragment

        // データ受け取り
        // dmcInfo(DMCサーバー)かsmileInfo（Smileサーバー）か
        val isDmcInfo = arguments?.getBoolean("is_dmc") ?: true
        // video.dmcInfo.qualityかvideo.smileInfo.qualityIdsの値。
        val qualityList = arguments?.getString("quality")

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
                // TextView
                val textView = TextView(context).apply {
                    text = label
                    textSize = 24F
                    setPadding(10, 10, 10, 10)
                    setOnClickListener {
                        // 画質変更して再リクエスト
                        // 今の再生時間控える
                        devNicoVideoFragment.apply {
                            rotationProgress = exoPlayer.currentPosition
                            coroutine(false, id, audioId)
                        }
                        this@DevNicoVideoQualityBottomFragment.dismiss()
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
                            rotationProgress = exoPlayer.currentPosition
                            coroutine(false, "", "", name == "低画質")
                        }
                        this@DevNicoVideoQualityBottomFragment.dismiss()
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