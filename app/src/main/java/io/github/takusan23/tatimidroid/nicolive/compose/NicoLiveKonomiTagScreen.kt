package io.github.takusan23.tatimidroid.nicolive.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.compose.OrigamiLayout
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveKonomiTagData
import io.github.takusan23.tatimidroid.nicoapi.nicolive.dataclass.NicoLiveProgramData
import io.github.takusan23.tatimidroid.nicolive.viewmodel.NicoLiveKonomiTagViewModel

/**
 * 好みタグで番組を検索する画面
 *
 * @param onClickProgram 番組押したときに呼ばれる。引数は番組のデータクラス
 * @param onClickMenu メニュー押したときに呼ばれる
 * */
@Composable
fun NicoLiveKonomiTagScreen(
    onClickProgram: (NicoLiveProgramData) -> Unit,
    onClickMenu: (NicoLiveProgramData) -> Unit
) {
    // ViewModel
    val viewModel = viewModel<NicoLiveKonomiTagViewModel>()
    // フォロー中好みタグ一覧
    val followingKonomiTagList = viewModel.followingKonomiTagListLiveData.observeAsState()
    // 好みタグで検索した結果
    val konomiTagProgramList = viewModel.konomiTagProgramListLiveData.observeAsState()
    // 好みタグを展開するか
    val isExpended = remember { mutableStateOf(false) }

    Column(Modifier.padding(5.dp)) {
        // 好みタグ表示
        if (followingKonomiTagList.value != null) {
            NicoLiveKonomiTagList(
                konomiTagList = followingKonomiTagList.value!!,
                isExpended = isExpended.value,
                onClickExpended = { isExpended.value = !isExpended.value },
                onClickKonomiTagEdit = { },
                onClickKonomiTag = { nicoLiveKonomiTagData -> viewModel.searchProgramFromKonomiTag(nicoLiveKonomiTagData.tagId) }
            )
        }
        // 好みタグが登録されている番組
        if (konomiTagProgramList.value != null) {
            NicoLiveProgramList(
                list = konomiTagProgramList.value!!,
                onClickProgram = { nicoLiveProgramData -> onClickProgram(nicoLiveProgramData) },
                onClickMenu = { nicoLiveProgramData -> onClickMenu(nicoLiveProgramData) }
            )
        }
    }

}

/**
 * 好みタグ一覧を表示する部品
 *
 * @param isExpended すべて表示する場合はtrue
 * @param konomiTagList 好みタグの配列
 * @param onClickKonomiTag 好みタグを押したときに呼ばれる
 * @param onClickKonomiTagEdit 好みタグ編集ボタンを押したら呼ばれる
 * */
@Composable
fun NicoLiveKonomiTagList(
    konomiTagList: List<NicoLiveKonomiTagData>,
    isExpended: Boolean = false,
    onClickExpended: () -> Unit,
    onClickKonomiTagEdit: () -> Unit,
    onClickKonomiTag: (NicoLiveKonomiTagData) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = painterResource(id = R.drawable.ic_outline_favorite_border_24), contentDescription = null)
            Text(text = stringResource(id = R.string.nicolive_konomi_tag_following), modifier = Modifier.weight(1f))
            // 展開ボタン
            IconButton(onClick = { onClickExpended() }) {
                Icon(painter = if (isExpended) painterResource(id = R.drawable.ic_expand_more_24px) else painterResource(id = R.drawable.ic_expand_less_black_24dp), contentDescription = "表示")
            }
            // 編集ボタン
            OutlinedButton(onClick = { onClickKonomiTagEdit() }) {
                Icon(painter = painterResource(id = R.drawable.ic_outline_create_24px), contentDescription = null)
                Text(text = stringResource(id = R.string.nicolive_konomi_tag_edit))
            }
        }
        OrigamiLayout(
            isExpended = isExpended,
            modifier = Modifier.verticalScroll(rememberScrollState()),
            minHeight = 200
        ) {
            konomiTagList.forEach { konomiTagData ->
                OutlinedButton(
                    onClick = { onClickKonomiTag(konomiTagData) },
                    modifier = Modifier.padding(5.dp)
                ) {
                    Text(text = konomiTagData.name)
                }
            }
        }
    }
}