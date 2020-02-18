package io.github.takusan23.tatimidroid

import org.json.JSONObject

class CommentJSONParse(val commentJson: String, var roomName: String) {

    var comment = ""
    var commentNo = ""
    var userId = ""
    var date = ""
    var premium = ""
    var mail = ""
    var vpos = ""
    var origin = ""
    var score = ""

    init {
        val jsonObject = JSONObject(commentJson)
        if (jsonObject.has("chat")) {
            val chatObject = jsonObject.getJSONObject("chat")
            comment = chatObject.getString("content")
            if (chatObject.has("no")) {
                commentNo = chatObject.getString("no")
            }
            userId = chatObject.getString("user_id")
            date = chatObject.getString("date")
            if (chatObject.has("vpos")) {
                vpos = chatObject.getString("vpos")
            }
            //プレミアムかどうかはJSONにpremiumがあればいい（一般にはないので存在チェックいる）
            if (chatObject.has("premium")) {
                when (chatObject.getString("premium").toInt()) {
                    1 -> premium = "\uD83C\uDD7F"
                    2 -> premium = "運営"
                    3 -> premium = "生主"
                }
            }
            //NGスコア？
            if (chatObject.has("score")) {
                score = chatObject.getInt("score").toString()
            }
            //コメントが服従表示される問題
            if (chatObject.has("origin")) {
                origin = chatObject.getString("origin")
            }
            //mailの中に色コメントの色の情報があったりする
            if (chatObject.has("mail")) {
                mail = chatObject.getString("mail")
            }
        }
    }
}