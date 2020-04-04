package io.github.takusan23.tatimidroid.NicoVideo.Adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.R.string.no
import android.os.Message
import androidx.appcompat.app.AppCompatActivity


class NicoVideoAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<String>>) :
    RecyclerView.Adapter<NicoVideoAdapter.ViewHolder>() {

    //ニコるようThreadId
    var threadId = ""
    var user_session = ""
    var isPremium = false
    var userId = ""

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

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val name = item.get(1)
        val comment = item.get(2)
        val date = item.get(3)
        val vpos = item.get(4)
        val time = vpos.toFloat() / 100 //再生時間。100で割ればいいっぽい？
        // きれいな形へ
        val formattedTime = formatTime(time)
        val mail = item.get(5)
        var nicoruCount = item.get(6)
        val no = item.get(7)

        if (nicoruCount.isNotEmpty()) {
            nicoruCount = "${context.getString(R.string.nicoru)} $nicoruCount"
        }

        holder.commentTextView.text = comment
        holder.userNameTextView.text =
            "${setTimeFormat(date.toLong())} | $formattedTime | $mail | $nicoruCount"

        // JSONぱーす


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

    fun getNicoruKey(context: Context): Deferred<String> = GlobalScope.async {
        //ニコるための nicorukey を取得する
        val url = "https://nvapi.nicovideo.jp/v1/nicorukey?language=0&threadId=$threadId"
        println(url)
        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", "user_session=$user_session")
            .addHeader("User-Agent", "TatimiDroid;@takusan_23")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        println(response.body?.string())
        if (response.isSuccessful) {
            val responseString = response.body?.string()
            val jsonObject = JSONObject(responseString)
            val nicoruKey = jsonObject.getJSONObject("data").getString("nicorukey")
            return@async nicoruKey
        } else {
            showToast(context, "${context.getString(R.string.error)}\n${response.code}")
            return@async ""
        }
    }

    fun postNicoru(context: Context, nicoruKey: String, commentNo: String, commentString: String) {
        val url = "https://nmsg.nicovideo.jp/api.json/"

        var premium = 0
        if (isPremium) {
            premium = 1
        }

        //POSTするJSON
        val postJSON = JSONObject()
        val nicoru = JSONObject().apply {
            put("thread", threadId)
            put("user_id", userId)
            put("premium", premium)
            put("fork", 0)
            put("language", 0)
            put("id", commentNo)
            put("comment", commentString)
            put("nicorukey", nicoruKey)
        }
        postJSON.put("nicoru", nicoru)

        val request = Request.Builder()
            .url(url)
            .addHeader("Cookie", "user_session=$user_session")
            .addHeader("User-Agent", "TatimiDroid;@takusan_23")
            .post(postJSON.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(context, context.getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println(response.body?.string())
                } else {
                    showToast(context, "${context.getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    fun showToast(context: Context, message: String) {
        (context as AppCompatActivity).runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

}
