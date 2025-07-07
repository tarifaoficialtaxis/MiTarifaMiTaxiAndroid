package com.mitarifamitaxi.taximetrousuario.models

data class Rates(
    val city: String? = null,
    val showUnits: Boolean? = null,
    val dragSpeed: Double? = null,
    val meters: Int? = null,
    val minimumRateUnits: Double? = null,
    val startRateUnits: Double? = null,
    val unitPrice: Double? = null,
    val waitTime: Int? = null,
    val waitTimeRateUnit: Double? = null,
    val speedLimit: Int? = null,
    val speedUnits: String? = null,
    val recharges: List<Recharge> = emptyList(),
)

data class Recharge(
    val key: String? = null,
    val name: String? = null,
    val order: Int? = null,
    val units: Double? = null,
    val show: Boolean? = null
)

