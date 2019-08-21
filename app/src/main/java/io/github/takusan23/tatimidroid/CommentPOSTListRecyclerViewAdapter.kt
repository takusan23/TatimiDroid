package io.github.takusan23.tatimidroid

import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Activity.CommentPOSTList
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentPOSTListSQLiteHelper
import java.util.ArrayList

class CommentPOSTListRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<CommentPOSTListRecyclerViewAdapter.ViewHolder>() {

    lateinit var commentPOSTList: CommentPOSTListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_comment_post_list_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return arrayListArrayAdapter.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = arrayListArrayAdapter[position] as ArrayList<String>
        val title = item.get(1)

        if (!this@CommentPOSTListRecyclerViewAdapter::commentPOSTList.isInitialized) {
            //データベース
            commentPOSTList = CommentPOSTListSQLiteHelper(holder.textview.context)
            sqLiteDatabase = commentPOSTList.writableDatabase
            commentPOSTList.setWriteAheadLoggingEnabled(false)
        }

        holder.textview.text = title
        val context = holder.textview.context
        holder.deletebutton.setOnClickListener {
            //Snackbar
            Snackbar.make(
                holder.textview,
                context.getText(R.string.delete_message),
                Snackbar.LENGTH_SHORT
            )
                .setAction(context.getText(R.string.delete)) {
                    //削除
                    sqLiteDatabase.delete(
                        "comment_post_list",
                        "comment=?",
                        arrayOf(holder.textview.text.toString())
                    )
                    //再読み込み
                    if (context is CommentPOSTList) {
                        context.loadList()
                    }
                    //消したメッセージ
                    Snackbar.make(
                        holder.textview,
                        context.getText(R.string.delete_successful),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }.show()
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textview: TextView
        var deletebutton: ImageButton

        init {
            textview = itemView.findViewById(R.id.adapter_comment_post_list_textview)
            deletebutton = itemView.findViewById(R.id.adapter_comment_post_list_delete_button)
        }
    }
}
