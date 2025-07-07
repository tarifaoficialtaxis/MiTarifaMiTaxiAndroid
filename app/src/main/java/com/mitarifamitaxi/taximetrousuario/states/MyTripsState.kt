package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Trip

data class MyTripsState(
    val trips: List<Trip> = emptyList()
)