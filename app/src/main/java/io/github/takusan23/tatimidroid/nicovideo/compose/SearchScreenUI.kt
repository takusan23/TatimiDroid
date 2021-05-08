package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.room.entity.SearchHistoryDBEntity
import java.text.SimpleDateFormat


/**
 * キーワード、タグ検索切り替えトグルボタン
 *
 * @param isTag タグ検索時はtrue。キーワード検索時はfalse
 * @param onChange 切り替えたら呼ばれる関数。trueならタグ検索に切り替わった
 * */
@Composable
fun SearchTagOrKeyword(
    isTag: Boolean,
    onChange: (Boolean) -> Unit
) {
    Surface(
        border = ButtonDefaults.outlinedBorder,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.requiredHeight(IntrinsicSize.Min)
        ) {
            IconButton(onClick = { onChange(false) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_font_download_24px),
                    contentDescription = "tag",
                    tint = if (!isTag) Color.Blue else LocalContentColor.current,
                )
            }
            // 区切り線
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .requiredWidth(1.dp)
                    .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
            )
            IconButton(onClick = { onChange(true) }) {
                Icon(
                    modifier = Modifier.padding(10.dp),
                    painter = painterResource(id = R.drawable.ic_local_offer_24px),
                    contentDescription = "tag",
                    tint = if (isTag) Color.Blue else LocalContentColor.current,
                )
            }
        }
    }
}

/**
 * 検索履歴一覧表示
 *
 * @param list 検索履歴
 * */
@Composable
fun SearchHistoryList(
    list: List<SearchHistoryDBEntity>,
    onClickSearchHistory: (SearchHistoryDBEntity) -> Unit
) {
    // 日付のフォーマット
    val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(list) { history ->
            // 項目押したとき
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onClickSearchHistory(history) }
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // タグなのかキーワードなのか
                Icon(
                    painter = if (history.isTagSearch) {
                        painterResource(id = R.drawable.ic_local_offer_24px)
                    } else {
                        painterResource(id = R.drawable.ic_font_download_24px)
                    },
                    contentDescription = "pinned"
                )
                // 検索ワード、検索日時
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = history.text)
                    Text(text = "${history.sort} / ${simpleDateFormat.format(history.addTime)}")
                }
                // ピン止めしている場合
                if (history.pin) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_push_pin_black_24dp),
                        contentDescription = "pinned"
                    )
                }
            }
            Divider()
        }
    }
}