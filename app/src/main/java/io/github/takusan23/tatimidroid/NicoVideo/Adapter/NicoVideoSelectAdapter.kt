package io.github.takusan23.tatimidroid.NicoVideo.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoActivity
import io.github.takusan23.tatimidroid.R
import java.text.SimpleDateFormat
import java.util.*

class NicoVideoSelectAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<NicoVideoSelectAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.adapter_nicovideo_select, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val type = item.get(0)
        val id = item.get(1)
        val title = item.get(2)
        val lastWatch = item.get(3)
        val registeredAt = item.get(4)
        val watchCount = item.get(5)
        val thumbnailUrl = item.get(6)

        val commentCount = item.get(7)
        val playCount = item.get(8)
        val mylistCount = item.get(9)

        val context = holder.titleTextView.context

        holder.titleTextView.text = title

        when (type) {
            "history" -> {
                holder.dateTextView.text =
                    "${context.getString(R.string.last_watch)} : ${setTimeFormat(lastWatch)}"
                holder.commentCountTextView.text =
                    "${context.getString(R.string.comment_count)} : $commentCount"
                holder.playCountTextView.text =
                    "${context.getString(R.string.play_count)} : $playCount"
                holder.mylistCountTextView.text =
                    "${context.getString(R.string.mylist)} : $mylistCount"
                holder.postDateTextView.text =
                    "${context.getString(R.string.post_date)} : ${setTimeFormat(registeredAt)}"
            }
            "mylist" -> {
                holder.dateTextView.text =
                    "${context.getString(R.string.register_date)} : ${unixTimeToFormat(lastWatch.toLong() * 1000)}" //ほしいのはミリ秒なUnixTimeなので
                holder.commentCountTextView.text =
                    "${context.getString(R.string.comment_count)} : $commentCount"
                holder.playCountTextView.text =
                    "${context.getString(R.string.play_count)} : $playCount"
                holder.mylistCountTextView.text =
                    "${context.getString(R.string.mylist)} : $mylistCount"
                holder.postDateTextView.text =
                    "${context.getString(R.string.post_date)} : ${unixTimeToFormat(registeredAt.toLong())}"
            }
            "content_tree" -> {
                holder.dateTextView.text = ""
                holder.commentCountTextView.text = ""
                holder.playCountTextView.text = ""
                holder.mylistCountTextView.text = ""
                holder.postDateTextView.text =
                    "${context.getString(R.string.register_date)} : $registeredAt"
            }
            "series" -> {
                holder.dateTextView.text =
                    "${context.getString(R.string.post_date)} : ${setTimeFormat(lastWatch)}" //ほしいのはミリ秒なUnixTimeなので
                holder.commentCountTextView.text = ""
                holder.playCountTextView.text = ""
                holder.mylistCountTextView.text = ""
                holder.postDateTextView.text = ""
            }
            else -> {
                holder.dateTextView.text = lastWatch
                holder.commentCountTextView.text = commentCount
                holder.playCountTextView.text = playCount
                holder.mylistCountTextView.text = mylistCount
                holder.postDateTextView.text = registeredAt
            }
        }

        //Card押した時
        holder.cardView.setOnClickListener {
            val intent = Intent(context, NicoVideoActivity::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView
        var dateTextView: TextView
        var cardView: CardView
        val postDateTextView: TextView

        var commentCountTextView: TextView
        var playCountTextView: TextView
        var mylistCountTextView: TextView

        init {
            titleTextView = itemView.findViewById(R.id.adapter_nicovideo_select_title_textview)
            dateTextView = itemView.findViewById(R.id.adapter_nicovideo_select_date_textview)
            cardView = itemView.findViewById(R.id.adapter_nicovideo_select_cardview)
            postDateTextView =
                itemView.findViewById(R.id.adapter_nicovideo_select__post_date_textview)
            commentCountTextView =
                itemView.findViewById(R.id.adapter_nicovideo_select_comment_count_textview)
            playCountTextView =
                itemView.findViewById(R.id.adapter_nicovideo_select_play_count_textview)
            mylistCountTextView =
                itemView.findViewById(R.id.adapter_nicovideo_select_mylist_count_textview)
        }
    }

    //日付フォーマット。投稿のときは利用しない
    fun setTimeFormat(iso8601: String): String? {
        if (!iso8601.contains("投稿") && iso8601.isNotEmpty()) {
            //パース前の形。ISO8601の形
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
            val date = simpleDateFormat.parse(iso8601)
            //以下の形に戻す
            val fromSimpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            return fromSimpleDateFormat.format(date)
        }
        return iso8601
    }

    fun unixTimeToFormat(unixtime: Long): String {
        //パース前の形。ISO8601の形
        val fromSimpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        return fromSimpleDateFormat.format(Date(unixtime))
    }

}