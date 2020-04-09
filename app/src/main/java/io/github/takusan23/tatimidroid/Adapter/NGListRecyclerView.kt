package io.github.takusan23.tatimidroid.Adapter

import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import java.util.ArrayList

class NGListRecyclerView(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<NGListRecyclerView.ViewHolder>() {

    //NGデータベース
    lateinit var ngListSQLiteHelper: NGListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_ng_list_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val context = holder.nameTextView.context

        //入れる
        val type = item.get(1)
        val value = item.get(2)
        holder.nameTextView.text = value
        //削除
        holder.deleteButton.setOnClickListener {
            if (!this@NGListRecyclerView::ngListSQLiteHelper.isInitialized) {
                //データベース
                ngListSQLiteHelper = NGListSQLiteHelper(context!!)
                sqLiteDatabase = ngListSQLiteHelper.writableDatabase
                ngListSQLiteHelper.setWriteAheadLoggingEnabled(false)
            }

            //Snackbar
            Snackbar.make(holder.nameTextView, context.getText(R.string.delete_message), Snackbar.LENGTH_SHORT)
                .setAction(context.getText(R.string.delete)) {
                    //削除
                    sqLiteDatabase.delete("ng_list", "value=?", arrayOf(value))
                    //再読み込み
                    if (context is NGListActivity) {
                        if (type.contains("user")) {
                            context.loadNGUser()
                        } else {
                            context.loadNGComment()
                        }
                    }
                    //消したメッセージ
                    Snackbar.make(
                        holder.nameTextView,
                        context.getText(R.string.delete_successful),
                        Snackbar.LENGTH_SHORT
                    ).show()
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
