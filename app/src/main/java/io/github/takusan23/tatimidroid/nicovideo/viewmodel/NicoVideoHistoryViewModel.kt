package io.github.takusan23.tatimidroid.nicovideo.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLogin
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.NicoVideoHistoryAPI
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ニコ動の視聴履歴ViewModel
 * */
class NicoVideoHistoryViewModel(application: Application) : AndroidViewModel(application) {
    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private var userSession = prefSetting.getString("user_session", "") ?: ""

    /** 視聴履歴API */
    private val nicoVideoHistoryAPI = NicoVideoHistoryAPI()

    /** 視聴履歴を送信するLiveData */
    val historyListLiveData = MutableLiveData<ArrayList<NicoVideoData>>()

    /** 読み込み中LiveData */
    val loadingLiveData = MutableLiveData(false)

    init {
        getHistoryList()
    }

    /** 視聴履歴を取得する */
    fun getHistoryList() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.Default) {
            loadingLiveData.postValue(true)
            val response = nicoVideoHistoryAPI.getHistory(userSession)
            when (response.code) {
                in 200..299 -> {
                    // 成功
                    historyListLiveData.postValue(nicoVideoHistoryAPI.parseHistoryJSONParse(response.body?.string()))
                }
                401 -> {
                    // 再ログインする
                    userSession = NicoLogin.secureNicoLogin(context) ?: return@launch
                    getHistoryList()
                }
                else -> showToast("${getString(R.string.error)}\n${response.code}")
            }
            loadingLiveData.postValue(false)
        }
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    private fun showToast(s: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        }
    }

}