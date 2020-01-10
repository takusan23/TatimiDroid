package io.github.takusan23.tatimidroid.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import java.text.SimpleDateFormat
import java.util.ArrayList

class NicoHistoryAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<String>>) :
    RecyclerView.Adapter<NicoHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_nico_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val id = item.get(1)
        val type = item.get(2)
        val date = item.get(3)

        holder.titleTextView.text = id
        holder.dateTextView.text = "$type / ${unixToDataFormat(date.toLong())}"

    }

    fun unixToDataFormat(unixTime: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyy/MM/dd HH:mm:ss")
        return simpleDateFormat.format(unixTime)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView
        var dateTextView: TextView

        init {
            titleTextView = itemView.findViewById(R.id.adapter_nico_history_title)
            dateTextView = itemView.findViewById(R.id.adapter_nico_history_date)
        }
    }
}
