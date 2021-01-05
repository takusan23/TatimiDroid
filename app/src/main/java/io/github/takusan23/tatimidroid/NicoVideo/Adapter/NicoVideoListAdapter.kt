package io.github.takusan23.tatimidroid.NicoVideo.Adapter

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoListMenuBottomFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.AnniversaryDate
import io.github.takusan23.tatimidroid.Tool.calcAnniversary
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動の動画を一覧で表示するときに使うAdapter。
 * ランキング、視聴履歴の一覧から関連動画等色んな所で使ってる。
 * */
class NicoVideoListAdapter(val nicoVideoDataList: ArrayList<NicoVideoData>) : RecyclerView.Adapter<NicoVideoListAdapter.ViewHolder>() {

    private lateinit var prefSetting: SharedPreferences
    private lateinit var nicoVideoCache: NicoVideoCache

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_title)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_info)
        val dateTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_date)
        val cardView = itemView.findViewById<CardView>(R.id.adapter_nicovideo_list_cardview)
        val thumImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_list_thum)
        val menuImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_list_menu)
        val durationTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NicoVideoListAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return nicoVideoDataList.size
    }

    override fun onBindViewHolder(holder: NicoVideoListAdapter.ViewHolder, position: Int) {
        val data = nicoVideoDataList[position]
        holder.apply {
            val context = titleTextView.context
            // 初期化
            if (!::prefSetting.isInitialized) {
                prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
                nicoVideoCache = NicoVideoCache(context)
            }
            // 投稿日時。一周年とかを祝えるように
            val anniversary = calcAnniversary(data.date)
            when {
                anniversary == 0 -> {
                    // 本日投稿のときは文字赤くするだけ
                    dateTextView.text = "${toFormatTime(data.date)} ${context?.getString(R.string.post)}"
                    dateTextView.setTextColor(Color.RED)
                }
                anniversary != -1 -> {
                    // お祝い！
                    dateTextView.text = "${AnniversaryDate.makeAnniversaryMessage(anniversary)}\n${toFormatTime(data.date)} ${context?.getString(R.string.post)}"
                    dateTextView.setTextColor(Color.RED)
                }
                else -> {
                    // いつもどおり
                    dateTextView.text = "${toFormatTime(data.date)} ${context?.getString(R.string.post)}"
                    dateTextView.setTextColor(titleTextView.textColors)
                }
            }
            // TextView
            titleTextView.text = "${data.title}\n${data.videoId}"
            // 再生回数、マイリスト数、コメント数がすべて-1以外なら表示させる（ニコレポは再生回数取れない）
            if (data.viewCount != "-1" && data.mylistCount != "-1" && data.commentCount != "-1") {
                infoTextView.text = "${context.getString(R.string.view_count)}：${data.viewCount} | ${context.getString(R.string.comment_count)}：${data.commentCount} | ${context.getString(R.string.mylist_count)}：${data.mylistCount}"
            } else {
                infoTextView.text = ""
            }
            // 再生時間。ない場合がある
            if (data.duration != null && data.duration > 0) {
                val formatTime = SimpleDateFormat("mm:ss").format(data.duration * 1000)
                durationTextView.isVisible = true
                durationTextView.text = formatTime
            } else {
                durationTextView.isVisible = false
            }
            // 再生画面表示
            cardView.setOnClickListener {

                // 再生方法
                val playType = prefSetting.getString("setting_play_type_video", "default") ?: "default"

                // 連続再生を利用するか。trueで利用
                val isDefaultPlaylistMode = prefSetting.getBoolean("setting_nicovideo_default_playlist_mode", true)

                when (playType) {
                    "default" -> {
                        // 画面遷移
                        (context as? MainActivity)?.setNicovideoFragment(
                            videoId = data.videoId,
                            isCache = data.isCache,
                            _videoList = if (isDefaultPlaylistMode) nicoVideoDataList else null
                        )
                    }
                    "popup" -> {
                        startVideoPlayService(context = context, mode = "popup", videoId = data.videoId, isCache = data.isCache)
                    }
                    "background" -> {
                        startVideoPlayService(context = context, mode = "background", videoId = data.videoId, isCache = data.isCache)
                    }
                }

            }

            // メニュー画面表示
            menuImageView.setOnClickListener {
                val menuBottomSheet = NicoVideoListMenuBottomFragment()
                // データ渡す
                val bundle = Bundle()
                bundle.putString("video_id", data.videoId)
                bundle.putBoolean("is_cache", data.isCache)
                bundle.putSerializable("data", data)
                bundle.putSerializable("video_list", nicoVideoDataList)
                menuBottomSheet.arguments = bundle
                (context as MainActivity).currentFragment()?.apply {
                    menuBottomSheet.show(this.childFragmentManager, "menu")
                }
            }

            // サムネイル
            thumImageView.imageTintList = null
            Glide.with(thumImageView)
                .load(data.thum)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(thumImageView)

        }
    }

    fun toFormatTime(time: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss")
        return simpleDateFormat.format(time)
    }

}