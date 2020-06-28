package io.github.takusan23.tatimidroid.DevNicoVideo.Adapter

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.DevNicoVideo.BottomFragment.DevNicoVideoAddMylistBottomFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.ArrayList

class DevNicoVideoMylistAdapter(val mylistList: ArrayList<Pair<String, String>>) : RecyclerView.Adapter<DevNicoVideoMylistAdapter.ViewHolder>() {

    // マイリスAPI
    val myListAPI = NicoVideoMyListAPI()
    val nicoVideoHTML = NicoVideoHTML()

    // 動画ID
    var id = ""

    lateinit var prefSetting: SharedPreferences
    lateinit var mylistBottomFragment: DevNicoVideoAddMylistBottomFragment

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val linearLayout = itemView.findViewById<LinearLayout>(R.id.adapter_nicovideo_mylist_parent)
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_mylist_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo_mylist, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mylistList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            val context = linearLayout.context

            prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
            val userSession = prefSetting.getString("user_session", "") ?: ""

            val title = mylistList[position].first
            val mylistId = mylistList[position].second

            titleTextView.text = title

            // マイリスト追加
            linearLayout.setOnClickListener {
                GlobalScope.launch(Dispatchers.IO) {
                    // 登録終わるまで閉じれないようにする。
                    withContext(Dispatchers.Main) {
                        mylistBottomFragment.isCancelable = false
                    }
                    // マイリストToken取得
                    val mylistHTMLResponse = myListAPI.getMyListHTML(userSession).await()
                    val nicoVideoResponse = nicoVideoHTML.getHTML(id, userSession).await()
                    if (mylistHTMLResponse.isSuccessful && nicoVideoResponse.isSuccessful) {
                        // ThreadId
                        val htmlJSON = nicoVideoHTML.parseJSON(nicoVideoResponse.body?.string())
                        val threadId = htmlJSON.getJSONObject("thread").getJSONObject("ids").getString("default")
                        // マイリストToken
                        val token = myListAPI.getToken(mylistHTMLResponse.body?.string()) ?: ""
                        // マイリスト追加API叩く
                        val mylistResponse = myListAPI.mylistAddVideo(mylistId, threadId, "", token, userSession).await()
                        if (mylistResponse.isSuccessful) {
                            // 成功したかどうか
                            val mylistResponseJSON = JSONObject(mylistResponse.body?.string())
                            // 閉じれるようにする
                            withContext(Dispatchers.Main) {
                                withContext(Dispatchers.Main) {
                                    mylistBottomFragment.isCancelable = true
                                }
                                if (mylistResponseJSON.has("status") && mylistResponseJSON.getString("status") == "ok") {
                                    // 成功時
                                    showToast(context, context.getString(R.string.mylist_add_ok))
                                    withContext(Dispatchers.Main) {
                                        mylistBottomFragment.dismiss()
                                    }
                                } else {
                                    // 失敗時。すでに登録済みなど
                                    val error = mylistResponseJSON.getString("error")
                                    showToast(context, "${context.getString(R.string.error)}\n${error}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Toast表示
    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}