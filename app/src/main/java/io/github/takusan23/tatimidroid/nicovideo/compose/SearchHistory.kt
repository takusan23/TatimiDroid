package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.room.entity.SearchHistoryDBEntity
import java.text.SimpleDateFormat

/**
 * 検索履歴一覧表示
 *
 * @param list 検索履歴
 * @param onClickSearchHistory 検索履歴を押したとき。
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
            Surface(
                modifier = Modifier
                    .clickable(indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { onClickSearchHistory(history) }
                    )
            ) {
                // 項目押したとき
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
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
                    IconButton(onClick = {}) {
                        Icon(
                            painter = if (history.pin) {
                                painterResource(id = R.drawable.ic_baseline_done_24)
                            } else {
                                painterResource(id = R.drawable.ic_push_pin_black_24dp)
                            },
                            contentDescription = "pinned"
                        )
                    }
                }
            }
            Divider()
        }
    }
}