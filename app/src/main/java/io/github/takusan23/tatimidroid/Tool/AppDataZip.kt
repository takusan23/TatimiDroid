package io.github.takusan23.tatimidroid.Tool

import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object AppDataZip {

    fun createZipFile(activity: AppCompatActivity, vararg files: File) {
        // Activity Result API を使う
        val callback = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // ZIP作成
                val outputStream = activity.contentResolver.openOutputStream(result.data!!.data!!)
                ZipOutputStream(outputStream).apply {
                    // ZipEntryを作成してZipOutputStreamへ入れる
                    files.forEach { file ->
                        val inputStream = file.inputStream() // ファイル読み出し
                        val entry = ZipEntry(file.name) // ファイル名
                        putNextEntry(entry)
                        write(inputStream.readBytes()) // 書き込む。Kotlinかんたんすぎい
                        inputStream.close()
                        closeEntry()
                    }
                    close()
                }
            }
        }
        val simpleDateFormat = SimpleDateFormat("HH:mm:ss")
        // ZIPファイル生成
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.type = "application/zip"
        intent.putExtra(Intent.EXTRA_TITLE, "TatimiDroid_${simpleDateFormat.format(System.currentTimeMillis())}.zip")
        callback.launch(intent)
    }
}