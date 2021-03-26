package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.KokosukiViewModel
import io.github.takusan23.tatimidroid.NicoVideo.ViewModel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.R

/**
 * ここすき機能（動画スクショ機能）
 *
 * @param viewModel ここすきViewModel
 * @param onClickSaveButton 保存ボタン押したとき
 * */
@Composable
fun NicoVideoSavePictureScreen(viewModel: KokosukiViewModel, onClickSaveButton: () -> Unit) {
    val bitmap = viewModel.makeBitmapLiveData.observeAsState()
    val isWriteVideoInfoAndDate = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // タイトル
        KokosukiTitle()
        Divider()
        // プレビュー
        KokosukiPreviewImage(bitmap = bitmap.value)
        // 設定など
        KokosukiSettingSwitch(isWriteVideoInfoAndDate = isWriteVideoInfoAndDate.value) {
            isWriteVideoInfoAndDate.value = it
            viewModel.makeBitmap(it)
        }
        // 保存ボタン
        KokosukiSaveButton {
            viewModel.saveBitmapToMediaStore()
            onClickSaveButton()
        }
    }
}