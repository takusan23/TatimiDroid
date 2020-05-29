package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.Tool.CustomFont
import io.github.takusan23.tatimidroid.DevNicoVideo.DevNicoVideoFragment
import io.github.takusan23.tatimidroid.Fragment.CommentLockonBottomFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * ニコ動のコメント表示Adapter
 * */
class NicoVideoAdapter(private val arrayListArrayAdapter: ArrayList<CommentJSONParse>) : RecyclerView.Adapter<NicoVideoAdapter.ViewHolder>() {

    lateinit var font: CustomFont
    lateinit var devNicoVideoFragment: DevNicoVideoFragment
    lateinit var prefSetting: SharedPreferences

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo, parent, false)
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

        // プレ垢
        val isPremium = devNicoVideoFragment.nicoVideoHTML.isPremium(devNicoVideoFragment.jsonObject)

        // オフライン再生かどうか
        val isOfflinePlay = devNicoVideoFragment.isCache

        // 動画でコテハン？いる
        val kotehanOrUserId = devNicoVideoFragment.kotehanMap[item.userId] ?: item.userId

        // ニコるくん表示
        val isShowNicoruButton = prefSetting.getBoolean("setting_nicovideo_nicoru_show", false)
        // mail（コマンド）がないときは表示しない
        val mailText = if (item.mail.isNotEmpty()) {
            "| $mail "
        } else {
            ""
        }

        // NGスコア表示するか
        val ngScore = if (prefSetting.getBoolean("setting_show_ng", false) && item.score.isNotEmpty()) {
            "| ${item.score} "
        } else {
            ""
        }

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
        if (isInitDevNicoVideoFragment() && isPremium && isShowNicoruButton && !isOfflinePlay) {
            holder.nicoruButton.visibility = View.VISIBLE
        }

        // 一般会員にはニコる提供されてないのでニコる数だけ表示
        // あとDevNicoVideoFragmentはがめんスワイプしてたらなんか落ちたので
        val nicoruCount = if (holder.nicoruButton.visibility == View.GONE && item.nicoru > 0) {
            "| ニコる ${item.nicoru} "
        } else {
            ""
        }

        holder.userNameTextView.text = "${setTimeFormat(date.toLong())} | $formattedTime $mailText$nicoruCount$ngScore| ${kotehanOrUserId}"
        holder.nicoruButton.text = item.nicoru.toString()

        // ロックオン芸（詳細画面表示）
        holder.cardView.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("comment", item.comment)
            bundle.putString("user_id", item.userId)
            bundle.putString("liveId", item.videoOrLiveId)
            bundle.putString("label", holder.userNameTextView.text.toString())
            val commentLockonBottomFragment = CommentLockonBottomFragment()
            commentLockonBottomFragment.arguments = bundle
            if (context is AppCompatActivity) {
                commentLockonBottomFragment.show(context.supportFragmentManager, "comment_menu")
            }
        }


        // ニコる押したとき
        holder.nicoruButton.setOnClickListener {
            GlobalScope.launch {
                devNicoVideoFragment.apply {
                    if (isPremium) {
                        val nicoruKey = nicoruAPI.nicoruKey
                        val responseNicoru = nicoruAPI.postNicoru(userSession, threadId, userId, item.commentNo, item.comment, "${item.date}.${item.dateUsec}", nicoruKey).await()
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
                            val nicoruId = nicoruAPI.nicoruResultId(jsonObject)
                            showSnackbar("${getString(R.string.nicoru_ok)}：${item.nicoru}\n${item.comment}", getString(R.string.nicoru_delete)) {
                                // 取り消しAPI叩く
                                GlobalScope.launch {
                                    val deleteResponse = nicoruAPI.deleteNicoru(userSession, nicoruId).await()
                                    if (deleteResponse.isSuccessful) {
                                        this@NicoVideoAdapter.showToast(context, getString(R.string.nicoru_delete_ok))
                                    } else {
                                        this@NicoVideoAdapter.showToast(context, "${getString(R.string.error)}${deleteResponse.code}")
                                    }
                                }
                            }
                            // ニコるボタンに再適用
                            holder.nicoruButton.post {
                                holder.nicoruButton.text = item.nicoru.toString()
                            }
                        } else {
                            this@NicoVideoAdapter.showToast(context, getString(R.string.nicoru_error))
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
        var commentTextView: TextView = itemView.findViewById(R.id.adapter_nicovideo_comment_textview)
        var userNameTextView: TextView = itemView.findViewById(R.id.adapter_nicovideo_user_textview)
        var nicoruButton: Button = itemView.findViewById(R.id.adapter_nicovideo_comment_nicoru)
        var cardView: CardView = itemView.findViewById(R.id.adapter_nicovideo_comment_cardview)
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

    // DevNicoVideoFragmentが初期化済みか
    fun isInitDevNicoVideoFragment(): Boolean = ::devNicoVideoFragment.isInitialized
}
