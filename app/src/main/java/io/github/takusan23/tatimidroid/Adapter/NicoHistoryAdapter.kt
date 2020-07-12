package io.github.takusan23.tatimidroid.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import java.text.SimpleDateFormat
import java.util.ArrayList

class NicoHistoryAdapter(private val arrayListArrayAdapter: ArrayList<NicoHistoryDBEntity>) :
    RecyclerView.Adapter<NicoHistoryAdapter.ViewHolder>() {

    lateinit var editText: EditText
    lateinit var bottomSheetDialogFragment: BottomSheetDialogFragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_nico_history, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = arrayListArrayAdapter[position]
        val id = item.serviceId
        val type = item.type
        val date = item.unixTime
        val title = item.title
        val communityId = item.userId

        holder.titleTextView.text = "$title / $id"
        holder.dateTextView.text = unixToDataFormat(date).toString()

        //コミュIDをいれる
        holder.cardView.setOnClickListener {
            if (::editText.isInitialized) {
                val text = if (communityId.isNotEmpty()) {
                    communityId
                } else {
                    id // 動画用に
                }
                editText.setText(text)
            }
            //けす
            if (::bottomSheetDialogFragment.isInitialized) {
                bottomSheetDialogFragment.dismiss()
            }
        }
        // 長押しで番組ID
        holder.cardView.setOnLongClickListener {
            if (::editText.isInitialized) {
                editText.setText(id)
            }
            //けす
            if (::bottomSheetDialogFragment.isInitialized) {
                bottomSheetDialogFragment.dismiss()
            }
            true
        }

        // アイコン
        val icon = if (type == "video") {
            holder.typeIcon.context.getDrawable(R.drawable.ic_local_movies_24px)
        } else {
            holder.typeIcon.context.getDrawable(R.drawable.ic_outline_live_tv_24px_black)
        }
        holder.typeIcon.setImageDrawable(icon)
    }

    fun unixToDataFormat(unixTime: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyy/MM/dd HH:mm:ss")
        return simpleDateFormat.format(unixTime * 1000)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView
        var dateTextView: TextView
        var parentLinearLayout: LinearLayout
        var cardView: CardView
        var typeIcon: ImageView

        init {
            parentLinearLayout = itemView.findViewById(R.id.adapter_nico_history_parent)
            titleTextView = itemView.findViewById(R.id.adapter_nico_history_title)
            dateTextView = itemView.findViewById(R.id.adapter_nico_history_date)
            cardView = itemView.findViewById(R.id.adapter_nico_hisotry_cardview)
            typeIcon = itemView.findViewById(R.id.adapter_nico_history_icon)
        }
    }
}
