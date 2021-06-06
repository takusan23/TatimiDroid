package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoList
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoHistoryViewModel

/**
 * ニコ動の視聴履歴画面。Composeでできている
 *
 * @param viewModel 履歴ViewModel
 * @param onVideoClick 動画押したときに呼ばれる
 * @param onMenuClick メニュー押したときに呼ばれる
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoHistoryScreen(
    viewModel: NicoVideoHistoryViewModel,
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit
) {
    // 読み込み中
    val isLoading = viewModel.loadingLiveData.observeAsState(initial = true)
    // 動画一覧
    val videoList = viewModel.historyListLiveData.observeAsState()

    if (isLoading.value || videoList.value == null) {
        // 読み込み中
        FillLoadingScreen()
    } else {
        // 履歴一覧
        NicoVideoList(
            list = videoList.value!!,
            onVideoClick = { onVideoClick(it) },
            onMenuClick = { onMenuClick(it) }
        )
    }
}