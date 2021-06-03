package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.compose.PlaceholderImage
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoSeriesData
import io.github.takusan23.tatimidroid.nicovideo.compose.getBitmapCompose
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSeriesListViewModel

/**
 * ニコ動のシリーズ一覧画面。Composeでできている
 *
 * @param viewModel シリーズ一覧ViewModel
 * @param onSeriesClick シリーズ押したら呼ばれる
 * */
@Composable
fun NicoVideoSeriesParentScreen(
    viewModel: NicoVideoSeriesListViewModel,
    onSeriesClick: (NicoVideoSeriesData) -> Unit,
) {
    // 読み込み中
    val isLoading = viewModel.loadingLiveData.observeAsState(initial = true)
    // シリーズ一覧
    val seriesList = viewModel.nicoVideoDataListLiveData.observeAsState()

    if (isLoading.value || seriesList.value == null) {
        FillLoadingScreen()
    } else {
        NicoVideoSeriesList(
            list = seriesList.value!!,
            onSeriesClick = { onSeriesClick(it) }
        )
    }
}

/**
 * シリーズ一覧表示
 *
 * @param list シリーズのデータクラスの配列
 * @param onSeriesClick シリーズ押したら呼ばれる
 * */
@Composable
private fun NicoVideoSeriesList(
    list: List<NicoVideoSeriesData>,
    onSeriesClick: (NicoVideoSeriesData) -> Unit,
) {
    LazyColumn {
        items(list) { seriesData ->
            NicoVideoSeriesListItem(
                nicoVideoSeriesData = seriesData,
                onSeriesClick = { onSeriesClick(it) }
            )
            Divider()
        }
    }
}

/**
 * シリーズ一覧表示の各UI
 *
 * @param nicoVideoSeriesData シリーズのデータクラス
 * @param onSeriesClick シリーズ押したら呼ばれる
 * */
@Composable
private fun NicoVideoSeriesListItem(
    nicoVideoSeriesData: NicoVideoSeriesData,
    onSeriesClick: (NicoVideoSeriesData) -> Unit,
) {
    // シリーズのサムネ
    val seriesThumb = getBitmapCompose(url = nicoVideoSeriesData.thumbUrl)?.asImageBitmap()

    Surface(modifier = Modifier.clickable { onSeriesClick(nicoVideoSeriesData) }) {
        Row(modifier = Modifier.padding(5.dp).fillMaxWidth()) {
            PlaceholderImage(
                modifier = Modifier
                    .width(100.dp)
                    .aspectRatio(1.7f),
                isLoading = seriesThumb == null,
                imageBitmap = seriesThumb,
            )
            Column(modifier = Modifier.padding(5.dp)) {
                Text(text = nicoVideoSeriesData.title, fontSize = 18.sp)
                Text(text = "${nicoVideoSeriesData.itemsCount} 件")
            }
        }
    }
}