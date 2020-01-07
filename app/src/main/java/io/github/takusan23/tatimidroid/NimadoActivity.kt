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
import androidx.core.view.forEach
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import io.github.takusan23.tatimidroid.Fragment.CommentFragment
import io.github.takusan23.tatimidroid.Fragment.NimadoLiveIDBottomFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_nimado.*
import kotlinx.android.synthetic.main.fragment_gift_layout.*
import okhttp3.*
import okhttp3.Callback
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.text.FieldPosition


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
    //番組の名前
    var programNameList = arrayListOf<String>()
    //公式番組かどうか
    var officialList = arrayListOf<String>()

    var fragmentList = arrayListOf<Fragment>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //ダークモード
        darkModeSupport = DarkModeSupport(this)
        darkModeSupport.setNimadoActivityTheme(this)

        setContentView(R.layout.activity_nimado)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        // println(savedInstanceState == null)


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
    /*
    * アプリから離れたらFragment終了/Fragmentを置いたViewを消す/RecyclerViewのItemを消す
    * */
    override fun onPause() {
        super.onPause()
        //値をonCreateの引数「savedInstanceState」に値を入れる
        intent.putStringArrayListExtra("program_list", programList)
        intent.putStringArrayListExtra("watch_mode_list", watchModeList)
        intent.putStringArrayListExtra("program_name", programNameList)
        intent.putStringArrayListExtra("official_list", officialList)
        intent.putExtra("fragment_list", fragmentList)
        nimado_activity_linearlayout.removeAllViews()
        recyclerViewList.clear()
        fragmentList.forEach {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    /*
    * アプリへ戻ってきたらFragmentを再設置する
    * 戻ってきた以外で画面回転時もここを --通過-- する
    * */
    override fun onResume() {
        super.onResume()
        if (intent.getStringArrayListExtra("program_list") != null) {
            programList = intent.getStringArrayListExtra("program_list")
            programNameList = intent.getStringArrayListExtra("program_name")
            watchModeList = intent.getStringArrayListExtra("watch_mode_list")
            officialList = intent.getStringArrayListExtra("official_list")

            //復活させる
            for (index in 0 until programList.size) {
                val liveID = programList[index]
                val watchMode = watchModeList[index]
                val isOfficial = officialList[index].toBoolean()
                addNimado(liveID, watchMode, isOfficial, true)
            }
        }
    }


    fun addNimado(
        liveId: String,
        watchMode: String,
        isOfficial: Boolean,
        isResume: Boolean = false
    ) {
        //番組ID
        //二窓中の番組IDを入れる配列
        if (!programList.contains(liveId)) {
            programList.add(liveId)
            watchModeList.add(watchMode)
            officialList.add(isOfficial.toString())
        }
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
        bundle.putBoolean("isOfficial", isOfficial)
        commentFragment.arguments = bundle
        val trans = supportFragmentManager.beginTransaction()
        trans.replace(linearLayout.id, commentFragment, liveId)
        trans.commit()

        fragmentList.add(commentFragment)

        //RecyclerViewへアイテム追加
        //onResumeから来たときはAPIを叩かない（非同期処理は難しすぎる）
        if (isResume) {
            val pos = programList.indexOf(liveId)
            //RecyclerViewついか
            val item = arrayListOf<String>()
            item.add("")
            item.add(programNameList[pos])
            item.add(liveId)
            //非同期処理なので順番を合わせる
            recyclerViewList.add(item)
            runOnUiThread {
                nimadoListRecyclerViewAdapter.notifyDataSetChanged()
            }
        } else {
            addRecyclerViewItem(liveId)
        }

    }

    fun addRecyclerViewItem(liveId: String) {
        val user_session = pref_setting.getString("user_session", "") ?: ""
        //API叩いてタイトルを取得する
        //適当にAPI叩いて認証情報エラーだったら再ログインする
        val request = Request.Builder()
            .url("https://live.nicovideo.jp/api/getplayerstatus?v=${liveId}")
            .header("Cookie", "user_session=${user_session}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //？
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                if (response.isSuccessful) {
                    val xml = Jsoup.parse(responseString)
                    val title = xml.getElementsByTag("title")[0].text()
                    //RecyclerViewついか
                    val item = arrayListOf<String>()
                    item.add("")
                    item.add(title)
                    item.add(liveId)
                    //非同期処理なので順番を合わせる
                    recyclerViewList.add(item)
                    programNameList.add(title)
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

        //ドラッグできるようにする
        val itemTouchHelper =
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val adapter = recyclerView.adapter as NimadoListRecyclerViewAdapter
                    val old = viewHolder.adapterPosition
                    val new = target.adapterPosition

                    //移動させる
                    adapter.notifyItemMoved(old, new)

                    //配列の値も入れ替える
                    //入れ替えて再設置する
                    val liveID = programList[old]
                    val watchMode = watchModeList[old]
                    val isOfficial = officialList[old]
                    programList.removeAt(old)
                    watchModeList.removeAt(old)
                    officialList.removeAt(old)
                    programList.add(new, liveID)
                    watchModeList.add(new, watchMode)
                    officialList.add(new, isOfficial)
                    //Fragmentが入るView再設置
                    //全部消すのでは無く移動するところだけ消す
                    val cardView = (nimado_activity_linearlayout[old] as CardView)
                    val fragment = supportFragmentManager.findFragmentByTag(liveID)
                    if (fragment != null) {
                        supportFragmentManager.beginTransaction().remove(fragment).commit()
                    }
                    nimado_activity_linearlayout.removeView(cardView)
                    nimado_activity_linearlayout.addView(cardView, new)
                    //Fragment再設置
                    val commentFragment = CommentFragment()
                    val bundle = Bundle()
                    bundle.putString("liveId", liveID)
                    bundle.putString("watch_mode", watchMode)
                    commentFragment.arguments = bundle
                    val trans = supportFragmentManager.beginTransaction()
                    //cardViewの0番目のViewがFragmentを入れるViewなので
                    trans.replace(cardView[0].id, commentFragment, liveID)
                    trans.commit()


                    //これだけ！RecyclerViewに圧倒的感謝だな！！！！！！！
                    return true
                }

                override fun onSelectedChanged(
                    viewHolder: RecyclerView.ViewHolder?,
                    actionState: Int
                ) {
                    super.onSelectedChanged(viewHolder, actionState)
                    if (actionState == ACTION_STATE_DRAG) {
                        //ドラッグ中はItemを半透明にする
                        viewHolder?.itemView?.alpha = 0.5f
                    }
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {
                    super.clearView(recyclerView, viewHolder)
                    //半透明しゅーりょー
                    viewHolder.itemView.alpha = 1f
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    //スワイプしないのでいらない
                }
            })
        itemTouchHelper.attachToRecyclerView(nimado_activity_list_recyclerview)
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
