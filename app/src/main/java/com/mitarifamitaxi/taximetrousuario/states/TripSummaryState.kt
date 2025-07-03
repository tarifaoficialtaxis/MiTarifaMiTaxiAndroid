package com.mitarifamitaxi.taximetrousuario.states

import com.mitarifamitaxi.taximetrousuario.models.Trip

data class TripSummaryState(
    val isDetails: Boolean = false,
    val tripData: Trip = Trip(),
    val showShareDialog: Boolean = false,
    val shareNumber: String = "",
    val isShareNumberError: Boolean = false,
    val isDetailsOpen: Boolean = false
)