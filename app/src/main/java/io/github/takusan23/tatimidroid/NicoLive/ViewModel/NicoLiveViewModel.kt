package io.github.takusan23.tatimidroid.NicoLive.ViewModel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.NicoLiveHTML
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Room.Entity.NicoHistoryDBEntity
import io.github.takusan23.tatimidroid.Room.Init.NicoHistoryDBInit
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * [io.github.takusan23.tatimidroid.NicoLive.CommentFragment]のViewModel
 * @param liveId 番組ID
 * @param isJK 実況の時はtrue
 * @param isLoginMode HTML取得時にログインする場合はtrue
 * */
class NicoLiveViewModel(application: Application, val liveId: String,val isLoginMode:Boolean, val isJK: Boolean) : AndroidViewModel(application) {

    /** Context */
    private val context = getApplication<Application>().applicationContext

    /** 設定 */
    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ニコニコのログイン情報。ユーザーセッション */
    private var userSession = prefSetting.getString("user_session", "") ?: ""

    /** HTML取得からWebSocket接続など */
    val nicoLiveHTML = NicoLiveHTML()

    /** Snackbar表示用LiveData */
    val snackbarLiveData = MutableLiveData<String>()

    /** Fragment(Activity)へメッセージを送信するためのLiveData。Activity終了など */
    val messageLiveData = MutableLiveData<String>()

    /** ニコ生のHTML内にあるJSONを入れる */
    val nicoLiveJSON = MutableLiveData<JSONObject>()

    /** 番組名 */
    var  programTitle = ""

    /** コミュID */
    var communityId = ""

    /** サムネURL */
    var thumbnailURL = ""

    init {
        // エラーのとき（タイムアウトなど）はここでToastを出すなど
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler + Dispatchers.Default) {
            // 情報取得
            val html = getNicoLiveHTML()
            val jsonObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(html)
            nicoLiveJSON.postValue(jsonObject)
            // 番組名取得など
            nicoLiveHTML.initNicoLiveData(jsonObject)
            programTitle = nicoLiveHTML.programTitle
            communityId = nicoLiveHTML.communityId
            thumbnailURL = nicoLiveHTML.thumb
            // 履歴に追加
            insertDB()
        }
    }

    /** 履歴DBに入れる */
    private fun insertDB() {
        viewModelScope.launch(Dispatchers.IO) {
            val unixTime = System.currentTimeMillis() / 1000
            // 入れるデータ
            val nicoHistoryDBEntity = NicoHistoryDBEntity(
                type = "video",
                serviceId = liveId,
                userId = communityId,
                title = programTitle,
                unixTime = unixTime,
                description = ""
            )
            // 追加
            NicoHistoryDBInit.getInstance(context).nicoHistoryDBDAO().insert(nicoHistoryDBEntity)
        }
    }

    /** ニコ生放送ページのHTML取得。コルーチンです */
    private suspend fun getNicoLiveHTML(): String? = withContext(Dispatchers.Default) {
        // ニコ生視聴ページリクエスト
        val livePageResponse = nicoLiveHTML.getNicoLiveHTML(liveId, userSession, isLoginMode)
        if (!livePageResponse.isSuccessful) {
            // 失敗のときは落とす
            messageLiveData.postValue("finish")
            showToast("${getString(R.string.error)}\n${livePageResponse.code}")
            null
        }
        if (!nicoLiveHTML.hasNiconicoID(livePageResponse)) {
            // niconicoIDがない場合（ログインが切れている場合）はログインする（この後の処理でユーザーセッションが必要）
            userSession = NicoLogin.reNicoLogin(context)
            // 視聴モードなら再度視聴ページリクエスト
            if (isLoginMode) {
                getNicoLiveHTML()
            }
        }
        livePageResponse.body?.string()
    }

    /** Toast表示関数 */
    private fun showToast(message: String?) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /** Context.getStringを短く */
    private fun getString(resourceId: Int): String {
        return context.getString(resourceId)
    }

    override fun onCleared() {
        super.onCleared()
        nicoLiveHTML.destroy()
    }
}