package io.github.takusan23.tatimidroid

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.takusan23.tatimidroid.Tool.DisplaySizeTool
import kotlin.math.roundToInt

/**
 * 第3世代プレイヤーレイアウト
 *
 * このFrameLayoutに乗った [playerViewParentViewGroup] を動かします
 *
 * また、[playerView]のアスペも16:9にします
 *
 * [setup]関数参照
 * */
class PlayerParentFrameLayout(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {

    /** [addOnStateChangeListener]の引数に来る定数たち */
    companion object {
        /** 1：通常プレイヤーを表す */
        const val PLAYER_STATE_DEFAULT = 1

        /** 2：ミニプレイヤーを表す */
        const val PLAYER_STATE_MINI = 2

        /** ３：プレイヤーが終了したことを表す */
        const val PLAYER_STATE_DESTROY = 3
    }

    /** ミニプレイヤー時の幅 */
    private var miniPlayerWidth = if (isLandScape()) DisplaySizeTool.getDisplayWidth(context) / 3 else DisplaySizeTool.getDisplayWidth(context) / 2

    /** ミニプレイヤーになったときの高さ。[miniPlayerWidth]を16で割って9をかけることで16:9になるようにしている */
    private val miniPlayerHeight: Int
        get() = (miniPlayerWidth / 16) * 9

    /** プレイヤーのView */
    private var playerView: View? = null

    /** プレイヤーが乗っているViewGroup */
    private var playerViewParentViewGroup: ViewGroup? = null

    /**
     * ミニプレイヤーを無効にする場合はtrue
     * */
    var isDisableMiniPlayerMode = false

    /** デフォルトプレイヤー時全体の高さ */
    private val parentViewGroupHeight: Int
        get() = playerViewParentViewGroup!!.height

    /** デフォルトプレイヤー時全体の幅 */
    private val parentViewGroupWidth: Int
        get() = playerViewParentViewGroup!!.width

    /** 遷移アニメ中の場合はtrue */
    var isMoveAnimating = false

    /** 今[playerView]を触っているか */
    var isTouchingPlayerView = false

    /**
     * 現在プレイヤー移動中かどうか。ただしこっちはユーザーが操作している場合のみ
     * アニメーション時は[isMoveAnimating]を参照
     * */
    var isProgress = false

    /** 勢いよくスワイプしたときにミニプレイヤー、通常画面に切り替えられるんだけど、それのしきい値 */
    val flickSpeed = 5000

    /**
     * [toDefaultPlayer]、[toMiniPlayer]、[toDestroyPlayer]で実行されるアニメーションの
     * 実行時間
     * */
    val durationMs = 500L

    /**
     * いまフルスクリーンかどうか
     * */
    var isFullScreenMode = false
        private set

    /** 終了済みかどうか。（２回[addOnStateChangeListener]の[PLAYER_STATE_DESTROY]が呼ばれるらしいので） */
    var isAlreadyDestroyed = false
        private set

    /** 進捗状態 0から1だけど、終了アニメの場合は1以上になる */
    var progress = 0f
        private set

    /**
     * 終了したときに呼ばれる関数。関数が入ってる配列、なんかおもろい
     * [isHideable]がtrueじゃないと呼ばれない。あと複数回呼ばれるかも
     * */
    private var endListenerList = arrayListOf<(() -> Unit)>()

    // 移動速度計測で使う
    private var mVelocityTracker: VelocityTracker? = null

    // タッチ位置を修正するために最初のタッチ位置を持っておく
    private var firstTouchYPos = 0f

    /** 操作中の移動速度 */
    private var slidingSpeed = 0f

    /** 今の状態を持っておく */
    private var currentState = PLAYER_STATE_DEFAULT


    /**
     * 終了したときに呼ばれる関数。関数が入ってる配列、なんかおもろい
     * [isHideable]がtrueじゃないと呼ばれない。あと複数回呼ばれるかも
     * */
    private var stateChangeListenerList = arrayListOf<((state: Int) -> Unit)>()

    /**
     * 操作中に呼ばれる。関数が入ってる配列、なんかおもろい
     * 通常プレイヤーなら0、ミニプレイヤーなら1になると思う
     * */
    private var progressListenerList = arrayListOf<((progress: Float) -> Unit)>()

    /**
     * 初期設定を行いますので利用前にこの関数をよんでください
     * @param playerView サイズ変更を行うView。これはアスペクト比の調整のために使う。VideoViewとかSurfaceViewとか？
     * @param playerViewParent [playerView]が乗っているViewGroup。こいつを[View.setTranslationY]などを使って動かす。
     * @param portlateMiniPlayerWidth 省略可能。縦画面のときのミニプレイヤーの幅。省略すると画面の幅の半分
     * @param landscapeMiniPlayerWidth 省略可能。横画面のときのミニプレイヤーの幅。省略すると画面の幅の三分の一
     * */
    fun setup(playerView: View, playerViewParent: ViewGroup, portlateMiniPlayerWidth: Int = DisplaySizeTool.getDisplayWidth(context) / 2, landscapeMiniPlayerWidth: Int = DisplaySizeTool.getDisplayWidth(context) / 3) {
        this.playerView = playerView
        this.playerViewParentViewGroup = playerViewParent
        this.miniPlayerWidth = if (isLandScape()) landscapeMiniPlayerWidth else portlateMiniPlayerWidth
        // 横画面時は上方向のマージンをかける
        setLandScapeTopMargin(1f)
    }

    /**
     * クリックイベントを貼ってるとうまく動かないので、ViewGroup特権を発動させる
     *
     * ViewGroupの場合は[onTouchEvent]の他に[onInterceptTouchEvent]が使えるのでこっちでタッチイベントをもらう
     * */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // 移動させる
        if (ev != null) {
            passMotionEvent(ev)
        }
        return super.onInterceptTouchEvent(ev)
    }

    /** タッチイベントを元にプレイヤーを動かす */
    private fun passMotionEvent(event: MotionEvent) {
        // apply { } を利用しているので translationY を利用すると playerViewParentViewGroup になります
        playerViewParentViewGroup?.apply {

            // 無効状態 か プレイヤーをタッチしていない場合は return
            if (isDisableMiniPlayerMode) return

            // setup関数よんだ？
            if (playerView != null && playerViewParentViewGroup != null) {

                /** タッチ位置修正 */
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> firstTouchYPos = event.y - translationY
                    MotionEvent.ACTION_UP -> firstTouchYPos = 0f
                }
                /** タッチ位置を修正する。そのままevent.yを使うと指の位置にプレイヤーの先頭が来るので */
                val fixYPos = event.y - firstTouchYPos

                /** 進捗具合 */
                val progress = fixYPos / (parentViewGroupHeight - miniPlayerHeight).toFloat()

                /** 進行途中の場合はtrue */
                isProgress = progress < 1f && progress > 0f

                /** プレイヤータッチしているか */
                isTouchingPlayerView = isTouchingPlayerView(event)

                // フリック時の処理。早くフリックしたときにミニプレイヤー、通常画面へ素早く切り替える
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mVelocityTracker?.clear()
                        mVelocityTracker = mVelocityTracker ?: VelocityTracker.obtain()
                        mVelocityTracker?.addMovement(event)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // プレイヤータッチ中のみ
                        if (isTouchingPlayerView) {
                            // 移動中。ここでは移動速度を計測している
                            mVelocityTracker?.apply {
                                val pointerId = event.getPointerId(event.actionIndex)
                                addMovement(event)
                                computeCurrentVelocity(1000)
                                slidingSpeed = getYVelocity(pointerId)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        /**
                         * 指を離した時に、移動速度をもとにしてプレイヤーを移動させて姿を変える
                         * ちなみに ACTION_MOVE でプレイヤーの移動処理をやらないかというと、移動中に減速した場合に対応できないため（減速したらプレイヤーは指と一緒に動いてほしい）
                         * */
                        mVelocityTracker?.recycle()
                        mVelocityTracker = null
                        when {
                            // ミニプレイヤーへ
                            slidingSpeed > flickSpeed -> {
                                toMiniPlayer()
                                mVelocityTracker?.recycle()
                                mVelocityTracker = null
                            }
                            // 通常プレイヤーへ
                            slidingSpeed < -flickSpeed -> {
                                toDefaultPlayer()
                                mVelocityTracker?.recycle()
                                mVelocityTracker = null
                            }
                        }
                    }
                }

                // フリックによる遷移をしていない場合
                if (!isMoveAnimating) {
                    // プレイヤーを操作中 または 進行中...（通常画面でもなければミニプレイヤーでもない）
                    if (isTouchingPlayerView || (!isDefaultScreen() && !isMiniPlayer())) {
                        when (event.action) {
                            MotionEvent.ACTION_MOVE -> {
                                // サイズ変更
                                toPlayerProgress(progress)
                            }
                            MotionEvent.ACTION_UP -> {
                                /**
                                 * 上のフリックでのミニプレイヤー、通常切り替えを実施済みかどうか
                                 * 移動速度から判定
                                 * */
                                val isAlreadyMoveAnimated = slidingSpeed > flickSpeed || slidingSpeed < -flickSpeed
                                if (!isAlreadyMoveAnimated) {
                                    // 画面の半分以上か以下か
                                    if (event.y < (parentViewGroupHeight / 2)) {
                                        // 以上。上半分で離した場合は上に戻す
                                        toDefaultPlayer()
                                    } else {
                                        // 以下。下半分で戻した場合はミニプレイヤーへ
                                        if (translationY > (parentViewGroupHeight - miniPlayerHeight) + (miniPlayerHeight / 2)) {
                                            // 消せる + ミニプレイヤーでも更に半分進んだ場合は終了アニメへ
                                            toDestroyPlayer()
                                        } else {
                                            toMiniPlayer()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * [progress]に入れた分だけ通常プレイヤーに切り替えていく関数
     *
     * 1 -> 0 通常プレイヤーへ
     * 0 -> 1 ミニプレイヤーへ
     *
     * 1f以上を入れる + [isHideable]がtrue の場合は終了アニメーションとなります
     *
     * @param progress 0から1まで
     * */
    private fun toPlayerProgress(argProgress: Float) {
        playerViewParentViewGroup?.apply {
            // 進捗具合。小数点3桁ぐらいまでにする
            this@PlayerParentFrameLayout.progress = argProgress

            val maxTransitionX = (parentViewGroupWidth - miniPlayerWidth).toFloat()
            // サイズ変更
            val calcTranslationY = (parentViewGroupHeight - miniPlayerHeight) * progress

            if (calcTranslationY >= 0f) {
                translationY = calcTranslationY
                // 横にずらす / プレイヤーサイズ変更 は終了アニメで使わないため
                if (calcTranslationY <= (parentViewGroupHeight - miniPlayerHeight).toFloat()) {
                    // 横にずらす
                    translationX = maxTransitionX * progress
                    // プレイヤーサイズ変更
                    if (isLandScape()) {
                        playerView!!.updateLayoutParams {
                            // 展開時のプレイヤーとミニプレイヤーとの差分を出す。どれぐらい掛ければ展開時のサイズになるのか
                            val sabun = if (isFullScreenMode) {
                                parentViewGroupWidth - miniPlayerWidth.toFloat()
                            } else {
                                (parentViewGroupWidth / 2f) - miniPlayerWidth
                            }
                            width = miniPlayerWidth + (sabun * (1f - progress)).toInt()
                            height = (width / 16) * 9
                        }
                    } else {
                        playerView!!.updateLayoutParams {
                            width = miniPlayerWidth + (maxTransitionX * (1f - progress)).toInt()
                            height = (width / 16) * 9
                        }
                    }
                }
            }

            /** それとは関係ないんだけど、横モード時は上方向のマージンを掛けて真ん中に来るようにしたい */
            if ((1f - progress) in 0f..1f) {
                setLandScapeTopMargin((1f - progress))
            }

            /**
             * [progressListenerList]を呼ぶ
             * 0から1までの範囲で
             * */
            if (progress in 0f..1f || progress in 1f..0f) {
                progressListenerList.forEach { it.invoke(progress) }
            }

            // 画面回転するとなんか NaN になる。JS以外にもこの概念あったのか
            if (!progress.isNaN()) {
                /**
                 * [addOnStateChangeListener]を呼ぶ
                 * */
                when {
                    isDefaultScreen() -> {
                        // 違ったら入れる
                        if (currentState != PLAYER_STATE_DEFAULT) {
                            stateChangeListenerList.forEach { it.invoke(PLAYER_STATE_DEFAULT) }
                            currentState = PLAYER_STATE_DEFAULT
                        }
                    }
                    isMiniPlayer() -> {
                        // 違ったら入れる
                        if (currentState != PLAYER_STATE_MINI) {
                            stateChangeListenerList.forEach { it.invoke(PLAYER_STATE_MINI) }
                            currentState = PLAYER_STATE_MINI
                        }
                    }
                    translationY.roundToInt() == parentViewGroupHeight -> {
                        // まだ終了済みではない
                        if (!isAlreadyDestroyed) {
                            // 違ったら入れる
                            if (currentState != PLAYER_STATE_DESTROY) {
                                stateChangeListenerList.forEach { it.invoke(PLAYER_STATE_DESTROY) }
                                isAlreadyDestroyed = true
                                currentState = PLAYER_STATE_DESTROY
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * 横画面時にプレイヤーの上方向にマージンをかける
     *
     * @param progress 0 ~ 1 の範囲で。1が通常
     * */
    private fun setLandScapeTopMargin(progress: Float) {
        if (isLandScape() && !isFullScreenMode) {
            playerView!!.updateLayoutParams<LinearLayout.LayoutParams> {
                // 横画面時はプレイヤーを真ん中にしたい。ので上方向のマージンを設定して真ん中にする
                // とりあえず最大時にかけるマージン計算
                val maxTopMargin = (DisplaySizeTool.getDisplayHeight(context) - height) / 2
                // そして現在かけるべきマージンを計算
                val currentTopMargin = maxTopMargin * progress
                topMargin = currentTopMargin.roundToInt()
            }
        }
    }

    /**
     * 全画面へ遷移する
     * */
    fun toFullScreen() {
        isFullScreenMode = true
        playerView!!.updateLayoutParams<LinearLayout.LayoutParams> {
            // 幅を治す
            width = DisplaySizeTool.getDisplayWidth(context)
            height = DisplaySizeTool.getDisplayHeight(context)
            // マージン解除
            topMargin = 0
        }
    }


    /**
     * フルスクリーンを解除する。[toDefaultPlayer]と空目しないように！
     *
     * 再生画面（[draggablePlayerView]）の幅を画面の半分の値にして、マージンを設定してるだけ
     *
     * 横画面である必要があります。
     * */
    fun toDefaultScreen() {
        isFullScreenMode = false
        playerView!!.updateLayoutParams<LinearLayout.LayoutParams> {
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

    /** 通常プレイヤーへ遷移 */
    fun toDefaultPlayer() {
        // 同じなら無視
        if (isDefaultScreen()) return

        isMoveAnimating = true

        /** 開始時の進行度。途中で指を離した場合はそこからアニメーションを始める */
        val startProgress = playerViewParentViewGroup!!.translationY / parentViewGroupHeight

        /** 第一引数から第２引数までの値を払い出してくれるやつ。 */
        ValueAnimator.ofFloat(startProgress, 0f).apply {
            duration = durationMs
            addUpdateListener {
                val progress = (it.animatedValue as Float)
                toPlayerProgress(progress)
                // 最初(0f)と最後(1f)以外にいたら動作中フラグを立てる
                isMoveAnimating = it.animatedFraction < 1f && it.animatedFraction > 0f
            }
        }.start()
    }

    /** ミニプレイヤーへ遷移する */
    fun toMiniPlayer() {
        // 同じなら無視
        if (isMiniPlayer()) return

        isMoveAnimating = true

        /** 開始時の進行度。途中で指を離した場合はそこからアニメーションを始める */
        val startProgress = (playerViewParentViewGroup!!.translationY + miniPlayerHeight) / parentViewGroupHeight

        /** 第一引数から第２引数までの値を払い出してくれるやつ。 */
        ValueAnimator.ofFloat(startProgress, 1f).apply {
            duration = durationMs
            addUpdateListener {
                val progress = it.animatedValue as Float
                toPlayerProgress(progress)
                // 最初(0f)と最後(1f)以外にいたら動作中フラグを立てる
                isMoveAnimating = it.animatedFraction < 1f && it.animatedFraction > 0f
            }
        }.start()
    }

    /**
     * プレイヤーを終了させる。
     *
     * [isHideable]がtrueじゃないと動かない
     * */
    fun toDestroyPlayer() {
        /**
         * 開始時の進行度。途中で指を離した場合はそこからアニメーションを始める
         * */
        val startProgress = (playerViewParentViewGroup!!.translationY + miniPlayerHeight) / parentViewGroupHeight

        /**
         * 終了地点
         * なんかしらんけどこの計算式で出せた（1.8位になると思う。この値を[toPlayerProgress]に渡せばええんじゃ？）
         * */
        val endProgress = parentViewGroupHeight / (parentViewGroupHeight - miniPlayerHeight).toFloat()

        /**
         * 第一引数から第２引数までの値を払い出してくれるやつ。
         * */
        ValueAnimator.ofFloat(startProgress, endProgress).apply {
            duration = durationMs
            addUpdateListener {
                toPlayerProgress(it.animatedValue as Float)
                // 最初(0f)と最後(1f)以外にいたら動作中フラグを立てる
                isMoveAnimating = it.animatedFraction < 1f && it.animatedFraction > 0f
            }
        }.start()
    }

    /**
     * 進捗状況のコールバックを追加する
     * @param callback プレイヤーが移動すると呼ばれる
     * */
    fun addOnProgressListener(callback: (progress: Float) -> Unit) {
        progressListenerList.add(callback)
    }

    /**
     * プレイヤーの状態が変わったら呼ばれる
     * stateの値の説明は[PLAYER_STATE_DEFAULT]等を参照してください
     * @param callback プレイヤーの移動、アニメーションが終了したら呼ばれる
     * */
    fun addOnStateChangeListener(callback: (state: Int) -> Unit) {
        stateChangeListenerList.add(callback)
    }

    /**
     * BottomNavigationのHeightも一緒に変化させる場合
     * @param bottomNavigationView 変化させたいView
     * */
    fun setupBottomNavigation(bottomNavigationView: BottomNavigationView) {
        // doOnLayoutを使うとHeightが取れる状態になったらコールバックが呼ばれる（本当は：生成直後にgetHeight()を呼ぶと0が帰ってくるのでちょっと待たないといけない）
        bottomNavigationView.doOnLayout { navView ->
            // 高さ調整
            val defaultNavViewHeight = navView.height
            // 進捗を購読する
            addOnProgressListener { progress ->
                navView.updateLayoutParams {
                    // LienarLayoutが親の場合は height = 0 が使えるけどそうじゃないので最低値は1にする
                    height = (defaultNavViewHeight * progress).toInt() + 1
                }
            }
            // とりあえず1にする
            bottomNavigationView.updateLayoutParams {
                height = 1
            }
        }
    }

    /**
     * 現在、プレイヤー（[playerView]）に触れているかを返す
     * @param [android.view.View.OnTouchListener]など参照
     * @return 触れていればtrue
     * */
    private fun isTouchingPlayerView(event: MotionEvent): Boolean {
        val left = playerViewParentViewGroup!!.translationX
        val right = left + playerView!!.width
        val top = playerViewParentViewGroup!!.translationY
        val bottom = top + playerView!!.height
        // Kotlinのこの書き方（in演算子？範囲内かどうかを比較演算子なしで取れる）すごい
        return event.x in left..right && event.y in top..bottom
    }


    /**
     * 通常画面の場合はtrueを返す
     * */
    fun isDefaultScreen() = progress == 0.0f

    /**
     * ミニプレイヤーのときはtrueを返す
     * */
    fun isMiniPlayer() = progress == 1.0f

    /**
     * 画面が横向きかどうかを返す
     * */
    private fun isLandScape() = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}