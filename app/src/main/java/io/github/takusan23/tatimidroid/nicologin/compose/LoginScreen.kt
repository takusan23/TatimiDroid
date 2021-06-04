package io.github.takusan23.tatimidroid.nicologin.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLoginDataClass
import io.github.takusan23.tatimidroid.nicologin.viewmodel.NicoLoginViewModel

/**
 * ニコニコログイン画面。Composeでできている
 *
 * @param viewModel ログイン画面ViewModel
 * @param onTwoFactorLogin 二段階認証が必要になったら呼ばれます
 * */
@Composable
fun NicoLoginScreen(viewModel: NicoLoginViewModel, onTwoFactorLogin: (NicoLoginDataClass) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        val mail = remember { mutableStateOf(viewModel.mail) }
        val pass = remember { mutableStateOf(viewModel.password) }
        val isShowPassword = remember { mutableStateOf(false) }

        // 二段階認証が必要？
        val twoFactorLoginData = viewModel.twoFactorAuthLiveData.observeAsState()
        if (twoFactorLoginData.value != null) {
            onTwoFactorLogin(twoFactorLoginData.value!!)
        }

        OutlinedTextField(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            value = mail.value,
            label = { Text(text = stringResource(id = R.string.mail)) },
            onValueChange = { mail.value = it }
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            value = pass.value,
            visualTransformation = if (isShowPassword.value) VisualTransformation.None else PasswordVisualTransformation(),
            label = { Text(text = stringResource(id = R.string.pass)) },
            trailingIcon = {
                IconButton(onClick = { isShowPassword.value = !isShowPassword.value }) {
                    Icon(painter = painterResource(id = R.drawable.ic_baseline_visibility_24), contentDescription = "パスワード表示")
                }
            },
            onValueChange = { pass.value = it },
            keyboardOptions = KeyboardOptions()
        )
        // ログインボタン
        OutlinedButton(
            modifier = Modifier.padding(10.dp),
            onClick = {
                viewModel.login(mail = mail.value, pass = pass.value)
            }
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_lock_outline_black_24dp), contentDescription = null)
            Text(text = stringResource(id = R.string.login))
        }
    }
}