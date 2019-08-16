package io.github.takusan23.tatimidroid

import org.json.JSONObject

class CommentJSONParse(val commentJson: String, var roomName: String) {

    var comment = ""
    var commentNo = ""
    var userId = ""
    var date = ""
    var premium = ""

    init {
        val jsonObject = JSONObject(commentJson)
        if (jsonObject.has("chat")) {
            val chatObject = jsonObject.getJSONObject("chat")
            comment = chatObject.getString("content")
            commentNo = chatObject.getString("no")
            userId = chatObject.getString("user_id")
            date = chatObject.getString("date")
            //プレミアムかどうかはJSONにpremiumがあればいい（一般にはないので存在チェックいる）
            if (chatObject.has("premium")) {
                when (chatObject.getString("premium").toInt()) {
                    1 -> premium = "\uD83C\uDD7F"
                    2 -> premium = "運営"
                }
            }
        }
    }
}