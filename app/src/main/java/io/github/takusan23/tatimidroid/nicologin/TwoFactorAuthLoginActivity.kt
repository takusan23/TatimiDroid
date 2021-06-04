package io.github.takusan23.tatimidroid.nicologin

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.ui.platform.LocalContext
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLoginDataClass
import io.github.takusan23.tatimidroid.nicologin.compose.NicoTwoFactorLoginScreen
import io.github.takusan23.tatimidroid.nicologin.viewmodel.NicoTwoFactorLoginViewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.DarkColors
import io.github.takusan23.tatimidroid.nicovideo.compose.LightColors
import io.github.takusan23.tatimidroid.tool.LanguageTool
import io.github.takusan23.tatimidroid.tool.isDarkMode

/**
 * 二段階認証を完了させるActivity
 * */
class TwoFactorAuthLoginActivity : ComponentActivity() {

    private val viewModel by viewModels<NicoTwoFactorLoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colors = if (isDarkMode(LocalContext.current)) DarkColors else LightColors) {
                Surface {
                    // データを受け取る
                    val loginData = intent.getSerializableExtra("login") as NicoLoginDataClass

                    NicoTwoFactorLoginScreen(viewModel = viewModel, nicoLoginDataClass = loginData)
                }
            }
        }

        // ログイン成功コールバック
        viewModel.loginSuccessfulLiveData.observe(this) {
            finish()
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
                viewModel.setOTP(clipboardText.toString())
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