package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoRankingViewModel

/**
 * ニコ動ランキング画面。Composeでできている
 *
 * @param viewModel ランキングViewModel
 * @param onClickVideo 動画押したときに呼ばれる
 * */
@Composable
fun NicoVideoRankingScreen(
    viewModel: NicoVideoRankingViewModel,
    onClickVideo: (NicoVideoData) -> Unit,
    onClickMenu: (NicoVideoData) -> Unit
) {

    // 関連タグ一覧
    val tagList = viewModel.rankingTagList.observeAsState()
    // 動画一覧
    val videoList = viewModel.rankingVideoList.observeAsState()

    Column {
        Row {
            Button(onClick = { }, modifier = Modifier.padding(2.dp)) {
                Text(text = viewModel.lastOpenGenre)
            }
            Button(onClick = { }, modifier = Modifier.padding(2.dp)) {
                Text(text = viewModel.lastOpenTime)
            }
            // 関連タグ
            if (tagList.value != null) {
                LazyRow {
                    items(tagList.value!!) { tag ->
                        OutlinedButton(onClick = { }, modifier = Modifier.padding(2.dp)) {
                            Text(text = tag)
                        }
                    }
                }
            }
        }
        // 動画一覧
        if (videoList.value != null) {
            NicoVideoList(
                list = videoList.value!!,
                onVideoClick = { onClickVideo(it) },
                onMenuClick = { onClickMenu(it) }
            )
        }
    }

}