package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Trip

data class HomeState(
    val trips: List<Trip> = emptyList()
)
