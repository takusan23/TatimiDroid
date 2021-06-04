package io.github.takusan23.tatimidroid.nicologin.compose

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.login.NicoLoginDataClass
import io.github.takusan23.tatimidroid.nicologin.viewmodel.NicoTwoFactorLoginViewModel

/**
 * ニコニコの二段階認証のワンタイムパスワード入力画面
 *
 * @param viewModel 二段階認証画面ViewModel
 * @param nicoLoginDataClass 二段階認証で使うデータクラス
 * */
@Composable
fun NicoTwoFactorLoginScreen(viewModel: NicoTwoFactorLoginViewModel, nicoLoginDataClass: NicoLoginDataClass) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ワンタイムパスワード
        val keyVisualArts = viewModel.otpLiveData.observeAsState(initial = "")
        // デバイスを信頼するか
        val isTrustDevice = remember { mutableStateOf(false) }

        // アプリ開いてねとかの文字
        NicoTwoFactorLoginMessage()
        // ワンタイムパスワード入力欄
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            value = keyVisualArts.value,
            onValueChange = { viewModel.setOTP(it) },
            label = { Text(text = stringResource(id = R.string.two_factor_auth_code)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        // 信頼するか
        NicoTwoFactorTrustCheckbox(
            isTrust = isTrustDevice.value,
            onTrustChange = { isTrustDevice.value = it }
        )
        // 認証ボタン
        OutlinedButton(
            onClick = {
                viewModel.startTwoFactorLogin(nicoLoginDataClass = nicoLoginDataClass, otp = keyVisualArts.value, isTrustDevice = isTrustDevice.value)
            }
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_lock_outline_black_24dp), contentDescription = null)
            Text(text = stringResource(id = R.string.two_factor_auth_start))
        }
    }
}

/**
 * 二段階認証が必要メッセージを表示する
 * */
@Composable
private fun NicoTwoFactorLoginMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painter = painterResource(id = R.drawable.ic_security), contentDescription = null)
        Text(text = stringResource(id = R.string.two_factor_auth), fontSize = 24.sp)
        Text(text = stringResource(id = R.string.need_two_factor_auth))
        Text(text = stringResource(id = R.string.how_to_get_two_factor_auth_code))
    }
}

/**
 * このデバイスを信頼するチェックボックス
 * @param isTrust 信頼する場合はtrue
 * @param onTrustChange チェックボックスの値が変わったら呼ばれる
 * */
@Composable
private fun NicoTwoFactorTrustCheckbox(
    isTrust: Boolean,
    onTrustChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.clickable { onTrustChange(!isTrust) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.two_factor_auth_trust_device),
            Modifier
                .padding(5.dp)
                .weight(1f)
        )
        Checkbox(
            modifier = Modifier.padding(5.dp),
            checked = isTrust,
            onCheckedChange = { onTrustChange(!isTrust) }
        )
    }
}