package com.r0ld3x.telegramsticker

import kotlinx.coroutines.runBlocking
import android.util.Log
import com.google.gson.Gson
import com.r0ld3x.telegramsticker.services.OkHttpClientSingleton
import com.r0ld3x.telegramsticker.types.GetFile
import com.r0ld3x.telegramsticker.types.GetFileStickerResponse
import com.r0ld3x.telegramsticker.types.StickerResponse
import com.r0ld3x.telegramsticker.types.StickerSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request


class TelegramHelper private constructor() {

    companion object {
        const val BOT_TOKEN = "5655509332:AAGWLCm-lCDHeRJE-wRA2-cPEz9xH5v5HSA"
        private val client: OkHttpClient = OkHttpClientSingleton.client

        @Volatile
        private var INSTANCE: TelegramHelper? = null

        fun getInstance(): TelegramHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = TelegramHelper()
                INSTANCE = instance
                instance
            }
        }
    }


    suspend fun getStickerSet(stickerName: String): StickerSet? {
        val url = "https://api.telegram.org/bot$BOT_TOKEN/getStickerSet?name=${stickerName}"
        val request = Request.Builder()
            .url(url)
            .build()

        return withContext(Dispatchers.IO) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val responseJson = Gson().fromJson(responseBody, StickerResponse::class.java)
                    responseJson.result
                } else {
                    println("Unsuccessful response: ${response.code}, ${response.body?.string()}")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

        }


    }

    suspend fun getFile(fileId:String): GetFile? {

        return withContext(Dispatchers.IO){
            val url = "https://api.telegram.org/bot$BOT_TOKEN/getFile?file_id=$fileId"
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                if(response.isSuccessful){
                    val responseBody = response.body?.string() ?: ""
                    val responseJson = Gson().fromJson(responseBody, GetFileStickerResponse::class.java)
//                    responseJson.result.filePath = getFileNameFromPath(responseJson.result.filePath)
                    return@withContext responseJson.result
                }else{
                    Log.i("DEBUG", "Unsuccessful response: ${response.code}, ${response.body?.string()}")
                    null
                }
            }catch (e:Exception){
                e.printStackTrace()
                null
            }
        }
    }


//    suspend fun getFileDownThumbUri(stickerSet: StickerSet ): GetFileStickerSet? {
//        val stickers = stickerSet.stickers
//
//        stickers.forEach { sticker ->
//            val downLoadfile = getFile(sticker.fileId) ?: return@forEach
//            val thumbnaiFile = getFile(sticker.thumbnail.fileId)  ?: return@forEach
//            println("File response for ${sticker.fileId}: ${downLoadfile.filePath}")
//            println("File response for ${sticker.thumbnail.fileId}: ${thumbnaiFile.filePath}")
//        }
//        return null
//    }

    private fun getFileNameFromPath(path: String): String {
        return path.substringAfterLast("/", path)
    }



}


