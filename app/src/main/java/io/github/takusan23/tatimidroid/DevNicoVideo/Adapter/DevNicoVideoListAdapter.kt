package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoListMenuBottomFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.DevNicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.Fragment.DialogBottomSheet
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.startCacheService
import io.github.takusan23.tatimidroid.Service.startVideoPlayService
import java.text.SimpleDateFormat
import java.util.ArrayList

class DevNicoVideoListAdapter(val nicoVideoDataList: ArrayList<NicoVideoData>) :
    RecyclerView.Adapter<DevNicoVideoListAdapter.ViewHolder>() {

    lateinit var prefSetting: SharedPreferences
    lateinit var nicoVideoCache: NicoVideoCache

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_title)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_info)
        val dateTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_date)
        val cardView = itemView.findViewById<CardView>(R.id.adapter_nicovideo_list_cardview)
        val thumImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_list_thum)
        val menuImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_list_menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DevNicoVideoListAdapter.ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_nicovideo_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return nicoVideoDataList.size
    }

    override fun onBindViewHolder(holder: DevNicoVideoListAdapter.ViewHolder, position: Int) {
        val data = nicoVideoDataList[position]
        holder.apply {
            val context = titleTextView.context
            // 初期化
            if (!::prefSetting.isInitialized) {
                prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
                nicoVideoCache = NicoVideoCache(context)
            }
            // TextView
            dateTextView.text = "${toFormatTime(data.date)} ${context?.getString(R.string.post)}"
            titleTextView.text = "${data.title}\n${data.videoId}"
            // 再生回数、マイリスト数、コメント数がすべて-1以外なら表示させる（ニコレポは再生回数取れない）
            if (data.viewCount != "-1" && data.mylistCount != "-1" && data.commentCount != "-1") {
                infoTextView.text =
                    "${context.getString(R.string.view_count)}：${data.viewCount} | ${context.getString(R.string.comment_count)}：${data.commentCount} | ${context.getString(R.string.mylist_count)}：${data.mylistCount}"
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
                            // 再取得
                            startCacheService(context, data.videoId, false)
                        }
                    }.show((context as AppCompatActivity).supportFragmentManager, "cache")
                } else {
                    // 再生方法
                    val playType =
                        prefSetting.getString("setting_play_type_video", "default") ?: "default"
                    when (playType) {
                        "default" -> {
                            val intent = Intent(context, NicoVideoActivity::class.java)
                            intent.putExtra("id", data.videoId)
                            intent.putExtra("cache", data.isCache)
                            context?.startActivity(intent)
                        }
                        "popup" -> {
                            startVideoPlayService(context, "popup", data.videoId, data.isCache)
                        }
                        "background" -> {
                            startVideoPlayService(context, "background", data.videoId, data.isCache)
                        }
                    }
                }
            }

            // メニュー画面表示
            menuImageView.setOnClickListener {
                val menuBottomSheet = DevNicoVideoListMenuBottomFragment()
                // データ渡す
                menuBottomSheet.apply {
                    nicoVideoData = data
                    nicoVideoListAdapter = this@DevNicoVideoListAdapter
                    show((context as AppCompatActivity).supportFragmentManager, "menu")
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