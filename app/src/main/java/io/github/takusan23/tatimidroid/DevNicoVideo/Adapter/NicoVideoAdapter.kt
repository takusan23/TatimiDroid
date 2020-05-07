package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.CustomFont
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動のコメント表示Adapter
 * */
class NicoVideoAdapter(private val arrayListArrayAdapter: ArrayList<CommentJSONParse>) :
    RecyclerView.Adapter<NicoVideoAdapter.ViewHolder>() {

    lateinit var font: CustomFont
    lateinit var devNicoVideoFragment: DevNicoVideoFragment
    lateinit var prefSetting: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val context = holder.commentTextView.context

        // しょっきかー
        if (!::font.isInitialized) {
            font = CustomFont(context)
            prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        }

        val item = arrayListArrayAdapter[position]
        val comment = item.comment
        val date = item.date
        val vpos = item.vpos
        val time = vpos.toFloat() / 100 //再生時間。100で割ればいいっぽい？
        // きれいな形へ
        val formattedTime = formatTime(time)
        val mail = item.mail

        holder.userNameTextView.maxLines = 1
        // たちみどろいど以外のキャッシュはCommentNoがないので？
        if (item.commentNo == "-1") {
            holder.commentTextView.text = "$comment"
        } else {
            holder.commentTextView.text = "${item.commentNo}：$comment"
        }

        // ニコるくん表示
        val isShowNicoruButton =
            devNicoVideoFragment.prefSetting.getBoolean("setting_nicovideo_nicoru_show", false)
        // mail（コマンド）がないときは表示しない
        val mailText = if (item.mail.isNotEmpty()) {
            "| $mail "
        } else {
            ""
        }
        // 一般会員にはニコる提供されてないのでニコる数だけ表示
        val nicoruCount =
            if (item.nicoru > 0 && !(devNicoVideoFragment.isPremium && isShowNicoruButton)) {
                "| ニコる ${item.nicoru} "
            } else {
                ""
            }

        // NGスコア表示するか
        val ngScore =
            if (prefSetting.getBoolean("setting_show_ng", false) && item.score.isNotEmpty()) {
                "| ${item.score} "
            } else {
                ""
            }

        holder.userNameTextView.text =
            "${setTimeFormat(date.toLong())} | $formattedTime $mailText$nicoruCount$ngScore| ${item.userId}"
        holder.nicoruButton.text = item.nicoru.toString()

        // ユーザーの設定したフォントサイズ
        font.apply {
            holder.commentTextView.textSize = commentFontSize
            holder.userNameTextView.textSize = userIdFontSize
        }
        // フォント
        font.apply {
            setTextViewFont(holder.commentTextView)
            setTextViewFont(holder.userNameTextView)
        }

        // プレ垢はニコるくんつける
        if (devNicoVideoFragment.isPremium && isShowNicoruButton) {
            holder.nicoruButton.visibility = View.VISIBLE
        }

        // ニコる押したとき
        holder.nicoruButton.setOnClickListener {
            GlobalScope.launch {
                devNicoVideoFragment.apply {
                    if (isPremium) {
                        val nicoruKey = nicoruAPI.nicoruKey
                        val responseNicoru =
                            nicoruAPI.postNicoru(userSession, threadId, userId, item.commentNo, item.comment, "${item.date}.${item.dateUsec}", nicoruKey)
                                .await()
                        if (!responseNicoru.isSuccessful) {
                            // 失敗時
                            showToast(context, "${context?.getString(R.string.error)}\n${responseNicoru.code}")
                            return@launch
                        }
                        val responseString = responseNicoru.body?.string()
                        // 成功したか
                        val jsonObject = JSONArray(responseString).getJSONObject(0)
                        val status = nicoruAPI.nicoruResultStatus(jsonObject)
                        if (status == 0) {
                            item.nicoru = nicoruAPI.nicoruResultNicoruCount(jsonObject)
                            showSnackbar("${getString(R.string.nicoru_ok)}：${item.nicoru}\n${item.comment}", null, null)
                            // ニコるボタンに再適用
                            holder.nicoruButton.post {
                                holder.nicoruButton.text = item.nicoru.toString()
                            }
                        } else {
                            showSnackbar(getString(R.string.nicoru_error), null, null)
                        }
                    } else {
                        showToast(context, "プレ垢限定です")
                    }
                }
            }
        }

    }

    /**
     * 時間表記をきれいにする関数
     * */
    private fun formatTime(time: Float): String {
        val minutes = time / 60
        val hour = (minutes / 60).toInt()
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "${hour}:${simpleDateFormat.format(time * 1000)}"
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var commentTextView: TextView
        var userNameTextView: TextView
        var nicoruButton: Button

        init {
            userNameTextView = itemView.findViewById(R.id.adapter_nicovideo_user_textview)
            commentTextView = itemView.findViewById(R.id.adapter_nicovideo_comment_textview)
            nicoruButton = itemView.findViewById(R.id.adapter_nicovideo_comment_nicoru)
        }
    }

    fun setTimeFormat(date: Long): String? {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        return simpleDateFormat.format(date * 1000)
    }

    fun showToast(context: Context, message: String) {
        (context as AppCompatActivity).runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
