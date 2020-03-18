package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.NicoLiveAPI.ProgramAPI
import io.github.takusan23.tatimidroid.NicoLiveAPI.ProgramData
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import kotlinx.android.synthetic.main.fragment_commnunity_list_layout.*
import kotlinx.android.synthetic.main.fragment_community_list_layout.community_recyclerview
import java.util.*
import kotlin.collections.ArrayList


class CommunityListFragment : Fragment() {
    var user_session = ""
    lateinit var pref_setting: SharedPreferences
    var recyclerViewList: ArrayList<ProgramData> = arrayListOf()
    var autoAdmissionRecyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var communityRecyclerViewAdapter: CommunityRecyclerViewAdapter
    lateinit var autoAdmissionAdapter: AutoAdmissionAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commnunity_list_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        val darkModeSupport = DarkModeSupport(context!!)
        program_tablayout.backgroundTintList =
            ColorStateList.valueOf(darkModeSupport.getThemeColor())

        (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.follow_program)

        recyclerViewList = ArrayList()
        community_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        community_recyclerview.layoutManager = mLayoutManager as RecyclerView.LayoutManager?
        communityRecyclerViewAdapter = CommunityRecyclerViewAdapter(recyclerViewList)
        autoAdmissionAdapter = AutoAdmissionAdapter(autoAdmissionRecyclerViewList)
        community_recyclerview.adapter = communityRecyclerViewAdapter
        recyclerViewLayoutManager = community_recyclerview.layoutManager!!

        user_session = pref_setting.getString("user_session", "") ?: ""

        (activity as AppCompatActivity).supportActionBar?.title =
            getString(R.string.follow_program)

        //参加中のコミュニティ読み込み
        getFavouriteCommunity()

        community_swipe.isRefreshing = true

        program_tablayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                //クリア
                recyclerViewList.clear()
                community_recyclerview.adapter?.notifyDataSetChanged()
                setNicoLoad(tab?.text.toString() ?: getString(R.string.follow_program))
            }
        })

        community_swipe.setOnRefreshListener {
            val pos = program_tablayout.selectedTabPosition
            val text = program_tablayout.getTabAt(pos)?.text ?: getString(R.string.follow_program)
            setNicoLoad(text.toString())
        }


    }

    fun setNicoLoad(text: String) {
        //くるくる
        community_swipe.isRefreshing = true
        when (text) {
            getString(R.string.follow_program) -> {
                getFavouriteCommunity()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.follow_program)
            }
            getString(R.string.nicorepo) -> {
                getNicorepo()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.nicorepo)
            }
            getString(R.string.auto_admission) -> {
                getAutoAdmissionList()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.auto_admission)
                //Service再起動
                val intent = Intent(context, AutoAdmissionService::class.java)
                context?.stopService(intent)
                context?.startService(intent)
            }
            getString(R.string.osusume) -> {
                getRecommend()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.osusume)
            }
            getString(R.string.ranking) -> {
                getRanking()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.ranking)
            }
            getString(R.string.nico_nama_game_recruiting_program) -> {
                // 募集中
                getNicoNamaGameMatching()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.nico_nama_game_recruiting_program)
            }
            getString(R.string.nico_nama_game_playing_program) -> {
                // プレイ中
                getNicoNamaGamePlaying()
                (activity as AppCompatActivity).supportActionBar?.title =
                    getString(R.string.nico_nama_game_playing_program)
            }
        }
    }

    /**
     * ニコ生ゲーム募集中番組取得。
     * */
    fun getNicoNamaGameMatching() {
        recyclerViewList.clear()
        val programAPI = ProgramAPI(context)
        programAPI.getNicoNamaGame(programAPI.NICONAMA_GAME_MATCHING, null) { response, arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                community_swipe.isRefreshing = false
            }
        }
    }

    /**
     * ニコ生ゲームプレイ中番組取得。
     * */
    fun getNicoNamaGamePlaying() {
        recyclerViewList.clear()
        val programAPI = ProgramAPI(context)
        programAPI.getNicoNamaGame(programAPI.NICONAMA_GAME_PLAYING, null) { response, arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                community_swipe.isRefreshing = false
            }
        }
    }

    //ランキング取得
    fun getRanking() {
        recyclerViewList.clear()
        ProgramAPI(context).getRanking(null) { response, arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                community_swipe.isRefreshing = false
            }
        }
    }

    //おすすめの番組
    fun getRecommend() {
        recyclerViewList.clear()
        ProgramAPI(context).getRecommend(null) { response, arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                community_swipe.isRefreshing = false
            }
        }
    }


    // 参加中コミュニティから放送中、予約枠を取得する。
    // 今まではスマホサイトにアクセスしてJSON取ってたけど動かなくなった。
    // のでPC版にアクセスしてJSONを取得する（PC版にもJSON存在した。）
    fun getFavouriteCommunity() {
        recyclerViewList.clear()
        ProgramAPI(context).getFollowProgram(null) { response, arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                community_swipe.isRefreshing = false
            }
        }
    }

    //ニコレポ取得
    fun getNicorepo() {
        recyclerViewList.clear()
        ProgramAPI(context)?.getNicorepo(null) { response, arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                community_swipe.isRefreshing = false
            }
        }
    }

    //予約枠自動入場機能
    fun getAutoAdmissionList() {
        recyclerViewList.clear()

        //初期化したか
        if (!this@CommunityListFragment::autoAdmissionSQLiteSQLite.isInitialized) {
            autoAdmissionSQLiteSQLite =
                AutoAdmissionSQLiteSQLite(context!!)
            sqLiteDatabase = autoAdmissionSQLiteSQLite.writableDatabase
            autoAdmissionSQLiteSQLite.setWriteAheadLoggingEnabled(false)
        }

        //SQLite読み出し
        val cursor = sqLiteDatabase.query(
            "auto_admission",
            arrayOf("name", "liveid", "start", "app"),
            null, null, null, null, null
        )
        cursor.moveToFirst()
        for (i in 0 until cursor.count) {

            val programName = cursor.getString(0)
            val liveId = cursor.getString(1)
            val start = cursor.getString(2)
            val app = cursor.getString(3)

            //未来の番組だけ読み込む（終わってるのは読み込まない）
            if ((Calendar.getInstance().timeInMillis / 1000L) < start.toLong()) {
                //RecyclerView追加
                val item = arrayListOf<String>()
                item.add("")
                item.add(programName)
                item.add(liveId)
                item.add(start)
                item.add(app)
                autoAdmissionRecyclerViewList.add(item)
            }
            cursor.moveToNext()
        }
        cursor.close()

        //リスト更新
        activity?.runOnUiThread {
            autoAdmissionAdapter.notifyDataSetChanged()
            community_recyclerview.adapter = autoAdmissionAdapter
            community_swipe.isRefreshing = false
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}