package io.github.takusan23.tatimidroid

/** MainActivityの上に表示させるFragmentにはこれを継承してほしい。 */
interface MainActivityPlayerFragmentInterface {

    /**
     * 戻るボタンを押した時に呼ばれる関数
     * */
    fun onBackButtonPress()

    /**
     * ミニプレイヤーで再生している場合はtrueを返してほしい
     * */
    fun isMiniPlayerMode():Boolean

}