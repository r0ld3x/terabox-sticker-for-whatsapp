package com.r0ld3x.telegramsticker.types

import com.google.gson.annotations.SerializedName

data class StickerResponse(
    val ok: Boolean,
    val result: StickerSet
)

data class StickerSet(
    val name: String,
    val title: String,
    @SerializedName("sticker_type") val stickerType: String,
    val stickers: List<Sticker>,
)

data class Sticker(
    val width: Int,
    val height: Int,
    val emoji: String,
    val thumbnail: Thumbnail,
    @SerializedName("is_animated") val isAnimated: Boolean,
    @SerializedName("is_video") val isVideo: Boolean,
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_size") val fileSize: Int,
    val downloadUri: String?,
    val absolutePath: String?,
)

data class Thumbnail(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_size") val fileSize: Int,
    val width: Int,
    val height: Int,
    var thumbnailUri: String?,
)

data class GetFileStickerResponse(
    val ok: Boolean,
    val result: GetFile
)

data class GetFile(
    @SerializedName("file_id") val fileId: String,
    @SerializedName("file_unique_id") val fileUniqueId: String,
    @SerializedName("file_size") val fileSize: Int,
    @SerializedName("file_path") val filePath: String
)