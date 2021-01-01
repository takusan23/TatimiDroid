package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R

/**
 * 3DS、かんたんこめんと排除スイッチをおいてる。
 *
 * @param is3DSHide 3DSを排除するか
 * @param isKandanCommentHide かんたんコメントを排除するか
 * @param dsSwitchChange 3DS排除スイッチを切り替えたときに呼ばれる
 * @param kantanCommentSwitchChange かんたんコメント排除スイッチを切り替えたときに呼ばれる
 * */
@Composable
fun NicoVideoCommentHideMenu(
    is3DSHide: Boolean,
    isKandanCommentHide: Boolean,
    dsSwitchChange: (Boolean) -> Unit,
    kantanCommentSwitchChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(5.dp)
        ) {
            Icon(imageVector = Icons.Outlined.Comment)
            Text(text = "コメント非表示設定")
        }
        Row(
            modifier = Modifier.padding(5.dp),
        ) {
            Text(
                text = stringResource(id = R.string.nicovideo_setting_hide_device_3ds),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = is3DSHide,
                onCheckedChange = {
                    dsSwitchChange(it)
                },
            )
        }
        Row(
            modifier = Modifier.padding(5.dp),
        ) {
            Text(
                text = stringResource(id = R.string.nicovideo_setting_hide_kantan_comment),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isKandanCommentHide,
                onCheckedChange = {
                    kantanCommentSwitchChange(it)
                },
            )
        }
    }
}

/**
 * マイリスト追加ボタン
 *
 * @param addMylist マイリスト追加ボタンを押した時
 * @param addAtodemiru あとでみる追加ボタンを押した時
 * */
@Composable
fun NicoVideoMylistsMenu(addMylist: () -> Unit, addAtodemiru: () -> Unit) {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Folder)
            Text(text = stringResource(id = R.string.mylist))
        }
        // マイリスト追加
        TextButton(
            onClick = { addMylist() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Folder)
                Text(
                    text = stringResource(id = R.string.add_mylist),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
        // あとでみる（旧：とりあえずマイリスト）
        TextButton(
            onClick = { addAtodemiru() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Folder)
                Text(
                    text = stringResource(id = R.string.add_atodemiru),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
    }
}

/**
 * その他のメニュー。画質変更とかスキップ秒数変更とか画面回転とか
 *
 * @param qualityChane 画質変更ボタン押した時
 * @param screenRotation 画面回転ボタン押した時
 * @param openBrowser ブラウザで開くボタンを押した時
 * @param ngList NG一覧ボタンを押した時
 * @param kotehanList コテハン一覧ボタンを押した時
 * @param skipSetting スキップ秒数変更ボタン押した時
 * */
@Composable
fun NicoVideoOtherButtonMenu(
    qualityChane: () -> Unit,
    screenRotation: () -> Unit,
    openBrowser: () -> Unit,
    ngList: () -> Unit,
    kotehanList: () -> Unit,
    skipSetting: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Menu)
            Text(text = stringResource(id = R.string.menu))
        }
        // 画質変更
        TextButton(
            onClick = { },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.PhotoFilter)
                Text(
                    text = stringResource(id = R.string.quality),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
        // 画面回転
        TextButton(
            onClick = { },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.ScreenRotation)
                Text(
                    text = stringResource(id = R.string.landscape_portrait),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
        // ブラウザで開く
        TextButton(
            onClick = { },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.OpenInBrowser)
                Text(
                    text = stringResource(id = R.string.open_browser),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
        // NG一覧
        TextButton(
            onClick = { },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Block)
                Text(
                    text = stringResource(id = R.string.ng_list),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
        // コテハン
        TextButton(
            onClick = { },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.AccountBox)
                Text(
                    text = stringResource(id = R.string.kotehan_list),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
        // スキップ秒数
        TextButton(
            onClick = { },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Redo)
                Text(
                    text = stringResource(id = R.string.skip_setting),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
    }
}

/**
 * キャッシュ取得ボタン
 *
 * @param cacheGet キャッシュ取得ボタン押した時
 * */
@Composable
fun NicoVideoCacheMenu(
    cacheGet: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Folder)
            Text(text = stringResource(id = R.string.cache))
        }
        // 取得ボタン
        TextButton(
            onClick = { cacheGet() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Folder)
                Text(
                    text = stringResource(id = R.string.get_cache),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        }
    }
}

/** 共有 */
@Composable
fun NicoVideoShareMenu() {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Share)
            Text(text = stringResource(id = R.string.share))
        }
        Row {
            TextButton(onClick = { }) {
                Text(text = stringResource(id = R.string.share))
            }
            TextButton(onClick = { }) {
                Text(text = stringResource(id = R.string.share_attach_image))
            }
        }
    }
}

/**
 * 音量
 *
 * @param volume 音量の値。1f
 * @param volumeChange シークバーいじったら呼ばれる
 * */
@Composable
fun NicoVideoVolumeMenu(
    volume: Float,
    volumeChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(5.dp),
        ) {
            Icon(imageVector = Icons.Outlined.VolumeUp)
            Text(text = stringResource(id = R.string.volume))
        }
        Slider(
            value = volume,
            onValueChange = {
                volumeChange(it)
            }
        )
    }
}