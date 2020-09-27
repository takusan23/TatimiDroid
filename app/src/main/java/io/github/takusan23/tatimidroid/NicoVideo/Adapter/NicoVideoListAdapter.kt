package io.github.takusan23.tatimidroid.NicoVideo.Adapter

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.github.takusan23.tatimidroid.Fragment.DialogBottomSheet
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoListMenuBottomFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import io.github.takusan23.tatimidroid.Tool.AnniversaryDate
import io.github.takusan23.tatimidroid.Tool.calcAnniversary
import io.github.takusan23.tatimidroid.Tool.isConnectionInternet
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動の動画を一覧で表示するときに使うAdapter。
 * ランキング、視聴履歴の一覧から関連動画等色んな所で使ってる。
 * */
class NicoVideoListAdapter(private val nicoVideoDataList: ArrayList<NicoVideoData>) : RecyclerView.Adapter<NicoVideoListAdapter.ViewHolder>() {

    lateinit var prefSetting: SharedPreferences
    lateinit var nicoVideoCache: NicoVideoCache

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
                // すでにあるActivityを消す？
                if (context is NicoVideoActivity) {
                    context.finish()
                }
                // なんかキャッシュが存在しない時があるらしい？
                if (data.isCache && nicoVideoCache.getCacheFolderVideoFileName(data.videoId) == null) {
                    // 再取得ダイアログ出す
                    val buttonItems = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>().apply {
                        add(DialogBottomSheet.DialogBottomSheetItem("再取得する", R.drawable.ic_save_alt_black_24dp))
                        add(DialogBottomSheet.DialogBottomSheetItem("キャンセル", R.drawable.ic_arrow_back_black_24dp, Color.parseColor("#ff0000")))
                    }
                    DialogBottomSheet("キャッシュが存在しませんでした。再取得しますか？", buttonItems) { i, bottomSheetDialogFragment ->
                        if (i == 0) {
                            // インターネット環境があれば再取得
                            if (isConnectionInternet(context)) {
                                // 再取得
                                startCacheService(context, data.videoId, false)
                            } else {
                                Toast.makeText(context, "インターネットへ接続できません", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.show((context as AppCompatActivity).supportFragmentManager, "cache")
                } else {
                    // 再生方法
                    val playType = prefSetting.getString("setting_play_type_video", "default") ?: "default"
                    when (playType) {
                        "default" -> {
                            // 画面遷移
                            val intent = Intent(context, NicoVideoActivity::class.java)
                            intent.putExtra("id", data.videoId)
                            intent.putExtra("cache", data.isCache)
                            context.startActivity(intent)
                        }
                        "popup" -> {
                            startVideoPlayService(context = context, mode = "popup", videoId = data.videoId, isCache = data.isCache)
                        }
                        "background" -> {
                            startVideoPlayService(context = context, mode = "background", videoId = data.videoId, isCache = data.isCache)
                        }
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
                menuBottomSheet.show((context as AppCompatActivity).supportFragmentManager, "menu")
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