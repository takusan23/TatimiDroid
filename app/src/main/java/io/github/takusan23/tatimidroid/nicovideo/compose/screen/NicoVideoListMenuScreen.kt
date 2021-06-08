package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.FillTextButton
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData

/**
 * ニコ動一覧で使うメニュー。Composeでできている
 *
 * @param nicoVideoData 動画情報
 * */
@Composable
fun NicoVideoListMenuScreen(nicoVideoData: NicoVideoData) {
    val context = LocalContext.current
    val state = rememberCoroutineScope()

    // 各メニュー
    LazyColumn {
        item {
            // 動画情報
            NicoVideoListMenuScreenInfo(nicoVideoData = nicoVideoData)
            Divider()

            FillTextButton(onClick = { }) {
                Icon(painter = painterResource(id = R.drawable.ic_content_paste_black_24dp), contentDescription = null)
                Text(text = stringResource(id = R.string.video_id_copy))
            }
            if (nicoVideoData.isMylist) {
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null)
                    Text(text = stringResource(id = R.string.mylist_delete))
                }
            } else {
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_folder_open_black_24dp), contentDescription = null)
                    Text(text = stringResource(id = R.string.add_mylist))
                }
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_folder_open_black_24dp), contentDescription = null)
                    Text(text = stringResource(id = R.string.add_atodemiru))
                }
            }
            FillTextButton(
                onClick = { },
                modifier = Modifier.padding(5.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_comment_24px), contentDescription = null)
                Text(text = stringResource(id = R.string.comment_list_only))
            }
            if (nicoVideoData.isCache) {
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_tatimidroid_playlist_play_black), contentDescription = null)
                    Text(text = stringResource(id = R.string.nicovideo_cache_music_mode))
                }
            }
            FillTextButton(
                onClick = { },
                modifier = Modifier.padding(5.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_tatimidroid_list_icon_black), contentDescription = null)
                Text(text = stringResource(id = R.string.playlist_button))
            }
            FillTextButton(
                onClick = { },
                modifier = Modifier.padding(5.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_play_arrow_24px), contentDescription = null)
                Text(text = stringResource(id = R.string.play_video))
            }
            FillTextButton(
                onClick = { },
                modifier = Modifier.padding(5.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_play_arrow_24px), contentDescription = null)
                Text(text = stringResource(id = R.string.internet_play))
            }
            FillTextButton(
                onClick = { },
                modifier = Modifier.padding(5.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_money_24px), contentDescription = null)
                Text(text = stringResource(id = R.string.nicoads))
            }
            FillTextButton(
                onClick = { },
                modifier = Modifier.padding(5.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_popup_icon_black), contentDescription = null)
                Text(text = stringResource(id = R.string.popup_player))
            }
            FillTextButton(
                onClick = { },
                modifier = Modifier.padding(5.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_background_icon_black), contentDescription = null)
                Text(text = stringResource(id = R.string.background_play))
            }
            if (nicoVideoData.isCache) {
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_refresh_black_24dp), contentDescription = null)
                    Text(text = stringResource(id = R.string.get_cache_re_get))
                }
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_outline_delete_24px), contentDescription = null)
                    Text(text = stringResource(id = R.string.cache_delete))
                }
            } else {
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_cache_progress_icon), contentDescription = null)
                    Text(text = stringResource(id = R.string.get_cache))
                }
                FillTextButton(
                    onClick = { },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_cache_progress_icon), contentDescription = null)
                    Text(text = stringResource(id = R.string.get_cache_eco))
                }
            }
        }
    }
}

/**
 * 動画情報表示部分
 * */
@Composable
private fun NicoVideoListMenuScreenInfo(nicoVideoData: NicoVideoData) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // タイトル
            Text(
                text = nicoVideoData.title,
                style = TextStyle(fontSize = 18.sp),
            )
            // 生放送ID
            Text(
                text = nicoVideoData.videoId,
                style = TextStyle(fontSize = 12.sp),
            )
        }
    }
}

/**
 * 文字をコピーする関数
 * @param text コピーする文字列
 * */
private fun copyText(context: Context, text: String) {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText(text, text))
}
