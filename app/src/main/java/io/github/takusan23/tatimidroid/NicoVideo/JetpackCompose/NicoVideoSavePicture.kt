package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R

/**
 * ここすきBottomFragmentのタイトル
 * */
@Composable
fun KokosukiTitle() {
    // タイトル
    Text(
        text = "ここすき（画像として保存）",
        modifier = Modifier
            .padding(5.dp)
            .fillMaxWidth(),
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
    )
}

/**
 * ここすきBottomFragmentのImage。16：9
 *
 * @param bitmap nullのときは変わりの画像を表示させます
 * */
@Composable
fun KokosukiPreviewImage(bitmap: Bitmap?) {
    val imageModifier = Modifier
        .fillMaxWidth()
        .padding(10.dp)
        .aspectRatio(16f / 9f) // 16:9
    if (bitmap == null) {
        // 読込中
        Image(
            painter = painterResource(id = R.drawable.screen_shot_icon),
            contentDescription = "preview",
            modifier = imageModifier,
        )
    } else {
        // プレビュー
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "preview",
            modifier = imageModifier,
        )
    }
}

/**
 * ここすきBottomFragmentの設定。
 * */
@Composable
fun KokosukiSettingSwitch(isWriteVideoInfoAndDate: Boolean, onChange: (Boolean) -> Unit) {
    // 設定など
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(10.dp)
    ) {
        Text(
            text = "タイトル、ID、日付を右下に残す",
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = isWriteVideoInfoAndDate,
            onCheckedChange = { onChange(it) }
        )
    }
}

/**
 * ここすきBottomFragmentの保存ボタン
 * */
@Composable
fun KokosukiSaveButton(onClickSaveButton: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 保存
        Button(
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.CenterHorizontally),
            onClick = { onClickSaveButton() }
        ) {
            Icon(painter = painterResource(R.drawable.ic_folder_open_black_24dp), contentDescription = "save")
            Text(text = "保存する")
        }
    }
}