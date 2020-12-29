package io.github.takusan23.tatimidroid

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.github.takusan23.tatimidroid.Tool.DisplaySizeTool
import kotlin.math.roundToInt

/**
 * ミニプレイヤー powered by BottomSheetBehavior
 *
 * [BottomSheetBehavior]をカスタマイズしてミニプレイヤーを再実装する。MotionLayout使いにくい(Visibility変更とか)しMotionEditor重いので
 *
 * ちなみに[View.setTranslationX]で横方向にずらすことで実装してる
 *
 * 別の作戦
 * */
class BottomSheetPlayerBehavior<T : View>(val context: Context, attributeSet: AttributeSet) : BottomSheetBehavior<T>() {

    companion object {

        /**
         * これを使うにはこれを呼んでBottomSheetを作成し、[currentMiniPlayerHeight]、[currentMiniPlayerWidth]、[currentMiniPlayerXPos]を更新し続けてください。
         * */
        fun <V : View> from(view: V): BottomSheetPlayerBehavior<V> {
            val params = view.layoutParams
            require(params is CoordinatorLayout.LayoutParams) { "The view is not a child of CoordinatorLayout" }
            val behavior = params.behavior
            require(behavior is BottomSheetBehavior<*>) { "The view is not associated with BottomSheetBehavior" }
            return behavior as BottomSheetPlayerBehavior<V>
        }

    }

    /** ミニプレイヤー時の幅。大きさが変わったらその都度入れて */
    var currentMiniPlayerWidth = 0

    /** ミニプレイヤー時の高さ。大きさが変わったらその都度入れて */
    var currentMiniPlayerHeight = 0

    /**
     * ↓ここの大きさ。値が変わったらその都度更新して
     *
     * |<--->■|
     * */
    var currentMiniPlayerXPos = 0f

    /** 現在の進捗 */
    var progress = 0f

    /**
     * スワイプの処理等、めんどいのはこっちでやる
     *
     * サイズ変更等やります。
     * @param videoHeight 動画の高さ
     * @param bottomSheetView BottomSheetを設定したView
     * @param playerView プレイヤーのView
     * */
    fun init(videoHeight: Int, bottomSheetView: View, playerView: View) {

        // 最小値
        val videoWidth = (videoHeight / 9) * 16
        peekHeight = videoHeight

        // 画面の幅
        val displayWidth = DisplaySizeTool.getDisplayWidth(context)

        // ミニプレイヤーの幅を引いた値
        val maxTransitionX = (displayWidth - videoWidth).toFloat()

        bottomSheetView.translationX = maxTransitionX

        currentMiniPlayerHeight = videoHeight
        currentMiniPlayerWidth = videoWidth
        currentMiniPlayerXPos = maxTransitionX
        isHideable = true

        // プレイヤーの大きさ
        playerView.updateLayoutParams {
            height = videoHeight
            width = videoWidth
        }

        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    STATE_EXPANDED -> {
                        // 展開時のプレイヤーの大きさ
                        if (isLandScape()) {
                            playerView.updateLayoutParams {
                                width = displayWidth / 2
                                height = (width / 16) * 9
                            }
                        } else {
                            playerView.updateLayoutParams {
                                width = displayWidth
                                height = (width / 16) * 9
                            }
                        }
                    }
                    STATE_COLLAPSED -> {
                        // 格納時のプレイヤーの大きさ
                        playerView.updateLayoutParams {
                            height = videoHeight
                            width = videoWidth
                        }
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // 操作中
                val inverseOffset = 1.0f - slideOffset
                progress = slideOffset

                // 負の値なら操作しない
                if (slideOffset > 0) {
                    // X方向を操作することでLayoutParamsを操作せずに済む
                    bottomSheetView.translationX = maxTransitionX * inverseOffset

                    // プレイヤーのサイズも変更
                    if (isLandScape()) {
                        playerView.updateLayoutParams<LinearLayout.LayoutParams> {
                            width = videoWidth + (maxTransitionX * slideOffset).toInt() / 4 // なんか4で割るとうまくいく
                            height = (width / 16) * 9

                            // 横画面時はプレイヤーを真ん中にしたい。ので上方向のマージンを設定して真ん中にする
                            println("${bottomSheetView.height} ${DisplaySizeTool.getDisplayHeight(context)}  ${DisplaySizeTool.getDisplaySizeOldAPI(context).y}")
                            // とりあえず最大時にかけるマージン計算
                            val maxTopMargin = (DisplaySizeTool.getDisplaySizeOldAPI(context).y - playerView.height) / 2
                            // そして現在かけるべきマージンを計算
                            val currentTopMargin = maxTopMargin * slideOffset
                            topMargin = currentTopMargin.roundToInt()
                        }

                    } else {
                        playerView.updateLayoutParams {
                            width = videoWidth + (maxTransitionX * slideOffset).toInt()
                            height = (width / 16) * 9
                        }
                    }

                    // 値更新
                    currentMiniPlayerHeight = playerView.height
                    currentMiniPlayerWidth = playerView.width
                    currentMiniPlayerXPos = bottomSheetView.translationX

                }

            }
        })
    }

    private fun isLandScape() = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    override fun onTouchEvent(parent: CoordinatorLayout, child: T, event: MotionEvent): Boolean {
        // プレイヤーを触っているときのみタッチイベントを渡す。translateXの値変えてもタッチは何故か行くので制御
        // ちなみにchild.leftは0を返す
        val isTouchingSwipeTargetView = event.x > child.x
        // もしくは進行中なら操作を許可
        val isProgress = progress < 1f && progress > 0f

        return if (isTouchingSwipeTargetView || isProgress) {
            super.onTouchEvent(parent, child, event)
        } else {
            false
        }
    }


}