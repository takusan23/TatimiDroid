package io.github.takusan23.tatimidroid.Tool

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.TextView
import androidx.preference.PreferenceManager
import java.io.File

/**
 * フォント変更機能 / フォントサイズ変更機能 でよく使うやつ。
 *
 * フォントサイズ（ユーザーID）：setting_font_size_id　Float　初期値　12F
 * フォントサイズ（コメント）：setting_font_size_comment　Float 初期値　14F
 *
 * */
class CustomFont(val context: Context?) {

    private var prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

    // フォントがあるフォルダー
    private var fontFolder: File = File("${context?.getExternalFilesDir(null)}/font")

    // フォントフォルダーには一つのファイル（フォントファイル）しか存在しないでーす
    private lateinit var fontFile: File

    // TypeFace
    lateinit var typeface: Typeface

    // フォントサイズ（ゆーざーID）
    var userIdFontSize = 12F

    // フォントサイズ（コメント）
    var commentFontSize = 14F

    init {
        // フォントフォルダーには一つのファイル（フォントファイル）しか存在しないでーす
        if (fontFolder.exists() && fontFolder.listFiles().isNotEmpty()) {
            // ファイルが存在する場合はTypeFaceつくる
            fontFile = fontFolder.listFiles()[0]
            typeface = Typeface.createFromFile(fontFile)
        }
        // フォントサイズ取得
        userIdFontSize = prefSetting.getFloat("setting_font_size_id", 12F)
        commentFontSize = prefSetting.getFloat("setting_font_size_comment", 14F)
    }


    /**
     * TextViewにフォントを設定する
     * */
    fun setTextViewFont(textView: TextView) {
        if (!::typeface.isInitialized) {
            //TypeFace初期化できないときデフォルトを指定
            textView.typeface = Typeface.DEFAULT
        } else {
            textView.typeface = typeface
        }
    }

    /**
     * PaintにTypeFaceを設定する
     * */
    fun setPaintTypeFace(paint: Paint) {
        if (!::typeface.isInitialized) {
            //TypeFace初期化できない とき終了
            return
        }
        paint.typeface = typeface
    }

    /**
     * コメントファイルをコメント描画（CommentCanvas）にも適用するか？
     * @return CommentCanvasにも適用する設定有効時はtrue/そうじゃなければfalse
     * */
    val isApplyFontFileToCommentCanvas = prefSetting.getBoolean("setting_comment_canvas_font_file", false)

}