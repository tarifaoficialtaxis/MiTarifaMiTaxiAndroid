package com.mitarifamitaxi.taximetrousuario.helpers

import android.graphics.Bitmap
import androidx.core.graphics.scale
import kotlin.math.min

fun Bitmap.scaled(maxWidth: Int, maxHeight: Int): Bitmap {
    val ratio = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    val w = (width * ratio).toInt()
    val h = (height * ratio).toInt()
    return this.scale(w, h)
}