package io.github.takusan23.tatimidroid.tool

import java.text.SimpleDateFormat
import java.util.*

/**
 * 時間をHH:mm:ssに変換する
 * */
object TimeFormatTool {

    /**
     * 変換する関数
     * @param position 時間（秒）
     * @return HH:mm:ss
     * */
    fun liveTimeFormat(position: Long): String {
        // 経過時間 - 番組開始時間
        val date = Date(position * 1000L)
        //時間はUNIX時間から計算する
        val hour = (position / 60 / 60)
        val simpleDateFormat = SimpleDateFormat("mm:ss")
        return "$hour:${simpleDateFormat.format(date.time)}"
    }

}