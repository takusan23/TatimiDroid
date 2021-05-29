package io.github.takusan23.tatimidroid.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R

/**
 * MainActivityで使うID入力用TextField
 * */
@Composable
fun MainActivityIDInput(
    idText: String = "",
    onClickPlayButton: () -> Unit,
    onClickHistoryButton: () -> Unit,
) {
    val id = remember { mutableStateOf(idText) }

    Card(
        modifier = Modifier.padding(10.dp),
        elevation = 10.dp,
        shape = RoundedCornerShape(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                modifier = Modifier.weight(1f),
                value = id.value,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onClickPlayButton() }),
                onValueChange = { text -> id.value = text },
                placeholder = { Text(text = stringResource(id = R.string.liveid_or_communityid)) },
                maxLines = 1,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
            IconButton(onClick = { onClickHistoryButton() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_history_24px),
                    contentDescription = "履歴"
                )
            }
            IconButton(onClick = { onClickPlayButton() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_arrow_24px),
                    contentDescription = "開く"
                )
            }
        }
    }
}