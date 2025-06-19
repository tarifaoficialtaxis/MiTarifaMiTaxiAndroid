package com.mitarifamitaxi.taximetrousuario.helpers

fun String.isValidEmail(): Boolean {
    return this.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun String.getShortAddress(): String {
    return this.split(",").getOrNull(0) ?: ""
}

fun String.isValidPassword(): Boolean {
    val minLength = 8
    val hasNumber = this.any { it.isDigit() }
    val hasSymbol = this.any { !it.isLetterOrDigit() }

    return this.length >= minLength && hasNumber && hasSymbol
}