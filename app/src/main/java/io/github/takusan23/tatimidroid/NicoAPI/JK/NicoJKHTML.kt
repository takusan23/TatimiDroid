package io.github.takusan23.tatimidroid.NicoAPI.JK

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * ニコニコ実況のHTML取得からgetflvのAPI叩くところからコメントサーバー接続。
 * こいつhttpなのでAndroidManifestに書き足さないとhttp通信許可してくれない。運営ニコニコ実況切るだろこれ
 * これチャンネル一覧スクレイピングじゃなくてそのままハードコートしても良くね？
 * */
class NicoJKHTML {

    // コメントのやり取りに使う
    lateinit var socket: Socket

    /**
     * コメント送信時に使う値。
     * Socket通信すると最初に流れてくるらしい。
     * */
    var ticket = ""

    /**
     * チャンネル一覧のHTML取得。スクレイピング。コルーチンです。
     * @param type "tv"か"radio"か"bs"のどれか。基本"tv"で良いと思う。
     * @param userSession ユーザーセッション。多分なくても良い。
     * */
    fun getChannelListHTML(type: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("http://jk.nicovideo.jp/$type")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("Cookie", "user_session=$userSession")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            return@async okHttpClient.newCall(request).execute()
        }

    /**
     * getChannelListHTML()で取得したHTMLをスクレイピングする関数。
     * @param responseString getChannelListHTML()の中身。
     * @return NicoJKDataの配列
     * */
    fun parseChannelListHTML(responseString: String?): ArrayList<NicoJKData> {
        val dataList = arrayListOf<NicoJKData>()
        val html = Jsoup.parse(responseString)
        // チャンネル、チャンネルID
        val channelNameList = arrayListOf<String>()
        val channelIdList = arrayListOf<String>()

        html.getElementsByClass("name").forEach {
            val aTag = it.getElementsByTag("a")
            if (aTag.isNotEmpty()) {
                val name = aTag[0].text()
                val href = aTag[0].attr("href")
                channelNameList.add(name)
                channelIdList.add(href.replace("/watch/", ""))
            }
        }
        // 勢い
        val powerList = arrayListOf<String>()
        html.getElementsByClass("power").forEach {
            powerList.add(it.text())
        }
        for (i in 0 until channelNameList.size) {
            val name = channelNameList[i]
            val id = channelIdList[i]
            val power = powerList[i + 1]
            val data = NicoJKData(name, id, power)
            dataList.add(data)
        }
        return dataList
    }

    /**
     * getflvのAPIを叩く。
     * @param id jk1とか
     * @param userSession ユーザーセッション
     * */
    fun getFlv(id: String, userSession: String): Deferred<Response> = GlobalScope.async {
        val request = Request.Builder().apply {
            url("http://jk.nicovideo.jp/api/getflv?v=$id")
            header("User-Agent", "TatimiDroid;@takusan_23")
            header("Cookie", "user_session=$userSession")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        return@async okHttpClient.newCall(request).execute()
    }

    /**
     * getflvのAPIをパースする
     * @param getFlv()のレスポンス
     * @return getFlvData。コメントサーバーに接続する情報が入っている。getFlvResponseStringがnullの場合はnullが帰ってきます。
     * */
    fun parseGetFlv(getFlvResponseString: String?): getFlvData? {
        if (getFlvResponseString == null) {
            return null
        }
        // &区切りになっているので
        val list = getFlvResponseString.split("&")
        val threadId = list[1].split("=")[1]
        val ms = list[2].split("=")[1]
        val port = list[3].split("=")[1]
        val baseTime = list[13].split("=")[1]
        val userId = list[17].split("=")[1]
        val channelName = URLDecoder.decode(list[6].split("=")[1], "UTF-8") // パーセントエンコーディングうざい
        val isPremium = list[18].split("=")[1].toInt()
        return getFlvData(threadId, ms, port, baseTime, userId, channelName, isPremium)
    }

    lateinit var bufferedReader: BufferedReader

    /**
     * コメントサーバーに接続する。Socket通信だって。
     * 注意：while(true)が含まれているので一番最後に呼ばないとこれ以降の処理が行われません。
     * え？兵庫県警？アラートループ事件？知らないですね
     * @param flvData parseGetFlv()の戻り値
     * @param onMessageFunc コメントが来たときに来る高階関数。コメントの中身（JSON形式に変換された文字列が来ます。）
     * */
    fun connectionCommentServer(flvData: getFlvData, onMessageFunc: (String, String, Boolean) -> Unit) {
        // TCP？Socket通信
        socket = Socket()
        socket.connect(InetSocketAddress(flvData.messageServer, flvData.messageServerPort.toInt()))
        // XML送信
        val outputStream = socket.getOutputStream()
        outputStream?.write("<thread thread=\"${flvData.threadId}\" version=\"20061206\" res_from=\"-100\" scores=\"1\" />\u0000".toByteArray())
        outputStream?.flush()
        bufferedReader = socket.getInputStream().bufferedReader()
        // コメント受け取り
        var message = ""
        var c = 0
        // 画面回転すると落ちるので例外処理
        try {
            // whileの条件式何やってんのかよくわからん。
            while (!socket.isClosed && bufferedReader.read().also { c = it } != -1) {
                // charが流れてくるので。readTextとかは使えないの？
                if (c == 0) {
                    // 文字終了
                    when {
                        message.contains("chat_result") -> {
                        }
                        message.contains("ticket") -> {
                            // ticket流れてきたら
                            val regex = "ticket=\"(.+?)\""
                            val pattern = Pattern.compile(regex)
                            val matcher = pattern.matcher(message)
                            if (matcher.find()) {
                                ticket = matcher.group(1) ?: ""
                            }
                        }
                        message.contains("chat") -> {
                            // chatオブジェクトなら
                            onMessageFunc(xmlToJSON(message).toString(), "JK", false)
                        }
                    }
                    message = ""
                } else {
                    // 文字足していく
                    message += (c.toChar())
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
    }

    // XMLをJSONに変換するやつ。
    fun xmlToJSON(xml: String): JSONObject? {
        val jsoup = Jsoup.parse(xml)
        val thread = jsoup.getElementsByTag("chat").attr("thread")
        val no = jsoup.getElementsByTag("chat").attr("no")
        val vpos = jsoup.getElementsByTag("chat").attr("vpos")
        val date = jsoup.getElementsByTag("chat").attr("date")
        val date_usec = jsoup.getElementsByTag("chat").attr("date_usec")
        val userId = jsoup.getElementsByTag("chat").attr("user_id")
        val anonymcommenty = jsoup.getElementsByTag("chat").attr("anonymcommenty")
        val score = if (jsoup.getElementsByTag("chat").attr("score") != null) {
            jsoup.getElementsByTag("chat").attr("score")
        } else {
            ""
        }
        val mail = jsoup.getElementsByTag("chat").attr("mail")
        val origin = jsoup.getElementsByTag("chat").attr("origin")
        val premium = jsoup.getElementsByTag("chat").attr("premium")
        val content = jsoup.getElementsByTag("chat").text()
        // JSONのchatオブジェクト作成
        val chatObject = JSONObject().apply {
            put("thread", thread)
            put("no", no)
            put("vpos", vpos)
            put("leaf", 1)
            put("date", date)
            put("date_usec", date_usec)
            put("anonymcommenty", anonymcommenty)
            put("user_id", userId)
            put("mail", mail)
            put("origin", origin)
            put("content", content)
            if (score.isNotEmpty()) {
                put("score", score)
            }
            if (premium.isNotEmpty()) {
                put("premium", premium)
            }
        }
        return JSONObject().put("chat", chatObject)
    }

    /**
     * 終了時に呼んでね。
     * */
    fun destroy() {
        if (::socket.isInitialized) {
            socket.close()
        }
    }

    /**
     * PostKeyを取得する関数。postComment()関数の中で使っているのでこの関数を単体で使うことはないと思う。
     * @param threadId getFlvで取得できる値。
     * @param userSession ユーザーセッション
     * */
    private fun getPostKey(threadId: String, userSession: String): Deferred<Response> =
        GlobalScope.async {
            val request = Request.Builder().apply {
                url("http://jk.nicovideo.jp/api/v2/getpostkey?thread=$threadId")
                header("User-Agent", "TatimiDroid;@takusan_23")
                header("Cookie", "user_session=$userSession")
                get()
            }.build()
            val okHttpClient = OkHttpClient()
            val response = okHttpClient.newCall(request).execute()
            return@async response
        }

    /**
     * コメントを投稿する関数。
     * */
    fun postCommnet(comment: String, userId: String, baseTime: Long, threadId: String, userSession: String) {
        GlobalScope.launch {
            val postKeyResponse = getPostKey(threadId, userSession).await()
            if (!postKeyResponse.isSuccessful) {
                return@launch
            }
            // postKey
            val postKey = postKeyResponse.body?.string()?.replace("postkey=", "")
            //  100=1秒らしい。 例：300->3秒
            val unixTime = System.currentTimeMillis() / 1000L
            val vpos = (unixTime - baseTime) * 100
            val post =
                "<chat thread=\"${threadId}\" ticket=\"${ticket}\"  vpos=\"${vpos}\" postkey=\"${postKey}\" mail=\"  184\" user_id=\"${userId}\" premium=\"1\">${comment}</chat>\u0000"
            // XML送信
            val outputStream = socket.getOutputStream()
            outputStream.write(post.toByteArray())
            outputStream.flush()
        }
    }

    // getFlvからコメントサーバーに接続するのに必要な値だけ
    // channelNameはパーセントエンコーディングされてます
    data class getFlvData(val threadId: String, val messageServer: String, val messageServerPort: String, val baseTime: String, val userId: String, val channelName: String, val isPremium: Int)

}