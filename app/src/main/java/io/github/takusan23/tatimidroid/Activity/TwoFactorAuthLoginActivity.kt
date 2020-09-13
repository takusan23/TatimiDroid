package io.github.takusan23.tatimidroid.Activity

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.Login.NicoLoginDataClass
import io.github.takusan23.tatimidroid.NicoAPI.Login.NicoLoginTwoFactorAuth
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.DarkModeSupport
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import kotlinx.android.synthetic.main.activity_two_factor_auth.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 二段階認証を完了させるActivity
 * */
class TwoFactorAuthLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ダークモード
        DarkModeSupport(this).setActivityTheme(this)
        setContentView(R.layout.activity_two_factor_auth)
        two_factor_auth_activity_parent.backgroundTintList = ColorStateList.valueOf(getThemeColor(this))

        // データを受け取る
        val loginData = intent.getSerializableExtra("login") as NicoLoginDataClass

        // 認証ボタン押す
        two_factor_auth_activity_login_button.setOnClickListener {

            // 認証コード
            val keyVisualArts = two_factor_auth_activity_key_input.text.toString()
            // このデバイスを信用する場合
            val isTrustDevice = two_factor_auth_activity_trust_check.isChecked

            // 例外
            val errorHandler = CoroutineExceptionHandler { coroutineContext, throwable ->
                runOnUiThread {
                    Toast.makeText(this, "${getString(R.string.error)}\n${throwable}", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            // 二段階認証開始
            lifecycleScope.launch(Dispatchers.Main + errorHandler) {
                val nicoLoginTwoFactorAuth = NicoLoginTwoFactorAuth(loginData)
                val (userSession, trustDeviceToken) = nicoLoginTwoFactorAuth.twoFactorAuth(keyVisualArts, isTrustDevice)

                // 保存
                PreferenceManager.getDefaultSharedPreferences(this@TwoFactorAuthLoginActivity).edit {
                    putString("user_session", userSession)
                    // デバイスを信頼している場合は次回からスキップできる値を保存
                    putString("trust_device_token", trustDeviceToken)
                    // もしログイン無しで利用するが有効の場合は無効にする
                    putBoolean("setting_no_login", false)
                }
                // ログインできたよ！
                Toast.makeText(this@TwoFactorAuthLoginActivity, getString(R.string.successful), Toast.LENGTH_SHORT).show()
                finish()
            }

        }
    }

}