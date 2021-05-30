package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.isDarkMode
import kotlinx.coroutines.launch

/**
 * Backdropに押したら展開機能を付け足したりしたやつ
 * @param backLayerContent 後ろ側に描画するUI
 * @param frontLayerContent 表側に描画するUI
 * @param openText ここを押して展開のテキスト
 * @param scaffoldState BackDropの状態
 * */
@ExperimentalMaterialApi
@Composable
fun SimpleBackdrop(
    openText: String = stringResource(id = R.string.dropdown_title),
    scaffoldState: BackdropScaffoldState = rememberBackdropScaffoldState(BackdropValue.Concealed),
    backLayerContent: @Composable () -> Unit,
    frontLayerContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /** Backdropを開く */
    fun openMenu() = scope.launch { scaffoldState.conceal() }

    /** Backdropを閉じる */
    fun closeMenu() = scope.launch { scaffoldState.reveal() }

    BackdropScaffold(
        scaffoldState = scaffoldState,
        appBar = { },
        backLayerBackgroundColor = if (isDarkMode(context)) Color.Black else Color.White,
        frontLayerElevation = 10.dp,
        backLayerContent = { backLayerContent() },
        frontLayerContent = {
            Column {
                // ここを押して展開ボタン
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { if (scaffoldState.isRevealed) openMenu() else closeMenu() }
                ) {
                    // 棒
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(20.dp)
                            .padding(bottom = 5.dp, top = 5.dp)
                            .background(color = MaterialTheme.colors.primary, shape = RoundedCornerShape(50))
                    )
                    Text(
                        text = openText,
                        textAlign = TextAlign.Center
                    )
                }
                frontLayerContent()
            }
        }
    )
}