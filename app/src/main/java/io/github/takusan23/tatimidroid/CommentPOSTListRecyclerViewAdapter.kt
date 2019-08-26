package io.github.takusan23.tatimidroid

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.Activity.CommentActivity
import io.github.takusan23.tatimidroid.Activity.CommentCollectionListActivity
import io.github.takusan23.tatimidroid.Activity.NGListActivity
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentCollectionSQLiteHelper
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentPOSTListSQLiteHelper
import kotlinx.android.synthetic.main.activity_comment_postlist.*
import java.util.ArrayList

class CommentPOSTListRecyclerViewAdapter(private val arrayListArrayAdapter: ArrayList<ArrayList<*>>) :
    RecyclerView.Adapter<CommentPOSTListRecyclerViewAdapter.ViewHolder>() {

    lateinit var commentCollectionSQLiteHelper: CommentCollectionSQLiteHelper
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
        val yomi = item.get(2)

        val context = holder.textview.context
        val commentActivity = context as CommentCollectionListActivity

        if (!this@CommentPOSTListRecyclerViewAdapter::commentCollectionSQLiteHelper.isInitialized) {
            //データベース
            commentCollectionSQLiteHelper = CommentCollectionSQLiteHelper(context)
            sqLiteDatabase = commentCollectionSQLiteHelper.writableDatabase
            commentCollectionSQLiteHelper.setWriteAheadLoggingEnabled(false)
        }

        //入れる
        holder.textview.text = title
        holder.yomitextview.text = yomi

        //削除
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
                        "comment_collection_db",
                        "comment=?",
                        arrayOf(title)
                    )
                    //再読み込み
                    context.loadList()

                    //消したメッセージ
                    Snackbar.make(
                        holder.textview,
                        context.getText(R.string.delete_successful),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }.show()
        }

        //編集
        holder.editbutton.setOnClickListener {
            //ActivityのEditTextに入れる
            commentActivity.activity_comment_post_list_comment_inputedittext.setText(title)
            commentActivity.activity_comment_post_list_yomi_inputedittext.setText(yomi)
        }

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var textview: TextView
        var yomitextview: TextView
        var deletebutton: ImageButton
        var editbutton: ImageButton

        init {
            textview = itemView.findViewById(R.id.adapter_comment_post_list_textview)
            yomitextview = itemView.findViewById(R.id.adapter_comment_post_list_yomi_textview)
            deletebutton = itemView.findViewById(R.id.adapter_comment_post_list_delete_button)
            editbutton = itemView.findViewById(R.id.adapter_comment_post_list_edit_button)
        }
    }
}
