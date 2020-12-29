package io.github.takusan23.tatimidroid.Tool

import android.content.Context
import android.graphics.Insets
import android.graphics.Point
import android.os.Build
import android.util.Size
import android.view.WindowInsets
import android.view.WindowManager
import androidx.annotation.RequiresApi

/**
 * 画面の大きさを取得する関数。
 * Android 11から新しい方法で取得する用になって長くなるので関数にまとめた
 * */
object DisplaySizeTool {

    /**
     * 画面の幅を返す関数。
     * Android 11から取得方法が変わってしまった。
     * @param context こんてきすと
     * @return 画面の幅。Width
     * */
    fun getDisplayWidth(context: Context?): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getDisplaySizeNewAPI(context).width
        } else {
            getDisplaySizeOldAPI(context).x
        }
    }

    /**
     * 画面の高さを返す関数。
     * Android 11から取得方法が変わってしまった。
     * @param context こんてきすと
     * @return 画面の高さ。Height
     * */
    fun getDisplayHeight(context: Context?): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getDisplaySizeNewAPI(context).height
        } else {
            getDisplaySizeOldAPI(context).y
        }
    }

    /**
     * 従来の方法で画面の大きさ取得するやつ
     * */
    fun getDisplaySizeOldAPI(context: Context?): Point {
        // まどまねーじゃー
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // 従来の方法で
        val display = windowManager.defaultDisplay
        val point = Point()
        display?.getSize(point)
        return point
    }

    /**
     * Android 11からこの方法で画面の大きさを取得するらしい。Display#getPoint()の代替。
     * パクリ元：https://developer.android.com/reference/android/view/WindowMetrics#getBounds()
     * */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getDisplaySizeNewAPI(context: Context?): Size {
        // まどまねーじゃー
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = windowManager.currentWindowMetrics
        val windowInsets = metrics.windowInsets
        // なにしてるかわからん
        var insets = windowInsets.getInsets(WindowInsets.Type.systemBars())
        windowInsets.displayCutout?.run {
            insets = Insets.max(insets, Insets.of(safeInsetLeft, safeInsetTop, safeInsetRight, safeInsetBottom))
        }
        val insetsWidth = insets.right + insets.left
        val insetsHeight = insets.top + insets.bottom
        // Display#getHeight()と同じようになる
        return Size(metrics.bounds.width() - insetsWidth, metrics.bounds.height() - insetsHeight)
    }

}