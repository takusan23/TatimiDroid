package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveTagDataClass
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoAPI.User.UserData
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoListAdapter
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.*
import kotlinx.coroutines.launch

/**
 * Jetpack Compose 略してJC
 * */

/** [NicoVideoInfoCard]とかの親のCardに指定するModifier */
private val parentCardModifier = Modifier.padding(5.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定する丸み */
private val parentCardShape = RoundedCornerShape(3.dp)

/** [NicoVideoInfoCard]とかの親のCardに指定するElevation */
private val parentCardElevation = 3.dp

/**
 * 動画説明、タイトルCard
 * @param nicoVideoData ニコ動データクラス。
 * @param isLiked いいねしたかどうか。LiveDataの変更を検知する形になってるはず（[androidx.lifecycle.LiveData.observeAsState]）
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
                    text = "投稿日：${toFormatTime(nicoVideoData?.date ?: 0L)}",
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
                NicoVideoLikeButton(
                    isLiked = isLiked,
                    scaffoldState = scaffoldState,
                    postLike = postLike,
                    postRemoveLike = postRemoveLike
                )
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
                    val click = scaffoldState.snackbarHostState.showSnackbar(
                        message = removeMessage,
                        actionLabel = torikesu,
                        duration = SnackbarDuration.Short,
                    )
                    if (click == SnackbarResult.ActionPerformed) {
                        // Snackbarのボタン押した時
                        postRemoveLike()
                    }
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

/*
@Preview
@Composable
fun PreviewVideoInfoCard() {
    VideoInfoCard()
}

*/
