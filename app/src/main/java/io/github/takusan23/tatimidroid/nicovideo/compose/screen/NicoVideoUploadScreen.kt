package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import io.github.takusan23.tatimidroid.compose.FillLoadingScreen
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoUploadVideoViewModel

/**
 * 投稿動画一覧画面。Composeでできている
 *
 * @param viewModel ViewModel
 * @param onClickVideo 動画選択したら呼ばれる
 * @param onClickMenu メニュー押したとき呼ばれる
 * */
@Composable
fun NicoVideoUploadScreen(
    viewModel: NicoVideoUploadVideoViewModel,
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit
) {
    // 動画一覧
    val videoList = viewModel.nicoVideoDataListLiveData.observeAsState()
    // 読み込み中ならtrue
    val isLoading = viewModel.loadingLiveData.observeAsState(initial = true)

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