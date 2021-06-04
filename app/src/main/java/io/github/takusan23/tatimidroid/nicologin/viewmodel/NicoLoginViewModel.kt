package io.github.takusan23.tatimidroid.nicologin.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLogin
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLoginDataClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ニコニコログイン画面で使うViewModel
 * */
class NicoLoginViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    /** 二段階認証開始LiveData */
    val twoFactorAuthLiveData = MutableLiveData<NicoLoginDataClass>()

    /** ログイン終了コールバック */
    val loginSuccessfulLiveData = MutableLiveData<String>()

    /** メアド。すでにログインしている場合のみ */
    val mail = prefSetting.getString("mail", "") ?: ""

    /** パスワード。すでにログインしている場合のみ */
    val password = prefSetting.getString("password", "") ?: ""

    /**
     * ログインをする。二段階認証が設定されている場合は[twoFactorAuthLiveData]へ値が送信される
     *
     * @param mail メアド
     * @param pass パスワード
     * */
    fun login(mail: String, pass: String) {
        viewModelScope.launch {
            // メアドを保存する
            NicoLogin.saveMailPassPreference(context, mail, pass)
            // 二段階認証時以外ならnull。二段階認証時でもデバイスを信頼してない場合はnull。それ以外なら信頼されてるとして、二段階認証時をパスできる。
            val trustDeviceToken = prefSetting.getString("trust_device_token", null)
            // ログインAPIを叩く
            val nicoLoginResult = NicoLogin.nicoLoginCoroutine(mail, pass, trustDeviceToken)
            if (nicoLoginResult != null) {
                if (!nicoLoginResult.isNeedTwoFactorAuth) {
                    // 二段階認証未設定時
                    prefSetting.edit {
                        putString("user_session", nicoLoginResult.userSession)
                    }
                    // ログイン成功したので
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.successful, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    twoFactorAuthLiveData.postValue(nicoLoginResult)
                }
            } else {
                withContext(Dispatchers.Main) {
                    // しっぱい
                    Toast.makeText(context, R.string.login_error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}