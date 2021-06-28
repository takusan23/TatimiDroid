package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import android.view.Gravity
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import io.github.takusan23.tatimidroid.ReCommentCanvas
import io.github.takusan23.tatimidroid.compose.MiniPlayerCompose
import io.github.takusan23.tatimidroid.compose.MiniPlayerState
import io.github.takusan23.tatimidroid.nicovideo.compose.*
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoViewModel
import io.github.takusan23.tatimidroid.service.startVideoPlayService
import io.github.takusan23.tatimidroid.tool.isConnectionWiFiInternet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ニコニコ動画プレイヤー
 *
 * @param nicoVideoViewModel ニコ動ViewModel
 * @param onDestroy 終了時に呼ばれる
 * */
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun NicoVideoPlayerScreen(
    nicoVideoViewModel: NicoVideoViewModel,
    onDestroy: () -> Unit,
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
        // // 関連動画表示Card
        // if (recommendList.value != null) {
        //     NicoVideoRecommendCard(recommendList.value!!)
        // }
    }
}

/**
 * プレイヤー部分
 * @param viewModel ViewModel
 * */
@ExperimentalComposeUiApi
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
    // コメント一覧
    val commentList = viewModel.commentList.observeAsState()
    // Preference
    val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
    // シーク
    val seek = viewModel.playerSetSeekMs.observeAsState(initial = 0)

    // 時間
    if (videoData.value != null) {

        // ExoPlayerとコメント描画
        NicoVideoPlayerExoPlayerAndCommentCanvas(viewModel = viewModel)

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

/**
 * コメント描画とExoPlayerは既存のViewを使う
 *
 * @param viewModel ニコ動ViewModel
 * */
@Composable
fun NicoVideoPlayerExoPlayerAndCommentCanvas(viewModel: NicoVideoViewModel) {
    val composeContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    // ExoPlayer
    val exoPlayer = remember {
        val exoPlayer = SimpleExoPlayer.Builder(composeContext).build()
        // ExoPlayerのイベント
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                // 動画時間をセットする
                viewModel.playerDurationMs.postValue(exoPlayer.duration)
                // くるくる
                if (state == Player.STATE_READY || state == Player.STATE_ENDED) {
                    viewModel.playerIsLoading.postValue(false)
                } else {
                    viewModel.playerIsLoading.postValue(true)
                }
                // 動画おわった。連続再生時なら次の曲へ
                if (state == Player.STATE_ENDED && exoPlayer.playWhenReady) {
                    viewModel.nextVideo()
                }
            }
        })
        return@remember exoPlayer
    }
    // Preference
    val prefSetting = remember { PreferenceManager.getDefaultSharedPreferences(composeContext) }

    /** ExoPlayerで動画を再生する */
    fun playExoPlayer(contentUrl: String) {
        // キャッシュ再生と分ける
        when {
            // キャッシュを優先的に利用する　もしくは　キャッシュ再生時
            viewModel.isOfflinePlay.value ?: false -> {
                // キャッシュ再生
                val dataSourceFactory = DefaultDataSourceFactory(composeContext, "TatimiDroid;@takusan_23")
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value).build())
                exoPlayer.setMediaSource(videoSource)
            }
            // それ以外：インターネットで取得
            else -> {
                // SmileサーバーはCookieつけないと見れないため
                val dataSourceFactory = DefaultHttpDataSourceFactory("TatimiDroid;@takusan_23", null)
                dataSourceFactory.defaultRequestProperties.set("Cookie", viewModel.nicoHistory)
                val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.Builder().setUri(contentUrl.toUri()).setMediaId(viewModel.playingVideoId.value).build())
                exoPlayer.setMediaSource(videoSource)
            }
        }
        // 準備と再生
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.7f),
        factory = { context ->

            val surfaceView = SurfaceView(context)
            val reCommentCanvas = ReCommentCanvas(context, null)

            /**
             * いい感じに書けそうに無いので今まで通りLiveDataを受け取る
             * */

            // コメント
            viewModel.commentList.observe(lifecycleOwner) { commentList ->
                // ついでに動画の再生時間を取得する。非同期
                viewModel.playerDurationMs.observe(lifecycleOwner, object : Observer<Long> {
                    override fun onChanged(t: Long?) {
                        if (t != null && t > 0) {
                            reCommentCanvas.initCommentList(commentList, t)
                            // 一回取得したらコールバック無効化。SAM変換をするとthisの指すものが変わってしまう
                            viewModel.playerDurationMs.removeObserver(this)
                        }
                    }
                })
            }
            // 動画再生
            viewModel.contentUrl.observe(lifecycleOwner) { contentUrl ->
                val oldPosition = exoPlayer.currentPosition
                playExoPlayer(contentUrl)
                // 画質変更時は途中から再生。動画IDが一致してないとだめ
                if (oldPosition > 0 && exoPlayer.currentMediaItem?.mediaId == viewModel.playingVideoId.value) {
                    exoPlayer.seekTo(oldPosition)
                }
                exoPlayer.setVideoSurfaceView(surfaceView)
            }
            // 一時停止、再生になったとき
            viewModel.playerIsPlaying.observe(lifecycleOwner) { isPlaying ->
                exoPlayer.playWhenReady = isPlaying
                reCommentCanvas.isPlaying = isPlaying
            }
            // シークしたとき
            viewModel.playerSetSeekMs.observe(lifecycleOwner) { seekPos ->
                if (0 <= seekPos) {
                    viewModel.playerCurrentPositionMs = seekPos
                    exoPlayer.seekTo(seekPos)
                } else {
                    // 負の値に突入するので０
                    viewModel.playerCurrentPositionMs = 0
                }
                // シークさせる
                reCommentCanvas.currentPos = seekPos
                reCommentCanvas.seekComment()
            }
            // リピートモードが変わったとき
            viewModel.playerIsRepeatMode.observe(lifecycleOwner) { isRepeatMode ->
                exoPlayer.repeatMode = if (isRepeatMode) {
                    // リピート有効時
                    Player.REPEAT_MODE_ONE
                } else {
                    // リピート無効時
                    Player.REPEAT_MODE_OFF
                }
                prefSetting.edit { putBoolean("nicovideo_repeat_on", isRepeatMode) }
            }
            // 音量調整
            viewModel.volumeControlLiveData.observe(lifecycleOwner) { volume ->
                exoPlayer.volume = volume
            }

            // 100msごとに再生位置を外部に公開
            scope.launch {
                while (isActive) {
                    delay(100)
                    // 再生時間をコメント描画Canvasへ入れ続ける
                    reCommentCanvas.currentPos = viewModel.playerCurrentPositionMs
                    // 再生中かどうか
                    reCommentCanvas.isPlaying = if (viewModel.isNotPlayVideoMode.value == false) {
                        // 動画バッファー中かも？
                        exoPlayer.isPlaying
                    } else {
                        viewModel.playerIsPlaying.value!!
                    }
                    // 再生中のみ
                    if (viewModel.playerIsPlaying.value == true) {
                        // ExoPlayerが利用できる場合は再生時間をViewModelへ渡す
                        if (viewModel.isNotPlayVideoMode.value == false) {
                            viewModel.playerCurrentPositionMs = exoPlayer.currentPosition
                        }
                    }
                }
            }

            FrameLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                this.foregroundGravity = Gravity.CENTER

                addView(surfaceView)
                addView(reCommentCanvas)
            }
        }
    )

    // Composeの世界のonDestroy。
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

}