package io.github.takusan23.tatimidroid.JK

import java.io.BufferedReader
import java.io.IOException
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket

class TCPCommentServer {
    private var socket: Socket? = null
    private var bufferedReader: BufferedReader? = null
    private var printWriter: PrintWriter? = null
    fun connection(addr: String?, port: Int, thread: String) {
        try {
            socket = Socket()
            socket!!.connect(InetSocketAddress(addr, port))
            // XML送信
            val outputStream = socket?.getOutputStream()
            outputStream?.write("<thread thread=\"$thread\" version=\"20061206\" res_from=\"-0\" scores=\"1\" />\u0000".toByteArray())
            outputStream?.flush()
            bufferedReader = socket?.getInputStream()?.bufferedReader()
            try {
                // コメント受け取り
                var message = ""
                var c: Int
                while (bufferedReader!!.read().also { c = it } != -1) {
                    // charが流れてくるので。readTextとかは使えないの？
                    if (c == 0) {
                        // 文字終了
                        println(message)
                        message = ""
                    } else {
                        // 文字足していく
                        message += (c.toChar())
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}