package io.github.takusan23.tatimidroid.NicoLive.Adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveTagBottomFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTagAPI
import io.github.takusan23.tatimidroid.R
import okhttp3.*
import java.io.IOException
import java.util.*

class TagRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<NicoLiveTagAPI.NicoLiveTagItemData>) :
    RecyclerView.Adapter<TagRecyclerViewAdapter.ViewHolder>() {

    lateinit var bottomFragment: NicoLiveTagBottomFragment
    var liveId = ""
    var tagToken = ""
    var user_session = ""

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_tag_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position]
        val context = holder.textView.context

        val tagName = item.title
        val isLocked = item.isLocked

        holder.textView.text = tagName

        //　削除できない場合
        if (isLocked) {
            holder.lockedButton.visibility = View.VISIBLE
            holder.deleteButton.visibility = View.GONE
        } else {
            holder.lockedButton.visibility = View.GONE
            holder.deleteButton.visibility = View.VISIBLE
        }

        // 削除ボタン
        holder.deleteButton.setOnClickListener {
            deleteTag(context, tagName) {
                bottomFragment.apply {
                    // 再読み込み
                    tagCoroutine()
                    programFragment.coroutineGetTag()
                }
            }
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textView = itemView.findViewById<TextView>(R.id.adapter_tag_textview)
        var deleteButton = itemView.findViewById<ImageButton>(R.id.adapter_tag_remove_button)
        var lockedButton = itemView.findViewById<ImageButton>(R.id.adapter_tag_is_locked)

    }

    /**
     * タグを削除する関数。
     * @param context Context
     * @param tagName タグの名前。
     * @param success 成功したら
     * */
    fun deleteTag(context: Context?, tagName: String, success: () -> Unit) {
        val sendData = FormBody.Builder().apply {
            add("tag", tagName)
            add("token", tagToken)
        }.build()
        val request = Request.Builder().apply {
            url("https://papi.live.nicovideo.jp/api/relive/livetag/$liveId/?_method=DELETE")
            header("Cookie", "user_session=$user_session")
            header("User-Agent", "TatimiDroid;@takusan_23")
            post(sendData)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, context?.getString(R.string.error), Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "削除しました。$tagName", Toast.LENGTH_SHORT).show()
                        success()
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            "${context?.getString(R.string.error)}\n${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }


}