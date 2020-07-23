package io.github.takusan23.tatimidroid.Activity

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.takusan23.tatimidroid.Adapter.KotehanListAdapter
import io.github.takusan23.tatimidroid.Adapter.NGListRecyclerView
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.KotehanDBEntity
import io.github.takusan23.tatimidroid.Room.Init.KotehanDBInit
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import kotlinx.android.synthetic.main.activity_kotehan_list.*
import kotlinx.android.synthetic.main.activity_nglist.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * コテハン一覧あくてぃびてぃー
 * */
class KotehanListActivity : AppCompatActivity() {

    // RecyclerView
    val kotehanList = arrayListOf<KotehanDBEntity>()
    val kotehanListAdapter = KotehanListAdapter(kotehanList) // 新しい順に

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotehan_list)

        // RecyclerView初期化
        initRecyclerView()

        // 読み込む
        loadDB()

    }

    /**
     * データベースからコテハンを取り出す関数。非同期＾～
     * */
    fun loadDB() {
        kotehanList.clear()
        GlobalScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                KotehanDBInit(this@KotehanListActivity).kotehanDB.kotehanDBDAO().getAll().forEach {
                    kotehanList.add(0, it)
                }
            }
            kotehanListAdapter.notifyDataSetChanged()
        }
    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        // RecyclerView初期化
        activity_kotehan_list_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this@KotehanListActivity)
        activity_kotehan_list_recyclerview.layoutManager = mLayoutManager
        activity_kotehan_list_recyclerview.adapter = kotehanListAdapter
        val itemDecoration = DividerItemDecoration(this@KotehanListActivity, DividerItemDecoration.VERTICAL)
        activity_kotehan_list_recyclerview.addItemDecoration(itemDecoration)
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool().setLanguageContext(newBase))
    }

}