package io.github.takusan23.tatimidroid.nicolive.compose.screen

import android.content.Intent
import android.view.SurfaceView
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import io.github.takusan23.tatimidroid.CommentCanvas
import io.github.takusan23.tatimidroid.compose.MiniPlayerCompose
import io.github.takusan23.tatimidroid.compose.MiniPlayerState
import io.github.takusan23.tatimidroid.nicolive.compose.*
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveViewModel
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoUserUI
import io.github.takusan23.tatimidroid.service.startLivePlayService
import io.github.takusan23.tatimidroid.tool.NicoVideoDescriptionText
import io.github.takusan23.tatimidroid.tool.isConnectionWiFiInternet

/**
 * ニコ生プレイヤー。Compose版
 *
 * @param viewModel ニコ生ViewModel
 * @param onDestroy 終了時に呼ばれる
 * */
@ExperimentalAnimationApi
@Composable
fun NicoLivePlayerScreen(viewModel: NicoLiveViewModel, onDestroy: () -> Unit) {
    MiniPlayerCompose(
        onMiniPlayerState = { state -> if (state == MiniPlayerState.End) onDestroy() },
        backgroundContent = { },
        playerContent = {
            // 生放送再生部分
            NicoLivePlayerUI(viewModel = viewModel)
        },
        detailContent = {
            // 生放送情報部分
            NicoLiveDetailUI(viewModel = viewModel)
            // コメント投稿部分
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                NicoLiveCommentPostBar(viewModel = viewModel)
            }
        },
    )
}

/**
 * ニコ生番組情報画面
 * @param viewModel ニコ生ViewModel
 * */
@ExperimentalAnimationApi
@Composable
fun NicoLiveDetailUI(viewModel: NicoLiveViewModel) {
    val context = LocalContext.current
    // 番組情報
    val programData = viewModel.nicoLiveProgramData.observeAsState()
    // 説明文
    val description = viewModel.nicoLiveProgramDescriptionLiveData.observeAsState()
    // ユーザー情報
    val userData = viewModel.nicoLiveUserDataLiveData.observeAsState()
    // コミュ、チャンネル情報
    val communityOrChannelData = viewModel.nicoLiveCommunityOrChannelDataLiveData.observeAsState()
    // コミュ、チャンネルフォロー中か
    val isCommunityOrChannelFollow = viewModel.isCommunityOrChannelFollowLiveData.observeAsState(initial = false)
    // タグ
    val tagData = viewModel.nicoLiveTagDataListLiveData.observeAsState()
    // 好みタグ
    val konomiTagList = viewModel.nicoLiveKonomiTagListLiveData.observeAsState(initial = arrayListOf())
    // 統計情報LiveData
    val statisticsLiveData = viewModel.statisticsLiveData.observeAsState()
    // タイムシフト予約済みかどうか（なお予約済みかどうかはAPIを叩くまでわからん）
    val isRegisteredTimeShift = viewModel.isTimeShiftRegisteredLiveData.observeAsState(initial = false)
    // タイムシフト予約が可能かどうか
    val isAllowTSRegister = viewModel.isAllowTSRegister.observeAsState(initial = true)
    // コメント一覧表示中か
    val isVisibleCommentList = viewModel.commentListShowLiveData.observeAsState(initial = false)

    /**
     * ブラウザを起動する
     * @param url URL
     * */
    fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    }

    /** スクロールできるColumn。LazyColumnがサイズ変わったときになんかおかしくなる */
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 番組情報
        if (programData.value != null && description.value != null) {
            NicoLiveInfoCard(
                nicoLiveProgramData = programData.value!!,
                programDescription = description.value!!,
                isRegisteredTimeShift = isRegisteredTimeShift.value,
                isAllowTSRegister = isAllowTSRegister.value!!,
                onClickTimeShift = { },
                descriptionClick = { link, type ->
                    if (type == NicoVideoDescriptionText.DESCRIPTION_TYPE_URL) {
                        val intent = Intent(Intent.ACTION_VIEW, link.toUri())
                        context.startActivity(intent)
                    }
                }
            )
        }
        // ユーザー情報。ニコ動用のがそのまま使えた
        if (userData.value != null) {
            NicoVideoUserUI(
                userData = userData.value!!,
                onUserOpenClick = {
                }
            )
        }
        // コミュ、番組情報
        if (communityOrChannelData.value != null) {
            NicoLiveCommunityCard(
                communityOrChannelData = communityOrChannelData.value!!,
                isFollow = isCommunityOrChannelFollow.value,
                onFollowClick = {
                    if (isCommunityOrChannelFollow.value) {
                        // 解除

                    } else {
                        // コミュをフォローする

                    }
                },
                onCommunityOpenClick = {
                    openBrowser("https://com.nicovideo.jp/community/${communityOrChannelData.value!!.id}")
                }
            )
        }
        // タグ
        if (tagData.value != null) {
            NicoLiveTagCard(
                tagItemDataList = tagData.value!!.tagList,
                onClickTag = { openBrowser("https://live.nicovideo.jp/search?keyword=${it.tagName}&isTagSearch=true") },
                isEditable = !tagData.value!!.isLocked,
                onClickEditButton = { },
                onClickNicoPediaButton = { openBrowser(it) }
            )
        }
        // 好みタグ
        NicoLiveKonomiCard(
            konomiTagList = konomiTagList.value,
            onClickEditButton = { }
        )

        if (statisticsLiveData.value != null) {
            // ニコニ広告 投げ銭
            NicoLivePointCard(
                totalNicoAdPoint = statisticsLiveData.value!!.adPoints,
                totalGiftPoint = statisticsLiveData.value!!.giftPoints,
                onClickNicoAdOpen = { },
                onClickGiftOpen = { }
            )
        }

        // スペース
        Spacer(modifier = Modifier.height(100.dp))
    }
}

/**
 * コメント投稿部分（テキストボックスとか）
 * @param viewModel ニコ生ViewModel
 * @param modifier Modifier
 * */
@Composable
fun NicoLiveCommentPostBar(modifier: Modifier = Modifier, viewModel: NicoLiveViewModel) {
    val context = LocalContext.current
    val prefSetting = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // コメント展開するかどうか
    val isComment = viewModel.commentListShowLiveData.observeAsState(initial = false)
    // コメント本文
    val commentPostText = remember { mutableStateOf("") }
    // 匿名で投稿するか
    val isTokumeiPost = remember { mutableStateOf(viewModel.nicoLiveHTML.isPostTokumeiComment) }
    // 文字の大きさ
    val commentSize = remember { mutableStateOf("medium") }
    // 文字の位置
    val commentPos = remember { mutableStateOf("naka") }
    // 文字の色
    val commentColor = remember { mutableStateOf("white") }
    // 複数行コメントを許可している場合はtrue。falseならEnterキーでコメント送信
    val isAcceptMultiLineComment = !prefSetting.getBoolean("setting_enter_post", true)
    // タイムシフト視聴中はテキストボックス出さない
    val isTimeShiftWatching = viewModel.isWatchingTimeShiftLiveData.observeAsState(initial = false)

    NicoLiveCommentInputButton(
        onClick = { viewModel.commentListShowLiveData.postValue(!isComment.value) },
        isComment = isComment.value,
        comment = commentPostText.value,
        onCommentChange = { commentPostText.value = it },
        onPostClick = {
            // コメント投稿
            viewModel.sendComment(commentPostText.value, commentColor.value, commentSize.value, commentPos.value)
            commentPostText.value = "" // クリアに
        },
        position = commentPos.value,
        size = commentSize.value,
        color = commentColor.value,
        isTimeShiftMode = isTimeShiftWatching.value,
        onPosValueChange = { commentPos.value = it },
        onSizeValueChange = { commentSize.value = it },
        onColorValueChange = { commentColor.value = it },
        is184 = isTokumeiPost.value,
        onTokumeiChange = {
            // 匿名、生ID切り替わった時
            isTokumeiPost.value = !isTokumeiPost.value
            prefSetting.edit { putBoolean("nicolive_post_tokumei", it) }
            viewModel.nicoLiveHTML.isPostTokumeiComment = it
        },
        isMultiLine = isAcceptMultiLineComment,
    )

}

/**
 * ニコ生映像再生部分
 * @param viewModel ニコ生ViewModel
 * */
@Composable
fun NicoLivePlayerUI(viewModel: NicoLiveViewModel) {
    val context = LocalContext.current
    // 番組情報
    val programData = viewModel.nicoLiveProgramData.observeAsState()
    // 経過時間
    val currentPosSec = viewModel.programCurrentPositionSecLiveData.observeAsState(initial = 0)
    // 番組の期間（放送時間）
    val duration = viewModel.programDurationTimeLiveData.observeAsState(initial = 0)
    // ミニプレイヤーかどうか
    val isMiniPlayerMode = viewModel.isMiniPlayerMode.observeAsState(false)
    // コメント描画
    val isCommentShow = remember { mutableStateOf(false) }
    // TS再生中？
    val isWatchingTS = viewModel.isWatchingTimeShiftLiveData.observeAsState(initial = false)

    if (programData.value != null) {
        // 映像部分
        NicoLiveExoPlayerAndCommentCanvas(viewModel = viewModel)

        // プレイヤー
        NicoLivePlayerControlUI(
            liveTitle = programData.value!!.programId,
            liveId = programData.value!!.programId,
            isMiniPlayer = isMiniPlayerMode.value,
            isDisableMiniPlayerMode = false,
            isFullScreen = viewModel.isFullScreenMode,
            isConnectedWiFi = isConnectionWiFiInternet(context),
            isShowCommentCanvas = isCommentShow.value,
            isAudioOnlyMode = viewModel.currentQuality == "audio_high",
            isTimeShiftMode = isWatchingTS.value,
            currentPosition = currentPosSec.value,
            duration = duration.value,
            onMiniPlayerClick = { },
            onFullScreenClick = { },
            onNetworkClick = { },
            onCommentDrawClick = { isCommentShow.value = !isCommentShow.value },
            onPopUpPlayerClick = {
                startLivePlayService(
                    context = context,
                    mode = "popup",
                    liveId = programData.value!!.programId,
                    isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                    startQuality = viewModel.currentQuality
                )
            },
            onBackgroundPlayerClick = {
                startLivePlayService(
                    context = context,
                    mode = "background",
                    liveId = programData.value!!.programId,
                    isTokumei = viewModel.nicoLiveHTML.isPostTokumeiComment,
                    startQuality = viewModel.currentQuality
                )
            },
            onTsSeek = { position -> viewModel.tsSeekPosition(position) },
            onCommentPostClick = { comment -> viewModel.sendComment(comment) }
        )
    }
}

/**
 * コメント描画とExoPlayerは既存のViewを使う
 *
 * @param viewModel ニコ生ViewModel
 * @param onRetryMessage 映像取得でコケたので再取得するときに呼ばれます
 * */
@Composable
private fun NicoLiveExoPlayerAndCommentCanvas(viewModel: NicoLiveViewModel, onRetryMessage: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    // Preference
    val prefSetting = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // 映像を受信しないモード
    val isDisableReceiveVideo = viewModel.isNotReceiveLive.observeAsState(initial = false)

    // ExoPlayer
    val exoPlayer = remember {
        val exoPlayer = SimpleExoPlayer.Builder(context).build()
        // もしエラー出たら
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerError(error: ExoPlaybackException) {
                super.onPlayerError(error)
                error.printStackTrace()
                println("生放送の再生が止まりました。")
                //再接続する？
                //それからニコ生視聴セッションWebSocketが切断されてなければ
                if (!viewModel.nicoLiveHTML.nicoLiveWebSocketClient.isClosed) {
                    println("再度再生準備を行います")
                    //再生準備
                    val contentUrl = viewModel.hlsAddressLiveData.value!!
                    val mediaItem = MediaItem.fromUri(contentUrl.toUri())
                    exoPlayer.setMediaItem(mediaItem)
                    exoPlayer.prepare()
                    onRetryMessage()
                }
            }
        })
        return@remember exoPlayer
    }

    /** ExoPlayerで動画を再生する */
    fun playExoPlayer(contentUrl: String) {
        val mediaItem = MediaItem.fromUri(contentUrl.toUri())
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    // Viewを生成
    val (surfaceView, commentCanvas) = remember {
        val surfaceView = SurfaceView(context)
        val commentCanvas = CommentCanvas(context, null)

        /**
         * いい感じに書けそうに無いので今まで通りLiveDataを受け取る
         * */

        // 動画再生
        viewModel.hlsAddressLiveData.observe(lifecycleOwner) { contentUrl ->
            playExoPlayer(contentUrl)
            exoPlayer.setVideoSurfaceView(surfaceView)
        }
        // 音量調整LiveData
        viewModel.exoplayerVolumeLiveData.observe(lifecycleOwner) { volume ->
            exoPlayer.volume = volume
        }
        // コメントうけとる
        viewModel.commentReceiveLiveData.observe(lifecycleOwner) { commentJSONParse ->
            // 豆先輩とか
            if (!commentJSONParse.comment.contains("\n")) {
                commentCanvas.postComment(commentJSONParse.comment, commentJSONParse)
            } else {
                val asciiArtComment = if (commentJSONParse.mail.contains("shita")) {
                    commentJSONParse.comment.split("\n").reversed() // 下コメントだけ逆順にする
                } else {
                    commentJSONParse.comment.split("\n")
                }
                // 複数行対応Var
                commentCanvas.postCommentAsciiArt(asciiArtComment, commentJSONParse)
            }
        }

        return@remember Pair(surfaceView, commentCanvas)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF252525))
            .aspectRatio(1.7f),
    ) {
        // ExoPlayerのSurfaceView
        if (!isDisableReceiveVideo.value) {
            AndroidView(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center),
                factory = { surfaceView }
            )
        }
        // コメント描画
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            factory = { commentCanvas }
        )
    }

    // Composeの世界のonDestroy。
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

}