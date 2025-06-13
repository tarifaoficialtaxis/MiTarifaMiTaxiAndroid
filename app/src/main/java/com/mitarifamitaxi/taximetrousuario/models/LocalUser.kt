package com.mitarifamitaxi.taximetrousuario.models

import java.util.Date

data class LocalUser(
    val id: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val mobilePhone: String? = null,
    val email: String? = null,
    val countryCode: String? = null,
    val countryCodeWhatsapp: String? = null,
    val countryCurrency: String? = null,
    val city: String? = null,
    val familyNumber: String? = null,
    val supportNumber: String? = null,
    val lastActive: Date? = null,
    var role: UserRole? = null,
    var authProvider: AuthProvider? = null,
)

enum class AuthProvider {
    google,
    email,
    apple
}

enum class UserRole {
    USER,
    DRIVER,
    ADMIN
}

