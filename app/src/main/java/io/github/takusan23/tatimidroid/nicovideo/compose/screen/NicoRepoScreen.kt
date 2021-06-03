package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.ComposeCheckableChip
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.compose.PlaceholderImage
import io.github.takusan23.tatimidroid.nicoapi.nicorepo.NicoRepoDataClass
import io.github.takusan23.tatimidroid.nicovideo.compose.getBitmapCompose
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoRepoViewModel

/**
 * ニコレポ画面。Composeでできている
 *
 * @param viewModel ニコレポViewModel
 * @param onClickNicoRepo ニコレポ押したら呼ばれる
 * */
@Composable
fun NicoRepoScreen(
    viewModel: NicoRepoViewModel,
    onClickNicoRepo: (NicoRepoDataClass) -> Unit
) {
    // ニコレポデータ
    val isLoading = viewModel.loadingLiveData.observeAsState(initial = true)
    // ニコレポのデータ配列
    val nicorepoDataList = viewModel.nicoRepoDataListLiveData.observeAsState()

    if (isLoading.value || nicorepoDataList.value == null) {
        FillLoadingScreen()
    } else {
        Column {
            // フィルター
            NicoRepoFilter(
                isVideoOnly = true,
                isLiveOnly = true,
                onVideoOnlyChange = { },
                onLiveOnlyChange = { }
            )
            Divider()
            // 一覧表示
            NicoRepoList(
                list = nicorepoDataList.value!!,
                onClickNicoRepo = { onClickNicoRepo(it) }
            )
        }

    }

}

/**
 * ニコレポのフィルター。生放送だけとか動画だけとか
 * */
@Composable
private fun NicoRepoFilter(
    isVideoOnly: Boolean,
    isLiveOnly: Boolean,
    onVideoOnlyChange: (Boolean) -> Unit,
    onLiveOnlyChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        ComposeCheckableChip(
            modifier = Modifier.padding(5.dp),
            isChecked = isVideoOnly,
            onCheckedChange = { onVideoOnlyChange(!isVideoOnly) },
            label = {
                Text(text = "生放送")
            }
        )
        ComposeCheckableChip(
            modifier = Modifier.padding(5.dp),
            isChecked = isLiveOnly,
            onCheckedChange = { onLiveOnlyChange(!isLiveOnly) },
            label = {
                Text(text = "動画")
            }
        )
    }
}

/**
 * ニコレポ一覧表示
 *
 * @param list ニコレポデータの配列
 * @param onClickNicoRepo ニコレポ押したときに呼ばれる
 * */
@Composable
private fun NicoRepoList(
    list: List<NicoRepoDataClass>,
    onClickNicoRepo: (NicoRepoDataClass) -> Unit
) {
    LazyColumn {
        items(list) { nicorepo ->
            NicoRepoListItem(
                nicoRepoDataClass = nicorepo,
                onClickNicoRepo = { onClickNicoRepo(it) }
            )
            Divider()
        }
    }
}

/**
 * ニコレポ一覧の各レイアウト
 *
 * @param nicoRepoDataClass ニコレポデータクラス
 * @param onClickNicoRepo ニコレポ押したときに呼ばれる
 * */
@Composable
private fun NicoRepoListItem(
    nicoRepoDataClass: NicoRepoDataClass,
    onClickNicoRepo: (NicoRepoDataClass) -> Unit
) {
    // ユーザーのアイコン画像取得
    val userIconThumb = getBitmapCompose(url = nicoRepoDataClass.userIcon)?.asImageBitmap()
    // 投稿動画のサムネ画像取得
    val contentThumb = getBitmapCompose(url = nicoRepoDataClass.thumbUrl)?.asImageBitmap()
    // アイコンサイズ
    val userIconDp = 50.dp

    Surface(modifier = Modifier.clickable { onClickNicoRepo(nicoRepoDataClass) }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlaceholderImage(
                    modifier = Modifier
                        .size(userIconDp)
                        .padding(2.dp)
                        .clip(RoundedCornerShape(10)),
                    isLoading = userIconThumb == null,
                    imageBitmap = userIconThumb,
                    placeHolder = { Box(modifier = Modifier.background(Color.Gray)) }
                )
                Column {
                    Text(text = nicoRepoDataClass.userName)
                    Text(text = HtmlCompat.fromHtml(nicoRepoDataClass.message, HtmlCompat.FROM_HTML_MODE_COMPACT).toString())
                }
            }
            Row(
                modifier = Modifier.padding(start = userIconDp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaceholderImage(
                    modifier = Modifier
                        .padding(2.dp)
                        .width(100.dp)
                        .aspectRatio(1.7f),
                    isLoading = contentThumb == null,
                    imageBitmap = contentThumb,
                    placeHolder = { Box(modifier = Modifier.background(Color.Gray)) }
                )
                Column {
                    NicoRepoContentLabel(nicoRepoDataClass.isVideo)
                    Text(text = nicoRepoDataClass.title, fontSize = 18.sp)
                }
            }
        }
    }
}

/**
 * 動画、生放送を表示するラベル
 * @param isVideo 動画ならtrue
 * */
@Composable
fun NicoRepoContentLabel(isVideo: Boolean) {
    Surface(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(5.dp),
        border = BorderStroke(1.dp, MaterialTheme.colors.primary)
    ) {
        Row(modifier = Modifier.padding(start = 5.dp, end = 5.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = if (isVideo) painterResource(id = R.drawable.video_icon) else painterResource(id = R.drawable.live_icon),
                contentDescription = null
            )
            Text(text = if (isVideo) stringResource(id = R.string.video) else stringResource(id = R.string.nicolive))
        }
    }
}