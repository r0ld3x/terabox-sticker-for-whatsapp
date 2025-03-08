package com.r0ld3x.telegramsticker.adapters

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.r0ld3x.telegramsticker.R
import com.r0ld3x.telegramsticker.adapters.ShowStickerPackAdapter.ViewHolder
import com.r0ld3x.telegramsticker.dependency.BuildConfig
import com.r0ld3x.telegramsticker.types.StickerSet

class ShowStickerListAdapter(
    private val stickerList: List<StickerSet>,
    private val addStickersToWhatsApp: (
        identifier: String,
        name: String
    ) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<ShowStickerListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_stickers_design, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return stickerList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = stickerList[position]
        print(data)

        holder.stickerName.text = data.name
        val firstFourStickers = data.stickers.take(4)

        holder.imageView.let { Glide.with(holder.itemView.context).load(firstFourStickers.getOrNull(0)?.absolutePath).into(it) }
        holder.imageView2.let { Glide.with(holder.itemView.context).load(firstFourStickers.getOrNull(1)?.absolutePath).into(it) }
        holder.imageView3.let { Glide.with(holder.itemView.context).load(firstFourStickers.getOrNull(2)?.absolutePath).into(it) }
        holder.imageView4.let { Glide.with(holder.itemView.context).load(firstFourStickers.getOrNull(3)?.absolutePath).into(it) }

        holder.stickerFizeSize.text = data.stickers.size.toString()

        try {
            val uri = Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority("com.whatsapp.provider.sticker_whitelist_check")
                .appendPath("is_whitelisted")
                .appendQueryParameter("authority", BuildConfig.CONTENT_PROVIDER_AUTHORITY)
                .appendQueryParameter("identifier", data.name)
                .build()

            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    try {
                    val isWhitelisted = it.getInt(it.getColumnIndexOrThrow("result")) == 1
                    if(isWhitelisted){
                        Log.w("DEBUG", "Whitelist ${data.name} : $isWhitelisted")
                        Glide.with(holder.itemView.context)
                         .load(R.drawable.sticker_3rdparty_added)
                         .into(holder.addBtn)
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                    }

                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        holder.addBtn.setOnClickListener {
            addStickersToWhatsApp(data.name, data.title)
        }
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val addBtn: ImageView = itemView.findViewById(R.id.add_button_on_list)
        val stickerName: TextView = itemView.findViewById(R.id.sticker_pack_title)
        val stickerFizeSize: TextView = itemView.findViewById(R.id.sticker_pack_filesize)
        val imageView: ImageView = itemView.findViewById(R.id.imageView1)
        val imageView2: ImageView = itemView.findViewById(R.id.imageView2)
        val imageView3: ImageView = itemView.findViewById(R.id.imageView3)
        val imageView4: ImageView = itemView.findViewById(R.id.imageView4)


    }

}