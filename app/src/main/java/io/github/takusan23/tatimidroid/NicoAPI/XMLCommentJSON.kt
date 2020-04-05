package io.github.takusan23.tatimidroid.NicoAPI

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File

/**
 * xml形式のコメントをJSON形式に変換する。
 * */
class XMLCommentJSON(val context: Context?) {

    /**
     * xmlのコメントをJSON形式に変換する。多分重いからコルーチン
     * @param fileName ファイル名。
     * @return 0=成功 / 1=ファイル無いよ
     * */
    fun xmlToJSON(fileName: String): Deferred<Int> = GlobalScope.async {
        // ScopedStorage
        val media = context?.getExternalFilesDir(null)
        // コメントXML
        val xmlFile = File("${media?.path}/cache/$fileName/${fileName}.xml")
        // ファイル存在するか
        if (!commentXmlFileExists(fileName)) {
            return@async 1
        }
        // 読み込む
        val xmlData = xmlFile.readText()
        val xmlDocument = Jsoup.parse(xmlData, "", Parser.xmlParser())
        val chat = xmlDocument.getElementsByTag("chat")
        // 出力JSON
        val jsonArray = JSONArray()
        chat.forEach {
            val thread = it.attr("thread")
            val vpos = it.attr("vpos")
            val date = it.attr("date")
            val date_usec = it.attr("date_usec")
            val userId = it.attr("user_id")
            val anonymity = it.attr("anonymity")
            val score = if (it.hasAttr("score")) {
                it.attr("score")
            } else {
                ""
            }
            val mail = it.attr("mail")
            val origin = it.attr("origin")
            val premium = it.attr("premium")
            val content = it.text()
            // JSONのchatオブジェクト作成
            val chatObject = JSONObject().apply {
                put("thread", thread)
                put("no", -1)
                put("vpos", vpos)
                put("leaf", 1)
                put("date", date)
                put("date_usec", date_usec)
                put("anonymity", anonymity)
                put("user_id", userId)
                put("mail", mail)
                put("origin", origin)
                put("score", score)
                put("content", content)
                put("premium", premium)
            }
            jsonArray.put(JSONObject().put("chat", chatObject))
            // 保存。
            val jsonFile = File("${media?.path}/cache/$fileName/${fileName}_comment.json")
            jsonFile.writeText(jsonArray.toString())
            println("${chat.size} / ${jsonArray.length()}")
        }
        return@async 0
    }

    /**
     * XML形式のコメントファイルが存在するか。存在するときはtrue
     * @param fileName ファイル名。基本動画ID
     * */
    fun commentXmlFileExists(fileName: String): Boolean {
        // ScopedStorage
        val media = context?.getExternalFilesDir(null)
        // コメントXML
        val xmlFile = File("${media?.path}/cache/$fileName/${fileName}.xml")
        // ファイル存在するか
        return xmlFile.exists()
    }

    /**
     * JSON形式のコメントが存在するか。存在するとtrue
     * @param fileName ファイル名。基本動画ID
     * */
    fun commentJSONFileExists(fileName: String): Boolean {
        // ScopedStorage
        val media = context?.getExternalFilesDir(null)
        // コメントXML
        val xmlFile = File("${media?.path}/cache/$fileName/${fileName}_comment.json")
        // ファイル存在するか
        return xmlFile.exists()
    }

}