package app.askday.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** Copies a picked photo into the app's own folder, shrunk to a square-ish 512 px so it loads fast. */
fun savePhoto(context: Context, uri: Uri, name: String): String? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    val out = File(dir, "$name.jpg")
    out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
    out.absolutePath
}.getOrNull()
