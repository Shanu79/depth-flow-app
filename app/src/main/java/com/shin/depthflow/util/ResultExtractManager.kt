package com.shin.depthflow.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    return withContext(Dispatchers.IO) {
        val filename = "DepthFlow_${System.currentTimeMillis()}.png"
        var fos: OutputStream? = null
        var imageUri: Uri? = null

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Save to the Pictures/DepthFlow directory on Android 10+
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DepthFlow")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val contentResolver = context.contentResolver
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            imageUri?.let { uri ->
                fos = contentResolver.openOutputStream(uri)
                fos?.let { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
                true
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            fos?.close()
        }
    }
}

suspend fun shareBitmap(context: Context, bitmap: Bitmap) {
    withContext(Dispatchers.IO) {
        try {
            // Save to app's cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "shared_image.png")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
            fileOutputStream.close()

            // Get URI using FileProvider
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Launch share sheet on the main thread
            withContext(Dispatchers.Main) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, imageUri)

                    // Updated Marketing Message
                    putExtra(Intent.EXTRA_TEXT, "I just brought this photo to life in 3D using DepthFlow AI! ✨ Download the app to try it yourself! 💜")

                    type = "image/png"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share DepthFlow 3D Image"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error sharing image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}







