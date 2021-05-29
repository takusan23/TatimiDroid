package io.github.takusan23.tatimidroid.tool

import android.text.format.DateUtils
import java.text.SimpleDateFormat

/**
 * 時間をHH:mm:ssに変換する
 * */
object TimeFormatTool {

    /**
     * 秒をMM:ssとかに変換する関数
     * @param position 時間（秒）
     * @return HH:mm:ss。0以下の場合は空文字
     * */
    fun timeFormat(position: Long): String {
        return if (position < 0) {
            ""
        } else {
            DateUtils.formatElapsedTime(position)
        }
    }

    /**
     * UnixTimeを「MM/dd HH:mm:ss EEE曜日」に変換する
     *
     * @param unixTime UnixTime
     * @return MM/dd HH:mm:ss EEE曜日
     * */
    fun unixTimeToFormatDate(unixTime: Long): String {
        return SimpleDateFormat("MM/dd HH:mm:ss EEE曜日").format(unixTime)
    }

}