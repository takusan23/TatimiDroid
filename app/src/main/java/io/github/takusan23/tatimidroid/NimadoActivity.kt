package io.github.takusan23.tatimidroid

import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.takusan23.tatimidroid.Fragment.CommentFragment
import kotlinx.android.synthetic.main.activity_nimado.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


/*
* にまど！！！！
* */
class NimadoActivity : AppCompatActivity() {

    lateinit var pref_setting:SharedPreferences

    lateinit var darkModeSupport: DarkModeSupport

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var nimadoListRecyclerViewAdapter: NimadoListRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    //番組IDの配列
    val programList = arrayListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ダークモード
        darkModeSupport = DarkModeSupport(this)

        setContentView(R.layout.activity_nimado)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        //自作Toolbarを適用させる
        setSupportActionBar(nimado_activity_toolbar)

        //ステータスバーを透過する
        window.statusBarColor = Color.TRANSPARENT

        //ハンバーガーアイコンを実装
        // sync drawer
        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, drawer_layout, nimado_activity_toolbar, R.string.nimado, R.string.nimado
        )
        drawer_layout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()

        //RecyclerView初期化
        initRecyclerView()

        //追加ボタン
        nimado_activity_add_liveid_button.setOnClickListener {
            val editText = EditText(this)
            //ダイアログ
            AlertDialog.Builder(this)
                .setTitle("番組ID")
                .setView(editText)
                .setNegativeButton("キャンセル") { dialogInterface: DialogInterface, i: Int -> }
                .setPositiveButton("二窓開始") { dialogInterface: DialogInterface, i: Int ->
                    //番組ID
                    val liveId = editText.text.toString()

                    programList.add(liveId)

                    //動的にView作成
                    val disp = windowManager.defaultDisplay
                    val size = Point()
                    disp.getSize(size)
                    val screenWidth = size.x
                    val screenHeight = size.y

                    val linearLayout = LinearLayout(this)
                    val layoutParams = LinearLayout.LayoutParams(
                        screenWidth / 2,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    linearLayout.layoutParams = layoutParams

                    linearLayout.setPadding(20, 20, 20, 20)

                    linearLayout.orientation = LinearLayout.VERTICAL

                    linearLayout.id = View.generateViewId()
                    nimado_activity_linearlayout.addView(linearLayout)

                    linearLayout.addView(setCloseButton(liveId, linearLayout))

                    //Fragment設置
                    val commentFragment = CommentFragment()
                    val bundle = Bundle()
                    bundle.putString("liveId", liveId)
                    commentFragment.arguments = bundle
                    val trans = supportFragmentManager.beginTransaction()
                    trans.replace(linearLayout.id, commentFragment, liveId)
                    trans.commit()


                }
                .show()
        }
    }


    fun addRecyclerViewItem(liveId: String) {
        val user_session = pref_setting.getString("user_session", "") ?: ""
        //API叩いてタイトルを取得する
        //適当にAPI叩いて認証情報エラーだったら再ログインする
        val request = Request.Builder()
            .url("https://live2.nicovideo.jp/watch/${liveId}/programinfo")
            .header("Cookie", "user_session=${user_session}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //？
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    //そもそも番組が終わってる可能性があるのでチェック
                    val response_string = response.body?.string()
                    val jsonObject = JSONObject(response_string)
                    val data = jsonObject.getJSONObject("data")
                    val title = data.getString("title")
                    //RecyclerViewついか
                    val item = arrayListOf<String>()
                    item.add("")
                    item.add(title)
                    item.add(liveId)
                    recyclerViewList.add(item)
                    runOnUiThread {
                        nimadoListRecyclerViewAdapter.notifyDataSetChanged()
                    }
                }
            }
        })
    }

    private fun initRecyclerView() {
        nimado_activity_list_recyclerview.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        nimado_activity_list_recyclerview.layoutManager =
            mLayoutManager as RecyclerView.LayoutManager?
        nimadoListRecyclerViewAdapter = NimadoListRecyclerViewAdapter(recyclerViewList)
        nimado_activity_list_recyclerview.adapter = nimadoListRecyclerViewAdapter
        recyclerViewLayoutManager = gift_recyclerview.layoutManager!!
        nimadoListRecyclerViewAdapter.linearLayout = nimado_activity_linearlayout
        nimadoListRecyclerViewAdapter.activity = this
    }

    /*
    * 閉じるボタン
    * */
    fun setCloseButton(liveId: String, linearLayout: LinearLayout): Button {
        val button =
            MaterialButton(this)
        button.text = "閉じる"
        button.setOnClickListener {
            val trans = supportFragmentManager.beginTransaction()
            val fragment = supportFragmentManager.findFragmentByTag(liveId)
            trans.remove(fragment!!)
            trans.commit()
            //けす
            (linearLayout.parent as LinearLayout).removeView(linearLayout)
        }
        return button
    }

}
