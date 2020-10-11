package io.github.takusan23.tatimidroid

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout

/**
 * MotionLayoutを置くためにあるFrameLayout。
 * そのままの状態で<onSwipe>を使うと、階層的に見て上の階層にあるViewへタッチイベントがいかなくなるのでその対策。
 * ついでに<onSwipe>で指定したViewのクリックイベントも取れるようにするなど。
 *
 * もしMotionLayoutが動かないときは `android:clickable="true"` を全部消してみると動くかもしれない
 *
 * */
class SwipeFixVerMotionLayoutParentFrameLayout(context: Context, attributeSet: AttributeSet? = null) : FrameLayout(context, attributeSet) {

    /** ドラッグする（スワイプに設定した）View。 */
    var swipeTargetView: View? = null

    /** この配列内に入っているIDは状態に関係なくタッチイベントを渡す */
    private var allowIdList: List<Int>? = null

    /** MotionLayoutの状態を知るためにMotionLayoutが必要 */
    var motionLayout: MotionLayout? = null

    /**
     * [swipeTargetView]のクリックイベント
     * 注意：ほんのちょっとだけ遅延させてから高階関数を呼んでいます。理由はこの上（子のView）のクリックイベントがうまく処理できないため。
     * ちょっと無理矢理感あるね
     * */
    var onSwipeTargetViewClickFunc: (() -> Unit)? = null

    /** [onSwipeTargetViewClickFunc]を呼ぶまでどれぐらい遅延させるか。 */
    var onSwipeTargetViewClickFuncDelayMs = 100L

    /**
     * 強制的にクリックを渡す時に使う。ここで指定しない場合、MotionLayout傘下にRecyclerView等が有ってもスクロール出来ない可能性があります。
     * そのためのこの関数。
     * MotionLayoutのConstraintSetで指定したIDを引数に入れることでそのIDの状態ならタッチを渡すことが出来ます。
     *
     * ここで入れたIDとタッチ中MotionLayoutの状態を取得して、一致している場合はタッチを特別に渡します。
     * */
    fun setAllowIds(vararg endId: Int) {
        this.allowIdList = endId.asList()
    }

    /** 子のViewへタッチイベントを渡すかどうか */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (swipeTargetView != null && ev != null && motionLayout != null) {
            // タッチがPlayerViewの中にあるときのみタッチイベントを渡す
            if (ev.x > swipeTargetView!!.left && ev.x < swipeTargetView!!.right && ev.y > swipeTargetView!!.top && ev.y < swipeTargetView!!.bottom) {
                // クリックさせるなど
                if (ev.action == MotionEvent.ACTION_DOWN) {
                    postDelayed({ onSwipeTargetViewClickFunc?.invoke() }, onSwipeTargetViewClickFuncDelayMs)
                }
                // 指定したViewを動かしている場合は渡す
                return super.onInterceptTouchEvent(ev)
            } else if (allowIdList!!.contains(motionLayout!!.currentState)) {
                // タッチイベントを渡すことが許可されているIDなら渡す
                return super.onInterceptTouchEvent(ev)
            } else {
                return true // true渡すとタッチイベントを渡さない（MotionLayoutが動く）
            }
        } else {
            return super.onInterceptTouchEvent(ev)
        }
    }

}