package io.github.takusan23.tatimidroid.NicoLive.JetpackCompose

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
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
import io.github.takusan23.tatimidroid.R


/**
 * 生放送のコメント表示用、投稿UIをComposeで作成する
 * @param click ボタンを押した時
 * @param isComment コメントのアイコンを表示する場合はtrue
 * @param comment コメント本文
 * @param commentChange コメントInputに変更が入ったときに呼ばれる
 * @param postClick 投稿ボタン押した時
 * @param changeEnterToSend Enterキーを送信キーに変更するか。
 * @param isHideCommentInputLayout コメント入力テキストボックス非表示にするかどうか
 * @param hideCommentInputLayoutChange コメント入力テキストボックスの表示が切り替わったら呼ばれる
 * */
@ExperimentalAnimationApi
@Composable
fun NicoLiveCommentInputButton(
    click: () -> Unit,
    isComment: Boolean,
    comment: String,
    commentChange: (String) -> Unit,
    postClick: () -> Unit,
    changeEnterToSend: Boolean = true,
    isHideCommentInputLayout: Boolean = false,
    hideCommentInputLayoutChange: (Boolean) -> Unit = {},
) {
    // コメント入力テキストボックスを格納するかどうか
    val isHideCommentLayout = remember { mutableStateOf(isHideCommentInputLayout) }
    // コメント入力テキストボックスの表示、非表示変更時に呼ばれるやつ
    hideCommentInputLayoutChange(isHideCommentLayout.value)

    // margin代わり
    Row(
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
        Row(
            modifier = Modifier.padding(5.dp),
            verticalAlignment = Alignment.CenterVertically, // 真ん中にする
        ) {
            // コメント投稿エリア収納
            IconButton(onClick = {
                isHideCommentLayout.value = !isHideCommentLayout.value
            }) {
                Icon(
                    imageVector = if (isHideCommentLayout.value) Icons.Outlined.Create else Icons.Outlined.KeyboardArrowRight,
                    tint = Color.White,
                )
            }
            // コメント入力展開するか
            if (!isHideCommentLayout.value) {
                // コマンドパネル
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Outlined.FormatPaint,
                        tint = Color.White,
                    )
                }
                // コメント入力
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = comment,
                    label = { Text(text = stringResource(id = R.string.comment)) },
                    onValueChange = { commentChange(it) },
                    activeColor = Color.White,
                    inactiveColor = Color.White,
                    textStyle = TextStyle(Color.White),
                    keyboardOptions = if (changeEnterToSend) KeyboardOptions(imeAction = ImeAction.Send) else KeyboardOptions.Default,
                    onImeActionPerformed = { imeAction, softwareKeyboardController ->
                        if (imeAction == ImeAction.Send) {
                            // 送信！
                            postClick()
                        }
                    }
                )
                // 投稿ボタン
                IconButton(onClick = { postClick() }) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        tint = Color.White,
                    )
                }
            }
            // ボタン
            IconButton(onClick = { click() }) {
                Icon(
                    imageVector = if (isComment) Icons.Outlined.Comment else Icons.Outlined.Info,
                    tint = Color.White,
                )
            }
        }
    }

}