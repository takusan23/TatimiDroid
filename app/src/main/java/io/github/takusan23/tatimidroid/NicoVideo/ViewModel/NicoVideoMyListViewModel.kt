package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.NicoVideoSPMyListAPI
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [io.github.takusan23.tatimidroid.NicoVideo.VideoList.NicoVideoMyListFragment]で使うViewModel
 *
 * マイリスト一覧を取得する
 * @param userId ない場合は自分のマイリストを取りに行きます
 * */
class NicoVideoMyListViewModel(application: Application, val userId: String? = null) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** マイリスト一覧を送信するLiveData */
    val myListDataLiveData = MutableLiveData<ArrayList<NicoVideoSPMyListAPI.MyListData>>()

    init {
        getMyListList()
    }

    /**
     * マイリスト一覧を取得する
     * */
    fun getMyListList() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.IO) {
            val spMyListAPI = NicoVideoSPMyListAPI()
            val response = if (userId == null) {
                // 自分
                spMyListAPI.getMyListList(userSession)
            } else {
                // 他の人
                spMyListAPI.getMyListList(userSession, userId)
            }
            // LiveData送信
            myListDataLiveData.postValue(spMyListAPI.parseMyListList(response.body?.string(), userId == null))
        }
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    private fun showToast(s: String) {
        viewModelScope.launch {
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
        }
    }

}