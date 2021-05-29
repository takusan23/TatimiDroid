package io.github.takusan23.tatimidroid.nicolive.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.jk.NicoLiveJKHTML
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLogin
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveProgramHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveRankingHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicoapi.nicorepo.NicoRepoAPIX
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ニコ生の番組一覧で使うViewModel
 *
 * Fragment時代はViewModel使わずFragmentに書いてたのでマウスホイールを一生懸命回さないと（それ以前に画面回転を超えるのがめんどい）
 *
 * 最初はフォロー中番組を取得しに行きます
 * */
class NicoLiveProgramListViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    private var userSession = prefSetting.getString("user_session", "") ?: ""

    /** ニコ生トップページスクレイピング */
    private val nicoLiveProgramHTML = NicoLiveProgramHTML()

    /** ニコレポAPI */
    private val nicoRepoAPIX = NicoRepoAPIX()

    /** ニコ生ランキングスクレイピング */
    private val nicoLiveRanking = NicoLiveRankingHTML()

    /** 実況スクレイピング */
    private val nicoLiveJKHTML = NicoLiveJKHTML()

    /** 一応 */
    private val nicoLiveHTML = NicoLiveHTML()

    /** 番組一覧を送信するLiveData */
    val programListLiveData = MutableLiveData<List<NicoLiveProgramData>>()

    /** 読込中ならtrueを送るLiveData */
    val isLoadingLiveData = MutableLiveData(false)

    init {
        getFollowingProgram()
    }

    /** フォロー中番組を取得する */
    fun getFollowingProgram() = getProgramList(NicoLiveProgramHTML.FAVOURITE_PROGRAM)

    /** あなたへのおすすめ番組を取得する */
    fun getRecommendProgram() = getProgramList(NicoLiveProgramHTML.RECOMMEND_PROGRAM)

    /** 放送中の注目番組を取得する */
    fun getFocusProgram() = getProgramList(NicoLiveProgramHTML.FORCUS_PROGRAM)

    /** これからの注目番組を取得する */
    fun getRecentJustBeforeBroadcastStatusProgramListState() = getProgramList(NicoLiveProgramHTML.RECENT_JUST_BEFORE_BROADCAST_STATUS_PROGRAM)

    /** 人気の予約されている番組を取得する */
    fun getPopularBeforeOpenBroadcastStatusProgramListState() = getProgramList(NicoLiveProgramHTML.POPULAR_BEFORE_OPEN_BROADCAST_STATUS_PROGRAM)

    /** ルーキー番組を取得する */
    fun getRookieProgram() = getProgramList(NicoLiveProgramHTML.ROOKIE_PROGRAM)

    /**
     * ニコ生のトップページから取得する
     * フォロー中番組や、あなたへのおすすめはここから取得できる。
     *
     * レスポンスはLiveDataへ送信される
     *
     * @param [io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveProgramHTML.FAVOURITE_PROGRAM] などを入れてください
     * */
    private fun getProgramList(jsonObjectName: String) {
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            isLoadingLiveData.postValue(true)
            val response = nicoLiveProgramHTML.getNicoLiveTopPageHTML(userSession)
            if (response.isSuccessful) {
                // 成功時
                // でもログインセッション切れたとき
                if (!nicoLiveHTML.hasNiconicoID(response)) {
                    // 再ログインを行う
                    userSession = NicoLogin.secureNicoLogin(context) ?: return@launch
                    showToast(getString(R.string.re_login_successful))
                    // もう一度取得
                    getProgramList(jsonObjectName)
                } else {
                    withContext(Dispatchers.Default) {
                        val followProgram = nicoLiveProgramHTML.parseJSON(response.body?.string(), jsonObjectName)
                        programListLiveData.postValue(followProgram)
                    }
                }
            } else {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
            }
            isLoadingLiveData.postValue(false)
        }
    }

    /** ニコレポAPIを叩いて番組開始を取得する */
    fun getNicorepoProgramList() {
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            isLoadingLiveData.postValue(true)
            val response = nicoRepoAPIX.getNicoRepoResponse(userSession)
            when {
                response.isSuccessful -> {
                    // 成功時
                    withContext(Dispatchers.Default) {
                        val followProgram = nicoRepoAPIX.parseNicoRepoResponse(response.body?.string()).filter { nicoRepoDataClass -> !nicoRepoDataClass.isVideo }
                        // ニコレポのデータクラスを番組情報のデータクラスに変換してLiveDataへ
                        programListLiveData.postValue(nicoRepoAPIX.toProgramDataList(followProgram))
                    }
                }
                !nicoLiveHTML.hasNiconicoID(response) -> {
                    // 再ログインを行う
                    userSession = NicoLogin.secureNicoLogin(context) ?: return@launch
                    showToast(getString(R.string.re_login_successful))
                    // もう一度取得
                    getNicorepoProgramList()
                }
                else -> {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
            isLoadingLiveData.postValue(false)
        }
    }

    /** ランキングを取得する */
    fun getRanking() {
        // 例外を捕まえる。これでtry/catchをそれぞれ書かなくても済む？
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            isLoadingLiveData.postValue(true)
            val html = nicoLiveRanking.getRankingHTML()
            if (html.isSuccessful) {
                // 成功時
                withContext(Dispatchers.Default) {
                    val rankingList = nicoLiveRanking.parseJSON(html.body?.string())
                    programListLiveData.postValue(rankingList)
                }
            } else {
                showToast("${getString(R.string.error)}\n${html.code}")
            }
            isLoadingLiveData.postValue(false)
        }
    }

    /**
     * ニコニコ実況の番組を取得する
     *
     * @param isRequestOfficial 公式で用意しているニコニコ実況番組を取得する場合はtrue。有志の番組を取得する場合はfalse
     * */
    fun getNicoJKProgramList(isRequestOfficial: Boolean = true) {
        viewModelScope.launch {
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                // エラー時
                showToast("${getString(R.string.error)}\n${throwable}")
            }
            viewModelScope.launch(errorHandler + Dispatchers.Default) {
                isLoadingLiveData.postValue(true)
                // HTMLリクエスト
                val response = nicoLiveJKHTML.getNicoLiveJKProgramList(userSession)
                if (!response.isSuccessful) {
                    // 失敗時
                    showToast("${getString(R.string.error)}\n${response.code}")
                    return@launch
                }
                // LiveDataへ送信
                if (isRequestOfficial) {
                    // ニコニコ公式
                    programListLiveData.postValue(nicoLiveJKHTML.parseNicoLiveJKProgramList(response.body?.string()))
                } else {
                    // ユーザー有志
                    programListLiveData.postValue(nicoLiveJKHTML.parseNicoLiveJKTagProgramList(response.body?.string()))
                }
                isLoadingLiveData.postValue(false)
            }
        }
    }

    /** Toast表示 */
    fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Context#getString */
    fun getString(resourceId: Int): String {
        return context.getString(resourceId)
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