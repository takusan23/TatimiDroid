package io.github.takusan23.tatimidroid.nicovideo.compose

import android.text.format.DateUtils
import android.widget.TextView
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.OrigamiLayout
import io.github.takusan23.tatimidroid.compose.TagButton
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoTagItemData
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoHTMLSeriesData
import io.github.takusan23.tatimidroid.nicoapi.user.UserData
import io.github.takusan23.tatimidroid.tool.*
import kotlinx.coroutines.launch

/**
 * Jetpack Compose 略してJC
 * */

/** [NicoVideoInfoCard]とかの親のCardに指定するModifier */
val parentCardModifier = Modifier
    .animateContentSize()
    .padding(5.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定する丸み */
val parentCardShape = RoundedCornerShape(3.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定するElevation */
val parentCardElevation = 3.dp

/**
 * Composeリメイクに合わせてちょっとUIも変更。
 *
 * Android 12みたいに丸くしてみた？
 *
 * @param nicoVideoData ニコ動データクラス。
 * @param isLiked いいねしたかどうか。
 * @param isOffline キャッシュ再生用。trueにするといいねボタンを非表示にします。
 * @param description 動画説明文
 * @param onLikeClick いいね押したときに呼ばっる
 * @param descriptionClick 動画説明文のリンクを押した時。[NicoVideoDescriptionText.DESCRIPTION_TYPE_MYLIST]等参照
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoInfoCardReEditioN(
    nicoVideoData: NicoVideoData,
    columnSpace: @Composable ColumnScope.() -> Unit,
    isLiked: Boolean,
    isOffline: Boolean,
    description: String,
    onLikeClick: () -> Unit,
    descriptionClick: (id: String, type: String) -> Unit,
) {
    // 動画説明文表示状態
    val expanded = remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {

            // 真ん中にする
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    // タイトル
                    Text(
                        text = nicoVideoData.title,
                        fontSize = 20.sp,
                    )
                    // 動画ID表示
                    Text(
                        text = nicoVideoData.videoId,
                        fontSize = 12.sp,
                    )
                }
                // 展開ボタン。動画説明文の表示を切り替える
                IconButton(onClick = { expanded.value = !expanded.value }) {
                    Icon(
                        painter = if (expanded.value) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = stringResource(id = R.string.nicovideo_info)
                    )
                }
            }

            // 再生数や、いいねボタンのやつ
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    // マイリスト数とかコメント数とか
                    NicoVideoCountText(
                        viewCount = nicoVideoData.viewCount.toInt(),
                        commentCount = nicoVideoData.commentCount.toInt(),
                        mylistCount = nicoVideoData.mylistCount.toInt()
                    )
                    // 投稿日時
                    NicoVideoUploadText(
                        uploadDateUnixTime = nicoVideoData.date,
                        isCountDown = true
                    )
                }
                // いいねぼたん
                if (!isOffline) {
                    NicoVideoLikeButton(
                        isLiked = isLiked,
                        onLikeClick = onLikeClick,
                    )
                }
            }

            // 詳細表示
            if (expanded.value) {
                Column {
                    // 区切り線
                    Divider(modifier = Modifier.padding(5.dp))
                    /** 多分HTMLを表示する機能はないので従来のTextView登場 */
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                // リンク押せるように
                                NicoVideoDescriptionText.setLinkText(text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT), this, descriptionClick)
                            }
                        }
                    )
                }
            }

            Column(content = columnSpace)

        }
    }

}

/**
 * ニコ動メニュー。キャッシュ取得ボタンとか
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoMenuButtonGroup() {
    Surface(
        modifier = Modifier.padding(5.dp),
        color = MaterialTheme.colors.primary.copy(0.2f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(modifier = Modifier.padding(5.dp)) {
            NicoVideoMenuButton(Modifier.weight(1f), painterResource(id = R.drawable.ic_folder_open_black_24dp), stringResource(id = R.string.mylist), onClick = {})
            NicoVideoMenuButton(Modifier.weight(1f), painterResource(id = R.drawable.ic_outline_menu_24), stringResource(id = R.string.menu), onClick = {})
            NicoVideoMenuButton(Modifier.weight(1f), painterResource(id = R.drawable.ic_share), stringResource(id = R.string.share), onClick = {})
            NicoVideoMenuButton(Modifier.weight(1f), painterResource(id = R.drawable.ic_cache_progress_icon), stringResource(id = R.string.cache), onClick = {})
        }
    }
}

/**
 * [NicoVideoMenuButtonGroup]の各ボタン
 * @param icon アイコン
 * @param text テキスト
 * @param onClick 押したときに呼ばれる
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoMenuButton(modifier: Modifier, icon: Painter, text: String, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .padding(5.dp),
        color = MaterialTheme.colors.primary.copy(0.5f),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick,
    ) {
        Column(
            modifier = modifier
                .padding(start = 2.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(30.dp))
            Text(text = text, textAlign = TextAlign.Center)
        }
    }
}

/**
 * 動画説明、タイトルCard
 * @param nicoVideoData ニコ動データクラス。
 * @param isLiked いいねしたかどうか。
 * @param isOffline キャッシュ再生用。trueにするといいねボタンを非表示にします。
 * @param description 動画説明文
 * @param onLikeClick いいね押したときに呼ばっる
 * @param descriptionClick 動画説明文のリンクを押した時。[NicoVideoDescriptionText.DESCRIPTION_TYPE_MYLIST]等参照
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoInfoCard(
    nicoVideoData: NicoVideoData?,
    isLiked: Boolean,
    onLikeClick: () -> Unit,
    isOffline: Boolean,
    description: String,
    descriptionClick: (id: String, type: String) -> Unit,
) {
    // 動画説明文表示状態
    val expanded = remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 10.dp, bottomStart = 10.dp),
        color = MaterialTheme.colors.primary.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {

            // 真ん中にする
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    // タイトル
                    Text(
                        text = nicoVideoData?.title ?: "",
                        fontSize = 20.sp,
                        maxLines = 2,
                    )
                    // 動画ID表示
                    Text(
                        text = nicoVideoData?.videoId ?: "",
                        fontSize = 12.sp,
                    )
                }
                // 展開ボタン。動画説明文の表示を切り替える
                IconButton(onClick = { expanded.value = !expanded.value }) {
                    Icon(
                        painter = if (expanded.value) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = stringResource(id = R.string.nicovideo_info)
                    )
                }
            }

            Row {
                Column(modifier = Modifier.weight(1f)) {
                    // マイリスト数とかコメント数とか
                    NicoVideoCountText(
                        viewCount = nicoVideoData?.viewCount?.toInt() ?: 0,
                        commentCount = nicoVideoData?.commentCount?.toInt() ?: 0,
                        mylistCount = nicoVideoData?.mylistCount?.toInt() ?: 0
                    )
                    // 投稿日時
                    NicoVideoUploadText(
                        uploadDateUnixTime = nicoVideoData?.date!!,
                        isCountDown = true
                    )
                }
                // いいねぼたん
                if (!isOffline) {
                    NicoVideoLikeButton(
                        isLiked = isLiked,
                        onLikeClick = onLikeClick,
                    )
                }
            }

            // 詳細表示
            if (expanded.value) {
                Column {
                    // 区切り線
                    Divider(modifier = Modifier.padding(5.dp))
                    /** 多分HTMLを表示する機能はないので従来のTextView登場 */
                    AndroidView(
                        factory = { context ->
                            TextView(context).apply {
                                // リンク押せるように
                                NicoVideoDescriptionText.setLinkText(text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT), this, descriptionClick)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * 投稿日を表示するやつ
 *
 * @param uploadDateUnixTime 投稿日。UnixTime
 * @param isCountDown 今日から何日前を表示する場合はtrue
 * */
@Composable
fun NicoVideoUploadText(uploadDateUnixTime: Long, isCountDown: Boolean = false) {
    Column {
        // お祝いメッセージ機能。お誕生日
        val anniversary = calcAnniversary(uploadDateUnixTime) // AnniversaryDateクラス みて
        // たんおめ
        val isBirthday = !(anniversary == 0) && anniversary != -1
        if (isBirthday) {
            Text(
                text = AnniversaryDate.makeAnniversaryMessage(anniversary),
                color = Color.Red,
                fontSize = 14.sp,
            )
        }
        // 投稿日時
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_event_available_24px),
                contentDescription = null,
            )
            Text(
                text = "${toFormatTime(uploadDateUnixTime)} ${stringResource(id = R.string.post)}",
                fontSize = 14.sp,
            )
        }
        if (isCountDown) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_history_24px),
                    contentDescription = null,
                )
                Text(
                    text = "今日から ${calcDayCount(toFormatTime(uploadDateUnixTime))} 日前に投稿",
                    fontSize = 14.sp,
                )
            }
        }
    }
}

/**
 * 再生数、マイリスト数、コメント数を表示するやつ
 *
 * @param viewCount 再生数
 * @param mylistCount マイリスト数
 * @param commentCount コメント数
 * */
@Composable
fun NicoVideoCountText(
    viewCount: Int,
    commentCount: Int,
    mylistCount: Int
) {
    // マイリスト数とかコメント数とか
    Row {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_play_arrow_24px),
                contentDescription = stringResource(id = R.string.view_count)
            )
            Text(
                text = viewCount.toString(),
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_comment_24px),
                contentDescription = stringResource(id = R.string.comment_count)
            )
            Text(
                text = commentCount.toString(),
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.ic_folder_open_black_24dp),
                contentDescription = stringResource(id = R.string.mylist_count)
            )
            Text(
                text = mylistCount.toString(),
                fontSize = 14.sp
            )
        }
    }
}

/**
 * いいねボタン。長いので切り出した
 * @param isLiked いいね済みかどうか。
 * @param onLikeClick いいねを押したら呼ばれる
 * @param scaffoldState Snackbar表示の際に使う。
 * [_root_ide_package_.androidx.compose.material.rememberScaffoldState()]をインスタンス化して[_root_ide_package_.androidx.compose.material.Scaffold() {}]
 * で指定したのを利用してください。
 * */
@ExperimentalMaterialApi
@Composable
private fun NicoVideoLikeButton(
    isLiked: Boolean,
    onLikeClick: () -> Unit,
) {
    // ピンク色
    val pinkColor = Color(android.graphics.Color.parseColor("#F69896"))

    // いいねボタン
    OutlinedButton(
        shape = RoundedCornerShape(20.dp), // 丸み
        onClick = { onLikeClick() },
    ) {
        Text(text = if (isLiked) stringResource(id = R.string.liked) else stringResource(id = R.string.like))
        Icon(
            painter = if (isLiked) painterResource(id = R.drawable.ic_favorite_black_24dp) else painterResource(id = R.drawable.ic_outline_favorite_border_24),
            tint = if (isLiked) pinkColor else LocalContentColor.current.copy(alpha = LocalContentColor.current.alpha),
            contentDescription = stringResource(id = R.string.like)
        )
    }
}


/**
 * 関連動画表示Card
 *
 * @param nicoVideoDataList [NicoVideoData]の配列
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoRecommendCard(nicoVideoDataList: List<NicoVideoData>) {
    Surface(
        modifier = Modifier.padding(5.dp),
        color = MaterialTheme.colors.primary.copy(0.2f),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // 関連動画
            Row(Modifier.padding(5.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_local_movies_24px),
                    contentDescription = null
                )
                Text(text = stringResource(id = R.string.recommend_video))
            }
/*
            // 一覧表示
            NicoVideoList(
                list = nicoVideoDataList,
                onVideoClick = { },
                onMenuClick = { }
            )
*/
/*
            // 一覧表示
            nicoVideoDataList.forEach { data ->
                NicoVideoListItem(
                    nicoVideoData = data,
                    onVideoClick = { },
                    onMenuClick = { }
                )
            }
*/
        }
    }
}

/**
 * ユーザー情報Card
 * @param userData ユーザー情報データクラス
 * @param onUserOpenClick ユーザー情報詳細ボタン押した時に呼ばれる
 * */
@Composable
fun NicoVideoUserUI(userData: UserData, onUserOpenClick: () -> Unit) {
    Surface(
        modifier = Modifier.padding(5.dp),
        color = MaterialTheme.colors.primary.copy(0.5f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 5.dp, bottom = 5.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bitmap = getBitmapCompose(url = userData.largeIcon)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    modifier = Modifier.clip(RoundedCornerShape(5.dp)),
                    contentDescription = null,
                )
            }
            Text(
                text = userData.nickName,
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
            )
            IconButton(onClick = { onUserOpenClick() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_outline_open_in_browser_24px),
                    contentDescription = stringResource(id = R.string.open_browser)
                )
            }
        }
    }
}

/**
 * タグ一覧表示Card
 * @param tagItemDataList [NicoTagItemData]配列
 * @param onTagClick 押したときに呼ばれる。
 * @param onNicoPediaClick ニコニコ大百科ボタンを押したときに呼ばれる
 * */
@Composable
fun NicoVideoTagCard(
    tagItemDataList: ArrayList<NicoTagItemData>,
    onTagClick: (NicoTagItemData) -> Unit,
    onNicoPediaClick: (String) -> Unit
) {
    // 展開状態かどうか
    val isShowAll = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.padding(5.dp),
        color = MaterialTheme.colors.primary.copy(0.5f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_local_offer_24px),
                    contentDescription = "タグ"
                )
                Text(
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    text = "${stringResource(id = R.string.tag)}：${tagItemDataList.subList(0, 3).joinToString(separator = "/", postfix = "...") { it.tagName }}"
                )
                // 展開ボタン
                IconButton(onClick = { isShowAll.value = !isShowAll.value }) {
                    if (isShowAll.value) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_expand_less_black_24dp),
                            contentDescription = "格納"
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_expand_more_24px),
                            contentDescription = "展開"
                        )
                    }
                }
            }
            if (isShowAll.value) {
                OrigamiLayout(
                    modifier = Modifier.padding(bottom = 10.dp),
                    isExpended = true
                ) {
                    tagItemDataList.forEach { data ->
                        // タグのボタン設置
                        TagButton(
                            data = data,
                            onClickTag = { onTagClick(it) },
                            onClickNicoPedia = { onNicoPediaClick(it) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * シリーズが設定されてる場合は表示する
 *
 * @param nicoVideoHTMLSeriesData シリーズ情報データクラス。次の動画とかを表示するため
 * @param onStartSeriesPlayClick 連続再生押した時
 * @param onFirstVideoPlayClick シリーズの最初の動画を再生するボタンを押した時
 * @param onNextVideoPlayClick 次の動画を再生するボタンを押した時
 * @param onPrevVideoPlayClick 前の動画を再生するボタンを押した時
 * */
@Composable
fun NicoVideoSeriesUI(
    nicoVideoHTMLSeriesData: NicoVideoHTMLSeriesData,
    onStartSeriesPlayClick: () -> Unit,
    onFirstVideoPlayClick: (NicoVideoData) -> Unit,
    onNextVideoPlayClick: (NicoVideoData) -> Unit,
    onPrevVideoPlayClick: (NicoVideoData) -> Unit,
) {

    /**
     * シリーズメニュー表示状態
     *
     * 本当は引数に出すべきなんだけど、なんか引数にすると全部のUIに更新が行ってしまう
     * */
    val expanded = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.padding(5.dp),
        color = MaterialTheme.colors.primary.copy(0.5f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // さむね
                val bitmap = getBitmapCompose(url = nicoVideoHTMLSeriesData.seriesData.thumbUrl)
                if (bitmap != null) {
                    // ちいさめ
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        contentDescription = null,
                    )
                }
                // タイトル
                Text(
                    text = "${stringResource(id = R.string.series)} : ${nicoVideoHTMLSeriesData.seriesData.title}",
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)
                )
                // 一覧表示
                IconButton(onClick = { expanded.value = !expanded.value }) {
                    Icon(
                        painter = if (expanded.value) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = "シリーズメニュー",
                    )
                }
            }
            if (expanded.value) {
                Column {
                    // 区切り
                    Divider()
                    // 連続再生開始などのメニュー
                    TextButton(onClick = { onStartSeriesPlayClick() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_play_arrow_24px),
                            contentDescription = stringResource(id = R.string.nicovideo_playlist_start)
                        )
                        Text(text = stringResource(id = R.string.nicovideo_playlist_start), modifier = Modifier.weight(1f))
                    }
                    // 最初から再生
                    if (nicoVideoHTMLSeriesData.firstVideoData != null) {
                        TextButton(onClick = { onFirstVideoPlayClick(nicoVideoHTMLSeriesData.firstVideoData) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_filter_1_24),
                                contentDescription = stringResource(id = R.string.nicovideo_series_first_video)
                            )
                            Text(text = "${stringResource(id = R.string.nicovideo_series_first_video)}\n${nicoVideoHTMLSeriesData.firstVideoData.title}", modifier = Modifier.weight(1f))
                        }
                    }
                    // 前の動画
                    if (nicoVideoHTMLSeriesData.prevVideoData != null) {
                        TextButton(onClick = { onPrevVideoPlayClick(nicoVideoHTMLSeriesData.prevVideoData) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back_black_24dp),
                                contentDescription = stringResource(id = R.string.nicovideo_series_prev_video)
                            )
                            Text(text = "${stringResource(id = R.string.nicovideo_series_prev_video)}\n${nicoVideoHTMLSeriesData.prevVideoData.title}", modifier = Modifier.weight(1f))
                        }
                    }
                    // 次の動画
                    if (nicoVideoHTMLSeriesData.nextVideoData != null) {
                        TextButton(onClick = { onNextVideoPlayClick(nicoVideoHTMLSeriesData.nextVideoData) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_arrow_forward_24),
                                contentDescription = stringResource(id = R.string.nicovideo_series_next_video)
                            )
                            Text(text = "${stringResource(id = R.string.nicovideo_series_next_video)}\n${nicoVideoHTMLSeriesData.nextVideoData.title}", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 連続再生一覧。BottomFragmentでも良かった？
 * */
@Composable
fun NicoVideoPlayList(
    isShowList: Boolean,
    showButtonClick: () -> Unit,
    videoList: ArrayList<NicoVideoData>,
    playingVideoId: String,
    videoClick: (String) -> Unit,
    isReverse: Boolean,
    reverseClick: () -> Unit,
    isShuffle: Boolean,
    shuffleClick: () -> Unit,
) {
    // 表示中かどうか
    val isPlaylistShow = remember { mutableStateOf(false) }

    // 選択中
    val playingColor = colorResource(id = R.color.colorAccent)
    // 影をつけるため？
    Surface(
        elevation = 10.dp
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = LocalContext.current.getDrawable(R.drawable.ic_tatimidroid_list_icon_black)?.toBitmap()?.asImageBitmap()
                if (icon != null) {
                    Icon(
                        bitmap = icon,
                        modifier = Modifier.padding(5.dp),
                        contentDescription = stringResource(id = R.string.playlist_button)
                    )
                }
                Text(
                    text = stringResource(id = R.string.playlist_button),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { isPlaylistShow.value = !isPlaylistShow.value }) {
                    Icon(
                        painter = if (isPlaylistShow.value) painterResource(id = R.drawable.ic_expand_less_black_24dp) else painterResource(id = R.drawable.ic_expand_more_24px),
                        contentDescription = "動画一覧"
                    )
                }
            }
            if (isPlaylistShow.value) {
                // シャッフルとか
                LazyRow {
                    item {
                        // 動画時間
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { }
                        ) {
                            // 何分か
                            val totalDuration = videoList.sumBy { nicoVideoData -> nicoVideoData.duration?.toInt() ?: 0 }
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_timer_24),
                                contentDescription = stringResource(id = R.string.playlist_total_time),
                            )
                            Text(text = "${stringResource(id = R.string.playlist_total_time)}：${DateUtils.formatElapsedTime(totalDuration.toLong())}")
                        }
                        // 作品数
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_view_list_black),
                                contentDescription = stringResource(id = R.string.video_count),
                            )
                            Text(text = "${stringResource(id = R.string.video_count)}：${videoList.size}")
                        }
                        // 逆順
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { reverseClick() }
                        ) {
                            Icon(
                                painter = if (isReverse) painterResource(id = R.drawable.ic_outline_check_box_24) else painterResource(id = R.drawable.ic_outline_check_box_outline_blank_24),
                                contentDescription = stringResource(id = R.string.reverse),
                            )
                            Text(text = stringResource(id = R.string.reverse))
                        }
                        // シャッフル
                        OutlinedButton(
                            modifier = Modifier.padding(2.dp),
                            onClick = { shuffleClick() }
                        ) {
                            Icon(
                                painter = if (isShuffle) painterResource(id = R.drawable.ic_outline_check_box_24) else painterResource(id = R.drawable.ic_outline_check_box_outline_blank_24),
                                contentDescription = stringResource(id = R.string.shuffle),
                            )
                            Text(text = stringResource(id = R.string.shuffle))
                        }
                    }
                }
                // 一覧表示
                val scope = rememberCoroutineScope()
                val state = rememberLazyListState()
                // スクロール実行
                scope.launch {
                    // 位置を特定
                    val index = videoList.indexOfFirst { it.videoId == playingVideoId }
                    if (index != -1) {
                        state.scrollToItem(index)
                    }
                }
                // RecyclerViewみたいに画面外は描画しないやつ
                LazyColumn(
                    state = state,
                    content = {
                        this.items(videoList) { data ->
                            Row(
                                modifier = Modifier
                                    .background(color = if (playingVideoId == data.videoId) playingColor else Color.Transparent)
                                    .clickable(onClick = {
                                        scope.launch {
                                            // 位置を特定
                                            val index = videoList.indexOfFirst { it.videoId == data.videoId }
                                            // スクロール実行
                                            state.scrollToItem(index)
                                        }
                                        videoClick(data.videoId)
                                    }),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bitmap = getBitmapCompose(url = data.thum)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        modifier = Modifier
                                            .height(60.dp)
                                            .width(110.dp)
                                            .padding(5.dp),
                                        contentDescription = null
                                    )
                                }
                                Text(
                                    text = data.title,
                                    maxLines = 2,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(5.dp),
                                )
                            }
                            // 区切り線
                            Divider()
                        }
                    }
                )
            }
            // 区切り線
            Divider()
        }
    }
}

/**
 * コメント一覧表示用Fabです
 *
 * @param isShowCommentList コメント一覧表示中かどうか。trueで表示中
 * @param click Fab押した時
 * */
@Composable
fun NicoVideoCommentListFab(
    isShowCommentList: Boolean,
    click: () -> Unit,
) {
    // コメント表示Fabを出す
    FloatingActionButton(
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 20.dp),
        onClick = {
            // 押した時
            click()
        })
    {
        Icon(
            painter = if (isShowCommentList) painterResource(id = R.drawable.ic_outline_info_24px) else painterResource(id = R.drawable.ic_outline_comment_24px),
            contentDescription = null
        )
    }
}

/**
 * Adapterにわたす引数が足りてない。から使う時気をつけて
 *
 * コメント一覧表示BottomSheet。Jetpack Compose結構揃ってる。なお現状めっちゃ落ちるので使ってない。バージョン上がったら使いたい。
 *
 * 注意 このレイアウトを最上位にしてその他は[content]の中に書いてください。
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoCommentBottomSheet(commentList: ArrayList<CommentJSONParse>, commentClick: (CommentJSONParse) -> Unit, content: @Composable () -> Unit) {
    // BottomSheetを表示させるかどうか
    val bottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.HalfExpanded)

    ModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetElevation = 10.dp,
        sheetShape = RoundedCornerShape(10.dp, 10.dp, 0.dp, 0.dp),
        sheetContent = {
            LazyColumn(content = {
                this.item {
                    Column {
                        // コメントBottomSheetだよー
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_outline_comment_24px),
                                contentDescription = null
                            )
                            Text(text = stringResource(id = R.string.comment))
                        }
                        // コメント一覧。AndroidViewで既存のRecyclerViewを使い回す。
                        AndroidView(factory = { context ->
                            RecyclerView(context).apply {
                                setHasFixedSize(true)
                                layoutManager = LinearLayoutManager(context)
                                // adapter = NicoVideoAdapter(commentList)
                                val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
                                addItemDecoration(itemDecoration)
                            }
                        })
                    }
                }
            })
        },
        content = {
            // モーダル以外のレイアウト
            content()
        }
    )
}

/** BottomSheet表示用FAB */
@Composable
fun BottomSheetFab(fabClick: () -> Unit) {
    // BottomSheetを表示するためのFab
    // 右下にするために
    Column(
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
    ) {
        // コメント表示Fabを出す
        FloatingActionButton(modifier = Modifier.padding(16.dp),
            onClick = {
                // 押した時
                fabClick()
            }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_outline_comment_24px),
                contentDescription = null
            )
        }
    }
}

/*
@Preview
@Composable
fun PreviewVideoInfoCard() {
    VideoInfoCard()
}

*/
