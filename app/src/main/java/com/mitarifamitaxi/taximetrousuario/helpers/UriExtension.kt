package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.ImageDecoder

fun Uri.toBitmap(context: Context): Bitmap? {
    return try {
        val source = ImageDecoder.createSource(context.contentResolver, this)
        ImageDecoder.decodeBitmap(source)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
