package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.content.SharedPreferences
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.Adapter.AutoAdmissionAdapter
import io.github.takusan23.tatimidroid.Adapter.CommunityRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoAPI.ProgramAPI
import io.github.takusan23.tatimidroid.NicoAPI.ProgramData
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionSQLiteSQLite
import kotlinx.android.synthetic.main.fragment_community_list_layout.community_recyclerview
import java.util.*
import kotlin.collections.ArrayList

/**
 * フォロー中とかニコ生TOP表示Fragment
 * */
class CommunityListFragment : Fragment() {
    var user_session = ""
    lateinit var pref_setting: SharedPreferences
    var recyclerViewList: ArrayList<ProgramData> = arrayListOf()
    var autoAdmissionRecyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var communityRecyclerViewAdapter: CommunityRecyclerViewAdapter
    lateinit var autoAdmissionAdapter: AutoAdmissionAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    lateinit var autoAdmissionSQLiteSQLite: AutoAdmissionSQLiteSQLite
    lateinit var sqLiteDatabase: SQLiteDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commnunity_list_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_setting.getString("user_session", "") ?: ""

        initRecyclerView()

        swipeRefreshLayout = view.findViewById(R.id.community_swipe)

        swipeRefreshLayout.setOnRefreshListener {
            setNicoLoad()
        }

        val pos = arguments?.getInt("page") ?: FOLLOW
        if (savedInstanceState == null) {
            // は　じ　め　て ///
            setNicoLoad()
        } else {
            // 画面回転復帰時
            if (pos == ADMISSION) {
                // 予約枠自動入場
                (savedInstanceState.getSerializable("auto") as ArrayList<ArrayList<*>>).forEach {
                    autoAdmissionRecyclerViewList.add(it)
                }
                autoAdmissionAdapter.notifyDataSetChanged()
            } else {
                // それいがい
                (savedInstanceState.getSerializable("list") as ArrayList<ProgramData>).forEach {
                    recyclerViewList.add(it)
                }
                communityRecyclerViewAdapter.notifyDataSetChanged()
            }
        }

    }

    private fun initRecyclerView() {
        recyclerViewList = ArrayList()
        community_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        community_recyclerview.layoutManager = mLayoutManager as RecyclerView.LayoutManager?
        communityRecyclerViewAdapter = CommunityRecyclerViewAdapter(recyclerViewList)
        autoAdmissionAdapter = AutoAdmissionAdapter(autoAdmissionRecyclerViewList)
        autoAdmissionAdapter.communityListFragment = this
        community_recyclerview.adapter = communityRecyclerViewAdapter
        recyclerViewLayoutManager = community_recyclerview.layoutManager!!
    }

    fun setNicoLoad() {
        swipeRefreshLayout.isRefreshing = true
        // 読み込むTL
        val pos = arguments?.getInt("page") ?: FOLLOW
        when (pos) {
            FOLLOW -> getFavouriteCommunity()
            NICOREPO -> getNicorepo()
            RECOMMEND -> getRecommend()
            RANKING -> getRanking()
            GAME_MATCHING -> getNicoNamaGameMatching()
            GAME_PLAYING -> getNicoNamaGamePlaying()
            ADMISSION -> {
                getAutoAdmissionList()
                //Service再起動
                val intent = Intent(context, AutoAdmissionService::class.java)
                context?.stopService(intent)
                context?.startService(intent)
            }
            CHUMOKU -> getChumoku()
            YOYAKU -> getBeforeOpen()
            KOREKARA -> getKorekara()
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
                swipeRefreshLayout.isRefreshing = false
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
                swipeRefreshLayout.isRefreshing = false
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
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    //おすすめの番組
    fun getRecommend() {
        recyclerViewList.clear()
        ProgramAPI(context).getRecommend(null) { arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // 放送中の注目番組
    fun getChumoku() {
        recyclerViewList.clear()
        ProgramAPI(context).getFocusProgramListState(null) { arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // 人気の予約番組
    fun getBeforeOpen() {
        recyclerViewList.clear()
        ProgramAPI(context).getPopularBeforeOpenBroadcastStatusProgramListState(null) { arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // これからの注目番組
    fun getKorekara() {
        recyclerViewList.clear()
        ProgramAPI(context).getRecentJustBeforeBroadcastStatusProgramListState(null) { arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    // 参加中コミュニティから放送中、予約枠を取得する。
    // 今まではスマホサイトにアクセスしてJSON取ってたけど動かなくなった。
    // のでPC版にアクセスしてJSONを取得する（PC版にもJSON存在した。）
    fun getFavouriteCommunity() {
        recyclerViewList.clear()
        ProgramAPI(context).getFollowProgram(null) { arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    //ニコレポ取得
    fun getNicorepo() {
        recyclerViewList.clear()
        ProgramAPI(context).getNicorepo(null) { response, arrayList ->
            //リスト更新
            activity?.runOnUiThread {
                communityRecyclerViewAdapter.notifyDataSetChanged()
                arrayList.forEach {
                    recyclerViewList.add(it)
                }
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    //予約枠自動入場機能
    fun getAutoAdmissionList() {
        recyclerViewList.clear()

        //初期化したか
        if (!this@CommunityListFragment::autoAdmissionSQLiteSQLite.isInitialized) {
            autoAdmissionSQLiteSQLite = AutoAdmissionSQLiteSQLite(context!!)
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
            swipeRefreshLayout.isRefreshing = false
        }
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("list", recyclerViewList)
    }

    companion object {
        val FOLLOW = 0 // フォロー中番組
        val NICOREPO = 1 // ニコレポ
        val RECOMMEND = 2 // おすすめ
        val RANKING = 3 // ランキング
        val GAME_MATCHING = 4 // ゲームマッチング
        val GAME_PLAYING = 5 // ゲームプレイ中
        val ADMISSION = 6 // 予約枠自動入場
        val CHUMOKU = 7 // 放送中の注目番組
        val YOYAKU = 8 // 人気の予約されてる番組
        val KOREKARA = 9 // これから
    }

}