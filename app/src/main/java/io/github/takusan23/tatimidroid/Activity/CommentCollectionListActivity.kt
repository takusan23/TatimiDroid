package io.github.takusan23.tatimidroid.Activity

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.Adapter.CommentPOSTListRecyclerViewAdapter
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.CommentCollectionSQLiteHelper
import kotlinx.android.synthetic.main.activity_comment_postlist.*

class CommentCollectionListActivity : AppCompatActivity() {

    lateinit var commentCollection: CommentCollectionSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    //コメントコレクションにすでに登録されていれば上書きする
    val commentCollectionYomiList = arrayListOf<String>()

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var commentPOSTListRecyclerViewAdapter: CommentPOSTListRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment_postlist)

        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)


        supportActionBar?.title = getString(R.string.comment_post_list)

        recyclerViewList = ArrayList()
        activity_comment_post_list_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        activity_comment_post_list_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        commentPOSTListRecyclerViewAdapter =
            CommentPOSTListRecyclerViewAdapter(recyclerViewList)
        activity_comment_post_list_recyclerview.adapter = commentPOSTListRecyclerViewAdapter
        recyclerViewLayoutManager = activity_comment_post_list_recyclerview.layoutManager!!

        //データベース
        commentCollection = CommentCollectionSQLiteHelper(this)
        sqLiteDatabase = commentCollection.writableDatabase
        commentCollection.setWriteAheadLoggingEnabled(false)

        //登録
        activity_comment_post_list_add.setOnClickListener {
            val comment = activity_comment_post_list_comment_inputedittext.text.toString()
            val yomi = activity_comment_post_list_yomi_inputedittext.text.toString()
            val contentValues = ContentValues()
            contentValues.put("comment", comment)
            contentValues.put("yomi", yomi)
            contentValues.put("description", "")
            //上書きするか新規で入れるか。読みがかぶったら上書き。
            if (commentCollectionYomiList.contains(yomi)) {
                //上書き
                sqLiteDatabase.update(
                    "comment_collection_db",
                    contentValues,
                    "yomi=?",
                    arrayOf(yomi)
                )
                //Toast
                Toast.makeText(
                    this,
                    getString(R.string.comment_collection_change_successful),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //新規
                sqLiteDatabase.insert("comment_collection_db", null, contentValues)
                //Toast
                Toast.makeText(
                    this,
                    getString(R.string.comment_collection_add_successful),
                    Toast.LENGTH_SHORT
                ).show()
            }
            //読み込み
            loadList()
        }

        //読み込み
        loadList()
    }

    fun loadList() {
        recyclerViewList.clear()
        val cursor = sqLiteDatabase.query(
            "comment_collection_db",
            arrayOf("comment", "yomi", "description"),
            null, null, null, null, null
        )
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {
            val comment = cursor.getString(0)
            val yomi = cursor.getString(1)
            //RecyclerView追加
            val item = arrayListOf<String>()
            item.add("")
            item.add(comment)
            item.add(yomi)
            recyclerViewList.add(item)
            //上書きか新規か確認できるように配列に入れておく
            commentCollectionYomiList.add(yomi)
            //つぎへ
            cursor.moveToNext()
        }
        cursor.close()
        //リスト更新
        runOnUiThread {
            commentPOSTListRecyclerViewAdapter.notifyDataSetChanged()
        }
    }

}
