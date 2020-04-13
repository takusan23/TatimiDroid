package io.github.takusan23.tatimidroid.JK

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.R

class NicoJKProgramAdapter(val nicoJKDataList: ArrayList<NicoJKData>) :
    RecyclerView.Adapter<NicoJKProgramAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_nico_jk, parent, false)
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
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("liveId", nicoJKData.channnelId)
                intent.putExtra("watch_mode", "comment_post")
                intent.putExtra("isOfficial", false)
                intent.putExtra("is_jk", true) // 実況ならtrue
                context?.startActivity(intent)
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