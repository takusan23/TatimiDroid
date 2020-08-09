package io.github.takusan23.tatimidroid.NicoVideo.Adapter

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
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoAddMylistBottomFragment
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.*
import java.util.ArrayList

/**
 * マイリスト追加BottomFragmentで使ってるRecyclerViewで使うAdapter
 * */
class NicoVideoMylistAdapter(val mylistList: ArrayList<Pair<String, String>>) : RecyclerView.Adapter<NicoVideoMylistAdapter.ViewHolder>() {

    // スマホ版マイリストAPI
    private val spMyListAPI = NicoVideoSPMyListAPI()

    // 動画ID
    var id = ""

    lateinit var prefSetting: SharedPreferences
    lateinit var mylistBottomFragment: NicoVideoAddMylistBottomFragment

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val linearLayout = itemView.findViewById<LinearLayout>(R.id.adapter_nicovideo_mylist_parent)
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_mylist_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo_mylist, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = mylistList.size

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
                val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                    showToast(context, "${context.getString(R.string.error)}\n${throwable}")
                }
                GlobalScope.launch(errorHandler) {
                    // 登録終わるまで閉じれないようにする。
                    withContext(Dispatchers.Main) {
                        mylistBottomFragment.isCancelable = false
                    }
                    // マイリスト追加APIを叩く
                    val addResponse = spMyListAPI.addMylistVideo(userSession, mylistId, id)
                    when (addResponse.code) {
                        201 -> {
                            // マイリスト追加成功
                            showToast(context, context.getString(R.string.mylist_add_ok))
                        }
                        200 -> {
                            // すでに追加済み
                            showToast(context, context.getString(R.string.mylist_added))
                        }
                        else -> {
                            // エラー
                            showToast(context, "${context.getString(R.string.error)}\n${addResponse.code}")
                        }
                    }
                    // 閉じる
                    withContext(Dispatchers.Main) {
                        mylistBottomFragment.dismiss()
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