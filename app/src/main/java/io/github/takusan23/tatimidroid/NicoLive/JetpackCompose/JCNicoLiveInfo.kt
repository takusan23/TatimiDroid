package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.CommunityOrChannelData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.NicoAPI.NicoLive.DataClass.NicoTagItemData
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.getBitmapCompose
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.parentCardElevation
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.parentCardModifier
import io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose.parentCardShape
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Tool.toFormatTime

/**
 * 番組情報を表示するCard
 * @param nicoLiveProgramData 番組情報データクラス
 * @param programDescription 番組説明文
 * @param isRegisteredTimeShift タイムシフト予約済みかどうか
 * @param onClickTimeShift タイムシフト予約ボタンを押した時
 * */
@Composable
fun NicoLiveInfoCard(
    nicoLiveProgramData: NicoLiveProgramData,
    programDescription: String,
    isRegisteredTimeShift: Boolean,
    onClickTimeShift: () -> Unit,
) {
    // 動画説明文表示状態
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
        ) {
            // 番組開始、終了時刻
            Row {
                Icon(
                    imageVector = Icons.Outlined.MeetingRoom
                )
                Text(
                    text = "${stringResource(id = R.string.nicolive_begin_time)}：${toFormatTime(nicoLiveProgramData.beginAt.toLong() * 1000)}",
                )
            }
            Row {
                Icon(
                    imageVector = Icons.Outlined.NoMeetingRoom
                )
                Text(
                    text = "${stringResource(id = R.string.nicolive_end_time)}：${toFormatTime(nicoLiveProgramData.endAt.toLong() * 1000)}",
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
                        text = nicoLiveProgramData.title,
                        style = TextStyle(fontSize = 18.sp),
                        maxLines = 2,
                    )
                    // 生放送ID
                    Text(
                        text = nicoLiveProgramData.programId,
                        style = TextStyle(fontSize = 12.sp),
                    )
                }
                // タイムシフト予約ボタン
                TimeShiftRegisterButton(
                    isRegisteredTimeShift = isRegisteredTimeShift,
                    onClickTimeShift = onClickTimeShift
                )
                // 展開ボタン。動画説明文の表示を切り替える
                IconButton(onClick = { expanded = !expanded }) {
                    // アイコンコード一行で召喚できる。Node.jsのnpmのmdiみたいだな！
                    Icon(imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore)
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
                            text = HtmlCompat.fromHtml(programDescription, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        }
                    })
                }
            }
        }
    }
}

/**
 * タイムシフト予約ボタン
 *
 * @param isRegisteredTimeShift タイムシフト予約済みかどうか
 * @param onClickTimeShift タイムシフト予約ボタン押した時
 * */
@Composable
fun TimeShiftRegisterButton(
    isRegisteredTimeShift: Boolean,
    onClickTimeShift: () -> Unit
) {
    // いいねボタン
    OutlinedButton(
        shape = RoundedCornerShape(20.dp), // 丸み
        onClick = { onClickTimeShift() },
    ) {
        Icon(imageVector = Icons.Outlined.History)
        Text(text = if (isRegisteredTimeShift) stringResource(id = R.string.nicolive_time_shift_un_register_short) else stringResource(id = R.string.nicolive_time_shift_register_short))
    }
}

/**
 * コミュニティー情報表示Card
 *
 * @param communityOrChannelData コミュ、番組情報
 * @param onCommunityOpenClick コミュ情報押した時に呼ばれる
 * @param isFollow フォロー中かどうか
 * @param onFollowClick フォロー押した時
 * */
@Composable
fun NicoLiveCommunityCard(
    communityOrChannelData: CommunityOrChannelData,
    onCommunityOpenClick: () -> Unit,
    isFollow: Boolean,
    onFollowClick: () -> Unit,
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.SupervisorAccount)
                Text(text = stringResource(id = R.string.community_name))
            }
            Divider(modifier = Modifier.padding(5.dp))
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val bitmap = getBitmapCompose(url = communityOrChannelData.icon)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    )
                }
                Text(
                    text = communityOrChannelData.name,
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)
                )
            }
            Divider(modifier = Modifier.padding(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // コミュだけフォローボタンを出す
                if (!communityOrChannelData.isChannel) {
                    TextButton(modifier = Modifier.padding(3.dp), onClick = { onFollowClick() }) {
                        if (isFollow) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.PersonRemove)
                                Text(text = stringResource(id = R.string.nicovideo_account_remove_follow))
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.StarBorder)
                                Text(text = stringResource(id = R.string.community_follow))
                            }
                        }
                    }
                }
                TextButton(modifier = Modifier.padding(3.dp), onClick = { onCommunityOpenClick() }) {
                    Icon(imageVector = Icons.Outlined.OpenInBrowser)
                }
            }
        }
    }
}

/**
 * タグ表示Card。動画とは互換性がない（データクラスが違うの）
 * @param list [NicoTagItemData]の配列
 * @param onTagClick タグを押した時
 * @param isEditable 編集可能かどうか。falseで編集ボタンを非表示にします。
 * @param onClickEditButton 編集ボタンを押した時
 * */
@Composable
fun NicoLiveTagCard(
    list: ArrayList<NicoTagItemData>,
    onTagClick: (NicoTagItemData) -> Unit,
    isEditable: Boolean,
    onClickEditButton: () -> Unit,
) {
    Card(
        modifier = parentCardModifier.fillMaxWidth(),
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        // 横方向スクロール。LazyRowでRecyclerViewみたいに画面外は描画しない
        LazyRow(
            modifier = Modifier.padding(3.dp),
            content = {
                this.item {
                    // 編集ボタン
                    if (isEditable) {
                        Button(
                            modifier = Modifier.padding(3.dp),
                            onClick = { onClickEditButton() }
                        ) {
                            Icon(imageVector = Icons.Outlined.Edit)
                            Text(text = stringResource(id = R.string.tag_edit))
                        }
                    }
                }
                this.items(list) { data ->
                    OutlinedButton(
                        modifier = Modifier.padding(3.dp),
                        onClick = {
                            onTagClick(data)
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
 * 好みタグ表示Card。いまいちよくわからん機能
 *
 * @param konomiTagList 好みタグの文字列配列。いまんところ文字列の配列でいいや（そもそもこの機能いる？）
 * */
@Composable
fun NicoLiveKonomiCard(
    konomiTagList: ArrayList<String>
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            Row(
                modifier = Modifier.padding(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Outlined.FavoriteBorder)
                Text(text = stringResource(id = R.string.konomi_tag))
            }
            Divider(modifier = Modifier.padding(5.dp))
            // 0件の場合
            if (konomiTagList.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.konomi_tag_empty),
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                konomiTagList.forEach { text ->
                    Text(text = text, modifier = Modifier.padding(10.dp))
                    Divider()
                }
            }
        }
    }
}

/**
 * ニコニ広告 / 投げ銭のポイントを表示するCard
 *
 * @param totalNicoAdPoint 広告ポイント
 * @param totalGiftPoint 投げ銭ポイント
 * @param onClickNicoAdOpen ニコニ広告画面に遷移するボタンを押した時
 * @param onClickGiftOpen 投げ銭画面に遷移するボタンを押した時
 * */
@Composable
fun NicoLivePointCard(
    totalNicoAdPoint: Int,
    totalGiftPoint: Int,
    onClickNicoAdOpen: () -> Unit,
    onClickGiftOpen: () -> Unit,
) {
    Card(
        modifier = parentCardModifier,
        shape = parentCardShape,
        elevation = parentCardElevation,
    ) {
        Column {
            // ニコニ広告、投げ銭を表示
            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .fillMaxWidth(),
            ) {
                // ニコニ広告
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Outlined.Money)
                        Text(text = stringResource(id = R.string.nicoads))
                    }
                    Divider()
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(5.dp),
                        text = "$totalNicoAdPoint pt",
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                    )
                    TextButton(
                        onClick = { onClickNicoAdOpen() },
                        modifier = Modifier
                            .align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Outlined.ArrowForward)
                        Text(text = stringResource(id = R.string.show_nicoad))
                    }
                }
                // 投げ銭
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Outlined.CardGiftcard)
                        Text(text = stringResource(id = R.string.gift))
                    }
                    Divider()
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(5.dp),
                        text = "$totalGiftPoint pt",
                        textAlign = TextAlign.Center,
                        fontSize = 20.sp,
                    )
                    TextButton(
                        onClick = { onClickGiftOpen() },
                        modifier = Modifier
                            .align(Alignment.End)
                    ) {
                        Icon(imageVector = Icons.Outlined.ArrowForward)
                        Text(text = stringResource(id = R.string.show_gift))
                    }
                }
            }
        }
    }
}

