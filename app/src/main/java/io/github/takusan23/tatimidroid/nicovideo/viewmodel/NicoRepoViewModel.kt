package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.nicorepo.NicoRepoAPIX
import io.github.takusan23.tatimidroid.nicoapi.nicorepo.NicoRepoDataClass
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [io.github.takusan23.tatimidroid.nicovideo.fragment.NicoVideoNicoRepoFragment]で使うViewModel
 *
 * @param userId nullの場合は自分のデータを取りに行きます。
 * */
class NicoRepoViewModel(application: Application, val userId: String? = null) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** ニコレポデータ */
    val nicoRepoDataListLiveData = MutableLiveData<ArrayList<NicoRepoDataClass>>()

    /** とりあえずAPIを叩いてパースした結果を持っておく。このあとフィルターで初期状態の配列がほしいので */
    private var nicoRepoDataListRaw = arrayListOf<NicoRepoDataClass>()

    /** 読み込み中LiveData */
    val loadingLiveData = MutableLiveData(false)

    /** 動画を一覧に表示する場合はtrue */
    var isShowVideo = true

    /** 生放送を一覧に表示する場合はtrue */
    var isShowLive = true

    init {
        getNicoRepo()
    }

    /**
     * ニコレポを取得する
     * */
    fun getNicoRepo() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            // API叩く
            loadingLiveData.postValue(true)
            val nicoRepoAPI = NicoRepoAPIX()
            val response = nicoRepoAPI.getNicoRepoResponse(userSession, userId)
            // 失敗時
            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
            loadingLiveData.postValue(false)
            nicoRepoDataListRaw = nicoRepoAPI.parseNicoRepoResponse(response.body?.string())
            // フィルター適用とLiveData送信
            filterAndPostLiveData()
        }
    }

    /**
     * [isShowLive]、[isShowVideo]を適用してLiveDataへ送信する
     *
     * チェックが切り替わったら呼べばいいと思います。
     * */
    fun filterAndPostLiveData() {
        // 両方表示ならそのまま
        if (isShowLive && isShowVideo) {
            nicoRepoDataListLiveData.postValue(nicoRepoDataListRaw)
            return
        }
        val filterList = nicoRepoDataListRaw.filter { nicoRepoDataClass ->
            when {
                isShowVideo -> nicoRepoDataClass.isVideo
                isShowLive -> !nicoRepoDataClass.isVideo
                else -> false
            }
        }
        nicoRepoDataListLiveData.postValue(filterList as ArrayList<NicoRepoDataClass>?)
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    /** Toastを表示する */
    private fun showToast(s: String) {
        viewModelScope.launch {
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        }
    }

}