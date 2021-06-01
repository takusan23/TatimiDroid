package io.github.takusan23.tatimidroid.tool

/**
 * キリ番か判断する関数がある
 *
 * たまによくキリ番に遭遇するので
 *
 * # キリ番の定義
 * - 前提として3桁以上
 * - 1000 など100で割ってあまりが出ない
 * - すべて同じ数字
 * - 2525
 *
 * */
object KiribanTool {

    /**
     * 1000とか5000とかの、先頭以外が0の数字のみをキリ番として扱う場合の正規表現。今は使っていない
     * 先頭が0から9、二番目から最後までが0の正規表現。
     * */
    val zeroRegex = "(^[0-9])(0+\$)".toRegex()

    /**
     * キリ番判定機。なんかもっと複雑になるかと思ってたんだけどなんか短く掛けたのでKotlinすごい
     *
     * キリ番ならtrueを返す
     * */
    fun checkKiriban(number: Int): Boolean {
        return when {
            // まず3桁以上
            number <= 100 -> false
            // 割り算のあまりで判定
            (number % 100) == 0 -> true
            // 同じ数字連続。文字列を文字の配列に変換した後、同じ数字を消し飛ばした結果の配列の要素数が1ならゾロ目
            number.toString().toCharArray().distinct().size == 1 -> true
            // 2525
            number == 2525 -> true
            // 当てはまらない
            else -> false
        }
    }

}