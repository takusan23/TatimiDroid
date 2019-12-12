package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.github.takusan23.tatimidroid.R
import okhttp3.*
import java.io.IOException
import java.util.regex.Pattern

class NicoVideoContentTreeFragment :Fragment(){

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_content_tree,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }

    /*
    * 親作品を取得する。
    * JSONっぽいけど最初と最後にJSっぽいのがあるので取り除く
    * */
    fun getParentContent(){
        val url = ""
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object :Callback{
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.isSuccessful){
                    //正規表現で取り出す
                    val response_string = response.body?.string()
                    val pattern = Pattern.compile("callback(\\(.*?)\\)")
                    val matcher = pattern.matcher(response_string)
                    if(matcher.find()){
                        val json = matcher.group(1)
                    }
                }else{
                    showToast("${getString(R.string.error)}\n${response.code}")
                }
            }
        })
    }

    fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


}