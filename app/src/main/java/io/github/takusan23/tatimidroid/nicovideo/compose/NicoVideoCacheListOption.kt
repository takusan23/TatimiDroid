package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.FillTextButton

/**
 * キャッシュ一覧で使う画面。
 *
 * @param isSaveDevice 保存先が端末の場合はtrue
 * @param usingGB キャッシュ使用容量。
 * @param onCacheMusicModeClick キャッシュ用音楽モードを押したときに呼ばれる
 * @param onPlaylistClick 連続再生を押したときに呼ばれる
 * */
@Composable
fun NicoVideoCacheListOption(
    usingGB: String,
    isSaveDevice: Boolean = true,
    onPlaylistClick: () -> Unit,
    onCacheMusicModeClick: () -> Unit
) {
    Column {
        Row {
            FillTextButton(
                onClick = { onPlaylistClick() },
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_tatimidroid_list_icon_black), contentDescription = null)
                Text(text = stringResource(id = R.string.playlist_button))
            }
            FillTextButton(
                onClick = { onCacheMusicModeClick() },
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_tatimidroid_playlist_play_black), contentDescription = null)
                Text(text = stringResource(id = R.string.nicovideo_cache_music_mode))
            }
        }
        // 保存先、キャッシュ容量の表示
        Row {
            NicoVideoCacheListOptionCard(
                modifier = Modifier.weight(1f),
                text = "${stringResource(id = R.string.cache_usage)} : $usingGB GB",
                icon = painterResource(id = R.drawable.ic_data_usage_black_24dp)
            )
            NicoVideoCacheListOptionCard(
                modifier = Modifier.weight(1f),
                text = if (isSaveDevice) stringResource(id = R.string.nicovideo_cache_storage_device) else stringResource(id = R.string.nicovideo_cache_storage_sd_card),
                icon = painterResource(id = R.drawable.ic_folder_open_black_24dp)
            )
        }
    }
}

/**
 * キャッシュ保存先、使用中の容量を表示する角丸いやつ
 * @param modifier paddingなど
 * @param icon アイコン
 * @param text テキスト
 * */
@Composable
private fun NicoVideoCacheListOptionCard(
    modifier: Modifier = Modifier,
    text: String,
    icon: Painter
) {
    Card(
        modifier = modifier.padding(5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.primary)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Icon(painter = icon, contentDescription = null)
            Text(text = text)
        }
    }
}