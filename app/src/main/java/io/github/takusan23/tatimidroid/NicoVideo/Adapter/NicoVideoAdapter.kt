package io.github.takusan23.tatimidroid.NicoVideo.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import java.text.SimpleDateFormat
import java.util.*

class NicoVideoAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<NicoVideoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val name = item.get(1)
        val comment = item.get(2)
        val date = item.get(3)
        val vpos = item.get(4)

        holder.commentTextView.text = comment
        holder.userNameTextView.text = setTimeFormat(date.toLong()) + " / " + vpos

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var commentTextView: TextView
        var userNameTextView: TextView

        init {
            userNameTextView = itemView.findViewById(R.id.adapter_nicovideo_user_textview)
            commentTextView = itemView.findViewById(R.id.adapter_nicovideo_comment_textview)
        }
    }

    fun setTimeFormat(date: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        return simpleDateFormat.format(date * 1000)
    }

}
