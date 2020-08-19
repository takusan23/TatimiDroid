package io.github.takusan23.tatimidroid.NicoVideo.Adapter

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import io.github.takusan23.tatimidroid.NicoAPI.NicoVideo.DataClass.NicoVideoData
import io.github.takusan23.tatimidroid.NicoVideo.BottomFragment.NicoVideoPlayListBottomFragment
import io.github.takusan23.tatimidroid.NicoVideo.NicoVideoPlayListFragment
import io.github.takusan23.tatimidroid.R

/**
 * ニコ動連続再生で一覧表示する用のAdapter
 * */
class NicoVideoPlayListAdapter(val list: ArrayList<NicoVideoData>) : RecyclerView.Adapter<NicoVideoPlayListAdapter.ViewHolder>() {

    /** 動画切り替えるのに使う */
    var nicoVideoPlayListFragment: NicoVideoPlayListFragment? = null

    /** BottomFragmentを操作するのに使う */
    var nicoVideoPlayListBottomFragment: NicoVideoPlayListBottomFragment? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.adapter_nicovideo_playlist, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            // 情報入れる
            val data = list[position]

            val context = titleTextView.context

            // タイトルなど
            titleTextView.text = data.title
            idTextView.text = data.videoId
            // サムネイル
            thumbImageView.imageTintList = null
            Glide.with(thumbImageView)
                .load(data.thum)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(10)))
                .into(thumbImageView)

            // 再生
            parentLinearLayout.setOnClickListener {
                // Fragment取得
                nicoVideoPlayListFragment?.setVideo(data.videoId, data.isCache)
                setPlayingItemBackgroundColor(data.videoId, parentLinearLayout)
                notifyDataSetChanged() // 一覧更新
            }

            setPlayingItemBackgroundColor(data.videoId, parentLinearLayout)

        }
    }

    /** 再生中の動画の背景を変えるなど */
    private fun setPlayingItemBackgroundColor(videoId: String, view: View) {
        // 現在再生中は色変える
        if (videoId == nicoVideoPlayListFragment?.currentVideoId) {
            view.background = ColorDrawable(Color.parseColor("#494949"))
        } else {
            view.background = ColorDrawable(Color.BLACK)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbImageView = itemView.findViewById<ImageView>(R.id.adapter_nicovideo_playlist_thum)
        val titleTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_playlist_title)
        val idTextView = itemView.findViewById<TextView>(R.id.adapter_nicovideo_playlist_id)
        val parentLinearLayout = itemView.findViewById<LinearLayout>(R.id.adapter_nicovideo_playlist_parent)
    }

}