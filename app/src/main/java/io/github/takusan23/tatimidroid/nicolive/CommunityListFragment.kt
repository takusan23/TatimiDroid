package io.github.takusan23.tatimidroid.nicolive

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.MainActivity
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.databinding.FragmentNicoliveCommunityBinding
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLogin
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveGameProgram
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveProgramHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveRankingHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicoapi.nicorepo.NicoRepoAPIX
import io.github.takusan23.tatimidroid.nicolive.adapter.CommunityRecyclerViewAdapter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

/**
 * フォロー中とかニコ生TOP表示Fragment
 * */
class CommunityListFragment : Fragment() {

    private var userSession = ""
    private lateinit var prefSetting: SharedPreferences
    private var recyclerViewList: ArrayList<NicoLiveProgramData> = arrayListOf()
    private lateinit var communityRecyclerViewAdapter: CommunityRecyclerViewAdapter

    /** findViewById駆逐 */
    private val viewBinding by lazy { FragmentNicoliveCommunityBinding.inflate(layoutInflater) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        userSession = prefSetting.getString("user_session", "") ?: ""

        initRecyclerView()

        viewBinding.fragmentNicoliveCommunitySwipe.setOnRefreshListener {
            setNicoLoad()
        }

        if (savedInstanceState == null) {
            // は　じ　め　て ///
            setNicoLoad()
        } else {
            // 画面回転復帰時
            // それいがい
            (savedInstanceState.getSerializable("list") as ArrayList<NicoLiveProgramData>).forEach {
                recyclerViewList.add(it)
            }
            communityRecyclerViewAdapter.notifyDataSetChanged()
        }

    }

    // RecyclerView初期化
    private fun initRecyclerView() {
        viewBinding.fragmentNicoliveCommunityRecyclerView.apply {
            recyclerViewList = ArrayList()
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            communityRecyclerViewAdapter = CommunityRecyclerViewAdapter(recyclerViewList, isDisableCache = true)
            adapter = communityRecyclerViewAdapter
        }
    }

    fun setNicoLoad() {
        viewBinding.fragmentNicoliveCommunitySwipe.isRefreshing = true
        // 読み込むTL
        val pos = arguments?.getInt("page") ?: FOLLOW
        when (pos) {
            FOLLOW -> getProgramDataFromNicoLiveTopPage(NicoLiveProgramHTML.FAVOURITE_PROGRAM)
            NICOREPO -> getProgramDataFromNicorepo()
            RECOMMEND -> getProgramDataFromNicoLiveTopPage(NicoLiveProgramHTML.RECOMMEND_PROGRAM)
            RANKING -> getRanking()
            GAME_MATCHING -> getProgramFromNicoNamaGame(NicoLiveGameProgram.NICONAMA_GAME_MATCHING)
            GAME_PLAYING -> getProgramFromNicoNamaGame(NicoLiveGameProgram.NICONAMA_GAME_PLAYING)
            CHUMOKU -> getProgramDataFromNicoLiveTopPage(NicoLiveProgramHTML.FORCUS_PROGRAM)
            YOYAKU -> getProgramDataFromNicoLiveTopPage(NicoLiveProgramHTML.POPULAR_BEFORE_OPEN_BROADCAST_STATUS_PROGRAM)
            KOREKARA -> getProgramDataFromNicoLiveTopPage(NicoLiveProgramHTML.RECENT_JUST_BEFORE_BROADCAST_STATUS_PROGRAM)
            ROOKIE -> getProgramDataFromNicoLiveTopPage(NicoLiveProgramHTML.ROOKIE_PROGRAM)
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
                viewBinding.fragmentNicoliveCommunitySwipe.isRefreshing = false
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
        val nicoLiveRanking = NicoLiveRankingHTML()
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
                viewBinding.fragmentNicoliveCommunitySwipe.isRefreshing = false
            } else {
                showToast("${getString(R.string.error)}\n${html.code}")
            }
        }
    }

    /**
     * ニコ生TOPページから番組を取得してRecyclerViewに入れる。フォロー中番組など
     * @param jsonObjectName [NicoLiveProgramHTML.FAVOURITE_PROGRAM] 等入れてね。
     * */
    private fun getProgramDataFromNicoLiveTopPage(jsonObjectName: String) {
        recyclerViewList.clear()
        val nicoLiveProgram = NicoLiveProgramHTML()
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
                            userSession = NicoLogin.secureNicoLogin(context) ?: return@launch
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
                viewBinding.fragmentNicoliveCommunitySwipe.isRefreshing = false
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
        val nicoRepoAPI = NicoRepoAPIX()
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        // コルーチン実行
        lifecycleScope.launch(errorHandler) {
            withContext(Dispatchers.IO) {
                val response = nicoRepoAPI.getNicoRepoResponse(userSession)
                when {
                    response.isSuccessful -> {
                        // 成功時
                        withContext(Dispatchers.Default) {
                            val followProgram = nicoRepoAPI.parseNicoRepoResponse(response.body?.string()).filter { nicoRepoDataClass -> !nicoRepoDataClass.isVideo }
                            nicoRepoAPI.toProgramDataList(followProgram).forEach {
                                recyclerViewList.add(it)
                            }
                        }
                        // 反映
                        withContext(Dispatchers.Main) {
                            communityRecyclerViewAdapter.notifyDataSetChanged()
                            viewBinding.fragmentNicoliveCommunitySwipe.isRefreshing = false
                        }
                    }
                    !NicoLiveHTML().hasNiconicoID(response) -> {
                        // ログインキレた
                        showSnackBar(message = getString(R.string.login_disable_message), showTime = Snackbar.LENGTH_INDEFINITE, buttonText = getString(R.string.login)) {
                            lifecycleScope.launch {
                                // 再ログイン+再取得
                                userSession = NicoLogin.secureNicoLogin(context) ?: return@launch
                                getProgramDataFromNicorepo()
                            }
                            return@showSnackBar
                        }
                    }
                    else -> {
                        // 失敗時
                        showToast("${getString(R.string.error)}\n${response.code}")
                    }
                }
            }
        }
    }

    // SnackBar表示
    private fun showSnackBar(message: String, showTime: Int = Snackbar.LENGTH_SHORT, buttonText: String? = null, click: (() -> Unit)? = null) = lifecycleScope.launch(Dispatchers.Main) {
        Snackbar.make(viewBinding.fragmentNicoliveCommunityRecyclerView, message, showTime).apply {
            anchorView = (activity as MainActivity).viewBinding.mainActivityBottomNavigationView
            if (buttonText != null && click != null) {
                // nullじゃなければボタン表示
                setAction(buttonText) { click() }
            }
            show()
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