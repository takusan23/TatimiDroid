package io.github.takusan23.tatimidroid.Fragment

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import io.github.takusan23.tatimidroid.Tool.CustomFont
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.adapter_comment_layout.*
import kotlinx.android.synthetic.main.fragment_font_setting.*
import java.io.File


class FontSettingFragment : Fragment() {

    // SAFのリクエスト判断用
    val fontFileOpenCode = 845

    lateinit var pref_setting: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_font_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)

        // フォントサイズEditTextへ
        fontSizeEditTextInit()

        // 人生リセットボタン
        fragment_font_setting_font_size_reset_button.setOnClickListener {
            // フォントサイズリセット
            // フォントサイズが指定されていなければ空文字が入る。
            pref_setting.edit {
                putFloat("setting_font_size_id", 12F)
                putFloat("setting_font_size_comment", 14F)
                apply()
                fontSizeEditTextInit()
            }
        }

        // フォント選択SAF
        fragment_font_setting_font_file_select_button.setOnClickListener {
            // フォント選択
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.apply {
                type = "font/*"
            }
            startActivityForResult(intent, fontFileOpenCode)
        }
        // フォントファイル削除
        fragment_font_setting_font_file_reset_button.setOnClickListener {
            // 選択したフォントリセット
            resetFont()
            // 更新
            updatePreview()
            // ファイル名
            fragment_font_setting_font_file_select_button.text =
                "${getString(R.string.setting_select_font)}\n${getSelectFontName()}"
        }

        // プレビュー更新
        updatePreview()
        // ファイル名
        fragment_font_setting_font_file_select_button.text =
            "${getString(R.string.setting_select_font)}\n${getSelectFontName()}"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fontFileOpenCode && resultCode == Activity.RESULT_OK && data!!.data != null) {
            // リクエストがフォント、選択が成功している場合
            // 削除
            resetFont()
            // コピー先フォルダー。ScopedStorageです。
            val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
            fontFolder.mkdir()
            // ふぁいるこぴー。
            val fileName = getFileName(data.data)
            val scopedStorageFontFile = File("${fontFolder}/$fileName")
            scopedStorageFontFile.createNewFile()
            scopedStorageFontFile.outputStream().use {
                context?.contentResolver?.openInputStream(data.data!!)?.copyTo(it)
            }
            // ファイル名
            fragment_font_setting_font_file_select_button.text =
                "${getString(R.string.setting_select_font)}\n${getSelectFontName()}"
            // 更新
            updatePreview()
        }
    }

    // プレビューの更新をするとき
    fun updatePreview() {
        adapter_room_name_textview.text = "ここがIDです"
        adapter_comment_textview.text = "ここがコメントです"
        // カスタムフォント、フォントサイズとか
        val customFont = CustomFont(context)
        // フォントサイズ適用
        adapter_room_name_textview.apply {
            textSize = customFont.userIdFontSize
        }
        adapter_comment_textview.apply {
            textSize = customFont.commentFontSize
        }
        // TypeFace適用
        customFont.apply {
            setTextViewFont(adapter_room_name_textview)
            setTextViewFont(adapter_comment_textview)
        }
    }


    // フォントサイズのEditText初期化
    fun fontSizeEditTextInit() {
        val customFont = CustomFont(context)
        fragment_font_setting_font_size_id_edittext.setText(customFont.userIdFontSize.toString())
        fragment_font_setting_font_size_comment_edittext.setText(customFont.commentFontSize.toString())
        // テキスト監視して自動で保存
        setTextWatcher(
            fragment_font_setting_font_size_comment_edittext,
            "setting_font_size_comment"
        )
        setTextWatcher(
            fragment_font_setting_font_size_id_edittext,
            "setting_font_size_id"
        )
    }

    // EditTextを監視して保存するやーつ
    fun setTextWatcher(textView: TextView, preferenceName: String) {
        textView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                pref_setting.edit {
                    // からのときは動かない + 大きすぎないように
                    if (s.toString().isNotEmpty() && s.toString().toFloat() <= 100F) {
                        putFloat(preferenceName, s.toString().toFloat())
                        apply()
                        // 更新
                        updatePreview()
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })
    }

    // 選択したフォント削除
    private fun resetFont() {
        // 削除
        val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
        if (fontFolder.exists()) {
            // 存在するとき削除
            fontFolder.listFiles()?.forEach {
                it.delete()
            }
        }
    }

    // 選択中のフォントの名前
    fun getSelectFontName(): CharSequence? {
        val fontFolder = File("${context?.getExternalFilesDir(null)}/font")
        if (fontFolder.listFiles()?.size ?: 0 <= 0) {
            return "未設定"
        }
        return fontFolder.listFiles()?.get(0)?.name ?: ""
    }

    // Uriからファイル名取得。取得失敗時は空の文字が
    fun getFileName(uri: Uri?): String {
        var fileName = ""
        if (uri == null) {
            return fileName
        }
        val cursor = context?.contentResolver?.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.moveToFirst()
        // 取得
        fileName = cursor?.getString(0) ?: ""
        cursor?.close()
        return fileName
    }

}