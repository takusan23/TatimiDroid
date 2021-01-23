package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollableRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.NicoAPI.CommentColorList
import io.github.takusan23.tatimidroid.R


/**
 * 生放送のコメント表示用、投稿UIをComposeで作成する
 * @param onClick ボタンを押した時
 * @param isComment コメントのアイコンを表示する場合はtrue
 * @param comment コメント本文
 * @param isPremium プレミアム会員かどうか。trueにするとプレ垢限定色を開放します。
 * @param commentChange コメントInputに変更が入ったときに呼ばれる
 * @param is184 匿名で投稿する場合はtrue。もしfalseになった場合はテキストボックスのヒントにに生IDで投稿されるという旨が表示されます。
 * @param onPostClick 投稿ボタン押した時
 * @param isMultiLine 複数行コメントを送信する場合はtrue。falseの場合はEnterキーを送信キーに変更します。
 * @param isHideCommentInputLayout コメント入力テキストボックス非表示にするかどうか
 * @param onHideCommentInputLayoutChange コメント入力テキストボックスの表示が切り替わったら呼ばれる
 * @param onPosValueChange 固定位置が変わったら呼ばれる
 * @param onSizeValueChange 大きさが変わったら呼ばれる
 * @param onColorValueChange 色が変わったら呼ばれる
 * @param onTokumeiChange いやよ、生IDが切り替わったら呼ばれる。trueで匿名
 * @param position コメントの位置
 * @param size コメントの大きさ
 * @param color コメントの色
 * @param onChangeMultiLine 複数行での投稿を有効にした場合は呼ばれる
 * */
@ExperimentalAnimationApi
@Composable
fun NicoLiveCommentInputButton(
    onClick: () -> Unit,
    isComment: Boolean,
    is184: Boolean = true,
    isPremium: Boolean = true,
    comment: String,
    commentChange: (String) -> Unit,
    onPostClick: () -> Unit,
    isMultiLine: Boolean,
    isHideCommentInputLayout: Boolean = false,
    onHideCommentInputLayoutChange: (Boolean) -> Unit = {},
    position: String,
    size: String,
    color: String,
    onPosValueChange: (String) -> Unit,
    onSizeValueChange: (String) -> Unit,
    onColorValueChange: (String) -> Unit,
    onTokumeiChange: (Boolean) -> Unit
) {
    // コメント入力テキストボックスを格納するかどうか
    val isHideCommentLayout = remember { mutableStateOf(isHideCommentInputLayout) }
    // コメント入力テキストボックスの表示、非表示変更時に呼ばれるやつ
    onHideCommentInputLayoutChange(isHideCommentLayout.value)
    // コマンドパネル表示するか
    val isShowCommandPanel = remember { mutableStateOf(false) }

    // margin代わり
    Column(
        modifier = Modifier.background(
            colorResource(id = R.color.colorPrimary),
            RoundedCornerShape(
                // コメント入力テキストボックス表示中は角を丸くしない
                topLeft = if (!isHideCommentLayout.value) 0.dp else 20.dp,
                topRight = 0.dp,
                bottomRight = 0.dp,
                bottomLeft = 0.dp
            )
        ),
    ) {
        // コマンドパネル
        if (isShowCommandPanel.value && !isHideCommentLayout.value) {
            NicoLiveCommentCommandPanel(
                isPremium = isPremium,
                is184 = is184,
                position = position,
                size = size,
                color = color,
                onPosValueChange = onPosValueChange,
                onSizeValueChange = onSizeValueChange,
                onColorValueChange = onColorValueChange,
                onTokumeiChange = onTokumeiChange
            )
        }
        // コメント投稿欄
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically, // 真ん中にする
        ) {
            // コメント投稿エリア収納
            IconButton(onClick = { isHideCommentLayout.value = !isHideCommentLayout.value }) {
                Icon(
                    imageVector = if (isHideCommentLayout.value) Icons.Outlined.Create else Icons.Outlined.KeyboardArrowRight,
                    tint = Color.White,
                )
            }
            // コメント入力展開するか
            if (!isHideCommentLayout.value) {
                // コマンドパネル
                IconButton(onClick = { isShowCommandPanel.value = !isShowCommandPanel.value }) {
                    Icon(
                        imageVector = Icons.Outlined.FormatPaint,
                        tint = Color.White,
                    )
                }
                // コメント入力
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = comment,
                    label = {
                        Text(
                            text = if (is184) {
                                stringResource(id = R.string.comment)
                            } else {
                                // 生IDで投稿する旨を表示
                                "${stringResource(id = R.string.comment)} ${stringResource(id = R.string.disabled_tokumei_comment)}"
                            }
                        )
                    },
                    onValueChange = { commentChange(it) },
                    activeColor = Color.White,
                    inactiveColor = Color.White,
                    textStyle = TextStyle(Color.White),
                    // 複数行投稿が無効な場合はEnterキーを送信、そうじゃない場合は改行へ
                    keyboardOptions = if (!isMultiLine) KeyboardOptions(imeAction = ImeAction.Send) else KeyboardOptions.Default,
                    onImeActionPerformed = { imeAction, softwareKeyboardController ->
                        if (imeAction == ImeAction.Send) {
                            // 送信！
                            onPostClick()
                        }
                    }
                )
                // 投稿ボタン
                IconButton(onClick = { onPostClick() }) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        tint = Color.White,
                    )
                }
            }
            // ボタン
            IconButton(onClick = { onClick() }) {
                Icon(
                    imageVector = if (isComment) Icons.Outlined.Comment else Icons.Outlined.Info,
                    tint = Color.White,
                )
            }
        }
    }

}

/**
 * コマンドを選ぶやつ。色とか位置とか。
 *
 * @param isPremium プレミアム会員かどうか。trueにするとプレ垢限定色を使うことができます
 * （というかカラーコードがそのまま使えるようになる方が有能だったりする）
 * @param is184 匿名で投稿するか。
 * @param position コメントの位置
 * @param size コメントの大きさ
 * @param color コメントの色
 * @param onPosValueChange 固定位置が変わったら呼ばれる
 * @param onSizeValueChange 大きさが変わったら呼ばれる
 * @param onColorValueChange 色が変わったら呼ばれる
 * @param onTokumeiChange いやよ、生IDが切り替わったら呼ばれる。trueで匿名
 * */
@Composable
fun NicoLiveCommentCommandPanel(
    isPremium: Boolean = true,
    is184: Boolean = true,
    position: String,
    size: String,
    color: String,
    onPosValueChange: (String) -> Unit,
    onSizeValueChange: (String) -> Unit,
    onColorValueChange: (String) -> Unit,
    onTokumeiChange: (Boolean) -> Unit,
) {
    // 一般
    val colorList = CommentColorList.COLOR_LIST
    // プレ垢のみ
    val premiumColorList = CommentColorList.PREMIUM_COLOR_LIST
    // ボタンの色
    val buttonColor = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
    // ボタンのアウトラインの色
    val buttonOutlineColor = BorderStroke(1.dp, Color.White)
    // どの位置にしたか
    val selectPos = remember { mutableStateOf(position) }
    // どの大きさにしたか
    val selectSize = remember { mutableStateOf(size) }
    // どの色押したかどうか
    val selectColor = remember { mutableStateOf(color) }
    // 引数の関数たちをよぶ
    onPosValueChange(selectPos.value)
    onSizeValueChange(selectSize.value)
    onColorValueChange(selectColor.value)
    Column(modifier = Modifier.padding(5.dp)) {
        // サイズ
        Row {
            OutlinedButton(
                onClick = { selectSize.value = "big" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "大", fontSize = 15.sp, color = Color.White)
            }
            OutlinedButton(
                onClick = { selectSize.value = "medium" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "中", fontSize = 13.sp, color = Color.White)
            }
            OutlinedButton(
                onClick = { selectSize.value = "small" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "小", fontSize = 10.sp, color = Color.White)
            }
        }
        // 位置
        Row {
            OutlinedButton(
                onClick = { selectPos.value = "ue" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "↑", color = Color.White)
            }
            OutlinedButton(
                onClick = { selectPos.value = "naka" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "←", color = Color.White)
            }
            OutlinedButton(
                onClick = { selectPos.value = "shita" },
                colors = buttonColor,
                border = buttonOutlineColor,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
            ) {
                Text(text = "↓", color = Color.White)
            }
        }
        // 一般でも使える
        ScrollableRow {
            colorList.forEach { color ->
                Button(
                    onClick = { selectColor.value = color.name },
                    modifier = Modifier.padding(5.dp),
                    colors = ButtonDefaults.textButtonColors(backgroundColor = Color(android.graphics.Color.parseColor(color.colorCode)))
                ) {

                }
            }
        }
        // プレミアム限定
        if (isPremium) {
            ScrollableRow {
                premiumColorList.forEach { color ->
                    Button(
                        onClick = { selectColor.value = color.name },
                        modifier = Modifier.padding(5.dp),
                        colors = ButtonDefaults.textButtonColors(backgroundColor = Color(android.graphics.Color.parseColor(color.colorCode)))
                    ) {

                    }
                }
            }
        }
        // それぞれのテキストボックス
        Row {
            OutlinedTextField(
                value = selectPos.value,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                inactiveColor = Color.White,
                activeColor = Color.White,
                textStyle = TextStyle(Color.White),
                label = { Text(text = stringResource(id = R.string.position)) },
                onValueChange = { selectPos.value = it }
            )
            OutlinedTextField(
                value = selectSize.value,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                inactiveColor = Color.White,
                activeColor = Color.White,
                textStyle = TextStyle(Color.White),
                label = { Text(text = stringResource(id = R.string.size)) },
                onValueChange = { selectSize.value = it }
            )
            OutlinedTextField(
                value = selectColor.value,
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                inactiveColor = Color.White,
                activeColor = Color.White,
                textStyle = TextStyle(Color.White),
                label = { Text(text = stringResource(id = R.string.color)) },
                onValueChange = { selectColor.value = it }
            )
            // リセットボタン
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(2.dp),
                onClick = {
                    // 初期値に戻す
                    selectSize.value = "medium"
                    selectPos.value = "naka"
                    selectColor.value = "white"
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backspace,
                    tint = Color.White
                )
            }
        }
        // 匿名切り替えスイッチ
        Row(
            modifier = Modifier
                .padding(5.dp)
                .clickable(onClick = { onTokumeiChange(!is184) }, indication = null),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.iyayo_comment), modifier = Modifier.weight(1f),
                color = Color.White
            )
            Switch(checked = is184, onCheckedChange = { onTokumeiChange(!is184) })
        }
    }
}