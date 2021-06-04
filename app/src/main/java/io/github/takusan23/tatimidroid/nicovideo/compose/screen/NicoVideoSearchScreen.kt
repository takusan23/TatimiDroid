package io.github.takusan23.tatimidroid.nicovideo.compose.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.isFocused
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.nicoapi.nicovideo.dataclass.NicoVideoData
import io.github.takusan23.tatimidroid.nicovideo.compose.NicoVideoList
import io.github.takusan23.tatimidroid.nicovideo.viewmodel.NicoVideoSearchViewModel

/**
 * ニコ動検索画面。Composeでできている
 * */
@Composable
fun NicoVideoSearchScreen(
    viewModel: NicoVideoSearchViewModel,
    onVideoClick: (NicoVideoData) -> Unit,
    onMenuClick: (NicoVideoData) -> Unit
) {

    // 読み込み中かどうか
    val isLoading = viewModel.isLoadingLiveData.observeAsState(initial = false)
    // 動画一覧
    val searchResultVideoList = viewModel.searchResultNicoVideoDataListLiveData.observeAsState()
    // 関連タグ一覧
    val recommendTagList = viewModel.searchResultTagListLiveData.observeAsState()
    // サジェスト一覧
    val suggestList = viewModel.suggestListLiveData.observeAsState(initial = listOf())
    // 検索ワード
    val searchWord = remember { mutableStateOf("") }
    // テキストボックスにフォーカスがあたっている場合はtrue
    val isInputting = remember { mutableStateOf(false) }
    // タグ検索かどうか
    val isTagSearch = remember { mutableStateOf(true) }

    /**
     * 検索する関数
     * @param page ページ数。1から
     * */
    fun search(page: Int) {
        viewModel.search(searchWord.value, page, isTagSearch.value)
    }

    Column {
        NicoVideoSearchInput(
            searchWord = searchWord.value,
            isTagSearch = isTagSearch.value,
            onSearchWordChange = {
                searchWord.value = it
                viewModel.getSuggest(it)
            },
            onNGUploaderClick = {},
            onHistoryClick = {},
            onSearch = { search(1) },
            onFocusChange = { isFocus -> isInputting.value = isFocus },
            onSearchTypeClick = {
                isTagSearch.value = it
                search(1)
            }
        )
        if (isInputting.value) {
            // サジェストはキーボード操作中のみ
            NicoVideoSearchSuggestList(
                list = suggestList.value,
                onClick = { word ->
                    searchWord.value = word
                    isInputting.value = false
                    search(1)
                }
            )
        } else {
            // 関連タグ
            if (recommendTagList.value != null) {
                NicoVideoSearchRecommendTag(tagList = recommendTagList.value!!)
            }
            // 動画一覧
            if (searchResultVideoList.value != null) {
                NicoVideoList(
                    list = searchResultVideoList.value!!,
                    onVideoClick = { onVideoClick(it) },
                    onMenuClick = { onMenuClick(it) },
                    onLastScroll = { search(viewModel.currentSearchPage + 1) }
                )
            }
        }
    }

}

/**
 * NG投稿者、検索ボックス、検索履歴、検索ボタンがある部品
 *
 * @param onSearch 検索ボタンを押したか、キーボードのEnterを押したときに呼ばれる
 * @param onHistoryClick 検索履歴を押したとき
 * @param onNGUploaderClick NG投稿者を押したとき
 * @param onSearchWordChange テキストボックスのテキストが変更されたとき
 * @param searchWord 検索ワード
 * @param isFocus フォーカスを当てたい場合はtrue
 * @param onFocusChange フォーカスの変更があったら呼ばれる。
 * @param isTagSearch タグ検索時はtrue、キーワード検索時はfalse
 * @param onSearchTypeClick タグ検索に変更されたらtrue。キーワード検索に切り替わったらfalse
 * */
@Composable
private fun NicoVideoSearchInput(
    searchWord: String,
    isTagSearch: Boolean,
    onSearchWordChange: (String) -> Unit,
    onNGUploaderClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSearchTypeClick: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Column {
        Row {
            Box(modifier = Modifier.weight(1f)) {
                // 入力欄
                val focusRequester = remember { FocusRequester() }
                BasicTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .padding(10.dp)
                        .onFocusChanged { state -> onFocusChange(state.isFocused) },
                    value = searchWord,
                    onValueChange = { onSearchWordChange(it) },
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    singleLine = true,
                )
                if (searchWord.isEmpty()) {
                    // 検索ワード無いときは「検索」のテキストを表示させる
                    Text(
                        text = stringResource(id = R.string.serch),
                        modifier = Modifier.padding(10.dp),
                        color = LocalTextStyle.current.color.copy(alpha = 0.5f)
                    )
                }
            }
            IconButton(onClick = { onSearch() }) {
                Icon(painter = painterResource(id = R.drawable.ic_24px), contentDescription = "検索")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // タグ、キーワード検索切り替え
            NicoVideoSearchTagOrKeywordToggleSwitch(
                isTagSearch = isTagSearch,
                onSearchClick = { onSearchTypeClick(it) }
            )
            IconButton(onClick = { onNGUploaderClick() }) {
                Icon(painter = painterResource(id = R.drawable.ng_uploader_icon), contentDescription = "NG投稿者")
            }
            IconButton(onClick = { onHistoryClick() }) {
                Icon(painter = painterResource(id = R.drawable.ic_history_24px), contentDescription = "履歴")
            }
        }
        Divider(Modifier.padding(start = 5.dp, end = 5.dp))
    }
}

/**
 * サジェスト一覧
 *
 * @param list 関連する検索ワード
 * @param onClick 検索ワードを押したら呼ばれる
 * */
@Composable
private fun NicoVideoSearchSuggestList(
    list: List<String>,
    onClick: (String) -> Unit
) {
    // サジェスト
    LazyColumn {
        items(list) { word ->
            Row(
                modifier = Modifier.clickable { onClick(word) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = word,
                    modifier = Modifier
                        .padding(10.dp)
                        .weight(1f)
                )
                Icon(painter = painterResource(id = R.drawable.ic_24px), contentDescription = "検索")
            }
            Divider(Modifier.padding(start = 10.dp, end = 10.dp))
        }
    }
}

/**
 * タグ検索、キーワード検索切り替えトグルスイッチ
 *
 * @param isTagSerch タグ検索ならtrue。キーワード検索ならfalse
 * @param onSearchClick タグ検索押したらtrue。キーワード検索押したらfalse
 * */
@Composable
private fun NicoVideoSearchTagOrKeywordToggleSwitch(
    isTagSearch: Boolean,
    onSearchClick: (Boolean) -> Unit
) {
    Surface(
        border = ButtonDefaults.outlinedBorder,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .padding(2.dp)
            .wrapContentWidth(align = Alignment.Start)
            .wrapContentHeight(align = Alignment.Top)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.requiredHeight(IntrinsicSize.Min)
        ) {
            IconButton(onClick = { onSearchClick(false) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_font_download_24px),
                    contentDescription = "tag",
                    tint = if (!isTagSearch) MaterialTheme.colors.primary else LocalContentColor.current,
                )
            }
            // 区切り線
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .requiredWidth(1.dp)
                    .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
            )
            IconButton(onClick = { onSearchClick(true) }) {
                Icon(
                    modifier = Modifier.padding(10.dp),
                    painter = painterResource(id = R.drawable.ic_local_offer_24px),
                    contentDescription = "tag",
                    tint = if (isTagSearch) MaterialTheme.colors.primary else LocalContentColor.current,
                )
            }
        }
    }
}

/**
 * ニコ動検索の関連タグを表示する
 *
 * @param tagList タグの配列
 * */
@Composable
private fun NicoVideoSearchRecommendTag(tagList: List<String>) {
    LazyRow {
        items(tagList) { tag ->
            OutlinedButton(onClick = { }, modifier = Modifier.padding(2.dp)) {
                Text(text = tag)
            }
        }
    }
}
