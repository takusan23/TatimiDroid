package io.github.takusan23.tatimidroid.homewidget

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.text.HtmlCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import io.github.takusan23.tatimidroid.nicoapi.nicorepo.NicoRepoAPIX
import io.github.takusan23.tatimidroid.nicoapi.nicorepo.NicoRepoDataClass
import io.github.takusan23.tatimidroid.R
import io.github.takusan23.tatimidroid.tool.isDarkMode
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

/**
 * ホーム画面に置くウイジェットでListViewを使うときに使う
 * */
class NicoRepoHomeWidgetRemoteViewService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent?): RemoteViewsFactory {

        val context = applicationContext
        val prefSetting = PreferenceManager.getDefaultSharedPreferences(context)

        /** ユーザーセッション */
        val userSession = prefSetting.getString("user_session", "")!!

        /** ニコレポデータ */
        val nicoRepoList = arrayListOf<NicoRepoHomeWidgetData>()

        return object : RemoteViewsFactory {
            override fun onCreate() {

            }

            /** データを取得する処理はここ */
            override fun onDataSetChanged() {
                // println("widget データ取得")
                // ドキュメントにも同期処理していいよって書いてあったので遠慮なく
                runBlocking {
                    nicoRepoList.clear()
                    val nicoRepoAPIX = NicoRepoAPIX()
                    val response = nicoRepoAPIX.getNicoRepoResponse(userSession, null)
                    if (response.isSuccessful) {
                        nicoRepoAPIX.parseNicoRepoResponse(response.body?.string()).forEach { data ->
                            // Glide。同期処理
                            val iconAsync = async { Glide.with(context).asBitmap().load(data.userIcon).transform(RoundedCorners(20)).submit().get() }
                            val thumbAsync = async { Glide.with(context).asBitmap().load(data.thumbUrl).submit().get() }
                            val iconBitmap = iconAsync.await()
                            val thumbBitmap = thumbAsync.await()
                            nicoRepoList.add(NicoRepoHomeWidgetData(data, iconBitmap, thumbBitmap))
                        }
                    }
                }
            }

            override fun onDestroy() {
                nicoRepoList.clear()
            }

            /** リスト数を返す */
            override fun getCount() = nicoRepoList.size

            /** リストの個々のビューを返す */
            override fun getViewAt(position: Int): RemoteViews {
                val nicoRepoData = nicoRepoList[position]
                return RemoteViews(applicationContext.packageName, R.layout.home_widget_nicorepo_item).apply {
                    // 文字入れ
                    setTextViewText(R.id.home_widget_nicorepo_item_name_text_view, nicoRepoData.data.userName)
                    setTextViewText(R.id.home_widget_nicorepo_item_description_text_view, HtmlCompat.fromHtml(nicoRepoData.data.message, HtmlCompat.FROM_HTML_MODE_COMPACT))
                    setTextViewText(R.id.home_widget_nicorepo_item_title_text_view, nicoRepoData.data.title)
                    // 画像
                    setImageViewBitmap(R.id.home_widget_nicorepo_item_avater_image_view, nicoRepoData.iconBitmap)
                    setImageViewBitmap(R.id.home_widget_nicorepo_item_thumb_image_view, nicoRepoData.thumbBitmap)
                    // 押したとき
                    val mainActivityIntent = Intent().apply {
                        if (nicoRepoData.data.isVideo) {
                            putExtra("videoId", nicoRepoData.data.contentId)
                        } else {
                            putExtra("liveId", nicoRepoData.data.contentId)
                        }
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    setOnClickFillInIntent(R.id.home_widget_nicorepo_item_root, mainActivityIntent)
                    val color = if (isDarkMode(context)) Color.BLACK else Color.WHITE
                    if (isDarkMode(context)) {
                        // ダークモード
                        setTextColor(R.id.home_widget_nicorepo_item_name_text_view, Color.WHITE)
                        setTextColor(R.id.home_widget_nicorepo_item_description_text_view, Color.WHITE)
                        setTextColor(R.id.home_widget_nicorepo_item_title_text_view, Color.WHITE)
                    } else {
                        // らいとてーま
                        val defaultTextViewColor = -1979711488
                        setTextColor(R.id.home_widget_nicorepo_item_name_text_view, defaultTextViewColor)
                        setTextColor(R.id.home_widget_nicorepo_item_description_text_view, defaultTextViewColor)
                        setTextColor(R.id.home_widget_nicorepo_item_title_text_view, defaultTextViewColor)
                    }
                    setInt(R.id.home_widget_nicorepo_item_root, "setBackgroundColor", color)
                }
            }

            override fun getLoadingView(): RemoteViews? {
                return null
            }

            override fun getViewTypeCount(): Int {
                return 1
            }

            override fun getItemId(position: Int): Long {
                return position.toLong()
            }

            override fun hasStableIds(): Boolean {
                return true
            }
        }
    }

    /**
     * ウイジェットのListView表示で渡すデータ
     *
     * @param data ニコレポのデータ
     * @param iconBitmap ユーザーアイコンのBitmap
     * @param thumbBitmap サムネのBitmap
     * */
    private data class NicoRepoHomeWidgetData(
        val data: NicoRepoDataClass,
        val iconBitmap: Bitmap,
        val thumbBitmap: Bitmap,
    )

}