package io.github.takusan23.tatimidroid

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.widget.TextView
import java.io.File

/**
 * フォント変更機能でよく使うやつ。
 * */
class CustomFont(val context: Context?) {

    // フォントがあるフォルダー
    var fontFolder: File = File("${context?.getExternalFilesDir(null)}/font")

    // フォントフォルダーには一つのファイル（フォントファイル）しか存在しないでーす
    var fontFile = fontFolder.listFiles()[0]

    // TypeFace
    lateinit var typeface: Typeface

    init {
        // ファイルが存在する場合はTypeFaceつくる
        if (!isFontFileExists()) {
            typeface = Typeface.createFromFile(fontFile)
        }
    }

    /**
     * フォントファイルが存在するか
     * */
    fun isFontFileExists(): Boolean {
        return fontFile.exists()
    }

    /**
     * TextViewにフォントを設定する
     * */
    fun setTextViewFont(textView: TextView) {
        if (!isFontFileExists() && !::typeface.isInitialized) {
            // 存在しない + TypeFace初期化できない とき終了
            return
        }
        textView.typeface = typeface
    }

    /**
     * PaintにTypeFaceを設定する
     * */
    fun setPaintTypeFace(paint: Paint) {
        if (!isFontFileExists() && !::typeface.isInitialized) {
            // 存在しない + TypeFace初期化できない とき終了
            return
        }
        paint.typeface = typeface
    }


}