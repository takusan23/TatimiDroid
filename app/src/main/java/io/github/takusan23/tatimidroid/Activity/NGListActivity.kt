package io.github.takusan23.tatimidroid.Activity

import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Adapter.NGListRecyclerView
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.SQLiteHelper.NGListSQLiteHelper
import io.github.takusan23.tatimidroid.Tool.DataClass.NGData
import io.github.takusan23.tatimidroid.Tool.NGDataBaseTool
import kotlinx.android.synthetic.main.activity_nglist.*

/**
 * NG一覧Activity
 * */
class NGListActivity : AppCompatActivity() {

    // RecyclerView関連
    var recyclerViewList = arrayListOf<NGData>()
    lateinit var ngListRecyclerView: NGListRecyclerView

    // NGデータベース
    lateinit var ngDataBaseTool: NGDataBaseTool

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //ダークモード
        val darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setActivityTheme(this)
        setContentView(R.layout.activity_nglist)

        supportActionBar?.title = getString(R.string.ng_list)

        activity_ng_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        activity_ng_recyclerview.layoutManager = mLayoutManager
        ngListRecyclerView = NGListRecyclerView(recyclerViewList)
        activity_ng_recyclerview.adapter = ngListRecyclerView

        ngDataBaseTool = NGDataBaseTool(this)

        // BottomNavigation
        activity_ng_bottom_nav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ng_menu_user -> loadNGUser()
                R.id.ng_menu_comment -> loadNGComment()
            }
            true
        }

        // はじめてはNGユーザー表示させる
        activity_ng_bottom_nav.selectedItemId = R.id.ng_menu_user
    }

    //NGコメント読み込み
    fun loadNGComment() {
        recyclerViewList.clear()
        ngDataBaseTool.ngCommentList.forEach {
            recyclerViewList.add(it)
        }
        runOnUiThread {
            ngListRecyclerView.notifyDataSetChanged()
        }
    }

    //NGユーザー読み込み
    fun loadNGUser() {
        recyclerViewList.clear()
        ngDataBaseTool.ngUserList.forEach {
            recyclerViewList.add(it)
        }
        runOnUiThread {
            ngListRecyclerView.notifyDataSetChanged()
        }
    }
}
