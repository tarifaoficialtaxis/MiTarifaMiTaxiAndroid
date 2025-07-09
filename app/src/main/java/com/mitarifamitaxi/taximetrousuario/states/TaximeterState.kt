package com.mitarifamitaxi.taximetrousuario.states

import com.google.android.gms.maps.model.LatLng
import com.mitarifamitaxi.taximetrousuario.models.Rates
import com.mitarifamitaxi.taximetrousuario.models.Recharge
import com.mitarifamitaxi.taximetrousuario.models.UserLocation

data class TaximeterState(
    val total: Double = 0.0,
    val distanceMade: Double = 0.0,
    val units: Double = 0.0,
    val rechargeUnits: Double = 0.0,
    val formattedTime: String = "00:00",
    val startAddress: String = "",
    val endAddress: String = "",
    val currentPosition: UserLocation = UserLocation(),
    val startLocation: UserLocation = UserLocation(),
    val endLocation: UserLocation = UserLocation(),
    val routeCoordinates: List<LatLng> = emptyList(),
    val isTaximeterStarted: Boolean = false,
    val dragTimeElapsed: Int = 0,
    val currentSpeed: Int = 0,
    val isFabExpanded: Boolean = false,
    val fitCameraPosition: Boolean = false,
    val takeMapScreenshot: Boolean = false,
    val isMapLoaded: Boolean = false,
    val rates: Rates = Rates(),
    val isRechargesOpen: Boolean = false,
    val rechargesSelected: List<Recharge> = emptyList(),
    val isSoundEnabled: Boolean = true,
)