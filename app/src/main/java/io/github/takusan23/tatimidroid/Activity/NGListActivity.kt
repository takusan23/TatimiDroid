package io.github.takusan23.tatimidroid.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Adapter.NGListRecyclerView
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NGDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NGDBInit
import kotlinx.android.synthetic.main.activity_nglist.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * NG一覧Activity
 * */
class NGListActivity : AppCompatActivity() {

    // RecyclerView関連
    var recyclerViewList = arrayListOf<NGDBEntity>()
    lateinit var ngListRecyclerView: NGListRecyclerView

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
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                // データ読み出し
                NGDBInit(this@NGListActivity).ngDataBase.ngDBDAO().getNGCommentList().forEach {
                    recyclerViewList.add(it)
                }
            }
            // リスト更新
            ngListRecyclerView.notifyDataSetChanged()
        }
    }

    //NGユーザー読み込み
    fun loadNGUser() {
        recyclerViewList.clear()
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                // データ読み出し
                NGDBInit(this@NGListActivity).ngDataBase.ngDBDAO().getNGUserList().forEach {
                    recyclerViewList.add(it)
                }
            }
            // リスト更新
            ngListRecyclerView.notifyDataSetChanged()
        }
    }
}
