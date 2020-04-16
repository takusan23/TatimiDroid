package io.github.takusan23.tatimidroid

import java.util.regex.Pattern

/**
 * 文字列の中からニコニコのID(sm/so/lv/co/ch)を見つける。
 * @param text 文字列
 * @return ID。なければnullです。
 * */
internal fun IDRegex(text: String): String? {
    // 正規表現
    val nicoIDMatcher = Pattern.compile("(lv)([0-9]+)")
        .matcher(text)
    val communityIDMatcher = Pattern.compile("(co|ch)([0-9]+)")
        .matcher(text)
    val nicoVideoIdMatcher = Pattern.compile("(sm|so)([0-9]+)")
        .matcher(text)
    return when {
        nicoIDMatcher.find() -> nicoIDMatcher.group()
        communityIDMatcher.find() -> communityIDMatcher.group()
        nicoVideoIdMatcher.find() -> nicoVideoIdMatcher.group()
        else -> null
    }
}