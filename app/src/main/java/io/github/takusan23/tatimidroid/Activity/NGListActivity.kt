package io.github.takusan23.tatimidroid.Activity

import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.DarkModeSupport
import io.github.takusan23.tatimidroid.GiftRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NGListRecyclerView
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import kotlinx.android.synthetic.main.activity_nglist.*
import kotlinx.android.synthetic.main.fragment_commnunity_list_layout.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*

class NGListActivity : AppCompatActivity() {
    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var ngListRecyclerView: NGListRecyclerView
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    //NGデータベース
    lateinit var ngListSQLiteHelper: NGListSQLiteHelper
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //ダークモード
        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)
        setContentView(R.layout.activity_nglist)

        supportActionBar?.title = getString(R.string.ng_list)

        recyclerViewList = ArrayList()
        activity_ng_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        activity_ng_recyclerview.layoutManager = mLayoutManager as RecyclerView.LayoutManager?
        ngListRecyclerView = NGListRecyclerView(recyclerViewList)
        activity_ng_recyclerview.adapter = ngListRecyclerView
        recyclerViewLayoutManager = activity_ng_recyclerview.layoutManager!!

        if (!this@NGListActivity::ngListSQLiteHelper.isInitialized) {
            //データベース
            ngListSQLiteHelper = NGListSQLiteHelper(this)
            sqLiteDatabase = ngListSQLiteHelper.writableDatabase
            ngListSQLiteHelper.setWriteAheadLoggingEnabled(false)
        }

        loadNGUser()

        activity_ng_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }


            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.text) {
                    getString(R.string.ng_comment) -> {
                        loadNGComment()
                    }
                    getString(R.string.ng_user) -> {
                        loadNGUser()
                    }
                }
            }
        })
    }

    //NGコメント読み込み
    fun loadNGComment() {
        recyclerViewList.clear()
        //SQLite読み出し
        val cursor = sqLiteDatabase.query(
            "ng_list",
            arrayOf("type", "value"),
            "type=?", arrayOf("comment"), null, null, null
        )
        cursor.moveToFirst()
        //for
        for (i in 0 until cursor.count) {
            val item = arrayListOf<String>()
            item.add("")
            item.add(cursor.getString(0))
            item.add(cursor.getString(1))
            recyclerViewList.add(item)
            cursor.moveToNext()
        }
        cursor.close()
        runOnUiThread {
            ngListRecyclerView.notifyDataSetChanged()
        }
    }

    //NGユーザー読み込み
    fun loadNGUser() {
        recyclerViewList.clear()
        //SQLite読み出し
        val cursor = sqLiteDatabase.query(
            "ng_list",
            arrayOf("type", "value"),
            "type=?", arrayOf("user"), null, null, null
        )
        cursor.moveToFirst()
        //for
        for (i in 0 until cursor.count) {
            val item = arrayListOf<String>()
            item.add("")
            item.add(cursor.getString(0))
            item.add(cursor.getString(1))
            recyclerViewList.add(item)
            cursor.moveToNext()
        }
        cursor.close()
        runOnUiThread {
            ngListRecyclerView.notifyDataSetChanged()
        }
    }
}
