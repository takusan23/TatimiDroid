package io.github.takusan23.tatimidroid.NicoLive.Adapter

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import io.github.takusan23.tatimidroid.NicoLive.CommunityListFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Database.AutoAdmissionDB
import io.github.takusan23.tatimidroid.Room.Init.AutoAdmissionDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 予約枠自動入場の一覧表示で使うAdapter
 * */
class AutoAdmissionAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<AutoAdmissionAdapter.ViewHolder>() {

    // データベース
    lateinit var autoAdmissionDB: AutoAdmissionDB
    lateinit var communityListFragment: CommunityListFragment

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_auto_admission_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item: ArrayList<String> = arrayListArrayAdapter[position] as ArrayList<String>
        val content = holder.timeTextView.context

        val programName = item.get(1)
        val liveid = item.get(2)
        val time = parseTime(item.get(3))
        val app = item.get(4)

        holder.timeTextView.text = time
        holder.titleTextView.text = "$programName - $liveid (${getAppName(content, app)})"
        holder.deleteButton.setOnClickListener {

            //初期化したか
            if (!this@AutoAdmissionAdapter::autoAdmissionDB.isInitialized) {
                autoAdmissionDB = AutoAdmissionDBInit(content).commentCollectionDB
            }

            //削除
            Snackbar.make(holder.timeTextView, content.getText(R.string.delete_message), Snackbar.LENGTH_SHORT).setAction(content.getText(R.string.delete)) {
                // 削除
                GlobalScope.launch(Dispatchers.Main) {
                    withContext(Dispatchers.IO) {
                        autoAdmissionDB.autoAdmissionDBDAO().deleteById(liveid)
                    }
                    // 再読み込み
                    communityListFragment.getAutoAdmissionList()
                    // Service再起動
                    val intent = Intent(content, AutoAdmissionService::class.java)
                    content.stopService(intent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        content.startForegroundService(intent)
                    } else {
                        content.startService(intent)
                    }
                }
            }.show()

        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView
        var timeTextView: TextView
        var deleteButton: Button

        init {
            titleTextView = itemView.findViewById(R.id.adapter_auto_admission_program_textview)
            timeTextView = itemView.findViewById(R.id.adapter_auto_admission_time_textview)
            deleteButton = itemView.findViewById(R.id.adapter_auto_admission_delete_button)
        }
    }

    fun parseTime(string: String): String {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.JAPAN)
        return simpleDateFormat.format(string.toLong() * 1000)
    }

    fun getAppName(context: Context, name: String): String {
        if (name.contains("tatimidroid_app")) {
            return context.getString(R.string.app_name)
        } else {
            return context.getString(R.string.nicolive_app)
        }
    }

}