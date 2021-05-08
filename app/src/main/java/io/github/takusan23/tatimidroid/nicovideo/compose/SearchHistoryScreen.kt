package io.github.takusan23.tatimidroid.nicovideo.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchHistoryViewModel

/**
 * 検索履歴の画面
 * */
@Composable
fun SearchHistoryScreen(viewModel: NicoVideoSearchHistoryViewModel, onSearch: (searchText: String, isTag: Boolean, sort: String) -> Unit) {
    val searchHistoryList = viewModel.searchHistoryAllListLiveData.observeAsState()

    MaterialTheme {
        Column {
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