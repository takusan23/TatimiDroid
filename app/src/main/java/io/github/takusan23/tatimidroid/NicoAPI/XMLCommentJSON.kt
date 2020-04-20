package io.github.takusan23.tatimidroid.NicoAPI

import android.content.Context
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader


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
        println(System.currentTimeMillis())

        var tmp = ""

        // ScopedStorage
        val media = context?.getExternalFilesDir(null)

        // コメントXML
        val xmlPath = commentXmlFilePath(fileName) ?: return@async 1
        val xmlFile = File(xmlPath)

        // 出力JSON
        val jsonArray = JSONArray()

        /**
         * Android標準でXMLをパースする。
         * 本当はJsoup使いたかったんだけど遅すぎた
         * */
        val factory =
            XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlFile.readText()));
        var eventType = parser.eventType
        // 終了まで繰り返す
        while (eventType != XmlPullParser.END_DOCUMENT) {
            // コメントのみ選ぶ
            if (parser.name == "chat") {
                val thread = parser.getAttributeValue(null, "thread")
                val vpos = parser.getAttributeValue(null, "vpos")
                val date = parser.getAttributeValue(null, "date")
                val date_usec = parser.getAttributeValue(null, "date_usec")
                val userId = parser.getAttributeValue(null, "user_id")
                val anonymcommenty = parser.getAttributeValue(null, "anonymcommenty")
                val score = if (parser.getAttributeValue(null, "score") != null) {
                    parser.getAttributeValue(null, "score")
                } else {
                    ""
                }
                val mail = parser.getAttributeValue(null, "mail")
                val origin = parser.getAttributeValue(null, "origin")
                val premium = parser.getAttributeValue(null, "premium")
                val content = parser.nextText()
                // JSONのchatオブジェクト作成
                val chatObject = JSONObject().apply {
                    put("thread", thread)
                    put("no", -1)
                    put("vpos", vpos)
                    put("leaf", 1)
                    put("date", date)
                    put("date_usec", date_usec)
                    put("anonymcommenty", anonymcommenty)
                    put("user_id", userId)
                    put("mail", mail)
                    put("origin", origin)
                    put("score", score)
                    put("content", content)
                    put("premium", premium)
                }
                jsonArray.put(JSONObject().put("chat", chatObject))
            }
            eventType = parser.next()
        }

        // 保存。
        val jsonFile = File("${media?.path}/cache/$fileName/${fileName}_comment.json")
        jsonFile.writeText(jsonArray.toString())

        println(System.currentTimeMillis())
        return@async 0
    }

/*
    */
    /**
     * XML形式のコメントファイルが存在するか。存在するときはtrue
     * @param fileName ファイル名。基本動画ID
     * *//*

    fun commentXmlFileExists(fileName: String): Boolean {
        // ScopedStorage
        val media = context?.getExternalFilesDir(null)
        // コメントXML
        val xmlFile = File("${media?.path}/cache/$fileName/${fileName}.xml")
        // ファイル存在するか
        return xmlFile.exists()
    }
*/

    /**
     * コメントXMLファイルのパスを返す関数。ない場合はnull
     * @param fileName フォルダ名。基本動画ID
     * */
    fun commentXmlFilePath(fileName: String): String? {
        // ScopedStorage
        val media = context?.getExternalFilesDir(null)
        // ふぉるだ
        val videoFile = File("${media?.path}/cache/$fileName")
        if (videoFile.listFiles() != null) {
            videoFile.listFiles().forEach {
                if (it.extension == "xml") {
                    return it.path
                }
            }
        }
        return null
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