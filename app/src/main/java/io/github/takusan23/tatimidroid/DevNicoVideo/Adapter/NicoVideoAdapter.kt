package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.CustomFont
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoruAPI
import kotlinx.coroutines.launch
import org.json.JSONArray

/**
 * ニコ動のコメント表示Adapter
 * */
class NicoVideoAdapter(private val arrayListArrayAdapter: ArrayList<CommentJSONParse>) :
    RecyclerView.Adapter<NicoVideoAdapter.ViewHolder>() {

    lateinit var font: CustomFont
    lateinit var devNicoVideoFragment: DevNicoVideoFragment

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

        if (!::font.isInitialized) {
            font = CustomFont(context)
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

        // mail（コマンド）がないときは表示しない
        val mailText = if (item.mail.isNotEmpty()) {
            "| $mail "
        } else {
            ""
        }
        holder.userNameTextView.text =
            "${setTimeFormat(date.toLong())} | $formattedTime $mailText| ${item.userId}"

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
        if (devNicoVideoFragment.isPremium) {
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
