package io.github.takusan23.tatimidroid.NicoVideo.ViewModel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.User.UserAPI
import io.github.takusan23.tatimidroid.NicoAPI.User.UserData
import io.github.takusan23.tatimidroid.R
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * [io.github.takusan23.tatimidroid.NicoVideo.NicoAccountFragment]で使うViewModel
 *
 * @param userId ユーザーID。いつの間にか1億ID突破してた。nullを入れると自分のアカウント情報を取りに行きます。
 * */
class NicoAccountViewModel(application: Application, userId: String?) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** ユーザー情報を送るLiveData */
    val userDataLiveData = MutableLiveData<UserData>()

    init {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${context.getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val userData = withContext(Dispatchers.IO) {
                val userAPI = UserAPI()
                // userIdがnullなら自分の情報を取りに行く
                val response = if (userId == null) {
                    userAPI.getMyAccountUserData(userSession)
                } else {
                    userAPI.getUserData(userSession, userId)
                }
                userAPI.parseUserData(response.body?.string())
            }
            userDataLiveData.value = userData
        }
    }

    private fun showToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }

}