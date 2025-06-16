package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

fun Uri.toBitmap(
    context: Context,
    maxSizeBytes: Long = 1_500_000L
): Bitmap? {
    val original: Bitmap = try {
        val source = ImageDecoder.createSource(context.contentResolver, this)
        ImageDecoder.decodeBitmap(source)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }

    var quality = 100
    var compressedBytes: ByteArray

    do {
        val baos = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Bitmap.CompressFormat.WEBP_LOSSY
        else
            Bitmap.CompressFormat.WEBP

        original.compress(format, quality, baos)
        compressedBytes = baos.toByteArray()
        baos.close()

        quality -= 5
    } while (compressedBytes.size > maxSizeBytes && quality > 0)

    if (compressedBytes.size > maxSizeBytes) return null

    return BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
}
