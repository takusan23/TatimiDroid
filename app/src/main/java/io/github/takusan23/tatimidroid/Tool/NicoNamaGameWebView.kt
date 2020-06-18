package io.github.takusan23.tatimidroid.Tool

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager

/**
 * ニコ生ゲームを遊べるようにする？
 * 公式アプリでできるけどね
 * 注意：ハードウェアアクセラレーションをViewレベルで無効にします。
 * @param isWebViewPlayer コメント表示、生放送再生をWebViewで行います。完全にPC版サイトを表示していることになります。trueの場合は ハードウェアアクセラレーション を利用します
 * */
class NicoNamaGameWebView(val context: Context, val liveId: String, val isWebViewPlayer: Boolean = false) {

    // WebView。これにニコ生のPC版サイトを表示させてゲームを遊べるようにする。
    val webView: WebView = WebView(context)
    private val pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

    private var user_session: String

    init {
        // ユーザーセッション
        user_session = pref_setting.getString("user_session", "") ?: ""
        // WebViewサイズ
        webView.apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        // Cookieセットする
        val cookie = android.webkit.CookieManager.getInstance()
        cookie.apply {
            setAcceptCookie(true)
            setCookie(
                "https://www.nicovideo.jp/",
                "user_session=$user_session; Domain=.nicovideo.jp; Path=/;"
            )
            setAcceptThirdPartyCookies(webView, true)
        }
        webView.apply {
            // 読み込み終わるまで非表示？
            isVisible = false
            // JS有効
            settings.javaScriptEnabled = true
            // User-Agent変更
            settings.userAgentString = "TatimiDroid;@takusan_23"
            // 番組URL
            loadUrl("https://live2.nicovideo.jp/watch/$liveId")

            if (!isWebViewPlayer) {
                // 背景透明化。これはハードウェアアクセラレーションを無効にしないと動かない。
                // ExoPlayerの上に重ねるので透明必須。
                setBackgroundColor(Color.TRANSPARENT)
                setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
            }

            // 読み込み完了したらゲームのCanvas要素のみ取り出すJavaScript（ブックマークレット）を実行する
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 読み込み完了

                    // 全画面ボタン押す
                    loadUrl("javascript:document.getElementsByClassName('___fullscreen-button___1ZfbK')[0].click()")
                    if (isWebViewPlayer) {
                        // WebViewでPC版を再生するとき
                    } else {
                        // ニコ生ゲームのみを利用する場合
                        // コメント非表示ボタン押す
                        loadUrl("javascript:document.getElementsByClassName('___comment-button___KaSS7')[0].click()")
                        // 動画プレイヤー消す
                        loadUrl("javascript:(function(){document.getElementsByTagName('video')[0].parentNode.remove()})()")
                        // 背景を透明に
                        loadUrl("javascript:(function(){document.getElementsByClassName('___player-display___35bAr')[0].style.backgroundColor = 'transparent' })()")
                        loadUrl("javascript:(function(){document.getElementsByClassName('___watch-page___th_ha ___ga-ns-watch-page___pYeNv ___page___1G6yH')[0].style.backgroundColor = 'transparent' })()")
                    }
                    // 新市場の位置を変える
                    loadUrl("javascript:(function(){var ichiba = document.getElementsByClassName('___ichiba-counter-section___2B9Wc')[0]; ichiba.parentElement.parentElement.parentElement.parentElement.appendChild(ichiba)})()")
                    // 新市場サイズ変更と下にPadding
                    loadUrl("javascript:(function(){var ichiba = document.getElementsByClassName('___ichiba-counter-section___2B9Wc')[0]; ichiba.style.transform='scale(1.5)'; ichiba.style.width='1px'; ichiba.style.paddingBottom = '20px'})()")
                    // 新市場追加ボタンを消す
                    loadUrl("javascript:(function(){document.getElementsByClassName('___control-area___AqrGV')[0].remove()})()")
                    // シークバー/コメント投稿欄消す
                    loadUrl("javascript:(function(){document.getElementsByClassName('___player-display-footer___2DTQK')[0].style.display='none'})()")
                    //
                    loadUrl("javascript:(function(){document.getElementsByClassName('___launch-item-area___tiVih')[0].style.paddingRight='10px'})()")
                    // 何故か遅延実行しないと削除できないギフト・ニコニ広告ボタン
                    postDelayed({
                        loadUrl("javascript:(function(){document.getElementsByClassName('___official-locked-item-area___wS6uH')[0].remove()})()")
                        if (isWebViewPlayer) {
                            // WebViewでPC版を再生する場合
                            // 何故か初回読み込みに失敗するので再読み込み押す
                            loadUrl("javascript:(function(){document.getElementsByClassName('___reload-button___3V4Ng')[0].click()})()")
                        }
                    }, 5000)

                    // 読み込み完了したので表示
                    view?.isVisible = true
                }
            }

            setInitialScale(200)

        }
    }

}