package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.User.User
import io.github.takusan23.tatimidroid.NicoAPI.User.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [io.github.takusan23.tatimidroid.NicoVideo.NicoAccountFragment]で使うViewModel
 * */
class NicoAccountViewModel(application: Application, userId: String) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** ユーザー情報を送るLiveData */
    val userDataLiveData = MutableLiveData<UserData>()

    init {
        viewModelScope.launch {
            val userData = withContext(Dispatchers.IO) {
                val userAPI = User()
                val response = userAPI.getUserCoroutine(userSession, userId)
                userAPI.parseUserData(response.body?.string())
            }
            userDataLiveData.value = userData
        }
    }

}