package io.github.takusan23.tatimidroid.Adapter

import android.database.sqlite.SQLiteDatabase
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
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import io.github.takusan23.tatimidroid.Tool.DataClass.NGData
import io.github.takusan23.tatimidroid.Tool.NGDataBaseTool
import java.util.ArrayList

/**
 * NG一覧表示RecyclerView。削除とかできるよ
 * */
class NGListRecyclerView(private val arrayListArrayAdapter: ArrayList<NGData>) : RecyclerView.Adapter<NGListRecyclerView.ViewHolder>() {

    // NGデータベース関連
    lateinit var ngDataBaseTool: NGDataBaseTool

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

        // 初期化する
        if (!::ngDataBaseTool.isInitialized) {
            ngDataBaseTool = (context as NGListActivity).ngDataBaseTool
        }

        //入れる
        val type = item.type
        val value = item.value
        holder.nameTextView.text = value
        //削除
        holder.deleteButton.setOnClickListener {
            //Snackbar
            Snackbar.make(holder.nameTextView, context.getText(R.string.delete_message), Snackbar.LENGTH_SHORT).setAction(context.getText(R.string.delete)) {
                //削除
                if (item.isComment) {
                    ngDataBaseTool.deleteNGComment(value)
                } else {
                    ngDataBaseTool.deleteNGUser(value)
                }
                //再読み込み
                if (context is NGListActivity) {
                    if (item.isComment) {
                        context.loadNGComment()
                    } else {
                        context.loadNGUser()
                    }
                }
                //消したメッセージ
                Toast.makeText(context, context.getText(R.string.delete_successful), Toast.LENGTH_SHORT).show()
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
