package com.mitarifamitaxi.taximetrousuario.models

data class Contact(
    val city: String? = null,
    val countryCode: String? = null,
    val lines: List<ContactCatalog> = emptyList(),
    val pqrEmail: String? = null,
    val showSosWarning: Boolean = false,
    val warningMessage: String? = null
)

data class ContactCatalog(
    val key: String? = null,
    val line1: String? = null,
    val line2: String? = null,
    val whatsapp: String? = null,
    val web: String? = null,
    val image: String? = null,
    val order: Int = 0,
    val show: Boolean = true
)

