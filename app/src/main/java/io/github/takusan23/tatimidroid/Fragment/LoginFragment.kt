package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.NicoAPI.NicoLogin
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class LoginFragment : Fragment() {
    lateinit var prefSetting: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        //保存していたら取得
        fragment_login_mail_inputedittext.setText(prefSetting.getString("mail", ""))
        fragment_login_password_inputedittext.setText(prefSetting.getString("password", ""))

        //おしたとき
        fragment_login_button.setOnClickListener {
            //ログイン
            lifecycleScope.launch(Dispatchers.Main) {
                val mail = fragment_login_mail_inputedittext.text.toString()
                val pass = fragment_login_password_inputedittext.text.toString()
                // ログインAPIを叩く
                val userSession = withContext(Dispatchers.Default) {
                    NicoLogin.nicoLoginCoroutine(mail, pass)
                }
                if (userSession != null) {
                    // 成功時
                    Toast.makeText(activity, getString(R.string.successful), Toast.LENGTH_SHORT).show()
                    //めあど、ぱすわーども保存する
                    prefSetting.edit {
                        putString("mail", mail)
                        putString("password", pass)
                        putString("user_session", userSession)
                        // もしログイン無しで利用するが有効の場合は無効にする
                        putBoolean("setting_no_login", false)
                    }
                } else {
                    // 失敗時
                    Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}