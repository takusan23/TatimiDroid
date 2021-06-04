package io.github.takusan23.tatimidroid.nicologin.viewmodel

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLoginDataClass
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLoginTwoFactorAuth
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

/**
 * 二段階認証の画面で使うViewModel
 * */
class NicoTwoFactorLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    /** ログイン完了コールバック */
    val loginSuccessfulLiveData = MutableLiveData<String>()

    /** ワンタイムパスワード。ほかから触れるようにLiveData */
    val otpLiveData = MutableLiveData("")

    /**
     * 二段階認証を完了させる関数
     *
     * @param nicoLoginDataClass ログイン画面でもらえる
     * @param otp ワンタイムパスワード
     * @param isTrustDevice 次回以降は２段階認証を省略する場合はtrue
     * */
    fun startTwoFactorLogin(nicoLoginDataClass: NicoLoginDataClass, otp: String, isTrustDevice: Boolean) {
        val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
            showToast("${getString(R.string.error)}\n${throwable}")
        }
        viewModelScope.launch(errorHandler) {
            val nicoLoginTwoFactorAuth = NicoLoginTwoFactorAuth(nicoLoginDataClass)
            val (userSession, trustDeviceToken) = nicoLoginTwoFactorAuth.twoFactorAuth(otp, isTrustDevice)
            // 保存
            PreferenceManager.getDefaultSharedPreferences(context).edit {
                putString("user_session", userSession)
                // デバイスを信頼している場合は次回からスキップできる値を保存
                putString("trust_device_token", trustDeviceToken)
            }
            // 成功した
            showToast(getString(R.string.successful))
            // コールバック
            loginSuccessfulLiveData.postValue(userSession)
        }
    }

    /** ワンタイムパスワードをLiveDataに送信する */
    fun setOTP(otp: String) = otpLiveData.postValue(otp)

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