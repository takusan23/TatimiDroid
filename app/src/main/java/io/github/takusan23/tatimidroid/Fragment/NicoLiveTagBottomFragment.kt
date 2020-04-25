package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Adapter.TagRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.bottom_fragment_tags.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class NicoLiveTagBottomFragment : BottomSheetDialogFragment() {

    lateinit var programFragment: ProgramInfoFragment
    // RecyclerView
    val recyclerViewList = arrayListOf<ArrayList<String>>()
    lateinit var tagRecyclerViewAdapter: TagRecyclerViewAdapter

    lateinit var pref_setting: SharedPreferences

    var user_session = ""
    var liveId = ""
    var tagToken = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_setting.getString("user_session", "") ?: ""

        // LiveID
        liveId = arguments?.getString("liveId", "") ?: ""
        // タグ編集に使うトークン
        tagToken = arguments?.getString("tagToken", "") ?: ""

        bottom_fragment_tag_recyclerview.apply {
            setHasFixedSize(true)
            val mLayoutManager = LinearLayoutManager(context)
            layoutManager = mLayoutManager
            tagRecyclerViewAdapter = TagRecyclerViewAdapter(recyclerViewList)
            adapter = tagRecyclerViewAdapter
        }

        tagRecyclerViewAdapter.apply {
            this.tagToken = this@NicoLiveTagBottomFragment.tagToken
            this.user_session = this@NicoLiveTagBottomFragment.user_session
            this.liveId = this@NicoLiveTagBottomFragment.liveId
            this.bottomFragment = this@NicoLiveTagBottomFragment
        }

        bottom_fragment_tag_add_button.setOnClickListener {
            //番組情報
            addTag(tagToken, bottom_fragment_tag_edittext.text.toString()) {
                // 追加できたらタグ再取得
                tagCoroutine()
                programFragment.apply {
                    // 番組情報Fragmentのタグも取得（更新）
                    coroutineGetTag()
                }
            }
        }

        tagCoroutine()

    }

    // タグAPI叩く。メインスレッド指定
    fun tagCoroutine() {
        GlobalScope.launch(Dispatchers.Main) {
            // APIアクセス
            val response = getTagAPI().await()
            // タグパース
            parseTagList(response)
            // タグ件数
            parseTagSize(response)
        }
    }

    fun parseTagSize(json: String?) {
        val jsonObject = JSONObject(json)
        val data = jsonObject.getJSONObject("data")
        val userTagCount = data.getInt("userTagCount")
        val userTagMax = data.getInt("userTagMax")
        bottom_fragment_tag_textview.text = "タグ件数：$userTagCount/$userTagMax"
    }

    /**
     * タグを追加するAPI。
     * @param token HTMLの中のJSON
     * @param tagName タグの名前。
     * @param success 成功したら
     * */
    fun addTag(token: String, tagName: String, success: () -> Unit) {
        val sendData = FormBody.Builder().apply {
            add("tag", tagName)
            add("token", token)
        }.build()
        val request = Request.Builder().apply {
            url("https://papi.live.nicovideo.jp/api/relive/livetag/$liveId/?_method=PUT")
            header("Cookie", "user_session=$user_session")
            header("User-Agent", "TatimiDroid;@takusan_23")
            post(sendData)
        }.build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(context, getString(R.string.error), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "追加しました。$tagName", Toast.LENGTH_SHORT).show()
                        success()
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            context,
                            "${getString(R.string.error)}\n${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    // タグのJSON配列をパース
    fun parseTagList(json: String?) {
        val jsonObject = JSONObject(json)
        val tags = jsonObject.getJSONObject("data").getJSONArray("tags")
        for (i in 0 until tags.length()) {
            val tagObject = tags.getJSONObject(i)
            val tagName = tagObject.getString("tag")
            val isLocked = tagObject.getBoolean("isLocked")
            // RecyclerViewに追加
            val item = arrayListOf<String>().apply {
                add("")
                add(tagName)
                add(isLocked.toString())
            }
            recyclerViewList.add(item)
        }
        activity?.runOnUiThread {
            tagRecyclerViewAdapter.notifyDataSetChanged()
        }
    }

    /** タグのAPIリクエスト。こるーちん */
    fun getTagAPI(): Deferred<String?> = GlobalScope.async {
        recyclerViewList.clear()
        val request = Request.Builder().apply {
            url("https://papi.live.nicovideo.jp/api/relive/livetag/$liveId")
            header("Cookie", "user_session=$user_session")
            header("User-Agent", "TatimiDroid;@takusan_23")
            get()
        }.build()
        val okHttpClient = OkHttpClient()
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            return@async response.body?.string()
        } else {
            return@async ""
        }
    }

}