package com.mitarifamitaxi.taximetrousuario.models

data class PqrsData(
    val city: String? = null,
    val countryCode: String? = null,
    val email: String? = null,
    val reasons: List<PqrsReasons> = emptyList()
)

data class PqrsReasons(
    val key: String? = null,
    val name: String? = null,
    val order: Int = 0,
    val show: Boolean = true
)

