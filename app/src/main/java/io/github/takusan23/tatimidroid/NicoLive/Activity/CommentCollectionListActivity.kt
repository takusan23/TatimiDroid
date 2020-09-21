package io.github.takusan23.tatimidroid.NicoLive.Activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.Adapter.CommentPOSTListRecyclerViewAdapter
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Database.CommentCollectionDB
import io.github.takusan23.tatimidroid.Room.Entity.CommentCollectionDBEntity
import io.github.takusan23.tatimidroid.Room.Init.CommentCollectionDBInit
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import kotlinx.android.synthetic.main.activity_comment_postlist.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentCollectionListActivity : AppCompatActivity() {

    // データベース
    lateinit var commentCollectionDB: CommentCollectionDB

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
        commentPOSTListRecyclerViewAdapter = CommentPOSTListRecyclerViewAdapter(recyclerViewList)
        activity_comment_post_list_recyclerview.adapter = commentPOSTListRecyclerViewAdapter
        recyclerViewLayoutManager = activity_comment_post_list_recyclerview.layoutManager!!

        // データベース初期化
        commentCollectionDB = CommentCollectionDBInit(this).commentCollectionDB

        //登録
        activity_comment_post_list_add.setOnClickListener {
            val comment = activity_comment_post_list_comment_inputedittext.text.toString()
            val yomi = activity_comment_post_list_yomi_inputedittext.text.toString()
            lifecycleScope.launch(Dispatchers.Main) {
                val filterList = withContext(Dispatchers.IO) {
                    commentCollectionDB.commentCollectionDBDAO().getAll().filter { commentCollectionEntity -> commentCollectionEntity.yomi == yomi }
                }
                // 上書きするか新規で入れるか。読みがかぶったら上書き。
                if (filterList.isNotEmpty()) {
                    // 上書き
                    val newEntity = filterList[0].copy(comment = comment, yomi = yomi)
                    withContext(Dispatchers.IO) {
                        commentCollectionDB.commentCollectionDBDAO().update(newEntity)
                    }
                    //Toast
                    Toast.makeText(this@CommentCollectionListActivity, getString(R.string.comment_collection_change_successful), Toast.LENGTH_SHORT).show()
                    //読み込み
                    loadList()
                } else {
                    // 新規作成
                    withContext(Dispatchers.IO) {
                        val commentCollectionEntity = CommentCollectionDBEntity(comment = comment, yomi = yomi, description = "")
                        commentCollectionDB.commentCollectionDBDAO().insert(commentCollectionEntity)
                    }
                    //Toast
                    Toast.makeText(this@CommentCollectionListActivity, getString(R.string.comment_collection_add_successful), Toast.LENGTH_SHORT).show()
                    //読み込み
                    loadList()
                }
            }
        }

        //読み込み
        loadList()
    }

    /**
     * 読み込む
     * */
    fun loadList() {
        recyclerViewList.clear()
        commentCollectionYomiList.clear()
        lifecycleScope.launch(Dispatchers.Main) {
            // コルーチン
            withContext(Dispatchers.IO) {
                // データベースから値を取る
                commentCollectionDB.commentCollectionDBDAO().getAll().forEach { data ->
                    //RecyclerView追加
                    val item = arrayListOf<String>()
                    item.add(data.id.toString())
                    item.add(data.comment)
                    item.add(data.yomi)
                    recyclerViewList.add(item)
                    //上書きか新規か確認できるように配列に入れておく
                    commentCollectionYomiList.add(data.yomi)
                }
            }
            // 更新
            commentPOSTListRecyclerViewAdapter.notifyDataSetChanged()
        }
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }


}
