package io.github.takusan23.tatimidroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Fragment.AutoAdmissionBottomFragment
import io.github.takusan23.tatimidroid.Fragment.BottomSheetDialogWatchMode
import io.github.takusan23.tatimidroid.NicoLiveAPI.ProgramData
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class CommunityRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<ProgramData>) :
    RecyclerView.Adapter<CommunityRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_community_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = holder.timeTextView.context
        val activity = (content as MainActivity)
        val darkModeSupport = DarkModeSupport(content)

        val item = arrayListArrayAdapter[position]
        val title = item.title
        val name = item.communityName
        // val live = item.get(3)
        val live_time = item.beginAt
        // val timeshift = item.get(5)
        val liveId = item.programId
        val datetime = item.beginAt
        val liveNow = item.lifeCycle

        //時間を文字列に
        val simpleDateFormat = SimpleDateFormat("MM/dd HH:mm:ss")
        val time = simpleDateFormat.format(live_time.toLong())

        holder.titleTextView.text = "${title}\n[${name}]"

        if (liveNow.contains("ON_AIR") || liveNow.contains("Begun")) {
            //放送中
            holder.timeTextView.text = time
            holder.timeTextView.setTextColor(Color.RED)
        } else {
            //予約枠
            holder.timeTextView.text = time
            if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
                holder.timeTextView.setTextColor(Color.parseColor("#ffffff"))
            } else {
                holder.timeTextView.setTextColor(-1979711488)   //デフォルトのTextViewのフォント色
            }
        }

        //Cardを選択したらコメントビューワーに
        holder.communityCard.setOnClickListener {
            //ダイアログ
            val bundle = Bundle()
            bundle.putString("liveId", liveId)
            val dialog = BottomSheetDialogWatchMode()
            dialog.arguments = bundle
            dialog.show((content as AppCompatActivity).supportFragmentManager, "watchmode")
        }

        /*
        * 番組IDコピー機能
        * */
        holder.communityCard.setOnLongClickListener {
            Toast.makeText(
                    content, "${content.getString(R.string.copy_program_id)} : $liveId",
                    Toast.LENGTH_SHORT
                )
                .show()
            val clipboardManager =
                content.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
            true
        }

    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView
        var timeTextView: TextView
        var communityCard: CardView

        init {
            titleTextView = itemView.findViewById(R.id.adapter_community_title_textview)
            timeTextView = itemView.findViewById(R.id.adapter_community_time_textview)
            communityCard = itemView.findViewById(R.id.adapter_community_card)
        }
    }
}
