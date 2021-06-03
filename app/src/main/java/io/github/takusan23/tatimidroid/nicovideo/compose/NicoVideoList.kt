package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.tool.TimeFormatTool

/**
 * 動画一覧表示で使うやつ
 *
 * @param list 動画情報の配列
 * @param onLastScroll 最後までスクロールすると呼ぶ。呼び続けますので各自APIを叩きすぎないように気をつけてください。
 * @param onVideoClick 押したとき
 * @param onMenuClick メニュー押したとき
 * */
@Composable
fun NicoVideoList(
    list: List<NicoVideoData>,
    onLastScroll: () -> Unit = {},
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit
) {
    // スクロール制御用
    val state = rememberLazyListState()
    // リスト表示数
    val showItemCount = state.layoutInfo.visibleItemsInfo.size
    // 最後判定
    val isLastedScroll = (list.size - showItemCount) == state.firstVisibleItemIndex
    if (isLastedScroll) {
        onLastScroll()
    }

    LazyColumn(state = state) {
        items(list) { data ->
            NicoVideoListItem(
                data,
                onVideoClick = { onVideoClick(it) },
                onMenuClick = { onMenuClick(it) }
            )
            Divider()
        }
    }
}

/**
 * 動画一覧表示で使う、一つ一つの部品
 *
 * @param nicoVideoData 動画情報
 * @param onVideoClick 押したとき
 * @param onMenuClick メニュー押したとき
 * */
@Composable
fun NicoVideoListItem(
    nicoVideoData: NicoVideoData,
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit
) {
    Surface(modifier = Modifier.clickable { onVideoClick(nicoVideoData) }) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            val thumb = getBitmapCompose(url = nicoVideoData.thum)?.asImageBitmap()

            // さむね
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(1.7f)
                    .clip(shape = RoundedCornerShape(5.dp))
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb,
                        contentDescription = nicoVideoData.thum,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                // 再生時間
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = TimeFormatTool.timeFormat(nicoVideoData.duration ?: 0),
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
            // タイトル名など
            Column(
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f)
            ) {
                Text(text = "${TimeFormatTool.unixTimeToFormatDateYearEdition(nicoVideoData.date)} 投稿", fontSize = 12.sp)
                Text(text = nicoVideoData.title, maxLines = 2, fontWeight = FontWeight.Bold)
                // コメント数など
                NicoVideoCountText(
                    viewCount = nicoVideoData.viewCount.toInt(),
                    commentCount = nicoVideoData.commentCount.toInt(),
                    mylistCount = nicoVideoData.mylistCount.toInt()
                )
            }
            // メニューボタン
            IconButton(onClick = { onMenuClick(nicoVideoData) }) {
                Icon(painter = painterResource(id = R.drawable.ic_more_vert_24px), contentDescription = "メニュー")
            }

        }
    }
}