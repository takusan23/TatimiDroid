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
    lateinit var fontFile: File

    // TypeFace
    lateinit var typeface: Typeface

    init {
        // フォントフォルダーには一つのファイル（フォントファイル）しか存在しないでーす
        if (fontFolder.exists() && fontFolder.listFiles().isNotEmpty()) {
            // ファイルが存在する場合はTypeFaceつくる
            fontFile = fontFolder.listFiles()[0]
            typeface = Typeface.createFromFile(fontFile)
        }
    }


    /**
     * TextViewにフォントを設定する
     * */
    fun setTextViewFont(textView: TextView) {
        if (!::typeface.isInitialized) {
            //TypeFace初期化できない とき終了
            return
        }
        textView.typeface = typeface
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


}