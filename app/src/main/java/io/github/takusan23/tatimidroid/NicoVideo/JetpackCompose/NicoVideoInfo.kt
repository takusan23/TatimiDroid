package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import android.text.format.DateUtils
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.CommentJSONParse
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveTagDataClass
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoSeriesData
import io.github.takusan23.tatimidroid.NicoAPI.User.UserData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.*
import kotlinx.coroutines.launch

/**
 * Jetpack Compose 略してJC
 * */

/** [NicoVideoInfoCard]とかの親のCardに指定するModifier */
val parentCardModifier = Modifier.padding(5.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定する丸み */
val parentCardShape = RoundedCornerShape(3.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定するElevation */
val parentCardElevation = 3.dp

/**
 * 動画説明、タイトルCard
 * @param nicoVideoData ニコ動データクラス。
 * @param isLiked いいねしたかどうか。LiveDataの変更を検知する形になってるはず（[androidx.lifecycle.LiveData.observeAsState]）
 * @param isOffline キャッシュ再生用。trueにするといいねボタンを非表示にします。
 * @param description 動画説明文
 * @param postLike いいね押した時
 * @param postRemoveLike いいね解除押した時
 * @param scaffoldState Snackbar表示で使う。[_root_ide_package_.androidx.compose.material.Scaffold]を使おう
 * @param descriptionClick 動画説明文のリンクを押した時。[NicoVideoDescriptionText.DESCRIPTION_TYPE_MYLIST]等参照
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoInfoCard(
    nicoVideoData: NicoVideoData?,
    isLiked: State<Boolean>,
    isOffline: Boolean,
    scaffoldState: ScaffoldState,
    description: String,
    postLike: () -> Unit,
    postRemoveLike: () -> Unit,
    descriptionClick: (id: String, type: String) -> Unit,
) {
    // 動画説明文表示状態
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = parentCardModifier,
        // border = BorderStroke(1.dp, Color.Black),
        shape = parentCardShape,
        // backgroundColor = Color.Transparent,
        elevation = parentCardElevation,
    ) {

        Column(
            modifier = Modifier.padding(10.dp),
        ) {

            // お祝いメッセージ機能。お誕生日
            val anniversary = calcAnniversary(nicoVideoData?.date ?: 0L) // AnniversaryDateクラス みて
            // たんおめ
            val isBirthday = !(anniversary == 0) && anniversary != -1
            if (isBirthday) {
                Text(
                    text = AnniversaryDate.makeAnniversaryMessage(anniversary),
                    color = Color.Red
                )
            }

            // 投稿日時
            Row {
                Icon(
                    imageVector = Icons.Outlined.EventAvailable
                )
                Text(
                    text = "${stringResource(id = R.string.post_date)}：${toFormatTime(nicoVideoData?.date ?: 0L)}",
                )
            }
            Row {
                Icon(
                    imageVector = Icons.Outlined.History
                )
                Text(
                    text = "今日から ${calcDayCount(toFormatTime(nicoVideoData?.date ?: 0L))} 日前に投稿",
                )
            }
            // 区切り線
            Divider(modifier = Modifier.padding(5.dp))

            // 真ん中にする
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    // タイトル
                    Text(
                        text = nicoVideoData?.title ?: "",
                        style = TextStyle(fontSize = 18.sp),
                        maxLines = 2,
                    )
                    // 動画ID表示
                    Text(
                        text = nicoVideoData?.videoId ?: "",
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
                // いいねぼたん
                if (!isOffline) {
                    NicoVideoLikeButton(
                        isLiked = isLiked,
                        scaffoldState = scaffoldState,
                        postLike = postLike,
                        postRemoveLike = postRemoveLike
                    )
                }
                // 展開ボタン
                IconButton(onClick = {
                    // 動画説明文の表示を切り替える
                    expanded = !expanded
                }) {
                    // アイコンコード一行で召喚できる。Node.jsのnpmのmdiみたいだな！
                    Icon(imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore)
                }
            }

            // マイリスト数とかコメント数とか
            Row {
                Row {
                    Icon(imageVector = Icons.Outlined.PlayArrow)
                    Text(text = nicoVideoData?.viewCount ?: "0")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row {
                    Icon(imageVector = Icons.Outlined.Comment)
                    Text(text = nicoVideoData?.commentCount ?: "0")
                }
                Spacer(modifier = Modifier.width(10.dp))
                Row {
                    Icon(imageVector = Icons.Outlined.Folder)
                    Text(text = nicoVideoData?.mylistCount ?: "0")
                }
            }

            // 詳細表示
            if (expanded) {
                Column {
                    // 区切り線
                    Divider(modifier = Modifier.padding(5.dp))
                    /** 多分HTMLを表示する機能はないので従来のTextView登場 */
                    AndroidView(viewBlock = { context ->
                        TextView(context).apply {
                            // リンク押せるように
                            NicoVideoDescriptionText.setLinkText(text = HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT), this, descriptionClick)
                        }
                    })
                }
            }

        }
    }
}

/**
 * いいねボタン。長いので切り出した
 * @param isLiked いいね済みかどうか。LiveData経由でいいねの状態は更新されるはずです。[androidx.lifecycle.LiveData.observeAsState]
 * @param postLike いいねを押したときに呼ばれる。
 * @param postRemoveLike いいね解除のときに呼ばれる
 * @param scaffoldState Snackbar表示の際に使う。
 * [_root_ide_package_.androidx.compose.material.rememberScaffoldState()]をインスタンス化して[_root_ide_package_.androidx.compose.material.Scaffold() {}]
 * で指定したのを利用してください。
 * */
@ExperimentalMaterialApi
@Composable
fun NicoVideoLikeButton(isLiked: State<Boolean>, scaffoldState: ScaffoldState, postLike: () -> Unit, postRemoveLike: () -> Unit) {
    // Snackbar表示で使うコルーチンスコープ
    val scope = rememberCoroutineScope()
    // リソース取得
    val removeMessage = stringResource(id = R.string.unlike)
    val torikesu = stringResource(id = R.string.torikesu)
    // いいねボタン
    OutlinedButton(
        shape = RoundedCornerShape(20.dp), // 丸み
        onClick = {
            scope.launch {
                if (isLiked.value) {
                    // 登録済みなら
                    // Snackbarのボタン押した時
                    postRemoveLike()
                } else {
                    postLike()
                }
            }
        },
    ) {
        Text(text = if (isLiked.value) stringResource(id = R.string.liked) else stringResource(id = R.string.like))
        Icon(imageVector = Icons.Outlined.FavoriteBorder)
    }
}


/**
 * 関連動画表示Card。従来のRecyclerViewを置いてる。使い回せるGJ
 *
 * @param nicoVideoDataList [NicoVideoData]の配列
 * */
@Composable
fun NicoVideoRecommendCard(nicoVideoDataList: ArrayList<NicoVideoData>) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {
            // 関連動画
            Row {
                Icon(imageVector = Icons.Outlined.LocalMovies)
                Text(text = stringResource(id = R.string.recommend_video))
            }
            // 一覧表示。RecyclerViewを使い回す
            AndroidView(viewBlock = { context ->
                RecyclerView(context).apply {
                    setHasFixedSize(true)
                    isNestedScrollingEnabled = false // これしないと関連動画スクロールした状態でミニプレイヤーに遷移できない
                    layoutManager = LinearLayoutManager(context)
                    adapter = NicoVideoListAdapter(nicoVideoDataList)
                }
            })
        }
    }
}

/**
 * ユーザー情報Card
 * @param userData ユーザー情報データクラス
 * @param userOpenClick ユーザー情報詳細ボタン押した時に呼ばれる
 * */
@Composable
fun NicoVideoUserCard(userData: UserData, userOpenClick: () -> Unit) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bitmap = getBitmapCompose(url = userData.largeIcon)
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    modifier = Modifier.clip(RoundedCornerShape(5.dp))
                )
            }
            Text(
                text = userData.nickName,
                modifier = Modifier.weight(1f).padding(5.dp)
            )
            OutlinedButton(onClick = {
                userOpenClick()
            }) {
                Icon(imageVector = Icons.Outlined.OpenInBrowser)
            }
        }
    }
}

/**
 * タグ一覧表示Card
 * @param tagDataList [NicoLiveTagDataClass]配列
 * @param tagClick 押したときに呼ばれる。
 * */
@Composable
fun NicoVideoTagCard(tagDataList: ArrayList<NicoLiveTagDataClass>, tagClick: (NicoLiveTagDataClass) -> Unit) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        // 横方向スクロール。LazyRowでRecyclerViewみたいに画面外は描画しない
        LazyRow(
            modifier = Modifier.padding(3.dp),
            content = {
                this.items(tagDataList) { data ->
                    OutlinedButton(
                        modifier = Modifier.padding(3.dp),
                        onClick = {
                            tagClick(data)
                        },
                    ) {
                        Icon(imageVector = Icons.Outlined.LocalOffer)
                        Text(text = data.tagName)
                    }
                }
            }
        )
    }
}

/**
 * シリーズが設定されてる場合は表示する
 *
 * @param nicoVideoSeriesData シリーズ情報データクラス
 * @param startSeriesPlay 連続再生押した時
 * */
@Composable
fun NicoVideoSeriesCard(
    nicoVideoSeriesData: NicoVideoSeriesData,
    startSeriesPlay: () -> Unit,
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {
            Row {
                Icon(imageVector = Icons.Outlined.Folder)
                Text(text = stringResource(id = R.string.series))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // さむね
                val bitmap = getBitmapCompose(url = nicoVideoSeriesData.thumbUrl)
                if (bitmap != null) {
                    // ちいさめ
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        modifier = Modifier.height(40.dp).clip(RoundedCornerShape(5.dp))
                    )
                }
                // タイトル
                Text(
                    text = nicoVideoSeriesData.title,
                    modifier = Modifier.weight(1f).padding(5.dp)
                )
                // 一覧表示
                OutlinedButton(onClick = { startSeriesPlay() }) {
                    Icon(imageVector = Icons.Outlined.PlayArrow)
                    Text(text = stringResource(id = R.string.nicovideo_playlist_start))
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
    // 選択中
    val playingColor = colorResource(id = R.color.colorAccent)
    // 影をつけるため？
    Surface(
        elevation = 10.dp
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = AmbientContext.current.getDrawable(R.drawable.ic_tatimidroid_list_icon_black)?.toBitmap()?.asImageBitmap()
                if (icon != null) {
                    Icon(
                        bitmap = icon,
                        modifier = Modifier.padding(5.dp),
                    )
                }
                Text(
                    text = stringResource(id = R.string.playlist_button),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showButtonClick() }) {
                    Icon(
                        imageVector = if (isShowList) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore
                    )
                }
            }
            if (isShowList) {
                // シャッフルとか
                ScrollableRow {
                    // 動画時間
                    OutlinedButton(
                        modifier = Modifier.padding(2.dp),
                        onClick = { }
                    ) {
                        // 何分か
                        val totalDuration = videoList.sumBy { nicoVideoData -> nicoVideoData.duration?.toInt() ?: 0 }
                        Icon(imageVector = Icons.Outlined.Timer)
                        Text(text = "${stringResource(id = R.string.playlist_total_time)}：${DateUtils.formatElapsedTime(totalDuration.toLong())}")
                    }
                    // 作品数
                    OutlinedButton(
                        modifier = Modifier.padding(2.dp),
                        onClick = { }
                    ) {
                        Icon(imageVector = Icons.Outlined.ViewList)
                        Text(text = "${stringResource(id = R.string.video_count)}：${videoList.size}")
                    }
                    // 逆順
                    OutlinedButton(
                        modifier = Modifier.padding(2.dp),
                        onClick = { reverseClick() }
                    ) {
                        Icon(imageVector = if (isReverse) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank)
                        Text(text = stringResource(id = R.string.reverse))
                    }
                    // シャッフル
                    OutlinedButton(
                        modifier = Modifier.padding(2.dp),
                        onClick = { shuffleClick() }
                    ) {
                        Icon(imageVector = if (isShuffle) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank)
                        Text(text = stringResource(id = R.string.shuffle))
                    }
                }
                // 一覧表示
                val scope = rememberCoroutineScope()
                val state = rememberLazyListState()
                // スクロール実行
                scope.launch {
                    // 位置を特定
                    val index = videoList.indexOfFirst { it.videoId == playingVideoId }
                    state.snapToItemIndex(index)
                }
                // RecyclerViewみたいに画面外は描画しないやつ
                LazyColumn(
                    state = state,
                    content = {
                        this.items(videoList) { data ->
                            Row(
                                modifier = Modifier
                                    .background(color = if (playingVideoId == data.videoId) playingColor else Color.Transparent),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bitmap = getBitmapCompose(url = data.thum)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        modifier = Modifier.height(60.dp).width(110.dp).padding(5.dp)
                                    )
                                }
                                Text(
                                    text = data.title,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f).padding(5.dp),
                                )
                                // 再生
                                IconButton(onClick = {
                                    scope.launch {
                                        // 位置を特定
                                        val index = videoList.indexOfFirst { it.videoId == data.videoId }
                                        // スクロール実行
                                        state.snapToItemIndex(index)
                                    }
                                    videoClick(data.videoId)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        modifier = Modifier.padding(10.dp),
                                    )
                                }
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
        Icon(imageVector = if (isShowCommentList) Icons.Outlined.Info else Icons.Outlined.Comment)
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
                            Icon(imageVector = Icons.Outlined.Comment)
                            Text(text = stringResource(id = R.string.comment))
                        }
                        // コメント一覧。AndroidViewで既存のRecyclerViewを使い回す。
                        AndroidView(viewBlock = { context ->
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
        modifier = Modifier.fillMaxHeight().fillMaxWidth(),
    ) {
        // コメント表示Fabを出す
        FloatingActionButton(modifier = Modifier.padding(16.dp),
            onClick = {
                // 押した時
                fabClick()
            }) {
            Icon(imageVector = Icons.Outlined.Comment)
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
