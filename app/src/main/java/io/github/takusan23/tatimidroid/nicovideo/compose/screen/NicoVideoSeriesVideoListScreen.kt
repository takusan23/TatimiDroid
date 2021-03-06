package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoList
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSeriesViewModel

/**
 * シリーズの動画一覧画面。Composeでできている
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoSeriesVideoListScreen(
    viewModel: NicoVideoSeriesViewModel,
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit
) {
    // 読み込み中
    val isLoading = viewModel.loadingLiveData.observeAsState(initial = true)
    // 動画一覧
    val videoList = viewModel.nicoVideoDataListLiveData.observeAsState()

    if (isLoading.value || videoList.value == null) {
        FillLoadingScreen()
    } else {
        NicoVideoList(
            list = videoList.value!!,
            onVideoClick = { onVideoClick(it) },
            onMenuClick = { onMenuClick(it) }
        )
    }

}