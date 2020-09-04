package io.github.takusan23.tatimidroid.NicoLive

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.*
import io.github.takusan23.tatimidroid.NicoLive.Adapter.AutoAdmissionAdapter
import io.github.takusan23.tatimidroid.NicoLive.Adapter.CommunityRecyclerViewAdapter
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.*
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.Room.Init.AutoAdmissionDBInit
import io.github.takusan23.tatimidroid.Service.AutoAdmissionService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_community_list_layout.community_recyclerview
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

/**
 * フォロー中とかニコ生TOP表示Fragment
 * */
class CommunityListFragment : Fragment() {
    var userSession = ""
    lateinit var pref_setting: SharedPreferences
    var recyclerViewList: ArrayList<NicoLiveProgramData> = arrayListOf()
    var autoAdmissionRecyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var communityRecyclerViewAdapter: CommunityRecyclerViewAdapter
    lateinit var autoAdmissionAdapter: AutoAdmissionAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_commnunity_list_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = pref_setting.getString("user_session", "") ?: ""

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
                (savedInstanceState.getSerializable("list") as ArrayList<NicoLiveProgramData>).forEach {
                    recyclerViewList.add(it)
                }
                communityRecyclerViewAdapter.notifyDataSetChanged()
            }
        }

    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        recyclerViewList = ArrayList()
        community_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        community_recyclerview.layoutManager = mLayoutManager
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
            FOLLOW -> getProgramDataFromNicoLiveTopPage(NicoLiveProgram.FAVOURITE_PROGRAM)
            NICOREPO -> getProgramDataFromNicorepo()
            RECOMMEND -> getProgramDataFromNicoLiveTopPage(NicoLiveProgram.RECOMMEND_PROGRAM)
            RANKING -> getRanking()
            GAME_MATCHING -> getProgramFromNicoNamaGame(NicoLiveGameProgram.NICONAMA_GAME_MATCHING)
            GAME_PLAYING -> getProgramFromNicoNamaGame(NicoLiveGameProgram.NICONAMA_GAME_PLAYING)
            ADMISSION -> {
                getAutoAdmissionList()
                //Service再起動
                val intent = Intent(context, AutoAdmissionService::class.java)
                context?.stopService(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(intent)
                } else {
                    context?.startService(intent)
                }
            }
            CHUMOKU -> getProgramDataFromNicoLiveTopPage(NicoLiveProgram.FORCUS_PROGRAM)
            YOYAKU -> getProgramDataFromNicoLiveTopPage(NicoLiveProgram.POPULAR_BEFORE_OPEN_BROADCAST_STATUS_PROGRAM)
            KOREKARA -> getProgramDataFromNicoLiveTopPage(NicoLiveProgram.RECENT_JUST_BEFORE_BROADCAST_STATUS_PROGRAM)
            ROOKIE -> getProgramDataFromNicoLiveTopPage(NicoLiveProgram.ROOKIE_PROGRAM)
        }
    }

    /**
     * ニコ生ゲーム募集中番組取得してRecyclerViewに入れる。
     * @param url プレイ中なら[NicoLiveGameProgram.NICONAMA_GAME_PLAYING]。募集中なら[NicoLiveGameProgram.NICONAMA_GAME_MATCHING]
     * */
    private fun getProgramFromNicoNamaGame(url: String) {
        recyclerViewList.clear()
        val nicoLiveGameProgram = NicoLiveGameProgram()
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // コルーチン実行
        lifecycleScope.launch(errorHandler) {
            val html = nicoLiveGameProgram.getNicoNamaGameProgram(userSession, url)
            if (html.isSuccessful) {
                // 成功時
                withContext(Dispatchers.Default) {
                    val gameProgram = nicoLiveGameProgram.parseJSON(html.body?.string())
                    gameProgram.forEach {
                        recyclerViewList.add(it)
                    }
                }
                // 反映
                communityRecyclerViewAdapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            } else {
                showToast("${getString(R.string.error)}\n${html.code}")
            }
        }
    }

    /**
     * ランキング取得
     * */
    private fun getRanking() {
        recyclerViewList.clear()
        val nicoLiveRanking = NicoLiveRanking()
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // コルーチン実行
        lifecycleScope.launch(errorHandler) {
            val html = nicoLiveRanking.getRankingHTML()
            if (html.isSuccessful) {
                // 成功時
                withContext(Dispatchers.Default) {
                    val rankingList = nicoLiveRanking.parseJSON(html.body?.string())
                    rankingList.forEach {
                        recyclerViewList.add(it)
                    }
                }
                // 反映
                communityRecyclerViewAdapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            } else {
                showToast("${getString(R.string.error)}\n${html.code}")
            }
        }
    }

    /**
     * ニコ生TOPページから番組を取得してRecyclerViewに入れる。フォロー中番組など
     * @param jsonObjectName [NicoLiveProgram.FAVOURITE_PROGRAM] 等入れてね。
     * */
    private fun getProgramDataFromNicoLiveTopPage(jsonObjectName: String) {
        recyclerViewList.clear()
        val nicoLiveProgram = NicoLiveProgram()
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // コルーチン実行
        lifecycleScope.launch(errorHandler) {
            val html = nicoLiveProgram.getNicoLiveTopPageHTML(userSession)
            if (html.isSuccessful) {
                // ログインキレたとき
                if (!NicoLiveHTML().hasNiconicoID(html)) {
                    showSnackBar(message = getString(R.string.login_disable_message), showTime = Snackbar.LENGTH_INDEFINITE, buttonText = getString(R.string.login)) {
                        lifecycleScope.launch {
                            // 再ログイン+再取得
                            userSession = NicoLogin.reNicoLogin(context)
                            getProgramDataFromNicoLiveTopPage(jsonObjectName)
                        }
                        return@showSnackBar
                    }
                }
                // 成功時
                withContext(Dispatchers.Default) {
                    val followProgram = nicoLiveProgram.parseJSON(html.body?.string(), jsonObjectName)
                    followProgram.forEach {
                        recyclerViewList.add(it)
                    }
                }
                // UIスレッドで反映
                communityRecyclerViewAdapter.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            } else {
                // 失敗時
                showToast("${getString(R.string.error)}\n${html.code}")
            }
        }
    }

    /**
     * ニコレポを取得してRecyclerViewに入れる関数
     * */
    private fun getProgramDataFromNicorepo() {
        recyclerViewList.clear()
        val nicoLiveNicoRepoAPI = NicoLiveNicoRepoAPI()
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // コルーチン実行
        lifecycleScope.launch(errorHandler) {
            val nicorepo = nicoLiveNicoRepoAPI.getNicorepo(userSession)
            when {
                nicorepo.isSuccessful -> {
                    // 成功時
                    withContext(Dispatchers.Default) {
                        val followProgram = nicoLiveNicoRepoAPI.parseJSON(nicorepo.body?.string())
                        followProgram.forEach {
                            recyclerViewList.add(it)
                        }
                    }
                    // 反映
                    communityRecyclerViewAdapter.notifyDataSetChanged()
                    swipeRefreshLayout.isRefreshing = false
                }
                !NicoLiveHTML().hasNiconicoID(nicorepo) -> {
                    // ログインキレた
                    showSnackBar(message = getString(R.string.login_disable_message), showTime = Snackbar.LENGTH_INDEFINITE, buttonText = getString(R.string.login)) {
                        lifecycleScope.launch {
                            // 再ログイン+再取得
                            userSession = NicoLogin.reNicoLogin(context)
                            getProgramDataFromNicorepo()
                        }
                        return@showSnackBar
                    }
                }
                else -> {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${nicorepo.code}")
                }
            }
        }
    }

    // SnackBar表示
    private fun showSnackBar(message: String, showTime: Int = Snackbar.LENGTH_SHORT, buttonText: String? = null, click: (() -> Unit)? = null) = lifecycleScope.launch(Dispatchers.Main) {
        Snackbar.make(community_recyclerview, message, showTime).apply {
            anchorView = (activity as MainActivity).main_activity_bottom_navigationview
            if (buttonText != null && click != null) {
                // nullじゃなければボタン表示
                setAction(buttonText) { click() }
            }
            show()
        }
    }

    //予約枠自動入場一覧取得
    fun getAutoAdmissionList() {
        recyclerViewList.clear()
        autoAdmissionRecyclerViewList.clear()
        // データベースアクセス
        lifecycleScope.launch(Dispatchers.Main) {
            // 取り出す
            withContext(Dispatchers.IO) {
                val autoAdmissionList = AutoAdmissionDBInit(requireContext()).commentCollectionDB.autoAdmissionDBDAO().getAll()
                // RecyclerViewへ
                autoAdmissionList.forEach { data ->
                    //未来の番組だけ読み込む（終わってるのは読み込まない）
                    if ((Calendar.getInstance().timeInMillis / 1000L) < data.startTime.toLong()) {
                        // RecyclerView追加
                        val item = arrayListOf<String>()
                        item.add("")
                        item.add(data.name)
                        item.add(data.liveId)
                        item.add(data.startTime)
                        item.add(data.lanchApp)
                        autoAdmissionRecyclerViewList.add(item)
                    }
                }
            }
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
        /** フォロー中番組 */
        const val FOLLOW = 0

        /** ニコレポ */
        const val NICOREPO = 1

        /** おすすめ */
        const val RECOMMEND = 2

        /** ランキング */
        const val RANKING = 3

        /** ゲームマッチング */
        const val GAME_MATCHING = 4

        /** ゲームプレイ中 */
        const val GAME_PLAYING = 5

        /** 予約枠自動入場 */
        const val ADMISSION = 6

        /** 放送中の注目番組 */
        const val CHUMOKU = 7

        /** 人気の予約されている番組 */
        const val YOYAKU = 8

        /** これから */
        const val KOREKARA = 9

        /** ルーキー番組 */
        const val ROOKIE = 10
    }

}