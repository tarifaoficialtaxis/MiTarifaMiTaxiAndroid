package com.mitarifamitaxi.taximetrousuario.states


import com.mitarifamitaxi.taximetrousuario.models.PlacePrediction
import com.mitarifamitaxi.taximetrousuario.models.UserLocation
import com.google.android.gms.maps.model.LatLng

data class RoutePlannerState(
    val startAddress: String = "",
    val startLocation: UserLocation? = null,
    val endAddress: String = "",
    val endLocation: UserLocation? = null,
    val isSelectingStart: Boolean = true,
    val isStartAddressFocused: Boolean = false,
    val isEndAddressFocused: Boolean = false,
    val places: List<PlacePrediction> = emptyList(),
    val routePoints: List<LatLng> = emptyList()
)