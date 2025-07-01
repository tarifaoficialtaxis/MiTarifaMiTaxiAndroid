package com.mitarifamitaxi.taximetrousuario.models

data class Contact(
    val ambulance: ContactCatalog? = null,
    val animalCare: ContactCatalog? = null,
    val city: String? = null,
    val firefighter: ContactCatalog? = null,
    val police: ContactCatalog? = null,
    val pqrEmail: String? = null,

    val showSosWarning: Boolean = false,
    val warningMessage: String? = null,
)

data class ContactCatalog(
    val line1: String? = null,
    val line2: String? = null,
    val whatsapp: String? = null,
)

