package io.github.takusan23.tatimidroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class GiftRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<GiftRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_gift_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val name = item.get(1)
        val point = item.get(2)

        holder.pointTextView.text = point
        holder.nameTextView.text = name

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var pointTextView: TextView
        var nameTextView: TextView

        init {
            pointTextView = itemView.findViewById(R.id.adapter_gift_point)
            nameTextView = itemView.findViewById(R.id.adapter_gift_name)
        }
    }
}
