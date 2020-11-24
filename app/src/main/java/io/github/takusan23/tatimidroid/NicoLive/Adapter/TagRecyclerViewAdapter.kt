package io.github.takusan23.tatimidroid.NicoLive.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveTagDataClass
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveTagAPI
import io.github.takusan23.tatimidroid.NicoLive.BottomFragment.NicoLiveTagBottomFragment
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

/** タグ編集で使う一覧表示Adapter */
class TagRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<NicoLiveTagDataClass>) : RecyclerView.Adapter<TagRecyclerViewAdapter.ViewHolder>() {

    lateinit var bottomFragment: NicoLiveTagBottomFragment
    var liveId = ""
    var tagToken = ""
    var user_session = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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

        val tagName = item.tagName
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
            val tagAPI = NicoLiveTagAPI()
            GlobalScope.launch(Dispatchers.Main) {
                val response = tagAPI.deleteTag(liveId, user_session, tagName)
                if (response.isSuccessful) {
                    Toast.makeText(context, "削除しました。$tagName", Toast.LENGTH_SHORT).show()
                    bottomFragment.apply {
                        // 再読み込み
                        tagCoroutine()
                        programFragment.coroutineGetTag()
                    }
                } else {
                    Toast.makeText(context, "${context?.getString(R.string.error)}\n${response.code}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView = itemView.findViewById<TextView>(R.id.adapter_tag_textview)
        var deleteButton = itemView.findViewById<ImageButton>(R.id.adapter_tag_remove_button)
        var lockedButton = itemView.findViewById<ImageButton>(R.id.adapter_tag_is_locked)
    }

}