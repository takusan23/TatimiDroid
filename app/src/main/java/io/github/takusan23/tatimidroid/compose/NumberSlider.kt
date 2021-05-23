package io.github.takusan23.tatimidroid.compose

import androidx.compose.material.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 整数の値を扱うのに適したSlider。整数と言いながらIntではなくLong
 *
 * なんかComposeのSliderはFloatなので動画アプリでは扱いにくい
 *
 * 最小値は0
 *
 * @param maxValue 最大値
 * @param modifier 大きさなど
 * @param onValueChange 値が変更されたら呼ばれる
 * @param currentValue 現在の値
 * */
@Composable
fun NumberSlider(
    modifier: Modifier = Modifier,
    maxValue: Long = 10L,
    currentValue: Long = 5L,
    onValueChange: (Long) -> Unit,
) {

    /** [currentValue]を0fから1fまでに変換する */
    fun longToProgress(currentValue: Long, maxValue: Long): Float {
        return (currentValue.toFloat() / maxValue)
    }

    /** [currentValue]を整数に戻す */
    fun progressToLong(currentFloat: Float, maxValue: Long): Long {
        return (currentFloat * maxValue).toLong()
    }

    Slider(
        modifier = modifier,
        value = longToProgress(currentValue, maxValue),
        onValueChange = { progress -> onValueChange(progressToLong(progress, maxValue)) }
    )
}