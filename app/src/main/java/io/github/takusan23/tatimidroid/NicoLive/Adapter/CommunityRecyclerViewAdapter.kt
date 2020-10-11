package io.github.takusan23.tatimidroid.NicoLive.Adapter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.DialogWatchModeBottomFragment
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.ProgramMenuBottomSheet
import io.github.takusan23.tatimidroid.NicoLive.CommentFragment
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.isDarkMode
import java.text.SimpleDateFormat
import java.util.*

// 番組RecyclerViewAdapter
class CommunityRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<NicoLiveProgramData>) : RecyclerView.Adapter<CommunityRecyclerViewAdapter.ViewHolder>() {

    lateinit var prefSetting: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_community_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val content = holder.timeTextView.context
        prefSetting = PreferenceManager.getDefaultSharedPreferences(content)

        val item = arrayListArrayAdapter[position]
        val title = item.title
        val name = item.communityName
        val live_time = item.beginAt
        val liveId = item.programId
        val datetime = item.beginAt
        val liveNow = item.lifeCycle
        val thumb = item.thum
        val isOfficial = item.isOfficial // 公式ならtrue
        val isOnAir = liveNow.contains("ON_AIR") || liveNow.contains("Begun")

        //時間を文字列に
        val simpleDateFormat = SimpleDateFormat("MM/dd HH:mm:ss")
        val time = simpleDateFormat.format(live_time.toLong())

        holder.titleTextView.text = title
        holder.communityNameTextView.text = "[${name}]"

        if (isOnAir) {
            //放送中
            holder.timeTextView.text = time
            holder.timeTextView.setTextColor(Color.RED)
            // 視聴モード
            holder.watchModeLinearLayout.visibility = View.VISIBLE
        } else {
            //予約枠
            holder.timeTextView.text = time
            if (isDarkMode(content)) {
                holder.timeTextView.setTextColor(Color.parseColor("#ffffff"))
            } else {
                holder.timeTextView.setTextColor(-1979711488)   //デフォルトのTextViewのフォント色
            }
            // 視聴モード
            holder.watchModeLinearLayout.visibility = View.GONE
        }

        /*
        * 番組IDコピー機能
        * */
        holder.communityCard.setOnLongClickListener {
            Toast.makeText(content, "${content.getString(R.string.copy_program_id)} : $liveId", Toast.LENGTH_SHORT)
                .show()
            val clipboardManager =
                content.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboardManager.setPrimaryClip(ClipData.newPlainText("liveid", liveId))
            true
        }

        // サムネ
        holder.thumbImageView.imageTintList = null
        Glide.with(holder.thumbImageView)
            .load(thumb)
            .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
            .into(holder.thumbImageView)


        // 視聴モードボタン
        initWatchModeButton(holder, item)
        // TS予約などのボタン
        holder.liveMenuIconImageView.setOnClickListener {
            val programMenuBottomSheet = ProgramMenuBottomSheet()
            val bundle = Bundle()
            bundle.putString("liveId", liveId)
            programMenuBottomSheet.arguments = bundle
            programMenuBottomSheet.show((it.context as AppCompatActivity).supportFragmentManager, "menu")
        }

    }

    fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var titleTextView: TextView = itemView.findViewById(R.id.adapter_community_title_textview)
        var timeTextView: TextView = itemView.findViewById(R.id.adapter_community_time_textview)
        var communityCard: CardView = itemView.findViewById(R.id.adapter_community_card)

        // 視聴モード
        val watchModeLinearLayout: LinearLayout = itemView.findViewById(R.id.adapter_community_watchmode_linearlayout)
        val watchModeComeView: Button = itemView.findViewById(R.id.adapter_community_watchmode_comeview)
        val watchModeComePost: Button = itemView.findViewById(R.id.adapter_community_watchmode_comepost)
        val watchModeComeCas: Button = itemView.findViewById(R.id.adapter_community_watchmode_nicocas)
        val watchModeDesc: Button = itemView.findViewById(R.id.adapter_community_watchmode_description)
        val thumbImageView: ImageView = itemView.findViewById(R.id.adapter_community_program_thumb)
        val liveMenuIconImageView: ImageView = itemView.findViewById(R.id.adapter_community_menu_icon)
        val communityNameTextView: TextView = itemView.findViewById(R.id.adapter_community_community_name_textview)
    }

    // 視聴モード選択ボタン初期化
    private fun initWatchModeButton(itemHolder: ViewHolder, nicoLiveProgramData: NicoLiveProgramData) {
        itemHolder.apply {
            val context = itemHolder.watchModeComeCas.context
            val mainActivity = context as MainActivity
            watchModeComeView.setOnClickListener {
                val commentFragment = CommentFragment()
                // LiveID詰める
                val bundle = Bundle()
                bundle.putString("liveId", nicoLiveProgramData.programId)
                bundle.putString("watch_mode", "comment_viewer")
                bundle.putBoolean("isOfficial", nicoLiveProgramData.isOfficial)
                commentFragment.arguments = bundle
                mainActivity.setPlayer(commentFragment, nicoLiveProgramData.programId)
            }
            watchModeComePost.setOnClickListener {
                val commentFragment = CommentFragment()
                // LiveID詰める
                val bundle = Bundle()
                bundle.putString("liveId", nicoLiveProgramData.programId)
                bundle.putString("watch_mode", "comment_post")
                bundle.putBoolean("isOfficial", nicoLiveProgramData.isOfficial)
                commentFragment.arguments = bundle
                mainActivity.setPlayer(commentFragment, nicoLiveProgramData.programId)
            }
            watchModeComeCas.setOnClickListener {
                val commentFragment = CommentFragment()
                // LiveID詰める
                val bundle = Bundle()
                bundle.putString("liveId", nicoLiveProgramData.programId)
                bundle.putString("watch_mode", "nicocas")
                bundle.putBoolean("isOfficial", nicoLiveProgramData.isOfficial)
                commentFragment.arguments = bundle
                mainActivity.setPlayer(commentFragment, nicoLiveProgramData.programId)
            }
            watchModeDesc.setOnClickListener {
                //ダイアログ
                val bundle = Bundle()
                bundle.putString("liveId", nicoLiveProgramData.programId)
                val dialog = DialogWatchModeBottomFragment()
                dialog.arguments = bundle
                dialog.show((context as AppCompatActivity).supportFragmentManager, "watchmode")
            }
        }
    }

}
