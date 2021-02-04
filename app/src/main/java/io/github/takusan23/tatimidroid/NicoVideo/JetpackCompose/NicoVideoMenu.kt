package io.github.takusan23.tatimidroid.NicoVideo.JetpackCompose

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.takusan23.tatimidroid.JetpackCompose.TabPadding
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
 * 3DS、かんたんこめんと排除スイッチをおいてる。
 *
 * @param is3DSHide 3DSを排除するか
 * @param isKandanCommentHide かんたんコメントを排除するか
 * @param onDsSwitchChange 3DS排除スイッチを切り替えたときに呼ばれる
 * @param onKantanCommentSwitchChange かんたんコメント排除スイッチを切り替えたときに呼ばれる
 * */
@Composable
fun NicoVideoCommentHideMenu(
    is3DSHide: Boolean,
    isKandanCommentHide: Boolean,
    onDsSwitchChange: (Boolean) -> Unit,
    onKantanCommentSwitchChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.padding(10.dp)) {
        // 3ds排除
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.nicovideo_setting_hide_device_3ds),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = is3DSHide,
                onCheckedChange = {
                    onDsSwitchChange(it)
                },
            )
        }
        // かんたんコメント排除
        Row(modifier = Modifier.padding(5.dp)) {
            Text(
                text = stringResource(id = R.string.nicovideo_setting_hide_kantan_comment),
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = isKandanCommentHide,
                onCheckedChange = {
                    onKantanCommentSwitchChange(it)
                },
            )
        }
    }
}

/**
 * マイリスト追加ボタン
 *
 * @param onClickAddMylist マイリスト追加ボタンを押した時
 * @param onClickAddAtodemiru あとでみる追加ボタンを押した時
 * */
@Composable
fun NicoVideoMylistsMenu(onClickAddMylist: () -> Unit, onClickAddAtodemiru: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // マイリスト追加
        TextButton(
            onClick = { onClickAddMylist() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = stringResource(id = R.string.add_mylist)
                )
                Text(
                    text = stringResource(id = R.string.add_mylist),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // あとでみる（旧：とりあえずマイリスト）
        TextButton(
            onClick = { onClickAddAtodemiru() },
        )
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = stringResource(id = R.string.add_atodemiru)
                )
                Text(
                    text = stringResource(id = R.string.add_atodemiru),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
    }
}

/**
 * その他のメニュー。画質変更とかスキップ秒数変更とか画面回転とか
 *
 * @param onClickQualityChange 画質変更ボタン押した時
 * @param onClickScreenRotation 画面回転ボタン押した時
 * @param onClickOpenBrowser ブラウザで開くボタンを押した時
 * @param onClickNgList NG一覧ボタンを押した時
 * @param onClickKotehanList コテハン一覧ボタンを押した時
 * @param onClickSkipSetting スキップ秒数変更ボタン押した時
 * @param
 * */
@Composable
fun NicoVideoOtherButtonMenu(
    onClickQualityChange: () -> Unit,
    onClickCopyVideoId: () -> Unit,
    onClickScreenRotation: () -> Unit,
    onClickOpenBrowser: () -> Unit,
    onClickNgList: () -> Unit,
    onClickKotehanList: () -> Unit,
    onClickSkipSetting: () -> Unit,
    onClickShowNicoAd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // 画質変更
        TextButton(onClick = { onClickQualityChange() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoFilter,
                    contentDescription = stringResource(id = R.string.quality)
                )
                Text(
                    text = stringResource(id = R.string.quality),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // ニコニ広告
        TextButton(onClick = { onClickShowNicoAd() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Money,
                    contentDescription = stringResource(id = R.string.nicoads)
                )
                Text(
                    text = stringResource(id = R.string.nicoads),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // 画面回転
        TextButton(onClick = { onClickScreenRotation() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ScreenRotation,
                    contentDescription = stringResource(id = R.string.landscape_portrait)
                )
                Text(
                    text = stringResource(id = R.string.landscape_portrait),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // 動画IDコピー
        TextButton(onClick = { onClickCopyVideoId() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(id = R.string.video_id_copy)
                )
                Text(
                    text = stringResource(id = R.string.video_id_copy),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // ブラウザで開く
        TextButton(onClick = { onClickOpenBrowser() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInBrowser,
                    contentDescription = stringResource(id = R.string.open_browser)
                )
                Text(
                    text = stringResource(id = R.string.open_browser),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // NG一覧
        TextButton(onClick = { onClickNgList() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = stringResource(id = R.string.ng_list)
                )
                Text(
                    text = stringResource(id = R.string.ng_list),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // コテハン
        TextButton(onClick = { onClickKotehanList() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBox,
                    contentDescription = stringResource(id = R.string.kotehan)
                )
                Text(
                    text = stringResource(id = R.string.kotehan_list),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
        // スキップ秒数
        TextButton(onClick = { onClickSkipSetting() })
        {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Redo,
                    contentDescription = stringResource(id = R.string.skip_setting)
                )
                Text(
                    text = stringResource(id = R.string.skip_setting),
                    modifier = Modifier
                        .weight(1f)
                        .padding(5.dp),
                )
            }
        }
    }
}

/**
 * キャッシュ取得ボタン
 *
 * @param onClickCacheGet キャッシュ取得ボタン押した時
 * @param onClickCacheGetEco キャッシュ取得ボタン（エコノミー）押した時
 * @param isCachePlay キャッシュ再生の場合はtrueにすることで動画情報を更新するボタンを表示します。ですがtrueの場合は前述のボタンを表示しません。
 * @param onClickCacheUpdate キャッシュの動画情報更新ボタンを押した時
 * */
@Composable
fun NicoVideoCacheMenu(
    onClickCacheGet: () -> Unit,
    onClickCacheGetEco: () -> Unit,
    isCachePlay: Boolean,
    onClickCacheUpdate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // キャッシュ再生 か それ以外
        if (isCachePlay) {
            // 情報更新ボタン表示
            TextButton(onClick = { onClickCacheUpdate() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(id = R.string.get_cache_re_get)
                    )
                    Text(
                        text = stringResource(id = R.string.get_cache_re_get),
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                    )
                }
            }
        } else {
            // 取得ボタン
            TextButton(onClick = { onClickCacheGet() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = stringResource(id = R.string.get_cache)
                    )
                    Text(
                        text = stringResource(id = R.string.get_cache),
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                    )
                }
            }
            // 取得ボタン（エコノミー）
            TextButton(onClick = { onClickCacheGetEco() }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = stringResource(id = R.string.get_cache_eco)
                    )
                    Text(
                        text = stringResource(id = R.string.get_cache_eco),
                        modifier = Modifier
                            .weight(1f)
                            .padding(5.dp),
                    )
                }
            }
        }
    }
}

/**
 * 共有
 *
 * @param onClickShare 共有ボタン押した時
 * @param onClickShareAttachImg 画像つき共有ボタン押した時
 * */
@Composable
fun NicoVideoShareMenu(
    onClickShare: () -> Unit,
    onClickShareAttachImg: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
    ) {
        // 共有
        Row {
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { onClickShare() },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(id = R.string.share)
                )
                Text(
                    text = stringResource(id = R.string.share),
                    modifier = Modifier.padding(5.dp),
                )
            }
            TextButton(
                modifier = Modifier.weight(1f),
                onClick = { onClickShareAttachImg() },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(id = R.string.share_attach_image)
                )
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
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
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