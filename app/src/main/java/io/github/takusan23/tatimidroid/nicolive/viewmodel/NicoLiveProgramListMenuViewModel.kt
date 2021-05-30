package io.github.takusan23.tatimidroid.nicolive.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveHTML
import io.github.takusan23.tatimidroid.nicoapi.nicolive.NicoLiveTimeShiftAPI
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.CommunityOrChannelData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ニコ生の番組一覧のメニュー押して表示するメニューで使うViewModel
 *
 * @param liveId 番組ID
 * */
class NicoLiveProgramListMenuViewModel(application: Application, private val liveId: String) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** ユーザーセッション */
    private val userSession = prefSetting.getString("user_session", "") ?: ""

    /** クリップボード */
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /** ニコ生情報取得 */
    private val nicoLiveHTML = NicoLiveHTML()

    /** タイムシフトAPI */
    private val timeShiftAPI = NicoLiveTimeShiftAPI()

    /** コミュ情報 */
    private var communityOrChannelData: CommunityOrChannelData? = null

    /** 番組情報を送信するLiveData */
    val programDataLiveData = MutableLiveData<NicoLiveProgramData>()

    init {
        // データ取得
        getProgramData()
    }

    /**
     * 番組情報を取得する。結果はLiveDataへ送信されます。
     * @param liveId 番組ID
     * */
    private fun getProgramData() {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            // エラー
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val response = nicoLiveHTML.getNicoLiveHTML(liveId, userSession)
            if (!response.isSuccessful) {
                // 失敗時
                showToast("${getString(R.string.error)}\n${response.code}")
                return@launch
            }
            withContext(Dispatchers.Default) {
                val jsonObject = nicoLiveHTML.nicoLiveHTMLtoJSONObject(response.body?.string())
                programDataLiveData.postValue(nicoLiveHTML.getProgramData(jsonObject))
                communityOrChannelData = nicoLiveHTML.getCommunityOrChannelData(jsonObject)
            }
        }
    }

    /** コミュIDをコピーする関数 */
    fun copyCommunityId() {
        if (communityOrChannelData != null) {
            copyText(communityOrChannelData!!.id)
            showToast("${getString(R.string.copy_communityid)}：${communityOrChannelData!!.id}")
        }
    }

    /** 番組IDをコピーする関数 */
    fun copyProgramId() {
        copyText(liveId)
        showToast("${getString(R.string.copy_program_id)}：$liveId")
    }

    /**
     * タイムシフト登録、登録解除する
     *
     * 登録済みの場合は解除し、未登録時は登録します
     * */
    fun registerOrUnRegisterTimeShift() {
        // エラー時
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val registerResponse = timeShiftAPI.registerTimeShift(liveId, userSession)
            if (registerResponse.isSuccessful) {
                // 成功
                showToast(getString(R.string.timeshift_reservation_successful))
            } else if (registerResponse.code == 500) {
                // 登録解除する
                val unRegisterResponse = timeShiftAPI.deleteTimeShift(liveId, userSession)
                if (unRegisterResponse.isSuccessful) {
                    // 登録解除成功
                    showToast(getString(R.string.timeshift_delete_reservation_successful))
                } else {
                    showToast("${getString(R.string.error)}\n${unRegisterResponse.code}")
                }
            }
        }
    }

    /**
     * 文字をコピーする関数
     * @param text コピーする文字列
     * */
    private fun copyText(text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(text, text))
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

}