package com.axerioo.gamesphere.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// --- ImageStorageManager Class ---
// This class is responsible for managing the local storage of images.

class ImageStorageManager(private val context: Context) {

    // --- Get Cover Image Directory ---
    // Private helper function to get or create the directory where cover images will be stored.
    // Images are stored in a subdirectory named "cover_images" within the app's internal files directory.

    private fun getCoverImageDirectory(): File {
        val directory = File(context.filesDir, "cover_images")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return directory
    }

    // --- Save Image From URL ---
    // Downloads an image from the given `imageUrl`, optionally scales it, compresses it to JPEG format, and saves it to a local file.
    // The filename is based on the `gameId`. This operation is performed on the IO dispatcher.
    // @return The absolute path to the saved image file, or null if an error occurs.

    suspend fun saveImageFromUrl(
        imageUrl: String,
        gameId: Long,
        targetWidth: Int? = null,  // Optional width
        targetHeight: Int? = null, // Optional height
        quality: Int = 85          // JPEG compression quality
    ): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        var fos: FileOutputStream? = null
        var tempBitmap: Bitmap? = null // To hold the bitmap that needs to be recycled

        try {
            val url = URL(imageUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000 // 15 seconds connection timeout
            connection.readTimeout = 15000    // 15 seconds read timeout
            connection.doInput = true         // Allow input streams
            connection.connect()              // Establish the connection

            // Check for successful HTTP response
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                System.err.println("Server returned HTTP ${connection.responseCode} ${connection.responseMessage} for $imageUrl")
                return@withContext null // Return null if connection is not OK
            }

            val inputStream = connection.inputStream
            // Decode the input stream into a Bitmap
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close() // Close the input stream as soon as possible

            if (bitmap == null) {
                System.err.println("Failed to decode bitmap from $imageUrl")
                return@withContext null
            }
            tempBitmap = bitmap // Assign to tempBitmap for later recycling

            // --- Image Scaling (Optional) ---
            // If targetWidth and targetHeight are provided and valid, scale the bitmap.

            if (targetWidth != null && targetHeight != null && targetWidth > 0 && targetHeight > 0) {
                val originalWidth = bitmap.width
                val originalHeight = bitmap.height

                // Calculate scale factor.
                val scaleFactor = if (originalWidth.toFloat() / originalHeight.toFloat() > targetWidth.toFloat() / targetHeight.toFloat()) {
                    targetWidth.toFloat() / originalWidth.toFloat() // Scale based on width
                } else {
                    targetHeight.toFloat() / originalHeight.toFloat() // Scale based on height
                }

                if (scaleFactor < 1.0f || (targetWidth != originalWidth || targetHeight != originalHeight)) {
                    val matrix = Matrix()
                    matrix.postScale(scaleFactor, scaleFactor)
                    val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)

                    if (scaledBitmap != bitmap) { // If a new bitmap was created
                        tempBitmap.recycle() // Recycle the original bitmap if it's different from the scaled one
                    }
                    bitmap = scaledBitmap // Use the scaled bitmap
                    tempBitmap = scaledBitmap // Update tempBitmap to the latest one for recycling
                }
            }

            // Create the output file in the designated directory.
            val file = File(getCoverImageDirectory(), "cover_$gameId.jpg")
            fos = FileOutputStream(file)
            // Compress and save the bitmap to the file as JPEG.
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), fos)
            fos.flush() // Ensure all data is written to the file.

            file.absolutePath // Return the absolute path of the saved file.
        } catch (e: IOException) {
            // Handle IO exceptions (e.g., network issues, file writing errors).
            e.printStackTrace()
            null
        } catch (e: OutOfMemoryError) {
            // Handle OutOfMemoryError, which can occur with large images.
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {

            // --- Resource Cleanup ---
            // Ensure resources are released regardless of whether an exception occurred.

            tempBitmap?.recycle() // Recycle the last used bitmap instance.
            connection?.disconnect() // Close the HTTP connection.
            try {
                fos?.close() // Close the file output stream.
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // --- Delete Image ---
    // Deletes a locally stored image file given its `filePath`.

    suspend fun deleteImage(filePath: String?): Boolean = withContext(Dispatchers.IO) {
        filePath?.let { path -> // Proceed only if filePath is not null.
            try {
                val file = File(path)
                if (file.exists()) {
                    return@withContext file.delete() // Attempt to delete the file and return success status.
                }
                return@withContext true // If file doesn't exist, it was successfully "deleted".
            } catch (e: SecurityException) {
                // Handle security exceptions (e.g., lack of permission to delete).
                e.printStackTrace()
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } ?: true // If filePath is null, consider it a "successful" deletion (nothing to delete).
    }
}