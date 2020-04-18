package io.github.takusan23.tatimidroid

import java.util.regex.Pattern

/**
 * 正規表現リスト。他でも使うときに
 * */
internal val NICOVIDEO_ID_REGEX = "(sm|so)([0-9]+)"
internal val NICOCOMMUNITY_ID_REGEX = "(co|ch)([0-9]+)"
internal val NICOLIVE_ID_REGEX = "(lv)([0-9]+)"
internal val NICOVIDEO_MYLIST_ID_REGEX = "(mylist/)([0-9]+)"

/**
 * 文字列の中からニコニコのID(sm/so/lv/co/ch)を見つける。
 * @param text 文字列
 * @return ID。なければnullです。
 * */
internal fun IDRegex(text: String): String? {
    // 正規表現
    val nicoIDMatcher = Pattern.compile(NICOLIVE_ID_REGEX)
        .matcher(text)
    val communityIDMatcher = Pattern.compile(NICOCOMMUNITY_ID_REGEX)
        .matcher(text)
    val nicoVideoIdMatcher = Pattern.compile(NICOVIDEO_ID_REGEX)
        .matcher(text)
    return when {
        nicoIDMatcher.find() -> nicoIDMatcher.group()
        communityIDMatcher.find() -> communityIDMatcher.group()
        nicoVideoIdMatcher.find() -> nicoVideoIdMatcher.group()
        else -> null
    }
}


