package io.github.takusan23.tatimidroid

import android.content.DialogInterface
import android.graphics.Point
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import io.github.takusan23.tatimidroid.Fragment.CommentFragment
import kotlinx.android.synthetic.main.activity_nimado.*


/*
* にまど！！！！
* */
class NimadoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nimado)

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

                    linearLayout.setPadding(20,20,20,20)

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
