package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import android.view.SurfaceView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.google.android.exoplayer2.video.VideoListener
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
            NicoVideoPlayerUI(nicoVideoViewModel)
        },
        detailContent = {
            // 動画情報部分
            NicoVideoDetailUI(nicoVideoViewModel)
        },
    )
}

/**
 * 動画情報部分
 * @param viewModel ViewModel
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoDetailUI(viewModel: NicoVideoViewModel) {
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

        // 動画情報表示
        if (videoData.value != null) {
            NicoVideoInfoCardReEditioN(
                nicoVideoData = videoData.value!!,
                isLiked = isLiked.value,
                onLikeClick = { },
                isOffline = isOfflinePlayLiveData.value,
                description = description.value,
                descriptionClick = { link, type -> }, // 押した時
                columnSpace = {

                    // タグ表示
                    if (tagList.value != null) {
                        NicoVideoTagCard(
                            tagList.value!!,
                            onNicoPediaClick = {},
                            onTagClick = {}
                        )
                    }

                    // シリーズ情報
                    if (seriesHTMLLiveData.value != null) {
                        NicoVideoSeriesUI(
                            nicoVideoHTMLSeriesData = seriesHTMLLiveData.value!!,
                            onStartSeriesPlayClick = { },
                            onFirstVideoPlayClick = { },
                            onNextVideoPlayClick = { },
                            onPrevVideoPlayClick = { }
                        )
                    }

                    // ユーザー情報
                    if (userData.value != null) {
                        NicoVideoUserUI(
                            userData = userData.value!!,
                            onUserOpenClick = {
                                if (userData.value!!.isChannel) {

                                } else {

                                }
                            }
                        )
                    }
                }
            )
        }

        // 共有、メニュー、キャッシュ取得ボタンなど
        NicoVideoMenuButtonGroup()

        // 関連動画表示
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
@ExperimentalComposeUiApi
@Composable
fun NicoVideoPlayerUI(viewModel: NicoVideoViewModel) {
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
        NicoVideoPlayerControlUI(
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    // Preference
    val prefSetting = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    // ExoPlayer
    val exoPlayer = remember {
        val exoPlayer = SimpleExoPlayer.Builder(context).build()
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
        exoPlayer.addVideoListener(object : VideoListener {
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                super.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
                viewModel.videoAspectRateLiveData.postValue(width.toFloat() / height.toFloat())
            }
        })
        return@remember exoPlayer
    }

    /** ExoPlayerで動画を再生する */
    fun playExoPlayer(contentUrl: String) {
        // キャッシュ再生と分ける
        when {
            // キャッシュを優先的に利用する　もしくは　キャッシュ再生時
            viewModel.isOfflinePlay.value ?: false -> {
                // キャッシュ再生
                val dataSourceFactory = DefaultDataSourceFactory(context, "TatimiDroid;@takusan_23")
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

    // Viewを生成
    val (surfaceView, reCommentCanvas) = remember {
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
                        viewModel.playerCurrentPositionMsLiveData.value = viewModel.playerCurrentPositionMs
                    }
                }
            }
        }

        return@remember Pair(surfaceView, reCommentCanvas)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF252525))
            .aspectRatio(1.7f),
    ) {
        // アスペクト比
        val aspectRate = viewModel.videoAspectRateLiveData.observeAsState(initial = 1.7f)
        // ExoPlayerのSurfaceView
        AndroidView(
            modifier = Modifier
                .fillMaxHeight()
                .align(Alignment.Center)
                .aspectRatio(aspectRate.value),
            factory = { surfaceView }
        )
        // コメント描画
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            factory = { reCommentCanvas }
        )
    }

    // Composeの世界のonDestroy。
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

}