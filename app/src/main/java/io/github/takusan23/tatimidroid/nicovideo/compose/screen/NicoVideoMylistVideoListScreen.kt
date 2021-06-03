package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoMyListListViewModel

/**
 * マイリストの詳細（動画一覧）画面。Composeでできている。
 *
 * @param viewModel マイリストの中身ViewModel
 * @param onClickVideo 動画押したときに呼ばれる
 * @param onClickMenu メニュー押したときに呼ばれる
 * */
@Composable
fun NicoVideoMylistVideoListScreen(
    viewModel: NicoVideoMyListListViewModel,
    onClickVideo: (NicoVideoData) -> Unit,
    onClickMenu: (NicoVideoData) -> Unit,
) {
    // 動画一覧
    val videoList = viewModel.nicoVideoDataListLiveData.observeAsState()
    // 読み込み中ならtrue
    val isLoading = viewModel.loadingLiveData.observeAsState(initial = true)

    if (isLoading.value || videoList.value == null) {
        // 読み込み中
        FillLoadingScreen()
    } else {
        // 動画一覧
        NicoVideoList(
            list = videoList.value!!,
            onVideoClick = { onClickVideo(it) },
            onMenuClick = { onClickMenu(it) }
        )
    }
}