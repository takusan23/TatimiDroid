package io.github.takusan23.tatimidroid.NicoVideo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoVideo.Adapter.NicoVideoSelectAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.fragment_nicovideo_content_tree.*
import kotlinx.android.synthetic.main.fragment_nicovideo_select.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.regex.Pattern

class NicoVideoContentTreeFragment : Fragment() {

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var nicoVideoSelectAdapter: NicoVideoSelectAdapter

    var id = "sm157"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nicovideo_content_tree, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        id = arguments?.getString("id") ?: "sm157"

        initRecyclerView()
        getParentContent()

    }

    private fun initRecyclerView() {
        fragment_nicovideo_content_tree_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(context)
        fragment_nicovideo_content_tree_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        nicoVideoSelectAdapter = NicoVideoSelectAdapter(recyclerViewList)
        fragment_nicovideo_content_tree_recyclerview.adapter = nicoVideoSelectAdapter
    }


    /*
    * 親作品を取得する。
    * JSONっぽいけど最初と最後にJSっぽいのがあるので取り除く
    * */
    fun getParentContent() {
        recyclerViewList.clear()
        val url = "https://api.commons.nicovideo.jp/tree/summary/get?id=$id&limit=1000"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast(getString(R.string.error))
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //正規表現諦めた。
                    var response_string = response.body?.string()
                    response_string =
                        response_string?.replace("callback(", "")?.replace("}]});", "}]}")

                    val jsonObject = JSONObject(response_string)
                    val parent = jsonObject.getJSONArray("parent")
                    for (i in 0 until parent.length()) {
                        val video = parent.getJSONObject(i)
                        val title = video.getString("title")
                        val videoId = video.getString("parentid")
                        val thumbnail = video.getString("thumbnail")
                        val registered_date = video.getString("registered_date")

                        val item = arrayListOf<String>().apply {
                            add("content_tree")//親作品だよー
                            add(videoId)
                            add(title)
                            add("")
                            add(registered_date)
                            add("")
                            add(thumbnail)
                            add("")
                            add("")
                            add("")
                        }
                        recyclerViewList.add(item)
                    }

                    activity?.runOnUiThread {
                        nicoVideoSelectAdapter.notifyDataSetChanged()
                        //Snackbar.make(
                        //    activity?.findViewById(android.R.id.content)!!,
                        //    "${getString(R.string.get_parent_count)}：${recyclerViewList.size}",
                        //    Snackbar.LENGTH_SHORT
                        //).show()
                    }


                } else {
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