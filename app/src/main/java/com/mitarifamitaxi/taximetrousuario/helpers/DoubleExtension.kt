package com.mitarifamitaxi.taximetrousuario.helpers
import kotlin.math.roundToInt

fun Double.formatDigits(digits: Int) = "%.${digits}f".format(this)

fun Double.formatNumberWithDots(): String =
    this.roundToInt().formatNumberWithDots()