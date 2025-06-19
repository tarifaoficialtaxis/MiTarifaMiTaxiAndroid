package com.mitarifamitaxi.taximetrousuario.helpers

import android.content.Context
import com.mitarifamitaxi.taximetrousuario.R

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

fun getFirebaseAuthErrorMessage(appContext: Context, errorCode: String): String {
    return when (errorCode) {
        "ERROR_INVALID_EMAIL" -> appContext.getString(R.string.error_invalid_email)
        "ERROR_INVALID_CREDENTIAL" -> appContext.getString(R.string.error_wrong_credentials)
        "ERROR_USER_NOT_FOUND" -> appContext.getString(R.string.error_user_not_found)
        "ERROR_USER_DISABLED" -> appContext.getString(R.string.error_user_disabled)
        "ERROR_TOO_MANY_REQUESTS" -> appContext.getString(R.string.error_too_many_requests)
        "ERROR_OPERATION_NOT_ALLOWED" -> appContext.getString(R.string.error_operation_not_allowed)
        "ERROR_EMAIL_ALREADY_IN_USE" -> appContext.getString(R.string.error_email_already_in_use)
        else -> appContext.getString(R.string.error_authentication_failed)
    }
}