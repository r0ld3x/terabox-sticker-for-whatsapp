package com.r0ld3x.telegramsticker.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.r0ld3x.telegramsticker.R
import com.r0ld3x.telegramsticker.types.Sticker


class ShowStickerPackAdapter(private val stickerList: List<Sticker>) : RecyclerView.Adapter<ShowStickerPackAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = stickerList[position]
        holder.textView.text = data.emoji
            Glide.with(holder.itemView.context)
                .load(data.thumbnail.thumbnailUri)
                .into(holder.imageView)

    }



    override fun getItemCount(): Int {
        return stickerList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
