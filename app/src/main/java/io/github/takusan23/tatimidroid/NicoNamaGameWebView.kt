package io.github.takusan23.tatimidroid

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager

class NicoNamaGameWebView(val context: Context?, val liveId: String) {

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

            // 背景透明化。これはハードウェアアクセラレーションを無効にしないと動かない。
            // ExoPlayerの上に重ねるので透明必須。
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

            // 読み込み完了したらゲームのCanvas要素のみ取り出すJavaScript（ブックマークレット）を実行する
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // 読み込み完了

                    // 全画面ボタン押す
                    loadUrl("javascript:document.getElementsByClassName('___fullscreen-button___1ZfbK')[0].click()")
                    // コメント非表示ボタン押す
                    loadUrl("javascript:document.getElementsByClassName('___comment-button___KaSS7')[0].click()")
                    // 動画プレイヤー消す
                    loadUrl("javascript:(function(){document.getElementsByTagName('video')[0].parentNode.remove()})()")
                    // コントローラー非表示
                    loadUrl("javascript:(function(){document.getElementsByClassName('___controller-display-button___18KFH')[0].click())()")
                    // 背景を透明に
                    loadUrl("javascript:(function(){document.getElementsByClassName('___player-display___35bAr')[0].style.backgroundColor = 'transparent' })()")
                    loadUrl("javascript:(function(){document.getElementsByClassName('___watch-page___th_ha ___ga-ns-watch-page___pYeNv ___page___1G6yH')[0].style.backgroundColor = 'transparent' })()")

                    // 読み込み完了したので表示
                    view?.isVisible = true
                }
            }
        }
    }

    /**
     * ライフサイクル等でアプリに戻ってきたら必ず呼ぶ必要があります。
     * が、別にWebView#reload()を読んでるだけなんですけどね。
     * */
    fun reload() {
        webView.apply {
            isVisible = false
            reload()
        }
    }

}