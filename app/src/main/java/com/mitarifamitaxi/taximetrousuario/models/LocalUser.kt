package com.mitarifamitaxi.taximetrousuario.models

import java.util.Date
import kotlin.reflect.full.memberProperties

data class LocalUser(
    val id: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val documentNumber: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val countryCode: String? = null,
    val countryCodeWhatsapp: String? = null,
    val countryCurrency: String? = null,
    val city: String? = null,
    val familyNumber: String? = null,
    val supportNumber: String? = null,
    val lastActive: Date? = null,
    var authProvider: AuthProvider? = null,
    var profilePicture: String? = null
)

enum class AuthProvider {
    google,
    email,
    apple
}


enum class DriverStatus {
    PENDING,
    APPROVED,
    REJECTED
}

fun LocalUser.toUpdateMapReflective(): Map<String, Any> =
    this::class.memberProperties
        .mapNotNull { prop ->
            prop.getter.call(this)
                ?.let { prop.name to it }
        }
        .toMap()
