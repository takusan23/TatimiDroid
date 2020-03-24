package io.github.takusan23.tatimidroid

import android.content.*
import android.content.res.Configuration
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Fragment.AutoAdmissionBottomFragment
import io.github.takusan23.tatimidroid.Fragment.BottomSheetDialogWatchMode
import io.github.takusan23.tatimidroid.NicoLiveAPI.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoLiveAPI.NicoLiveTimeShiftAPI
import io.github.takusan23.tatimidroid.NicoLiveAPI.ProgramData
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionDBUtil
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_fragment_program_reservation.*
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
        val isOfficial = item.isOfficial // 公式ならtrue
        val isOnAir = liveNow.contains("ON_AIR") || liveNow.contains("Begun")

        //時間を文字列に
        val simpleDateFormat = SimpleDateFormat("MM/dd HH:mm:ss")
        val time = simpleDateFormat.format(live_time.toLong())

        holder.titleTextView.text = "${title}\n[${name}]"

        if (isOnAir) {
            //放送中
            holder.timeTextView.text = time
            holder.timeTextView.setTextColor(Color.RED)
            // 視聴モード
            holder.watchModeLinearLayout.visibility = View.VISIBLE
            holder.communityBeforeLinearLayout.visibility = View.GONE
        } else {
            //予約枠
            holder.timeTextView.text = time
            if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
                holder.timeTextView.setTextColor(Color.parseColor("#ffffff"))
            } else {
                holder.timeTextView.setTextColor(-1979711488)   //デフォルトのTextViewのフォント色
            }
            // 視聴モード
            holder.watchModeLinearLayout.visibility = View.GONE
            holder.communityBeforeLinearLayout.visibility = View.GONE
        }

        //Cardを選択したら
        holder.communityCard.setOnClickListener {
            if (!isOnAir) {
                // TS予約など。表示/非表示
                holder.communityBeforeLinearLayout.visibility =
                    if (holder.communityBeforeLinearLayout.visibility == View.VISIBLE) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
            } else {
                //

            }
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


        // 視聴モードボタン
        initWatchModeButton(holder, item)
        // TS予約などのボタン
        initTSButton(holder, item)

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView = itemView.findViewById(R.id.adapter_community_title_textview)
        var timeTextView: TextView = itemView.findViewById(R.id.adapter_community_time_textview)
        var communityCard: CardView = itemView.findViewById(R.id.adapter_community_card)

        // 視聴モード
        val watchModeLinearLayout: LinearLayout =
            itemView.findViewById(R.id.adapter_community_watchmode_linearlayout)
        val watchModeComeView: Button =
            itemView.findViewById(R.id.adapter_community_watchmode_comeview)
        val watchModeComePost: Button =
            itemView.findViewById(R.id.adapter_community_watchmode_comepost)
        val watchModeComeCas: Button =
            itemView.findViewById(R.id.adapter_community_watchmode_nicocas)
        val watchModeDesc: Button =
            itemView.findViewById(R.id.adapter_community_watchmode_description)

        // 予約枠
        val communityBeforeLinearLayout: LinearLayout =
            itemView.findViewById(R.id.adapter_community_before_linearlayout)
        val timeShiftButton: Button = itemView.findViewById(R.id.adapter_community_ts)
        val calendarButton: Button = itemView.findViewById(R.id.adapter_community_calendar)
        val shareButton: Button = itemView.findViewById(R.id.adapter_community_share)
        val autoTatimiButton: Button = itemView.findViewById(R.id.adapter_community_auto)
        val autoNicoLiveButton: Button = itemView.findViewById(R.id.adapter_community_auto_nicocas)
    }

    // 視聴モード選択ボタン初期化
    fun initWatchModeButton(itemHolder: CommunityRecyclerViewAdapter.ViewHolder, programData: ProgramData) {
        itemHolder.apply {
            val context = itemHolder.watchModeComeCas.context
            watchModeComeView.setOnClickListener {
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("liveId", programData.programId)
                intent.putExtra("watch_mode", "comment_viewer")
                intent.putExtra("isOfficial", programData.isOfficial)
                context?.startActivity(intent)
            }
            watchModeComePost.setOnClickListener {
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("liveId", programData.programId)
                intent.putExtra("watch_mode", "comment_post")
                intent.putExtra("isOfficial", programData.isOfficial)
                context?.startActivity(intent)
            }
            watchModeComeCas.setOnClickListener {
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("liveId", programData.programId)
                intent.putExtra("watch_mode", "nicocas")
                intent.putExtra("isOfficial", programData.isOfficial)
                context?.startActivity(intent)
            }
            watchModeDesc.setOnClickListener {
                //ダイアログ
                val bundle = Bundle()
                bundle.putString("liveId", programData.programId)
                val dialog = BottomSheetDialogWatchMode()
                dialog.arguments = bundle
                dialog.show((context as AppCompatActivity).supportFragmentManager, "watchmode")
            }
        }
    }

    private fun initTSButton(holder: ViewHolder, item: ProgramData) {
        val context = holder.calendarButton.context
        val reservationUtil = ReservationUtil(context)
        holder.apply {
            calendarButton.setOnClickListener {
                reservationUtil.addCalendar(context, item)
            }
            shareButton.setOnClickListener {
                reservationUtil.showShareScreen(context, it, item)
            }
            timeShiftButton.setOnClickListener {
                reservationUtil.registerTimeShift(context, item, it)
            }
            autoNicoLiveButton.setOnClickListener {
                // ニコ生アプリで開く
                reservationUtil.admissionDBUtil.addAutoAdmissionDB("nicolive_app", context, item, it)
            }
            autoTatimiButton.setOnClickListener {
                // たちみどろいどでひらく
                reservationUtil.admissionDBUtil.addAutoAdmissionDB("tatimidroid_app", context, item, it)
            }
        }
    }

}
