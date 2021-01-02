package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.R

/**
 * メニューのタブの部分だけ
 *
 * マイリスト登録とかメニューとか？
 *
 * @param selectedIndex 選択中の位置。
 * @param tabClick Tabを押した時。引数はタブの位置
 * */
@Composable
fun NicoVideoMenuTab(
    selectedIndex: Int,
    tabClick: (Int) -> Unit,
) {
    ScrollableTabRow(
        modifier = Modifier.padding(10.dp),
        selectedTabIndex = selectedIndex,
        backgroundColor = Color.Transparent,
    ) {
        TabPadding(
            index = 0,
            tabName = stringResource(id = R.string.mylist),
            tabIcon = Icons.Outlined.Folder,
            selectedIndex = selectedIndex,
            tabClick = { tabClick(0) }
        )
        TabPadding(
            index = 1,
            tabName = stringResource(id = R.string.nicovideo_menu_comment_hide),
            tabIcon = Icons.Outlined.Comment,
            selectedIndex = selectedIndex,
            tabClick = { tabClick(1) }
        )
        TabPadding(
            index = 2,
            tabName = stringResource(id = R.string.menu),
            tabIcon = Icons.Outlined.Menu,
            selectedIndex = selectedIndex,
            tabClick = { tabClick(2) }
        )
        TabPadding(
            index = 3,
            tabName = stringResource(id = R.string.share),
            tabIcon = Icons.Outlined.Share,
            selectedIndex = selectedIndex,
            tabClick = { tabClick(3) }
        )
        TabPadding(
            index = 4,
            tabName = stringResource(id = R.string.cache),
            tabIcon = Icons.Outlined.Folder,
            selectedIndex = selectedIndex,
            tabClick = { tabClick(4) }
        )
        TabPadding(
            index = 5,
            tabName = stringResource(id = R.string.volume),
            tabIcon = Icons.Outlined.VolumeUp,
            selectedIndex = selectedIndex,
            tabClick = { tabClick(5) }
        )
    }
}

/**
 * タブレイアウトの一つ一つのTab。
 * @param index 何個目のTabかどうか
 * @param tabIcon アイコン
 * @param tabName 名前
 * @param selectedIndex 現在選択中のタブの位置
 * @param tabClick タブを押した時
 * */
@Composable
fun TabPadding(index:Int, tabName: String, tabIcon: ImageVector, selectedIndex: Int, tabClick: (Int) -> Unit) {
    Tab(
        modifier = Modifier.padding(5.dp),
        selected = selectedIndex == index,
        onClick = {
            tabClick(index)
        }
    ) {
        Icon(imageVector = tabIcon)
        Text(text = tabName)
    }
}

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
        // 3ds排除
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
        // かんたんコメント排除
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
 * @param qualityChange 画質変更ボタン押した時
 * @param screenRotation 画面回転ボタン押した時
 * @param openBrowser ブラウザで開くボタンを押した時
 * @param ngList NG一覧ボタンを押した時
 * @param kotehanList コテハン一覧ボタンを押した時
 * @param skipSetting スキップ秒数変更ボタン押した時
 * */
@Composable
fun NicoVideoOtherButtonMenu(
    qualityChange: () -> Unit,
    copyVideoId: () -> Unit,
    screenRotation: () -> Unit,
    openBrowser: () -> Unit,
    ngList: () -> Unit,
    kotehanList: () -> Unit,
    skipSetting: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        // 画質変更
        TextButton(
            onClick = { qualityChange() },
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
        // 動画IDコピー
        TextButton(
            onClick = { copyVideoId() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(imageVector = Icons.Outlined.ContentCopy)
                Text(
                    text = stringResource(id = R.string.video_id_copy),
                    modifier = Modifier.weight(1f).padding(5.dp),
                )
            }
        } // 画面回転
        TextButton(
            onClick = { screenRotation() },
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
            onClick = { openBrowser() },
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
            onClick = { ngList() },
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
            onClick = { kotehanList() },
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
            onClick = { skipSetting() },
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
 * @param cacheGetEco キャッシュ取得ボタン（エコノミー）押した時
 * @param isCachePlay キャッシュ再生の場合はtrueにすることで動画情報を更新するボタンを表示します。ですがtrueの場合は前述のボタンを表示しません。
 * @param cacheUpdate キャッシュの動画情報更新ボタンを押した時
 * */
@Composable
fun NicoVideoCacheMenu(
    cacheGet: () -> Unit,
    cacheGetEco: () -> Unit,
    isCachePlay: Boolean,
    cacheUpdate: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        // キャッシュ再生 か それ以外
        if (isCachePlay) {
            // 情報更新ボタン表示
            TextButton(onClick = { cacheUpdate() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(imageVector = Icons.Outlined.Refresh)
                    Text(
                        text = stringResource(id = R.string.get_cache_re_get),
                        modifier = Modifier.weight(1f).padding(5.dp),
                    )
                }
            }
        } else {
            // 取得ボタン
            TextButton(onClick = { cacheGet() }) {
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
            // 取得ボタン（エコノミー）
            TextButton(onClick = { cacheGetEco() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(imageVector = Icons.Outlined.Folder)
                    Text(
                        text = stringResource(id = R.string.get_cache_eco),
                        modifier = Modifier.weight(1f).padding(5.dp),
                    )
                }
            }
        }
    }
}

/**
 * 共有
 *
 * @param share 共有ボタン押した時
 * @param shareAttachImg 画像つき共有ボタン押した時
 * */
@Composable
fun NicoVideoShareMenu(
    share: () -> Unit,
    shareAttachImg: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(10.dp).fillMaxWidth(),
    ) {
        // 共有
        Row {
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { share() },
            ) {
                Icon(imageVector = Icons.Outlined.Share)
                Text(
                    text = stringResource(id = R.string.share),
                    modifier = Modifier.padding(5.dp),
                )
            }
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { shareAttachImg() },
            ) {
                Icon(imageVector = Icons.Outlined.Share)
                Text(
                    text = stringResource(id = R.string.share_attach_image),
                    modifier = Modifier.padding(5.dp),
                )
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
        // 音量調整スライダー
        Slider(
            value = volume,
            onValueChange = {
                volumeChange(it)
            }
        )
    }
}