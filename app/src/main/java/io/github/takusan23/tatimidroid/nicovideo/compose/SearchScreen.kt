package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchViewModel_

/**
 * 検索画面のUI。Composeで作成する
 *
 * @param searchViewModel ViewModel
 * @param onSearch 検索するときに呼ばれる関数
 * */
@Composable
fun SearchScreen(searchViewModel: NicoVideoSearchViewModel_, onSearch: (searchText: String, isTagSearch: Boolean, sort: String) -> Unit) {

    // 検索ボックス
    val searchText = remember { mutableStateOf("") }
    // タグ検索ならtrue。キーワードならfalse
    val isTagSearch = remember { mutableStateOf(true) }
    // 検索履歴
    val searchHistoryList = searchViewModel.searchHistoryAllListLiveData.observeAsState()

    MaterialTheme {
        Scaffold {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(5.dp)
                ) {
                    // タグ、キーワード切り替えボタン
                    SearchTagOrKeyword(
                        isTag = isTagSearch.value,
                        onChange = { isTagSearch.value = it }
                    )
                    // テキストボックス
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = searchText.value,
                        label = { Text(text = "検索ワード") },
                        onValueChange = { searchText.value = it },
                    )
                    // 検索
                    IconButton(onClick = {
                        // 検索する
                        onSearch(searchText.value, isTagSearch.value, "人気の高い順")
                    }) {
                        Icon(painter = painterResource(id = R.drawable.ic_24px), contentDescription = "検索")
                    }
                }
                Divider()
                // 検索履歴読み込めたら
                if (searchHistoryList.value != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(5.dp),
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_history_24px), contentDescription = "履歴")
                        Text(text = "検索履歴", fontSize = 20.sp)
                    }
                    SearchHistoryList(
                        list = searchHistoryList.value!!,
                        onClickSearchHistory = {
                            // 検索する
                            onSearch(it.text, it.isTagSearch, "人気の高い順")
                        }
                    )
                }
            }
        }
    }
}