package io.github.takusan23.tatimidroid.Activity

import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
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
import io.github.takusan23.tatimidroid.Tool.LanguageTool
import io.github.takusan23.tatimidroid.Tool.getThemeColor
import io.github.takusan23.tatimidroid.Tool.isDarkMode
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
        if (isDarkMode(this)) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(getThemeColor(this)))
        }
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

    /**
     * クリップボードに認証コードがある場合は貼り付ける
     * Android 11から画面内の文字をコピーできるように（選択可能）なったけどもしかしてワンタイムパスワードのために付いた機能か！？
     * */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipdata = clipboard.primaryClip
        if (clipdata?.getItemAt(0)?.text != null) {
            val clipboardText = clipdata.getItemAt(0).text
            if (clipboardText.matches(Regex("[0-9]+"))) {
                // 正規表現で数字だったときは貼り付ける
                two_factor_auth_activity_key_input.setText(clipboardText)
            }
        }
    }

    /**
     * 言語変更機能をつける
     * 端末の設定で日本語でもこのアプリだけ英語で使うみたいな使い方ができます。
     * */
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageTool.setLanguageContext(newBase))
    }

}