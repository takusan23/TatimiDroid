package io.github.takusan23.tatimidroid.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NGDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NGDBInit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

/**
 * NG一覧表示RecyclerView。削除とかできるよ
 * */
class NGListRecyclerView(private val arrayListArrayAdapter: ArrayList<NGDBEntity>) : RecyclerView.Adapter<NGListRecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_ng_list_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position]
        val context = holder.nameTextView.context

        // 入れる
        val type = item.type
        val value = item.value
        holder.nameTextView.text = value
        // 削除
        holder.deleteButton.setOnClickListener {
            // Snackbar
            Snackbar.make(holder.nameTextView, context.getText(R.string.delete_message), Snackbar.LENGTH_SHORT).setAction(context.getText(R.string.delete)) {
                GlobalScope.launch(Dispatchers.Main) {
                    //削除
                    withContext(Dispatchers.IO) {
                        NGDBInit.getInstance(context).ngDBDAO().deleteByValue(value)
                    }
                    //再読み込み
                    if (context is NGListActivity) {
                        if (type == "comment") {
                            context.loadNGComment()
                        } else {
                            context.loadNGUser()
                        }
                    }
                    //消したメッセージ
                    Toast.makeText(context, context.getText(R.string.delete_successful), Toast.LENGTH_SHORT).show()
                }
            }.show()
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var nameTextView: TextView
        var deleteButton: Button

        init {
            deleteButton = itemView.findViewById(R.id.adapter_ng_list_delete_button)
            nameTextView = itemView.findViewById(R.id.adapter_ng_list_text)
        }
    }
}
