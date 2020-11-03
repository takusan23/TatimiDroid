package io.github.takusan23.tatimidroid.Tool

import androidx.constraintlayout.motion.widget.MotionLayout

/**
 * MotionLayout関係で複数の個所で使いそうな関数たち
 * */
object MotionLayoutTool {

    /**
     * MotionLayoutの遷移をすべて 有効/無効 にする関数
     * @param motionLayout MotionLayout
     * @param isEnable 有効にするならtrue
     * */
    fun allTransitionEnable(motionLayout: MotionLayout, isEnable: Boolean) {
        motionLayout.definedTransitions.forEach { transition ->
            transition.setEnable(isEnable)
        }
    }



}