import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.r0ld3x.telegramsticker.types.StickerSet
import java.io.File
import java.io.FileReader

object InternalStorageManager {

    private const val PREF_NAME = "StickerPrefs"
    private const val IDENTIFIER_KEY = "identifiers"
    private const val ROOT_FOLDER = "StickerPacks"

    fun saveIdentifier(context: Context, identifier: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val existingIds = prefs.getStringSet(IDENTIFIER_KEY, mutableSetOf()) ?: mutableSetOf()
        existingIds.add(identifier)

        editor.putStringSet(IDENTIFIER_KEY, existingIds)
        editor.apply()
    }

    fun checkIdentifierExists(context: Context, identifier: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        Log.i("Tag", prefs.getStringSet().toString())
        val existingIds = prefs.getStringSet(IDENTIFIER_KEY, mutableSetOf()) ?: mutableSetOf()
        return existingIds.contains(identifier)
    }

    /**
     * Get a reference to the directory of a specific identifier.
     */
    fun getStickerFolder(context: Context, identifier: String): File {
        val dir = File(context.filesDir, "$ROOT_FOLDER/$identifier")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getStickerSetFromFile(context: Context, stickerName: String): StickerSet? {
        val file = getStickerFolder(context, "$stickerName/data.json")

        if (!file.exists()) {
            return null // File doesn't exist, return null
        }

        return try {
            val reader = FileReader(file)
            val stickerSet = Gson().fromJson(reader, StickerSet::class.java)
            reader.close()
            stickerSet
        } catch (e: JsonSyntaxException) {
            e.printStackTrace()
            null // If JSON parsing fails, return null
        } catch (e: Exception) {
            e.printStackTrace()
            null // Any other error, return null
        }
    }

    /**
     * Save a file (image/json) inside the identifier's folder.
     */
    fun saveFile(context: Context, identifier: String, fileName: String, fileContent: ByteArray) {
        val folder = getStickerFolder(context, identifier)
        val file = File(folder, fileName)
        file.writeBytes(fileContent)
    }

    /**
     * Save a JSON file inside the identifier's folder.
     */
    fun saveJson(context: Context, identifier: String, jsonString: String): String? {
        val folder = getStickerFolder(context, identifier)
        val file = File(folder, "data.json")
        file.writeText(jsonString)
        return getFileFromFolder(context, identifier, "data.json")


    }

    /**
     * Retrieve JSON data from the identifier's folder.
     */
    fun getJson(context: Context, identifier: String): String? {
        val folder = getStickerFolder(context, identifier)
        val file = File(folder, "data.json")
        return if (file.exists()) file.readText() else null
    }

    /**
     * Retrieve all files in an identifier's folder.
     */
    fun getFiles(context: Context, identifier: String): List<File> {
        val folder = getStickerFolder(context, identifier)
        return folder.listFiles()?.toList() ?: emptyList()
    }


    fun saveFileToFolder(context: Context, identifier: String, fileName: String, data: String) {
        val folder = getStickerFolder(context, identifier)
        val file = File(folder, fileName)
        try {
            file.writeText(data)
            Log.i("InternalStorage", "File saved successfully at ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("InternalStorage", "Error saving file: ${e.message}")
        }
    }

    fun getFileFromFolder(context: Context, identifier: String, fileName: String): String? {
        val folder = getStickerFolder(context, identifier)
        val file = File(folder, fileName)

        return if (file.exists()) {
            try {
                file.readText()
            } catch (e: Exception) {
                Log.e("InternalStorage", "Error reading file: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    fun getAllDataJsonFiles(context: Context): List<StickerSet> {
        val stickerPacksFolder =
            File(context.filesDir, "StickerPacks") // Path to StickerPacks folder
        val stickerSets = mutableListOf<StickerSet>()

        if (!stickerPacksFolder.exists()) {
            return stickerSets
        }


        // Get all subdirectories in StickerPacks
        val subfolders = stickerPacksFolder.listFiles { file -> file.isDirectory }

        subfolders?.forEach { folder ->
            val dataFile = File(folder, "data.json")
            if (dataFile.exists()) {
                try {
                    val jsonContent = dataFile.readText()
                    val stickerSet = Gson().fromJson<StickerSet>(
                        jsonContent,
                        object : TypeToken<StickerSet>() {}.type
                    )
                    stickerSets.add(stickerSet)
                } catch (e: Exception) {
                    Log.e(
                        "DataLoader",
                        "Error reading or parsing data.json in folder: ${folder.absolutePath}",
                        e
                    )
                }
            }
        }

        return stickerSets
    }
}
