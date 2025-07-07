package com.mitarifamitaxi.taximetrousuario.models

import android.graphics.Bitmap

data class Trip(
    val id: String? = null,
    val userId: String? = null,
    val startAddress: String? = null,
    val endAddress: String? = null,
    val startCoords: UserLocation? = null,
    val endCoords: UserLocation? = null,
    val startHour: String? = null,
    val endHour: String? = null,
    val isUnits: Boolean? = null,
    val unitPrice: Double? = null,
    val units: Double? = null,
    val baseUnits: Double? = null,
    val rechargeUnits: Double? = null,
    val total: Double? = null,
    val distance: Double? = null,
    val baseRate: Double? = null,
    val recharges: List<Recharge> = emptyList(),
    val routeImage: String? = null,
    val routeImageLocal: Bitmap? = null,
    val companyImage: String? = null,
    val currency: String? = null,
)
