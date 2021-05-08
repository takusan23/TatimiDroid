package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.TagButton
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoTagItemData

/**
 * 検索時に出る関連タグを表示する
 *
 * @param tagList タグの配列
 * */
@Composable
fun SearchResultTagGroup(
    tagList: List<NicoTagItemData>
) {
    // 横スクロールできるやつ
    LazyRow {
        items(tagList) { tagData ->
            TagButton(
                data = tagData,
                onClickTag = { },
                onClickNicoPedia = { },
            )
        }
    }
}

/**
 * 検索結果のToolBar
 *
 * @param onBackScreen 戻るボタンを押したとき
 * */
@Composable
fun SearchResultAppBar(
    onBackScreen: () -> Unit,
) {
    Surface(
        elevation = 5.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // 戻るボタンなど
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBackScreen() }) {
                    Icon(painter = painterResource(id = R.drawable.ic_arrow_back_black_24dp), contentDescription = "戻る")
                }
                Text(text = "検索結果")
            }
            // 並び変えとタグ、キーワード切り替え
            Row {
                SearchTagOrKeyword(
                    isTag = false,
                    onChange = { }
                )
                OutlinedButton(
                    modifier = Modifier.padding(2.dp),
                    onClick = { },
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_sort_black_24dp), contentDescription = "並び変え")
                    Text(text = "並び変え")
                }
            }
        }
    }
}

/**
 * タグを横に表示する
 * */
@Composable
fun SearchResultTagList(tagList: List<NicoTagItemData>) {
    LazyRow {
        items(tagList) { tag ->
            TagButton(
                data = tag,
                onClickTag = { },
                onClickNicoPedia = { },
            )
        }
    }
}