package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.compose.ComposeExoPlayer
import io.github.takusan23.tatimidroid.compose.MiniPlayerCompose
import io.github.takusan23.tatimidroid.compose.MiniPlayerState
import io.github.takusan23.tatimidroid.nicovideo.compose.*
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.service.startVideoPlayService
import io.github.takusan23.tatimidroid.tool.isConnectionWiFiInternet

/**
 * ニコニコ動画プレイヤー
 *
 * @param nicoVideoViewModel ニコ動ViewModel
 * @param onDestroy 終了時に呼ばれる
 * */
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun NicoVideoPlayerScreen(
    nicoVideoViewModel: NicoVideoViewModel,
    onDestroy: () -> Unit
) {

    MiniPlayerCompose(
        onMiniPlayerState = { state -> if (state == MiniPlayerState.End) onDestroy() },
        backgroundContent = { },
        playerContent = {
            // 動画再生部分
            NicoVideoPlayerScreen(nicoVideoViewModel)
        },
        detailContent = {
            // 動画情報部分
            NicoVideoDetailScreen(nicoVideoViewModel)
        },
    )
}

/**
 * 動画情報部分
 * @param viewModel ViewModel
 * */
@ExperimentalMaterialApi
@Composable
private fun NicoVideoDetailScreen(viewModel: NicoVideoViewModel) {
    val context = LocalContext.current
    // 動画情報
    val videoData = viewModel.nicoVideoData.observeAsState()
    // いいね状態
    val isLiked = viewModel.isLikedLiveData.observeAsState(initial = false)
    // 動画情報
    val description = viewModel.nicoVideoDescriptionLiveData.observeAsState(initial = "")
    // 関連動画
    val recommendList = viewModel.recommendList.observeAsState()
    // ユーザー情報
    val userData = viewModel.userDataLiveData.observeAsState()
    // タグ一覧
    val tagList = viewModel.tagListLiveData.observeAsState()
    // キャッシュ再生かどうか
    val isOfflinePlayLiveData = viewModel.isOfflinePlay.observeAsState(initial = false)
    // シリーズ
    val seriesHTMLLiveData = viewModel.seriesHTMLDataLiveData.observeAsState()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 動画情報表示Card
        if (videoData.value != null) {
            NicoVideoInfoCard(
                nicoVideoData = videoData.value,
                isLiked = isLiked.value,
                onLikeClick = { },
                isOffline = isOfflinePlayLiveData.value,
                description = description.value,
                descriptionClick = { link, type -> } // 押した時
            )
        }
        // シリーズ
        if (seriesHTMLLiveData.value != null) {
            NicoVideoSeriesCard(
                nicoVideoHTMLSeriesData = seriesHTMLLiveData.value!!,
                onClickStartSeriesPlay = { },
                // 後３つはそれぞれ動画再生関数を呼ぶ
                onClickFirstVideoPlay = { viewModel.load(it.videoId, it.isCache, viewModel.isEco, viewModel.useInternet) },
                onClickNextVideoPlay = { viewModel.load(it.videoId, it.isCache, viewModel.isEco, viewModel.useInternet) },
                onClickPrevVideoPlay = { viewModel.load(it.videoId, it.isCache, viewModel.isEco, viewModel.useInternet) }
            )
        }
        // タグ
        if (tagList.value != null) {
            NicoVideoTagCard(
                tagItemDataList = tagList.value!!,
                onClickTag = { data -> },
                onClickNicoPedia = { url -> }
            )
        }
        // ユーザー情報
        if (userData.value != null) {
            NicoVideoUserCard(
                userData = userData.value!!,
                onUserOpenClick = {
                    if (userData.value!!.isChannel) {

                    } else {

                    }
                }
            )
        }
        // 関連動画表示Card
        if (recommendList.value != null) {
            NicoVideoRecommendCard(recommendList.value!!)
        }
    }
}

/**
 * プレイヤー部分
 * @param viewModel ViewModel
 * */
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun NicoVideoPlayerScreen(viewModel: NicoVideoViewModel) {
    val context = LocalContext.current
    // 動画情報
    val videoData = viewModel.nicoVideoData.observeAsState()
    // ミニプレイヤーかどうか
    val isMiniPlayerMode = viewModel.isMiniPlayerMode.observeAsState(false)
    // コメント描画
    val isShowDrawComment = remember { mutableStateOf(true) }
    // 連続再生かどうか
    val isPlaylistMode = viewModel.isPlayListMode.observeAsState(initial = false)
    // 再生中かどうか
    val isPlaying = viewModel.playerIsPlaying.observeAsState(initial = false)
    // 再生時間
    val currentPosition = viewModel.playerCurrentPositionMsLiveData.observeAsState(initial = 0)
    // リピート再生？
    val isRepeat = viewModel.playerIsRepeatMode.observeAsState(initial = true)
    // 動画の長さ
    val duration = viewModel.playerDurationMs.observeAsState(initial = 0)
    // ローディング
    val isLoading = viewModel.playerIsLoading.observeAsState(initial = false)
    // 動画URL
    val contentUrl = viewModel.contentUrl.observeAsState()
    // Preference
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    // シーク
    val seek = viewModel.playerSetSeekMs.observeAsState(initial = 0)

    // 時間
    if (videoData.value != null) {
        // ExoPlayer
        if (contentUrl.value != null) {
            ComposeExoPlayer(
                contentUrl = contentUrl.value!!,
                isPlaying = isPlaying.value,
                seek = seek.value,
                onVideoDuration = { viewModel.playerDurationMs.postValue(it) },
                onUpdate = { currentPos, _ ->
                    viewModel.currentPosition = currentPos
                    viewModel.playerCurrentPositionMsLiveData.postValue(currentPos)
                }
            )
        }
        // プレイヤー
        NicoVideoPlayerUI(
            videoTitle = videoData.value!!.title,
            videoId = videoData.value!!.videoId,
            isMiniPlayer = isMiniPlayerMode.value,
            isDisableMiniPlayerMode = false,
            isFullScreen = viewModel.isFullScreenMode,
            isConnectedWiFi = isConnectionWiFiInternet(context),
            isShowCommentCanvas = isShowDrawComment.value,
            isRepeat = isRepeat.value,
            isCachePlay = videoData.value!!.isCache,
            isLoading = isLoading.value,
            isPlaylistMode = isPlaylistMode.value,
            isPlaying = isPlaying.value,
            currentPosition = currentPosition.value.toLong() / 1000,
            duration = duration.value.toLong() / 1000,
            onMiniPlayerClick = { },
            onFullScreenClick = { },
            onNetworkClick = { },
            onRepeatClick = { viewModel.playerIsRepeatMode.postValue(!viewModel.playerIsRepeatMode.value!!) },
            onCommentDrawClick = { isShowDrawComment.value = !isShowDrawComment.value },
            onPopupPlayerClick = {
                startVideoPlayService(
                    context = context,
                    mode = "popup",
                    videoId = videoData.value!!.videoId,
                    isCache = videoData.value!!.isCache,
                    seek = viewModel.currentPosition,
                    videoQuality = viewModel.currentVideoQuality,
                    audioQuality = viewModel.currentAudioQuality,
                    playlist = viewModel.playlistLiveData.value
                )
            },
            onBackgroundPlayerClick = {
                startVideoPlayService(
                    context = context,
                    mode = "background",
                    videoId = videoData.value!!.videoId,
                    isCache = videoData.value!!.isCache,
                    seek = viewModel.currentPosition,
                    videoQuality = viewModel.currentVideoQuality,
                    audioQuality = viewModel.currentAudioQuality,
                    playlist = viewModel.playlistLiveData.value
                )
            },
            onPictureClick = { },
            onPauseOrPlayClick = { viewModel.playerIsPlaying.postValue(!viewModel.playerIsPlaying.value!!) },
            onPrevClick = { viewModel.prevVideo() },
            onNextClick = { viewModel.nextVideo() },
            onSeekDoubleClick = { isPrev ->
                val seekValue = prefSetting.getString("nicovideo_skip_sec", "5")?.toLongOrNull() ?: 5
                val seekMs = if (isPrev) {
                    viewModel.currentPosition - (seekValue * 1000)
                } else {
                    viewModel.currentPosition + (seekValue * 1000)
                }
                viewModel.playerCurrentPositionMsLiveData.value = seekMs
                viewModel.playerSetSeekMs.value = seekMs
            },
            onSeek = { progress ->
                // ExoPlayer再開
                viewModel.playerCurrentPositionMsLiveData.value = progress * 1000
                viewModel.playerSetSeekMs.value = progress * 1000
            },
            onTouchingSeek = { },
        )
    }
}