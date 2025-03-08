package com.r0ld3x.telegramsticker

import com.r0ld3x.telegramsticker.types.Sticker
import com.r0ld3x.telegramsticker.types.StickerSet
import com.r0ld3x.telegramsticker.types.Thumbnail
import kotlin.math.ceil
import kotlin.random.Random

fun generateRandomSticker(): Sticker {
    val randomEmoji = listOf("😳", "😔", "😒", "🥺", "😢", "🙄", "😋", "😠").random()
    val randomWidth = 512
    val randomHeight = 512
    val randomFileId = "CAACAgUAAxUAAWemzCh${Random.nextInt(1000, 9999)}"
    val randomFileUniqueId = "AgAD${Random.nextInt(1000, 9999)}"
    val randomFileSize = Random.nextInt(10000, 50000)
    val randomThumbnailUri = "https://api.telegram.org/file/bot5655509332:AAGWLCm-lCDHeRJE-wRA2-cPEz9xH5v5HSA/thumbnails/file_${Random.nextInt(531, 600)}.webp"

    return Sticker(
        width = randomWidth,
        height = randomHeight,
        emoji = randomEmoji,
        thumbnail = Thumbnail(
            fileId = "AAMCBQADFQABZ6bMK${Random.nextInt(1000, 9999)}",
            fileUniqueId = "AQAD${Random.nextInt(1000, 9999)}",
            fileSize = Random.nextInt(5000, 20000),
            width = 320,
            height = 320,
            thumbnailUri = randomThumbnailUri
        ),
        isAnimated = false,
        isVideo = false,
        fileId = randomFileId,
        fileUniqueId = randomFileUniqueId,
        fileSize = randomFileSize,
        downloadUri = "TODO(),",
        absolutePath = "TODO()",
    )
}

fun main(){
    val stickerSet = StickerSet(
        name = "MeowAman",
        title = "Meow @aman_chabukswar",
        stickerType = "regular",
        stickers = mutableListOf<Sticker>().apply {
            repeat(35) {
                add(generateRandomSticker())
            }
        }
    )

    val noOfPacks = ceil(stickerSet.stickers.size / 30.0).toInt()
    val packs = stickerSet.stickers.chunked(30).mapIndexed { index, stickersChunk ->
        StickerSet(
            name = "${stickerSet.name}__${index + 1}",
            title = stickerSet.title,
            stickerType = stickerSet.stickerType,
            stickers = stickersChunk.toMutableList()
        )
    }

// Now `packs` contains the divided StickerSets
    packs.forEachIndexed { index, pack ->
        println("Pack ${index + 1}: ${pack.name} ${pack.title} stickers")
    }



}