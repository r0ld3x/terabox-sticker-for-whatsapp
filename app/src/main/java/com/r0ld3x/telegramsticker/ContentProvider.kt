package com.r0ld3x.telegramsticker



import InternalStorageManager
import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.MODE_READ_ONLY
import android.text.TextUtils
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.r0ld3x.telegramsticker.dependency.BuildConfig
import com.r0ld3x.telegramsticker.types.StickerSet
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random


class ContentProvider : ContentProvider() {
    companion object {
        private const val AUTHORITY = BuildConfig.CONTENT_PROVIDER_AUTHORITY
        private const val METADATA: String = "metadata"
        private const val STICKERS_ASSET = "stickers"
        private const val STICKER_FILE = "file"

        // Base URI to match the content provider's authority and metadata path
        val AUTHORITY_URI: Uri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
            .authority(BuildConfig.CONTENT_PROVIDER_AUTHORITY)
            .appendPath(METADATA).build()

        // Match codes for URI patterns
        private const val MATCH_PACKS = 1
        private const val MATCH_STICKERS = 2
        private const val MATCH_FILE = 3
        private val MATCHER: UriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "$METADATA/*", MATCH_PACKS)
            // Matches pattern for sticker pack assets (sticker folders)
            addURI(AUTHORITY, "$STICKERS_ASSET/*", MATCH_STICKERS)
            // Matches pattern for individual sticker file within the pack (file name)
            addURI(AUTHORITY, "$STICKERS_ASSET/*/$STICKER_FILE/*", MATCH_FILE)
        }

        // Constants for sticker pack metadata query parameters
        const val STICKER_PACK_IDENTIFIER_IN_QUERY: String = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY: String = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY: String = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY: String = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY: String = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK_IN_QUERY: String = "ios_app_download_link"
        const val PUBLISHER_EMAIL: String = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE: String = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE: String = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE: String = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION: String = "image_data_version"
        const val AVOID_CACHE: String = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK: String = "animated_sticker_pack"
        const val STICKER_FILE_NAME_IN_QUERY: String = "sticker_file_name"
        const val STICKER_FILE_EMOJI_IN_QUERY: String = "sticker_emoji"
        const val STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY: String = "sticker_accessibility_text"
    }

    private var stickerPacks: List<StickerSet>? = null

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(): Boolean {
        Log.w("DEBUG", "CALLED onCreate")
        this.stickerPacks = InternalStorageManager.getAllDataJsonFiles(this.requireContext())
        return this.stickerPacks != null
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        Log.w("DEBUG", "CALLED URI: $uri")

        val code = MATCHER.match(uri)
        Log.w("DEBUG", "Matched URI code: $code")

        return when (code) {
            MATCH_PACKS -> {
                val stickerPackName = uri.lastPathSegment ?: return null
                val stickerPack = this.stickerPacks?.find { it.name == stickerPackName }
                if(stickerPack == null) return null
                val data = getStickerPackInfo(uri,stickerPack )
                Log.i("DEBUG", cursorToArray(data).toString())
                data
            }
            MATCH_STICKERS -> {
                val stickerPackName = uri.lastPathSegment ?: return null
                val folder = InternalStorageManager.getStickerSetFromFile(
                    context = requireContext(),
                    stickerPackName
                ) ?: return null
                Log.i("DEBUG", "Calling getStickersForAStickerPack")
                return getStickersForAStickerPack(uri, folder)
            }
            else -> {
                Log.e("DEBUG", "Unknown URI: $uri")
                return null
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @NonNull
    private fun getStickerPackInfo(
        uri: Uri,
        stickerPack: StickerSet
    ): Cursor {

        val cursor = MatrixCursor(
            arrayOf(
                STICKER_PACK_IDENTIFIER_IN_QUERY,
                STICKER_PACK_NAME_IN_QUERY,
                STICKER_PACK_PUBLISHER_IN_QUERY,
                STICKER_PACK_ICON_IN_QUERY,
                ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
                IOS_APP_DOWNLOAD_LINK_IN_QUERY,
                PUBLISHER_EMAIL,
                PUBLISHER_WEBSITE,
                PRIVACY_POLICY_WEBSITE,
                LICENSE_AGREEMENT_WEBSITE,
                IMAGE_DATA_VERSION,
                AVOID_CACHE,
                ANIMATED_STICKER_PACK,
            )
        )
        val getFirstStickerFile = stickerPack.stickers[0].absolutePath?.substringAfterLast("/")
        val isAnimated = stickerPack.stickers.any { it.isVideo }
        val builder = cursor.newRow()
        builder.add(stickerPack.name)
        builder.add(stickerPack.title)
        builder.add("roldex.me")
        builder.add(if(isAnimated) "tray.png" else getFirstStickerFile)
        builder.add("")
        builder.add("")
        builder.add("")
        builder.add("https://roldex.me/")
        builder.add("https://roldex.me/")
        builder.add("https://roldex.me/")
        builder.add(1)
        builder.add(0)
        builder.add(if(isAnimated) 1 else 0)


        cursor.setNotificationUri(
            requireContext().contentResolver,
            uri
        )
        return cursor
    }


    @RequiresApi(Build.VERSION_CODES.R)
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        Log.i("DEBUG", "OPENING ASSETS")
        Log.i("DEBUG", uri.toString())
        val path = uri.path ?: return null
        val pathSegments = path.split("/")
        val folderName = pathSegments.getOrNull(pathSegments.size - 2)
        val fileName = pathSegments.getOrNull(pathSegments.size - 1)
        val folder = this.stickerPacks?.find { sticker -> sticker.name == folderName }
        if(folder == null) return null
        var filePath = fileName?.let { name ->
            folder.stickers.find { sticker ->
                sticker.absolutePath?.endsWith(name) == true
            }?.absolutePath
        }


        if (filePath == null) {
            Log.i("DEBUG", "FilePath Is Null, trying to find file in folder")

            val file = folderName?.let {
                InternalStorageManager.getStickerFolder(this.requireContext(), it)
            }

            // Only proceed if we have a valid folder
            if (file != null && fileName != null) {
                val fileObj = File(file, fileName)

                if (!fileObj.exists()) {
                    Log.w("DEBUG", "File doesn't exist: $fileName in ${file.absolutePath}")
                    return null
                }

                // If we found the file, assign its path to filePath
                filePath = fileObj.absolutePath
                Log.i("DEBUG", "Found file: $fileName at $filePath")
            } else {
                Log.w("DEBUG", "Invalid folder or filename")
                return null
            }
        }




        return try {
            Log.w("DEBUG", "$fileName : ${filePath.toString()}")
            val file = File(filePath)


            if (!file.exists()) {
                Log.w("DEBUG", "File path not exists")
                return null
            }else{
                Log.i("DEBUG", "File path: $file")
            }
            val dirfile = moveToExternalCacheDir(this.requireContext(), file)
//            Log.w("DEBUG", "FILE: ${dirfile.toString()}")
////            val dir =  context.filesDir.absolutePath
////            val file = File(dir, identifier + "/" + fileName)
//            val pfd = ParcelFileDescriptor.open(dirfile, MODE_READ_ONLY)
//            if (dirfile != null) {
//                return AssetFileDescriptor(pfd, 0, dirfile.length())
//            }
//            return null
            return if (file.exists()) {
                // Open a ParcelFileDescriptor for the file if it exists
                val parcelFileDescriptor = ParcelFileDescriptor.open(dirfile, ParcelFileDescriptor.MODE_READ_ONLY)
                AssetFileDescriptor(parcelFileDescriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
            } else {
                null // or handle the case where the file does not exist
            }

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun moveToExternalCacheDir(context: Context, sourceFile: File): File? {
        val externalFilesDir = context.getExternalFilesDir(null)
        val destinationFile = File(externalFilesDir, sourceFile.name) // Destination path
        if(destinationFile.exists()) return destinationFile
        return try {
            sourceFile.inputStream().use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)  // Copy file
                }
            }
            destinationFile // Return new file location
        } catch (e: IOException) {
            Log.e("FileMove", "Error moving file", e)
            null
        }
    }


    private fun cursorToArray(cursor: Cursor?): List<Map<String, String>> {
        val result: MutableList<Map<String, String>> = ArrayList()

        if (cursor == null || !cursor.moveToFirst()) {
            Log.i("DEBUG", "Cursor is empty or null.")
            return result
        }

        val columnNames = cursor.columnNames

        do {
            val rowMap: MutableMap<String, String> = HashMap()
            for (columnName in columnNames) {
                val columnIndex = cursor.getColumnIndex(columnName)
                val value = cursor.getString(columnIndex)
                rowMap[columnName] = value
            }
            result.add(rowMap)
        } while (cursor.moveToNext())

        return result
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getStickersForAStickerPack(
        uri: Uri,
        stickerPackList: StickerSet
    ): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                STICKER_FILE_NAME_IN_QUERY,
                STICKER_FILE_EMOJI_IN_QUERY,
                STICKER_FILE_ACCESSIBILITY_TEXT_IN_QUERY
            )
        )
        for (sticker in stickerPackList.stickers){
            val stickerName = sticker.absolutePath?.substringAfterLast("/")
            cursor.addRow(
                arrayOf<Any?>(
                    stickerName,
                    TextUtils.join(",", arrayOf(sticker.emoji)),
                    ""
                )
            )
        }


        cursor.setNotificationUri(
            requireContext().contentResolver,
            uri
        )

        Log.i("DEBUG", cursorToArray(cursor).toString())
        return cursor
    }


    override fun getType(uri: Uri): String? {
        Log.d("DEBUG", "getType() called with URI: $uri")
        return null
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun generateRandomEmojis(count: Int): String {
        // Unicode range for emojis (common ones)
        val emojiStart = 0x1F600 // 😀
        val emojiEnd = 0x1F64F   // 🙏

        return (1..count).joinToString("") {
            val randomEmoji = Random.nextInt(emojiStart, emojiEnd + 1)
            String(Character.toChars(randomEmoji))
        }
    }



}

