package com.mitarifamitaxi.taximetrousuario.models

data class Rates(
    val city: String? = null,
    val airportRateUnits: Double? = null,
    val doorToDoorRateUnits: Double? = null,
    val holidayRateUnits: Double? = null,
    val dragSpeed: Double? = null,
    val meters: Int? = null,
    val minimumRateUnits: Double? = null,
    val startRateUnits: Double? = null,
    val unitPrice: Double? = null,
    val waitTime: Int? = null,
    val waitTimeRateUnit: Double? = null,
    val nightHourSurcharge: Int? = null,
    val nighMinuteSurcharge: Int? = null,
    val morningHourSurcharge: Int? = null,
    val morningMinuteSurcharge: Int? = null,
    val validateHolidaySurcharge: Boolean? = null,
    val speedLimit: Int? = null,
    val speedUnits: String? = null
)