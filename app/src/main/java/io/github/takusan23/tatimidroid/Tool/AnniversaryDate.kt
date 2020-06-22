package io.github.takusan23.tatimidroid.Tool

import java.util.*

/**
 * 一周年とかを計算するやつ。一周年とかじゃないなら「-1」を返す
 * @param postedUnixTime 比較する時間。UnixTime（ミリ秒）
 * */
internal fun calcAnniversary(postedUnixTime: Long): Int {
    // 投稿日
    val postedCalendar = Calendar.getInstance()
    postedCalendar.timeInMillis = postedUnixTime
    // 本日
    val todayCalendar = Calendar.getInstance()
    if (postedCalendar[Calendar.MONTH] == todayCalendar[Calendar.MONTH] && postedCalendar[Calendar.DAY_OF_MONTH] == todayCalendar[Calendar.DAY_OF_MONTH]) {
        // 同じ日付。今年から引き算
        return todayCalendar[Calendar.YEAR] - postedCalendar[Calendar.YEAR]
    } else {
        // 違うので -1 返す
        return -1
    }
}

class AnniversaryDate {

    companion object {
        /**
         * お祝いテンプレ
         * @param anniversary 一周年なら1など。calcAnniversary()の返り値入れれば良くない？
         * */
        fun makeAnniversaryMessage(anniversary: Int) = "\uD83E\uDD73  本日は${anniversary}周年目です  \uD83C\uDF89"
    }

}