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
 *
 * (visibility変更がだるいから作り変えてるけど、これもVisibility変える時一瞬おかしくなるな。FrameLayoutとかで囲っておいたViewは大丈夫っぽい)
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

    /** プレイヤーのサイズ変更（ドラッグ操作）をプレイヤー範囲に限定するかどうか */
    var isDraggableAreaPlayerOnly = false

    /** [isDraggableAreaPlayerOnly]のときに使う */
    private var draggablePlayerView: View? = null

    /** [isDraggableAreaPlayerOnly]のときに使う */
    private var draggableBottomSheetView: View? = null

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
     * 全画面モードかどうか
     *
     * [toFullScreen]とかで自動で設定される。
     * */
    private var isFullScreenMode = false


    /**
     * スワイプの処理等、めんどいのはこっちでやる
     *
     * サイズ変更等やります。
     * @param videoHeight 動画の高さ
     * @param bottomSheetView BottomSheetを設定したView
     * @param playerView プレイヤーのView
     * */
    fun init(videoWidth: Int, bottomSheetView: View, playerView: View) {
        draggablePlayerView = playerView
        draggableBottomSheetView = bottomSheetView

        // 最小値
        val videoHeight = (videoWidth / 16) * 9
        peekHeight = videoHeight

        // 画面の幅
        val displayWidth = DisplaySizeTool.getDisplayWidth(context)

        /** 画面の幅からミニプレイヤーの幅を引いた値 */
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
                            if (isFullScreenMode) {
                                // 全画面
                                playerView.updateLayoutParams<LinearLayout.LayoutParams> {
                                    width = displayWidth
                                    height = DisplaySizeTool.getDisplayHeight(context)
                                    topMargin = 0
                                }
                            } else {
                                // そうじゃない（機械翻訳並感）
                                playerView.updateLayoutParams<LinearLayout.LayoutParams> {
                                    width = displayWidth / 2
                                    height = (width / 16) * 9
                                    // 横画面時はプレイヤーを真ん中にしたい。ので上方向のマージンを設定して真ん中にする
                                    // とりあえず最大時にかけるマージン計算
                                    val maxTopMargin = (DisplaySizeTool.getDisplayHeight(context) - height) / 2
                                    // そして現在かけるべきマージンを計算
                                    topMargin = maxTopMargin
                                }
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
                        if (isFullScreenMode) {
                            // フルスクリーン時
                            // 展開時のプレイヤーとミニプレイヤーとの差分を出す。引き算
                            val sabun = displayWidth - videoWidth
                            playerView.updateLayoutParams<LinearLayout.LayoutParams> {
                                val calcWidth = videoWidth + (sabun * slideOffset)
                                width = calcWidth.roundToInt()
                                height = (width / 16) * 9
                            }
                        } else {
                            // 展開時のプレイヤーとミニプレイヤーとの差分を出す。どれぐらい掛ければ展開時のサイズになるのか
                            val sabun = (displayWidth / 2f) - videoWidth
                            playerView.updateLayoutParams<LinearLayout.LayoutParams> {
                                // 最初にミニプレイヤーのサイズを足さないとミニプレイヤー消滅する
                                val calcWidth = videoWidth + (sabun * slideOffset)
                                width = calcWidth.roundToInt()
                                height = (width / 16) * 9
                                // 横画面時はプレイヤーを真ん中にしたい。ので上方向のマージンを設定して真ん中にする
                                // とりあえず最大時にかけるマージン計算
                                val maxTopMargin = (DisplaySizeTool.getDisplayHeight(context) - height) / 2
                                // そして現在かけるべきマージンを計算
                                val currentTopMargin = maxTopMargin * slideOffset
                                topMargin = currentTopMargin.roundToInt()
                            }
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

    /** ミニプレイヤー状態かどうかを返す */
    fun isMiniPlayerMode(): Boolean {
        return state == STATE_COLLAPSED
    }

    /** 横画面かどうか */
    private fun isLandScape() = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    /**
     * プレイヤー部分のタッチが検出できない(おそらくクリックイベント追加したから)のでViewGroup特権を発動
     * [onTouchEvent]が拾えなくてもViewGroupならこの方法が使える。
     *
     * [isDraggableAreaPlayerOnly]の判定で利用。
     * */
    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: T, event: MotionEvent): Boolean {
        // ちなみにchild.leftは0を返す
        val isTouchingSwipeTargetView = if (!isDraggableAreaPlayerOnly) {
            event.x > currentMiniPlayerXPos
                    && event.x < currentMiniPlayerXPos + draggablePlayerView!!.width
        } else {
            // 操作ターゲットをプレイヤーに限定
            event.x > currentMiniPlayerXPos
                    && event.x < currentMiniPlayerXPos + draggablePlayerView!!.width
                    && event.y > draggableBottomSheetView!!.y
                    && event.y < draggableBottomSheetView!!.y + draggablePlayerView!!.height
        }
        // 展開時 + ドラッグ範囲限定 + いま指がプレイヤーに触れている 場合はプレイヤーをミニプレイヤーにできる
        isDraggable = if (!isMiniPlayerMode() && isDraggableAreaPlayerOnly && isTouchingSwipeTargetView) {
            true
        } else isMiniPlayerMode() // そうじゃなくてもミニプレイヤー時は操作可能に
        return super.onInterceptTouchEvent(parent, child, event)
    }

    override fun onTouchEvent(parent: CoordinatorLayout, child: T, event: MotionEvent): Boolean {
        // プレイヤーを触っているときのみタッチイベントを渡す。translateXの値変えてもタッチは何故か行くので制御
        // ちなみにchild.leftは0を返す
        val isTouchingSwipeTargetView = if (!isDraggableAreaPlayerOnly) {
            event.x > currentMiniPlayerXPos
                    && event.x < currentMiniPlayerXPos + draggablePlayerView!!.width
        } else {
            // 操作ターゲットをプレイヤーに限定
            event.x > currentMiniPlayerXPos
                    && event.x < currentMiniPlayerXPos + draggablePlayerView!!.width
                    && event.y > draggableBottomSheetView!!.y
                    && event.y < draggableBottomSheetView!!.y + draggablePlayerView!!.height
        }
        // もしくは進行中なら操作を許可
        val isProgress = progress < 1f && progress > 0f
        return if (isTouchingSwipeTargetView || isProgress) {
            super.onTouchEvent(parent, child, event)
        } else {
            false // タッチイベントを他に回す。動画一覧RecyclerViewのスクロールなどで消費
        }
    }

    /**
     * フルスクリーンへ遷移する
     *
     * 再生画面（[draggablePlayerView]）の幅を画面いっぱいにして、マージンを解除してるだけ
     *
     * 横画面である必要があります。
     * */
    fun toFullScreen() {
        isFullScreenMode = true
        draggablePlayerView?.updateLayoutParams<LinearLayout.LayoutParams> {
            // 幅を治す
            width = DisplaySizeTool.getDisplayWidth(context)
            height = DisplaySizeTool.getDisplayHeight(context)
            // マージン解除
            topMargin = 0
        }
    }

    /**
     * フルスクリーンを解除する
     *
     * 再生画面（[draggablePlayerView]）の幅を画面の半分の値にして、マージンを設定してるだけ
     *
     * 横画面である必要があります。
     * */
    fun toDefaultScreen() {
        isFullScreenMode = false
        draggablePlayerView?.updateLayoutParams<LinearLayout.LayoutParams> {
            // 幅を治す
            width = DisplaySizeTool.getDisplayWidth(context) / 2
            height = (width / 16) * 9
            // 横画面時はプレイヤーを真ん中にしたい。ので上方向のマージンを設定して真ん中にする
            if (isLandScape()) {
                val maxTopMargin = (DisplaySizeTool.getDisplayHeight(context) - height) / 2
                topMargin = maxTopMargin
            }
        }
    }

}