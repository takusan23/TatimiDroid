package io.github.takusan23.tatimidroid.Activity

import android.content.ContentResolver
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentPOSTListRecyclerViewAdapter
import io.github.takusan23.tatimidroid.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentPOSTListSQLiteHelper
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import kotlinx.android.synthetic.main.activity_comment_postlist.*
import kotlinx.android.synthetic.main.fragment_commnunity_list_layout.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*

class CommentPOSTList : AppCompatActivity() {

    lateinit var commentPOSTList: CommentPOSTListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var commentPOSTListRecyclerViewAdapter: CommentPOSTListRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment_postlist)

        supportActionBar?.title = getString(R.string.comment_post_list)

        recyclerViewList = ArrayList()
        activity_comment_post_list_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        activity_comment_post_list_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        commentPOSTListRecyclerViewAdapter = CommentPOSTListRecyclerViewAdapter(recyclerViewList)
        activity_comment_post_list_recyclerview.adapter = commentPOSTListRecyclerViewAdapter
        recyclerViewLayoutManager = activity_comment_post_list_recyclerview.layoutManager!!

        //データベース
        commentPOSTList = CommentPOSTListSQLiteHelper(this)
        sqLiteDatabase = commentPOSTList.writableDatabase
        commentPOSTList.setWriteAheadLoggingEnabled(false)

        //登録
        activity_comment_post_list_add.setOnClickListener {
            val contentValues = ContentValues()
            contentValues.put("comment", activity_comment_post_list_inputedittext.text.toString())
            contentValues.put("description", "")
            sqLiteDatabase.insert("comment_post_list", null, contentValues)
            //読み込み
            loadList()
        }

        //読み込み
        loadList()
    }

    fun loadList() {
        recyclerViewList.clear()
        val cursor = sqLiteDatabase.query(
            "comment_post_list",
            arrayOf("comment", "description"),
            null, null, null, null, null
        )
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {
            val comment = cursor.getString(0)
            //RecyclerView追加
            val item = arrayListOf<String>()
            item.add("")
            item.add(comment)
            recyclerViewList.add(item)
            cursor.moveToNext()
        }
        cursor.close()
        //リスト更新
        runOnUiThread {
            commentPOSTListRecyclerViewAdapter.notifyDataSetChanged()
        }
    }

}
