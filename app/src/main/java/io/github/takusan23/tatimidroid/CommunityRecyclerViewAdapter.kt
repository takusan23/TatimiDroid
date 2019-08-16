package io.github.takusan23.tatimidroid

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Fragment.AutoAdmissionBottomFragment
import io.github.takusan23.tatimidroid.Fragment.BottomSheetDialogWatchMode
import kotlinx.android.synthetic.main.fragment_liveid.*
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class CommunityRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<CommunityRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_community_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = holder.timeTextView.context
        val darkModeSupport = DarkModeSupport(content)

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val title = item.get(1)
        val name = item.get(2)
        val live = item.get(3)
        val live_time = item.get(4)
        val timeshift = item.get(5)
        val liveId = item.get(6)
        val datetime = item.get(7)

        holder.titleTextView.text = "${title}\n[${name}]"

        if (live_time.isNotEmpty()) {
            //放送中
            holder.timeTextView.text = "${live} ${live_time}"
            holder.timeTextView.setTextColor(Color.RED)
        } else {
            //予約枠
            holder.timeTextView.text = "${timeshift}"
            if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
                holder.timeTextView.setTextColor(Color.parseColor("#ffffff"))
            } else {
                holder.timeTextView.setTextColor(-1979711488)   //デフォルトのTextViewのフォント色
            }
        }

        //Cardを選択したらコメントビューワーに
        holder.communityCard.setOnClickListener {
            if (live_time.isNotEmpty()) {
                //ダイアログ
                val bundle = Bundle()
                bundle.putString("liveId", liveId)
                val dialog = BottomSheetDialogWatchMode()
                dialog.arguments = bundle
                dialog.show((content as AppCompatActivity).supportFragmentManager, "watchmode")
            } else {
                if (datetime.isNotEmpty()) {
                    //予約枠自動入場機能つかうか？
                    Snackbar.make(holder.timeTextView, content.getText(R.string.timeshift_wait), Snackbar.LENGTH_SHORT)
                        .setAction(
                            content.getText(R.string.auto_admission)
                        ) {

                            //文字列（時間）->ミリ秒
                            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            val date = simpleDateFormat.parse(datetime)
                            val startTime = (date.time).toString()

                            //追加BottomSheet
                            val bundle = Bundle()
                            bundle.putString("program", name)
                            bundle.putString("liveId", liveId)
                            bundle.putString("start", startTime)
                            bundle.putString("description", "")

                            val autoAdmissionBottomFragment = AutoAdmissionBottomFragment()
                            autoAdmissionBottomFragment.arguments = bundle
                            autoAdmissionBottomFragment.show(
                                (content as AppCompatActivity).supportFragmentManager,
                                "auto_admission"
                            )
                        }.show()
                    //Toast.makeText(content, content.getText(R.string.timeshift_wait), Toast.LENGTH_SHORT).show()
                }
            }
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
