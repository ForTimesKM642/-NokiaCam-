package com.example.nokiacam.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoStorage {
    fun saveBitmap(context: Context, bitmap: Bitmap, mode: CameraMode): String {
        val name = "NOKIACAM_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NokiaCam")
            put(MediaStore.Images.Media.IS_PENDING, 1)
            put(MediaStore.Images.Media.DESCRIPTION, "Captured with ${mode.label} mode")
        }
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Could not create MediaStore entry")

        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, if (mode == CameraMode.RETRO) 88 else 95, out)) {
                throw IllegalStateException("Failed to write JPEG")
            }
        } ?: throw IllegalStateException("Could not open output stream")

        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri.toString()
    }
}
