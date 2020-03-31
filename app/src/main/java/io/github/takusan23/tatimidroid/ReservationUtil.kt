package io.github.takusan23.tatimidroid

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import io.github.takusan23.tatimidroid.NicoAPI.NicoLiveTimeShiftAPI
import io.github.takusan23.tatimidroid.NicoAPI.ProgramData
import io.github.takusan23.tatimidroid.SQLiteHelper.AutoAdmissionDBUtil

/**
 * 予約枠で使える関数たち
 * */
class ReservationUtil(context: Context?) {

    // 予約枠自動入場で使う関数たちがあるクラス
    var admissionDBUtil = AutoAdmissionDBUtil(context!!)

    /**
     * カレンダーに予定を追加する
     * https://developer.android.com/guide/components/intents-common?hl=ja#AddEvent
     * */
    fun addCalendar(context: Context?, programData: ProgramData) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, programData.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, programData.beginAt.toLong())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, programData.endAt.toLong())
        }
        context?.startActivity(intent)
    }

    // 共有画面出す
    fun showShareScreen(context: Context?, view: View?, programData: ProgramData) {
        if (view == null) {
            return
        }
        val programShare =
            ProgramShare(context as AppCompatActivity, view, programData.title, programData.programId)
        programShare.showShareScreen()
    }

    // TS登録
    fun registerTimeShift(context: Context?, programData: ProgramData, view: View) {
        val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)
        val user_session = prefSetting.getString("user_session", "")
        if (user_session != null) {
            val nicoLiveTimeShiftAPI =
                NicoLiveTimeShiftAPI(context, user_session, programData.programId)
            // API叩く
            nicoLiveTimeShiftAPI.registerTimeShift {
                if (it.isSuccessful) {
                    // 成功時。登録成功
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, R.string.timeshift_reservation_successful, Toast.LENGTH_SHORT)
                            .show()
                    }
                } else if (it.code == 500) {
                    // 失敗時。(500エラー)すでに登録済み
                    Handler(Looper.getMainLooper()).post {
                        // 削除するか？Snackbar出す
                        Snackbar.make(view, R.string.timeshift_reserved, Snackbar.LENGTH_SHORT)
                            .setAction(R.string.timeshift_delete_reservation_button) {
                                // TS取り消しAPI叩く
                                nicoLiveTimeShiftAPI.deleteTimeShift {
                                    Handler(Looper.getMainLooper()).post {
                                        // 削除成功
                                        Toast.makeText(context, R.string.timeshift_delete_reservation_successful, Toast.LENGTH_LONG)
                                            .show()
                                    }
                                }
                            }.show()
                    }
                }
            }
        }
    }
}