package io.github.takusan23.tatimidroid.Fragment

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideoCache
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.Service.CommentGetService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DevSettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.dev_preference, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // コメントベンチマーク
        initCommentBenchmark()
    }

    private fun initCommentBenchmark() {
        val commentGetButton =
            preferenceManager.findPreference<Preference>("dev_setting_get_comment")
        val commentValue =
            preferenceManager.sharedPreferences.getString("dev_setting_get_comment_limit", "0")
                ?.toInt()
        val commentServiceFinishButton =
            preferenceManager.findPreference<Preference>("dev_setting_get_comment_service_finish")

        val intent = Intent(context, CommentGetService::class.java)

        // キャッシュの中から選ぶ
        val nicoVideoCache = NicoVideoCache(context)
        var idList = arrayListOf<String>()
        var titleList = arrayListOf<String>()
        runBlocking {
            // 読み込んでIDとタイトルの文字が入った配列に変換
            val cacheList = nicoVideoCache.loadCache().await()
            titleList = ArrayList(cacheList.map { nicoVideoData -> nicoVideoData.title })
            idList = ArrayList(cacheList.map { nicoVideoData -> nicoVideoData.videoId })
        }
        // だいあろぐ
        val buttons = arrayListOf<DialogBottomSheet.DialogBottomSheetItem>()
        titleList.forEach {
            val items = DialogBottomSheet.DialogBottomSheetItem(it)
            buttons.add(items)
        }
        // サービス起動
        commentGetButton?.setOnPreferenceClickListener {
            // 選択画面出す
            DialogBottomSheet("キャッシュ取得の中から表示してます", buttons) { i, bottomSheetDialogFragment ->
                intent.putExtra("videoId", idList[i])
                context?.stopService(intent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(intent)
                } else {
                    context?.startService(intent)
                }
            }.show(fragmentManager!!, "select")
            false
        }

        // サービス終了
        commentServiceFinishButton?.setOnPreferenceClickListener {
            context?.stopService(intent)
            false
        }

    }
}