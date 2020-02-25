package io.github.takusan23.tatimidroid.Fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.tatimidroid.Adapter.TagRecyclerViewAdapter
import io.github.takusan23.tatimidroid.R
import kotlinx.android.synthetic.main.activity_comment.*
import kotlinx.android.synthetic.main.bottom_fragment_tags.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class NicoLiveTagBottomFragment : BottomSheetDialogFragment() {

    val recyclerViewList = arrayListOf<ArrayList<String>>()
    lateinit var tagRecyclerViewAdapter: TagRecyclerViewAdapter

    lateinit var pref_setting: SharedPreferences

    var user_session = ""
    var liveId = ""
    var tagToken = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(context)
        user_session = pref_setting.getString("user_session", "") ?: ""

        bottom_fragment_tag_recyclerview.apply {
            setHasFixedSize(true)
            tagRecyclerViewAdapter = TagRecyclerViewAdapter(recyclerViewList)
            adapter = tagRecyclerViewAdapter
        }

        // LiveID
        liveId = arguments?.getString("liveId", "") ?: ""
        // タグ編集に使うトークン
        tagToken = arguments?.getString("tagToken", "") ?: ""

        bottom_fragment_tag_add_button.setOnClickListener {
            //番組情報
            addTag(tagToken, bottom_fragment_tag_edittext.text.toString())
        }

    }

    /**
     * タグを追加するAPI。
     * @param token HTMLの中のJSON
     * @param tagName タグの名前。
     * */
    fun addTag(token: String, tagName: String) {
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

}