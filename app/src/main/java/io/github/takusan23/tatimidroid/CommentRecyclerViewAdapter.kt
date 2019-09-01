package io.github.takusan23.tatimidroid

import android.annotation.TargetApi
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Fragment.CommentMenuBottomFragment
import org.w3c.dom.Comment
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

class CommentRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<CommentRecyclerViewAdapter.ViewHolder>() {

    //UserIDの配列。初コメを太字表示する
    val userList = arrayListOf<String>()
    lateinit var pref_setting: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_comment_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: ArrayList<String> = arrayListArrayAdapter[position] as ArrayList<String>
        val context = holder.cardView.context
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        //NG配列
        val userNGList = (context as CommentActivity).userNGList
        val commentNGList = (context as CommentActivity).commentNGList
        //コテハンMAP
        val kotehanMap = (context as CommentActivity).kotehanMap
        //NGチェック
        var isNGUser = false
        var isNGComment = false

        val json: String = item[1]
        val roomName: String = item[2]

        val commentJSONParse = CommentJSONParse(json, roomName)

        //NGコメント、ユーザーか
        if (userNGList.indexOf(commentJSONParse.userId) != -1) {
            isNGUser = true
        }
        if (commentNGList.indexOf(commentJSONParse.comment) != -1) {
            isNGComment = true
        }


        //UnixTime -> Minute
        var time = ""
        if (commentJSONParse.date.isNotEmpty()) {
            val calendar = Calendar.getInstance(TimeZone.getDefault())
            calendar.timeInMillis = commentJSONParse.date.toLong() * 1000L
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)
            time =
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(
                    Calendar.SECOND
                )}"
        }

        //コテハン
        var userId: String
        if (kotehanMap.containsKey(commentJSONParse.userId)) {
            userId = kotehanMap.get(commentJSONParse.userId) ?: ""
        } else {
            userId = commentJSONParse.userId
        }


        var info = "${commentJSONParse.roomName} | $time | ${userId}"
        var comment = "${commentJSONParse.commentNo} : ${commentJSONParse.comment}"

        //プレ垢
        if (commentJSONParse.premium.isNotEmpty()) {
            info = "$info | ${commentJSONParse.premium}"
        }

        //NGの場合は置き換える
        if (isNGUser) {
            info = ""
            comment = context.getString(R.string.ng_user)
        }
        if (isNGComment) {
            info = ""
            comment = context.getString(R.string.ng_comment)
        }

        //UserIDの配列になければ配列に入れる。初コメ
        if (userList.indexOf(commentJSONParse.userId) == -1) {
            userList.add(commentJSONParse.userId)
            //初コメは太字にする
            holder.commentTextView.typeface = Typeface.DEFAULT_BOLD
        } else {
            holder.commentTextView.typeface = Typeface.DEFAULT
        }

        holder.commentTextView.text = comment
        holder.roomNameTextView.text = info


        //詳細画面出す
        holder.cardView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("comment", commentJSONParse.comment)
            bundle.putString("user_id", commentJSONParse.userId)
            val commentMenuBottomFragment = CommentMenuBottomFragment()
            commentMenuBottomFragment.arguments = bundle
            commentMenuBottomFragment.show(
                (context as AppCompatActivity).supportFragmentManager,
                "comment_menu"
            )
        }

        //部屋の色
        if (pref_setting.getBoolean("setting_room_color", true)) {
            holder.roomNameTextView.setTextColor(getRoomColor(roomName))
        }

        //ID非表示
        if (pref_setting.getBoolean("setting_id_hidden", false)) {
            //非表示
            holder.roomNameTextView.visibility = View.GONE
            //部屋の色をつける設定有効時はコメントのTextViewに色を付ける
            if (pref_setting.getBoolean("setting_room_color", true)) {
                holder.commentTextView.setTextColor(getRoomColor(roomName))
            }
        }
        //一行表示とか
        if (pref_setting.getBoolean("setting_one_line", false)) {
            holder.commentTextView.maxLines = 1
        }

    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var commentTextView: TextView
        var roomNameTextView: TextView
        var cardView: CardView

        init {
            commentTextView = itemView.findViewById(R.id.adapter_comment_textview)
            roomNameTextView = itemView.findViewById(R.id.adapter_room_name_textview)
            cardView = itemView.findViewById(R.id.adapter_room_name_cardview)
        }
    }

    //コメビュの部屋の色。NCVに追従する
    fun getRoomColor(room: String): Int {
        when (room) {
            "アリーナ" -> {
                return Color.argb(255, 0, 153, 229)
            }
            "立ち見1" -> {
                return Color.argb(255, 234, 90, 61)
            }
            "立ち見2" -> {
                return Color.argb(255, 172, 209, 94)
            }
            "立ち見3" -> {
                return Color.argb(255, 0, 217, 181)
            }
            "立ち見4" -> {
                return Color.argb(255, 229, 191, 0)
            }
            "立ち見5" -> {
                return Color.argb(255, 235, 103, 169)
            }
            "立ち見6" -> {
                return Color.argb(255, 181, 89, 217)
            }
            "立ち見7" -> {
                return Color.argb(255, 20, 109, 199)
            }
            "立ち見8" -> {
                return Color.argb(255, 226, 64, 33)
            }
            "立ち見9" -> {
                return Color.argb(255, 142, 193, 51)
            }
            "立ち見10" -> {
                return Color.argb(255, 0, 189, 120)
            }
        }
        return Color.argb(255, 0, 0, 0)
    }

}