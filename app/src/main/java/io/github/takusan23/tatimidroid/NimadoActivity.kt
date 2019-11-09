package io.github.takusan23.tatimidroid

import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
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
import androidx.cardview.widget.CardView
import androidx.core.view.marginBottom
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.takusan23.tatimidroid.Fragment.CommentFragment
import io.github.takusan23.tatimidroid.Fragment.NimadoLiveIDBottomFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_nimado.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


/*
* にまど！！！！
* */
class NimadoActivity : AppCompatActivity() {

    lateinit var pref_setting: SharedPreferences

    lateinit var darkModeSupport: DarkModeSupport

    var recyclerViewList: ArrayList<ArrayList<*>> = arrayListOf()
    lateinit var nimadoListRecyclerViewAdapter: NimadoListRecyclerViewAdapter
    lateinit var recyclerViewLayoutManager: RecyclerView.LayoutManager

    //番組IDの配列
    var programList = ArrayList<String>()
    //視聴モードの配列
    var watchModeList = ArrayList<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ダークモード
        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setNimadoActivityTheme(this)

        setContentView(R.layout.activity_nimado)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        if (savedInstanceState != null) {
            programList = savedInstanceState.getStringArrayList("program_list") as ArrayList<String>
            watchModeList =
                savedInstanceState.getStringArrayList("watch_mode_list") as ArrayList<String>

            //復活させる
            for (index in 0 until programList.size) {
                val liveID = programList[index]
                val watchMode = watchModeList[index]
                addNimado(liveID, watchMode)
            }
        }

        //自作Toolbarを適用させる
        setSupportActionBar(nimado_activity_toolbar)

        //ステータスバーを透過する
        //window.statusBarColor = Color.TRANSPARENT

        //ダークモード対応
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            supportActionBar?.setBackgroundDrawable(ColorDrawable(darkModeSupport.getThemeColor()))
        }

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
            val nimadoLiveIDBottomFragment = NimadoLiveIDBottomFragment()
            nimadoLiveIDBottomFragment.show(supportFragmentManager, "nimado_liveid")
        }

    }

    /*
    * 画面回転に耐えるアプリを作る。
    * ここで画面開店前にやりたいことを書く
    * */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //値をonCreateの引数「savedInstanceState」に値を入れる
        outState.putStringArrayList("program_list", programList)
        outState.putStringArrayList("watch_mode_list", watchModeList)
    }


    fun addNimado(liveId: String, watchMode: String) {
        //番組ID
        //二窓中の番組IDを入れる配列
        programList.add(liveId)
        watchModeList.add(watchMode)
        //動的にView作成
        val disp = windowManager.defaultDisplay
        val size = Point()
        disp.getSize(size)
        val screenWidth = size.x
        val screenHeight = size.y

        //区切りがあれなのでCardViewの上にLinearLayoutを乗せる
        val cardView = CardView(this)
        val linearLayout = LinearLayout(this)

        val layoutParams = LinearLayout.LayoutParams(
            screenWidth / 2,    //半分の大きさにする
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        linearLayout.layoutParams = layoutParams

        //CardViewの丸みとViewの感覚
        layoutParams.setMargins(2, 2, 2, 2)
        cardView.layoutParams = layoutParams
        cardView.radius = 20f

        linearLayout.setPadding(20, 20, 20, 20)

        linearLayout.orientation = LinearLayout.VERTICAL

        linearLayout.id = View.generateViewId()

        //ScrollViewなLinearLayoutに入れる
        cardView.addView(linearLayout)
        //ダークモード時はCardView黒くする
        if (darkModeSupport.nightMode == Configuration.UI_MODE_NIGHT_YES) {
            cardView.backgroundTintList = ColorStateList.valueOf(Color.BLACK)
        }
        nimado_activity_linearlayout.addView(cardView)


        //Fragment設置
        val commentFragment = CommentFragment()
        val bundle = Bundle()
        bundle.putString("liveId", liveId)
        bundle.putString("watch_mode", watchMode)
        commentFragment.arguments = bundle
        val trans = supportFragmentManager.beginTransaction()
        trans.replace(linearLayout.id, commentFragment, liveId)
        trans.commit()

        //RecyclerViewへアイテム追加
        addRecyclerViewItem(liveId)
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
        recyclerViewLayoutManager = nimado_activity_list_recyclerview.layoutManager!!
        nimadoListRecyclerViewAdapter.linearLayout = nimado_activity_linearlayout
        nimadoListRecyclerViewAdapter.activity = this
        //区切り線いれる
        val itemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        nimado_activity_list_recyclerview.addItemDecoration(itemDecoration)
    }

    /*
    * ヘッダーに
    * */

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

    //戻るキーを押した時に本当に終わるか聞く
    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.back_dialog))
            .setMessage(getString(R.string.back_dialog_description))
            .setPositiveButton(getString(R.string.end)) { dialogInterface: DialogInterface, i: Int ->
                finish()
                super.onBackPressed()
            }
            .setNegativeButton(getString(android.R.string.cancel)) { dialogInterface: DialogInterface, i: Int ->
                dialogInterface.dismiss()
            }
            .show()
    }

}
