package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoAdapter
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.R
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.ArrayList

class DevNicoVideoListAdapter(private val nicoVideoDataList: ArrayList<NicoVideoData>) :
    RecyclerView.Adapter<DevNicoVideoListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_title)
        val infoTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_info)
        val dateTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_list_date)
        val cardView = itemView.findViewById<CardView>(R.id.adapter_nicovideo_list_cardview)
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
            // TextView
            dateTextView.text = "${toFormatTime(data.date)} ${context?.getString(R.string.post)}"
            titleTextView.text = "${data.title}\n${data.videoId}"
            infoTextView.text =
                "${context.getString(R.string.view_count)}：${data.viewCount} | ${context.getString(R.string.comment_count)}：${data.commentCount} | ${context.getString(R.string.mylist_count)}：${data.mylistCount}"

            // 再生画面表示
            cardView.setOnClickListener {
                val intent = Intent(context, NicoVideoActivity::class.java)
                intent.putExtra("id", data.videoId)
                context?.startActivity(intent)
            }

        }
    }

    fun toFormatTime(time: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss")
        return simpleDateFormat.format(time)
    }

}