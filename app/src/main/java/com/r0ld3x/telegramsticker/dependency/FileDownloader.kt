import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.content.Context
import java.io.ByteArrayOutputStream

object FileDownloader {
    suspend fun downloadAndConvertToWebP(url: String, destinationFile: File, context: Context, isVideo: Boolean= false): String? {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null

            if(destinationFile.exists()){
                return@withContext destinationFile.absolutePath
            }

            try {
                // Step 1: Download the image to a temporary file
                val urlConnection = URL(url)
                connection = urlConnection.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val statusCode = connection.responseCode
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    println("Server returned HTTP error code $statusCode")
                    return@withContext null
                }

                inputStream = connection.inputStream
                if(isVideo){
                    FileOutputStream(destinationFile).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }

                    return@withContext if (destinationFile.exists()) {
                        println("Image converted to WebP: ${destinationFile.absolutePath}")
                        destinationFile.absolutePath
                    } else {
                        println("Failed to convert image to WebP.")
                        null
                    }
                }


                val tempFile = File(destinationFile.parent, "temp_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }

                println("File downloaded to temporary file: ${tempFile.absolutePath}")

                // Step 2: Convert the image to WebP with 512x512 dimensions in a contained manner
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                if (bitmap == null) {
                    println("Failed to decode downloaded image")
                    tempFile.delete()
                    return@withContext null
                }

                // Create a 512x512 bitmap with transparent background
                val resultBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resultBitmap)

                // Calculate scaling to fit the image within 512x512 while maintaining aspect ratio
                val sourceWidth = bitmap.width
                val sourceHeight = bitmap.height
                val targetWidth = 512
                val targetHeight = 512

                val scale: Float
                val dx: Float
                val dy: Float

                if (sourceWidth * targetHeight > targetWidth * sourceHeight) {
                    scale = targetWidth.toFloat() / sourceWidth.toFloat()
                    dx = 0f
                    dy = (targetHeight - sourceHeight * scale) * 0.5f
                } else {
                    scale = targetHeight.toFloat() / sourceHeight.toFloat()
                    dx = (targetWidth - sourceWidth * scale) * 0.5f
                    dy = 0f
                }

                val matrix = Matrix()
                matrix.setScale(scale, scale)
                matrix.postTranslate(dx, dy)

                // Draw the scaled bitmap onto the canvas
                canvas.drawBitmap(bitmap, matrix, null)

                // Save as WebP with 85% quality to the destination file
                val outputStream = ByteArrayOutputStream()
                resultBitmap.compress(Bitmap.CompressFormat.WEBP, 85, outputStream)
                FileOutputStream(destinationFile).write(outputStream.toByteArray())

                // Clean up
                bitmap.recycle()
                resultBitmap.recycle()
                tempFile.delete()

                return@withContext if (destinationFile.exists()) {
                    println("Image converted to WebP: ${destinationFile.absolutePath}")
                    destinationFile.absolutePath
                } else {
                    println("Failed to convert image to WebP.")
                    null
                }

            } catch (e: IOException) {
                println("Error processing file: ${e.message}")
                return@withContext null
            } finally {
                try {
                    inputStream?.close()
                    connection?.disconnect()
                } catch (e: IOException) {
                    println("Error closing streams: ${e.message}")
                }
            }
        }
    }


}