package io.github.takusan23.tatimidroid.NicoLive.Adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.JK.NicoJKData
import io.github.takusan23.tatimidroid.NicoLive.CommentFragment
import io.github.takusan23.tatimidroid.R

/** 実況チャンネル一覧Adapter */
class NicoJKProgramAdapter(private val nicoJKDataList: ArrayList<NicoJKData>) : RecyclerView.Adapter<NicoJKProgramAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nico_jk, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return nicoJKDataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val nicoJKData = nicoJKDataList[position]
        holder.apply {
            val context = nicoJKChannelIdTextView.context
            nicoJKChannelIdTextView.text = nicoJKData.channnelId
            nicoJKChannelNameTextView.text = nicoJKData.title
            nicoJKIkioiTextView.text = nicoJKData.ikioi
            // 押したとき
            cardView.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("liveId", nicoJKData.channnelId)
                    putString("watch_mode", "comment_post")
                    putBoolean("isOfficial", false)
                    putBoolean("is_jk",true)
                }
                val commentFragment = CommentFragment()
                commentFragment.arguments = bundle
                (context as MainActivity).setPlayer(commentFragment, nicoJKData.channnelId)
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nicoJKChannelIdTextView: TextView =
            itemView.findViewById(R.id.adapter_nico_jk_channel_id)
        val nicoJKChannelNameTextView: TextView =
            itemView.findViewById(R.id.adapter_nico_jk_channel_name)
        val nicoJKIkioiTextView: TextView = itemView.findViewById(R.id.adapter_nico_jk_ikioi)
        val cardView: CardView = itemView.findViewById(R.id.adapter_nico_jk_parent)
    }
}